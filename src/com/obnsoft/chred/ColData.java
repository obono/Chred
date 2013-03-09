/*
 * Copyright (C) 2013 OBN-soft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.obnsoft.chred;

import java.util.Arrays;

import android.graphics.Color;

public class ColData {

    public static final int MAX_PALS = 16;
    public static final int COLS_PER_PAL = 16;

    private static final byte[] HEADER =
            {'P', 'E', 'T', 'C', '0', '1', '0', '0', 'R', 'C', 'O', 'L'};

    private boolean mDirty = false;
    private int[] mColor = new int[COLS_PER_PAL * MAX_PALS];

    /*-----------------------------------------------------------------------*/

    public ColData() {
        for (int i = 0; i < MAX_PALS; i++) {
            for (int j = 0; j < COLS_PER_PAL; j++) {
                mColor[i * COLS_PER_PAL + j] = Color.rgb(j << 3, j << 3, j << 3);
            }
        }
    }

    public void resetDirty() {
        mDirty = false;
    }

    public boolean getDirty() {
        return mDirty;
    }

    public int getColor(int pal, int c) {
        if (pal < 0 || pal >= MAX_PALS || c < 0 || c >= COLS_PER_PAL) return Color.TRANSPARENT;
        return mColor[pal << 4 | c] & ((c == 0) ? 0x66FFFFFF : Color.WHITE);
    }

    public void setColor(int pal, int c, int val) {
        if (pal < 0 || pal >= MAX_PALS || c < 0 || c >= COLS_PER_PAL) return;
        mColor[pal << 4 | c] = val | 0xFF000000;
        mDirty = true;
    }

    public static int bits5To8(int val) {
        return val << 3 | val >> 2;
    }

    /*-----------------------------------------------------------------------*/

    public byte[] serialize() {
        byte[] data = new byte[HEADER.length + mColor.length * 2];
        System.arraycopy(HEADER, 0, data, 0, HEADER.length);
        int offset = HEADER.length;
        for (int i = 0; i < mColor.length; i++) {
            int val = Color.red(mColor[i]) >> 3 |
                    (Color.green(mColor[i]) & 0xF8) << 2 |
                    (Color.blue(mColor[i]) & 0xF8) << 7;
            data[offset + i * 2]     = (byte) (val & 0xFF);
            data[offset + i * 2 + 1] = (byte) (val >> 8 & 0xFF);
        }
        return data;
    }

    public boolean deserialize(byte[] data) {
        int headLen = HEADER.length;
        byte[] headData = new byte[headLen];
        System.arraycopy(data, 0, headData, 0, headLen);
        if (Arrays.equals(headData, HEADER)) {
            for (int i = 0; i < mColor.length; i++) {
                int val = data[headLen + i * 2] & 0xFF | data[headLen + i * 2 + 1] << 8 & 0x7F00;
                mColor[i] = Color.rgb(bits5To8(val & 0x1F),
                        bits5To8(val >> 5 & 0x1F), bits5To8(val >> 10 & 0x1F));
            }
            mDirty = true;
            return true;
        }
        return false;
    }

}
