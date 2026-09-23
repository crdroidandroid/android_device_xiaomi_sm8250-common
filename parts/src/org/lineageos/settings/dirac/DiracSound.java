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

package org.lineageos.settings.dirac;

import android.media.audiofx.AudioEffect;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class DiracSound extends AudioEffect {
    public static final int EQ_BAND_COUNT = 10;
    private static final int MISOUND_PARAM_ENABLE = 25;
    private static final int DIRACSOUND_PARAM_HEADSET_TYPE = 1;
    private static final int DIRACSOUND_PARAM_EQ_LEVEL = 2;
    private static final int DIRACSOUND_PARAM_MUSIC = 4;
    private static final int DIRACSOUND_PARAM_HIFI = 8;
    private static final int DIRACSOUND_PARAM_SCENE = 15;

    private static final UUID EFFECT_TYPE_DIRACSOUND =
            UUID.fromString("5b8e36a5-144a-4c38-b1d7-0002a5d5c51b");

    public DiracSound(int priority, int audioSession) {
        super(EFFECT_TYPE_NULL, EFFECT_TYPE_DIRACSOUND, priority, audioSession);
    }

    @Override
    public boolean hasControl() {
        try {
            return super.hasControl();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    /** MiSound has a native enable gate separate from music mode and AudioEffect state. */
    @Override
    public int setEnabled(boolean enabled) {
        if (!enabled) {
            RuntimeException failure = null;
            // Try both gates even if one fails. Never mask the first failure.
            try {
                checkStatus(super.setEnabled(false));
            } catch (RuntimeException error) {
                failure = error;
            }
            try {
                checkStatus(setParameter(MISOUND_PARAM_ENABLE, 0));
            } catch (RuntimeException error) {
                if (failure == null) failure = error;
                else if (failure != error) failure.addSuppressed(error);
            }
            if (failure != null) throw failure;
            return SUCCESS;
        }
        checkStatus(setParameter(MISOUND_PARAM_ENABLE, 1));
        final int status;
        try {
            status = super.setEnabled(true);
        } catch (RuntimeException failure) {
            rollbackEnable(failure);
            throw failure;
        }
        if (status != SUCCESS) {
            IllegalStateException failure = new IllegalStateException(
                    "Framework MiSound enable failed: " + status);
            try {
                checkStatus(super.setEnabled(false));
            } catch (RuntimeException rollback) {
                failure.addSuppressed(rollback);
            }
            try {
                checkStatus(setParameter(MISOUND_PARAM_ENABLE, 0));
            } catch (RuntimeException rollback) {
                failure.addSuppressed(rollback);
            }
            throw failure;
        }
        return SUCCESS;
    }

    private void rollbackEnable(RuntimeException failure) {
        try {
            checkStatus(setParameter(MISOUND_PARAM_ENABLE, 0));
        } catch (RuntimeException rollback) {
            if (failure != rollback) failure.addSuppressed(rollback);
        }
    }

    private int getIntParameter(int parameter) {
        int[] value = new int[1];
        int count = getParameter(parameter, value);
        checkStatus(count);
        // The int[] overload returns an element count, not a byte count.
        if (count != 1) throw new IllegalStateException("Incomplete MiSound scalar: " + count);
        return value[0];
    }

    @Override
    public boolean getEnabled() {
        return getIntParameter(MISOUND_PARAM_ENABLE) == 1 && super.getEnabled();
    }

    /** A false conjunction alone cannot prove that BOTH processing gates are off. */
    boolean hasExpectedEnabledState(boolean enabled) {
        return getIntParameter(MISOUND_PARAM_ENABLE) == (enabled ? 1 : 0)
                && super.getEnabled() == enabled;
    }

    /** Stock parameter 19 returns a little-endian count followed by headset IDs. */
    public int[] getHeadsetList() {
        byte[] reply = new byte[300];
        int size = getParameter(19, reply);
        checkStatus(size);
        if (size < 4 || size > reply.length) return new int[0];
        ByteBuffer buffer = ByteBuffer.wrap(reply).order(ByteOrder.LITTLE_ENDIAN);
        int count = buffer.getInt();
        if (count < 0 || count > (size - 4) / 4) {
            // Legacy alioth's five-byte reply does not contain a complete int32 ID.
            return new int[0];
        }
        int[] ids = new int[count];
        for (int i = 0; i < count; i++) ids[i] = buffer.getInt();
        return ids;
    }

    public int getMusic() {
        return getIntParameter(DIRACSOUND_PARAM_MUSIC);
    }

    public void setMusic(int enable) {
        if (enable != 0 && enable != 1) throw new IllegalArgumentException("Invalid music mode");
        checkStatus(setParameter(DIRACSOUND_PARAM_MUSIC, enable));
    }

    public void setHeadsetType(int type) {
        if (type < 0 || type > 255) throw new IllegalArgumentException("Invalid headset type");
        checkStatus(setParameter(DIRACSOUND_PARAM_HEADSET_TYPE, type));
    }

    public void setLevel(int band, float level) {
        if (band < 0 || band >= EQ_BAND_COUNT) throw new IllegalArgumentException("Invalid EQ band");
        if (!Float.isFinite(level)) throw new IllegalArgumentException("Non-finite EQ level");
        checkStatus(setParameter(new int[]{DIRACSOUND_PARAM_EQ_LEVEL, band},
                String.valueOf(level).getBytes(StandardCharsets.US_ASCII)));
    }

    public void setHifiMode(int mode) {
        if (mode < 0 || mode > 1) throw new IllegalArgumentException("Invalid Hi-Fi mode");
        checkStatus(setParameter(DIRACSOUND_PARAM_HIFI, mode));
    }

    public void setScenario(int scene) {
        if (scene < 0 || scene > 4) throw new IllegalArgumentException("Invalid scenario");
        checkStatus(setParameter(DIRACSOUND_PARAM_SCENE, scene));
    }
}
