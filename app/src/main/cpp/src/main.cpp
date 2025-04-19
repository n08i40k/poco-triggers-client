#include "constants.h"
#include "fts_lock.h"
#include "server.h"
#include "touch_emulator.h"
#include "virt_device.h"
#include "xm_watcher.h"

#include <algorithm>
#include <android/log.h>
#include <array>
#include <cstdio>
#include <linux/input.h>
#include <mutex>
#include <sys/wait.h>
#include <thread>
#include <unistd.h>

#define LOG_ANDROID(...)                                                       \
    ((void)__android_log_print(                                                \
        ANDROID_LOG_INFO, "AOSP-TD", __VA_ARGS__)) // aosp tower defense :)

enum trigger_index : std::uint8_t {
    UPPER_TRIGGER = 0,
    LOWER_TRIGGER,
    TRIGGERS_COUNT,
};

using namespace std::chrono;

struct trigger_data {
    /**
     * Is pressed.
     */
    bool pressed{};

    /**
     * Time point of last open.
     */
    system_clock::time_point open_tp{};

    /**
     * Time point of last close.
     */
    system_clock::time_point close_tp{};

    /**
     * Settings provided by client.
     */
    trigger_settings ev_settings{};
};

class triggers_array : public std::array<trigger_data, TRIGGERS_COUNT>
{
public:
    [[nodiscard]]
    bool
    any_pressed() const
    {
        return std::ranges::any_of(
            *this, [](const auto& data) { return data.pressed; });
    }

    [[nodiscard]]
    bool
    pressed(const trigger_index index) const
    {
        return this->at(index).pressed;
    }

    [[nodiscard]]
    trigger_data&
    by_key(const std::uint32_t key)
    {
        switch (key) {
        case UPPER_CLICK_KEY:
        case UPPER_DISABLE_KEY:
        case UPPER_ENABLE_KEY : return this->at(UPPER_TRIGGER);

        case LOWER_CLICK_KEY  :
        case LOWER_DISABLE_KEY:
        case LOWER_ENABLE_KEY : return this->at(LOWER_TRIGGER);
        default               : throw std::invalid_argument("Invalid key");
        }
    }

    [[nodiscard]]
    trigger_data&
    by_key_inv(const std::uint32_t key)
    {
        switch (key) {
        case UPPER_CLICK_KEY:
        case UPPER_DISABLE_KEY:
        case UPPER_ENABLE_KEY : return this->at(LOWER_TRIGGER);

        case LOWER_CLICK_KEY  :
        case LOWER_DISABLE_KEY:
        case LOWER_ENABLE_KEY : return this->at(UPPER_TRIGGER);
        default               : throw std::invalid_argument("Invalid key");
        }
    }
};

class endless_thread : public std::thread
{
public:
    explicit endless_thread(const std::function<void()>& func)
        : std::thread([func] {
            // ReSharper disable once CppDFAEndlessLoop
            while (true) func();
        })
    {
    }
};

namespace
{
void
try_open_overlay(const std::uint32_t key, triggers_array& triggers)
{
    auto&       cur_trigger = triggers.by_key(key);
    const auto& inv_trigger = triggers.by_key_inv(key);

    cur_trigger.open_tp = system_clock::now();

    if (cur_trigger.open_tp - inv_trigger.open_tp > milliseconds(500))
        return;

    printf("Posting open intent...\n");
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
            "am start-foreground-service "
            "ru.n08i40k.poco.triggers/"
            ".service.OverlayService",
            nullptr);
        _exit(127);
    }

    waitpid(pid, nullptr, 0);
}

void
try_close_overlay(const std::uint32_t key, triggers_array& triggers)
{
    auto&       cur_trigger = triggers.by_key(key);
    const auto& inv_trigger = triggers.by_key_inv(key);

    cur_trigger.close_tp = system_clock::now();

    if (cur_trigger.close_tp - inv_trigger.close_tp > milliseconds(1000))
        return;

    printf("Posting close intent...\n");
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
            "am "
            " broadcast"
            " -a"
            " ru.n08i40k.poco.triggers.intent.TRIGGERS_CLOSED",
            nullptr);
        _exit(127);
    }

    waitpid(pid, nullptr, 0);
}

struct trigger_click_ev {
    trigger_index index;

    bool pressed;
    bool opposite_pressed;

    bool no_taps;

    std::int32_t slot;
};

void
on_trigger_click(
    const std::int32_t     virt_fd,
    trigger_data&          trigger,
    const trigger_click_ev event)
{
    if (!(trigger.ev_settings.enabled || trigger.pressed))
        return;

    // овощ на clion eap.
    // ReSharper disable once CppDFANotInitializedField
    trigger.pressed = event.pressed;

    const touch_event touch_ev{ .index          = event.index,
                                .pressed        = event.pressed,
                                .x              = trigger.ev_settings.x,
                                .y              = trigger.ev_settings.y,
                                .no_screen_taps = event.no_taps,
                                .opposite_trigger_pressed =
                                    event.opposite_pressed,
                                .current_slot = event.slot };

    if (const auto evs = emulate_touch(touch_ev);
        write(virt_fd, &*evs.cbegin(), sizeof(evs[0]) * evs.size()) < 0)
        perror("Failed to write event to virtual device");
}

