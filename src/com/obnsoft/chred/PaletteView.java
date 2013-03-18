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
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.LinearLayout;

public class PaletteView extends LinearLayout {

    private Paint mPaint = new Paint();
    private ColorView[] mColViews = new ColorView[ColData.COLS_PER_PAL];
    private AdapterContextMenuInfo mContextMenuInfo;

    /*-----------------------------------------------------------------------*/

    public PaletteView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint.setStyle(Paint.Style.STROKE);

        setOrientation(LinearLayout.VERTICAL);
        LayoutParams lp = new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1f);
        int margin = Utils.dp2px(context, 1);
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

    @Override
    public void setOnCreateContextMenuListener(OnCreateContextMenuListener l) {
        super.setOnCreateContextMenuListener(l);
        OnLongClickListener ll = null;
        if (l != null) {
            ll = new OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    int idx = ((ColorView) v).getIndex();
                    mContextMenuInfo = new AdapterContextMenuInfo(v, idx, idx);
                    PaletteView.this.showContextMenu();
                    return true;
                }
            };
        }
        for (int i = 0; i < mColViews.length; i++) {
            mColViews[i].setOnLongClickListener(ll);
        }
    }

    @Override
    protected ContextMenuInfo getContextMenuInfo() {
        return mContextMenuInfo;
    }

    /*-----------------------------------------------------------------------*/

    public void setPalette(ColData colData, int palIdx) {
        for (int i = 0; i < mColViews.length; i++) {
            mColViews[i].setColor(colData.getColor(palIdx, i));
        }
    }

    public ColorView getColorView(int colIdx) {
        return (colIdx >= 0 && colIdx < mColViews.length) ? mColViews[colIdx] : null;
    }

    public void setSelection(int colIdx) {
        for (int i = 0; i < mColViews.length; i++) {
            mColViews[i].setSelected(i == colIdx);
        }
    }

}
