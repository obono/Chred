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

import com.obnsoft.view.HSVColorPickerView;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.TextView;

public class ColorView extends TextView {


    private int mIdx;
    private int mStroke;
    private boolean mIsLight;
    private GradientDrawable mBackGround;

    public ColorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setMinimumHeight(Utils.dp2px(context, 32));
        setGravity(Gravity.CENTER);
        setClickable(true);
        mStroke = Utils.dp2px(context, 2);
        mBackGround = new GradientDrawable();
        mBackGround.setCornerRadius(mStroke * 2);
        setBackgroundDrawable(mBackGround);
    }

    @Override
    public void draw(Canvas canvas) {
        if (isSelected()) {
            mBackGround.setStroke(mStroke, isPressed() ? Color.WHITE : Color.YELLOW);
        } else {
            mBackGround.setStroke(mStroke, isPressed() ? Color.LTGRAY : Color.GRAY,
                    mStroke, mStroke);
        }
        if (mIdx == 0) {
            canvas.drawLine(getWidth() - 1, 0, 0, getHeight() - 1, getPaint());
        }
        super.draw(canvas);
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        setLabelColor();
    }

    public int getIndex() {
        return mIdx;
    }

    public void setIndex(int idx) {
        mIdx = idx;
    }

    public void setColor(int color) {
        mBackGround.setColor(color);
        super.setText(String.format("[%02d]\n%06X", mIdx, color & 0xFFFFFF));
        mIsLight = (HSVColorPickerView.calcBrightness(color) < 0.5);
        setLabelColor();
    }

    private void setLabelColor() {
        int c;
        if (mIdx == 0) {
            c = Color.GRAY;
        } else if (isSelected()) {
            c = mIsLight ? Color.YELLOW : 0xFF666600;
        } else {
            c = mIsLight ? Color.WHITE : Color.BLACK;
        }
        setTextColor(c);
        setTypeface(isSelected() ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
    }

}
