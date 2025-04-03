//
// Created by n08i40k on 03.04.2025.
//

#ifndef XM_WATCHER_H
#define XM_WATCHER_H

#include <functional>
#include <linux/input-event-codes.h>

constexpr std::uint16_t UPPER_CLICK_KEY = KEY_F1;
constexpr std::uint16_t LOWER_CLICK_KEY = KEY_F2;

constexpr std::uint16_t UPPER_ENABLE_KEY = KEY_F3;
constexpr std::uint16_t LOWER_ENABLE_KEY = KEY_F5;

constexpr std::uint16_t UPPER_DISABLE_KEY = KEY_F4;
constexpr std::uint16_t LOWER_DISABLE_KEY = KEY_F6;

class xm_watcher
{
    std::int32_t fd_{};

public:
    xm_watcher();

    ~xm_watcher();

    void
    work_thread(
        const std::function<void(std::pair<std::uint16_t, bool>&)>& on_event)
        const;

    [[nodiscard]]
    std::int32_t
    fd() const
    {
        return fd_;
    }
};

#endif // XM_WATCHER_H
