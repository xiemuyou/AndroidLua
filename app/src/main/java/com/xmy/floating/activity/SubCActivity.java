package com.xmy.floating.activity;

import android.view.View;
import android.widget.TextView;
import com.xmy.floating.BaseActivity;
import com.xmy.floatlibrary.FloatWindowManager;
import com.xmy.floating.R;

/**
 * description:
 *
 * @author 杜小菜 Created on 2019-05-09 - 10:33.
 * E-mail:duqian2010@gmail.com
 */
public class SubCActivity extends BaseActivity {

    @Override
    protected void initData() {
        floatWindowType = FloatWindowManager.FW_TYPE_ROOT_VIEW;
    }

    @Override
    protected void initView() {
        ((TextView) findViewById(R.id.message)).setText(R.string.tips_no_float_window);
        findViewById(R.id.btn_open_no_float_win).setVisibility(View.GONE);
        findViewById(R.id.btn_open_wm).setVisibility(View.GONE);
    }

    @Override
    public int getLayoutResId() {
        return R.layout.activity_sub;
    }


    @Override
    protected boolean isShowFloatWindow() {
        return false;
    }
}
