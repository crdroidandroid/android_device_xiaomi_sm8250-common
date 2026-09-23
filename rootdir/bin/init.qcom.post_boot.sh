#! /vendor/bin/sh

# Copyright (c) 2012-2013, 2016-2020, The Linux Foundation. All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#     * Redistributions of source code must retain the above copyright
#       notice, this list of conditions and the following disclaimer.
#     * Redistributions in binary form must reproduce the above copyright
#       notice, this list of conditions and the following disclaimer in the
#       documentation and/or other materials provided with the distribution.
#     * Neither the name of The Linux Foundation nor
#       the names of its contributors may be used to endorse or promote
#       products derived from this software without specific prior written
#       permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
# NON-INFRINGEMENT ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
# CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
# EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
# PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
# OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
# WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
# OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
# ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#

target=`getprop ro.board.platform`


# Read MemTotal without fixed-width assumptions or spawning cat/grep.
function read_mem_total() {
    MemTotal=
    while read -r key value unit; do
        if [ "$key" = "MemTotal:" ]; then
            MemTotal=$value
            break
        fi
    done < /proc/meminfo
    case "$MemTotal" in
        ''|*[!0-9]*) return 1 ;;
    esac
    [ "$MemTotal" -gt 0 ]
}

function configure_zram_parameters() {
    # Never change the compressor or reformat an initialized ZRAM device.
    [ -r /sys/block/zram0/disksize ] || return 0
    disksize=$(cat /sys/block/zram0/disksize) || return 1
    case "$disksize" in
        ''|*[!0-9]*) return 1 ;;
    esac
    [ "$disksize" -eq 0 ] || return 0

    read_mem_total || return 1

    # Preserve the existing capacity policy: 75% at <=2 GiB, otherwise
    # half the rounded RAM tier, capped at 4 GiB (3/4 GiB on 6/8 GiB phones).
    RamSizeGB=$((MemTotal / 1048576 + 1))
    if [ "$RamSizeGB" -le 2 ]; then
        zRamSizeMB=$((RamSizeGB * 1024 * 3 / 4))
    else
        zRamSizeMB=$((RamSizeGB * 1024 / 2))
    fi
    [ "$zRamSizeMB" -le 4096 ] || zRamSizeMB=4096

    echo lz4 > /sys/block/zram0/comp_algorithm || return 1
    if [ -f /sys/block/zram0/use_dedup ]; then
        echo 1 > /sys/block/zram0/use_dedup
    fi
    if [ "$MemTotal" -le 524288 ]; then
        echo 402653184 > /sys/block/zram0/disksize || return 1
    elif [ "$MemTotal" -le 1048576 ]; then
        echo 805306368 > /sys/block/zram0/disksize || return 1
    else
        echo "${zRamSizeMB}M" > /sys/block/zram0/disksize || return 1
    fi

    # Retain the existing debug-SLAB mitigation on kernels that expose it.
    for slab in zs_handle zspage; do
        if [ -e /sys/kernel/slab/$slab/store_user ]; then
            echo 0 > /sys/kernel/slab/$slab/store_user
        fi
    done
    mkswap /dev/block/zram0 || return 1
    swapon /dev/block/zram0 -p 32758
}

function configure_read_ahead_kb_values() {
    read_mem_total || return 1

    # Preserve 128 KiB at <=3 GiB and 512 KiB above it.
    read_ahead=512
    [ "$MemTotal" -le 3145728 ] && read_ahead=128
    for node in /sys/block/mmcblk0/bdi/read_ahead_kb \
                /sys/block/mmcblk0rpmb/bdi/read_ahead_kb \
                /sys/block/dm-*/queue/read_ahead_kb \
                /sys/block/mmc*/queue/read_ahead_kb; do
        [ -f "$node" ] && echo "$read_ahead" > "$node"
    done
}

function enable_swap() {
    read_mem_total || return 1

    SWAP_ENABLE_THRESHOLD=1048576
    swap_enable=`getprop ro.vendor.qti.config.swap`

    # Enable swap initially only for 1 GB targets
    if [ "$MemTotal" -le "$SWAP_ENABLE_THRESHOLD" ] && [ "$swap_enable" == "true" ]; then
        # Static swiftness
        echo 1 > /proc/sys/vm/swap_ratio_enable
        echo 70 > /proc/sys/vm/swap_ratio

        # Swap disk - 200MB size
        if [ ! -f /data/vendor/swap/swapfile ]; then
            dd if=/dev/zero of=/data/vendor/swap/swapfile bs=1m count=200
        fi
        mkswap /data/vendor/swap/swapfile
        swapon /data/vendor/swap/swapfile -p 32758
    fi
}

