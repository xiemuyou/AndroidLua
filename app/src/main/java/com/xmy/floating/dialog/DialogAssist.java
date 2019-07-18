package com.xmy.floating.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import com.xmy.floating.R;

/**
 * @author xiemy2
 * @date 2019/7/18
 */
public class DialogAssist {

    private static DialogAssist dialogAssist;

    private DialogAssist() {

    }

    public static DialogAssist getIncetanse() {
        synchronized (DialogAssist.class) {
            if (dialogAssist == null) {
                dialogAssist = new DialogAssist();
            }
            return dialogAssist;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void overlayPermission(final Context context) {
        // 授权提示
        new AlertDialog.Builder(context).setTitle("悬浮窗权限未开启")
                .setMessage("你的手机没有授权" + context.getString(R.string.app_name) + "获得悬浮窗权限，视频悬浮窗功能将无法正常使用")
                .setPositiveButton("开启", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 显示授权界面
                        context.startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
                    }
                })
                .setNegativeButton("取消", null).show();
    }
}
