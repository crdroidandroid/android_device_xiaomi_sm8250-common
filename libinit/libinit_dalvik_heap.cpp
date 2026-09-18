/*
 * Copyright (C) 2021-2025 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#include "include/libinit_dalvik_heap.h"

#include "include/libinit_utils.h"

#include <sys/sysinfo.h>
#include <cstdint>
#include <string>

#define GB(b) (b * 1024ull * 1024 * 1024)

static const std::string kHeapStartSizeProp = "dalvik.vm.heapstartsize";
static const std::string kHeapGrowthLimitProp = "dalvik.vm.heapgrowthlimit";
static const std::string kHeapSizeProp = "dalvik.vm.heapsize";
static const std::string kHeapMinFreeProp = "dalvik.vm.heapminfree";
static const std::string kHeapMaxFreeProp = "dalvik.vm.heapmaxfree";
static const std::string kHeapTargetUtilizationProp = "dalvik.vm.heaptargetutilization";

struct dalvik_heap_info {
    std::string heapstartsize;
    std::string heapgrowthlimit;
    std::string heapsize;
    std::string heapminfree;
    std::string heapmaxfree;
    std::string heaptargetutilization;
};

// Keep the 6/8 GB profiles aligned with frameworks/native/build/phone-xhdpi-*
// defaults. Use detected RAM so alioth and aliothin share the same product image.
static const dalvik_heap_info dalvik_heap_info_8192 = {
        .heapstartsize = "16m",
        .heapgrowthlimit = "384m",
        .heapsize = "512m",
        .heapminfree = "8m",
        .heapmaxfree = "64m",
        .heaptargetutilization = "0.5",
};

static const dalvik_heap_info dalvik_heap_info_6144 = {
        .heapstartsize = "16m",
        .heapgrowthlimit = "256m",
        .heapsize = "512m",
        .heapminfree = "8m",
        .heapmaxfree = "32m",
        .heaptargetutilization = "0.5",
};

static const dalvik_heap_info dalvik_heap_info_4096 = {
        .heapstartsize = "8m",
        .heapgrowthlimit = "256m",
        .heapsize = "512m",
        .heapminfree = "8m",
        .heapmaxfree = "16m",
        .heaptargetutilization = "0.6",
};

static const dalvik_heap_info dalvik_heap_info_2048 = {
        .heapstartsize = "8m",
        .heapgrowthlimit = "192m",
        .heapsize = "512m",
        .heapminfree = "512k",
        .heapmaxfree = "8m",
        .heaptargetutilization = "0.75",
};

void set_dalvik_heap() {
    struct sysinfo sys {};
    const dalvik_heap_info* dhi;

    // Keep the inherited product defaults if RAM detection fails.
    if (sysinfo(&sys) != 0 || sys.mem_unit == 0 || sys.totalram == 0) {
        return;
    }
    const uint64_t total_ram = static_cast<uint64_t>(sys.totalram) * sys.mem_unit;

    // Kernel and firmware reservations leave less than the marketed RAM size.
    if (total_ram > GB(7)) {
        dhi = &dalvik_heap_info_8192;
    } else if (total_ram > GB(5)) {
        dhi = &dalvik_heap_info_6144;
    } else if (total_ram > GB(3)) {
        dhi = &dalvik_heap_info_4096;
    } else {
        dhi = &dalvik_heap_info_2048;
    }

    property_override(kHeapStartSizeProp, dhi->heapstartsize);
    property_override(kHeapGrowthLimitProp, dhi->heapgrowthlimit);
    property_override(kHeapSizeProp, dhi->heapsize);
    property_override(kHeapMinFreeProp, dhi->heapminfree);
    property_override(kHeapMaxFreeProp, dhi->heapmaxfree);
    property_override(kHeapTargetUtilizationProp, dhi->heaptargetutilization);

    // These differ between this tree's 6 GB and 8 GB ART profiles. The common
    // JIT/GC defaults still come from the inherited 6 GB product configuration.
    if (total_ram > GB(5)) {
        const bool is_8gb = total_ram > GB(7);
        const std::string madvise_size = is_8gb ? "157286400" : "104857600";
        property_override("dalvik.vm.madvise.vdexfile.size", madvise_size);
        property_override("dalvik.vm.madvise.odexfile.size", madvise_size);
        property_override("dalvik.vm.usap_pool_size_max", is_8gb ? "3" : "2");
    }
}
