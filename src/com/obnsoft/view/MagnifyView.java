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

package com.obnsoft.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;

public class MagnifyView extends View implements OnScaleGestureListener {

    private float   mUnit = 1f;
    private float   mMinUnit = 1f;
    private float   mMaxUnit = 64f;
    private int     mGridColor = Color.TRANSPARENT;
    private int     mFrameColor = Color.WHITE;
    private boolean mDotted;
    private boolean mScrollable;

    private boolean mIsSmooth;
    private boolean mIsMoving;
    private boolean mIsScaling;
    private float   mScalingUnit;
    private float   mScalingSpan;
    private float   mFocusX;
    private float   mFocusY;
    private float   mFocusUnitX;
    private float   mFocusUnitY;

    private Bitmap  mBitmap;
    private Rect    mWorkRect = new Rect();
    private Rect    mSrcRect = new Rect();
    private RectF   mDrawRect = new RectF();
    private Paint   mPaint = new Paint();

    private EventHandler mHandler;
    private ScaleGestureDetector mGestureDetector;

    /*-----------------------------------------------------------------------*/

    public interface EventHandler {
        public boolean onTouchEventUnit(
                int action, float unitX, float unitY, float[] historicalCoords);
    }

    /*-----------------------------------------------------------------------*/

    public MagnifyView(Context context) {
        this(context, null);
    }

