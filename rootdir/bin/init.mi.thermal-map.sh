#!/vendor/bin/sh
# Copyright (C) 2026 The LineageOS Project
# SPDX-License-Identifier: Apache-2.0

case "$1" in
    global)
        source=/vendor/etc/thermal-map.conf
        ;;
    india)
        source=/vendor/etc/thermal-map-india.conf
        ;;
    *) exit 1 ;;
esac

# Sources are fixed, read-only vendor blobs. Do not publish an empty or partial copy.
directory=/data/vendor/thermal/config
temporary="$directory/thermal-map.conf.tmp"
destination="$directory/thermal-map.conf"
trap 'rm -f "$temporary"' EXIT
trap 'exit 1' HUP INT TERM
umask 077

# Init creates and labels the directory during post-fs-data.
[ -d "$directory" ] && [ ! -L "$directory" ] || exit 1
[ -f "$source" ] && [ -s "$source" ] || exit 1
rm -f "$temporary" || exit 1
cp "$source" "$temporary" || exit 1
[ -s "$temporary" ] || exit 1
cmp -s "$source" "$temporary" || exit 1
chown root:system "$temporary" || exit 1
chmod 0644 "$temporary" || exit 1
restorecon "$temporary" || exit 1
mv -f "$temporary" "$destination" || exit 1
setprop ctl.restart mi_thermald
