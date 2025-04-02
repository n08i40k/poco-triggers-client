#define BIONIC_IOCTL_NO_SIGNEDNESS_OVERLOAD

#include <android/log.h>
#include <arpa/inet.h>
#include <array>
#include <cstdio>
#include <dirent.h>
#include <fcntl.h>
#include <functional>
#include <linux/input.h>
#include <linux/uinput.h>
#include <mutex>
#include <netinet/in.h>
#include <sys/socket.h>
#include <thread>
#include <unistd.h>
#include <queue>

#define LOG_ANDROID(...)                                                       \
    ((void)__android_log_print(ANDROID_LOG_INFO, "TriggersDaemon", __VA_ARGS__))

namespace
{
const auto* UDEV_NAME = "triggers_daemon";

constexpr __s32 SLOT_MAX       = 9;
constexpr __s32 TOTAL_SLOT_MAX = 11;

constexpr __s32 SLOT_COUNT       = SLOT_MAX + 1;
constexpr __s32 TOTAL_SLOT_COUNT = TOTAL_SLOT_MAX + 1;

int         real_fd   = -1;
int         virt_fd   = -1;
int         server_fd = -1;
sockaddr_in address{};

int
find_device(const char* name)
{
    auto* const dir = opendir("/dev/input");

    if (dir == nullptr) {
        perror("Failed to open /dev/input/");
        return -1;
    }

    int     fd = -1;
    dirent* entry;

    while ((entry = readdir(dir)) != nullptr) {
        if (strncmp(entry->d_name, "event", 5) != 0)
            continue;

        std::array<char, 256> path{};
        snprintf(path.data(), path.size(), "/dev/input/%s", entry->d_name);

        int _fd = open(path.data(), O_RDONLY);
        if (_fd < 0)
            continue;

        std::array<char, 256> _name{};
        if (ioctl(_fd, EVIOCGNAME(sizeof(_name) - 1), _name.data()) < 0) {
            close(_fd);
            continue;
        }

        if (strcmp(name, _name.data()) != 0) {
            close(_fd);
            continue;
        }

        fd = _fd;
        break;
    }

    closedir(dir);

    return fd;
}

void
open_device()
{
    const int fd = find_device("fts");

    if (fd < 0) {
        perror("Failed to open input device");
        return;
    }

    if (ioctl(fd, EVIOCGRAB, (void*)1) < 0) {
        perror("Не удалось захватить устройство");
        close(fd);
    }

    real_fd = fd;
}

void
cleanup_device()
{
    close(real_fd);
    real_fd = -1;
}

void
create_udevice()
{
    const int fd = open("/dev/uinput", O_RDWR | O_NONBLOCK);

    if (fd < 0) {
        perror("Failed to open uinput device");
        return;
    }

    if ((ioctl(fd, UI_SET_EVBIT, EV_KEY) | ioctl(fd, UI_SET_EVBIT, EV_ABS))
        < 0) {
        perror("Failed to set event bits for udevice");
        close(fd);
        return;
    }

    if (ioctl(fd, UI_SET_PROPBIT, INPUT_PROP_DIRECT) < 0) {
        perror("Failed to set prop bit for udevice");
        close(fd);
        return;
    }

    constexpr std::array keys{
        KEY_W,    KEY_E,      KEY_O,   KEY_LEFTBRACE,   KEY_RIGHTBRACE,
        KEY_S,    KEY_F,      KEY_L,   KEY_Z,           KEY_C,
        KEY_V,    KEY_M,      KEY_F1,  KEY_F2,          KEY_F3,
        KEY_F4,   KEY_F5,     KEY_UP,  KEY_LEFT,        KEY_RIGHT,
        KEY_DOWN, KEY_WAKEUP, KEY_WWW, BTN_TOOL_FINGER, BTN_TOUCH,
        KEY_GOTO, 0x152
    };

    for (const auto key : keys) {
        if (ioctl(fd, UI_SET_KEYBIT, key) < 0) {
            perror("Failed to set key for udevice");
            close(fd);
            return;
        }
    }

    if ((ioctl(fd, UI_SET_ABSBIT, ABS_MT_SLOT)
         | ioctl(fd, UI_SET_ABSBIT, ABS_MT_TOUCH_MAJOR)
         | ioctl(fd, UI_SET_ABSBIT, ABS_MT_TOUCH_MINOR)
         | ioctl(fd, UI_SET_ABSBIT, ABS_MT_WIDTH_MAJOR)
         | ioctl(fd, UI_SET_ABSBIT, ABS_MT_WIDTH_MINOR)
         | ioctl(fd, UI_SET_ABSBIT, ABS_MT_ORIENTATION)
         | ioctl(fd, UI_SET_ABSBIT, ABS_MT_POSITION_X)
         | ioctl(fd, UI_SET_ABSBIT, ABS_MT_POSITION_Y)
         | ioctl(fd, UI_SET_ABSBIT, ABS_MT_TRACKING_ID)
         | ioctl(fd, UI_SET_ABSBIT, ABS_MT_DISTANCE))
        < 0) {
        perror("Failed to set abs bits for udevice");
        close(fd);
        return;
    }

    uinput_user_dev uidev = {};
    snprintf(uidev.name, sizeof(uidev.name), "%s", UDEV_NAME);
    uidev.id.bustype = BUS_VIRTUAL;
    uidev.id.vendor  = 0x1234;
    uidev.id.product = 0x5678;
    uidev.id.version = 1;

    // ABS_MT_SLOT: [0, 9]
    uidev.absmin[ABS_MT_SLOT] = 0;
    // additional two slots for triggers
    uidev.absmax[ABS_MT_SLOT] = SLOT_MAX + 2;

    // ABS_MT_TOUCH_MAJOR: [0, 10800]
    uidev.absmin[ABS_MT_TOUCH_MAJOR] = 0;
    uidev.absmax[ABS_MT_TOUCH_MAJOR] = 10800;

    // ABS_MT_TOUCH_MINOR: [0, 24000]
    uidev.absmin[ABS_MT_TOUCH_MINOR] = 0;
    uidev.absmax[ABS_MT_TOUCH_MINOR] = 24000;

    // ABS_MT_WIDTH_MAJOR: [0, 127]
    uidev.absmin[ABS_MT_WIDTH_MAJOR] = 0;
    uidev.absmax[ABS_MT_WIDTH_MAJOR] = 127;

    // ABS_MT_WIDTH_MINOR: [0, 127]
    uidev.absmin[ABS_MT_WIDTH_MINOR] = 0;
    uidev.absmax[ABS_MT_WIDTH_MINOR] = 127;

    // ABS_MT_ORIENTATION: [-90, 90]
    uidev.absmin[ABS_MT_ORIENTATION] = -90;
    uidev.absmax[ABS_MT_ORIENTATION] = 90;

    // ABS_MT_POSITION_X: [0, 10799]
    uidev.absmin[ABS_MT_POSITION_X] = 0;
    uidev.absmax[ABS_MT_POSITION_X] = 10799;

    // ABS_MT_POSITION_Y: [0, 23999]
    uidev.absmin[ABS_MT_POSITION_Y] = 0;
    uidev.absmax[ABS_MT_POSITION_Y] = 23999;

    // ABS_MT_TRACKING_ID: [0, 65535]
    uidev.absmin[ABS_MT_TRACKING_ID] = 0;
    uidev.absmax[ABS_MT_TRACKING_ID] = 65535;

    // ABS_MT_DISTANCE: [0, 127]
    uidev.absmin[ABS_MT_DISTANCE] = 0;
    uidev.absmax[ABS_MT_DISTANCE] = 127;

    if (write(fd, &uidev, sizeof(uidev)) < 0) {
        perror("Failed to setup uinput device");
        close(fd);
        return;
    }

    if (ioctl(fd, UI_DEV_CREATE) < 0) {
        perror("Failed to create uinput device");
        close(fd);
    }

    virt_fd = fd;
}

void
cleanup_udevice()
{
    if (ioctl(virt_fd, UI_DEV_DESTROY) < 0)
        perror("Failed to destroy udevice");

    close(virt_fd);

    virt_fd = -1;
}

void
create_server()
{
    const int fd = socket(AF_INET, SOCK_STREAM, 0);

    if (fd == 0)
        perror("Failed to create socket");

    timeval tv;
    tv.tv_sec  = 5;
    tv.tv_usec = 0;
    setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, &tv, sizeof(tv));

