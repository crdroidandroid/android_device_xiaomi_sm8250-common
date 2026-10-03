#! /vendor/bin/sh

# Copyright (c) 2009-2016, The Linux Foundation. All rights reserved.
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

# This tree is SM8250/Kona-only (TARGET_BOARD_PLATFORM := kona).
# Keep the runtime setup limited to the path that can actually execute here.
echo 1 > /proc/sys/net/ipv6/conf/default/accept_ra_defrtr

if [ -f /vendor/bin/msm_irqbalance ]; then
    start vendor.msm_irqbalance
fi

#
# Make modem config folder and copy firmware config to that folder for RIL
#
function copy_modem_config() {
    local source=/vendor/firmware_mnt
    local dest=/data/vendor/modem_config
    local current previous

    # Do not destroy a usable cache when the firmware mount is unavailable.
    current=$(cat "$source/verinfo/ver_info.txt") || return 1
    [ -n "$current" ] || return 1
    [ -r "$source/image/modem_pr/mbn_ota.txt" ] || return 1
    [ -d "$source/image/modem_pr/mcfg/configs" ] || return 1
    set -- "$source/image/modem_pr/mcfg/configs/"*
    [ -e "$1" ] || return 1

    previous=
    if [ -f "$dest/ver_info.txt" ]; then
        previous=$(cat "$dest/ver_info.txt") || return 1
    fi
    if [ "$previous" != "$current" ]; then
        # Existing configuration subdirectories preserve firmware read-only modes.
        # The parent is created by init with group root and mode 0570.
        for entry in "$dest/"*; do
            [ -e "$entry" ] || [ -L "$entry" ] || continue
            chmod -R g+w "$entry" || return 1
        done
        rm -rf "$dest/"* || return 1
        cp --preserve=m -dr "$@" "$dest/" || return 1
        cp --preserve=m -d "$source/image/modem_pr/mbn_ota.txt" "$dest/" || return 1
        chown -hR radio.root "$dest/"* || return 1

        # Publish the version last. Failed copies must be retried next boot.
        cp --preserve=m -d "$source/verinfo/ver_info.txt" "$dest/ver_info.txt" &&
            chown radio.root "$dest/ver_info.txt" || {
                rm -f "$dest/ver_info.txt"
                return 1
            }
    fi
    chmod g-w "$dest" || return 1
}

if copy_modem_config; then
    setprop ro.vendor.ril.mbn_copy_completed 1
else
    log -t init.qcom -p e "Modem configuration copy failed; leaving completion unset"
fi

#check build variant for printk logging
#current default minimum boot-time-default
buildvariant=`getprop ro.build.type`
case "$buildvariant" in
    "userdebug" | "eng")
        #set default loglevel to KERN_INFO
        echo "4 6 1 7" > /proc/sys/kernel/printk
        ;;
    *)
        #set default loglevel to KERN_WARNING
        echo "4 4 1 4" > /proc/sys/kernel/printk
        ;;
esac
