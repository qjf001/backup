package com.qjf.backup.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

public class FtpUtil {

    public FTPClient getFtpClientBy(Context context) {
        Map<String, String> settingMap = SspUtil.getAll(context);
        String ftpServer = settingMap.getOrDefault("ftpServer", "");
        String ftpUser = settingMap.getOrDefault("ftpUser", "");
        String ftpPwd = settingMap.getOrDefault("ftpPwd", "");
        return getFtpClient(ftpServer, ftpUser, ftpPwd);
    }

    public static boolean uploadFile(FileLocalInfo localInfo, FTPClient ftpClient) throws IOException {
        File file = new File(localInfo.getPath());
        if (file.exists() && file.isFile() && file.canRead()) {
            BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file));
            // 没有成功？？？？
            // 没有成功是因为用户权限问题，文件名称 remote 需要带扩展名称
            Log.v("文件上传", "准备上传文件," + localInfo.getName());
            boolean result = ftpClient.storeFile(localInfo.getName(), bufferedInputStream);

            bufferedInputStream.close();
            return result;
        }
        return false;
    }

    public static void setFtpClient(FTPClient ftpClient, String remotePath) throws IOException {
        // data 下的子目录 上传不成功， 可能是权限的问题（连接的用户需要目录的写入权限）
        ftpClient.changeWorkingDirectory(remotePath);// 服务端设置的共享目录
        ftpClient.enterLocalPassiveMode();// 没有设置的话，listFiles 为空
        ftpClient.setControlEncoding("UTF-8");
        ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
        // failed to connect to /192.168.1.199 (port 36675) from /192.168.1.25 (port 39088) after 600000ms: isConnected failed: ETIMEDOUT (Connection timed out)
        ftpClient.setConnectTimeout(600000);// 60000ms
    }

    public static FTPClient getFtpClient(String remoteHost, String userName, String pwd) {
        if (StringUtils.isBlank(remoteHost)) {
            return null;
        }
        try {
            HostAndPort hostAndPort = new HostAndPort(remoteHost, 21);
            //1.要连接的FTP服务器Url,Port
            FTPClient ftpClient = new FTPClient();
            ftpClient.connect(hostAndPort.getHost(), hostAndPort.getPort());
//                ftpClient.connect("*.tpddns.cn", 2211);
            //2.登陆FTP服务器
            ftpClient.login(userName, pwd);// 用户给与读写共享文件夹的权限
            //3.看返回的值是不是230，如果是，表示登陆成功
            int reply = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                //断开
                ftpClient.disconnect();
                System.out.println("ftp 连接失败");
                return null;
            } else {
                System.out.println("ftp 连接成功");
                return ftpClient;
            }

            // 使用给定的名称并从给定的InputStream中获取输入，将文件存储在服务器上。此方法不会关闭给定的InputStream
            // ftpClient.storeFile("remote", InputStream.nullInputStream());// 上传文件
        } catch (Exception e) {
            Log.v("FTP", e.getMessage());
            return null;
        }
    }

    public static boolean ftpDisconnect(FTPClient client) {
        try {
            if (Objects.nonNull(client)) {
                client.logout();
                client.disconnect();
                return true;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return false;
    }
}
