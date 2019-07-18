package com.xmy.floating.activity;

import android.content.Intent;
import android.view.View;
import com.xmy.floating.BaseActivity;
import com.xmy.floatlibrary.FloatWindowManager;
import com.xmy.floating.R;

/**
 * description:
 * @author 杜小菜 Created on 2019-05-09 - 10:33.
 * E-mail:duqian2010@gmail.com
 */
public class SubAActivity extends BaseActivity {

    @Override
    protected void initData() {
        floatWindowType = FloatWindowManager.FW_TYPE_ROOT_VIEW;
    }

    @Override
    protected void initView() {
        findViewById(R.id.btn_open_wm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(mContext, SubBActivity.class));
            }
        });
        findViewById(R.id.btn_open_no_float_win).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(mContext, SubCActivity.class));
            }
        });
    }

    @Override
    public int getLayoutResId() {
        return R.layout.activity_sub;
    }

}
