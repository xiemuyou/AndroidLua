package com.xmy.floatlibrary.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.ViewUtils;

import java.lang.reflect.Field;

/**
 * Description:工具类
 *
 * @author 杜乾-Dusan,Created on 2018/2/9 - 16:11.
 * E-mail:duqian2010@gmail.com
 */
public class SystemUtils {

    private static int screenHeight = 0;
    private static int screenWidth = 0;
    private static int statusBarHeight = 0;

    private SystemUtils() {
    }

    /**
     * 获取屏幕宽度
     */
    public static int getScreenWidth(Context mContext) {
        if (screenWidth > 0) {
            return screenWidth;
        }

        if (mContext == null) {
            return 0;
        }
        return mContext.getResources().getDisplayMetrics().widthPixels;
    }

    /**
     * 获取屏幕高度,是否包含导航栏高度
     */
    public static int getScreenHeight(Context mContext, boolean isIncludeNav) {
        if (mContext == null) {
            return 0;
        }
        int screenHeight = getScreenHeight(mContext);
        if (isIncludeNav) {
            return screenHeight;
        } else {
            return screenHeight - getNavigationBarHeight(mContext);
        }
    }

    /**
     * 获取屏幕高(包括底部虚拟按键)
     *
     * @param mContext
     * @return
     */
    public static int getScreenHeight(Context mContext) {
        if (screenHeight > 0) {
            return screenHeight;
        }
        if (mContext == null) {
            return 0;
        }
        DisplayMetrics displayMetrics = new DisplayMetrics();
        Display display = getWindowManager(mContext).getDefaultDisplay();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                display.getRealMetrics(displayMetrics);
            } else {
                display.getMetrics(displayMetrics);
            }
            screenHeight = displayMetrics.heightPixels;
        } catch (Exception e) {
            screenHeight = display.getHeight();
        }
        return screenHeight;
    }

    /**
     * 获取WindowManager。
     */
    public static WindowManager getWindowManager(Context mContext) {
        if (mContext == null) {
            return null;
        }
        return (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
    }

    /**
     * 获取NavigationBar的高度
     */
    public static int getNavigationBarHeight(Context mContext) {
        if (!hasNavigationBar(mContext)) {
            return 0;
        }
        Resources resources = mContext.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height",
                "dimen", "android");
        return resources.getDimensionPixelSize(resourceId);
    }

    /**
     * 是否存在NavigationBar
     */
    public static boolean hasNavigationBar(Context mContext) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Display display = getWindowManager(mContext).getDefaultDisplay();
            Point size = new Point();
            Point realSize = new Point();
            display.getSize(size);
            display.getRealSize(realSize);
            return realSize.x != size.x || realSize.y != size.y;
        } else {
            boolean menu = ViewConfiguration.get(mContext).hasPermanentMenuKey();
            boolean back = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);
            return !(menu || back);
        }
    }

    /**
     * dp转成px
     *
     * @param mContext
     * @param dipValue
     * @return
     */
    public static int dip2px(Context mContext, float dipValue) {
        final float scale = mContext.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    public static float sp2px(Context mContext, float spValue) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, spValue, mContext.getResources().getDisplayMetrics());
    }

    /**
     * px转成dp
     *
     * @param mContext
     * @param pxValue
     * @return
     */
    public static int px2dip(Context mContext, float pxValue) {
        final float scale = mContext.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    public static int getStatusBarHeightByReflect(Context mContext) {
        //int sbHeight;
        if (statusBarHeight > 0) {
            return statusBarHeight;
        }
        try {
            Class<?> c = Class.forName("com.android.internal.R$dimen");
            Object obj = c.newInstance();
            Field field = c.getField("status_bar_height");
            int sbHeightId = Integer.parseInt(field.get(obj).toString());
            statusBarHeight = mContext.getResources().getDimensionPixelSize(sbHeightId);
        } catch (Exception e1) {
            e1.printStackTrace();
            statusBarHeight = 0;
        }
        return statusBarHeight;
    }

    public static int getStatusBarHeight(Context mContext) {
        int statusBarHeight = getStatusBarHeightByReflect(mContext);
        if (statusBarHeight == 0) {
            statusBarHeight = SystemUtils.dip2px(mContext, 30);
        }
        return statusBarHeight;
    }

    public static int getScreenHeight(View view) {
        setScreenWidthOrHeight(view);
        return screenHeight;
    }

    public static int getScreenWidth(View view) {
        setScreenWidthOrHeight(view);
        return screenWidth;
    }

    private static void setScreenWidthOrHeight(View view) {
        View rootView = (View) view.getParent();
        int w, h;
        if (rootView != null) {
            w = rootView.getWidth();
            h = rootView.getHeight();
        } else {
            w = screenWidth;
            h = screenHeight;
        }
        int max = Math.max(w, h);
        int min = Math.min(w, h);
        boolean isLandscape = isLandscape(view);
        screenHeight = isLandscape ? min : max;
        screenWidth = isLandscape ? max : min;
    }

    /**
     * 判断是横屏还是竖屏
     *
     * @return 横屏true/竖屏false
     */
    public static boolean isLandscape(View view) {
        Configuration mConfiguration = (view != null && view.getResources() != null) ? view.getResources().getConfiguration() : null;
        //屏幕方向 == 横屏
        return mConfiguration != null && mConfiguration.orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    /**
     * 获取系统状态栏高度
     *
     * @param context Activity
     * @return ActionBar 高度
     */
    public static int getActionBarHeight(@NonNull Activity context) {
        TypedArray actionBarSizeArray = context.obtainStyledAttributes(new int[]{
                android.R.attr.actionBarSize
        });
        int actionBarHeight = (int) actionBarSizeArray.getDimension(0, 0);
        actionBarSizeArray.recycle();
        return actionBarHeight;
    }

    public static int[] getViewLocationFormXY(View view) {
        int[] location = new int[2];
        //获取在当前窗口内的绝对坐标
        view.getLocationInWindow(location);
        //获取在整个屏幕内的绝对坐标
        //view.getLocationOnScreen(location);
        //location[1] -= getStatusBarHeight(view);
        return location;
    }
}
