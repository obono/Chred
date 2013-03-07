package com.obnsoft.chred;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.zip.Deflater;

public class PTCFile {

    public static final int PTC_TYPE_CHR = 3;
    public static final int PTC_TYPE_COL = 5;

    private static final String PTC_ID = "PX01";
    private static final String PTCQR_ID = "PT";
    private static final byte[] MD5HEADER =
            {'P', 'E', 'T', 'I', 'T', 'C', 'O', 'M'};

    private String mName;
    private int mType;
    private byte[] mData;

    /*-----------------------------------------------------------------------*/

    public String getName() {
        return (mName == null) ? MyApplication.PTC_KEYWORD : mName;
    }

    public int getType() {
        return mType;
    }

    public byte[] getData() {
        return mData;
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

    public void processQR() {
        processQR(getName(), getType(), getData());
    }

    public void clear() {
        mName = null;
        mType = 0;
        mData = null;
    }

    /*-----------------------------------------------------------------------*/

    public static boolean save(OutputStream out, String name, int type, byte[] data) {
        if (out == null || name == null || data == null) {
            return false;
        }
        byte[] header = new byte[20];
        copyString(header, 0, 4, PTC_ID);
        copyValue(header, 4, 4, data.length);
        copyValue(header, 8, 4, type);
        copyString(header, 12, 8, name);
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

    public static void processQR(String name, int type, byte[] data) {
        String strType = (type == PTC_TYPE_CHR) ? "PCHR" : "PCOL";
        byte[] cmprsData = compressData(name, strType, data);
        byte[] md5 = getMD5(cmprsData);
        byte[] qrData = new byte[666]; // TODO: Level 20 Error M
        int unit = qrData.length - 36;
        int qrCount = (int) Math.ceil(cmprsData.length / (double) unit);
        byte[] partData = new byte[unit];

        for (int i = 0; i < qrCount; i++) {
            int len = cmprsData.length - i * unit;
            if (len > unit) {
                len = unit;
            } else {
                partData = new byte[len];
            }
            System.arraycopy(cmprsData, i * unit, partData, 0, len);
            byte[] md5each = getMD5(partData);
            copyString(qrData, 0, 2, PTCQR_ID);
            copyValue(qrData, 2, 1, i + 1);
            copyValue(qrData, 3, 1, qrCount);
            System.arraycopy(md5each, 0, qrData, 4, 16);
            System.arraycopy(md5, 0, qrData, 20, 16);
            System.arraycopy(partData, 0, qrData, 36, len);
            if (len < unit) {
                Arrays.fill(qrData, 36 + len, 36 + unit, (byte) 0);
            }
            /* Create QR code */
        }
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
        byte[] work = new byte[MD5HEADER.length + data.length];
        System.arraycopy(MD5HEADER, 0, work, 0, MD5HEADER.length);
        System.arraycopy(data, 0, work, MD5HEADER.length, data.length);
        return getMD5(work);
    }

    private static byte[] compressData(String strName, String strType, byte[] orgData) {
        byte[] work = new byte[1024 * 1024];
        Deflater compresser = new Deflater();
        compresser.setInput(orgData);
        compresser.finish();
        int len = compresser.deflate(work);
        if (len > 0 && len < work.length - 1) {
            byte[] data = new byte[20 + len];
            copyString(data, 0, 8, strName);
            copyString(data, 8, 4, strType);
            copyValue(data, 12, 4, len);
            copyValue(data, 16, 4, orgData.length);
            System.arraycopy(work, 0, data, 20, len);
            return data;
        }
        return null;
    }

    private static String extractString(byte[] data, int start, int len) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < len; i++) {
            char c = (char) data[start + i];
            if (c > 0) buf.append(c); // TODO: is this safe?
        }
        return buf.toString();
    }

    private static void copyString(byte[] data, int start, int len, String str) {
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

    private static void copyValue(byte[] ary, int start, int len, int val) {
        for (int i = 0; i < len; i++) {
            ary[start + i] = (byte) (val & 0xFF);
            val >>= 8;
        }
    }

}
