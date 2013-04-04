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

import android.content.res.Configuration;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.RelativeLayout;

public class ScanQRCameraActivity extends ScanQRActivity implements SurfaceHolder.Callback {

    private Camera mCamera;
    private Camera.Size mCameraSize;
    private SurfaceView mQrView;

    /*-----------------------------------------------------------------------*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.scanqr_cam);
        super.onCreate(savedInstanceState);

        mQrView = (SurfaceView) findViewById(R.id.view_qrimage);
        SurfaceHolder holder = mQrView.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
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
            mCamera = Camera.open();
            mCamera.setPreviewDisplay(holder);
            int size = mQrFrame.getWidth();
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
        mCamera.stopPreview();
        mCamera.release();
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
        }
    }

}
