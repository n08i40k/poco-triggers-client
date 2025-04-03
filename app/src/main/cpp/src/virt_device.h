//
// Created by n08i40k on 03.04.2025.
//

#ifndef VIRT_DEVICE_H
#define VIRT_DEVICE_H

class virt_device
{
    int fd_{};

public:
    virt_device();
    ~virt_device();

    [[nodiscard]]
    int
    fd() const
    {
        return fd_;
    }
};

#endif // VIRT_DEVICE_H
