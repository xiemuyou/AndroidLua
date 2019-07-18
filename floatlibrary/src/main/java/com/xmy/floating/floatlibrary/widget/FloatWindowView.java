package com.xmy.floating.floatlibrary.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.xmy.floating.floatlibrary.FloatWindowManager;
import com.xmy.floating.floatlibrary.R;
import com.xmy.floating.floatlibrary.utils.SystemUtils;

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
    private TextView tvInfo;
    private RelativeLayout videoViewWrap;
    private RelativeLayout contentWrap;
    private ImageView iv_zoom_btn;

    private WindowManager mWindowManager = null;
    private WindowManager.LayoutParams mWindowParams = null;
    private FloatViewParams params = null;
    private FloatViewListener listener;
    private int statusBarHeight = 0;
    private int screenWidth;
    private int screenHeight;
    /***初始宽度**/
    private int mMinWidth;
    /***视频最大宽度**/
    private int mMaxWidth;
    /***窗口高/宽比**/
    private float mRatio = 1.77f;
    private int videoViewMargin;
    /***sdk版本是否>=23**/
    private boolean isSdkGt23 = false;
    /***是否正在拖拽中**/
    private boolean isDragged = false;
    /***是否进入编辑状态**/
    private boolean isEdit = false;
    /***x轴方向的变化量**/
    private int changedX = 0;
    /***缩放宽度变化量**/
    private int dx = 0;
    /***缩放高度变化量**/
    private int dy = 0;
    /***缩放前的x坐标**/
    private int startX = 0;
    /***缩放前的y坐标**/
    private int startY = 0;

    public FloatWindowView(Context context) {
        super(context);
        init();
    }

    public FloatWindowView(Context mContext, FloatViewParams floatViewParams, WindowManager.LayoutParams wmParams) {
        super(mContext);
        this.params = floatViewParams;
        this.mWindowParams = wmParams;
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
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View floatView = inflater.inflate(R.layout.float_view_inner_layout, null);
        contentWrap = floatView.findViewById(R.id.content_wrap);
        videoViewWrap = floatView.findViewById(R.id.videoViewWrap);
        tvInfo = floatView.findViewById(R.id.tv_info);
        tvInfo.setText(getResources().getString(R.string.title_alert_window));
        iv_zoom_btn = floatView.findViewById(R.id.iv_zoom_btn);

        iv_zoom_btn.setOnTouchListener(onZoomBtnTouchListener);
        contentWrap.setOnTouchListener(onMovingTouchListener);

        floatView.findViewById(R.id.iv_close_window).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != listener) {
                    listener.onClose();//关闭
                }
            }
        });

        final int lastViewWidth = params.contentWidth;
        final int lastViewHeight = (int) (lastViewWidth * mRatio);
        updateViewLayoutParams(lastViewWidth, lastViewHeight);
        addView(floatView);
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
        //要去掉状态栏高度
        screenHeight = params.screenHeight - statusBarHeight;
        videoViewMargin = params.viewMargin;
        mMaxWidth = params.mMaxWidth;
        mMinWidth = params.mMinWidth;
        mRatio = params.mRatio;
        //起点
        startX = params.x;
        startY = params.y;
        //isSdkGt23 = Build.VERSION.SDK_INT >= 23;
        // >=23的部分手机缩放会卡顿，系统弹窗更新位置迟缓不够平滑
    }

    private void updateViewLayoutParams(int width, int height) {
        if (contentWrap != null) {
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) contentWrap.getLayoutParams();
            layoutParams.height = height;
            layoutParams.width = width;
            contentWrap.setLayoutParams(layoutParams);
        }
    }

    private final OnTouchListener onZoomBtnTouchListener = new OnTouchListener() {
        float lastX = 0;
        float lastY = 0;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    isDragged = true;
                    lastX = event.getRawX();
                    lastY = event.getRawY();
                    changedX = 0;
                    break;
                case MotionEvent.ACTION_MOVE:
                    showZoomView();
                    handleMoveEvent(event);
                    break;
                case MotionEvent.ACTION_UP:
                    if (listener != null) {
                        listener.onDragged();
                    }
                    displayZoomViewDelay();
                    if (!isSdkGt23) {
                        //缩放完成，要调整悬浮窗到视频大小。由于wm更新布局不及时，会有闪烁的问题
                        rejuestWindow();
                    }
                    isDragged = false;
                    changedX = 0;
                    break;
                default:
                    break;
            }
            return true;
        }

        private void handleMoveEvent(MotionEvent event) {
            isDragged = true;
            float moveX = event.getRawX();
            float moveY = event.getRawY();
            float dx = moveX - lastX;
            float dy = moveY - lastY;
            double distance = Math.sqrt(dx * dx + dy * dy);
            if (distance >= 2) {//控制刷新频率
                //已经是最大或者最小不缩放
                int contentWidth = contentWrap.getWidth();
                if (moveY > lastY && moveX > lastX) {
                    //最小了，不能再小了
                    if (contentWidth == mMinWidth) {
                        return;
                    }
                    //缩小
                    distance = -distance;
                } else {
                    if (contentWidth == mMaxWidth) {
                        return;
                    }
                }
                //double angle = Math.atan2(Math.abs(dy), Math.abs(dx)) * 180 / Math.PI;
                //粗略计算
                int changedWidth = (int) (distance * Math.cos(45));
                if (!isSdkGt23) {
                    //调节内部view大小，先放大窗体到最大，方便调节大小
                    if (mWindowParams.width != mMaxWidth) {
                        updateWindowSize(mMaxWidth);
                    }
                    updateContentViewSize(changedWidth);
                } else {
                    //大于6.0则直接改变window大小
                    updateFloatWindowSize(changedWidth);
                }
            }
            lastX = moveX;
            lastY = moveY;
        }
    };

    /**
     * 更新FloatWindow的大小
     *
     * @param width 传入变化的宽度
     */
    private void updateFloatWindowSize(int width) {
        int currentWidth = mWindowParams.width;
        int newWidth = currentWidth + width;
        newWidth = checkWidth(newWidth);
        int height = (int) (newWidth * mRatio);
        setFloatViewXYPosition(width);
        //调整window的大小
        updateWindowWidthAndHeight(newWidth, height);
        //调整视频view的大小
        updateViewLayoutParams(newWidth, height);
    }

    /**
     * 设置悬浮窗坐标位置
     *
     * @param changedWidth view宽度的变化
     */
    private void setFloatViewXYPosition(int changedWidth) {
        changedX += changedWidth / 2;
        int x = startX - changedX;
        int y = (int) (startY - changedX * mRatio);
        int width = mWindowParams.width;
        if (width >= mMinWidth && width <= mMaxWidth) {
            mWindowParams.x = x;
            mWindowParams.y = y;
        }
    }

    /**
     * 改变窗体大小前，获取一下x，y变化大小
     */
    private void rejuestWindow() {
        dx = contentWrap.getLeft();
        dy = contentWrap.getTop();
        //修正窗体xy坐标
        fixWindowXYPosition();
        updateWindowSize(contentWrap.getWidth());
        if (dx > 0 && dy > 0) {
            removeCallbacks(updateWindowPostionRunnable);
            //回到缩放后的位置，用post，并且0 delay效果好一些
            long duration = 0;
            postDelayed(updateWindowPostionRunnable, duration);
        }
    }

    private final Runnable updateWindowPostionRunnable = new Runnable() {
        @Override
        public void run() {
            updateWindowXYPosition(mWindowParams.x + dx, mWindowParams.y + dy);
        }
    };

    public int getContentViewWidth() {
        return contentWrap != null ? contentWrap.getWidth() : mMinWidth;
    }

    /**
     * 更新内部view的大小
     *
     * @param width 传入变化的宽度
     */
    private void updateContentViewSize(int width) {
        int currentWidth = contentWrap.getWidth();
        int newWidth = currentWidth + width;
        newWidth = checkWidth(newWidth);
        int height = (int) (newWidth * mRatio);
        //调整视频view的大小
        updateViewLayoutParams(newWidth, height);
    }

    /**
     * 更新WM的宽高大小
     */
    private void updateWindowSize(int width) {
        width = checkWidth(width);
        int height = (int) (width * mRatio);
        updateWindowWidthAndHeight(width, height);
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
     * 修正大小，限制最大和最小值
     *
     * @param width
     * @return
     */
    private int checkWidth(int width) {
        if (width > mMaxWidth) {
            width = mMaxWidth;
        }
        if (width < mMinWidth) {
            width = mMinWidth;
        }
        return width;
    }

    /**
     * 调整悬浮窗坐标位置
     */
    private void fixWindowXYPosition() {
        int width = contentWrap.getWidth();
        if (mWindowParams.x + width >= screenWidth) {
            // 不让贴近右边和底部
            mWindowParams.x = screenWidth - width - 1;
        }
        if (mWindowParams.x <= 0) {
            mWindowParams.x = 0;
        }
        int height = contentWrap.getHeight();
        if (mWindowParams.y + height >= screenHeight) {
            mWindowParams.y = screenHeight - height - 1;
        }
        if (mWindowParams.y <= statusBarHeight) {
            mWindowParams.y = statusBarHeight;
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
        if (isDragged) {
            return true;
        }
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
                showZoomView();
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
                        if (secondClickTime - firstClickTime < 300) {//双击
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
                updateEditStatus();
                isMoving = false;
                break;
            default:
                break;
        }
        return true;
    }

    private void handleScaleEvent() {
        int scaleLevel = scaleCount % 3;//缩放级别
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
        //更新起点
        startX = x;
        startY = y;
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

    private void updateEditStatus() {
        handleZoomStatus();
        displayZoomViewDelay();
    }

    /**
     * 处理缩放按钮的状态
     */
    private void handleZoomStatus() {
        //左，上贴边时，隐藏dragView
        boolean isLeft = mWindowParams.x <= 0;
        boolean isTop = mWindowParams.y <= statusBarHeight;
        if (isLeft || isTop) {
            displayZoomView();
            // 贴边时设置视频margin
            if (isLeft && isTop) {
                updateVideoMargin(0, 0, videoViewMargin, videoViewMargin);
            } else if (isLeft) {
                updateVideoMargin(0, videoViewMargin, videoViewMargin, 0);
            } else {
                updateVideoMargin(videoViewMargin, 0, 0, videoViewMargin);
            }
        } else {
            showZoomView();
        }
    }

    private void showZoomView() {
        //&& isSdkGt23 只有6.0及以上才显示缩放按钮
        if (!isEdit) {
            updateVideoMargin(videoViewMargin, videoViewMargin, 0, 0);
            iv_zoom_btn.setVisibility(VISIBLE);
            videoViewWrap.setBackgroundColor(getResources().getColor(R.color.float_window_bg_border_edit));
            isEdit = true;
        }
    }

    /**
     * 调整视频view的边距
     */
    private void updateVideoMargin(int left, int top, int right, int bottom) {
        if (videoViewWrap != null) {
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) videoViewWrap.getLayoutParams();
            layoutParams.setMargins(left, top, right, bottom);
            videoViewWrap.setLayoutParams(layoutParams);
        }
    }

    private void displayZoomView() {
        isEdit = false;
        iv_zoom_btn.setVisibility(GONE);
        videoViewWrap.setBackgroundColor(getResources().getColor(R.color.float_window_bg_border_normal));
    }

    private void displayZoomViewDelay() {
        removeCallbacks(dispalyZoomBtnRunnable);
        postDelayed(dispalyZoomBtnRunnable, 2000);
    }

    private final Runnable dispalyZoomBtnRunnable = new Runnable() {
        @Override
        public void run() {
            displayZoomView();
        }
    };

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

    public void setWindowType(int floatWindowType) {
        if (floatWindowType == FloatWindowManager.FW_TYPE_APP_DIALOG) {
            tvInfo.setText(getResources().getString(R.string.title_float_window_dialog));
        } else if (floatWindowType == FloatWindowManager.FW_TYPE_ALERT_WINDOW) {
            tvInfo.setText(getResources().getString(R.string.title_alert_window));
        }
    }
}
