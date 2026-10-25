/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.liskovsoft.smartyoutubetv2.tv.ui.mod.leanback.playerglue.tweaks;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.Gravity;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.ArrayList;

import com.liskovsoft.smartyoutubetv2.tv.hunys.HunysUi;

class ControlBar extends LinearLayout {

    public interface OnChildFocusedListener {

        public void onChildFocusedListener(View child, View focused);
    }

    private int mChildMarginFromCenter;
    private OnChildFocusedListener mOnChildFocusedListener;
    // Can't set to static. Because we have two control bars.
    private int mLastFocusIndex = -1;
    // MOD: Sync focus index between rows
    private static int sSharedFocusIndex = -1;
    private boolean mIsSharedFocusEnabled = true;
    private boolean mDefaultFocusToMiddle = true;
    private boolean mFocusRecovery = true;
    // Hunys mod: fără distanțe egale între centre, pastilă de sticlă în spatele grupului din dreapta
    // Hunys mod: no equal center spacing, glass pill behind the right group
    private boolean mIsHunys;
    private Paint mHunysGlassPaint;
    private Paint mHunysStrokePaint;
    private Paint mHunysDividerPaint;
    private final RectF mHunysRect = new RectF();

    public ControlBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        resetFocus(); // MOD: reset global focus index
        initHunys(context);
    }

    public ControlBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        resetFocus(); // MOD: reset global focus index
        initHunys(context);
    }

    private void initHunys(Context context) {
        mIsHunys = HunysUi.isHunys(context);

        if (!mIsHunys) {
            return;
        }

        mIsSharedFocusEnabled = false;
        setGravity(Gravity.CENTER_VERTICAL);
        setClipChildren(false);
        setClipToPadding(false);
        setWillNotDraw(false);

        mHunysGlassPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mHunysGlassPaint.setColor(0x1FFFFFFF);
        mHunysStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mHunysStrokePaint.setStyle(Paint.Style.STROKE);
        mHunysStrokePaint.setStrokeWidth(HunysUi.dp(context, 1));
        mHunysStrokePaint.setColor(0x1AFFFFFF);
        mHunysDividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mHunysDividerPaint.setStrokeWidth(HunysUi.dp(context, 1));
        mHunysDividerPaint.setColor(0x2EFFFFFF);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (mIsHunys) {
            drawHunysGlass(canvas);
        }

        super.dispatchDraw(canvas);
    }

    private void drawHunysGlass(Canvas canvas) {
        int spacer = -1;

        for (int i = 0; i < getChildCount(); i++) {
            LayoutParams lp = (LayoutParams) getChildAt(i).getLayoutParams();
            if (lp.weight > 0) {
                spacer = i;
                break;
            }
        }

        int first = spacer + 1;
        int last = getChildCount() - 1;

        if (spacer < 0 || first > last) {
            return;
        }

        float centerY = getHeight() / 2f;
        float half = HunysUi.dp(getContext(), 19);
        mHunysRect.set(getChildAt(first).getLeft() - HunysUi.dp(getContext(), 3), centerY - half,
                getChildAt(last).getRight() + HunysUi.dp(getContext(), 3), centerY + half);
        canvas.drawRoundRect(mHunysRect, half, half, mHunysGlassPaint);
        canvas.drawRoundRect(mHunysRect, half, half, mHunysStrokePaint);

        // Linii între like | dislike | ⚙ / dividers between like | dislike | settings
        float dividerHalf = HunysUi.dp(getContext(), 7);
        for (int i = first; i < Math.min(first + 2, last); i++) {
            float x = getChildAt(i).getRight();
            canvas.drawLine(x, centerY - dividerHalf, x, centerY + dividerHalf, mHunysDividerPaint);
        }
    }

    void setDefaultFocusToMiddle(boolean defaultFocusToMiddle) {
        mDefaultFocusToMiddle = defaultFocusToMiddle;
    }

    /**
     * MOD: enable/disable focus restoration
     */
    void setFocusRecovery(boolean focusRecovery) {
        mFocusRecovery = focusRecovery;
    }

    /**
     * MOD: synced navigation between rows
     */
    void setSharedFocusEnabled(boolean enabled) {
        mIsSharedFocusEnabled = enabled && !mIsHunys;
    }

    void resetFocus() {
        setFocusIndex(-1);
    }

    int getDefaultFocusIndex() {
        // Hunys mod: implicit pe ▶ (mijlocul lui ⏮ ▶ ⏭) / default on play (middle of prev, play, next)
        return mDefaultFocusToMiddle || mIsHunys ? getChildCount() / 2 : 0;
    }

    @Override
    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        if (getChildCount() > 0) {
            // MOD: fix situation when rows have different length
            if (getFocusIndex() >= getChildCount()) {
                setFocusIndex(getChildCount() - 1);
            }

            int index = getFocusIndex() >= 0 && getFocusIndex() < getChildCount()
                    ? getFocusIndex() : getDefaultFocusIndex();

            if (getChildAt(index).requestFocus(direction, previouslyFocusedRect)) {
                return true;
            }
        }
        return super.onRequestFocusInDescendants(direction, previouslyFocusedRect);
    }

    @Override
    public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
        if ((direction == ViewGroup.FOCUS_UP || direction == ViewGroup.FOCUS_DOWN)) {
            if (getFocusIndex() >= 0 && getFocusIndex() < getChildCount()) {
                views.add(getChildAt(getFocusIndex()));
            } else if (getChildCount() > 0) {
                views.add(getChildAt(getDefaultFocusIndex()));
            }
        } else {
            super.addFocusables(views, direction, focusableMode);
        }
    }

    public void setOnChildFocusedListener(OnChildFocusedListener listener) {
        mOnChildFocusedListener = listener;
    }

    public void setChildMarginFromCenter(int marginFromCenter) {
        mChildMarginFromCenter = mIsHunys ? 0 : marginFromCenter;
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        super.requestChildFocus(child, focused);
        if (mFocusRecovery) {
            setFocusIndex(indexOfChild(child));
        }
        if (mOnChildFocusedListener != null) {
            mOnChildFocusedListener.onChildFocusedListener(child, focused);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mChildMarginFromCenter <= 0) {
            return;
        }

        int totalExtraMargin = 0;
        for (int i = 0; i < getChildCount() - 1; i++) {
            View first = getChildAt(i);
            View second = getChildAt(i+1);
            int measuredWidth = first.getMeasuredWidth() + second.getMeasuredWidth();
            int marginStart = mChildMarginFromCenter - measuredWidth / 2;
            LayoutParams lp = (LayoutParams) second.getLayoutParams();
            int extraMargin = marginStart - lp.getMarginStart();
            lp.setMarginStart(marginStart);
            second.setLayoutParams(lp);
            totalExtraMargin += extraMargin;
        }
        setMeasuredDimension(getMeasuredWidth() + totalExtraMargin, getMeasuredHeight());
    }

    private void setFocusIndex(int index) {
        mLastFocusIndex = index;
        if (index != -1) // don't reset global focus between player launches
            sSharedFocusIndex = index;
    }

    private int getFocusIndex() {
        return mIsSharedFocusEnabled ? sSharedFocusIndex : mLastFocusIndex;
    }
}
