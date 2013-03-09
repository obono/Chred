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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.TextView;

public class PaletteAdapter extends BaseAdapter {

    private Context mContext;
    private ColData mColData;
    private Rect mDrawRect = new Rect();
    private Paint mPaint = new Paint();

    /*-----------------------------------------------------------------------*/

    class PaletteView extends CheckedTextView {
        private int mPos;
        private Drawable mCheckMark;
        public PaletteView(Context context) {
            super(context);
            mCheckMark = context.getResources().getDrawable(android.R.drawable.btn_radio);
            setCheckMarkDrawable(mCheckMark);
            int padding = Utils.dp2px(context, 6);
            setPadding(padding, padding, padding, padding);
            setTextAppearance(mContext, android.R.style.TextAppearance_Widget_DropDownItem);
        }
        public void setPosition(int pos) {
            mPos = pos;
            setText("Palette ".concat(String.valueOf(mPos)));
        }
        @Override
        public void draw(Canvas canvas) {
            int left = getPaddingLeft();
            int right = getWidth() - mCheckMark.getIntrinsicWidth();
            int height = getHeight();
            int size = (right - left) / ColData.COLS_PER_PAL;
            mDrawRect.set(left, height / 2, left + size, height - getPaddingBottom());
            for (int i = 0; i < ColData.COLS_PER_PAL; i++) {
                mPaint.setStyle(Paint.Style.FILL);
                mPaint.setColor(mColData.getColor(mPos, i));
                canvas.drawRect(mDrawRect, mPaint);
                mPaint.setStyle(Paint.Style.STROKE);
                mPaint.setColor(Color.BLACK);
                canvas.drawRect(mDrawRect, mPaint);
                if (i == 0) {
                    canvas.drawLine(mDrawRect.right, mDrawRect.top,
                            mDrawRect.left, mDrawRect.bottom, mPaint);
                }
                mDrawRect.offset(size, 0);
            }
            super.draw(canvas);
        }
    }

    /*-----------------------------------------------------------------------*/

    public PaletteAdapter(Context context, ColData colData) {
        mContext = context;
        mColData = colData;
    }

    @Override
    public int getCount() {
        return ColData.MAX_PALS;
    }

    @Override
    public Object getItem(int pos) {
        return String.valueOf(pos);
    }

    @Override
    public long getItemId(int pos) {
        return pos;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        TextView textView = (TextView) convertView;
        if (textView == null) {
            textView = new TextView(mContext);
            textView.setTextAppearance(mContext, android.R.style.TextAppearance_Inverse);
        }
        textView.setText(String.valueOf(pos));
        return textView;
    }

    @Override
    public View getDropDownView(int pos, View convertView, ViewGroup parent) {
        PaletteView palView = (PaletteView) convertView;;
        if (palView == null) {
            palView = new PaletteView(mContext);
        }
        palView.setPosition(pos);
        return palView;
    }

}
