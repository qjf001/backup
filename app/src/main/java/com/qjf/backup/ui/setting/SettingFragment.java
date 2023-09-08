package com.qjf.backup.ui.setting;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.hierynomus.smbj.session.Session;
import com.qjf.backup.R;
import com.qjf.backup.databinding.FragmentSettingBinding;
import com.qjf.backup.ui.home.MediaFileBackUp;
import com.qjf.backup.ui.log.entity.BackupLog;
import com.qjf.backup.util.*;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class SettingFragment extends Fragment {

    private AlertDialog mAlertDialog = null;
    private FragmentSettingBinding binding;
    private static final int DIALOG_CODE = 9000;
    private static final int SCAN_NONE_RESULT_DIALOG_CODE = 6000;// 扫描结果弹窗:没有扫描文件

    Timer dialogTimer;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentSettingBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

//        binding.smbShareName.addTextChangedListener(shareNameTextWatcher);// 设置一个文本输入监听
        binding.smbShareName.setOnFocusChangeListener(shareNameFocusChange);// 设置一个焦点变化的监听
        binding.imgPath.setOnClickListener(pathClickListener);//
        binding.videoPath.setOnClickListener(pathClickListener);
        binding.audioPath.setOnClickListener(pathClickListener);

        binding.imgPath.addTextChangedListener(pathTextChangedListener);
        binding.videoPath.addTextChangedListener(pathTextChangedListener);
        binding.audioPath.addTextChangedListener(pathTextChangedListener);

        // ui表单回填:从本地存储中获取数据，然后填充到页面中
        fillFormByLocalData();

        View view = View.inflate(getContext(), R.layout.dialog_content, null);
        mAlertDialog = DialogUtil.showDialog(getContext(), "", view);

        // bt 绑定监听事件
        setSubmitBtListener();
        setSmbConnectTestBtListener();
//        binding.selectImgDatetimeBt.setOnClickListener(v -> new DateTimePickDialog(getContext(), binding.imgLastBackupDatetime).show());
//        binding.selectVideoDatetimeBt.setOnClickListener(v -> new DateTimePickDialog(getContext(), binding.videoLastBackupDatetime).show());
//        binding.selectAudioDatetimeBt.setOnClickListener(v -> new DateTimePickDialog(getContext(), binding.audioLastBackupDatetime).show());

        binding.manualSync.setOnClickListener(v -> manualSync());

        return root;
    }

    // 手动同步
    @SuppressLint({"CutPasteId", "SimpleDateFormat"})
    private void manualSync() {
        Map<String, String> typeMap = new HashMap<>();
        typeMap.put("图片", "IMG");
        typeMap.put("视频", "VIDEO");
        typeMap.put("音频", "AUDIO");
        View contentView = View.inflate(getContext(), R.layout.manual_sync_dialog_view, null);
        contentView.findViewById(R.id.select_createtime_start_bt).setOnClickListener(v -> {
            // 弹窗选择
            new DateTimePickDialog(getContext(), contentView.findViewById(R.id.file_createtime_start)).show();
        });
        contentView.findViewById(R.id.select_createtime_end_bt).setOnClickListener(v -> {
            // 弹窗选择
            new DateTimePickDialog(getContext(), contentView.findViewById(R.id.file_createtime_end)).show();
        });

        String dateTimeEndStr = LocalDateTime.now(ZoneId.of("+8")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        contentView.findViewById(R.id.today_bt).setOnClickListener(v -> {
            String dateTimeStart = LocalDate.now(ZoneId.of("+8")).atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            ((TextView) contentView.findViewById(R.id.file_createtime_start)).setText(dateTimeStart);
            ((TextView) contentView.findViewById(R.id.file_createtime_end)).setText(dateTimeEndStr);
        });

        contentView.findViewById(R.id.two_days_bt).setOnClickListener(v -> {
            String dateTimeStart = LocalDate.now(ZoneId.of("+8")).plusDays(-1).atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            ((TextView) contentView.findViewById(R.id.file_createtime_start)).setText(dateTimeStart);
            ((TextView) contentView.findViewById(R.id.file_createtime_end)).setText(dateTimeEndStr);
        });

        contentView.findViewById(R.id.one_week_bt).setOnClickListener(v -> {
            String dateTimeStart = LocalDate.now(ZoneId.of("+8")).plusDays(-6).atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            ((TextView) contentView.findViewById(R.id.file_createtime_start)).setText(dateTimeStart);
            ((TextView) contentView.findViewById(R.id.file_createtime_end)).setText(dateTimeEndStr);
        });

        AlertDialog dialog = DialogUtil.showDialog(getContext(), "", contentView);
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "确定", (dialog1, which) -> {
            Spinner spinner = contentView.findViewById(R.id.dialog_select_type);
            String selectedType = typeMap.get(spinner.getSelectedItem().toString());
            String startDateTimeStr = ((TextView) contentView.findViewById(R.id.file_createtime_start)).getText().toString();
            String endDateTimeStr = ((TextView) contentView.findViewById(R.id.file_createtime_end)).getText().toString();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            boolean check;
            try {
                check = sdf.parse(endDateTimeStr).after(sdf.parse(startDateTimeStr));
            } catch (ParseException e) {
                check = false;
            }

            if (!check) {
                DialogUtil.showDialog(getContext(), "请选择正确的时间段");
            } else {
                asyncScanAndUpload(selectedType, startDateTimeStr, endDateTimeStr);
            }
        });
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "取消", (dialog1, which) -> {
            // 关闭弹窗即可
        });

        dialog.show();

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextSize(24);
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextSize(24);

        // 自定义 alert的view_content alertdialog 同步媒体文件类型、创建文件起始时间、结束时间
        // 弹窗提示扫描到多少个文件，正在同步第N个文件。。。。。 同步完成

        contentView.findViewById(R.id.today_bt).callOnClick();
    }

    // 将扫描文件和上传都放在子线程中执行
    private void asyncScanAndUpload(String selectedType, String startDateTimeStr, String endDateTimeStr) {
        AlertDialog scanDialog = DialogUtil.showDialog(getContext(), "正在扫描需要上传的文件，请稍等......");
        new Thread(() -> {
            // 按照两个给定的日期 和 类型扫描 文件，然后上传
            // 按照日期倒排的数据
            try {
                Future<List<FileLocalInfo>> datasFuture = asyncQueryLocalFiles(selectedType, startDateTimeStr, endDateTimeStr);
                List<FileLocalInfo> datas = datasFuture.get();// 这个是同步执行的影响了主线程，可以把这个做成异步执行
                scanDialog.cancel();
                if (datas.size() > 0) {
                    asyncUpload(selectedType, datas);
                } else {
                    Bundle bundle = new Bundle();
                    bundle.putString("msg", "没有扫描到需要上传的文件");
                    Message message = mHandler.obtainMessage(SCAN_NONE_RESULT_DIALOG_CODE);
                    message.setData(bundle);
                    message.sendToTarget();
                }
            } catch (Exception e) {
                Log.v("扫描文件:" + selectedType, e.getMessage());
                DialogUtil.showDialog(getContext(), "扫描文件失败");
            }
        }).start();
    }

    // 这个是同步执行的影响了主线程，可以把这个做成异步执行
    private Future<List<FileLocalInfo>> asyncQueryLocalFiles(String selectedType, String startDateTimeStr, String endDateTimeStr) {
        QueryCallable queryCallable = new QueryCallable(getContext(), selectedType, startDateTimeStr, endDateTimeStr);
        FutureTask<List<FileLocalInfo>> queryFuture = new FutureTask<>(queryCallable);
        new Thread(queryFuture).start();
        return queryFuture;
    }

    class QueryCallable implements Callable<List<FileLocalInfo>> {
        String selectedType, startDateTimeStr, endDateTimeStr;
        Context context;

        QueryCallable(Context context, String selectedType, String startDateTimeStr, String endDateTimeStr) {
            this.context = context;
            this.selectedType = selectedType;
            this.startDateTimeStr = startDateTimeStr;
            this.endDateTimeStr = endDateTimeStr;
        }

        @Override
        public List<FileLocalInfo> call() throws Exception {
            return MediaFileUtil.getAndConvertByCursor(context, selectedType, startDateTimeStr, endDateTimeStr);
        }
    }

    private void asyncUpload(String selectedType, List<FileLocalInfo> datas) {
        long totalByte = totalSumByte(datas);
        int total = datas.size();
        String localFileInfoStr = new StringBuilder("总共").append(total).append("个文件(").append(ByteConvert.convertToStr(totalByte)).append(")").toString();
        List<BackupLog> succ = new ArrayList<>(datas.size());
        List<BackupLog> fail = new ArrayList<>();
        List<FileLocalInfo> currentFiles = new ArrayList<>();
        new Thread(() -> {  // 要在子线程中执行，因为需要异步读取进度信息
            MediaFileBackUp.uploadBySmb(getContext(), selectedType, new SmbUtil().getSmbSessionBy(getContext()), datas, succ, fail, currentFiles);// asyncUpload 方法已属于子线程中的方法了
        }).start();

        // 开一个计时器 和  dialog 每一秒改变 dialog 中显示的内容，知道dialog cancel
        dialogTimer = new Timer();
        dialogTimer.schedule(new TimerTask() {// 不要使用 scheduleAtFixedRate 如果间隔周期很短会出现 数据错乱
            public void run() {
                List<BackupLog> cpSucc = new ArrayList<>(succ);// 将动态数组拷贝一份，然后再去计算，消除线程之间的影响，或者使用线程安全的数组
                List<BackupLog> cpFail = new ArrayList<>(fail);
                getUploadSpeedAndShow(localFileInfoStr, total, totalByte, cpSucc, cpFail, getCurrentFileInfo(currentFiles));
            }
        }, 800, 800);// 1个文件7-12M，2秒上传一个文件
    }

    private static String getCurrentFileInfo(List<FileLocalInfo> currentFiles) {
        FileLocalInfo currentFile = !currentFiles.isEmpty() ? currentFiles.get(0) : null;
        return Objects.nonNull(currentFile) ? "正在上传文件: " + currentFile.getName() + " (" + ByteConvert.convertToStr(currentFile.getSize()) + ")" : "";
    }

    private static long totalSumByte(List<FileLocalInfo> datas) {
        return datas.stream().mapToLong(FileLocalInfo::getSize).sum();
    }

    private static long totalSumByteByStr(List<BackupLog> datas) {
        return datas.stream().mapToLong(data -> Long.parseLong(data.getFileSize())).sum();
    }

    private void getUploadSpeedAndShow(String localFileInfoStr, int size, long totalByte, List<BackupLog> succ, List<BackupLog> fail, String currentFileInfo) {
        String wrap = "\r\n";
        long succByte = totalSumByteByStr(succ), failByte = totalSumByteByStr(fail);
        StringBuilder msgSb = new StringBuilder(size == succ.size() + fail.size() ? "上传完毕" : "正在上传......").append(wrap)
                .append(localFileInfoStr).append(wrap)
                .append("已上传").append(succ.size()).append("个文件(").append(ByteConvert.convertToStr(succByte)).append(")").append(wrap)
                .append("上传失败").append(fail.size()).append("个文件(").append(ByteConvert.convertToStr(failByte)).append(")").append(wrap)
                .append("当前进度").append(ByteConvert.getSpeed(totalByte, succByte + failByte));
        if (StringUtils.isNotBlank(currentFileInfo) && size != succ.size() + fail.size()) {
            msgSb.append(wrap).append(currentFileInfo);
        }
        // 子线程中的弹窗 需要借住 handler
        Bundle bundle = new Bundle();
        bundle.putString("msg", msgSb.toString());
        Message message = mHandler.obtainMessage(DIALOG_CODE);
        message.setData(bundle);
        message.sendToTarget();
    }

    View.OnClickListener pathClickListener = v -> {
        String shareName = Objects.requireNonNull(binding.smbShareName.getText()).toString();
        if (StringUtils.isBlank(shareName)) {
            DialogUtil.showDialog(v.getContext(), dialog -> {
                binding.smbShareName.setFocusable(true);
                binding.smbShareName.setFocusableInTouchMode(true);

                binding.smbShareName.requestFocus();
                binding.smbShareName.requestFocusFromTouch();
            }, "请先输入共享目录名称");
        }
    };

    TextWatcher pathTextChangedListener = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            String shareName = binding.smbShareName.getText().toString();
            if (StringUtils.isNotBlank(shareName)) {
                String sharePath = File.separator + shareName + File.separator;
                String pathStr = s.toString();
                if (!pathStr.startsWith(sharePath)) {
                    s.replace(0, s.length(), sharePath);
                    DialogUtil.showDialog(getContext(), "输入有误请重新输入");
                }
            }
        }
    };

    View.OnFocusChangeListener shareNameFocusChange = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (!hasFocus) {
                String s = binding.smbShareName.getText().toString();
                if (StringUtils.isNotBlank(s.toString())) {
                    String shareDir = File.separator + s + File.separator;
                    if (!Objects.requireNonNull(binding.imgPath.getText()).toString().startsWith(shareDir)) {
                        binding.imgPath.setText(shareDir);
                    }
                    if (!Objects.requireNonNull(binding.videoPath.getText()).toString().startsWith(shareDir)) {
                        binding.videoPath.setText(shareDir);
                    }
                    if (!Objects.requireNonNull(binding.audioPath.getText()).toString().startsWith(shareDir)) {
                        binding.audioPath.setText(shareDir);
                    }
                }
            }
        }
    };

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @SuppressLint({"NotifyDataSetChanged", "HandlerLeak"})
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case DIALOG_CODE:
                    DialogInterface.OnCancelListener listener = Objects.isNull(dialogTimer) ? null : dialog -> dialogTimer.cancel();
                    DialogUtil.showDialog(mAlertDialog, getContext(), listener, msg.getData().getString("msg"));
                    break;
                case SCAN_NONE_RESULT_DIALOG_CODE:
                    DialogUtil.showDialog(mAlertDialog, getContext(), null, msg.getData().getString("msg"));
                    break;
            }
        }
    };

    private void fillFormByLocalData() {
        Map<String, String> settingMap = SspUtil.getAll(getContext());

        binding.smbServer.setText(settingMap.getOrDefault("smbServer", ""));
        binding.smbShareName.setText(settingMap.getOrDefault("smbShareName", ""));
        binding.smbUser.setText(settingMap.getOrDefault("smbUser", ""));
        binding.smbPwd.setText(settingMap.getOrDefault("smbPwd", ""));

        binding.imgPath.setText(settingMap.getOrDefault("imgPath", ""));
        binding.videoPath.setText(settingMap.getOrDefault("videoPath", ""));
        binding.audioPath.setText(settingMap.getOrDefault("audioPath", ""));
        binding.otherPath.setText(settingMap.getOrDefault("otherPath", ""));

        binding.autoBackUp.setChecked("autoBackUp".equals(settingMap.getOrDefault("backUpStrategy", "")));// "autoBackUp" / "handBackUp"
        binding.handBackUp.setChecked("handBackUp".equals(settingMap.getOrDefault("backUpStrategy", "")));// "autoBackUp" / "handBackUp"

//        binding.imgLastBackupDatetime.setText(settingMap.getOrDefault("imgLastBackupDatetime", ""));
//        binding.videoLastBackupDatetime.setText(settingMap.getOrDefault("videoLastBackupDatetime", ""));
//        binding.audioLastBackupDatetime.setText(settingMap.getOrDefault("audioLastBackupDatetime", ""));

        binding.onlyWifiBackUp.setChecked("Y".equals(settingMap.getOrDefault("onlyWifiBackUp", "")));//
        binding.allNetworkBackup.setChecked("N".equals(settingMap.getOrDefault("onlyWifiBackUp", "")));//
    }

    private void setSubmitBtListener() {
        // button 添加监听事件
        binding.settingSubmitButton.setOnClickListener(v -> {

            String smbServer = Objects.requireNonNull(binding.smbServer.getText()).toString();
            String smbShareName = Objects.requireNonNull(binding.smbShareName.getText()).toString();
            String smbUser = Objects.requireNonNull(binding.smbUser.getText()).toString();
            String smbPwd = Objects.requireNonNull(binding.smbPwd.getText()).toString();

            String imgPath = Objects.requireNonNull(binding.imgPath.getText()).toString();
            String videoPath = Objects.requireNonNull(binding.videoPath.getText()).toString();
            String audioPath = Objects.requireNonNull(binding.audioPath.getText()).toString();
            String otherPath = Objects.requireNonNull(binding.otherPath.getText()).toString();

            RadioButton view = binding.backUpStrategyGroup.findViewById(binding.backUpStrategyGroup.getCheckedRadioButtonId());
            String text = view.getText().toString();
            String backUpStrategy1 = "自动备份".equals(text) ? "autoBackUp" : "handBackUp";

            RadioButton backupNetworkView = binding.networkBackupGroup.findViewById(binding.networkBackupGroup.getCheckedRadioButtonId());
            String wifibackupText = backupNetworkView.getText().toString();
            String backupNetworkStrategy = "只在WIFI环境下备份".equals(wifibackupText) ? "Y" : "N";


//            String imgLastBackupDatetime = Objects.requireNonNull(binding.imgLastBackupDatetime.getText()).toString();// 格式 yyyy-MM-dd HH:mm:ss
//            String videoLastBackupDatetime = Objects.requireNonNull(binding.videoLastBackupDatetime.getText()).toString();// 格式 yyyy-MM-dd HH:mm:ss
//            String audioLastBackupDatetime = Objects.requireNonNull(binding.audioLastBackupDatetime.getText()).toString();// 格式 yyyy-MM-dd HH:mm:ss

            // 把这些数据保存到本地
            SharedPreferences.Editor editor = SspUtil.getShare(getContext()).edit();

            editor.putString("smbServer", smbServer);
            editor.putString("smbShareName", smbShareName);
            editor.putString("smbUser", smbUser);
            editor.putString("smbPwd", smbPwd);

            editor.putString("imgPath", imgPath);
            editor.putString("videoPath", videoPath);
            editor.putString("audioPath", audioPath);
            editor.putString("otherPath", otherPath);

            editor.putString("backUpStrategy", backUpStrategy1);

//            editor.putString("imgLastBackupDatetime", imgLastBackupDatetime);
//            editor.putString("videoLastBackupDatetime", videoLastBackupDatetime);
//            editor.putString("audioLastBackupDatetime", audioLastBackupDatetime);

            editor.putString("onlyWifiBackUp", backupNetworkStrategy);

            // 如果不关心返回值，可以由apply 替换 commit, apply 会立即提交到内存然后异步更新到磁盘
            editor.apply();// editor.commit();
            DialogUtil.showDialog(getContext(), "保存配置成功");
        });
    }

    private void setSmbConnectTestBtListener() {
        binding.smbTestConnect.setOnClickListener(v -> {

            DialogUtil.showDialog(mAlertDialog, getContext(), dialog -> binding.smbTestConnect.setEnabled(true), "连接中......");

            binding.smbTestConnect.setEnabled(false);
            String smbServer = Objects.requireNonNull(binding.smbServer.getText()).toString();
            String smbUser = Objects.requireNonNull(binding.smbUser.getText()).toString();
            String smbPwd = Objects.requireNonNull(binding.smbPwd.getText()).toString();
            String smbShareName = Objects.requireNonNull(binding.smbShareName.getText()).toString();

            Future<String[]> connTestFuture = asyncTestConn(smbServer, smbUser, smbPwd, smbShareName);
            try {
                String[] connTestResultAll = connTestFuture.get();
                try {
                    Thread.sleep(800);
                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
                }

                if (mAlertDialog.isShowing()) {
                    TextView tv = mAlertDialog.findViewById(R.id.dialog_content_tv1);
                    tv.setText(connTestResultAll[0]);
                }
                if (connTestResultAll[0].equals("SMB连接成功")) {
                    try {
                        Thread.sleep(1000);
                        if (mAlertDialog.isShowing()) {
                            TextView tv = mAlertDialog.findViewById(R.id.dialog_content_tv1);
                            tv.setText(connTestResultAll[1]);
                        }
                    } catch (InterruptedException e) {
//                        throw new RuntimeException(e);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public Future<String[]> asyncTestConn(String smbServer, String smbUser, String smbPwd, String smbShareName) {
        FutureTask<String[]> futureTask = new FutureTask<>(() -> {
            String allSucc = "ALL_SUCC";
            Session session = SmbUtil.getSession(smbServer, smbUser, smbPwd);
            String toastMsg = Objects.nonNull(session) ? "SMB连接成功" : "SMB连接失败";
            if (Objects.isNull(session)) {
                allSucc = "NONE_SUCC";
            }
            String shareMsg = "SMB共享目录连接成功";
            if (StringUtils.isNotBlank(smbShareName)) {
                try {
                    session.connectShare(smbShareName);
                } catch (Exception e) {
                    shareMsg = "SMB共享目录连接失败";
                    allSucc = "SOME_SUCC";
                }
            }
            SmbUtil.smbDisconnect(session);
            return new String[]{toastMsg, shareMsg, allSucc};
        });
        new Thread(futureTask).start();
        return futureTask;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}