    public MagnifyView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MagnifyView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mPaint.setAntiAlias(false);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(1);
        mGestureDetector = new ScaleGestureDetector(context, this);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mBitmap == null) {
            return;
        }

        canvas.getClipBounds(mWorkRect);
        if (mWorkRect.isEmpty()) {
            mWorkRect.set(0, 0, getWidth(), getHeight());
        }
        float cl = Math.max(mWorkRect.left, mDrawRect.left);
        float cr = Math.min(mWorkRect.right, mDrawRect.right);
        float ct = Math.max(mWorkRect.top, mDrawRect.top);
        float cb = Math.min(mWorkRect.bottom, mDrawRect.bottom);
        cl -= (cl - mDrawRect.left) % mUnit;
        ct -= (ct - mDrawRect.top) % mUnit;

        mPaint.setColor(mGridColor);
        if (mGridColor != Color.TRANSPARENT) {
            float x1 = cl, x2 = cl;
            float y1 = ct, y2 = ct;
            while (x1 < cr || y1 < cb) {
                if (x1 < cr) x1 += mUnit; else y1 += mUnit;
                if (y2 < cb) y2 += mUnit; else x2 += mUnit;
                canvas.drawLine(x1 + 1, y1, x2, y2 + 1, mPaint);
            }
        }
        canvas.drawBitmap(mBitmap, mSrcRect, mDrawRect, null);
        if (mPaint.getColor() != Color.TRANSPARENT) {
            if (mDotted) {
                for (float x = cl; x <= cr; x += mUnit) {
                    for (float y = ct; y <= cb; y += mUnit) {
                        canvas.drawPoint(x, y, mPaint);
                    }
                }
            } else {
                for (float x = cl; x <= cr; x += mUnit) {
                    canvas.drawLine(x, ct, x, cb, mPaint);
                }
                for (float y = ct; y <= cb; y += mUnit) {
                    canvas.drawLine(cl, y, cr, y, mPaint);
                }
            }
        }
        if (mFrameColor != Color.TRANSPARENT) {
            mPaint.setColor(mFrameColor);
            int gap = (mGridColor == Color.TRANSPARENT) ? 1 : 0;
            canvas.drawRect(mDrawRect.left - gap, mDrawRect.top - gap,
                    mDrawRect.right, mDrawRect.bottom, mPaint);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        calcCoords();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        if (mHandler != null) {
            int action = event.getActionMasked();
            float unitX = (x - mDrawRect.left) / mUnit;
            float unitY = (y - mDrawRect.top) / mUnit;
            int histCount = event.getHistorySize();
            float[] histCoords = null;
            if (histCount > 0) {
                histCoords = new float[histCount * 2];
                for (int i = 0; i < histCount; i++) {
                    histCoords[i * 2]     = (event.getHistoricalX(i) - mDrawRect.left) / mUnit;
                    histCoords[i * 2 + 1] = (event.getHistoricalY(i) - mDrawRect.top) / mUnit;
                }
            }
            return mHandler.onTouchEventUnit(action, unitX, unitY, histCoords);
        }

        if (!mScrollable) return false;
        mGestureDetector.onTouchEvent(event);
        boolean ret = mIsScaling;
        if (!mIsScaling) {
            switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mIsMoving = true;
                mFocusX = x;
                mFocusY = y;
                ret = true;
                break;
            case MotionEvent.ACTION_MOVE:
                if (mIsMoving) {
                    mDrawRect.offset(x - mFocusX, y - mFocusY);
                    mFocusX = x;
                    mFocusY = y;
                    adjustDrawRect();
                    invalidate();
                    ret = true;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (mIsMoving) {
                    mIsMoving = false;
                    ret = true;
                }
                break;
            }
        }
        return ret;
    }

    /*-----------------------------------------------------------------------*/

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float span = detector.getCurrentSpan();
        if (span > 0) {
            float unit = mScalingUnit * span / mScalingSpan;
            if (!mIsSmooth) {
                unit = Math.round(unit);
            }
            if (unit < mMinUnit) {
                unit = mMinUnit;
            }
            if (unit > mMaxUnit) {
                unit = mMaxUnit;
            }
            if (unit != mUnit) {
                mUnit = unit;
                float dx = mFocusX - mFocusUnitX * unit;
                float dy = mFocusY - mFocusUnitY * unit;
                mDrawRect.set(dx, dy,
                        dx + mSrcRect.width() * mUnit, dy + mSrcRect.height() * mUnit);
                adjustDrawRect();
                invalidate();
            }
        }
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        mIsMoving = false;
        if (mBitmap != null) {
            mFocusX = detector.getFocusX();
            mFocusY = detector.getFocusY();
            mFocusUnitX = (mFocusX - mDrawRect.left) / mUnit;
            mFocusUnitY = (mFocusY - mDrawRect.top) / mUnit;
            mIsScaling = true;
            mScalingUnit = mUnit;
            mScalingSpan = detector.getCurrentSpan();
            return true;
        }
        return false;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        mIsScaling = false;
    }

    /*-----------------------------------------------------------------------*/

    public void setBitmap(Bitmap bmp) {
        setBitmap(bmp, 0, 0, (bmp == null) ? 0 : bmp.getWidth(),
                (bmp == null) ? 0 : bmp.getHeight());
    }

    public void setBitmap(Bitmap bmp, int l, int t, int r, int b) {
        mBitmap = bmp;
        mSrcRect.set(l, t, r, b);
        calcCoords();
        invalidate();
    }

    public void setBitmap(Bitmap bmp, boolean smooth) {
        mIsSmooth = smooth;
        setBitmap(bmp);
    }

    public void setScaleRange(float min, float max) {
        if (min <= max) {
            mMinUnit = min;
            mMaxUnit = max;
            if (mUnit < min || mUnit > max) {
                invalidate();
            }
        }
    }

    public void setGridColor(int color, boolean dotted) {
        mGridColor = color;
        mDotted = dotted;
        invalidate();
    }

    public void setFrameColor(int color) {
        mFrameColor = color;
        invalidate();
    }

    public void setScrollable(boolean enabled) {
        mScrollable = enabled;
    }

    public void setEventHandler(EventHandler handler) {
        mHandler = handler;
    }

    public void invalidateUnit(int x, int y) {
        float dx = mDrawRect.left;
        float dy = mDrawRect.top;
        invalidate((int) (dx + x * mUnit), (int) (dy + y * mUnit),
                (int) (dx + (x + 1) * mUnit), (int) (dy + (y + 1) * mUnit));
    }

    public void invalidateUnit(int l, int t, int r, int b) {
        if (l > r) {
            invalidateUnit(r, t, l, b);
        } else if (t > b) {
            invalidateUnit(l, b, r, t);
        } else {
            float dx = mDrawRect.left;
            float dy = mDrawRect.top;
            invalidate((int) (dx + l * mUnit), (int) (dy + t * mUnit),
                    (int) (dx + (r + 1) * mUnit), (int) (dy + (b + 1) * mUnit));
        }
    }

    public void getBitmapDrawRect(RectF outRect) {
        if (outRect != null) {
            outRect.set(mDrawRect);
        }
    }

    /*-----------------------------------------------------------------------*/

    private void calcCoords() {
        if (mBitmap == null) {
            mDrawRect.set(0, 0, 0, 0);
            return;
        }
        float sw = mSrcRect.width();
        float sh = mSrcRect.height();
        float dw = getWidth();
        float dh = getHeight();
        if (sw == 0 || sh == 0 || dw == 0 || dh == 0) {
            mDrawRect.set(0, 0, 0, 0);
            return;
        }
        mUnit = Math.min((dw - 2f) / sw, (dh - 2f) / sh);
        if (!mIsSmooth) {
            mUnit = (float) Math.floor(mUnit);
        }
        if (mUnit < mMinUnit) {
            mUnit = mMinUnit;
        }
        if (mUnit > mMaxUnit) {
            mUnit = mMaxUnit;
        }
        float dx = (dw - sw * mUnit) / 2f;
        float dy = (dh - sh * mUnit) / 2f;
        mDrawRect.set(dx, dy, dx + sw * mUnit, dy + sh * mUnit);
    }

    private void adjustDrawRect() {
        int dw = getWidth();
        int dh = getHeight();
        int mw = dw / 2;
        int mh = dh / 2;

        if (dw - mDrawRect.width() > mw * 2) {
            mDrawRect.offset((dw - mDrawRect.width()) / 2 - mDrawRect.left, 0);
        } else if (mDrawRect.left > mw){
            mDrawRect.offset(mw - mDrawRect.left, 0);
        } else if (dw - mDrawRect.right > mw){
            mDrawRect.offset(dw - mDrawRect.right - mw, 0);
        }

        if (dh - mDrawRect.height() > mh * 2) {
            mDrawRect.offset(0, (dh - mDrawRect.height()) / 2 - mDrawRect.top);
        } else if (mDrawRect.top > mh){
            mDrawRect.offset(0, mh - mDrawRect.top);
        } else if (dh - mDrawRect.bottom > mh){
            mDrawRect.offset(0, dh - mDrawRect.bottom - mh);
        }
    }

}
