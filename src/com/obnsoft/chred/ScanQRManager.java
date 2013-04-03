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

import android.content.Context;

import jp.sourceforge.qrcode.QRCodeDecoder;
import jp.sourceforge.qrcode.data.QRCodeImage;
import jp.sourceforge.qrcode.exception.DecodingFailedException;
//import jp.sourceforge.qrcode.util.DebugCanvasAdapter;
//import android.util.Log;

public class ScanQRManager {

    private boolean mInProgress;
    private int mCurCnt;
    private int mCurLen;
    private int mTotalCnt;
    private int mTotalLen;
    private String mEname;
    private byte[] mMd5All = new byte[16];
    private byte[] mCmprsData;

    private Context mContext;
    private QRCodeDecoder mDecoder;

    public ScanQRManager(Context context) {
        mContext = context;
        mDecoder = new QRCodeDecoder();
        //QRCodeDecoder.setCanvas(new MyDebugCanvas());
        clear();
    }

    public void clear() {
        mInProgress = true;
        mCurCnt = 0;
        mCurLen = 0;
        mTotalCnt = 0;
        mTotalLen = 0;
        mEname = null;
        mCmprsData = null;
    }

    public boolean inProgress() {
        return mInProgress;
    }

    public int getCurrentQRNumber() {
        return mCurCnt;
    }

    public int getTotalQRNumber() {
        return mTotalCnt;
    }

    public String getNameWithType() {
        return mEname;
    }

    public byte[] getData() {
        return mCmprsData;
    }

    public boolean executeScan(QRCodeImage image) {
        myLog("scanQR()");
        byte[] qrData = null;
        try {
            qrData = mDecoder.decode(image);
        } catch (DecodingFailedException e) {
            myLog("Not QR code.");
            return false;
        }
        if (qrData.length <= 36 || qrData[0] != 'P' || qrData[1] != 'T') {
            Utils.showToast(mContext, R.string.qr_err_invalid);
            myLog("Strange data.");
            return false; 
        }
        byte[] md5each = new byte[16];
        byte[] md5 = new byte[16];
        byte[] partData = new byte[qrData.length - 36];
        System.arraycopy(qrData, 4, md5each, 0, 16);
        System.arraycopy(qrData, 20, md5, 0, 16);
        System.arraycopy(qrData, 36, partData, 0, partData.length);
        if (!Arrays.equals(md5each, Utils.getMD5(partData))) {
            Utils.showToast(mContext, R.string.qr_err_corrupt);
            myLog("Hash of this data is wrong.");
            return false;
        }

        /*  First QR code  */
        if (mTotalCnt == 0) {
            if (partData.length <= PTCFile.HEADLEN_CMPRSDATA) {
                Utils.showToast(mContext, R.string.qr_err_corrupt);
                myLog("Too short as 1st QR code.");
                return false;
            }
            if (qrData[2] != 1) {
                Utils.showToast(mContext, String.format(
                        mContext.getString(R.string.qr_err_order), qrData[2]));
                myLog("Wrong number for current data.");
                return false;
            }
            mTotalLen = Utils.extractValue(partData, 12, 4) + PTCFile.HEADLEN_CMPRSDATA;
            if (mTotalLen < 0 || mTotalLen > PTCFile.WORKLEN_CMPRSDATA) {
                Utils.showToast(mContext, R.string.qr_err_corrupt);
                myLog("Strange total length.");
                return false;
            }
            mCurCnt = 0;
            mTotalCnt = qrData[3];
            System.arraycopy(qrData, 20, mMd5All, 0, 16);
            mEname = Utils.extractString(partData, 9, 3).concat(":")
                    .concat(Utils.extractString(partData, 0, 8));
            mCmprsData = new byte[mTotalLen];
        }

        /*  Append data after check  */
        if (qrData[3] != mTotalCnt || !Arrays.equals(md5, mMd5All)) {
            Utils.showToast(mContext, R.string.qr_err_different);
            myLog("Unsuitable for current data.");
            return false;
        }
        if (qrData[2] != mCurCnt + 1) {
            Utils.showToast(mContext, String.format(
                    mContext.getString(R.string.qr_err_order), qrData[2]));
            myLog("Wrong number for current data.");
            return false;
        }
        if (mCurLen + partData.length > mTotalLen) {
            myLog("Data is too much.");
            mInProgress = false;
            return false;
        }
        System.arraycopy(partData, 0, mCmprsData, mCurLen, partData.length);
        mCurCnt++;
        mCurLen += partData.length;

        /*  Last QR code  */
        if (mCurCnt == mTotalCnt) {
            if (mCurLen < mTotalLen) {
                myLog("Data isn't enough.");
                mInProgress = false;
                return false;
            }
            if (!Arrays.equals(md5, Utils.getMD5(mCmprsData))) {
                myLog("Hash of whole data is wrong.");
                mInProgress = false;
                return false;
            }
            myLog("Completed!!");
            mInProgress = false;
            return true;
        }
        myLog("Success!");
        return true;
    }

    private void myLog(String str) {
        //Log.i("CHRED", str);
    }

}
