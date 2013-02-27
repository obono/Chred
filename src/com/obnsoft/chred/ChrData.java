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

import java.io.InputStream;
import java.io.OutputStream;

import android.graphics.Bitmap;

public class ChrData {

    public static final int MAX_CHARS = 256;
    public static final int UNIT_SIZE = 8;

    private static final byte[] HEADER1 =
            {'P', 'X', '0', '1', 0x0C, 0x20, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00};
    private static final byte[] HEADER2 =
            {'P', 'E', 'T', 'C', '0', '1', '0', '0', 'R', 'C', 'H', 'R'};
    private static final int BYTES_PER_CHR = UNIT_SIZE * UNIT_SIZE / 2;

    private int mHUnits = 2;
    private int mVUnits = 2;
    private boolean mDirty = false;

    private ColData mColData;
    private ChrUnit[] mChrs;

    /*-----------------------------------------------------------------------*/

    public class ChrUnit {

        private byte[] mDots = new byte[UNIT_SIZE * UNIT_SIZE];

        public int getUnitDot(int x, int y) {
            //if (x < 0 || x >= UNIT_SIZE || y < 0 || y >= UNIT_SIZE) return -1;
            return mDots[y * UNIT_SIZE + x];
        }

        public void setUnitDot(int x, int y, int c) {
            //if (x < 0 || x >= UNIT_SIZE || y < 0 || y >= UNIT_SIZE) return;
            //if (c < 0 || c >= ColData.COLS_PER_PAL) return;
            mDots[y * UNIT_SIZE + x] = (byte) c;
            mDirty = true;
        }

        public void drawUnit(Bitmap bmp, int pal) {
            drawUnit(bmp, pal, 0, 0);
        }

        public void drawUnit(Bitmap bmp, int pal, int x, int y) {
            //if (pal < 0 || pal >= ColData.MAX_PALS || canvas == null || mColData == null) return;
            int idx = 0;
            for (int i = 0; i < UNIT_SIZE; i++) {
                for (int j = 0; j < UNIT_SIZE; j++) {
                    bmp.setPixel(x + j, y + i, mColData.getColor(pal, mDots[idx++]));
                }
            }
        }

        public byte[] getBytes() {
            byte[] bytes = new byte[BYTES_PER_CHR];
            for (int i = 0; i < BYTES_PER_CHR; i++) {
                bytes[i] = (byte) (mDots[i * 2] | mDots[i * 2 + 1] << 4);
            }
            return bytes;
        }

        public void setBytes(byte[] bytes) {
            setBytes(bytes, 0);
        }

        public void setBytes(byte[] bytes, int offset) {
            if (bytes.length < offset + BYTES_PER_CHR) return;
            for (int i = 0; i < BYTES_PER_CHR; i++) {
                mDots[i * 2]     = (byte) (bytes[offset + i] & 0xF);
                mDots[i * 2 + 1] = (byte) (bytes[offset + i] >> 4 & 0xF);
            }
        }

    }

    /*-----------------------------------------------------------------------*/

    public ChrData() {
        mChrs = new ChrUnit[MAX_CHARS];
        for (int i = 0; i < MAX_CHARS; i++) {
            mChrs[i] = new ChrUnit();
        }
    }

    public void setColData(ColData colData) {
        mColData = colData;
    }

    public int getTargetSizeH() {
        return mHUnits;
    }

    public int getTargetSizeV() {
        return mVUnits;
    }

    public void resetDirty() {
        mDirty = false;
    }

    public boolean getDirty() {
        return mDirty;
    }

    public void setTargetSize(int hUnits, int vUnits) {
        if (hUnits != 1 && hUnits != 2 && hUnits != 4 && hUnits != 8) return;
        if (vUnits != 1 && vUnits != 2 && vUnits != 4 && vUnits != 8) return;
        if (hUnits <= 2 && vUnits == 8 || hUnits == 8 && vUnits <= 2) return;
        mHUnits = hUnits;
        mVUnits = vUnits;
    }

    public int getTargetDot(int idx, int x, int y) {
        if (x < 0 || x >= mHUnits * UNIT_SIZE || y < 0 || y > mVUnits * UNIT_SIZE) return -1;
        if (idx < 0 || idx + mVUnits * mHUnits > MAX_CHARS) return -1;
        idx += (y / UNIT_SIZE) * mHUnits + (x / UNIT_SIZE);
        return mChrs[idx].getUnitDot(x % UNIT_SIZE, y % UNIT_SIZE);
    }

    public void setTargetDot(int idx, int x, int y, int c) {
        if (x < 0 || x >= mHUnits * UNIT_SIZE || y < 0 || y > mVUnits * UNIT_SIZE) return;
        if (c <  0 || c >= ColData.COLS_PER_PAL) return;
        if (idx < 0 || idx + mVUnits * mHUnits > MAX_CHARS) return;
        idx += (y / UNIT_SIZE) * mHUnits + (x / UNIT_SIZE);
        mChrs[idx].setUnitDot(x % UNIT_SIZE, y % UNIT_SIZE, c);
    }

    public void drawTarget(Bitmap bmp, int idx, int pal) {
        drawTarget(bmp, idx, pal, 0, 0);
    }

    public void drawTarget(Bitmap bmp, int idx, int pal, int x, int y) {
        if (pal < 0 || pal >= ColData.MAX_PALS || bmp == null || mColData == null) return;
        if (idx < 0 || idx + mVUnits * mHUnits > MAX_CHARS) return;
        for (int i = 0; i < mVUnits; i++) {
            for (int j = 0; j < mHUnits; j++) {
                mChrs[idx++].drawUnit(bmp, pal, x + j * UNIT_SIZE, y + i * UNIT_SIZE);
            }
        }
    }

    public boolean loadFromStream(InputStream in) {
        byte[] data = new byte[HEADER2.length + mChrs.length * BYTES_PER_CHR];
        if (Utils.loadFromStreamCommon(in, HEADER1, data)) {
            for (int i = 0; i < mChrs.length; i++) {
                mChrs[i].setBytes(data, HEADER2.length + i * BYTES_PER_CHR);
            }
            mDirty = true;
            return true;
        }
        return false;
    }

    public boolean saveToStream(OutputStream out, String strName) {
        byte[] data = new byte[HEADER2.length + mChrs.length * BYTES_PER_CHR];
        System.arraycopy(HEADER2, 0, data, 0, HEADER2.length);
        for (int i = 0; i < mChrs.length; i++) {
            System.arraycopy(mChrs[i].getBytes(), 0,
                    data, HEADER2.length + i * BYTES_PER_CHR, BYTES_PER_CHR);
        }
        return Utils.saveToStreamCommon(out, strName, HEADER1, data);
    }
}
