package com.qjf.backup.util;

import android.content.Context;
import android.util.Log;
import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2CreateOptions;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.utils.SmbFiles;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.*;

public class SmbUtil {

    public Session getSmbSessionBy(Context context) {
        Map<String, String> settingMap = SspUtil.getAll(context);
        String smbServer = settingMap.getOrDefault("smbServer", "");
        String smbUser = settingMap.getOrDefault("smbUser", "");
        String smbPwd = settingMap.getOrDefault("smbPwd", "");
        return getSession(smbServer, smbUser, smbPwd);
    }

    /**
     * 不知道是什么原因，手机上传依然比 电脑上传慢很多，电脑快4-5倍的样子
     * remoteDir:  xxx/xxx 或者 / 或者 xxx/ 格式的
     * result 1=成功，0=失败， 2 已经存在的文件
     */
    public static int uploadFile(FileLocalInfo localInfo, DiskShare share, String remoteDir) throws IOException {
        List<FileIdBothDirectoryInformation> remoteFiles = share.list(remoteDir, localInfo.getName());
        if (!remoteFiles.isEmpty()) {
            if (remoteFiles.get(0).getEndOfFile() == localInfo.getSize()) {
                Log.v("文件上传", localInfo.getName() + " 已经存在了");
                return 2;
            }
        }

        File f = new File(localInfo.getPath());
        // 如果文件存在则覆盖
        com.hierynomus.smbj.share.File openFile = share.openFile(remoteDir + localInfo.getName(), getAccessMask(), getFileAttributes(), getSmb2ShareAccess(), SMB2CreateDisposition.FILE_OVERWRITE_IF, getCreateOptions());
        OutputStream oStream = openFile.getOutputStream();
        BufferedOutputStream bfOutStream = new BufferedOutputStream(oStream);
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(f));

        int len = 0; //Read length
//        byte[] buffer = new byte[1024];// 一次0.1M
        byte[] buffer = new byte[1024 * 1024];// 一次1M
        while ((len = in.read(buffer, 0, buffer.length)) != -1) {
            bfOutStream.write(buffer, 0, len);
        }
        bfOutStream.flush();
        bfOutStream.close();
        in.close();
        return 1;
    }

    public static int uploadFile2(FileLocalInfo localInfo, DiskShare share, String remoteDir) throws IOException {
        List<FileIdBothDirectoryInformation> remoteFiles = share.list(remoteDir, localInfo.getName());
        if (!remoteFiles.isEmpty()) {
            if (remoteFiles.get(0).getEndOfFile() == localInfo.getSize()) {
//                Log.v("文件上传", localInfo.getName() + " 已经存在了");
                return 2;
            }
        }

        File source = new File(localInfo.getPath());
        long bytesWrite = SmbFiles.copy(source, share, remoteDir + File.separator + localInfo.getName(), true);// 如果已经上传的文件 只上传了一部分，这里允许覆盖原文件，重新上传
        return bytesWrite > 1 ? 1 : 0;
    }

    // remotePathFile 不包含共享目录， 包含路的文件名称
    public static int downLoadFile(String remotePathFile, String localFileAndPath, DiskShare share) {
        File localF = new File(localFileAndPath);
        if (localF.exists()) {//存在则需要比较文件大小,如果简单比较则比较文件是否存在即可
            return 2;
        }

        com.hierynomus.smbj.share.File smbFileRead = share.openFile(remotePathFile, EnumSet.of(AccessMask.GENERIC_READ), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, null);
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(localF)); InputStream ins = smbFileRead.getInputStream()) {
            byte[] buffer = new byte[1024 * 1024];
            int len;
            while ((len = ins.read(buffer, 0, buffer.length)) != -1) {
                bos.write(buffer, 0, len);
            }
            bos.flush();
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    public static DiskShare getDiskShare(Session session, Context context) {
        return (DiskShare) session.connectShare(SspUtil.getSmbShareName(context));
    }

    private static Set<AccessMask> getAccessMask() {
        Set<AccessMask> accessMasks = new HashSet<>();
        accessMasks.add(AccessMask.FILE_ADD_FILE);
        return accessMasks;
    }

    private static Set<SMB2ShareAccess> getSmb2ShareAccess() {
        // 共享读、共享写
        Set<SMB2ShareAccess> smb2ShareAccesses = new HashSet<>();
        smb2ShareAccesses.add(SMB2ShareAccess.FILE_SHARE_WRITE);
        smb2ShareAccesses.add(SMB2ShareAccess.FILE_SHARE_READ);
        return smb2ShareAccesses;
    }

    private static Set<FileAttributes> getFileAttributes() {
        Set<FileAttributes> fileAttributes = new HashSet<>();
        fileAttributes.add(FileAttributes.FILE_ATTRIBUTE_NORMAL);
        fileAttributes.add(FileAttributes.FILE_ATTRIBUTE_ARCHIVE);
        return fileAttributes;
    }

    private static Set<SMB2CreateOptions> getCreateOptions() {
        Set<SMB2CreateOptions> createOptions = new HashSet<>();
        createOptions.add(SMB2CreateOptions.FILE_NO_COMPRESSION);
        createOptions.add(SMB2CreateOptions.FILE_RANDOM_ACCESS);// 随机读写
        return createOptions;
    }

    public static Session getSession(String remoteHost, String userName, String pwd) {
        if (StringUtils.isBlank(remoteHost)) {
            return null;
        }

        Connection connection = null;
        try {
            HostAndPort hostAndPort = new HostAndPort(remoteHost, 445);
            connection = new SMBClient().connect(hostAndPort.getHost(), hostAndPort.getPort());
        } catch (IOException e) {
            Log.v("SMB", e.getMessage());
            return null;
        }

        AuthenticationContext ac = new AuthenticationContext(userName, pwd.toCharArray(), "");
        return connection.authenticate(ac);
    }

    public static boolean smbDisconnect(Session session) {
        try {
            if (Objects.nonNull(session)) {
                session.close();
                session.getConnection().close();
                return true;
            }
        } catch (IOException e) {
            Log.v("SMB", e.getMessage());
        }
        return false;
    }

    public static String getRemoteChildrenDir(String shareName, String remotePathSetting) {
        String sharePath = File.separator + shareName + File.separator;
        return remotePathSetting.startsWith(sharePath) ? remotePathSetting.replace(sharePath, "") : remotePathSetting;
    }

}


