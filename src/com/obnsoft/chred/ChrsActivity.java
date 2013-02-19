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

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

public class ChrsActivity extends Activity {

    private static final int GRIDITEM_SIZE = 80;

    private int mPalette = 2;

    private int mChrStep;
    private int mChrCount;
    private int mChrWidth;
    private int mChrHeight;
    private int mChrScale;

    private Bitmap mBitmap;
    private Rect mSrcRect = new Rect();
    private Rect mDestRect = new Rect();
    private Paint mPaint = new Paint();

    private MyApplication mApp;
    private MyAdapter mAdapter;
    private GridView mGridView;

    /*-----------------------------------------------------------------------*/

    class ChrView extends View {
        private int mPosition = 0;
        public ChrView(Context context) {
            super(context);
        }
        public void setPosition(int pos) {
            mPosition = pos;
        }
        @Override
        public void draw(Canvas canvas) {
            mSrcRect.set(0, mPosition * mChrHeight, mChrWidth, (mPosition + 1) * mChrHeight);
            canvas.drawBitmap(mBitmap, mSrcRect, mDestRect, null);
            canvas.drawRect(mDestRect, mPaint);
        }
    }

    class ViewHolder {
        public ChrView mChrView;
        public TextView mTextView;
        public ViewHolder(ChrView cv, TextView tv) {
            mChrView = cv;
            mTextView = tv;
        }
    }

    class MyAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mChrCount;
        }
        @Override
        public Object getItem(int position) {
            return null;
        }
        @Override
        public long getItemId(int position) {
            return position * mChrStep;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Context context = ChrsActivity.this;
            ChrView cv;
            TextView tv;
            if (convertView == null) {
                LinearLayout ll = new LinearLayout(context);
                ll.setOrientation(LinearLayout.VERTICAL);
                cv = new ChrView(context);
                tv = new TextView(context);
                ll.addView(cv, GRIDITEM_SIZE, GRIDITEM_SIZE);
                ll.addView(tv);
                convertView = ll;
                convertView.setTag(new ViewHolder(cv, tv));
            } else {
                ViewHolder holder = (ViewHolder) convertView.getTag();
                cv = holder.mChrView;
                tv = holder.mTextView;
            }
            int idx = (int) getItemId(position);
            cv.setPosition(position);
            tv.setText(String.valueOf(idx));
            return convertView;
        }
    }

    /*-----------------------------------------------------------------------*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chrs);

        mApp = (MyApplication) getApplication();
        changeTargetSize(0, 0);

        mPaint.setColor(Color.WHITE);
        mPaint.setStyle(Paint.Style.STROKE);
        mAdapter = new MyAdapter();
        mGridView = (GridView) findViewById(R.id.grid_chrs);
        mGridView.setAdapter(mAdapter);

        Spinner spinner = (Spinner) findViewById(R.id.spin_size);
        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
                Spinner spinner = (Spinner) parent;
                String item = (String) spinner.getSelectedItem();
                int hUnits = item.charAt(0) - '0';
                int vUnits = item.charAt(2) - '0';
                changeTargetSize(hUnits, vUnits);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        spinner = (Spinner) findViewById(R.id.spin_palette);
        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
                Spinner spinner = (Spinner) parent;
                String item = (String) spinner.getSelectedItem();
                mPalette = Integer.parseInt(item);
                changeTargetSize(0, 0);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (mBitmap != null) {
            mBitmap.recycle();
            mBitmap = null;
        }
        super.onDestroy();
    }

    /*-----------------------------------------------------------------------*/

    private void changeTargetSize(int hUnits, int vUnits) {
        ChrData chrData = mApp.mChrData;
        chrData.setTargetSize(hUnits, vUnits);
        hUnits = chrData.getTargetSizeH();
        vUnits = chrData.getTargetSizeV();

        if (mBitmap != null) {
            mBitmap.recycle();
        }
        mChrStep = hUnits * vUnits;
        mChrCount = ChrData.MAX_CHARS / mChrStep;
        mChrWidth = ChrData.UNIT_SIZE * hUnits;
        mChrHeight = ChrData.UNIT_SIZE * vUnits;
        mBitmap = Bitmap.createBitmap(mChrWidth, mChrHeight * mChrCount, Bitmap.Config.RGB_565);
        for (int i = 0; i < mChrCount; i++) {
            chrData.drawTarget(mBitmap, i * mChrStep, mPalette, 0, i * mChrHeight);
        }
        mChrScale = GRIDITEM_SIZE / ChrData.UNIT_SIZE / Math.max(hUnits, vUnits);
        mDestRect.set(0, 0, mChrWidth * mChrScale, mChrHeight * mChrScale);
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

}
