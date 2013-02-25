package com.obnsoft.chred;

import com.obnsoft.view.HSVColorPickerView;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.TextView;

public class ColorView extends TextView {


    private int mIdx;
    private int mStroke;
    private GradientDrawable mBackGround;

    public ColorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setMinimumHeight(Utils.dp2px(context, 32));
        setGravity(Gravity.CENTER);
        setClickable(true);
        mStroke = Utils.dp2px(context, 2);
        mBackGround = new GradientDrawable();
        mBackGround.setCornerRadius(mStroke * 4);
        setBackgroundDrawable(mBackGround);
    }

    @Override
    public void draw(Canvas canvas) {
        if (isSelected()) {
            mBackGround.setStroke(mStroke, isPressed() ? Color.WHITE : Color.LTGRAY);
        } else {
            mBackGround.setStroke(mStroke,
                    isPressed() ? Color.WHITE : Color.GRAY, mStroke, mStroke);
        }
        super.draw(canvas);
        if (mIdx == 0) {
            Paint p = getPaint();
            canvas.drawLine(getWidth() - 1, 0, 0, getHeight() - 1, p);
        }
    }

    public int getIndex() {
        return mIdx;
    }

    public void setIndex(int idx) {
        mIdx = idx;
    }

    public void setColor(int color) {
        mBackGround.setColor(color);
        super.setText(String.format("#%d\n%06X", mIdx, color & 0xFFFFFF));
        setTextColor((mIdx == 0 || HSVColorPickerView.calcBrightness(color) < 0.5) ?
                Color.WHITE : Color.BLACK);
    }

}
