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

import com.obnsoft.view.ColorPickerInterface;

import android.app.Activity;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;

public class PaletteActivity extends Activity implements OnItemSelectedListener,
        ColorPickerInterface.OnColorChangedListener, OnClickListener {

    private int mColIdx;

    private Paint mPaint = new Paint();

    private MyApplication mApp;
    private PaletteView mPalView;
    private PaletteView.ColorView mColView;
    private Spinner mPalSpinner;
    private ColorPickerInterface mColPicker;

    /*-----------------------------------------------------------------------*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.palette);

        mApp = (MyApplication) getApplication();
        mPaint.setStyle(Paint.Style.STROKE);

        mPalView = (PaletteView) findViewById(R.id.palview);
        mPalView.setPalette(mApp.mColData, mApp.mPalIdx);
        mColView = mPalView.setSelection(0);
        mPalView.setOnClickListener(this);

        mPalSpinner = (Spinner) findViewById(R.id.spin_palette);
        mPalSpinner.setAdapter(mApp.mPalAdapter);
        mPalSpinner.setOnItemSelectedListener(this);

        mColPicker = (ColorPickerInterface) findViewById(R.id.colpicker_hsv);
        mColPicker.setListener(this);
    }

    @Override
    protected void onResume() {
        if (mPalSpinner.getSelectedItemPosition() != mApp.mPalIdx) {
            mPalSpinner.setSelection(mApp.mPalIdx);
        } else {
            mPalView.setPalette(mApp.mColData, mApp.mPalIdx);
        }
        super.onResume();
    }

    /*-----------------------------------------------------------------------*/

    @Override
    public void onClick(View view) {
        mColView = (PaletteView.ColorView) view;
        mColIdx = mColView.getIndex();
        mPalView.setSelection(mColIdx);
        mColPicker.setColor(mApp.mColData.getColor(mApp.mPalIdx, mColIdx));
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        Spinner spinner = (Spinner) parent;
        if (spinner == mPalSpinner) {
            mApp.mPalIdx = spinner.getSelectedItemPosition();
            mPalView.setPalette(mApp.mColData, mApp.mPalIdx);
        }
    }
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Do nothing
    }

    @Override
    public void colorChanged(int color) {
        int pal = mApp.mPalIdx;
        mApp.mColData.setColor(pal, mColIdx, color);
        color = mApp.mColData.getColor(pal, mColIdx);
        mColView.setBackgroundColor(color);
    }

}
