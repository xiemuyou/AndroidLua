
package com.xmy.floatlibrary.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import android.widget.Toast;
import androidx.annotation.NonNull;
import com.xmy.floatlibrary.utils.SystemUtils;

/**
 * NonoFloatView:悬浮窗控件V2,普通的实现
 *
 * @author Nonolive-杜乾 Created on 2017/12/12 - 17:16.
 * E-mail:dusan.du@nonolive.com
 */
@SuppressLint("ViewConstructor")
public class FloatView extends FrameLayout implements IFloatView {

    private Context mContext;
    private View floatView;
    private FloatViewParams params;
    private boolean isActionBar = false;

    private float x;
    private float y;
    private float tx;
    private float ty;
    private float sx;
    private float sy;
    private float mSx;
    private float mSy;
    private float xj;

    public FloatView(@NonNull Context context, @NonNull FloatViewParams params, @NonNull View childView) {
        super(context);
        this.params = params;
        this.floatView = childView;
        init(mContext);
    }

    private void init(@NonNull Context context) {
        mContext = context;
        if (floatView == null) {
            throw new RuntimeException("FloatView is null!");
        }
        floatView.setOnTouchListener(onMovingTouchListener);
        addView(floatView);
        post(runnable);
    }

    private Runnable runnable = new Runnable() {

        @Override
        public void run() {
            int y = (int) getPivotY();
            int max = Math.max(params.screenHeight, params.screenWidth);
            int min = Math.min(params.screenHeight, params.screenWidth);
            int temp = (SystemUtils.isLandscape(floatView) ? min : max) - params.statusBarHeight;
            isActionBar = (temp / 2) > y;
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    private final OnTouchListener onMovingTouchListener = new OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            //获取到状态栏的高度?
            Rect frame = new Rect();
            floatView.getWindowVisibleDisplayFrame(frame);
            // statusBarHeight是系统状态栏的高度
            int statusBarHeight = frame.top;
            // 获取相对屏幕的坐标，即以屏幕左上角为原点
            x = (int) event.getRawX();
            int ah = isActionBar ? SystemUtils.getStatusBarHeight(getContext()) : 0;
            y = (int) (event.getRawY() - statusBarHeight - ah);
            switch (event.getAction()) {
                // 捕获手指触摸按下动作
                case MotionEvent.ACTION_DOWN:
                    // 获取相对View的坐标，即以此View左上角为原点
                    tx = event.getX();
                    ty = event.getY();
                    mSx = x;
                    mSy = y;
                    sx = event.getRawX();
                    sy = event.getRawY();
                    xj = 0;
                    break;

                // 捕获手指触摸移动动作
                case MotionEvent.ACTION_MOVE:
                    float rx = event.getRawX();
                    float ry = event.getRawY();
                    xj = Math.max(Math.abs(rx - sx), Math.abs(xj));
                    xj = Math.max(Math.abs(ry - sy), Math.abs(xj));
                    updateViewPosition(false);
                    break;

                // 捕获手指触摸离开动作
                case MotionEvent.ACTION_UP:
                    updateViewPosition(true);
                    tx = ty = 0;
                    if ((x - mSx) < 5 && (y - mSy) < 5 && xj < 7) {
                        Toast.makeText(getContext(), "点击", Toast.LENGTH_SHORT).show();
                        //imageClick(context);
                    }
                    break;

                default:
                    break;
            }
            return true;
        }
    };

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        floatLayout(params.x, params.y, true);
    }

    /**
     * 更新悬浮窗位置,此方法用于修正移动悬浮窗的边界问题，保证不移出应用可见范围
     */
    private synchronized void updateViewPosition(boolean isRefresh) {
        // 更新浮动窗口位置参数
        int left = (int) (x - tx);
        int top = (int) (y - ty);
        int maxLeft = SystemUtils.getScreenWidth(this);
        params.isLeft = left < maxLeft / 2;
        floatLayout(left, top, isRefresh);
    }

    private void floatLayout(int left, int top, boolean isRefresh) {
        if (floatView == null) {
            return;
        }
        if (isRefresh) {
            //最小边距
            int minMargin = 5;
            left = params.isLeft ? minMargin : SystemUtils.getScreenWidth(this) - floatView.getWidth() - minMargin;

            int maxTop = SystemUtils.getScreenHeight(this) - floatView.getHeight();
            if (top < minMargin) {
                top = minMargin;
            } else if (top > maxTop) {
                top = maxTop - minMargin;
            }
        }
        params.x = left;
        params.y = top;
        int right = left + floatView.getWidth();
        int bottom = top + floatView.getHeight();
        floatView.layout(left, top, right, bottom);
    }

    @Override
    public FloatViewParams getParams() {
        return params;
    }

    @Override
    public void setFloatViewListener(FloatViewListener listener) {

    }
}
