//
// Created by n08i40k on 03.04.2025.
//

#include "virt_device.h"

#define BIONIC_IOCTL_NO_SIGNEDNESS_OVERLOAD

#include "constants.h"

#include <array>
#include <cstdio>
#include <fcntl.h>
#include <linux/input.h>
#include <linux/uinput.h>
#include <unistd.h>

virt_device::virt_device()
    : fd_(open("/dev/uinput", O_RDWR | O_NONBLOCK))
{
    if (fd_ < 0) {
        perror("Failed to open uinput device");
        return;
    }

    if ((ioctl(fd_, UI_SET_EVBIT, EV_KEY) | ioctl(fd_, UI_SET_EVBIT, EV_ABS))
        < 0) {
        perror("Failed to set event bits for udevice");
        close(fd_);
        fd_ = -1;
        return;
    }

    if (ioctl(fd_, UI_SET_PROPBIT, INPUT_PROP_DIRECT) < 0) {
        perror("Failed to set prop bit for udevice");
        close(fd_);
        fd_ = -1;
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
        if (ioctl(fd_, UI_SET_KEYBIT, key) >= 0)
            continue;

        perror("Failed to set key for udevice");
        close(fd_);
        fd_ = -1;
        return;
    }

    if ((ioctl(fd_, UI_SET_ABSBIT, ABS_MT_SLOT)
         | ioctl(fd_, UI_SET_ABSBIT, ABS_MT_TOUCH_MAJOR)
         | ioctl(fd_, UI_SET_ABSBIT, ABS_MT_TOUCH_MINOR)
         | ioctl(fd_, UI_SET_ABSBIT, ABS_MT_WIDTH_MAJOR)
         | ioctl(fd_, UI_SET_ABSBIT, ABS_MT_WIDTH_MINOR)
         | ioctl(fd_, UI_SET_ABSBIT, ABS_MT_ORIENTATION)
         | ioctl(fd_, UI_SET_ABSBIT, ABS_MT_POSITION_X)
         | ioctl(fd_, UI_SET_ABSBIT, ABS_MT_POSITION_Y)
         | ioctl(fd_, UI_SET_ABSBIT, ABS_MT_TRACKING_ID)
         | ioctl(fd_, UI_SET_ABSBIT, ABS_MT_DISTANCE))
        < 0) {
        perror("Failed to set abs bits for udevice");
        close(fd_);
        fd_ = -1;
        return;
    }

    uinput_user_dev uidev = {};
    snprintf(uidev.name, sizeof(uidev.name), "%s", VIRT_UDEV_NAME);
    uidev.id.bustype = BUS_VIRTUAL;
    uidev.id.vendor  = 0x1234;
    uidev.id.product = 0x5678;
    uidev.id.version = 1;

    // ABS_MT_SLOT: [0, 9]
    uidev.absmin[ABS_MT_SLOT] = 0;
    uidev.absmax[ABS_MT_SLOT] = VIRT_SLOT_MAX;

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

    if (write(fd_, &uidev, sizeof(uidev)) < 0) {
        perror("Failed to setup uinput device");
        close(fd_);
        fd_ = -1;
        return;
    }

    if (ioctl(fd_, UI_DEV_CREATE) < 0) {
        perror("Failed to create uinput device");
        close(fd_);
        fd_ = -1;
    }
}

virt_device::~virt_device()
{
    if (fd_ > 0) {
        if (ioctl(fd_, UI_DEV_DESTROY) < 0)
            perror("Failed to destroy udevice");

        close(fd_);
    }
}
