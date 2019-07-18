
package com.xmy.floatlibrary.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

/**
 * NonoFloatView:悬浮窗控件V2,普通的实现
 *
 * @author Nonolive-杜乾 Created on 2017/12/12 - 17:16.
 * E-mail:dusan.du@nonolive.com
 */
public class FloatView extends FrameLayout implements IFloatView {

    private Context mContext;
    private View floatView;
    private FloatViewParams params = null;
    private FloatViewListener listener;

    private int screenWidth;
    /***初始宽度**/
    private int mMinWidth;
    /***视频最大宽度**/
    private int mMaxWidth;
    /***窗口高/宽比**/
    private float mRatio = 1.77f;
    /***是否可以点击**/
    private boolean canClick = true;
    /***统计双击缩放的次数**/
    private int scaleCount = 1;
    /***是否恢复上次页面位置**/
    private boolean isRestorePosition = false;
    /***悬浮窗可以移动的区域高度**/
    private int realHeight;
    /***第一次点击**/
    private long firstClickTime;
    /***是否正在拖拽中**/
    private boolean isDragged = false;
    /***是否进入编辑状态**/
    private boolean isEdit = false;

    private int viewMargin;
    private float xInView;
    private float yInView;
    private float xInScreen;
    private float yInScreen;
    private float xDownInScreen;
    private float yDownInScreen;
    private int countClick = 0;
    private boolean isMoving = false;
    private int oldX = 0;
    private int oldY = 0;
    private int mRight = 0;
    private int mBottom = 0;

    public final static int w = 150;
    public final static int h = 150;

    private final Runnable canClickRunnable = new Runnable() {
        @Override
        public void run() {
            canClick = true;
        }
    };

    private Handler handler = new Handler(Looper.getMainLooper());

    public FloatView(Context mContext) {
        super(mContext);
        init();
    }

    public FloatView(@NonNull Context mContext, @NonNull FloatViewParams params, View childView) {
        super(mContext);
        this.params = params;
        this.floatView = childView;
        init();
    }

    private void init() {
        try {
            initData();
            initView();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initView() {
        floatView.addOnLayoutChangeListener(onLayoutChangeListener);
        floatView.setOnTouchListener(onMovingTouchListener);
        int lastViewWidth = params.contentWidth;
        int lastViewHeight = (int) (lastViewWidth * mRatio);
        updateViewLayoutParams(lastViewWidth, lastViewHeight);
        addView(floatView/*, new ViewGroup.LayoutParams(w, h)*/);
    }

    private void initData() {
        mContext = getContext();
        // 如果显示了通知栏，标题栏，移动的区域边界问题要处理一下
        if (params != null) {
            viewMargin = params.viewMargin;
            screenWidth = params.screenWidth;
            int screenHeight = params.screenHeight;
            int statusBarHeight = params.statusBarHeight;
            realHeight = screenHeight - statusBarHeight - params.titleBarHeight;
            mMaxWidth = params.mMaxWidth;
            mMinWidth = params.mMinWidth;
            mRatio = params.mRatio;

            oldX = params.x;
            oldY = params.y;
            mRight = params.x + params.width;
            mBottom = params.y + params.height;
        }
    }

    private void updateViewLayoutParams(int width, int height) {
        if (floatView != null) {
//            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) floatView.getLayoutParams();
//            layoutParams.height = height;
//            layoutParams.width = width;
//            floatView.setLayoutParams(layoutParams);
//            params.width = width;
//            params.height = height;
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (floatView != null && !isRestorePosition) {
            floatView.layout(oldX, oldY, oldX + params.width, oldY + params.height);
            isRestorePosition = true;
        }
    }

    /**
     * 监听layout变化
     * left: View 左上顶点相对于父容器的横坐标
     * top: View 左上顶点相对于父容器的纵坐标
     * right: View 右下顶点相对于父容器的横坐标
     * bottom: View 右下顶点相对于父容器的纵坐标
     */
    private final OnLayoutChangeListener onLayoutChangeListener = new OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
            if (right != mRight || bottom != mBottom) {
                //Log.d("float", "dq onLayoutChange111 left=" + left + ",top=" + top + ",right=" + right + ",bottom=" + bottom);
                int width = floatView.getWidth();
                int height = floatView.getHeight();
                //防止拖出屏幕外部,顶部和右下角处理
                int l = mRight - width;
                int t = mBottom - height;
                int r = mRight;
                int b = mBottom;
                if (l < -viewMargin) {
                    l = -viewMargin;
                    r = l + width;
                }
                if (t < -viewMargin) {
                    t = -viewMargin;
                    b = t + height;
                }
                /*if (b > realHeight) {
                    b = realHeight;
                }*/
                try {
                    floatView.layout(l, t, r, b);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                params.x = l;
                params.y = t;
            }
        }
    };


    public int getContentViewWidth() {
        return floatView != null ? floatView.getWidth() : mMinWidth;
    }

    private final OnTouchListener onMovingTouchListener = new OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return onTouchEvent2(event);
        }
    };

