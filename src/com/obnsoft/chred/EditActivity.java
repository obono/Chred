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

import com.obnsoft.view.MagnifyView;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

public class EditActivity extends Activity implements MagnifyView.EventHandler {

    private Bitmap mBitmap;
    private MyApplication mApp;
    private MagnifyView mMgView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit);

        mApp = (MyApplication) getApplication();
        ChrData chrData = mApp.mChrData;
        chrData.setTargetSize(chrData.getTargetSizeH(), chrData.getTargetSizeV());

        mBitmap = Bitmap.createBitmap(chrData.getTargetSizeH() * ChrData.UNIT_SIZE,
                chrData.getTargetSizeV() * ChrData.UNIT_SIZE, Bitmap.Config.RGB_565);
        chrData.drawTarget(mBitmap, 0, mApp.mPalIdx);

        mMgView = (MagnifyView) findViewById(R.id.view_edit);
        mMgView.setBitmap(mBitmap);
        mMgView.setGridColor(Color.GRAY, false);

    }

    @Override
    protected void onDestroy() {
        mMgView.setBitmap(null);
        mBitmap.recycle();
        mBitmap = null;
        super.onDestroy();
    }

    @Override
    public boolean onTouchEventUnit(MotionEvent ev, int x, int y) {
        int c = 10;
        if (x >= 0 && y >= 0 && x < mBitmap.getWidth() && y < mBitmap.getHeight()) {
            mApp.mChrData.setTargetDot(0, x, y, c);
            mBitmap.setPixel(x, y, mApp.mColData.getColor(mApp.mPalIdx, c));
            mMgView.invalidateUnit(x, y);
        }
        return true;
    }

    public void onClickMoveButton(View v) {
        mMgView.setEventHandler(null);
    }

    public void onClickDrawButton(View v) {
        mMgView.setEventHandler(this);
    }

}
