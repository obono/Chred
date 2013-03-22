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
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import com.obnsoft.view.MagnifyView;

import jp.sourceforge.qrcode.QRCodeDecoder;
import jp.sourceforge.qrcode.data.QRCodeImage;
import jp.sourceforge.qrcode.exception.DecodingFailedException;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;

public class ScanQRActivity extends Activity {

    public static final String INTENT_EXTRA_DATA = "data";
    public static final int RESULT_FAILED = RESULT_FIRST_USER;

    private static final int REQUEST_ID_CHOOSE_FILE = 1;
    private static final int MSG_EXECSCAN = 1;
    private static final int MSEC_TIMEOUT_EXECSCAN = 1000;

    private int mCurCnt;
    private int mCurLen;
    private int mTotalCnt;
    private int mTotalLen;
    private int mFinalLen;
    private String mEname;
    private byte[] mMd5All = new byte[16];
    private byte[] mCmprsData;

    private boolean mIsOnResult;
    private Uri mUri;
    private Bitmap mBitmap;
    private RectF mWorkRect = new RectF();
    private QRImageMediator mQrImage;
    private MagnifyView mQrView;
    private View mQrFrame;

    private Handler mTimeoutHandler = new Handler() {
        @Override
        public void dispatchMessage(Message msg) {
            if (msg.what == MSG_EXECSCAN) {
                executeScan();
                AlphaAnimation anim = new AlphaAnimation(0f, 1f);
                anim.setDuration(1000);
                anim.setFillAfter(true);
                mQrFrame.startAnimation(anim);
            }
        }
    };

    /*-----------------------------------------------------------------------*/

    class QRImageMediator implements QRCodeImage {
        private int mX, mY, mW, mH;
        public void setTargetArea(int x, int y, int w, int h) {
            mX = x;
            mY = y;
            mW = w;
            mH = h;
        }
        @Override
        public int getWidth() {
            return mW;
        }
        @Override
        public int getHeight() {
            return mH;
        }
        @Override
        public int getPixel(int x, int y) {
            x += mX;
            y += mY;
            if (mBitmap != null && x >= 0 && y >= 0 &&
                    x < mBitmap.getWidth() && y < mBitmap.getHeight()) {
                return mBitmap.getPixel(x, y) & 0xFFFFFF;
            } else {
                return 0;
            }
        }
    }

    /*-----------------------------------------------------------------------*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scanqr);
        mQrImage = new QRImageMediator();
        mQrView = (MagnifyView) findViewById(R.id.view_qrimage);
        mQrView.setScrollable(true);
        mQrView.setScaleRange(.125f, 4f);
        mQrView.setFrameColor(Color.TRANSPARENT);
        mQrFrame = (View) findViewById(R.id.view_qrframe);
        mTotalCnt = 0;
    }

    @Override
    protected void onResume() {
        if (mUri != null) {
            try {
                InputStream in = getContentResolver().openInputStream(mUri);
                mBitmap = BitmapFactory.decodeStream(in);
                in.close();
                mQrView.setBitmap(mBitmap, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            if (mIsOnResult) {
                setCanceledResult();
            } else {
                requsetFileFromGallery();
            }
        }
        super.onResume();
        mIsOnResult = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopTimer();
        if (mBitmap != null) {
            mQrView.setBitmap(null, true);
            mBitmap.recycle();
            mBitmap = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
        case REQUEST_ID_CHOOSE_FILE:
            if (resultCode == RESULT_OK) {
                mUri = intent.getData();
            }
            break;
        }
        mIsOnResult = true;  // This function is called before onResume()
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean ret = super.dispatchTouchEvent(ev);
        if (mBitmap != null) {
            switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_UP:
                mQrView.getBitmapDrawRect(mWorkRect);
                float scale = mBitmap.getWidth() / mWorkRect.width();
                int size = Math.round(mQrFrame.getWidth() * scale);
                mQrImage.setTargetArea((int) ((mQrFrame.getLeft() - mWorkRect.left) / scale),
                        (int) ((mQrFrame.getTop() - mWorkRect.top) / scale), size, size);
                startTimer();
                break;
            case MotionEvent.ACTION_DOWN:
                stopTimer();
                break;
            }
        }
        return ret;
    }

    public void onClickOtherImage(View v) {
        requsetFileFromGallery();
    }

    /*-----------------------------------------------------------------------*/

