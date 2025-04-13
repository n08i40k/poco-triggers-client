//
// Created by n08i40k on 03.04.2025.
//

#include "touch_emulator.h"

#include "constants.h"

#include <linux/input.h>
#include <queue>
#include <unistd.h>

std::vector<input_event>
emulate_touch(const touch_event& event)
{
    std::vector<input_event> ev_queue{};
    ev_queue.reserve(9);

    input_event ev{};

    // set slot
    gettimeofday(&ev.time, nullptr);
    ev.type = EV_ABS;
    ev.code = ABS_MT_SLOT;
    ev.value = FTS_SLOT_COUNT + event.index;
    ev_queue.emplace_back(ev);

    if (event.pressed) {
        // set touch id
        gettimeofday(&ev.time, nullptr);
        ev.type = EV_ABS;
        ev.code = ABS_MT_TRACKING_ID;
        ev.value = get_tracking_id();
        ev_queue.emplace_back(ev);

        // notify system about tap if no any taps before
        if (event.no_screen_taps) {
            // set touch
            gettimeofday(&ev.time, nullptr);
            ev.type = EV_KEY;
            ev.code = BTN_TOUCH;
            ev.value = 1;
            ev_queue.emplace_back(ev);

            // set finger
            gettimeofday(&ev.time, nullptr);
            ev.type = EV_KEY;
            ev.code = BTN_TOOL_FINGER;
            ev.value = 1;
            ev_queue.emplace_back(ev);
        }

        // set x
        gettimeofday(&ev.time, nullptr);
        ev.type = EV_ABS;
        ev.code = ABS_MT_POSITION_X;
        ev.value = event.x;
        ev_queue.emplace_back(ev);

        // set y
        gettimeofday(&ev.time, nullptr);
        ev.type = EV_ABS;
        ev.code = ABS_MT_POSITION_Y;
        ev.value = event.y;
        ev_queue.emplace_back(ev);
    } else {
        // set touch id
        gettimeofday(&ev.time, nullptr);
        ev.type = EV_ABS;
        ev.code = ABS_MT_TRACKING_ID;
        ev.value = -1;
        ev_queue.emplace_back(ev);

        // notify system about no more taps on screen
        if (event.no_screen_taps && !event.opposite_trigger_pressed) {
            // set touch
            gettimeofday(&ev.time, nullptr);
            ev.type = EV_KEY;
            ev.code = BTN_TOUCH;
            ev.value = 0;
            ev_queue.emplace_back(ev);

            // set finger
            gettimeofday(&ev.time, nullptr);
            ev.type = EV_KEY;
            ev.code = BTN_TOOL_FINGER;
            ev.value = 0;
            ev_queue.emplace_back(ev);
        }
    }

    // flush
    gettimeofday(&ev.time, nullptr);
    ev.type = EV_SYN;
    ev.code = SYN_REPORT;
    ev.value = 0;
    ev_queue.emplace_back(ev);

    // set slot back
    gettimeofday(&ev.time, nullptr);
    ev.type = EV_ABS;
    ev.code = ABS_MT_SLOT;
    ev.value = event.current_slot;
    ev_queue.emplace_back(ev);

    // flush
    gettimeofday(&ev.time, nullptr);
    ev.type = EV_SYN;
    ev.code = SYN_REPORT;
    ev.value = 0;
    ev_queue.emplace_back(ev);

    return ev_queue;
}

std::int32_t
get_tracking_id() {
    // XD nice generator
    static std::int32_t tracking_id{};
    return tracking_id++;
}
