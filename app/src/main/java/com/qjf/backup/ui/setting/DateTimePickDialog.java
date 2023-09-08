package com.qjf.backup.ui.setting;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;
import com.qjf.backup.R;
import org.apache.commons.lang3.StringUtils;

import java.util.Calendar;

/**
 * 可以设计成通用组件使用
 **/
public class DateTimePickDialog {
    Context context;

    private View dateTimeDialogView;// 包含日期选择控件和时间选择控件的一个view,作为dialog的contentView存在

    private TextView dateTimeTextViewParentPage;// 父页面 时间日期回显区域

//    private DatePicker dp;
//    private TimePicker tp;

    private Calendar calendar;// 用来初始化日期选择器和时间选择器

    private String selectDateStr;// 保存日期选择控件 选择后的日期，需要初始化一个默认值，以便该控件没有使用时也能安全返回数据

    private String selectTimeStr;// 保存时间选择控件 选择后的时间，需要初始化一个默认值，以便该控件没有使用时也能安全返回数据

    private AlertDialog alertDialog;

    public DateTimePickDialog(Context context, TextView showDateTimeView) {
        this.context = context;
        dateTimeTextViewParentPage = showDateTimeView;// 控件上选择日期时间后需要回显到表单中

        initCalendar(showDateTimeView);// 使用表单中的日期时间来初始化 成员变量 private Calendar calendar

        initDialogCalendar();// 使用 initCalendar 后的 Calendar 实例 来初始化日期和时间控件

        initDialog();// 初始化一个 alertDialog 弹窗对象
    }

    public void show() {
        alertDialog.show();
    }

    private void initDialogCalendar() {
        int year = calendar.get(Calendar.YEAR);
        int monthOfYear = calendar.get(Calendar.MONTH);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        dateTimeDialogView = LayoutInflater.from(context).inflate(R.layout.datetime_dialog, null);
        DatePicker dp = dateTimeDialogView.findViewById(R.id.datepicker);
        TimePicker tp = dateTimeDialogView.findViewById(R.id.timepicker);

        dp.setMaxDate(System.currentTimeMillis());
        dp.init(year, monthOfYear, dayOfMonth, onDateChangedListener);//int year, int monthOfYear, int dayOfMonth, DatePicker.OnDateChangedListener onDateChangedListener

        tp.setIs24HourView(true);
        tp.setHour(hourOfDay);
        tp.setMinute(minute);
        tp.setOnTimeChangedListener(onTimeChangedListener);
    }

    // 使用原日期时间 初始化 calendar, 并一同初始化 日期选择器选择后的默认变量值
    private void initCalendar(TextView showDateTimeView) {
        calendar = Calendar.getInstance();
        String originDateTimeStr = showDateTimeView.getText().toString();
        if (StringUtils.isNotBlank(originDateTimeStr)) {
            try {
                String[] datatimeArray = originDateTimeStr.split(" ");
                String[] lastDates = datatimeArray[0].split("-");// 3个元素
                String[] lastTimes = datatimeArray[1].split(":");// 3个元素

                calendar.set(Integer.parseInt(lastDates[0]), Integer.parseInt(lastDates[1]) - 1, Integer.parseInt(lastDates[2]), Integer.parseInt(lastTimes[0]), Integer.parseInt(lastTimes[1]), Integer.parseInt(lastTimes[2]));
                selectDateStr = datatimeArray[0];
                selectTimeStr = datatimeArray[1];
                return;
            } catch (Exception e) {
            }
        }
        selectDateStr = formatDateStr(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        selectTimeStr = formatTimeStr(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
    }

    @SuppressLint("SetTextI18n")
    private void initDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(context);
        b.setTitle("");
        b.setView(dateTimeDialogView);
        //点击确定，回调数据
        b.setPositiveButton("确定", (dialog, which) -> dateTimeTextViewParentPage.setText(selectDateStr + " " + selectTimeStr));
        //取消后恢复原来选择的时间
        b.setNegativeButton("取消", (dialog, which) -> {
        });
        alertDialog = b.create();
    }

    DatePicker.OnDateChangedListener onDateChangedListener = new DatePicker.OnDateChangedListener() {
        @Override
        public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            selectDateStr = formatDateStr(year, monthOfYear, dayOfMonth);
        }
    };

    TimePicker.OnTimeChangedListener onTimeChangedListener = new TimePicker.OnTimeChangedListener() {
        @Override
        public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
            selectTimeStr = formatTimeStr(hourOfDay, minute);
        }
    };

    private static String formatDateStr(int year, int monthOfYear, int dayOfMonth) {
        int month = monthOfYear + 1;
        return year + "-" + (month < 10 ? "0" + month : month) + "-" + (dayOfMonth < 10 ? "0" + dayOfMonth : dayOfMonth);
    }

    private static String formatTimeStr(int hourOfDay, int minute) {
        String hourStr = hourOfDay < 10 ? "0" + hourOfDay : String.valueOf(hourOfDay);
        String minuteStr = minute < 10 ? "0" + minute : String.valueOf(minute);
        return hourStr + ":" + minuteStr + ":00";
    }

}
