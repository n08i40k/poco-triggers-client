#define BIONIC_IOCTL_NO_SIGNEDNESS_OVERLOAD

#include "constants.h"
#include "fts_lock.h"
#include "server.h"
#include "touch_emulator.h"
#include "virt_device.h"
#include "xm_watcher.h"

#include <android/log.h>
#include <arpa/inet.h>
#include <array>
#include <cstdio>
#include <functional>
#include <linux/input.h>
#include <mutex>
#include <sys/wait.h>
#include <thread>
#include <unistd.h>

#define LOG_ANDROID(...)                                                       \
    ((void)__android_log_print(ANDROID_LOG_INFO, "TriggersDaemon", __VA_ARGS__))

namespace
{

} // namespace

int
main()
{
    LOG_ANDROID("Hello!");
    printf("Hello, World!\n");
    fflush(stdout);

    // prepare

    const fts_lock fts{};

    if (fts.fd() < 0)
        return EXIT_FAILURE;

    const xm_watcher xm{};

    if (xm.fd() < 0)
        return EXIT_FAILURE;

    const virt_device virt{};

    if (virt.fd() < 0)
        return EXIT_FAILURE;

    server server{};

    if (server.fd() < 0)
        return EXIT_FAILURE;

    // used variables

    std::mutex mtx{};

    bool no_taps{ true };

    std::array<bool, 2> is_trigger_pressed{ false };
    std::array<std::chrono::system_clock::time_point, 2> trigger_enable_date{};

    client_message triggers_data{};

    input_event ev{};
    __s32       slot{};

    // server thread
    std::thread server_thread([&] {
        server.work_thread([&](const client_message& message) {
            printf("New triggers settings!\n");
            fflush(stdout);

            if (message.lower.index() > 1) {
                fprintf(
                    stderr,
                    "Invalid index passed to lower trigger settings: %d\n",
                    message.lower.index());
                fflush(stderr);

                return;
            }

            if (message.upper.index() > 1) {
                fprintf(
                    stderr,
                    "Invalid index passed to upper trigger settings: %d\n",
                    message.upper.index());
                fflush(stderr);

                return;
            }

            if (message.upper.index() == message.lower.index()) {
                fprintf(
                    stderr,
                    "Same indexes on triggers not allowed: %d\n",
                    message.upper.index());
                fflush(stderr);

                return;
            }

            const std::lock_guard lock{ mtx };
            triggers_data = message;
        });
    });

    // all events
    std::thread virt_thread([&] {
        // ReSharper disable once CppDFAEndlessLoop
        while (true) {
            if (read(virt.fd(), &ev, sizeof(ev)) != sizeof(ev))
                continue;

            const std::lock_guard lock{ mtx };

            if (ev.type == EV_ABS && ev.code == ABS_MT_SLOT)
                slot = ev.value;
        }
    });

    std::thread xm_thread([&] {
        xm.work_thread([&](const std::pair<std::uint16_t, bool>& event) {
            const auto& [key, pressed] = event;

            const std::lock_guard lock{ mtx };

            if (key == UPPER_CLICK_KEY) {
                is_trigger_pressed[triggers_data.upper.index()] = pressed;

                const touch_event touch_ev{
                    .index          = triggers_data.upper.index(),
                    .pressed        = pressed,
                    .x              = triggers_data.upper.x(),
                    .y              = triggers_data.upper.y(),
                    .no_screen_taps = no_taps,
                    .another_trigger_pressed =
                        is_trigger_pressed[triggers_data.upper.inv_index()],
                    .current_slot = slot
                };

                emulate_touch(virt.fd(), touch_ev);
                return;
            }

            if (key == LOWER_CLICK_KEY) {
                is_trigger_pressed[triggers_data.lower.index()] = pressed;

                const touch_event touch_ev{
                    .index          = triggers_data.lower.index(),
                    .pressed        = pressed,
                    .x              = triggers_data.lower.x(),
                    .y              = triggers_data.lower.y(),
                    .no_screen_taps = no_taps,
                    .another_trigger_pressed =
                        is_trigger_pressed[triggers_data.lower.inv_index()],
                    .current_slot = slot
                };

                emulate_touch(virt.fd(), touch_ev);
                return;
            }

            if (!pressed
                && (key == UPPER_ENABLE_KEY || key == LOWER_ENABLE_KEY)) {
                const auto this_index = key == UPPER_ENABLE_KEY
                                          ? triggers_data.upper.index()
                                          : triggers_data.lower.index();

                const auto another_index = key == UPPER_ENABLE_KEY
                                             ? triggers_data.lower.index()
                                             : triggers_data.upper.index();

                trigger_enable_date[this_index] =
                    std::chrono::system_clock::now();

                if ((trigger_enable_date[this_index]
                     - trigger_enable_date[another_index])
                    < std::chrono::milliseconds(500)) {
                    printf("Posting intent...\n");
                    fflush(stdout);

                    const auto pid = fork();

                    if (pid == -1) {
                        perror("fork");
                        return;
                    }

                    if (pid == 0) {
                        execlp(
                            "sh",
                            "sh",
                            "-c",
                            "am startservice "
                            "ru.n08i40k.poco.triggers/.service.OverlayService",
                            nullptr);
                        _exit(127);
                    }

                    waitpid(pid, nullptr, 0);
                }
            }
        });
    });

    // screen events
    while (true) {
        if (read(fts.fd(), &ev, sizeof(ev)) != sizeof(ev))
            continue;

        const std::lock_guard lock{ mtx };

        if (ev.type == EV_KEY
            && (ev.code == BTN_TOUCH || ev.code == BTN_TOOL_FINGER)) {
            no_taps = ev.value == 0;

            // если прекращено нажатие на экран, а триггеры не зажаты
            if (is_trigger_pressed[0] || is_trigger_pressed[1])
                continue;
        }

        if (write(virt.fd(), &ev, sizeof(ev)) < 0)
            perror("Failed to write event to uinput device");
    }

    server_thread.join();
    virt_thread.join();
    xm_thread.join();

    return 0;
}
