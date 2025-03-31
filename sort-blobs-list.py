#!/usr/bin/env python3
#
# SPDX-FileCopyrightText: 2024 The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#

import os
import sys
import subprocess
from pathlib import Path

# List of files to sort
PROPRIETARY_FILES_TXT = [
    "proprietary-files.txt",
    "proprietary-files-phone.txt"
]

def main():
    # Determine script directory
    script_dir = Path(__file__).parent.absolute()
    
    # Determine Android root directory
    android_root = script_dir.parent.parent.parent
    
    # Path to the helper script
    helper = android_root / "tools" / "extract-utils" / "sort-blobs-list.py"
    
    # Check if helper exists
    if not helper.is_file():
        print(f"Unable to find helper script at {helper}")
        sys.exit(1)
    
    # Call the helper to sort the list
    # Add --dir-first to give priority to directories and subdirectories
    try:
        subprocess.run(
            [str(helper), "--dir-first"] + PROPRIETARY_FILES_TXT,
            check=True
        )
    except subprocess.CalledProcessError as e:
        print(f"Error running sort-blobs-list.py: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()
