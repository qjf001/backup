package com.qjf.backup.ui.log;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;
import com.qjf.backup.R;
import com.qjf.backup.ui.home.FileObjEntity;
import com.qjf.backup.ui.log.entity.ScanLogOut;
import com.qjf.backup.util.DialogUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.Holder> {
    List<ScanLogOut> datas;
    Context context;

    FragmentActivity activity;
    FragmentManager fragmentManager;

    public RecyclerViewAdapter(List<ScanLogOut> datas, Context context, FragmentActivity activity, FragmentManager fragmentManager) {
        this.datas = datas;
        this.context = context;
        this.activity = activity;
        this.fragmentManager = fragmentManager;
    }

    @NonNull
    @NotNull
    @Override
    public Holder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        // 创建绑定关系
        return new Holder(View.inflate(parent.getContext(), R.layout.text_view6, null));
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull RecyclerViewAdapter.Holder holder, int position) {
        //通过该方法来展示不同类型的item
//        int type = getItemViewType(position);
//        switch (type) {
//            case 1:
////                holder.iv.setImageResource(R.mipmap.ic_launcher);
//                break;
//            case 2:
////                holder.iv.setImageResource(R.mipmap.ic_launcher_round);
//                break;
//        }
        ScanLogOut log = datas.get(position);
        holder.tv1.setText(log.getScanDate());
        holder.tv2.setText(log.getScanType() + "/" + log.getTotalFiles() + "/" + log.getTotalSize());
        holder.tv3.setText("成功");
        if (!log.isScanSucc()) {
            holder.tv3.setText(HtmlCompat.fromHtml("<span style=\"color:red\"><b>失败</b></span>", HtmlCompat.FROM_HTML_MODE_LEGACY));
            holder.tv3.setOnClickListener(v -> {
                // 弹窗显示异常信息
                DialogUtil.showDialog(v.getContext(), null, "失败原因", log.getErrMsg());
            });
        }

        holder.tv4.setText("--");
        if (log.isClickUpLoadList()) {
            holder.tv4.setText(HtmlCompat.fromHtml("<span style=\"color:red\"><b>查看</b></span>", HtmlCompat.FROM_HTML_MODE_LEGACY));
            holder.tv4.setOnClickListener(v -> {
                Bundle bundle = new Bundle();
                bundle.putString("scanId", log.getId() + "");
                NavController controller = Navigation.findNavController(v);
                controller.navigate(R.id.page_upload_log_fragment, bundle);// 不能使用 头部的返回 ，使用系统返回 返回后重新加载了页面数据
                //压栈式跳转
//                fragmentManager
//                        .beginTransaction()
//                        .replace(R.id.scan_log_container, new UploadLogViewFragment(), null)
//                        .addToBackStack(null)
//                        .commit();
            });
        }

        if (log.getId() == 0) {
            holder.tv1.setText("");
            holder.tv2.setText("");
            holder.tv3.setText("");
            holder.tv4.setText("");
        }
    }

    @Override
    public int getItemCount() {
        return datas.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void replaceDatas(List<ScanLogOut> reloadDatas) {
        if (datas.size() != 0) {
            datas = reloadDatas;
            notifyDataSetChanged();// 刷新全部的item
        }
    }

    //创建holder类继承于RecyclerView.ViewHolder,并通过传入的view来初始化子控件
    public class Holder extends RecyclerView.ViewHolder {
        TextView tv1;
        TextView tv2;
        TextView tv3;
        TextView tv4;

        public Holder(View itemView) {
            super(itemView);
            setView(itemView);
        }

        public void setView(View view) {
            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Rect rect = windowManager.getCurrentWindowMetrics().getBounds();
            int w = rect.width() / 8;// 获取屏幕宽度  w=列的平均宽度

            tv1 = view.findViewById(R.id.txtV1);
            tv1.setWidth(2 * w + w / 2);
            tv1.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);

            tv2 = view.findViewById(R.id.txtV2);
            tv2.setWidth(3 * w);
            tv2.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);

            tv3 = view.findViewById(R.id.txtV3);
            tv3.setWidth(w);
            tv3.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);

            tv4 = view.findViewById(R.id.txtV4);
            tv4.setWidth(w);
            tv4.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        }
    }
}
