#!/usr/bin/env -S PYTHONPATH=../../../tools/extract-utils python3
#
# SPDX-FileCopyrightText: 2024 The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#

from extract_utils.main import (
    ExtractUtils,
    ExtractUtilsModule,
)
from extract_utils.fixups_blob import (
    blob_fixup,
    blob_fixups_user_type,
)
from extract_utils.fixups_lib import (
    lib_fixups,
    lib_fixups_user_type,
)

blob_fixups: blob_fixups_user_type = {
    'system/framework/WfdCommon.jar': blob_fixup()
        .apktool_patch('blob-patches/WfdCommon.patch'),
    # Only the 64-bit WFD executable is packaged by this tree.
    'system_ext/etc/init/wfdservice.rc': blob_fixup()
        .regex_replace(r'service wfdservice /system_ext/bin/wfdservice\n(?:    [^\n]*\n)+\n', '')
        .regex_replace(r'on property:vendor\.wfdservice=(?:enable|disable)\n    (?:start|stop) wfdservice\n\n', ''),
    'system_ext/lib64/libwfdservice.so': blob_fixup()
        .replace_needed('android.media.audio.common.types-V4-cpp.so', 'android.media.audio.common.types-V5-cpp.so'),
    # SM8250 builds RMNET into the kernel; stock GKI module hooks cannot work.
    'vendor/etc/init/netmgrd.rc': blob_fixup()
        .regex_replace(r'    #Load rmnet_core driver\n(?:    exec [^\n]*modprobe[^\n]*\n)+',
                       '    # RMNET drivers are built into the SM8250 kernel.\n')
        .regex_replace(r'\non property:persist\.vendor\.data\.(?:shs|perf|offload)_ko_load=[0-3]\n(?:    exec [^\n]*modprobe[^\n]*\n)+', ''),
    'vendor/etc/init/android.hardware.neuralnetworks@1.3-service-qti.rc': blob_fixup()
        .regex_replace(r'writepid /dev/stune/nnapi-hal/tasks', 'task_profiles NNApiHALPerformance'),
    'vendor/etc/init/vendor.qti.media.c2@1.0-service.rc': blob_fixup()
        .regex_replace(r'writepid /dev/cpuset/foreground/tasks', 'task_profiles ProcessCapacityHigh'),
    'vendor/etc/init/android.hardware.drm@1.3-service.widevine.rc': blob_fixup()
        .regex_replace(r'writepid /dev/cpuset/foreground/tasks', 'task_profiles ProcessCapacityHigh')
        # The stock MIUI migration script is not present in this ROM.
        .regex_replace(r'    start vendor\.move_data_sh\n', '')
        .regex_replace(r'service vendor\.move_data_sh /system/bin/move_widevine_data\.sh\n(?:    [^\n]*\n)+\n', ''),
    'vendor/etc/init/init.mi_thermald.rc': blob_fixup()
        .regex_replace('.*seclabel u:r:mi_thermald:s0\n', ''),
    'vendor/etc/init/vendor.xiaomi.hardware.touchfeature@1.0-service.rc': blob_fixup()
        .regex_replace(r'(service toucheventcheck /vendor/bin/toucheventcheck\n)(?!    disabled)',
                       r'\1    disabled\n'),
    'vendor/etc/seccomp_policy/atfwd@2.0.policy': blob_fixup()
        .add_line_if_missing('gettid: 1'),
    'vendor/lib64/libril-qc-hal-qmi.so': blob_fixup()
        .binary_regex_replace(b'ro.product.vendor.device', b'ro.vendor.radio.midevice'),
    'vendor/lib64/libwvhidl.so': blob_fixup()
        .add_needed('libcrypto_shim.so'),
    'vendor/lib64/mediadrm/libwvdrmengine.so': blob_fixup()
        .add_needed('libcrypto_shim.so'),
    (
     'vendor/lib/libstagefright_soft_ac4dec.so',
     'vendor/lib/libstagefright_soft_ddpdec.so',
     'vendor/lib/libstagefrightdolby.so',
     'vendor/lib64/libdlbdsservice.so',
     'vendor/lib64/libstagefright_soft_ac4dec.so',
     'vendor/lib64/libstagefright_soft_ddpdec.so',
     'vendor/lib64/libstagefrightdolby.so'
     ): blob_fixup()
        .replace_needed('libstagefright_foundation.so', 'libstagefright_foundation-v33.so'),
}  # fmt: skip


def lib_fixup_vendor_suffix(lib: str, partition: str, *args, **kwargs):
    return f'{lib}_{partition}' if partition == 'vendor' else None


lib_fixups: lib_fixups_user_type = {
    **lib_fixups,
    (
        'com.qualcomm.qti.dpm.api@1.0',
        'libmmosal',
        'vendor.qti.hardware.wifidisplaysession@1.0',
        'vendor.qti.imsrtpservice@3.0',
    ): lib_fixup_vendor_suffix,
}

namespace_imports = [
    'hardware/qcom-caf/sm8250',
    'hardware/qcom-caf/wlan',
    'hardware/xiaomi',
    'vendor/qcom/opensource/commonsys-intf/display',
    'vendor/qcom/opensource/commonsys/display',
    'vendor/qcom/opensource/dataservices',
    'vendor/qcom/opensource/display',
    'vendor/xiaomi/sm8250-common',
]

module = ExtractUtilsModule(
    'sm8250-common',
    'xiaomi',
    blob_fixups=blob_fixups,
    lib_fixups=lib_fixups,
    namespace_imports=namespace_imports,
)

module.add_proprietary_file('proprietary-files-phone.txt').add_copy_files_guard(
    'TARGET_IS_TABLET', 'true', invert=True
)

if __name__ == '__main__':
    utils = ExtractUtils.device(module)
    utils.run()
