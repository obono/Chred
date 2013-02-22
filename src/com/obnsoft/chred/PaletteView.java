package com.obnsoft.chred;

import com.obnsoft.view.HSVColorPickerView;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PaletteView extends LinearLayout {

    private Paint mPaint = new Paint();
    private ColorView[] mColViews = new ColorView[ColData.COLS_PER_PAL];

    /*-----------------------------------------------------------------------*/

    class ColorView extends TextView {
        private int mIdx;
        public ColorView(Context context, int idx) {
            super(context);
            setMinimumHeight(Utils.dp2px(context, 32));
            setGravity(Gravity.CENTER);
            mIdx = idx;
        }
        @Override
        public void draw(Canvas canvas) {
            super.draw(canvas);
            int right = getWidth() - 1;
            int bottom = getHeight() - 1;
            mPaint.setColor(Color.WHITE);
            canvas.drawRect(0, 0, right, bottom, mPaint);
            if (mIdx == 0) {
                canvas.drawLine(right, 0, 0, bottom, mPaint);
            }
            if (isSelected()) {
                for (int i = 0; i < 3; i++) {
                    mPaint.setColor((i == 1) ? Color.RED : Color.YELLOW);
                    canvas.drawRect(i, i, right - i, bottom - i, mPaint);
                }
            }
        }
        @Override
        public void setBackgroundColor(int color) {
            super.setBackgroundColor(color);
            if (mIdx > 0) {
                super.setText(String.format("%06X", color & 0xFFFFFF));
                setTextColor((HSVColorPickerView.calcBrightness(color) < 0.5) ?
                        Color.WHITE : Color.BLACK);
            }
        }
        public int getIndex() {
            return mIdx;
        }
    }

    /*-----------------------------------------------------------------------*/

    public PaletteView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint.setStyle(Paint.Style.STROKE);

        setOrientation(LinearLayout.VERTICAL);
        LayoutParams lp = new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1f);
        int margin = Utils.dp2px(context, 4);
        lp.setMargins(margin, margin, margin, margin);
        for (int i = 0; i < 4; i++) {
            LinearLayout ll = new LinearLayout(context);
            for (int j = 0; j < 4; j++) {
                int idx = i * 4 + j;
                mColViews[idx] = new ColorView(context, idx);
                ll.addView(mColViews[idx], lp);
            }
            addView(ll, lp);
        }
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        for (int i = 0; i < mColViews.length; i++) {
            mColViews[i].setOnClickListener(l);
        }
    }

    public void setPalette(ColData colData, int palIdx) {
        for (int i = 0; i < mColViews.length; i++) {
            mColViews[i].setBackgroundColor(colData.getColor(palIdx, i));
        }
    }

    public ColorView setSelection(int colIdx) {
        for (int i = 0; i < mColViews.length; i++) {
            mColViews[i].setSelected(i == colIdx);
        }
        invalidate();
        return mColViews[colIdx];
    }

}
