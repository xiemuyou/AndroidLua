package com.xmy.floatlibrary.widget;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.TranslateAnimation;

/**
 * @author xiemy2
 * @date 2019/6/11
 */
public class FloatAnimator {

    private static FloatAnimator floatAnimator;
    private final long ANIM_SHOW_TIME = 500;

    private FloatAnimator() {
    }

    public static FloatAnimator getInstance() {
        synchronized (FloatAnimator.class) {
            if (floatAnimator == null) {
                floatAnimator = new FloatAnimator();
            }
        }
        return floatAnimator;
    }

    /**
     * 平移动画效果
     *
     * @param view       动画控件
     * @param fromXValue 平移开始坐标(%)
     * @param toXValue   平移结束坐标(%)
     */
    void startAnimation(View view, float fromXValue, float toXValue) {
        startAnimation(view, fromXValue, toXValue, ANIM_SHOW_TIME);
    }

    /**
     * 平移动画效果
     *
     * @param view       动画控件
     * @param fromXValue 平移开始坐标(%)
     * @param toXValue   平移结束坐标(%)
     * @param showTime   动画显示时长毫秒(默认500)
     */
    void startAnimation(View view, float fromXValue, float toXValue, long showTime) {
        TranslateAnimation translateAnimation = new TranslateAnimation(
                TranslateAnimation.RELATIVE_TO_PARENT, fromXValue,
                TranslateAnimation.RELATIVE_TO_PARENT, toXValue,
                TranslateAnimation.RELATIVE_TO_PARENT, 0.0F,
                TranslateAnimation.RELATIVE_TO_PARENT, 0.0F);
        translateAnimation.setDuration(showTime);
        view.startAnimation(translateAnimation);
    }

    /**
     * 红点淡入动画
     *
     * @param view 红点view控件
     */
    void showStartAnimator(View view) {
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, "alpha", 0.0F, 1.0F);
        AnimatorSet animSet = new AnimatorSet();
        animSet.play(alpha);
        animSet.setDuration(ANIM_SHOW_TIME);
        animSet.start();
    }
}
