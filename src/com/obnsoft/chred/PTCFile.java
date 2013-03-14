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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Locale;
import java.util.zip.Deflater;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.swetake.util.Qrcode;

public class PTCFile {

    public static final int PTC_TYPE_CHR = 3;
    public static final int PTC_TYPE_COL = 5;

    private static final String[] PTC_TYPE_PREFIX =
            {null, null, null, "CHR:", null, "COL:"};
    private static final String PTC_ID = "PX01";
    private static final String PTCQR_ID = "PT";
    private static final byte[] MD5EXTRA =
            {'P', 'E', 'T', 'I', 'T', 'C', 'O', 'M'};

    private static final int QR_CAPACITY_20_M = 666;
    //private static final int QR_CAPACITY_20_L = 858;
    private static final int QR_SIZE = 190; // 95*2
    private static final int QR_MARGIN = 16;
    private static final int QR_PADDING = 32;
    private static final int QR_STEP = QR_SIZE + QR_MARGIN * 2;

    private String mName;
    private int mType;
    private byte[] mData;

    /*-----------------------------------------------------------------------*/

    public String getName() {
        return (mName == null) ? MyApplication.ENAME_DEFAULT : mName;
    }

    public int getType() {
        return mType;
    }

    public byte[] getData() {
        return mData;
    }

    public String getNameWithType() {
        return getNameWithType(mType, getName());
    }

    public boolean load(InputStream in) {
        byte[] header = new byte[20];
        byte[] md5 = new byte[16];
        clear();
        try {
            in.read(header);
            if (!PTC_ID.equals(extractString(header, 0, 4))) {
                return false;
            }
            in.read(md5);
            mData = new byte[extractValue(header, 4, 4)];
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
        mType = extractValue(header, 8, 4);
        mName = extractString(header, 12, 8);
        return true;
    }

    public boolean save(OutputStream out) {
        return save(out, mName, mType, mData);
    }

    public Bitmap generateQRCodes(String footer) {
        return generateQRCodes(getName(), getData(), footer);
    }

    public void clear() {
        mName = null;
        mType = 0;
        mData = null;
    }

    /*-----------------------------------------------------------------------*/

    public static String getNameWithType(int type, String name) {
        switch (type) {
        case PTC_TYPE_CHR:
        case PTC_TYPE_COL:
            return PTC_TYPE_PREFIX[type].concat(name);
        default:
            return null;
        }
    }

    public static boolean save(OutputStream out, String name, int type, byte[] data) {
        if (out == null || name == null || data == null) {
            return false;
        }
        byte[] header = new byte[20];
        embedString(header, 0, 4, PTC_ID);
        embedValue(header, 4, 4, data.length);
        embedValue(header, 8, 4, type);
        embedString(header, 12, 8, name);
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

    public static Bitmap generateQRCodes(String name, byte[] data, String footer) {
        byte[] cmprsData = compressData(name, data);
        if (cmprsData == null) {
            return null;
        }

        /*  Prepare for processing data  */
        byte[] md5 = getMD5(cmprsData);
        byte[] qrData = new byte[QR_CAPACITY_20_M];
        int dataUnit = qrData.length - 36;
        int qrCount = (int) Math.ceil(cmprsData.length / (double) dataUnit);
        byte[] partData = new byte[dataUnit];
        Qrcode qrBuilder = new Qrcode();
        qrBuilder.setQrcodeVersion(20);
        qrBuilder.setQrcodeEncodeMode('B');
        qrBuilder.setQrcodeErrorCorrect('L');

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
        String lbl = extractString(cmprsData, 9, 3).concat(":").concat(name);
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
            byte[] md5each = getMD5(partData);
            embedString(qrData, 0, 2, PTCQR_ID);
            embedValue(qrData, 2, 1, i + 1);
            embedValue(qrData, 3, 1, qrCount);
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

    /*-----------------------------------------------------------------------*/

    private static byte[] getMD5(byte[] data) {
        byte[] md5 = null;
        try {
            MessageDigest digest;
            digest = MessageDigest.getInstance("MD5");
            md5 = digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return md5;
    }

    private static byte[] getPetitcomMD5(byte[] data) {
        byte[] work = new byte[MD5EXTRA.length + data.length];
        System.arraycopy(MD5EXTRA, 0, work, 0, MD5EXTRA.length);
        System.arraycopy(data, 0, work, MD5EXTRA.length, data.length);
        return getMD5(work);
    }

    private static byte[] compressData(String strName, byte[] orgData) {
        if (orgData == null) {
            return null;
        }
        byte[] work = new byte[1024 * 1024]; // 1MiB
        Deflater compresser = new Deflater(Deflater.BEST_COMPRESSION);
        compresser.setInput(orgData);
        compresser.finish();
        int len = compresser.deflate(work);
        if (len > 0 && len < work.length - 1) {
            byte[] data = new byte[20 + len];
            embedString(data, 0, 8, strName);
            System.arraycopy(orgData, 8, data, 8, 4); // "R***"
            embedValue(data, 12, 4, len);
            embedValue(data, 16, 4, orgData.length);
            System.arraycopy(work, 0, data, 20, len);
            return data;
        }
        return null;
    }

    private static String extractString(byte[] data, int start, int len) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < len; i++) {
            char c = (char) data[start + i];
            if (c >= 'a') c -= 0x20;
            if (c >= '0' && c <= '9' || c >= 'A' && c <= 'Z' || c == '_') buf.append(c);
        }
        return buf.toString();
    }

    private static void embedString(byte[] data, int start, int len, String str) {
        Arrays.fill(data, start, start + len, (byte) 0);
        System.arraycopy(str.getBytes(), 0, data, start,
                (str.length() > len) ? len : str.length());
    }

    private static int extractValue(byte[] data, int start, int len) {
        int val = 0;
        for (int i = 0; i < len; i++) {
            val |= (data[start + i] & 0xFF) << i * 8;
        }
        return val;
    }

    private static void embedValue(byte[] ary, int start, int len, int val) {
        for (int i = 0; i < len; i++) {
            ary[start + i] = (byte) (val & 0xFF);
            val >>= 8;
        }
    }

}
