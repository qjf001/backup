package com.qjf.backup.util;

import android.content.Context;
import android.text.Spanned;
import android.widget.Toast;
import androidx.core.text.HtmlCompat;

public class ToastUtils {

    public static void showCenter(Context context, String s) {
        Spanned s1 = HtmlCompat.fromHtml("<font color='red' size='30px'>" + s + "</font>", HtmlCompat.FROM_HTML_MODE_LEGACY);

        Toast toast = Toast.makeText(context, s1, Toast.LENGTH_SHORT);
//        toast.setGravity(Gravity.TOP|Gravity.LEFT, 0, 100);// 不管用， android 11 以上版本无效， 同样的 setView android 11 以上版本无效
        toast.show();
    }

}