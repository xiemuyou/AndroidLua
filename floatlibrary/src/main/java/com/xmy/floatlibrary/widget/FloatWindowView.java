package com.xmy.floatlibrary.widget;

import android.content.Context;
import android.view.*;
import com.xmy.floatlibrary.utils.SystemUtils;

/**
 * FloatWindowView:悬浮窗控件V1-利用windowManger控制窗口
 *
 * @author Nonolive-杜乾 Created on 2017/12/12 - 17:16.
 * E-mail:dusan.du@nonolive.com
 */
public class FloatWindowView extends FloatView {

    private WindowManager mWindowManager = null;
    private WindowManager.LayoutParams mWindowParams = null;
    /***初始宽度**/
    private int mMinWidth;

    public FloatWindowView(Context context) {
        super(context);
        initWindowView();
    }

    public FloatWindowView(Context mContext, FloatViewParams floatViewParams, WindowManager.LayoutParams wmParams, View floatView) {
        super(mContext);
        this.params = floatViewParams;
        this.mWindowParams = wmParams;
        this.floatView = floatView;
        initWindowView();
    }

    @Override
    protected void init() {
    }

    protected void initWindowView() {
        mWindowManager = SystemUtils.getWindowManager(getContext());
        mMinWidth = params.mMinWidth;
        //窗口高/宽比
        float mRatio = params.mRatio;
        floatView.setOnTouchListener(onMovingTouchListener);
        final int lastViewWidth = params.contentWidth;
        final int lastViewHeight = (int) (lastViewWidth * mRatio);
        updateViewLayoutParams(lastViewWidth, lastViewHeight);
        addView(floatView);
        floatView.post(new Runnable() {
            @Override
            public void run() {
                updateWindowWidthAndHeight(lastViewWidth, lastViewHeight);
            }
        });
    }

    private void updateViewLayoutParams(int width, int height) {
        if (floatView != null) {
            ViewGroup.LayoutParams layoutParams = floatView.getLayoutParams();
            layoutParams.height = height;
            layoutParams.width = width;
            floatView.setLayoutParams(layoutParams);
        }
    }

    public int getContentViewWidth() {
        return floatView != null ? floatView.getWidth() : mMinWidth;
    }

    /**
     * 更新WM的宽高大小
     */
    private synchronized void updateWindowWidthAndHeight(int width, int height) {
        if (mWindowManager != null) {
            mWindowParams.width = width;
            mWindowParams.height = height;
            mWindowManager.updateViewLayout(this, mWindowParams);
        }
    }

    /**
     * 更新悬浮窗位置
     */
    @Override
    protected boolean updateViewPosition() {
        int x = (int) (xInScreen - tx);
        int y = (int) (yInScreen - ty);
        //防止超出通知栏
        if (y < params.statusBarHeight) {
            y = params.statusBarHeight;
        }
        updateWindowPosition(x, y);
        return true;
    }

    protected synchronized void updateWindowPosition(int x, int y) {
        if (mWindowManager != null) {
            mWindowParams.x = x;
            mWindowParams.y = y;
            mWindowManager.updateViewLayout(this, mWindowParams);
        }
    }

    @Override
    public FloatViewParams getParams() {
        params.contentWidth = getContentViewWidth();
        params.x = mWindowParams.x;
        params.y = mWindowParams.y;
        params.width = mWindowParams.width;
        params.height = mWindowParams.height;
        return params;
    }
}
