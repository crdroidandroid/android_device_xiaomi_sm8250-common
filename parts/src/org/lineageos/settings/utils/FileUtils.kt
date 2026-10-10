/*
 * Copyright (C) 2016 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.settings.utils

import android.util.Log
import java.io.File
import java.io.IOException

object FileUtils {
    @JvmStatic
    fun readOneLine(fileName: String): String? =
        try {
            File(fileName).bufferedReader(bufferSize = 512).use { it.readLine() }
        } catch (error: IOException) {
            Log.w("FileUtils", "Could not read $fileName", error)
            null
        }

    // use closes/flushed the writer before success is reported, including sysfs close errors.
    @JvmStatic
    fun writeLine(fileName: String, value: String): Boolean =
        try {
            File(fileName).bufferedWriter().use { it.write(value) }
            true
        } catch (error: IOException) {
            Log.w("FileUtils", "Could not write $fileName", error)
            false
        }

    @JvmStatic fun fileExists(fileName: String) = File(fileName).exists()

    @JvmStatic
    fun isFileReadable(fileName: String) = File(fileName).let { it.exists() && it.canRead() }

    @JvmStatic
    fun isFileWritable(fileName: String) = File(fileName).let { it.exists() && it.canWrite() }

    @JvmStatic
    fun delete(fileName: String): Boolean =
        try {
            File(fileName).delete()
        } catch (error: SecurityException) {
            Log.w("FileUtils", "Cannot delete $fileName", error)
            false
        }

    @JvmStatic
    fun rename(srcPath: String, dstPath: String): Boolean =
        try {
            File(srcPath).renameTo(File(dstPath))
        } catch (error: SecurityException) {
            Log.w("FileUtils", "Cannot rename $srcPath", error)
            false
        }
}
