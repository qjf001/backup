package com.qjf.backup.ui.log;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.qjf.backup.databinding.FragmentUploadlogBinding;
import com.qjf.backup.ui.log.entity.BackupLog;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UploadLogViewFragment extends Fragment {
    private FragmentUploadlogBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentUploadlogBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        List<BackupLog> datas = Objects.nonNull(getArguments()) ? getData(getArguments().getString("scanId")) : getData("");

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);// 可以直接使用 linearLayoutManager
        binding.uploadLogRecycleView.setLayoutManager(layoutManager);
        binding.uploadLogRecycleView.setAdapter(new UploadLogAdapter(datas, getContext()));
        binding.uploadLogRecycleView.addItemDecoration(new DividerItemDecoration(getContext(), LinearLayoutManager.VERTICAL));

        return root;
    }

    public List<BackupLog> getData(String scanId) {
        if (StringUtils.isBlank(scanId)) {
            return new ArrayList<>();
        }
        return DBHelper.getInstance(getContext()).queryUploadLog(scanId);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
