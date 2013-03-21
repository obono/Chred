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

import jp.sourceforge.qrcode.QRCodeDecoder;
import jp.sourceforge.qrcode.data.QRCodeImage;
import jp.sourceforge.qrcode.exception.DecodingFailedException;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ScaleGestureDetector.OnScaleGestureListener;

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
    private byte[] mMd5All = new byte[16];
    private byte[] mCmprsData;

    private boolean mIsOnResult;
    private Uri mUri;
    private Bitmap mBitmap;
    private QRImageMediator mQrImage;

    private Handler mTimeoutHandler = new Handler() {
        @Override  
        public void dispatchMessage(Message msg) {
            if (msg.what == MSG_EXECSCAN) {
                executeScan();
            }
        }
    };

    /*-----------------------------------------------------------------------*/

    class MyView extends View implements OnScaleGestureListener {

        private boolean mIsMoving;
        private float mDrawX;
        private float mDrawY;
        private float mDrawScale = 1f;
        private float mFocusX;
        private float mFocusY;

        private Paint mPaint = new Paint();
        private ScaleGestureDetector mGestureDetector;

        public MyView(Context context) {
            super(context);
            mPaint.setColor(Color.RED);
            mPaint.setStyle(Paint.Style.STROKE);
            mGestureDetector = new ScaleGestureDetector(context, this);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (mBitmap != null) {
                canvas.save();
                canvas.scale(mDrawScale, mDrawScale);
                canvas.drawBitmap(mBitmap, mDrawX, mDrawY, null);
                canvas.restore();
                canvas.drawRect(0, 0, 200, 200, mPaint);
                mQrImage.setTargetArea((int) -mDrawX, (int) -mDrawY,
                        (int) (200 / mDrawScale), (int) (200 / mDrawScale));
            }
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            mDrawX = 0f;
            mDrawY = 0f;
            mDrawScale = 1f;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            boolean ret = false;
            float x = event.getX();
            float y = event.getY();
            mGestureDetector.onTouchEvent(event);
            switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mIsMoving = true;
                mFocusX = x;
                mFocusY = y;
                ret = true;
                break;
            case MotionEvent.ACTION_MOVE:
                if (mIsMoving) {
                    mDrawX += (x - mFocusX) / mDrawScale;
                    mDrawY += (y - mFocusY) / mDrawScale;
                    mFocusX = x;
                    mFocusY = y;
                    invalidate();
                    ret = true;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (mIsMoving) {
                    mIsMoving = false;
                    resetTimer();
                    ret = true;
                }
                break;
            }
            return ret;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mDrawScale *= detector.getScaleFactor();
            invalidate();
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            mIsMoving = false;
            mFocusX = detector.getFocusX();
            mFocusY = detector.getFocusY();
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            resetTimer();
        }
    }

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
        setContentView(new MyView(this));
        mQrImage = new QRImageMediator();
        mTotalCnt = 0;
    }

    @Override
    protected void onResume() {
        if (mUri != null) {
            try {
                InputStream in = getContentResolver().openInputStream(mUri);
                mBitmap = BitmapFactory.decodeStream(in);
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            if (mIsOnResult) {
                setCanceledResult();
            } else {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT/*ACTION_PICK*/);
                intent.setType("image/*");
                startActivityForResult(intent, REQUEST_ID_CHOOSE_FILE);
            }
        }
        super.onResume();
        mIsOnResult = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mTimeoutHandler.removeMessages(MSG_EXECSCAN);
        if (mBitmap != null) {
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

    /*-----------------------------------------------------------------------*/

    private void resetTimer() {
        myLog("resetTimer()");
        mTimeoutHandler.removeMessages(MSG_EXECSCAN);
        mTimeoutHandler.sendEmptyMessageDelayed(MSG_EXECSCAN, MSEC_TIMEOUT_EXECSCAN);
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
            // get name
            // get type
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
        mCurLen += partData.length;

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
    }
}