    if (constexpr int opt = 1;
        setsockopt(fd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt))) {
        perror("Failed to set socket option");
        close(fd);
        return;
    }

    address.sin_family      = AF_INET;
    address.sin_addr.s_addr = INADDR_ANY;
    address.sin_port        = htons(5555);

    if (bind(fd, reinterpret_cast<sockaddr*>(&address), sizeof(address)) < 0) {
        perror("Failed to bind socket");
        close(fd);
        return;
    }

    if (listen(fd, SOMAXCONN) < 0) {
        perror("Failed to listen on socket");
        close(fd);
        return;
    }

    server_fd = fd;
}

void
cleanup_server()
{
    close(server_fd);
    server_fd = -1;
}

struct client_message {
    __u16 trigger;
    __u16 touch;
    __s32 x;
    __s32 y;
};

void
listen_server(const std::function<void(client_message)>& on_message)
{
    auto* const addr     = reinterpret_cast<struct sockaddr*>(&address);
    socklen_t   addr_len = sizeof(address);

    while (true) {
        const int sock = accept(server_fd, addr, &addr_len);

        if (sock < 0) {
            if (server_fd == 0)
                return;

            if (errno == EAGAIN)
                continue;

            perror("Failed to accept connection");
            continue;
        }

        printf("New client!\n");
        fflush(stdout);

        client_message message{};

        while (true) {
            const auto n = read(sock, &message, sizeof(message));

            if (n == 0) {
                printf("Client disconnected!\n");
                fflush(stdout);

                break;
            }

            if (n != sizeof(message))
                continue;

            on_message(message);
        }

        close(sock);
    }
}

