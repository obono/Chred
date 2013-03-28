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
import java.util.Arrays;

import com.obnsoft.view.MagnifyView;

import jp.sourceforge.qrcode.QRCodeDecoder;
import jp.sourceforge.qrcode.data.QRCodeImage;
import jp.sourceforge.qrcode.exception.DecodingFailedException;
import jp.sourceforge.qrcode.util.DebugCanvasAdapter;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

public class ScanQRActivity extends Activity {

    public static final String INTENT_EXTRA_DATA = "data";
    public static final int RESULT_FAILED = RESULT_FIRST_USER;

    private static final int REQUEST_ID_CHOOSE_FILE = 1;
    private static final int MSG_EXECSCAN = 1;
    private static final int MSEC_TIMEOUT_EXECSCAN = 500;
    private static final int COLOR_DEFAULT = Color.argb(128, 255, 0, 0);
    private static final int COLOR_FAIL = Color.rgb(192, 192, 0);
    private static final int COLOR_SUCCESS = Color.rgb(0, 128, 255);

    private int mCurCnt;
    private int mCurLen;
    private int mTotalCnt;
    private int mTotalLen;
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
    private GradientDrawable mQrFrameDrawable;
    private int mQrFrameSize;
    private TextView mTextMsg;
    private TextView mTextInfo;

    private Handler mTimeoutHandler = new Handler() {
        @Override
        public void dispatchMessage(Message msg) {
            if (msg.what == MSG_EXECSCAN) {
                boolean ret = executeScan();
                mQrFrameDrawable.setStroke(mQrFrameSize, ret ? COLOR_SUCCESS : COLOR_FAIL);
            }
        }
    };

    /*-----------------------------------------------------------------------*/

    class QRImageMediator implements QRCodeImage {
        private static final int MAX_SIZE = 512;
        private static final int MIN_SIZE = 256;
        private int mX, mY, mW, mH;
        private int mSkip = 1;
        private int mScale = 1;
        public void setTargetArea(int x, int y, int w, int h) {
            mX = x;
            mY = y;
            mW = w;
            mH = h;
            mSkip = 1;
            mScale = 1;
            while (getWidth() > MAX_SIZE || getHeight() > MAX_SIZE) {
                mSkip++;
            }
            while (getWidth() < MIN_SIZE && getHeight() < MIN_SIZE) {
                mScale++;
            }
        }
        @Override
        public int getWidth() {
            return mW * mScale / mSkip;
        }
        @Override
        public int getHeight() {
            return mH * mScale / mSkip;
        }
        @Override
        public int getPixel(int x, int y) {
            x = x * mSkip / mScale + mX;
            y = y * mSkip / mScale + mY;
            if (mBitmap != null && x >= 0 && y >= 0 &&
                    x < mBitmap.getWidth() && y < mBitmap.getHeight()) {
                return mBitmap.getPixel(x, y);
            } else {
                return Color.BLACK;
            }
        }
    }

    class MyDebugCanvas extends DebugCanvasAdapter {
        @Override
        public void println(String s) {
            Log.d("CHRED", s);
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
        mQrView.setScaleRange(.25f, 4f);
        mQrView.setFrameColor(Color.TRANSPARENT);

        mQrFrame = (View) findViewById(R.id.view_qrframe);
        mQrFrameDrawable = new GradientDrawable();
        mQrFrameDrawable.setColor(Color.TRANSPARENT);
        mQrFrameSize = Utils.dp2px(this, 4);
        mQrFrameDrawable.setStroke(mQrFrameSize, COLOR_DEFAULT);
        mQrFrame.setBackgroundDrawable(mQrFrameDrawable);

        mTextMsg = (TextView) findViewById(R.id.text_qrmsg);
        mTextInfo = (TextView) findViewById(R.id.text_qrinfo);

        mCurCnt = 0;
        mTotalCnt = 0;
        setInformation();
    }

