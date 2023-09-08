package com.qjf.backup.ui.home;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.qjf.backup.ui.log.DBHelper;
import com.qjf.backup.ui.log.entity.BackupLog;
import com.qjf.backup.util.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTPClient;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MediaFileBackUp {

    // type=IMG、VIDEO、AUDIO
    public void syncMediaToRemote(String type, Context context, Long scanLogId) throws ParseException {
        // 后台运行时无法获取 mConnectivityManager.getActiveNetwork() 对象
        String onlyWifiBackUp = SspUtil.getOnlyWifiBackUp(context);
        if ("Y".equals(onlyWifiBackUp)) {
            boolean wifiConn = isWifiConnected(context);
            if (!wifiConn) { // 返回 非WIFI环境不进行备份
//                throw new RuntimeException("无法连接到WIFI");
                return;
            }
        }

        String endQueryDateTime = LocalDateTime.now(ZoneId.of("+8")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        // 按照日期倒排的数据
        List<FileLocalInfo> datas = MediaFileUtil.getAndConvertByCursor(context, type, SspUtil.getBackUpLastDayByType(context, type), endQueryDateTime);
        upScanLog(context, scanLogId, datas);

        FTPClient ftpClient = null;
        Session session = new SmbUtil().getSmbSessionBy(context);
        if (Objects.isNull(session)) {
            ftpClient = new FtpUtil().getFtpClientBy(context);
            if (Objects.isNull(session)) {
                throw new RuntimeException("无法连接到服务器");
            }
        }
        if (datas.isEmpty()) {
            closeClient(session, ftpClient);
            return;
        }

        if (Objects.nonNull(session)) {
            uploadBySmb(context, type, session, datas, scanLogId, endQueryDateTime);
        }
        closeClient(session, ftpClient);
    }

    public static void uploadBySmb(Context context, String mediaType, Session session, List<FileLocalInfo> datas, Long scanLogId, String scanLongDateTime) {
        Map<String, List<FileLocalInfo>> map = MediaFileUtil.convertToMap(datas);
        String remotePath = SspUtil.getRemotePathByType(context, mediaType);// 第一级目录需要设置为共享目录
        // 下边是两个异常 都是子目录没有挂载在
        // Could not connect to \\192.168.1.199\data/photo    这种情况是  data/photo 没有配置在SMB的共享目录下
        // Share name (data\photo) cannot contain '\' characters.  这种情况也是 data\photo 没有配置在SMB的共享目录下
        // remotePath 即使设置为 "data/photo" ， diskShare 也只能到达 第一级目录
        DiskShare diskShare = SmbUtil.getDiskShare(session, context);// shareName 只能是一个目录名称，且不能以 / 开头
        // STATUS_INVALID_PARAMETER (0xc000000d): Create failed for \\192.168.1.199\data\photo
        boolean hasAnyErrByUpload = false;
        for (String key : map.keySet()) {
            for (FileLocalInfo fileLocalInfo : Objects.requireNonNull(map.get(key))) {
                long start = System.currentTimeMillis();
                int result = 0;
                String errMsg = "";
                try {
                    result = SmbUtil.uploadFile2(fileLocalInfo, diskShare, SmbUtil.getRemoteChildrenDir(SspUtil.getSmbShareName(context), remotePath));
                } catch (Exception e) {
                    // 失败了
                    errMsg = e.getMessage();
                    result = 0;
                    hasAnyErrByUpload = true;
                }
                Log.v("文件上传", fileLocalInfo.getName() + " 上传" + getUploadResultStr(result) + " " + ByteConvert.convertToStr(fileLocalInfo.getSize()) + " 耗时=" + (System.currentTimeMillis() - start));
                if (result != 2 && Objects.nonNull(scanLogId)) {
                    try {
                        DBHelper.getInstance(context).insertBackLog(scanLogId, fileLocalInfo.getName(), fileLocalInfo.getSize(), result, System.currentTimeMillis() - start, errMsg);
                    } catch (Exception e) {
//
                    }
                }
            }

            if (!hasAnyErrByUpload && StringUtils.isNotBlank(scanLongDateTime)) {
                SspUtil.saveBackUpLastDayByType(context, mediaType, scanLongDateTime);// todo 必须时上传的文件没有一个失败的，否则不能保存这个值，随后可以继续扫描该日期的数据然后上传
            }
        }
    }

    // 准备两个容器： 1. 记录上传成功的文件信息：文件名称，文件大小，上传时间，上传耗时；2.记录上传失败的文件：文件名称，文件大小，上传时间，上传耗时，失败原因
    // 使用对象 BackupLog 对象保存以上信息
    public static void uploadBySmb(Context context, String mediaType, Session session, List<FileLocalInfo> datas, List<BackupLog> succ, List<BackupLog> fail, List<FileLocalInfo> currentFile) {
        Map<String, List<FileLocalInfo>> map = MediaFileUtil.convertToMap(datas);
        String remotePath = SspUtil.getRemotePathByType(context, mediaType);// 第一级目录需要设置为共享目录
        DiskShare diskShare = SmbUtil.getDiskShare(session, context);
        for (String key : map.keySet()) {
            for (FileLocalInfo fileLocalInfo : Objects.requireNonNull(map.get(key))) {
                long start = System.currentTimeMillis();
                int result = 0;
                BackupLog backupLog = BackupLog.convertBy(fileLocalInfo);
                try {
                    currentFile.add(0, fileLocalInfo);
                    result = SmbUtil.uploadFile2(fileLocalInfo, diskShare, SmbUtil.getRemoteChildrenDir(SspUtil.getSmbShareName(context), remotePath));
                } catch (Exception e) {
                    // 失败了
                    backupLog.setResult("0");
                    backupLog.setErrMsg(e.getMessage());
                }
                backupLog.setTakeMill((System.currentTimeMillis() - start) + "");
                boolean addResult = backupLog.getResult().equals("0") ? fail.add(backupLog) : succ.add(backupLog);
                Log.v("文件上传", fileLocalInfo.getName() + " 上传" + getUploadResultStr(result) + " " + ByteConvert.convertToStr(fileLocalInfo.getSize()) + " 耗时=" + (System.currentTimeMillis() - start) + ", err=" + backupLog.getErrMsg());
            }
        }
    }

    private void closeClient(Session session, FTPClient ftpClient) {
        if (Objects.nonNull(session)) {
            SmbUtil.smbDisconnect(session);
        }
        if (Objects.nonNull(ftpClient)) {
            FtpUtil.ftpDisconnect(ftpClient);
        }
    }

    private static void upScanLog(Context context, Long scanLogId, List<FileLocalInfo> fileDatas) {
        Long totalSize = fileDatas.stream().mapToLong(FileLocalInfo::getSize).sum();
        DBHelper.getInstance(context).upScanFileInfo(scanLogId, fileDatas.size(), totalSize);
    }

    private static String getUploadResultStr(int result) {
        return (result == 0 ? "失败" : "成功") + (result == 2 ? "(文件已经存在)" : "");
    }

    public boolean isWifiConnected(Context context) {
        if (Objects.nonNull(context)) {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            // 后台运行时 会出现一下问题 etwork network =  null
            // Attempt to invoke virtual method 'boolean android.net.NetworkCapabilities.hasCapability(int)' on a null object reference
            Network network = mConnectivityManager.getActiveNetwork();// 息屏后 获取到 null
            NetworkCapabilities networkCapabilities = mConnectivityManager.getNetworkCapabilities(network);

            boolean hasCapability = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);// 判断网络是否已连接，不代表可以访问互联网
            return hasCapability && networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);// // 是否是WIFI连接
        }
        return false;
    }

}