    private final Runnable clickRunnable = new Runnable() {
        @Override
        public void run() {
            //Logger.d("dq-fw canClick=" + canClick);
            if (countClick == 1 && canClick) {
                if (listener != null) {
                    listener.onClick();
                }
            }
            countClick = 0;
        }
    };

    public boolean onTouchEvent2(MotionEvent event) {
        if (isDragged) {
            return true;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isMoving = false;
                xInView = event.getX();
                yInView = event.getY();
                Rect rect = new Rect();
                floatView.getGlobalVisibleRect(rect);
//                if (!rect.contains((int) xInView, (int) yInView)) {
//                    //不在移动的view内，不处理
//                    return false;
//                }
                xDownInScreen = event.getRawX();
                yDownInScreen = event.getRawY();
                xInScreen = xDownInScreen;
                yInScreen = yDownInScreen;
                break;

            case MotionEvent.ACTION_MOVE:
                // 手指移动的时候更新小悬浮窗的位置
                xInScreen = event.getRawX();
                yInScreen = event.getRawY();
                if (!isMoving) {
                    isMoving = !isClickedEvent();
                } else {
                    updateViewPosition();
                }
                break;

            case MotionEvent.ACTION_UP:
                if (isClickedEvent()) {
                    countClick++;
                    if (countClick == 1) {
                        firstClickTime = System.currentTimeMillis();
                        handler.removeCallbacks(clickRunnable);
                        handler.postDelayed(clickRunnable, 300);
                    } else if (countClick == 2) {
                        long secondClickTime = System.currentTimeMillis();
                        //双击
                        if (secondClickTime - firstClickTime < 300) {
                            if (listener != null) {
                                listener.onDoubleClick();
                            }
                            scaleCount++;
                            handleScaleEvent();
                            countClick = 0;
                            //2秒后才允许再次点击
                            canClick = false;
                            handler.removeCallbacks(canClickRunnable);
                            handler.postDelayed(canClickRunnable, 1000);
                        }
                    }
                } else {
                    if (null != listener) {
                        listener.onMoved();
                    }
                    countClick = 0;
                }
                isMoving = false;
                break;

            default:
                break;
        }
        return true;
    }

    private void handleScaleEvent() {
        //缩放级别
        int scaleLevel = scaleCount % 3;
        int width = getFloatWindowWidth(true, screenWidth, scaleLevel);
        int height = (int) (width * mRatio);
        updateViewLayoutParams(width, height);
        Log.d("dq-fw", "handleScaleEvent width=" + width + ",height=" + height);
    }

    public int getFloatWindowWidth(boolean isPortrait, int screenWidth, int scaleLevel) {
        int width = 0;
        if (scaleLevel == 0) {
            width = (int) (isPortrait ? screenWidth * 0.30f : screenWidth * 0.45f);
        } else if (scaleLevel == 1) {
            width = (int) (isPortrait ? screenWidth * 0.40f : screenWidth * 0.65f);
        } else if (scaleLevel == 2) {
            width = (int) (isPortrait ? screenWidth * 0.50f : screenWidth * 0.92f);
        }
        return width;
    }

    /**
     * 是否为点击事件
     */
    private boolean isClickedEvent() {
        int scaledTouchSlop = ViewConfiguration.get(mContext).getScaledTouchSlop();
        return Math.abs(xDownInScreen - xInScreen) <= scaledTouchSlop && Math.abs(yDownInScreen - yInScreen) <= scaledTouchSlop;
    }

    /**
     * 更新悬浮窗位置,此方法用于修正移动悬浮窗的边界问题，保证不移出应用可见范围
     * todo 待修正有标题栏的情况下的边界问题
     */
    private synchronized void updateViewPosition() {
        int x = (int) (xInScreen - xInView);
        int y = (int) (yInScreen - yInView);
        int dWidth;
        //边界处理
        if (x < -viewMargin) {
            x = -viewMargin;
        }
        dWidth = screenWidth - floatView.getWidth();

        if (x > dWidth) {
            x = dWidth;
        }

        if (y < -viewMargin) {
            y = -viewMargin;
        }
        int dHeight = realHeight - floatView.getHeight();
        if (y > dHeight) {
            y = dHeight;
        }
        Log.d("duqian", "dq updateViewPosition x=" + x + ",y=" + y);
        reLayoutContentView(x, y);
    }

    /**
     * 重新布局
     *
     * @param x 左上角x坐标
     * @param y 左上角y坐标
     */
    private void reLayoutContentView(int x, int y) {
        //更新起点
        params.x = x;
        params.y = y;
        mRight = x + floatView.getWidth();
        mBottom = y + floatView.getHeight();
        floatView.layout(x, y, mRight, mBottom);
    }

    @Override
    public FloatViewParams getParams() {
        params.contentWidth = getContentViewWidth();
        return params;
    }

    @Override
    public void setFloatViewListener(FloatViewListener listener) {
        this.listener = listener;
    }
}
