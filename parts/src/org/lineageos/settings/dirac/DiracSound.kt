/*
 * Copyright (C) 2018 The LineageOS Project
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

package org.lineageos.settings.dirac

import android.media.audiofx.AudioEffect
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

class DiracSound(priority: Int, audioSession: Int) :
    AudioEffect(
        EFFECT_TYPE_NULL,
        UUID.fromString("5b8e36a5-144a-4c38-b1d7-0002a5d5c51b"),
        priority,
        audioSession,
    ) {
    companion object {
        const val EQ_BAND_COUNT = 10
    }

    override fun hasControl(): Boolean =
        try {
            super.hasControl()
        } catch (_: IllegalStateException) {
            false
        }

    override fun setEnabled(enabled: Boolean): Int {
        if (!enabled) {
            var failure: RuntimeException? = null
            try {
                checkStatus(super.setEnabled(false))
            } catch (error: RuntimeException) {
                failure = error
            }
            try {
                checkStatus(setParameter(25, 0))
            } catch (error: RuntimeException) {
                if (failure == null) failure = error
                else if (failure !== error) failure.addSuppressed(error)
            }
            failure?.let { throw it }
            return SUCCESS
        }
        checkStatus(setParameter(25, 1))
        val status =
            try {
                super.setEnabled(true)
            } catch (failure: RuntimeException) {
                try {
                    checkStatus(setParameter(25, 0))
                } catch (rollback: RuntimeException) {
                    if (failure !== rollback) failure.addSuppressed(rollback)
                }
                throw failure
            }
        if (status != SUCCESS) {
            val failure = IllegalStateException("Framework MiSound enable failed: $status")
            try {
                checkStatus(super.setEnabled(false))
            } catch (rollback: RuntimeException) {
                failure.addSuppressed(rollback)
            }
            try {
                checkStatus(setParameter(25, 0))
            } catch (rollback: RuntimeException) {
                failure.addSuppressed(rollback)
            }
            throw failure
        }
        return SUCCESS
    }

    private fun intParameter(parameter: Int): Int {
        val value = IntArray(1)
        val count = getParameter(parameter, value)
        checkStatus(count)
        check(count == 1) { "Incomplete MiSound scalar: $count" }
        return value[0]
    }

    override fun getEnabled() = intParameter(25) == 1 && super.getEnabled()

    fun hasExpectedEnabledState(enabled: Boolean) =
        intParameter(25) == (if (enabled) 1 else 0) && super.getEnabled() == enabled

    fun getHeadsetList(): IntArray {
        val reply = ByteArray(300)
        val size = getParameter(19, reply)
        checkStatus(size)
        if (size < 4 || size > reply.size) return IntArray(0)
        val buffer = ByteBuffer.wrap(reply).order(ByteOrder.LITTLE_ENDIAN)
        val count = buffer.int
        if (count < 0 || count > (size - 4) / 4) return IntArray(0)
        return IntArray(count) { buffer.int }
    }

    fun getMusic() = intParameter(4)

    fun setMusic(enable: Int) {
        require(enable in 0..1)
        checkStatus(setParameter(4, enable))
    }

    fun setHeadsetType(type: Int) {
        require(type in 0..255)
        checkStatus(setParameter(1, type))
    }

    fun setLevel(band: Int, level: Float) {
        require(band in 0 until EQ_BAND_COUNT && level.isFinite())
        checkStatus(
            setParameter(intArrayOf(2, band), level.toString().toByteArray(Charsets.US_ASCII))
        )
    }

    fun setHifiMode(mode: Int) {
        require(mode in 0..1)
        checkStatus(setParameter(8, mode))
    }

    fun setScenario(scene: Int) {
        require(scene in 0..4)
        checkStatus(setParameter(15, scene))
    }
}
