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

    input_event virt_ev{};

    // set slot
    gettimeofday(&virt_ev.time, nullptr);
    virt_ev.type  = EV_ABS;
    virt_ev.code  = ABS_MT_SLOT;
    virt_ev.value = FTS_SLOT_COUNT + event.index;
    ev_queue.emplace_back(virt_ev);

    if (event.pressed) {
        // set touch id
        gettimeofday(&virt_ev.time, nullptr);
        virt_ev.type  = EV_ABS;
        virt_ev.code  = ABS_MT_TRACKING_ID;
        virt_ev.value = event.index;
        ev_queue.emplace_back(virt_ev);

        // notify system about tap if no any taps before
        if (event.no_screen_taps) {
            // set touch
            gettimeofday(&virt_ev.time, nullptr);
            virt_ev.type  = EV_KEY;
            virt_ev.code  = BTN_TOUCH;
            virt_ev.value = 1;
            ev_queue.emplace_back(virt_ev);

            // set finger
            gettimeofday(&virt_ev.time, nullptr);
            virt_ev.type  = EV_KEY;
            virt_ev.code  = BTN_TOOL_FINGER;
            virt_ev.value = 1;
            ev_queue.emplace_back(virt_ev);
        }

        // set x
        gettimeofday(&virt_ev.time, nullptr);
        virt_ev.type  = EV_ABS;
        virt_ev.code  = ABS_MT_POSITION_X;
        virt_ev.value = event.x;
        ev_queue.emplace_back(virt_ev);

        // set y
        gettimeofday(&virt_ev.time, nullptr);
        virt_ev.type  = EV_ABS;
        virt_ev.code  = ABS_MT_POSITION_Y;
        virt_ev.value = event.y;
        ev_queue.emplace_back(virt_ev);
    } else {
        // set touch id
        gettimeofday(&virt_ev.time, nullptr);
        virt_ev.type  = EV_ABS;
        virt_ev.code  = ABS_MT_TRACKING_ID;
        virt_ev.value = -1;
        ev_queue.emplace_back(virt_ev);

        // notify system about no more taps on screen
        if (event.no_screen_taps && !event.opposite_trigger_pressed) {
            // set touch
            gettimeofday(&virt_ev.time, nullptr);
            virt_ev.type  = EV_KEY;
            virt_ev.code  = BTN_TOUCH;
            virt_ev.value = 0;
            ev_queue.emplace_back(virt_ev);

            // set finger
            gettimeofday(&virt_ev.time, nullptr);
            virt_ev.type  = EV_KEY;
            virt_ev.code  = BTN_TOOL_FINGER;
            virt_ev.value = 0;
            ev_queue.emplace_back(virt_ev);
        }
    }

    // flush
    gettimeofday(&virt_ev.time, nullptr);
    virt_ev.type  = EV_SYN;
    virt_ev.code  = SYN_REPORT;
    virt_ev.value = 0;
    ev_queue.emplace_back(virt_ev);

    // set slot back
    gettimeofday(&virt_ev.time, nullptr);
    virt_ev.type  = EV_ABS;
    virt_ev.code  = ABS_MT_SLOT;
    virt_ev.value = event.current_slot;
    ev_queue.emplace_back(virt_ev);

    // flush
    gettimeofday(&virt_ev.time, nullptr);
    virt_ev.type  = EV_SYN;
    virt_ev.code  = SYN_REPORT;
    virt_ev.value = 0;
    ev_queue.emplace_back(virt_ev);

    return ev_queue;
}
