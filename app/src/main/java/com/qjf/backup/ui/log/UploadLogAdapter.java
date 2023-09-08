package com.qjf.backup.ui.log;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.alibaba.fastjson2.JSONObject;
import com.qjf.backup.R;
import com.qjf.backup.ui.log.entity.BackupLog;
import com.qjf.backup.util.DialogUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class UploadLogAdapter extends RecyclerView.Adapter<UploadLogAdapter.Holder> {
    List<BackupLog> datas;
    Context context;

    public UploadLogAdapter(List<BackupLog> datas, Context context) {
        this.datas = datas;
        this.context = context;
    }

    @NonNull
    @NotNull
    @Override
    public UploadLogAdapter.Holder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        // 创建绑定关系
        return new UploadLogAdapter.Holder(View.inflate(parent.getContext(), R.layout.uploadlog_childview, null));
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull UploadLogAdapter.Holder holder, int position) {
        BackupLog log = datas.get(position);
        System.out.println("position=" + position + " , data=" + JSONObject.toJSONString(log));
        holder.tv1.setText(log.getUploadDate());
        holder.tv2.setText(log.getFileName() + "/" + log.getFileSize() + "/" + log.getTakeMill());
        holder.tv3.setText(log.getResult());
        if ("失败".equals(log.getResult())) {
            holder.tv3.setText(HtmlCompat.fromHtml("<span style=\"color:red\"><b>失败</b></span>", HtmlCompat.FROM_HTML_MODE_LEGACY));
            holder.tv3.setOnClickListener(v -> {
                // 弹窗显示异常信息
                DialogUtil.showDialog(v.getContext(), null, "失败原因", log.getErrMsg());
            });
        }
    }

    @Override
    public int getItemCount() {
        return datas.size();
    }

    public class Holder extends RecyclerView.ViewHolder {
        TextView tv1;
        TextView tv2;
        TextView tv3;

        public Holder(@NonNull @NotNull View itemView) {
            super(itemView);
            setItem(itemView);
        }

        private void setItem(View view) {
            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Rect rect = windowManager.getCurrentWindowMetrics().getBounds();
            int w = rect.width() / 8;// 获取屏幕宽度  w=列的平均宽度

            tv1 = view.findViewById(R.id.uploadLogV1);
            tv1.setWidth(2 * w + w / 2);
            tv1.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);

            tv2 = view.findViewById(R.id.uploadLogV2);
            tv2.setWidth(4 * w + w / 2);
            tv2.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);

            tv3 = view.findViewById(R.id.uploadLogV3);
            tv3.setWidth(w);
            tv3.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
        }
    }
}