void
cleanup()
{
    if (server_fd > 0)
        cleanup_server();

    if (virt_fd > 0)
        cleanup_udevice();

    if (real_fd > 0)
        cleanup_device();
}
} // namespace

int
main()
{
    LOG_ANDROID("Hello!");
    printf("Hello, World!\n");
    fflush(stdout);

    open_device();
    if (real_fd < 0)
        return 1;

    create_udevice();
    if (virt_fd < 0) {
        cleanup();
        return 1;
    }

    create_server();
    if (server_fd < 0) {
        cleanup();
        return 1;
    }

    std::mutex mtx{};

    bool no_triggers{ true };
    bool no_taps{ true };

    std::array<bool, 2> triggers{ false };

    input_event ev{};
    printf("sizeof(input_event): %lu\n", sizeof(input_event));
    fflush(stdout);
    __s32 slot{};

    std::thread server_thread([&] {
        listen_server([&](const client_message message) {
            printf(
                "Trigger: %d [%s]\n",
                message.trigger,
                message.trigger ? "UPPER" : "LOWER");
            fflush(stdout);

            if (message.trigger > 1)
                return;

            printf("Pressed: %d\n", message.touch);

            if (message.touch > 0) {
                printf("X: %d\n", message.x);
                printf("Y: %d\n", message.y);
            }

            fflush(stdout);

            const std::lock_guard lock{ mtx };

            triggers[message.trigger] = message.touch > 0;
            no_triggers               = !(triggers[0] || triggers[1]);

            std::queue<input_event> ev_queue{};

            input_event virt_ev{};

            // set slot
            gettimeofday(&virt_ev.time, nullptr);
            virt_ev.type  = EV_ABS;
            virt_ev.code  = ABS_MT_SLOT;
            virt_ev.value = SLOT_COUNT + message.trigger;
            ev_queue.push(virt_ev);

            if (message.touch) {
                // set touch id
                gettimeofday(&virt_ev.time, nullptr);
                virt_ev.type  = EV_ABS;
                virt_ev.code  = ABS_MT_TRACKING_ID;
                virt_ev.value = message.trigger;
                ev_queue.push(virt_ev);

                if (no_taps) {
                    // set touch
                    gettimeofday(&virt_ev.time, nullptr);
                    virt_ev.type  = EV_KEY;
                    virt_ev.code  = BTN_TOUCH;
                    virt_ev.value = 1;
                    ev_queue.push(virt_ev);

                    // set finger
                    gettimeofday(&virt_ev.time, nullptr);
                    virt_ev.type  = EV_KEY;
                    virt_ev.code  = BTN_TOOL_FINGER;
                    virt_ev.value = 1;
                    ev_queue.push(virt_ev);
                }

                // set x
                gettimeofday(&virt_ev.time, nullptr);
                virt_ev.type  = EV_ABS;
                virt_ev.code  = ABS_MT_POSITION_X;
                virt_ev.value = message.x;
                ev_queue.push(virt_ev);

                // set y
                gettimeofday(&virt_ev.time, nullptr);
                virt_ev.type  = EV_ABS;
                virt_ev.code  = ABS_MT_POSITION_Y;
                virt_ev.value = message.y;
                ev_queue.push(virt_ev);
            } else {
                // set touch id
                gettimeofday(&virt_ev.time, nullptr);
                virt_ev.type  = EV_ABS;
                virt_ev.code  = ABS_MT_TRACKING_ID;
                virt_ev.value = -1;
                ev_queue.push(virt_ev);

                if (no_taps && !triggers[message.trigger ? 0 : 1]) {
                    // set touch
                    gettimeofday(&virt_ev.time, nullptr);
                    virt_ev.type  = EV_KEY;
                    virt_ev.code  = BTN_TOUCH;
                    virt_ev.value = 0;
                    ev_queue.push(virt_ev);

                    // set finger
                    gettimeofday(&virt_ev.time, nullptr);
                    virt_ev.type  = EV_KEY;
                    virt_ev.code  = BTN_TOOL_FINGER;
                    virt_ev.value = 0;
                    ev_queue.push(virt_ev);
                }
            }

            // flush
            gettimeofday(&virt_ev.time, nullptr);
            virt_ev.type  = EV_SYN;
            virt_ev.code  = SYN_REPORT;
            virt_ev.value = 0;
            ev_queue.push(virt_ev);

            // if (message.touch > 0) {
            // set slot back
            gettimeofday(&virt_ev.time, nullptr);
            virt_ev.type  = EV_ABS;
            virt_ev.code  = ABS_MT_SLOT;
            virt_ev.value = slot;
            ev_queue.push(virt_ev);

            // flush
            gettimeofday(&virt_ev.time, nullptr);
            virt_ev.type  = EV_SYN;
            virt_ev.code  = SYN_REPORT;
            virt_ev.value = 0;
            ev_queue.push(virt_ev);

            do {
                const auto send_ev = ev_queue.front();

                if (write(virt_fd, &send_ev, sizeof(send_ev)) < 0)
                    perror("Failed to write event to uinput device");

                ev_queue.pop();
            } while (!ev_queue.empty());
        });
    });

    // all events
    std::thread virt_thread([&] {
        // ReSharper disable once CppDFAEndlessLoop
        while (true) {
            if (read(virt_fd, &ev, sizeof(ev)) != sizeof(ev))
                continue;

            const std::lock_guard lock{ mtx };

            if (ev.type == EV_ABS && ev.code == ABS_MT_SLOT) {
                slot = ev.value;

                continue;
            }

            if (slot > SLOT_MAX && ev.type == EV_KEY
                && (ev.code == BTN_TOUCH || ev.code == BTN_TOOL_FINGER)) {
                no_triggers = ev.value == 0;
            }
        }
    });

    // screen events
    while (true) {
        if (read(real_fd, &ev, sizeof(ev)) != sizeof(ev))
            continue;

        const std::lock_guard lock{ mtx };

        if (ev.type == EV_KEY
            && (ev.code == BTN_TOUCH || ev.code == BTN_TOOL_FINGER)) {
            no_taps = ev.value == 0;

            // если прекращено нажатие на экран, а триггеры не зажаты
            if (!no_triggers)
                continue;
        }

        if (write(virt_fd, &ev, sizeof(ev)) < 0)
            perror("Failed to write event to uinput device");
    }

    cleanup();

    server_thread.join();
    virt_thread.join();

    return 0;
}
