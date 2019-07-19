package com.xmy.floatlibrary.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.*;
import android.widget.FrameLayout;
import com.xmy.floatlibrary.utils.SystemUtils;

/**
 * FloatWindowView:悬浮窗控件V1-利用windowManger控制窗口
 *
 * @author Nonolive-杜乾 Created on 2017/12/12 - 17:16.
 * E-mail:dusan.du@nonolive.com
 */
public class FloatWindowView extends FrameLayout implements IFloatView {
    private static final String TAG = FloatWindowView.class.getSimpleName();
    private float xInView;
    private float yInView;
    private float xInScreen;
    private float yInScreen;
    private float xDownInScreen;
    private float yDownInScreen;
    private Context mContext;
    private View contentWrap;

    private WindowManager mWindowManager = null;
    private WindowManager.LayoutParams mWindowParams = null;
    private FloatViewParams params = null;
    private FloatViewListener listener;
    private int statusBarHeight = 0;
    private int screenWidth;
    /***初始宽度**/
    private int mMinWidth;
    /***窗口高/宽比**/
    private float mRatio = 1.77f;

    public FloatWindowView(Context context) {
        super(context);
        init();
    }

    public FloatWindowView(Context mContext, FloatViewParams floatViewParams, WindowManager.LayoutParams wmParams, View floatView) {
        super(mContext);
        this.params = floatViewParams;
        this.mWindowParams = wmParams;
        this.contentWrap = floatView;
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
        contentWrap.setOnTouchListener(onMovingTouchListener);
        final int lastViewWidth = params.contentWidth;
        final int lastViewHeight = (int) (lastViewWidth * mRatio);
        updateViewLayoutParams(lastViewWidth, lastViewHeight);
        addView(contentWrap);
        contentWrap.post(new Runnable() {
            @Override
            public void run() {
                updateWindowWidthAndHeight(lastViewWidth, lastViewHeight);
            }
        });
    }

    private void initData() {
        mContext = getContext();
        mWindowManager = SystemUtils.getWindowManager(mContext);
        statusBarHeight = params.statusBarHeight;
        screenWidth = params.screenWidth;
        mMinWidth = params.mMinWidth;
        mRatio = params.mRatio;
    }

    private void updateViewLayoutParams(int width, int height) {
        if (contentWrap != null) {
            ViewGroup.LayoutParams layoutParams = contentWrap.getLayoutParams();
            layoutParams.height = height;
            layoutParams.width = width;
            contentWrap.setLayoutParams(layoutParams);
        }
    }

    public int getContentViewWidth() {
        return contentWrap != null ? contentWrap.getWidth() : mMinWidth;
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

    private boolean isMoving = false;
    private final OnTouchListener onMovingTouchListener = new OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return onTouchEvent2(event);
        }
    };

    private long firstClickTime;//第一次点击
    private int countClick = 0;
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
    private boolean canClick = true;//是否可以点击
    private final Runnable canClickRunnable = new Runnable() {
        @Override
        public void run() {
            canClick = true;
        }
    };

    private Handler handler = new Handler(Looper.getMainLooper());
    /***统计双击缩放的次数**/
    private int scaleCount = 1;

    public boolean onTouchEvent2(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isMoving = false;
                xInView = event.getX();
                yInView = event.getY();
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
                        if (secondClickTime - firstClickTime < 300) {
                            //双击
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
        updateWindowWidthAndHeight(width, height);
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

    private boolean isClickedEvent() {
        int scaledTouchSlop = ViewConfiguration.get(mContext).getScaledTouchSlop();
        // 是点击事件
        return Math.abs(xDownInScreen - xInScreen) <= scaledTouchSlop
                && Math.abs(yDownInScreen - yInScreen) <= scaledTouchSlop;
    }

    /**
     * 更新悬浮窗位置
     */
    private void updateViewPosition() {
        int x = (int) (xInScreen - xInView);
        int y = (int) (yInScreen - yInView);
        //防止超出通知栏
        if (y < statusBarHeight) {
            y = statusBarHeight;
        }
        updateWindowXYPosition(x, y);
    }

    /**
     * 更新窗体坐标位置
     *
     * @param x x坐标
     * @param y 坐标
     */
    private synchronized void updateWindowXYPosition(int x, int y) {
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

    @Override
    public void setFloatViewListener(FloatViewListener listener) {
        this.listener = listener;
    }
}
