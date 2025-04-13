//
// Created by n08i40k on 03.04.2025.
//

#ifndef CONSTANTS_H
#define CONSTANTS_H
#include <cstdint>

// udev names
constexpr const auto* FTS_UDEV_NAME = "fts";
constexpr const auto* XM_UDEV_NAME  = "xm_gamekey";

constexpr const auto *VIRTUAL_DEVICE_NAME = "aosp_triggers_daemon";

// udev touch slots
constexpr std::int32_t FTS_SLOT_COUNT = 10;
constexpr std::int32_t FTS_SLOT_MAX   = FTS_SLOT_COUNT - 1;

constexpr std::int32_t VIRTUAL_SLOT_COUNT = 12;
constexpr std::int32_t VIRTUAL_SLOT_MAX = VIRTUAL_SLOT_COUNT - 1;

#endif // CONSTANTS_H
