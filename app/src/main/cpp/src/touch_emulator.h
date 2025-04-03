//
// Created by n08i40k on 03.04.2025.
//

#ifndef TOUCH_EMULATOR_H
#define TOUCH_EMULATOR_H
#include <cstdint>

struct touch_event {
    std::uint8_t index;
    bool         pressed;

    std::int32_t x;
    std::int32_t y;

    bool no_screen_taps;
    bool another_trigger_pressed;

    std::int32_t current_slot;
};

void
emulate_touch(std::int32_t fd, const touch_event& event);

#endif // TOUCH_EMULATOR_H
