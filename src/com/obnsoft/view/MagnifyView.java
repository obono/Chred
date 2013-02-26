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
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;

public class MagnifyView extends View implements OnScaleGestureListener {

    private int     mUnit = 1;
    private int     mMinUnit = 1;
    private int     mMaxUnit = 64;
    private int     mGridColor = Color.TRANSPARENT;
    private int     mFrameColor = Color.WHITE;
    private boolean mDotted;
    private boolean mScrollable;

    private boolean mIsMoving;
    private boolean mIsScaling;
    private int     mScalingUnit;
    private float   mScalingSpan;
    private int     mFocusX;
    private int     mFocusY;
    private int     mFocusUnitX;
    private int     mFocusUnitY;

    private Bitmap  mBitmap;
    private Rect    mWorkRect = new Rect();
    private Rect    mSrcRect = new Rect();
    private Rect    mDrawRect = new Rect();
    private Paint   mPaint = new Paint();

    private EventHandler mHandler;
    private ScaleGestureDetector mGestureDetector;

    /*-----------------------------------------------------------------------*/

    public interface EventHandler {
        public boolean onTouchEventUnit(MotionEvent ev, int unitX, int unitY);
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
        mPaint.setStyle(Style.STROKE);
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
        int cl = Math.max(mWorkRect.left, mDrawRect.left);
        int cr = Math.min(mWorkRect.right, mDrawRect.right);
        int ct = Math.max(mWorkRect.top, mDrawRect.top);
        int cb = Math.min(mWorkRect.bottom, mDrawRect.bottom);
        cl -= (cl - mDrawRect.left) % mUnit;
        ct -= (ct - mDrawRect.top) % mUnit;

        mPaint.setColor(mGridColor);
        if (mGridColor != Color.TRANSPARENT) {
            int x1 = cl, x2 = cl;
            int y1 = ct, y2 = ct;
            while (x1 < cr || y1 < cb) {
                if (x1 < cr) x1 += mUnit; else y1 += mUnit;
                if (y2 < cb) y2 += mUnit; else x2 += mUnit;
                canvas.drawLine(x1, y1, x2, y2, mPaint);
            }
        }
        canvas.drawBitmap(mBitmap, mSrcRect, mDrawRect, null);
        if (mPaint.getColor() != Color.TRANSPARENT) {
            if (mDotted) {
                for (int x = cl; x <= cr; x += mUnit) {
                    for (int y = ct; y <= cb; y += mUnit) {
                        canvas.drawPoint(x, y, mPaint);
                    }
                }
            } else {
                for (int x = cl; x <= cr; x += mUnit) {
                    canvas.drawLine(x, ct, x, cb, mPaint);
                }
                for (int y = ct; y <= cb; y += mUnit) {
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
        int x = (int) event.getX();
        int y = (int) event.getY();
        if (mHandler != null) {
            int unitX = (x - mDrawRect.left) / mUnit;
            int unitY = (y - mDrawRect.top) / mUnit;
            return mHandler.onTouchEventUnit(event, unitX, unitY);
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
            int unit = (int) (mScalingUnit * span / mScalingSpan + .5);
            if (unit < mMinUnit) {
                unit = mMinUnit;
            }
            if (unit > mMaxUnit) {
                unit = mMaxUnit;
            }
            if (unit != mUnit) {
                mUnit = unit;
                int dx = (int) (mFocusX - (mFocusUnitX + .5f) * unit);
                int dy = (int) (mFocusY - (mFocusUnitY + .5f) * unit);
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
            mFocusX = (int) detector.getFocusX();
            mFocusY = (int) detector.getFocusY();
            mFocusUnitX = ((int) mFocusX - mDrawRect.left) / mUnit;
            mFocusUnitY = ((int) mFocusY - mDrawRect.top) / mUnit;
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

    public void setScaleRange(int min, int max) {
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
        int dx = mDrawRect.left;
        int dy = mDrawRect.top;
        invalidate(dx + x * mUnit, dy + y * mUnit, dx + (x + 1) * mUnit, dy + (y + 1) * mUnit);
    }

    public void invalidateUnit(int l, int t, int r, int b) {
        if (l > r) {
            invalidateUnit(r, t, l, b);
        } else if (t > b) {
            invalidateUnit(l, b, r, t);
        } else {
            int dx = mDrawRect.left;
            int dy = mDrawRect.top;
            invalidate(dx + l * mUnit, dy + t * mUnit, dx + (r + 1) * mUnit, dy + (b + 1) * mUnit);
        }
    }

    /*-----------------------------------------------------------------------*/

    private void calcCoords() {
        if (mBitmap == null) {
            mDrawRect.set(0, 0, 0, 0);
            return;
        }
        int sw = mSrcRect.width();
        int sh = mSrcRect.height();
        int dw = getWidth();
        int dh = getHeight();
        if (sw == 0 || sh == 0 || dw == 0 || dh == 0) {
            mDrawRect.set(0, 0, 0, 0);
            return;
        }
        mUnit = Math.min((dw - 2) / sw, (dh - 2) / sh);
        if (mUnit < mMinUnit) {
            mUnit = mMinUnit;
        }
        int dx = (dw - sw * mUnit) / 2;
        int dy = (dh - sh * mUnit) / 2;
        mDrawRect.set(dx, dy, dx + sw * mUnit, dy + sh * mUnit);
    }

    private void adjustDrawRect() {
        int dw = getWidth();
        int dh = getHeight();
        int margin = Math.min(dw, dh) / 4;

        if (dw - mDrawRect.width() > margin * 2) {
            mDrawRect.offset((dw - mDrawRect.width()) / 2 - mDrawRect.left, 0);
        } else if (mDrawRect.left > margin){
            mDrawRect.offset(margin - mDrawRect.left, 0);
        } else if (dw - mDrawRect.right > margin){
            mDrawRect.offset(dw - mDrawRect.right - margin, 0);
        }

        if (dh - mDrawRect.height() > margin * 2) {
            mDrawRect.offset(0, (dh - mDrawRect.height()) / 2 - mDrawRect.top);
        } else if (mDrawRect.top > margin){
            mDrawRect.offset(0, margin - mDrawRect.top);
        } else if (dh - mDrawRect.bottom > margin){
            mDrawRect.offset(0, dh - mDrawRect.bottom - margin);
        }
    }

}