function configure_memory_parameters() {
    #add memory limit to camera cgroup
    read_mem_total || return 1
    if [ $MemTotal -gt 8388608 ]; then
        let LimitSize=838860800
    else
        let LimitSize=524288000
    fi

    # This limit is a cgroup v1 interface; newer releases use cgroup v2.
    if [ -f /dev/memcg/camera/memory.soft_limit_in_bytes ]; then
        echo $LimitSize > /dev/memcg/camera/memory.soft_limit_in_bytes
    fi

    # Set swappiness to 60 for all targets
    echo 60 > /proc/sys/vm/swappiness

    # Preserve the existing device watermark policy; retune only with workload data.
    echo 1 > /proc/sys/vm/watermark_scale_factor

    # Disable the feature of watermark boost for 8G and below device
    read_mem_total || return 1

    if [ $MemTotal -le 8388608 ]; then
        echo 0 > /proc/sys/vm/watermark_boost_factor
    fi

    configure_zram_parameters

    configure_read_ahead_kb_values

    enable_swap
}

# Apply settings for Kona

case "$target" in
	"kona")
	rev=`cat /sys/devices/soc0/revision`
	ddr_type=`od -An -tx /proc/device-tree/memory/ddr_device_type`
	ddr_type4="07"
	ddr_type5="08"

	# Core control parameters for gold
	echo 2 > /sys/devices/system/cpu/cpu4/core_ctl/min_cpus
	echo 60 > /sys/devices/system/cpu/cpu4/core_ctl/busy_up_thres
	echo 30 > /sys/devices/system/cpu/cpu4/core_ctl/busy_down_thres
	echo 100 > /sys/devices/system/cpu/cpu4/core_ctl/offline_delay_ms
	echo 3 > /sys/devices/system/cpu/cpu4/core_ctl/task_thres

	# Core control parameters for gold+
	echo 0 > /sys/devices/system/cpu/cpu7/core_ctl/min_cpus
	echo 60 > /sys/devices/system/cpu/cpu7/core_ctl/busy_up_thres
	echo 30 > /sys/devices/system/cpu/cpu7/core_ctl/busy_down_thres
	echo 100 > /sys/devices/system/cpu/cpu7/core_ctl/offline_delay_ms
	echo 1 > /sys/devices/system/cpu/cpu7/core_ctl/task_thres
	# Controls how many more tasks should be eligible to run on gold CPUs
	# w.r.t number of gold CPUs available to trigger assist (max number of
	# tasks eligible to run on previous cluster minus number of CPUs in
	# the previous cluster).
	#
	# Setting to 1 by default which means there should be at least
	# 4 tasks eligible to run on gold cluster (tasks running on gold cores
	# plus misfit tasks on silver cores) to trigger assitance from gold+.
	echo 1 > /sys/devices/system/cpu/cpu7/core_ctl/nr_prev_assist_thresh

	# Disable Core control on silver
	echo 0 > /sys/devices/system/cpu/cpu0/core_ctl/enable

	# Setting b.L scheduler parameters
	echo 95 95 > /proc/sys/kernel/sched_upmigrate
	echo 85 85 > /proc/sys/kernel/sched_downmigrate
	echo 100 > /proc/sys/kernel/sched_group_upmigrate
	echo 85 > /proc/sys/kernel/sched_group_downmigrate
	echo 1 > /proc/sys/kernel/sched_walt_rotate_big_tasks
	echo 400000000 > /proc/sys/kernel/sched_coloc_downmigrate_ns

	# cpuset parameters
        echo 0-2     > /dev/cpuset/background/cpus
        echo 0-3     > /dev/cpuset/system-background/cpus
        if [ -f /dev/cpuset/foreground/boost/cpus ]; then
            echo 4-7 > /dev/cpuset/foreground/boost/cpus
        fi
        echo 0-2,4-7 > /dev/cpuset/foreground/cpus
        echo 0-7     > /dev/cpuset/top-app/cpus

	# Turn off scheduler boost at the end
	echo 0 > /proc/sys/kernel/sched_boost

	# configure governor settings for silver cluster
	echo "schedutil" > /sys/devices/system/cpu/cpufreq/policy0/scaling_governor
	echo 0 > /sys/devices/system/cpu/cpufreq/policy0/schedutil/down_rate_limit_us
	echo 0 > /sys/devices/system/cpu/cpufreq/policy0/schedutil/up_rate_limit_us
        if [ $rev == "2.0" ] || [ $rev == "2.1" ]; then
		echo 1248000 > /sys/devices/system/cpu/cpufreq/policy0/schedutil/hispeed_freq
	else
		echo 1228800 > /sys/devices/system/cpu/cpufreq/policy0/schedutil/hispeed_freq
	fi
	echo 691200 > /sys/devices/system/cpu/cpufreq/policy0/scaling_min_freq
	echo 1 > /sys/devices/system/cpu/cpufreq/policy0/schedutil/pl

	# configure input boost settings
	echo "0:1344000" > /sys/devices/system/cpu/cpu_boost/input_boost_freq
	echo 120 > /sys/devices/system/cpu/cpu_boost/input_boost_ms

	# configure governor settings for gold cluster
	echo "schedutil" > /sys/devices/system/cpu/cpufreq/policy4/scaling_governor
	echo 0 > /sys/devices/system/cpu/cpufreq/policy4/schedutil/down_rate_limit_us
	echo 0 > /sys/devices/system/cpu/cpufreq/policy4/schedutil/up_rate_limit_us
	echo 1574400 > /sys/devices/system/cpu/cpufreq/policy4/schedutil/hispeed_freq
	echo 1 > /sys/devices/system/cpu/cpufreq/policy4/schedutil/pl

	# configure governor settings for gold+ cluster
	echo "schedutil" > /sys/devices/system/cpu/cpufreq/policy7/scaling_governor
	echo 0 > /sys/devices/system/cpu/cpufreq/policy7/schedutil/down_rate_limit_us
	echo 0 > /sys/devices/system/cpu/cpufreq/policy7/schedutil/up_rate_limit_us
        if [ $rev == "2.0" ] || [ $rev == "2.1" ]; then
		echo 1632000 > /sys/devices/system/cpu/cpufreq/policy7/schedutil/hispeed_freq
	else
		echo 1612800 > /sys/devices/system/cpu/cpufreq/policy7/schedutil/hispeed_freq
	fi
	echo 1 > /sys/devices/system/cpu/cpufreq/policy7/schedutil/pl

	# Enable bus-dcvs
	for device in /sys/devices/platform/soc
	do
	    for cpubw in $device/*cpu-cpu-llcc-bw/devfreq/*cpu-cpu-llcc-bw
	    do
		[ -d "${cpubw}" ] || continue
		echo "bw_hwmon" > $cpubw/governor
		echo "4577 7110 9155 12298 14236 15258" > $cpubw/bw_hwmon/mbps_zones
		echo 4 > $cpubw/bw_hwmon/sample_ms
		echo 50 > $cpubw/bw_hwmon/io_percent
		echo 20 > $cpubw/bw_hwmon/hist_memory
		echo 10 > $cpubw/bw_hwmon/hyst_length
		echo 30 > $cpubw/bw_hwmon/down_thres
		echo 0 > $cpubw/bw_hwmon/guard_band_mbps
		echo 250 > $cpubw/bw_hwmon/up_scale
		echo 1600 > $cpubw/bw_hwmon/idle_mbps
		echo 14236 > $cpubw/max_freq
		echo 40 > $cpubw/polling_interval
	    done

	    for llccbw in $device/*cpu-llcc-ddr-bw/devfreq/*cpu-llcc-ddr-bw
	    do
		[ -d "${llccbw}" ] || continue
		echo "bw_hwmon" > $llccbw/governor
		if [ ${ddr_type:4:2} == $ddr_type4 ]; then
			echo "1720 2086 2929 3879 5161 5931 6881 7980" > $llccbw/bw_hwmon/mbps_zones
		elif [ ${ddr_type:4:2} == $ddr_type5 ]; then
			echo "1720 2086 2929 3879 5931 6881 7980 10437" > $llccbw/bw_hwmon/mbps_zones
		fi
		echo 4 > $llccbw/bw_hwmon/sample_ms
		echo 80 > $llccbw/bw_hwmon/io_percent
		echo 20 > $llccbw/bw_hwmon/hist_memory
		echo 10 > $llccbw/bw_hwmon/hyst_length
		echo 30 > $llccbw/bw_hwmon/down_thres
		echo 0 > $llccbw/bw_hwmon/guard_band_mbps
		echo 250 > $llccbw/bw_hwmon/up_scale
		echo 1600 > $llccbw/bw_hwmon/idle_mbps
		echo 6881 > $llccbw/max_freq
		echo 40 > $llccbw/polling_interval
	    done

	    for npubw in $device/*npu*-ddr-bw/devfreq/*npu*-ddr-bw
	    do
		[ -d "${npubw}" ] || continue
		[ -w /sys/devices/virtual/npu/msm_npu/pwr ] || continue
		echo 1 > /sys/devices/virtual/npu/msm_npu/pwr
		echo "bw_hwmon" > $npubw/governor
		if [ ${ddr_type:4:2} == $ddr_type4 ]; then
			echo "1720 2086 2929 3879 5931 6881 7980" > $npubw/bw_hwmon/mbps_zones
		elif [ ${ddr_type:4:2} == $ddr_type5 ]; then
			echo "1720 2086 2929 3879 5931 6881 7980 10437" > $npubw/bw_hwmon/mbps_zones
		fi
		echo 4 > $npubw/bw_hwmon/sample_ms
		echo 160 > $npubw/bw_hwmon/io_percent
		echo 20 > $npubw/bw_hwmon/hist_memory
		echo 10 > $npubw/bw_hwmon/hyst_length
		echo 30 > $npubw/bw_hwmon/down_thres
		echo 0 > $npubw/bw_hwmon/guard_band_mbps
		echo 250 > $npubw/bw_hwmon/up_scale
		echo 1600 > $npubw/bw_hwmon/idle_mbps
		echo 40 > $npubw/polling_interval
		echo 0 > /sys/devices/virtual/npu/msm_npu/pwr
	    done

	    for npullccbw in $device/*npu*-llcc-bw/devfreq/*npu*-llcc-bw
	    do
		[ -d "${npullccbw}" ] || continue
		[ -w /sys/devices/virtual/npu/msm_npu/pwr ] || continue
		echo 1 > /sys/devices/virtual/npu/msm_npu/pwr
		echo "bw_hwmon" > $npullccbw/governor
		echo "4577 7110 9155 12298 14236 15258" > $npullccbw/bw_hwmon/mbps_zones
		echo 4 > $npullccbw/bw_hwmon/sample_ms
		echo 160 > $npullccbw/bw_hwmon/io_percent
		echo 20 > $npullccbw/bw_hwmon/hist_memory
		echo 10 > $npullccbw/bw_hwmon/hyst_length
		echo 30 > $npullccbw/bw_hwmon/down_thres
		echo 0 > $npullccbw/bw_hwmon/guard_band_mbps
		echo 250 > $npullccbw/bw_hwmon/up_scale
		echo 1600 > $npullccbw/bw_hwmon/idle_mbps
		echo 40 > $npullccbw/polling_interval
		echo 0 > /sys/devices/virtual/npu/msm_npu/pwr
	    done
	done
        # memlat specific settings are moved to seperate file under
        # device/target specific folder
        setprop vendor.dcvs.prop 0
	setprop vendor.dcvs.prop 1
    echo N > /sys/module/lpm_levels/parameters/sleep_disabled
    configure_memory_parameters
    ;;
esac

# Post-setup completion for this platform.
case "$target" in
    "kona") setprop vendor.post_boot.parsed 1 ;;
esac

# Let kernel know our image version/variant/crm_version
if [ -f /sys/devices/soc0/select_image ]; then
    image_version="10:"
    image_version+=`getprop ro.build.id`
    image_version+=":"
    image_version+=`getprop ro.build.version.incremental`
    image_variant=`getprop ro.product.name`
    image_variant+="-"
    image_variant+=`getprop ro.build.type`
    oem_version=`getprop ro.build.version.codename`
    echo 10 > /sys/devices/soc0/select_image
    echo $image_version > /sys/devices/soc0/image_version
    echo $image_variant > /sys/devices/soc0/image_variant
    echo $oem_version > /sys/devices/soc0/image_crm_version
fi

# Change console log level as per console config property
console_config=`getprop persist.vendor.console.silent.config`
case "$console_config" in
    "1")
        echo "Enable console config to $console_config"
        echo 0 > /proc/sys/kernel/printk
        ;;
    *)
        echo "Enable console config to $console_config"
        ;;
esac

# Parse misc partition path and set property
misc_link=$(ls -l /dev/block/bootdevice/by-name/misc)
real_path=${misc_link##*>}
setprop persist.vendor.mmi.misc_dev_path $real_path
