//
// Created by n08i40k on 03.04.2025.
//

#include "fts_lock.h"

#define BIONIC_IOCTL_NO_SIGNEDNESS_OVERLOAD

#include "device_utils.h"

#include <cstdio>
#include <linux/input.h>
#include <unistd.h>

fts_lock::fts_lock()
    : fd_(find_device("fts"))
{
    if (fd_ < 0) {
        perror("Failed to open input device");
        return;
    }

    if (ioctl(fd_, EVIOCGRAB, reinterpret_cast<void*>(1)) < 0) {
        perror("Failed to lock fts device");
        close(fd_);
        fd_ = -1;
    }
}

fts_lock::~fts_lock()
{
    if (fd_ > 0)
        close(fd_);
}
