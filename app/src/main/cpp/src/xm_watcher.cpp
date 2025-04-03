//
// Created by n08i40k on 03.04.2025.
//

#include "xm_watcher.h"

#define BIONIC_IOCTL_NO_SIGNEDNESS_OVERLOAD

#include "constants.h"
#include "device_utils.h"

#include <algorithm>
#include <linux/input.h>
#include <unistd.h>

xm_watcher::xm_watcher()
    : fd_(find_device(XM_UDEV_NAME))
{
    if (fd_ < 0) {
        perror("Failed to open xm input device");
        return;
    }
}

xm_watcher::~xm_watcher()
{
    if (fd_ > 0)
        close(fd_);
}

void
xm_watcher::work_thread(
    const std::function<void(std::pair<std::uint16_t, bool>&)>& on_event) const
{
    input_event key_ev{};
    input_event ev{};

    // ReSharper disable once CppDFAEndlessLoop
    while (true) {
        if (read(fd_, &ev, sizeof(ev)) != sizeof(ev))
            continue;

        if (ev.type == EV_KEY) {
            key_ev = ev;
            continue;
        }

        static constexpr std::array whitelisted_keys = {
            UPPER_CLICK_KEY, UPPER_ENABLE_KEY, UPPER_DISABLE_KEY,
            LOWER_CLICK_KEY, LOWER_ENABLE_KEY, LOWER_DISABLE_KEY,
        };

        if (ev.type == EV_SYN && ev.code == SYN_REPORT) {
            if (!std::ranges::contains(whitelisted_keys, key_ev.code))
                continue;

            auto send_ev = std::pair(key_ev.code, key_ev.value == 1);
            on_event(send_ev);
        }
    }
}
