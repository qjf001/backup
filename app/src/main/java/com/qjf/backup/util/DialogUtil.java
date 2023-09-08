package com.qjf.backup.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import com.qjf.backup.R;

import java.util.Objects;

public class DialogUtil {


    public static AlertDialog showDialog(Context context, DialogInterface.OnCancelListener listener, String title, String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
//        LayoutInflater inflater = LayoutInflater.from(context);
        //弹窗需要展示什么内容，就在 R.layout.my_dialog 自己添加
//        View root = inflater.inflate(R.layout.my_dialog, null);
//        builder.setView(root);
        builder.setMessage(msg);
        builder.setOnCancelListener(listener);
        AlertDialog dialog = builder.create();
        dialog.show();

        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Rect rect = windowManager.getCurrentWindowMetrics().getBounds();
        int w = Double.valueOf(rect.width() * 0.9).intValue();// 获取屏幕宽度
        int h = Double.valueOf(rect.height() * 0.4).intValue();// 获取屏幕宽度

        dialog.getWindow().setLayout(w, h);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.LTGRAY));
        return dialog;
    }

    public static AlertDialog showDialog(Context context, String title, View contentView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setView(contentView);
        AlertDialog dialog = builder.create();
        dialog.setView(contentView);
        return dialog;
//        dialog.show();
//
//        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
//        Rect rect = windowManager.getCurrentWindowMetrics().getBounds();
//        int w = Double.valueOf(rect.width() * 0.9).intValue();// 获取屏幕宽度
//        int h = Double.valueOf(rect.height() * 0.4).intValue();// 获取屏幕宽度
//
//        dialog.getWindow().setLayout(w, h);
//        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.LTGRAY));
    }

    public static AlertDialog showDialog(Context context, String msg) {
        return showDialog(context, null, "提示", msg);
    }

    public static AlertDialog showDialog(Context context, DialogInterface.OnCancelListener listener, String msg) {
        return showDialog(context, listener, "提示", msg);
    }

    public static void showDialog(AlertDialog mAlertDialog, Context context, DialogInterface.OnCancelListener listener, String msg) {
        if (Objects.nonNull(mAlertDialog)) {
            if (!mAlertDialog.isShowing()) {
                mAlertDialog.show();

                WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                Rect rect = windowManager.getCurrentWindowMetrics().getBounds();
                int w = Double.valueOf(rect.width() * 0.9).intValue();// 获取屏幕宽度
                int h = Double.valueOf(rect.height() * 0.3).intValue();// 获取屏幕宽度

                mAlertDialog.getWindow().setLayout(w, h);

                if (Objects.nonNull(listener)) {
                    mAlertDialog.setOnCancelListener(listener);
                }
            }

            TextView tv = mAlertDialog.findViewById(R.id.dialog_content_tv1);
            tv.setText(msg);
        } else {
            showDialog(context, listener, msg);
        }
    }
}
