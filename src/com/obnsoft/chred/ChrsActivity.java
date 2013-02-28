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
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

public class ChrsActivity extends Activity implements OnItemSelectedListener {

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
    private Spinner mPalSpinner;
    private AbsListView.LayoutParams mGridItemLayout;
    private RelativeLayout.LayoutParams mGridItemTextLayout;

    /*-----------------------------------------------------------------------*/

    class ChrView extends View {
        private int mPos;
        public ChrView(Context context) {
            super(context);
        }
        public void setPosition(int pos) {
            mPos = pos;
        }
        @Override
        public void draw(Canvas canvas) {
            if (mBitmap != null) {
            mSrcRect.set(0, mPos * mChrHeight, mChrWidth, (mPos + 1) * mChrHeight);
            canvas.drawBitmap(mBitmap, mSrcRect, mDestRect, null);
            canvas.drawRect(mDestRect.left - 1, mDestRect.top - 1,
                    mDestRect.right, mDestRect.bottom, mPaint);
            }
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
        public Object getItem(int pos) {
            return null;
        }
        @Override
        public long getItemId(int pos) {
            return pos * mChrStep;
        }
        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {
            Context context = ChrsActivity.this;
            ChrView cv;
            TextView tv;
            if (convertView == null) {
                RelativeLayout rl = new RelativeLayout(context);
                rl.setGravity(Gravity.BOTTOM);
                cv = new ChrView(context);
                tv = new TextView(context);
                tv.setGravity(Gravity.CENTER);
                tv.setTextAppearance(context, android.R.style.TextAppearance_Medium);
                rl.addView(cv);
                rl.addView(tv, mGridItemTextLayout);
                convertView = rl;
                convertView.setTag(new ViewHolder(cv, tv));
            } else {
                ViewHolder holder = (ViewHolder) convertView.getTag();
                cv = holder.mChrView;
                tv = holder.mTextView;
            }
            int idx = (int) getItemId(pos);
            cv.setPosition(pos);
            tv.setText(String.valueOf(idx));
            convertView.setLayoutParams(mGridItemLayout);
            convertView.setBackgroundColor((idx == mApp.mChrIdx) ? 0x99FFFF00 : Color.TRANSPARENT);
            return convertView;
        }
    }

    /*-----------------------------------------------------------------------*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chrs);

        mApp = (MyApplication) getApplication();
        mChrScale = Utils.dp2px(this, 4);
        mPaint.setColor(Color.WHITE);
        mPaint.setStyle(Paint.Style.STROKE);
        mGridItemTextLayout = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        mGridItemTextLayout.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

        Spinner spinner = (Spinner) findViewById(R.id.spin_size);
        spinner.setOnItemSelectedListener(this);
        String strSize = String.format("%dx%d",
                mApp.mChrData.getTargetSizeH(), mApp.mChrData.getTargetSizeV());
        for (int i = 0; i < spinner.getCount(); i++) {
            if (strSize.equals(spinner.getItemAtPosition(i))) {
                spinner.setSelection(i);
                break;
            }
        }

        mPalSpinner = (Spinner) findViewById(R.id.spin_palette);
        mPalSpinner.setAdapter(mApp.mPalAdapter);
        mPalSpinner.setOnItemSelectedListener(this);

        mAdapter = new MyAdapter();
        mGridView = (GridView) findViewById(R.id.grid_chrs);
        mGridView.setAdapter(mAdapter);
        mGridView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                mGridView.setSelection(pos);
                mApp.mChrIdx = (int) id;
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    protected void onResume() {
        if (mPalSpinner.getSelectedItemPosition() != mApp.mPalIdx) {
            mPalSpinner.setSelection(mApp.mPalIdx);
        } else {
            drawChrsBitmap();
            mGridView.setSelection(mApp.mChrIdx / mChrStep);
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mBitmap.recycle();
        mBitmap = null;
    }

    /*-----------------------------------------------------------------------*/

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        Spinner spinner = (Spinner) parent;
        if (spinner == mPalSpinner) {
            mApp.mPalIdx = spinner.getSelectedItemPosition();
            drawChrsBitmap();
        } else {
            String item = (String) spinner.getSelectedItem();
            int hUnits = item.charAt(0) - '0';
            int vUnits = item.charAt(2) - '0';
            mApp.mChrData.setTargetSize(hUnits, vUnits);
            drawChrsBitmap();
        }
    }
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Do nothing
    }

    /*-----------------------------------------------------------------------*/

    protected void drawChrsBitmap() {
        ChrData chrData = mApp.mChrData;
        int hUnits = chrData.getTargetSizeH();
        int vUnits = chrData.getTargetSizeV();
        mChrStep = hUnits * vUnits;
        mChrCount = ChrData.MAX_CHARS / mChrStep;
        mChrWidth = ChrData.UNIT_SIZE * hUnits;
        mChrHeight = ChrData.UNIT_SIZE * vUnits;

        if (mBitmap != null) {
            mBitmap.recycle();
        }
        mBitmap = Bitmap.createBitmap(mChrWidth, mChrHeight * mChrCount, Bitmap.Config.ARGB_8888);
        for (int i = 0; i < mChrCount; i++) {
            chrData.drawTarget(mBitmap, i * mChrStep, mApp.mPalIdx, 0, i * mChrHeight);
        }

        mApp.mChrIdx &= ~(mChrStep - 1);
        int itemWidth = mChrWidth * mChrScale;
        int itemHeight = mChrHeight * mChrScale;
        mDestRect.set(mChrScale, mChrScale, itemWidth + mChrScale, itemHeight + mChrScale);
        mGridView.setColumnWidth(itemWidth + mChrScale * 2);
        mGridView.setSelection(mApp.mChrIdx / mChrStep);
        mGridItemLayout = new AbsListView.LayoutParams(
                itemWidth + mChrScale * 2, itemHeight + mChrScale * 6);
        mAdapter.notifyDataSetChanged();
    }

}