    @Override
    protected void onResume() {
        if (mIsOnResult) {
            mIsOnResult = false;
        } else {
            requsetFileFromGallery();
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        stopTimer();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mBitmap != null) {
            mQrView.setBitmap(null, true);
            mBitmap.recycle();
            mBitmap = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
        case REQUEST_ID_CHOOSE_FILE:
            if (resultCode == RESULT_OK) {
                mUri = intent.getData();
                if (mUri != null) {
                    if (mBitmap != null) {
                        mBitmap.recycle();
                    }
                    try {
                        InputStream in = getContentResolver().openInputStream(mUri);
                        mBitmap = BitmapFactory.decodeStream(in);
                        in.close();
                        mQrView.setBitmap(mBitmap, true);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else if (mBitmap == null) {
                setCanceledResult();
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
                float ratio = mBitmap.getWidth() / mWorkRect.width();
                int size = Math.round(mQrFrame.getWidth() * ratio);
                mQrImage.setTargetArea((int) ((mQrFrame.getLeft() - mWorkRect.left) * ratio),
                        (int) ((mQrFrame.getTop() - mWorkRect.top) * ratio), size, size);
                startTimer();
                break;
            case MotionEvent.ACTION_DOWN:
                stopTimer();
                mQrFrameDrawable.setStroke(mQrFrameSize, COLOR_DEFAULT);
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

    private boolean executeScan() {
        byte[] qrData = scanQR(mQrImage);
        if (qrData == null) {
            myLog("Not QR-code.");
            return false;
        }
        if (qrData.length <= 36 || qrData[0] != 'P' || qrData[1] != 'T') {
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
            myLog("Hash of this data is wrong.");
            return false;
        }

        /*  First QR-code  */
        if (mTotalCnt == 0) {
            if (partData.length <= PTCFile.HEADLEN_CMPRSDATA) {
                myLog("Too short as 1st QR-code.");
                return false;
            }
            mTotalLen = Utils.extractValue(partData, 12, 4) + PTCFile.HEADLEN_CMPRSDATA;
            if (mTotalLen < 0 || mTotalLen > PTCFile.WORKLEN_CMPRSDATA) {
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
            myLog("Unsuitable for current data.");
            return false;
        }
        if (qrData[2] != mCurCnt + 1) {
            myLog("Wrong number for current data.");
            return false;
        }
        if (mCurLen + partData.length > mTotalLen) {
            myLog("Data is too much.");
            setFailedResult();
            return false;
        }
        System.arraycopy(partData, 0, mCmprsData, mCurLen, partData.length);
        mCurCnt++;
        mCurLen += partData.length;
        setInformation();

        /*  Last QR-code  */
        if (mCurCnt == mTotalCnt) {
            if (mCurLen < mTotalLen) {
                myLog("Data isn't enough.");
                setFailedResult();
                return false;
            }
            if (!Arrays.equals(md5, Utils.getMD5(mCmprsData))) {
                myLog("Hash of whole data is wrong.");
                setFailedResult();
                return false;
            }
            myLog("Completed!!");
            setSuccessResult(mCmprsData);
            return true;
        }
        myLog("Success!");
        return true;
    }

    private byte[] scanQR(QRCodeImage image) {
        QRCodeDecoder decoder = new QRCodeDecoder();
        QRCodeDecoder.setCanvas(new MyDebugCanvas());
        byte[] ret = null;
        myLog("scanQR()");
        try {
            ret = decoder.decode(image);
        } catch (DecodingFailedException e) {
            ret = null;
        }
        return ret;
    }

    private void setInformation() {
        if (mTotalCnt == 0 || mCurCnt < mTotalCnt) {
            mTextMsg.setText(String.format(getString(R.string.qr_msg), mCurCnt + 1));
        }
        if (mTotalCnt > 0) {
            mTextInfo.setText(String.format("%s(%d/%d)", mEname, mCurCnt, mTotalCnt));
        }

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
        Log.i("CHRED", str);
    }
}