    private void requsetFileFromGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT/*ACTION_PICK*/);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_ID_CHOOSE_FILE);
    }

    private void startTimer() {
        stopTimer();
        mTimeoutHandler.sendEmptyMessageDelayed(MSG_EXECSCAN, MSEC_TIMEOUT_EXECSCAN);
    }

    private void stopTimer() {
        mTimeoutHandler.removeMessages(MSG_EXECSCAN);
    }

    private void executeScan() {
        byte[] qrData = scanQR(mQrImage);
        if (qrData == null) {
            myLog("Not QR-code.");
            return;
        }
        if (qrData.length <= 36 || qrData[0] != 'P' || qrData[1] != 'T') {
            myLog("Strange data.");
            return; 
        }
        byte[] md5each = new byte[16];
        byte[] md5 = new byte[16];
        byte[] partData = new byte[qrData.length - 36];
        System.arraycopy(qrData, 4, md5each, 0, 16);
        System.arraycopy(qrData, 20, md5, 0, 16);
        System.arraycopy(qrData, 36, partData, 0, partData.length);
        if (!md5each.equals(Utils.getMD5(partData))) {
            myLog("Hash of this data is wrong.");
            return;
        }

        /*  First QR-code  */
        if (mTotalCnt == 0) {
            if (partData.length <= 20) {
                myLog("Strange data as first.");
                return;
            }
            mCurCnt = 0;
            mTotalCnt = qrData[3];
            System.arraycopy(qrData, 20, mMd5All, 0, 16);
            mEname = Utils.extractString(partData, 9, 3).concat(":")
                    .concat(Utils.extractString(partData, 0, 8));
            mTotalLen = Utils.extractValue(partData, 12, 4);
            mFinalLen = Utils.extractValue(partData, 16, 4);
            mCmprsData = new byte[mTotalLen];
        }

        /*  Append data after check  */
        if (qrData[3] != mTotalCnt || !md5.equals(mMd5All)) {
            myLog("Unsuitable for current data.");
            return;
        }
        if (qrData[2] != mCurCnt + 1) {
            myLog("Wrong number for current data.");
            return;
        }
        if (mCurLen + partData.length > mTotalLen) {
            myLog("Data is too much.");
            setFailedResult();
            return;
        }
        System.arraycopy(partData, 0, mCmprsData, mCurLen, partData.length);
        mCurCnt++;
        mCurLen += partData.length;
        TextView tv = (TextView) findViewById(R.id.text_qrinfo);
        tv.setText(String.format("%s (%d/%d)", mEname, mCurCnt, mTotalCnt));

        /*  Last QR-code  */
        if (++mCurCnt == mTotalCnt) {
            if (mCurLen != mTotalLen) {
                myLog("Data isn't enough.");
                setFailedResult();
                return;
            }
            if (!md5.equals(Utils.getMD5(mCmprsData))) {
                myLog("Hash of whole data is wrong.");
                setFailedResult();
                return;
            }
            Inflater expander = new Inflater();
            expander.setInput(mCmprsData);
            byte[] data = new byte[mFinalLen];
            int finalLen = 0;
            try {
                finalLen = expander.inflate(data);
            } catch (DataFormatException e) {
                e.printStackTrace();
            }
            expander.end();
            if (finalLen != mFinalLen) {
                myLog("Length of expanded data is wrong.");
                setFailedResult();
                return;
            }
            myLog("Completed!!");
            setSuccessResult(data);
            return;
        }
        myLog("Success!");
        return;
    }

    private byte[] scanQR(QRCodeImage image) {
        QRCodeDecoder decoder = new QRCodeDecoder();
        byte[] ret = null;
        myLog("scanQR()");
        try {
            ret = decoder.decode(image);
        } catch (DecodingFailedException e) {
            ret = null;
        }
        return ret;
    }

    private void setSuccessResult(byte[] data) {
        Intent intent = new Intent();
        intent.putExtra(INTENT_EXTRA_DATA, data);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void setFailedResult() {
        Intent intent = new Intent();
        setResult(RESULT_FAILED, intent);
        finish();
    }

    private void setCanceledResult() {
        Intent intent = new Intent();
        setResult(RESULT_CANCELED, intent);
        finish();
    }

    private void myLog(String str) {
        Log.d("CHRED", str);
        TextView tv = (TextView) findViewById(R.id.text_qrmsg);
        tv.setText(str);
    }
}
