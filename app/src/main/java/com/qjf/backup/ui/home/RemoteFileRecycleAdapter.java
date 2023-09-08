package com.qjf.backup.ui.home;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.qjf.backup.R;
import com.qjf.backup.util.DialogUtil;
import com.qjf.backup.util.SmbUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

public class RemoteFileRecycleAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    List<FileObjEntity> datas;
    Context context;

    AlertDialog dialog;

    private HomeFragment.SetChildFolder setChildFolder;
    private HomeFragment.FileRemotePathExcludeShareName remotePathObj;

    public RemoteFileRecycleAdapter(List<FileObjEntity> datas, Context context, HomeFragment.SetChildFolder setChildFolder, HomeFragment.FileRemotePathExcludeShareName remotePathObj) {
        this.datas = datas;
        this.context = context;
        this.setChildFolder = setChildFolder;
        this.remotePathObj = remotePathObj;
        dialog = DialogUtil.showDialog(context, "提示", View.inflate(context, R.layout.dialog_content, null));
    }

    @NonNull
    @NotNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        // 创建绑定关系
        if (viewType == 1) {
            return new HolderDir(View.inflate(parent.getContext(), R.layout.remote_dir_list, null));
        } else if (viewType == 0) {
            return new HolderFile(View.inflate(parent.getContext(), R.layout.remote_file_list, null));
        } else {
            return new HolderFileEmpty(View.inflate(parent.getContext(), R.layout.remote_file_list_nodata, null));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull RecyclerView.ViewHolder holder, int position) {
        FileObjEntity file = datas.get(position);
        int viewType = getItemViewType(position);
        if (viewType == 1) {// dir
            HolderDir holderDir = ((HolderDir) holder);
            long childrenDirCount = file.getChildrenDirCount();// 子目录数量
            long childrenFileCount = file.getChildrenFileCount();// 包含的文件数量

            String dirInfo = childrenDirCount + childrenFileCount == 0 ? "空目录" : "";
            if (childrenDirCount + childrenFileCount > 0) {
                if (childrenDirCount > 0) {
                    dirInfo = childrenDirCount + "个子目录";
                }
                if (childrenFileCount > 0) {
                    dirInfo += (childrenDirCount > 0 ? "，" : "") + childrenFileCount + "个文件";
                }
            }

            holderDir.tv1.setText(file.getFileName());
            holderDir.tv11.setText(dirInfo);
            holderDir.tv2.setText(HtmlCompat.fromHtml("<strong>打开</strong>", HtmlCompat.FROM_HTML_MODE_LEGACY));
            holderDir.itemView.setOnClickListener(new ItemClickListener("打开", file));
        } else if (viewType == 0) {// file
            HolderFile holderFile = ((HolderFile) holder);
            if (StringUtils.isBlank(file.getFileName())) {// 这个应该需要第三种布局，这里偷懒一下
                holderFile.tv2.setText("");
            } else {
                holderFile.tv1.setText(file.getFileName());
                holderFile.itemView.setOnClickListener(new ItemClickListener("下载", file));
            }
        } else {

        }
    }

    public int getItemViewType(int position) {
        FileObjEntity fileObj = datas.get(position);
        if (datas.size() == 1 && StringUtils.isBlank(fileObj.getFileName())) {
            return 2;
        }
        return fileObj.isDir() ? 1 : 0;
    }

    class ItemClickListener implements View.OnClickListener {
        public FileObjEntity file;
        private String eventType;

        public ItemClickListener(String eventType, FileObjEntity file) {
            this.eventType = eventType;
            this.file = file;
        }

        @Override
        public void onClick(View v) {
            if (eventType.equals("下载")) {// 先下载吧？？
                DialogUtil.showDialog(dialog, v.getContext(), null, "正在下载");

                new Thread(() -> {
                    String downloadPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
                    String localFileAndPath = downloadPath + File.separator + file.getFileName();
                    String remoteFileAndPath = remotePathObj.getRemotePath(file.getFileName());

                    Session session = new SmbUtil().getSmbSessionBy(v.getContext());
                    DiskShare share = SmbUtil.getDiskShare(session, v.getContext());
                    int result = SmbUtil.downLoadFile(remoteFileAndPath, localFileAndPath, share);
                    String msg = "下载失败";
                    if (result == 1) {
                        msg = "下载成功，路径:" + localFileAndPath;
                    } else if (result == 2) {
                        msg = "文件已存在";
                    }

                    Message message = dialogHandler.obtainMessage();
                    Bundle bundle = new Bundle();
                    bundle.putString("msg", msg);
                    message.setData(bundle);
                    message.sendToTarget();

                }).start();
            } else {
                setChildFolder.clickSearch(file.getFileName());// 继续打开  fileName=dirName
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private final Handler dialogHandler = new Handler() {
        @SuppressLint("NotifyDataSetChanged")
        @Override
        public void handleMessage(@NonNull Message msg) {
            String msgStr = msg.getData().getString("msg");
            if (StringUtils.isNotBlank(msgStr)) {
                if (dialog.isShowing()) {
                    TextView tv = dialog.findViewById(R.id.dialog_content_tv1);
                    tv.setTextSize(12);
                    tv.setText(msg.getData().getString("msg"));
                } else {
                    DialogUtil.showDialog(context, msg.getData().getString("msg"));
                }
            }
        }
    };

    @Override
    public int getItemCount() {
        return datas.size();
    }

    public void replaceDatas(List<FileObjEntity> remoteFiles) {
        if (datas.size() != 0) {
            int originSize = datas.size();
            datas.clear();
            notifyItemRangeRemoved(0, originSize);
            datas.addAll(remoteFiles);
            notifyItemRangeChanged(0, remoteFiles.size());
        }
    }

    public class HolderDir extends RecyclerView.ViewHolder {
        TextView tv1;
        TextView tv11;
        TextView tv2;

        public HolderDir(@NonNull @NotNull View itemView) {
            super(itemView);
            setItem(itemView);
        }

        private void setItem(View view) {
            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Rect rect = windowManager.getCurrentWindowMetrics().getBounds();
            int w = rect.width() / 8;// 获取屏幕宽度  w=列的平均宽度

            tv1 = view.findViewById(R.id.remote_dir_name);
            tv1.setWidth(7 * w);
            tv1.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);

            tv11 = view.findViewById(R.id.dir_info);
            tv11.setWidth(7 * w);
            tv11.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);

            tv2 = view.findViewById(R.id.remote_dir_open);
            tv2.setWidth(w);
//            tv2.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
        }
    }

    public class HolderFile extends RecyclerView.ViewHolder {
        TextView tv1;
        TextView tv2;

        public HolderFile(@NonNull @NotNull View itemView) {
            super(itemView);
            setItem(itemView);
        }

        private void setItem(View view) {
            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Rect rect = windowManager.getCurrentWindowMetrics().getBounds();
            int w = rect.width() / 8;// 获取屏幕宽度  w=列的平均宽度

            tv1 = view.findViewById(R.id.remote_file_name);
            tv1.setWidth(7 * w);
            tv1.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);

            tv2 = view.findViewById(R.id.remote_file_download);
            tv2.setWidth(w);
        }
    }

    public class HolderFileEmpty extends RecyclerView.ViewHolder {
        TextView tv1;

        public HolderFileEmpty(@NonNull @NotNull View itemView) {
            super(itemView);
            setItem(itemView);
        }

        private void setItem(View view) {
            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Rect rect = windowManager.getCurrentWindowMetrics().getBounds();
            int w = rect.width() / 8;// 获取屏幕宽度  w=列的平均宽度

            tv1 = view.findViewById(R.id.remote_file_empty);
            tv1.setWidth(rect.width());
            tv1.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        }
    }
}
