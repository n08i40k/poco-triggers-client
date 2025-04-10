//
// Created by n08i40k on 03.04.2025.
//

#ifndef TOUCH_EMULATOR_H
#define TOUCH_EMULATOR_H
#include <cstdint>
#include <linux/input.h>
#include <vector>

struct touch_event {
    /**
     * Index of trigger.
     *
     * 0 - Upper.
     * 1 - Lower.
     */
    std::uint8_t index;
    /**
     * Is trigger has been pressed.
     */
    bool pressed;

    /**
     * X coordinate in default orientation.
     */
    std::int32_t x;
    /**
     * Y coordinate in default orientation.
     */
    std::int32_t y;

    /**
     * Are there any screen taps.
     */
    bool no_screen_taps;

    /**
     * Is the opposite trigger pressed.
     */
    bool opposite_trigger_pressed;

    /**
     * Current udev MT_SLOT.
     */
    std::int32_t current_slot;
};

/**
 * Generate a list of input_event simulating the start or end of a press.
 * @param event Parameters of generated events.
 * @return List of generated input_event.
 */
std::vector<input_event>
emulate_touch(const touch_event& event);

#endif // TOUCH_EMULATOR_H
