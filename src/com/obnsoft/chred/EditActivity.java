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

import java.util.ArrayList;

import com.obnsoft.view.MagnifyView;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.TextView;
import android.widget.ToggleButton;

public class EditActivity extends Activity
        implements MagnifyView.EventHandler, OnItemSelectedListener {

    private Bitmap mBitmap;
    private UndoBuffer mUndoBuffer = new UndoBuffer();

    private MyApplication mApp;
    private MagnifyView mMagView;
    private MagnifyView mPreView;
    private TextView mIdxText;
    private Spinner mPalSpinner;
    private ToggleButton mMoveBtn;
    private ColorView mColView;

    /*-----------------------------------------------------------------------*/

    class UndoDot {
        public byte x, y, c;
        public UndoDot(int x, int y, int c) {
            this.x = (byte) x;
            this.y = (byte) y;
            this.c = (byte) c;
        }
    }

    class UndoStep {
        private static final int BUFFER_UNIT = 32;
        private ArrayList<UndoDot> mDots = new ArrayList<UndoDot>(BUFFER_UNIT);
        public void addDot(int x, int y, int c) {
            mDots.add(new UndoDot(x, y, c));
        }
        public void restoreStep() {
            for (UndoDot d : mDots) {
                drawDot(d.x, d.y, d.c);
            }
        }
    }

    class UndoBuffer {
        private static final int MAX_STEPS = 64;
        private UndoStep mCurStep;
        private ArrayList<UndoStep> mSteps = new ArrayList<UndoStep>();
        public void startStep() {
            if (mCurStep == null) {
                mCurStep = new UndoStep();
            }
        }
        public void addDot(int x, int y, int c) {
            if (mCurStep != null) {
                mCurStep.addDot(x, y, c);
            }
        }
        public void finishStep() {
            if (mCurStep != null) {
                mSteps.add(mCurStep);
                if (mSteps.size() > MAX_STEPS) {
                    mSteps.remove(0);
                }
                mCurStep = null;
            }
        }
        public boolean isEmpty() {
            return mSteps.isEmpty();
        }
        public void execUndo() {
            if (!isEmpty()) {
                mSteps.remove(mSteps.size() - 1).restoreStep();
            }
        }
        public void clear() {
            mSteps.clear();
            mCurStep = null;
        }
    }

    /*-----------------------------------------------------------------------*/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit);

        mApp = (MyApplication) getApplication();
        mMagView = (MagnifyView) findViewById(R.id.view_edit);
        int unit = Utils.dp2px(this, 4);
        mMagView.setScrollable(true);
        mMagView.setScaleRange(unit, unit * 16);
        mMagView.setGridColor(Color.GRAY, false);

        mPreView = (MagnifyView) findViewById(R.id.view_preview);
        mIdxText = (TextView) findViewById(R.id.text_index);
        mPalSpinner = (Spinner) findViewById(R.id.spin_palette);
        mPalSpinner.setAdapter(mApp.mPalAdapter);
        mPalSpinner.setOnItemSelectedListener(this);

        mMoveBtn = (ToggleButton) findViewById(R.id.btn_move);
        mColView = (ColorView) findViewById(R.id.btn_color);
    }

    @Override
    protected void onResume() {
        mPalSpinner.setSelection(mApp.mPalIdx);
        ChrData chrData = mApp.mChrData;
        int hUnits = chrData.getTargetSizeH();
        int vUnits = chrData.getTargetSizeV();
        mBitmap = Bitmap.createBitmap(hUnits * ChrData.UNIT_SIZE,
                vUnits * ChrData.UNIT_SIZE, Bitmap.Config.ARGB_8888);
        chrData.drawTarget(mBitmap, mApp.mChrIdx, mApp.mPalIdx);
        mMagView.setBitmap(mBitmap);
        mPreView.setBitmap(mBitmap);
        mIdxText.setText(String.format("%d\n(%dx%d)", mApp.mChrIdx, vUnits, hUnits));
        setButtonsStatus();

        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMagView.setBitmap(null);
        mPreView.setBitmap(null);
        mBitmap.recycle();
        mBitmap = null;
        mUndoBuffer.clear();
    }

    /*-----------------------------------------------------------------------*/

    @Override
    public boolean onTouchEventUnit(int action, float x, float y, float[] histCoords) {
        if (action == MotionEvent.ACTION_DOWN) {
            mUndoBuffer.startStep();
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP ||
                action == MotionEvent.ACTION_CANCEL) {
            mUndoBuffer.finishStep();
            setButtonsStatus();
            return true;
        }
        int dx = (int) x, dy = (int) y;
        int l = dx, r = dx, t = dy, b = dy;
        drawDotWithHist(dx, dy);
        if (histCoords != null) {
            int count = histCoords.length / 2;
            for (int i = 0; i < count; i++) {
                dx = (int) histCoords[i * 2];
                dy = (int) histCoords[i * 2 + 1];
                if (drawDotWithHist(dx, dy)) {
                    if (l > dx) l = dx;
                    if (r < dx) r = dx;
                    if (t > dy) t = dy;
                    if (b < dy) b = dy;
                }
            }
        }
        mMagView.invalidateUnit(l, t, r, b);
        mPreView.invalidateUnit(l, t, r, b);
        return true;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        Spinner spinner = (Spinner) parent;
        if (spinner == mPalSpinner) {
            mApp.mPalIdx = spinner.getSelectedItemPosition();
            mApp.mChrData.drawTarget(mBitmap, mApp.mChrIdx, mApp.mPalIdx);
            mMagView.invalidate();
            mPreView.invalidate();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Do nothing
    }

    /*-----------------------------------------------------------------------*/

    public void onClickMoveButton(View v) {
        setButtonsStatus();
    }

    public void onClickColorButton(View v) {
        PaletteView palView = new PaletteView(this, null);
        palView.setPalette(mApp.mColData, mApp.mPalIdx);
        palView.setSelection(mApp.mColIdx);
        final AlertDialog dlg = new AlertDialog.Builder(this)
                .setView(palView)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        palView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                ColorView colView = (ColorView) view;
                mApp.mColIdx = colView.getIndex();
                mMoveBtn.setChecked(false);
                setButtonsStatus();
                dlg.dismiss();
            }
        });
        dlg.show();
    }

    public void onClickUndoButton(View v) {
        mUndoBuffer.execUndo();
        mMagView.invalidate();
        mPreView.invalidate();
    }

    /*-----------------------------------------------------------------------*/

    private void setButtonsStatus() {
        mMagView.setEventHandler(mMoveBtn.isChecked() ? null : this);
        mColView.setIndex(mApp.mColIdx);
        mColView.setColor(mApp.mColData.getColor(mApp.mPalIdx, mApp.mColIdx));
    }

    private boolean drawDotWithHist(int x, int y) {
        int last;
        if (x >= 0 && y >= 0 && x < mBitmap.getWidth() && y < mBitmap.getHeight()
                && (last = mApp.mChrData.getTargetDot(mApp.mChrIdx, x, y)) != mApp.mColIdx) {
            mUndoBuffer.addDot(x, y, last);
            drawDot(x, y, mApp.mColIdx);
            return true;
        }
        return false;
    }

    private void drawDot(int x, int y, int c) {
        mApp.mChrData.setTargetDot(mApp.mChrIdx, x, y, c);
        mBitmap.setPixel(x, y, mApp.mColData.getColor(mApp.mPalIdx, c));
    }
}
