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
import com.obnsoft.view.MagnifyView;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;

public class PaletteActivity extends Activity implements OnItemSelectedListener,
        ColorPickerInterface.OnColorChangedListener, OnClickListener {

    private Bitmap mBitmap;
    private Paint mPaint = new Paint();

    private MyApplication mApp;
    private PaletteView mPalView;
    private ColorView mColView;
    private Spinner mPalSpinner;
    private ColorPickerInterface mColPicker;
    private MagnifyView mPreView;

    /*-----------------------------------------------------------------------*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.palette);

        mApp = (MyApplication) getApplication();
        mPaint.setStyle(Paint.Style.STROKE);

        mPalView = (PaletteView) findViewById(R.id.palview);
        mPalView.setPalette(mApp.mColData, mApp.mPalIdx);
        mPalView.setOnClickListener(this);

        mPalSpinner = (Spinner) findViewById(R.id.spin_palette);
        mPalSpinner.setAdapter(mApp.mPalAdapter);
        mPalSpinner.setOnItemSelectedListener(this);

        mColPicker = (ColorPickerInterface) findViewById(R.id.colpicker_hsv);
        mColPicker.setListener(this);

        mPreView = (MagnifyView) findViewById(R.id.view_preview);
    }

    @Override
    protected void onResume() {
        ChrData chrData = mApp.mChrData;
        mBitmap = Bitmap.createBitmap(chrData.getTargetSizeH() * ChrData.UNIT_SIZE,
                chrData.getTargetSizeV() * ChrData.UNIT_SIZE, Bitmap.Config.ARGB_8888);
        mPreView.setBitmap(mBitmap);
        if (mPalSpinner.getSelectedItemPosition() != mApp.mPalIdx) {
            mPalSpinner.setSelection(mApp.mPalIdx);
        } else {
            mPalView.setPalette(mApp.mColData, mApp.mPalIdx);
            updatePreview();
        }
        mPalView.setSelection(mApp.mColIdx);
        mColView = mPalView.getColorView(mApp.mColIdx);
        mColPicker.setColor(mApp.mColData.getColor(mApp.mPalIdx, mApp.mColIdx));
        super.onResume();
        registerForContextMenu(mPalView);
    }

    @Override
    protected void onPause() {
        unregisterForContextMenu(mPalView);
        super.onPause();
        mPreView.setBitmap(null);
        mBitmap.recycle();
        mBitmap = null;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view,
            ContextMenuInfo menuInfo) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        int col = info.position;
        if (view == mPalView && col != mApp.mColIdx) {
            menu.setHeaderTitle(R.string.menu_operation);
            onCreateContextMenuAddMenu(menu, R.id.menu_swap, R.string.menu_swap);
            onCreateContextMenuAddMenu(menu, R.id.menu_move, R.string.menu_move);
            onCreateContextMenuAddMenu(menu, R.id.menu_copy, R.string.menu_copy);
            onCreateContextMenuAddMenu(menu, R.id.menu_gradient, R.string.menu_gradient);
        }
        super.onCreateContextMenu(menu, view, menuInfo);
    }

    private void onCreateContextMenuAddMenu(ContextMenu menu, int menuId, int strId) {
        menu.add(Menu.NONE, menuId, Menu.NONE, String.format(getString(strId), mApp.mColIdx));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        boolean ret = false;
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        int col = info.position;
        switch (item.getItemId()) {
        case R.id.menu_swap:
            mApp.mColData.swapColors(mApp.mPalIdx, mApp.mColIdx, col);
            ret = true;
            break;
        case R.id.menu_move:
            mApp.mColData.moveColors(mApp.mPalIdx, mApp.mColIdx, col);
            ret = true;
            break;
        case R.id.menu_copy:
            mApp.mColData.copyColors(mApp.mPalIdx, mApp.mColIdx, col);
            ret = true;
            break;
        case R.id.menu_gradient:
            mApp.mColData.gradiantColors(mApp.mPalIdx, mApp.mColIdx, col);
            ret = true;
            break;
        }
        if (ret) {
            mApp.mColIdx = col;
            mPalView.setPalette(mApp.mColData, mApp.mPalIdx);
            mPalView.setSelection(col);
            mColView = mPalView.getColorView(col);
            mColPicker.setColor(mApp.mColData.getColor(mApp.mPalIdx, col));
            updatePreview();
        }
        return ret;
    }

    /*-----------------------------------------------------------------------*/

    @Override
    public void onClick(View view) {
        mColView = (ColorView) view;
        mApp.mColIdx = mColView.getIndex();
        mPalView.setSelection(mApp.mColIdx);
        mColPicker.setColor(mApp.mColData.getColor(mApp.mPalIdx, mApp.mColIdx));
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        Spinner spinner = (Spinner) parent;
        if (spinner == mPalSpinner) {
            mApp.mPalIdx = spinner.getSelectedItemPosition();
            mPalView.setPalette(mApp.mColData, mApp.mPalIdx);
            mColPicker.setColor(mApp.mColData.getColor(mApp.mPalIdx, mApp.mColIdx));
            updatePreview();
        }
    }
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Do nothing
    }

    @Override
    public void colorChanged(int color) {
        int pal = mApp.mPalIdx;
        mApp.mColData.setColor(pal, mApp.mColIdx, color);
        color = mApp.mColData.getColor(pal, mApp.mColIdx);
        mColView.setColor(color);
        updatePreview();
    }

    private void updatePreview() {
        mApp.mChrData.drawTarget(mBitmap, mApp.mChrIdx, mApp.mPalIdx);
        mPreView.invalidate();
    }
}
