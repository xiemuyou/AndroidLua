package com.xmy.floatlibrary;

import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import com.xmy.floatlibrary.utils.SystemUtils;
import com.xmy.floatlibrary.widget.*;

/**
 * FloatWindowManager:管理悬浮窗视频播放
 * android.view.WindowManager$BadTokenException:
 * Unable to add window android.view.ViewRootImpl$W@123e0ab --
 * permission denied for this window 2003,type
 *
 * @author Nonolive-杜乾 Created on 2017/12/12-17:35.
 * E-mail:dusan.du@nonolive.com
 */
public class FloatWindowManager {

    public static final int FW_TYPE_ROOT_VIEW = 10;
    public static final int FW_TYPE_APP_DIALOG = 11;
    public static final int FW_TYPE_ALERT_WINDOW = 12;
    private int floatWindowType = 0;
    private IFloatView floatView;
    private View childFloatView;
    private boolean isFloatWindowShowing = false;
    private FrameLayout contentView;
    private FloatViewParams floatViewParams;
    private WindowManager windowManager;
    private LastWindowInfo livePlayerWrapper;
    private Activity activity;

    public FloatWindowManager() {
        livePlayerWrapper = LastWindowInfo.getInstance();
    }

    /**
     * 显示悬浮窗口
     */
    public synchronized void showFloatWindow(Activity baseActivity, int floatWindowType, View floatView) {
        if (baseActivity == null) {
            return;
        }
        activity = baseActivity;
        Context mContext = baseActivity.getApplicationContext();
        showFloatWindow(mContext, floatWindowType, floatView);
    }

    private synchronized void showFloatWindow(Context context, int floatWindowType, View childFloatView) {
        if (context == null) {
            return;
        }
        this.floatWindowType = floatWindowType;
        this.childFloatView = childFloatView;
        try {
            isFloatWindowShowing = true;
            initFloatWindow(context);
        } catch (Exception e) {
            e.printStackTrace();
            isFloatWindowShowing = false;
        }
    }

    /**
     * 初始化悬浮窗
     */
    private void initFloatWindow(final Context mContext) {
        if (mContext == null) {
            return;
        }
        floatViewParams = initFloatViewParams(mContext);
        if (floatWindowType == FW_TYPE_ROOT_VIEW) {
            initCommonFloatView(mContext);
        } else {
            initSystemWindow(mContext);
        }
        isFloatWindowShowing = true;
    }

