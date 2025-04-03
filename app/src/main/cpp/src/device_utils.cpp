//
// Created by n08i40k on 03.04.2025.
//

#include "device_utils.h"

#include <array>
#include <cstdio>
#include <cstring>
#include <dirent.h>
#include <fcntl.h>
#include <linux/input.h>
#include <unistd.h>

int
find_device(const char* name)
{
    auto* const dir = opendir("/dev/input");

    if (dir == nullptr) {
        perror("Failed to open /dev/input/");
        return -1;
    }

    int     fd = -1;
    dirent* entry{};

    while ((entry = readdir(dir)) != nullptr) {
        if (strncmp(entry->d_name, "event", 5) != 0)
            continue;

        std::array<char, 256> path{};
        snprintf(path.data(), path.size(), "/dev/input/%s", entry->d_name);

        const auto entry_fd = open(path.data(), O_RDONLY);
        if (entry_fd < 0)
            continue;

        std::array<char, 256> entry_name{};

        if (ioctl(
                entry_fd, EVIOCGNAME(sizeof(entry_name) - 1), entry_name.data())
            < 0) {
            close(entry_fd);
            continue;
        }

        if (strcmp(name, entry_name.data()) != 0) {
            close(entry_fd);
            continue;
        }

        fd = entry_fd;
        break;
    }

    closedir(dir);

    return fd;
}
