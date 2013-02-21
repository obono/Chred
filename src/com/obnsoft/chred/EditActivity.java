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
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;

public class EditActivity extends Activity
        implements MagnifyView.EventHandler, OnItemSelectedListener {

    private int mColIdx;

    private Bitmap mBitmap;

    private MyApplication mApp;
    private Spinner mPalSpinner;
    private MagnifyView mMgView;

    /*-----------------------------------------------------------------------*/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit);

        mApp = (MyApplication) getApplication();
        mMgView = (MagnifyView) findViewById(R.id.view_edit);
        mMgView.setGridColor(Color.GRAY, false);

        Spinner spinner = (Spinner) findViewById(R.id.spin_color);
        spinner.setOnItemSelectedListener(this);
        mPalSpinner = (Spinner) findViewById(R.id.spin_palette);
        mPalSpinner.setAdapter(mApp.mPalAdapter);
        mPalSpinner.setOnItemSelectedListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPalSpinner.setSelection(mApp.mPalIdx);

        ChrData chrData = mApp.mChrData;
        mBitmap = Bitmap.createBitmap(chrData.getTargetSizeH() * ChrData.UNIT_SIZE,
                chrData.getTargetSizeV() * ChrData.UNIT_SIZE, Bitmap.Config.ARGB_8888);
        chrData.drawTarget(mBitmap, mApp.mChrIdx, mApp.mPalIdx);
        mMgView.setBitmap(mBitmap);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMgView.setBitmap(null);
        mBitmap.recycle();
        mBitmap = null;
    }

    /*-----------------------------------------------------------------------*/

    @Override
    public boolean onTouchEventUnit(MotionEvent ev, int x, int y) {
        if (x >= 0 && y >= 0 && x < mBitmap.getWidth() && y < mBitmap.getHeight()) {
            mApp.mChrData.setTargetDot(mApp.mChrIdx, x, y, mColIdx);
            mBitmap.setPixel(x, y, mApp.mColData.getColor(mApp.mPalIdx, mColIdx));
            mMgView.invalidateUnit(x, y);
        }
        return true;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        Spinner spinner = (Spinner) parent;
        if (spinner == mPalSpinner) {
            mApp.mPalIdx = spinner.getSelectedItemPosition();
            mApp.mChrData.drawTarget(mBitmap, mApp.mChrIdx, mApp.mPalIdx);
            mMgView.invalidate();
        } else {
            mColIdx = spinner.getSelectedItemPosition();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Do nothing
    }

    /*-----------------------------------------------------------------------*/

    public void onClickMoveButton(View v) {
        mMgView.setEventHandler(null);
    }

    public void onClickDrawButton(View v) {
        mMgView.setEventHandler(this);
    }

}
