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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.swetake.util.Qrcode;

public class PTCFile {

    public static final int PTC_TYPE_UNKNOWN = -1;
    public static final int PTC_TYPE_PRG = 0;
    public static final int PTC_TYPE_MEM = 1;
    public static final int PTC_TYPE_GRP = 2;
    public static final int PTC_TYPE_CHR = 3;
    public static final int PTC_TYPE_SCR = 4;
    public static final int PTC_TYPE_COL = 5;

    public static final int HEADLEN_CMPRSDATA = 20;
    public static final int WORKLEN_CMPRSDATA = 1024 * 1024; // 1MiB

    private static final String[] PTC_TYPE_PREFIX =
            {"PRG", "MEM", "GRP", "CHR", "SCR", "COL"};
    private static final String PTC_ID = "PX01";
    private static final String PTCQR_ID = "PT";
    private static final byte[] MD5EXTRA =
            {'P', 'E', 'T', 'I', 'T', 'C', 'O', 'M'};

    private static final int QR_CAPACITY_20_M = 666;
    private static final int QR_CAPACITY_20_L = 858;
    private static final int QR_SIZE = 190; // double of 95 (Ver20 QR size)
    private static final int QR_MARGIN = 16;
    private static final int QR_PADDING = 32;
    private static final int QR_STEP = QR_SIZE + QR_MARGIN * 2;

    private String mName;
    private int mType;
    private byte[] mData;

    /*-----------------------------------------------------------------------*/

    public PTCFile() {
        clear();
    }

