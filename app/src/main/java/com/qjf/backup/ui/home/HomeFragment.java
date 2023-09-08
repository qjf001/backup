package com.qjf.backup.ui.home;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.qjf.backup.R;
import com.qjf.backup.databinding.FragmentHomeBinding;
import com.qjf.backup.util.DialogUtil;
import com.qjf.backup.util.SmbUtil;
import com.qjf.backup.util.SspUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class HomeFragment extends Fragment {

    static List<String> clickAllPath = new ArrayList<>();// 点击的所有目录，包含设置的共享目录(第一个元素)

    List<FileObjEntity> remoteFiles;

    private static final int DIALOG_CODE = 1000;
    private static final int MSG_CODE = 1001;
    private static final int MSG_UPDATE_NAV = 1002;// 更新页面的导航

    private FragmentHomeBinding binding;

    RemoteFileRecycleAdapter remoteFileRecycleAdapter;

    private AlertDialog mAlertDialog = null;
    Bundle bundle = new Bundle();

    /**
     * 要实现的功能：1、选择要加载的文件类型，点击加载；2、然后按照日期倒序加载数据；3、滚动加载数据；
     * 将需要加载的数据按照创建日期加载到内存中()
     */
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());// 可以直接使用 linearLayoutManager
        binding.remoteFileRecycle.setLayoutManager(layoutManager);
        remoteFileRecycleAdapter = new RemoteFileRecycleAdapter(noData(), getContext(), new SetChildFolder(), new FileRemotePathExcludeShareName());
        binding.remoteFileRecycle.setAdapter(remoteFileRecycleAdapter);
        binding.remoteFileRecycle.addItemDecoration(new DividerItemDecoration(getContext(), LinearLayoutManager.VERTICAL));

        View view = View.inflate(getContext(), R.layout.dialog_content, null);
        mAlertDialog = DialogUtil.showDialog(getContext(), "提示", view);

        binding.navRootDir.setOnClickListener(v -> addShareDirNav());

        binding.navRootDir.callOnClick();// 自动点击根目录

        return root;
    }

    static class FileRemotePathExcludeShareName {
        public String getRemotePath(String fileName) {
            return clickAllPath.size() > 1 ? String.join(File.separator, clickAllPath.subList(1, clickAllPath.size())) + File.separator + fileName : fileName;
        }
    }

    // 点击根目录事件
    private void addShareDirNav() {
        String shareName = SspUtil.getSmbShareName(getContext());

        new Thread(() -> {
            List<FileObjEntity> datas = new ArrayList<>();
            FileObjEntity rootNav = new FileObjEntity(shareName, 0, true);

            // 校验共享目录是否存在，存在则将共享目录添加在列表中，否侧弹窗提示
            Session session = null;
            try {
                session = new SmbUtil().getSmbSessionBy(getContext());
            } catch (Exception e) {
//                throw new RuntimeException(e);
                // 无法建立连接
            }
            if (Objects.isNull(session)) {
                dialogByHandler("网络连接失败，请稍后重试");
                return;
            }
            try {
                DiskShare share = (DiskShare) session.connectShare(shareName);
                List<FileIdBothDirectoryInformation> remoteChildren = share.list("");// 包含目录
                List<FileObjEntity> children = filterAndConvert(remoteChildren);

                long childrenDirCount = children.stream().filter(FileObjEntity::isDir).count();// 子目录数量
                long childrenFileCount = children.size() - childrenDirCount;// 包含的文件数量

                rootNav.setChildrenDirCount(childrenDirCount);
                rootNav.setChildrenFileCount(childrenFileCount);
                datas.add(rootNav);
                SmbUtil.smbDisconnect(session);
            } catch (Exception e) {
//                throw new RuntimeException(e);
                // 找不到共享目录
                dialogByHandler("无法连接到共享目录:" + shareName);
                SmbUtil.smbDisconnect(session);
                return;
            }

            remoteFiles = datas;
            mHandler.obtainMessage(MSG_CODE).sendToTarget();
            clickAllPath.clear();
            int count = binding.dirNavLayout.getChildCount();
            if (count > 2) {
                Bundle bundleNav = new Bundle();
                bundleNav.putInt("start", 2);
                bundleNav.putInt("count", count - 2);
                Message message = mHandler.obtainMessage(MSG_UPDATE_NAV);
                message.setData(bundleNav);
                message.sendToTarget();
//                binding.dirNavLayout.removeViews(2, count - 2);// 子线程中不能更新ui
            }
        }).start();
    }

    private static List<FileObjEntity> filterAndConvert(List<FileIdBothDirectoryInformation> remoteChildren) {
        return remoteChildren.stream()
                .filter(file -> !(file.getFileName().equals(".") || file.getFileName().equals("..")))
                .map(FileObjEntity::convert).sorted(FileObjEntity::compareBy).collect(Collectors.toList());
    }

    private List<FileObjEntity> noData() {
        List<FileObjEntity> datas = new ArrayList<>();
        datas.add(new FileObjEntity("", 0, false));
        return datas;
    }

    // 点击子目录(childF)：1.查询当前点击的子目录(childF)下的 目录和文件；2.将点击的目录添加到导航中
    class SetChildFolder {
        public void clickSearch(String childF) {
            clickAllPath.add(childF);
            addNavItem();
            searchListByCurrentDir();
        }
    }

    private void searchListByCurrentDir() {
        // 子线程执行远程目录查找 子线程不能更新ui
        new Thread(() -> {
            Session session = new SmbUtil().getSmbSessionBy(getContext());
            if (Objects.isNull(session)) {
                dialogByHandler("网络连接失败，请稍后重试");
                return;
            }
            DiskShare share = null;//
            try {
                share = SmbUtil.getDiskShare(session, getContext());
            } catch (Exception e) {

            }
            if (Objects.isNull(share)) {
                dialogByHandler("无法连接到共享目录:" + SspUtil.getSmbShareName(getContext()));
                return;
            }

            try {
                String remotePath = clickAllPath.size() > 1 ? String.join(File.separator, clickAllPath.subList(1, clickAllPath.size())) : "";
                List<FileIdBothDirectoryInformation> allFile = share.list(remotePath);// 包含目录
                remoteFiles = filterAndConvert(allFile);

                for (FileObjEntity remoteFile : remoteFiles) {// todo 这种方式有没有更好的替代呢？效率太低下了
                    if (remoteFile.isDir()) {
                        List<FileIdBothDirectoryInformation> remoteChildren = share.list(remotePath + File.separator + remoteFile.getFileName());// 包含目录
                        List<FileObjEntity> children = filterAndConvert(remoteChildren);

                        long childrenDirCount = children.stream().filter(FileObjEntity::isDir).count();// 子目录数量
                        long childrenFileCount = children.size() - childrenDirCount;// 包含的文件数量

                        remoteFile.setChildrenDirCount(childrenDirCount);
                        remoteFile.setChildrenFileCount(childrenFileCount);
                    }
                }

                SmbUtil.smbDisconnect(session);// 关闭
                remoteFiles = remoteFiles.size() == 0 ? noData() : remoteFiles;
                mHandler.obtainMessage(MSG_CODE).sendToTarget();
            } catch (Exception e) {// 网络切换时 可能会有假连接问题
                Log.v("smb_err", e.getMessage());
                dialogByHandler("网络异常");// 清理列表  重置导航
                SmbUtil.smbDisconnect(session);// 关闭
            }

        }).start();
    }

    private void dialogByHandler(String msg) {
        bundle.putString("msg", msg);
        Message message = mHandler.obtainMessage(DIALOG_CODE);
        message.setData(bundle);
        message.sendToTarget();
    }

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @SuppressLint("NotifyDataSetChanged")
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_CODE -> remoteFileRecycleAdapter.replaceDatas(remoteFiles);
                case DIALOG_CODE -> handlerCaseDialog(msg);
                case MSG_UPDATE_NAV -> handlerUpdateNav(msg);
            }
        }
    };

    private void handlerUpdateNav(Message msg) {
        Bundle bundle = msg.getData();
        int start = bundle.getInt("start");
        int count = bundle.getInt("count");
        binding.dirNavLayout.removeViews(start, count);
    }

    private void addNavItem() {
        if (clickAllPath.isEmpty()) {
            return;
        }
        // clickAllPath 中获取节点名称
        String itemName = clickAllPath.get(clickAllPath.size() - 1);
        TextView tname = new TextView(getContext());
        tname.setText(itemName);
        tname.setTextSize(16);
        tname.setLayoutParams(new ViewGroup.LayoutParams(WRAP_CONTENT, MATCH_PARENT));
        tname.setOnClickListener(v -> {
            LinearLayout layout = (LinearLayout) v.getParent();
            int index = layout.indexOfChild(v);
            if (index + 1 < layout.getChildCount() - 1) {// 判断是否有下一位元素
                // 把 其后的 所有子元素 删除完
                layout.removeViews(index + 2, layout.getChildCount() - (index + 2));// 从下一位开始删除，保留前两个结点
                // 把 clickAllPath 按照序号 删除完 clickAllPath的元素内容比 LinearLayout 中的子集少两个

                List<String> subList = new ArrayList<>(clickAllPath.subList(0, index / 2));// 6个子节点 点击了index=2
                clickAllPath.clear();
                clickAllPath.addAll(subList);
            }

            // 调用查询接口
            searchListByCurrentDir();
            System.out.println("第几个子元素被点击了=" + index + "  子元素名称=" + ((TextView) v).getText());
            // 查询下级目录和文件
//            View rootV = v.getRootView();
//            System.out.println(rootV.getId());
        });
        binding.dirNavLayout.addView(tname);

        TextView tcode = new TextView(getContext());
        tcode.setText(">");
        tcode.setTextSize(16);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(WRAP_CONTENT, MATCH_PARENT);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(params);
        layoutParams.setMarginStart(16);
        layoutParams.setMarginEnd(16);
        tcode.setLayoutParams(layoutParams);
        binding.dirNavLayout.addView(tcode);
    }

    private void handlerCaseDialog(Message msg) {
        if (!mAlertDialog.isShowing()) {
            mAlertDialog.show();
            WindowManager windowManager = (WindowManager) Objects.requireNonNull(getContext()).getSystemService(Context.WINDOW_SERVICE);
            Rect rect = windowManager.getCurrentWindowMetrics().getBounds();
            int w = Double.valueOf(rect.width() * 0.9).intValue();// 获取屏幕宽度
            int h = Double.valueOf(rect.height() * 0.3).intValue();// 获取屏幕宽度
            mAlertDialog.getWindow().setLayout(w, h);
        }

        TextView tv = mAlertDialog.findViewById(R.id.dialog_content_tv1);
        tv.setTextSize(16);
        tv.setText(msg.getData().getString("msg"));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}