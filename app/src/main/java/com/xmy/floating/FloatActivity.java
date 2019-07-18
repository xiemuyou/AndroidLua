package com.xmy.floating;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.xmy.floating.activity.SubAActivity;
import com.xmy.floatlibrary.FloatWindowManager;

/**
 * description:Demo展示用法。todo 目前发现显示actionBar的情况下缩放有bug
 *
 * @author 杜小菜 Created on 2019-05-09 - 10:45.
 * E-mail:duqian2010@gmail.com
 */
public class FloatActivity extends BaseActivity {

    @Override
    public int getLayoutResId() {
        return R.layout.activity_main;
    }

    @Override
    protected void initData() {
        floatWindowType = FloatWindowManager.FW_TYPE_ROOT_VIEW;
    }

    @Override
    protected void initView() {
        findViewById(R.id.btn_open_wm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(mContext, SubAActivity.class));
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        //得到被点击的item的itemId
        switch (item.getItemId()) {
            case R.id.navigation_A:
                floatWindowType = FloatWindowManager.FW_TYPE_ROOT_VIEW;
                showFloatWindowDelay();
                break;

            case R.id.navigation_B:
                floatWindowType = FloatWindowManager.FW_TYPE_APP_DIALOG;
                showFloatWindowDelay();
                break;

            case R.id.navigation_C:
                floatWindowType = FloatWindowManager.FW_TYPE_ALERT_WINDOW;
                checkPermissionAndShow();
                break;

            default:
                break;
        }
        return true;
    }
}