    public PTCFile(String name, int type, byte[] data) {
        mName = name;
        mType = type;
        mData = data;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public int getType() {
        return mType;
    }

    public byte[] getData() {
        return mData;
    }

    public String getNameWithType() {
        if (mType == PTC_TYPE_UNKNOWN) {
            return null;
        }
        String name = getName();
        return getPrefixFromType(mType).concat(":").concat(
                (name == null || name.length() == 0) ? MyApplication.ENAME_DEFAULT : name);
    }

    public boolean load(InputStream in) {
        byte[] header = new byte[20];
        byte[] md5 = new byte[16];
        clear();
        try {
            in.read(header);
            if (!PTC_ID.equals(Utils.extractString(header, 0, 4))) {
                return false;
            }
            in.read(md5);
            mData = new byte[Utils.extractValue(header, 4, 4)];
            in.read(mData);
            if (!Arrays.equals(md5, getPetitcomMD5(mData))) {
                clear();
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            clear();
            return false;
        }
        mType = Utils.extractValue(header, 8, 4);
        mName = Utils.extractString(header, 12, 8);
        return true;
    }

    public boolean expand(byte[] cmprsData) {
        if (cmprsData.length <= 20) {
            return false;
        }
        int finalLen = Utils.extractValue(cmprsData, 16, 4);
        byte[] data = new byte[finalLen];
        int dataLen = 0;
        try {
            Inflater expander = new Inflater();
            expander.setInput(cmprsData, 20, cmprsData.length - 20);
            dataLen = expander.inflate(data);
            expander.end();
        } catch (DataFormatException e) {
            e.printStackTrace();
        }
        if (dataLen != finalLen) {
            return false;
        }
        mType = getTypeFromPrefix(Utils.extractString(cmprsData, 9, 3));
        if (mType == PTC_TYPE_UNKNOWN) {
            return false;
        }
        mName = Utils.extractString(cmprsData, 0, 8);
        mData = data;
        return true;
    }

    public boolean save(OutputStream out) {
        return save(out, mName, mType, mData);
    }

    public byte[] compress() {
        return compress(mName, mType, mData);
    }

    public Bitmap generateQRCodes(boolean isTight, String footer) {
        return generateQRCodes(compress(), isTight, footer);
    }

    public void clear() {
        mName = null;
        mType = PTC_TYPE_UNKNOWN;
        mData = null;
    }

    /*-----------------------------------------------------------------------*/

    public static int getTypeFromPrefix(String prefix) {
        for (int i = 0; i < PTC_TYPE_PREFIX.length; i++) {
            if (prefix.equals(PTC_TYPE_PREFIX[i])) {
                return i;
            }
        }
        return PTC_TYPE_UNKNOWN;
    }

    public static String getPrefixFromType(int type) {
        return (type >=0 && type < PTC_TYPE_PREFIX.length) ? PTC_TYPE_PREFIX[type] : null;
    }

    public static boolean save(OutputStream out, String name, int type, byte[] data) {
        if (out == null || type == PTC_TYPE_UNKNOWN || data == null) {
            return false;
        }
        if (name == null || name.length() == 0) {
            name = MyApplication.ENAME_DEFAULT;
        }
        byte[] header = new byte[20];
        Utils.embedString(header, 0, 4, PTC_ID);
        Utils.embedValue(header, 4, 4, data.length);
        Utils.embedValue(header, 8, 4, type);
        Utils.embedString(header, 12, 8, name);
        byte[] md5 = getPetitcomMD5(data);
        try {
            out.write(header);
            out.write(md5);
            out.write(data);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static byte[] compress(String name, int type, byte[] data) {
        if (type == PTC_TYPE_UNKNOWN || data == null) {
            return null;
        }
        if (name == null || name.length() == 0) {
            name = MyApplication.ENAME_DEFAULT;
        }
        byte[] work = new byte[WORKLEN_CMPRSDATA];
        Deflater compresser = new Deflater(Deflater.BEST_COMPRESSION);
        compresser.setInput(data);
        compresser.finish();
        int len = compresser.deflate(work);
        compresser.end();
        if (len > 0 && len < work.length - 1) {
            byte[] cmprsData = new byte[20 + len];
            Utils.embedString(cmprsData, 0, 8, name);
            cmprsData[8] = 'R';
            Utils.embedString(cmprsData, 9, 3, PTC_TYPE_PREFIX[type]);
            Utils.embedValue(cmprsData, 12, 4, len);
            Utils.embedValue(cmprsData, 16, 4, data.length);
            System.arraycopy(work, 0, cmprsData, 20, len);
            return cmprsData;
        }
        return null;
    }

    public static Bitmap generateQRCodes(byte[] cmprsData, boolean isTight, String footer) {
        if (cmprsData == null) return null;

        /*  Prepare for processing data  */
        byte[] md5 = Utils.getMD5(cmprsData);
        byte[] qrData = new byte[isTight ? QR_CAPACITY_20_L : QR_CAPACITY_20_M];
        int dataUnit = qrData.length - 36;
        int qrCount = (int) Math.ceil(cmprsData.length / (double) dataUnit);
        byte[] partData = new byte[dataUnit];
        Qrcode qrBuilder = new Qrcode();
        qrBuilder.setQrcodeVersion(20);
        qrBuilder.setQrcodeEncodeMode('B');
        qrBuilder.setQrcodeErrorCorrect(isTight ? 'L' : 'M');

        /*  Prepare bitmap  */
        int qw = (int) Math.ceil(Math.sqrt(qrCount));
        int qh = (qrCount + qw - 1) / qw;
        Bitmap bmp = Bitmap.createBitmap(qw * QR_STEP + QR_PADDING * 2,
                qh * QR_STEP + QR_PADDING * 2, Bitmap.Config.RGB_565);
        bmp.eraseColor(Color.WHITE);
        Canvas canvas = new Canvas(bmp);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        if (footer != null) {
            paint.setColor(Color.GRAY);
            paint.setTextSize(12);
            canvas.drawText(footer, bmp.getWidth() - paint.measureText(footer),
                    bmp.getHeight() - paint.descent(), paint);
        }
        paint.setColor(Color.BLACK);
        paint.setTextSize(24);
        String lbl = Utils.extractString(cmprsData, 9, 3)
                .concat(":").concat(Utils.extractString(cmprsData, 0, 8));
        canvas.drawText(lbl, (bmp.getWidth() - paint.measureText(lbl)) / 2, QR_PADDING, paint);
        int qx = QR_MARGIN + QR_PADDING;
        int qy = QR_MARGIN + QR_PADDING;
        paint.setTextSize(16);

        for (int i = 0; i < qrCount; i++) {
            /*  Generate QR code  */
            int len = cmprsData.length - i * dataUnit;
            if (len > dataUnit) {
                len = dataUnit;
            } else {
                partData = new byte[len];
                qrData = new byte[len + 36];
            }
            System.arraycopy(cmprsData, i * dataUnit, partData, 0, len);
            byte[] md5each = Utils.getMD5(partData);
            Utils.embedString(qrData, 0, 2, PTCQR_ID);
            Utils.embedValue(qrData, 2, 1, i + 1);
            Utils.embedValue(qrData, 3, 1, qrCount);
            System.arraycopy(md5each, 0, qrData, 4, 16);
            System.arraycopy(md5, 0, qrData, 20, 16);
            System.arraycopy(partData, 0, qrData, 36, len);
            boolean[][] qr = qrBuilder.calQrcode(qrData);

            /*  Draw QR code  */
            if (qrCount > 0) {
                paint.setAntiAlias(true);
                lbl = String.format(Locale.US, "%d / %d", i + 1, qrCount);
                canvas.drawText(lbl, qx - QR_MARGIN + (QR_STEP - paint.measureText(lbl)) / 2,
                        qy - QR_MARGIN + QR_STEP + paint.getTextSize() / 2, paint);
            }
            paint.setAntiAlias(false);
            for (int x = 0, xMax = qr.length; x < xMax; x++) {
                for (int y = 0, yMax = qr[x].length; y < yMax; y++) {
                    if (qr[x][y]) {
                        canvas.drawRect(qx + x * 2, qy + y * 2,
                                qx + x * 2 + 2, qy + y * 2 + 2, paint);
                    }
                }
            }
            qx += QR_STEP;
            if (qx >= qw * QR_STEP) {
                qx = QR_MARGIN + QR_PADDING;
                qy += QR_STEP;
            }
        }
        return bmp;
    }

    private static byte[] getPetitcomMD5(byte[] data) {
        byte[] work = new byte[MD5EXTRA.length + data.length];
        System.arraycopy(MD5EXTRA, 0, work, 0, MD5EXTRA.length);
        System.arraycopy(data, 0, work, MD5EXTRA.length, data.length);
        return Utils.getMD5(work);
    }

}
