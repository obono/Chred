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

import com.obnsoft.view.MagnifyView;

import jp.sourceforge.qrcode.data.QRCodeImage;
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
import android.view.MotionEvent;
import android.view.View;

public class ScanQRGalleryActivity extends ScanQRActivity {

    private static final int REQUEST_ID_CHOOSE_FILE = 1;
    private static final int MSG_EXECSCAN = 1;
    private static final int MSEC_TIMEOUT_EXECSCAN = 500;

    private boolean mIsOnResult;
    private Uri mUri;
    private Bitmap mBitmap;
    private RectF mWorkRect = new RectF();
    private QRImageMediator mQrImage;
    private MagnifyView mQrView;
    private View mQrFrame;
    private GradientDrawable mQrFrameDrawable;
    private int mQrFrameSize;

    private Handler mTimeoutHandler = new Handler() {
        @Override
        public void dispatchMessage(Message msg) {
            if (msg.what == MSG_EXECSCAN) {
                boolean ret = mQRMan.executeScan(mQrImage);
                if (mQRMan.inProgress()) {
                    if (ret) {
                        setInformation();
                    }
                    mQrFrameDrawable.setStroke(mQrFrameSize, ret ? COLOR_SUCCESS : COLOR_FAIL);
                } else {
                    if (ret) {
                        setSuccessResult(mQRMan.getData());
                    } else {
                        setFailedResult();
                    }
                }
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

    /*-----------------------------------------------------------------------*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.scanqr);
        super.onCreate(savedInstanceState);

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
        Intent intent = new Intent(Intent.ACTION_PICK);
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

}