    /**
     * 直接在activity根布局添加悬浮窗
     *
     * @param mContext
     */
    private void initCommonFloatView(Context mContext) {
        if (activity == null || mContext == null) {
            return;
        }
        try {
            floatView = new FloatView(mContext, floatViewParams, childFloatView);
            View rootView = activity.getWindow().getDecorView().getRootView();
            contentView = rootView.findViewById(android.R.id.content);
            int[] loc = SystemUtils.getViewLocationFormXY(contentView);
            floatViewParams.actionBarHeight = loc[1];
            contentView.addView((View) floatView);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 利用系统弹窗实现悬浮窗
     *
     * @param mContext
     */
    private void initSystemWindow(Context mContext) {
        windowManager = SystemUtils.getWindowManager(mContext);
        WindowManager.LayoutParams wmParams = new WindowManager.LayoutParams();
        wmParams.packageName = mContext.getPackageName();
        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_SCALED
                | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

        if (floatWindowType == FW_TYPE_APP_DIALOG) {
            //这个一定要activity running
            //wmParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;//TYPE_TOAST
            //TYPE_TOAST targetSDK必须小于26
            wmParams.type = WindowManager.LayoutParams.TYPE_TOAST;
        } else if (floatWindowType == FW_TYPE_ALERT_WINDOW) {
            //需要权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                wmParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                wmParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
            }
        }
        wmParams.format = PixelFormat.RGBA_8888;
        wmParams.gravity = Gravity.START | Gravity.TOP;
        wmParams.width = floatViewParams.width;
        wmParams.height = floatViewParams.height;
        wmParams.x = floatViewParams.x;
        wmParams.y = floatViewParams.y;
        floatView = new FloatWindowView(mContext, floatViewParams, wmParams, childFloatView);
        try {
            windowManager.addView((View) floatView, wmParams);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setFloatClickListener(FloatViewListener clickListener) {
        if (clickListener == null || floatView == null) {
            return;
        }
        floatView.setFloatViewListener(clickListener);
    }

    /**
     * 初始化窗口参数
     *
     * @param mContext
     * @return
     */
    private synchronized FloatViewParams initFloatViewParams(final Context mContext) {
        int screenWidth = SystemUtils.getScreenWidth(mContext);
        int screenHeight = SystemUtils.getScreenHeight(mContext, false);
        int statusBarHeight = SystemUtils.getStatusBarHeight(activity);
        Log.d("dq", "screenWidth=" + screenWidth + ",screenHeight=" + screenHeight + ",statusBarHeight=" + statusBarHeight);
        //根据实际宽高和设计稿尺寸比例适应。
        int marginBottom = SystemUtils.dip2px(mContext, 150);
        if (floatWindowType == FW_TYPE_ROOT_VIEW) {
            marginBottom += statusBarHeight;
        }
        //设置窗口大小，已view、视频大小做调整
        int winWidth = LastWindowInfo.getInstance().getWidth();
        int winHeight = LastWindowInfo.getInstance().getHeight();
        int margin = SystemUtils.dip2px(mContext, 15);

        int width = childFloatView.getLayoutParams().width;
        float ratio = 1.0f * winHeight / winWidth;
        int height = childFloatView.getLayoutParams().height;

        //如果上次的位置不为null，则用上次的位置
        floatViewParams = livePlayerWrapper.getLastParams();
        if (floatViewParams == null) {
            floatViewParams = new FloatViewParams();
            floatViewParams.width = width;
            floatViewParams.height = height;
            floatViewParams.x = screenWidth - width;
            floatViewParams.y = screenHeight - height - marginBottom;
            floatViewParams.contentWidth = width;
        }
        floatViewParams.screenWidth = screenWidth;
        floatViewParams.screenHeight = screenHeight;
        floatViewParams.statusBarHeight = statusBarHeight;
        if (floatWindowType == FW_TYPE_ROOT_VIEW) {
            initTitleBarHeight(floatViewParams, statusBarHeight);
            //params.screenHeight = screenHeight - statusBarHeight;
        }
        floatViewParams.viewMargin = margin;
        floatViewParams.mMaxWidth = screenWidth / 2 + margin;
        floatViewParams.mMinWidth = width;
        floatViewParams.mRatio = ratio;
        return floatViewParams;
    }

    /**
     * 应用内悬浮窗，边界设定，除去标题栏（96px？），状态栏高度 待优化有actionBar的情况下的移动边界问题
     */
    private void initTitleBarHeight(FloatViewParams params, int statusBarHeight) {
        int titleBarHeight;
        if (activity != null) {
            int contentTop = activity.getWindow().findViewById(Window.ID_ANDROID_CONTENT).getTop();
            titleBarHeight = contentTop - statusBarHeight;
            titleBarHeight = titleBarHeight > 0 ? titleBarHeight : 0;
            params.titleBarHeight = titleBarHeight;
            Log.d("dq", "titleBarHeight=" + titleBarHeight);
        }
    }

    public IFloatView getFloatView() {
        return floatView;
    }

    /**
     * 隐藏悬浮视频窗口
     */
    public synchronized void dismissFloatWindow() {
        if (!isFloatWindowShowing) {
            return;
        }
        try {
            isFloatWindowShowing = false;
            if (floatView != null) {
                FloatViewParams floatViewParams = floatView.getParams();
                livePlayerWrapper.setLastParams(floatViewParams);
            }
            removeWindow();

            if (contentView != null && floatView != null) {
                contentView.removeView((View) floatView);
            }
            floatView = null;
            windowManager = null;
            contentView = null;
            //防止activity泄漏
            activity = null;
        } catch (Exception e) {
            //nothing
        }
    }

    private void removeWindow() {
        if (windowManager != null && floatView != null) {
            windowManager.removeViewImmediate((View) floatView);
        }
    }
}