std::vector<input_event>
read_until_syn(const std::int32_t fd)
{
    std::vector<input_event> events;
    events.reserve(8);

    input_event ev{};
    memset(&ev, 0xFF, sizeof(ev));

    while (ev.type != EV_SYN || ev.code != SYN_REPORT) {
        if (read(fd, &ev, sizeof(ev)) != sizeof(ev))
            continue;

        events.emplace_back(ev);
    }

    return events;
}

void
hello()
{
    const auto  epoch = time(nullptr);
    const auto* time  = localtime(&epoch);

    std::array<char, 60> time_buf{};
    strftime(time_buf.data(), time_buf.size(), "%d-%m-%Y %H:%M:%S", time);

    LOG_ANDROID("Startup...");

    printf("Startup at %s...\n", time_buf.data());
    fflush(stdout);
}
} // namespace

int    // исключение тут невозможно, это из-за switch-case.
main() // NOLINT(*-exception-escape)
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

    triggers_array triggers{
#ifndef NDEBUG
        trigger_data{
                     .pressed  = false,
                     .open_tp  = system_clock::now(),
                     .close_tp = system_clock::now(),
                     .ev_settings =
                     trigger_settings{ .enabled = true, .x = 5400, .y = 17000 } },
        trigger_data{
                     .pressed  = false,
                     .open_tp  = system_clock::now(),
                     .close_tp = system_clock::now(),
                     .ev_settings =
                     trigger_settings{ .enabled = true, .x = 5400, .y = 7000 } },
#endif
    };

    __s32 slot{};
    bool  no_taps{ true };

    const auto on_server_message = [&](const client_message& message) {
        printf("New triggers settings!\n");
        fflush(stdout);

        const std::lock_guard lock{ mtx };

        triggers[UPPER_TRIGGER].ev_settings = message.upper;
        triggers[LOWER_TRIGGER].ev_settings = message.lower;
    };

    const auto on_virt_cycle = [&] {
        input_event ev{};

        if (read(virt.read_fd(), &ev, sizeof(ev)) != sizeof(ev))
            return;

        const std::lock_guard lock{ mtx };

        if (ev.type == EV_ABS && ev.code == ABS_MT_SLOT)
            slot = ev.value;
    };

    const auto on_xm_ev = [&](const std::pair<std::uint16_t, bool>& event) {
        const auto& [key, pressed] = event;

        const std::lock_guard lock{ mtx };

        if (key == UPPER_CLICK_KEY || key == LOWER_CLICK_KEY) {
            const trigger_click_ev click_ev{
                .index = key == UPPER_CLICK_KEY ? UPPER_TRIGGER : LOWER_TRIGGER,
                .pressed          = pressed,
                .opposite_pressed = triggers.by_key_inv(key).pressed,
                .no_taps          = no_taps,
                .slot             = slot
            };

            on_trigger_click(virt.fd(), triggers.by_key(key), click_ev);

            return;
        }

        if (!pressed) {
            switch (key) {
            case UPPER_ENABLE_KEY:
            case LOWER_ENABLE_KEY : try_open_overlay(key, triggers); break;
            case UPPER_DISABLE_KEY:
            case LOWER_DISABLE_KEY: try_close_overlay(key, triggers); break;
            default               : break;
            }
        }
    };

    auto           server_thread = server.start_thread(on_server_message);
    endless_thread virt_thread(on_virt_cycle);
    auto           xm_thread = xm.start_work_thread(on_xm_ev);

    std::int32_t screen_slot{};

    // screen events
    // ReSharper disable once CppDFAEndlessLoop
    while (true) {
        auto evs = read_until_syn(fts.fd());

        const std::lock_guard lock{ mtx };

        if (screen_slot != slot
            && (evs[0].type != EV_ABS || evs[0].code != ABS_MT_SLOT)) {
            input_event slot_ev{};
            slot_ev.time  = timeval{ .tv_sec  = evs[0].time.tv_sec,
                                     .tv_usec = evs[0].time.tv_usec - 10 };
            slot_ev.type  = EV_ABS;
            slot_ev.code  = ABS_MT_SLOT;
            slot_ev.value = screen_slot;

            evs.insert(evs.begin(), slot_ev);
        }

        for (auto ev_it = evs.begin(); ev_it != evs.end();) {
            switch (auto& [_, type, code, value] = *ev_it; type) {
            case EV_ABS: {
                if (code == ABS_MT_TRACKING_ID && value != 0xFFFFFFFF)
                    value = get_tracking_id();

                if (code == ABS_MT_SLOT)
                    screen_slot = value;

                    break;
                }
                case EV_KEY: {
                    if (code == BTN_TOUCH || code == BTN_TOOL_FINGER) {
                        no_taps = value == 0;
                break;
            }
            case EV_KEY: {
                if (code == BTN_TOUCH || code == BTN_TOOL_FINGER) {
                    no_taps = value == 0;

                    // если прекращено нажатие на экран, а триггеры зажаты
                    if (triggers.any_pressed()) {
                        ev_it = evs.erase(ev_it);
                        continue;
                    }
                }

                break;
            }
            default: break;
            }

            ++ev_it;
        }

        if (write(virt.fd(), &*evs.cbegin(), sizeof(evs[0]) * evs.size()) < 0)
            perror("Failed to write event to virtual device");
    }

    // ReSharper disable CppDFAUnreachableCode
    server_thread.join();
    virt_thread.join();
    xm_thread.join();

    return EXIT_SUCCESS;
    // ReSharper restore CppDFAUnreachableCode
}
