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
import com.obnsoft.view.HSVColorPickerView;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Spinner;
import android.widget.TextView;

public class PaletteActivity extends Activity
        implements OnItemSelectedListener, ColorPickerInterface.OnColorChangedListener {

    private int mColIdx;

    private Paint mPaint = new Paint();

    private MyApplication mApp;
    private ColorPickerInterface mColPicker;
    private Spinner mPalSpinner;
    private ColorView[] mColors = new ColorView[ColData.COLS_PER_PAL];

    /*-----------------------------------------------------------------------*/

    class ColorView extends TextView implements OnClickListener {
        private int mIdx;
        public ColorView(Context context, int idx) {
            super(context);
            setGravity(Gravity.CENTER);
            setOnClickListener(this);
            mIdx = idx;
        }
        @Override
        public void draw(Canvas canvas) {
            super.draw(canvas);
            if (isSelected()) {
                for (int i = 0; i < 3; i++) {
                    mPaint.setColor((i == 1) ? Color.BLACK : Color.WHITE);
                    canvas.drawRect(i, i, getWidth() - i - 1, getHeight() - i - 1, mPaint);
                }
            }
        }
        @Override
        public void setBackgroundColor(int color) {
            super.setBackgroundColor(color);
            setText(String.format("#%06X", color & 0xFFFFFF));
            setTextColor((HSVColorPickerView.calcBrightness(color) < 0.5) ?
                    Color.WHITE : Color.BLACK);
        }
        @Override
        public void onClick(View v) {
            mColIdx = mIdx;
            for (int i = 0; i < mColors.length; i++) {
                mColors[i].setSelected(i == mIdx);
            }
            mColPicker.setColor(mApp.mColData.getColor(mApp.mPalIdx, mColIdx));
        }
    }

    /*-----------------------------------------------------------------------*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.palette);

        mApp = (MyApplication) getApplication();
        mPaint.setStyle(Paint.Style.STROKE);

        mColPicker = (ColorPickerInterface) findViewById(R.id.colpicker_hsv);
        mColPicker.setListener(this);

        mPalSpinner = (Spinner) findViewById(R.id.spin_palette);
        mPalSpinner.setAdapter(mApp.mPalAdapter);
        mPalSpinner.setOnItemSelectedListener(this);

        LinearLayout ll = (LinearLayout) findViewById(R.id.table_palette);
        LayoutParams lp = new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1f);
        int margin = (int) (4f * getResources().getDisplayMetrics().density);
        lp.setMargins(margin, margin, margin, margin);
        for (int i = 0; i < 4; i++) {
            LinearLayout row = new LinearLayout(this);
            for (int j = 0; j < 4; j++) {
                int idx = i * 4 + j;
                mColors[idx] = new ColorView(this, idx);
                row.addView(mColors[idx], lp);
            }
            ll.addView(row,lp);
        }
        mColors[mColIdx].onClick(mColors[mColIdx]);
        setButtonsColor();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPalSpinner.setSelection(mApp.mPalIdx);
    }

    /*-----------------------------------------------------------------------*/

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        Spinner spinner = (Spinner) parent;
        if (spinner == mPalSpinner) {
            mApp.mPalIdx = spinner.getSelectedItemPosition();
            setButtonsColor();
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
        mColors[mColIdx].setBackgroundColor(color);
    }

    /*-----------------------------------------------------------------------*/

    private void setButtonsColor() {
        int pal = mApp.mPalIdx;
        for (int i = 0; i < mColors.length; i++) {
            mColors[i].setBackgroundColor(mApp.mColData.getColor(pal, i));
        }
        mColPicker.setColor(mApp.mColData.getColor(pal, mColIdx));
    }

}
