package com.obnsoft.chred;

import android.content.Context;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class PaletteView extends LinearLayout {

    private Paint mPaint = new Paint();
    private ColorView[] mColViews = new ColorView[ColData.COLS_PER_PAL];

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
                mColViews[idx] = new ColorView(context, null);
                mColViews[idx].setIndex(idx);
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
            mColViews[i].setColor(colData.getColor(palIdx, i));
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
