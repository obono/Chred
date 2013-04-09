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
import java.util.List;

import jp.sourceforge.qrcode.data.QRCodeImage;

import android.content.res.Configuration;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RelativeLayout;

public class ScanQRCameraActivity extends ScanQRActivity implements SurfaceHolder.Callback {

    private static final int MSG_SCAN_SUCCESS = 1;
    private static final int MSG_SCAN_FAIL = 2;
    private static final int MSEC_TIMEOUT_EXECSCAN = 500;

    private boolean mFocusing;
    private Camera mCamera;
    private Camera.Size mCameraSize;
    private SurfaceView mQrView;
    private ScanQRTask mTask;

    private Handler mMsgHandler = new Handler() {
        @Override
        public void dispatchMessage(Message msg) {
            if (msg.what == MSG_SCAN_SUCCESS || msg.what == MSG_SCAN_FAIL) {
                boolean ret = (msg.what == MSG_SCAN_SUCCESS);
                if (mQRMan.inProgress()) {
                    if (ret) {
                        setInformation();
                    }
                    if (msg.obj != null) {
                        Utils.showToast(ScanQRCameraActivity.this, (String) msg.obj);
                    }
                    if (ret || msg.obj != null) {
                        setFrameResultColor(ret);
                    } else {
                        setFrameDefaultColor();
                    }
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

    private Camera.AutoFocusCallback mAutoFocusListener = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            camera.autoFocus(null);
            mFocusing = false;
        }
    };
    /*-----------------------------------------------------------------------*/

    class ScanQRTask implements Runnable, Camera.PreviewCallback, QRCodeImage {

        private boolean mLoop;
        private int mSize;
        private byte[] mData;

        public ScanQRTask(int size) {
            mLoop = true;
            mSize = size;
        }

        public void finish() {
            mLoop = false;
        }

        @Override
        public void run() {
            mCamera.setPreviewCallbackWithBuffer(this);
            while (mLoop) {
                int bufSize = mCameraSize.width * mCameraSize.height * 2;
                mCamera.addCallbackBuffer(new byte[bufSize]);
                while (mData == null && mLoop) {
                    try {
                        Thread.sleep(MSEC_TIMEOUT_EXECSCAN);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (mData != null) {
                    boolean ret = mQRMan.executeScan(this);
                    Message msg = Message.obtain(null,
                            ret ? MSG_SCAN_SUCCESS : MSG_SCAN_FAIL, mQRMan.getMessage());
                    mMsgHandler.sendMessage(msg);
                    mData = null;
                }
            }
        }

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            if (mData == null) {
                mData = data;
            }
        }

        @Override
        public int getWidth() {
            return mSize;
        }

        @Override
        public int getHeight() {
            return mSize;
        }

        @Override
        public int getPixel(int x, int y) {
            x += (mCameraSize.width - mSize) / 2;
            y += (mCameraSize.height - mSize) / 2;
            byte b = mData[y * mCameraSize.width + x];
            int val = (b < 0) ? 256 + b : b;
            return 0x010101 * val;
        }
    }

    /*-----------------------------------------------------------------------*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.scanqr_cam);
        super.onCreate(savedInstanceState);

        mQrView = (SurfaceView) findViewById(R.id.view_qrimage);
        mQrView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCamera != null && !mFocusing) {
                    mFocusing = true;
                    mCamera.autoFocus(mAutoFocusListener);
                }
            }
        });
        SurfaceHolder holder = mQrView.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    protected void onResume() {
        if (mCamera != null) {
            mCamera.startPreview();
            if (mTask == null) {
                mTask = new ScanQRTask(mQrFrame.getWidth());
                new Thread(mTask).start();
            }
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mTask != null) {
            mTask.finish();
            mTask = null;
        }
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setCameraOrientation();
    }

    /*-----------------------------------------------------------------------*/

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            int size = mQrFrame.getWidth();
            mCamera = Camera.open();
            mCamera.setPreviewDisplay(holder);
            Camera.Parameters cp = mCamera.getParameters();
            List<Camera.Size> sizeList = cp.getSupportedPreviewSizes();
            mCameraSize = sizeList.get(0);
            for (Camera.Size s : sizeList) {
                if (s.width >= size && s.height >= size &&
                        s.width * s.height < mCameraSize.width * mCameraSize.height) {
                    mCameraSize = s;
                }
            }
            cp.setPreviewSize(mCameraSize.width, mCameraSize.height);
            cp.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
            mCamera.setParameters(cp);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        setCameraOrientation();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mTask != null) {
            mTask.finish();
            mTask = null;
        }
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }

    /*-----------------------------------------------------------------------*/

    private void setCameraOrientation() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 90;
        switch (rotation) {
            case Surface.ROTATION_0:   degrees = 90;  break;
            case Surface.ROTATION_90:  degrees = 0;   break;
            case Surface.ROTATION_180: degrees = 270; break;
            case Surface.ROTATION_270: degrees = 180; break;
        }
        int w, h;
        if (degrees % 180 == 90) {
            w = mCameraSize.height;
            h = mCameraSize.width;
        } else {
            w = mCameraSize.width;
            h = mCameraSize.height;
        }
        if (w != mQrView.getWidth() || h != mQrView.getHeight()) {
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(w, h);
            lp.addRule(RelativeLayout.CENTER_IN_PARENT);
            mQrView.setLayoutParams(lp);
        } else {
            mCamera.stopPreview();
            mCamera.setDisplayOrientation(degrees);
            mCamera.startPreview();
            if (mTask == null) {
                mTask = new ScanQRTask(mQrFrame.getWidth());
                new Thread(mTask).start();
            }
        }
    }

}
