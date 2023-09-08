package com.qjf.backup.ui.log;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.alibaba.fastjson2.JSONObject;
import com.qjf.backup.databinding.FragmentLogBinding;
import com.qjf.backup.ui.log.entity.ScanLogOut;

import java.util.ArrayList;
import java.util.List;

/**
 * https://www.jb51.cc/android/1048672.html
 */
public class LogFragment extends Fragment {

    private FragmentLogBinding binding;
    private boolean hasLoadData = false; // 是否已加载数据

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
//        LogViewModel logViewModel =
//                new ViewModelProvider(this).get(LogViewModel.class);

        binding = FragmentLogBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 给一个查询 button ,触发查询日志

        // xml 中的 recycleView 中不能包含子布局，否则 FragmentLogBinding.inflate(inflater, container, false) 抛出没有 layoutManager 的异常
//        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getContext(), 1);// 这里给一个列， 具体的列的数量由 linerlayout 控制
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());// 可以直接使用 linearLayoutManager
        binding.recycleView.setLayoutManager(layoutManager);
        RecyclerViewAdapter recyclerViewAdapter = new RecyclerViewAdapter(noData(), getContext(), getActivity(),getActivity().getSupportFragmentManager());
        binding.recycleView.setAdapter(recyclerViewAdapter);
        binding.recycleView.addItemDecoration(new DividerItemDecoration(getContext(), LinearLayoutManager.VERTICAL));

        binding.loadLogBt.setOnClickListener(v -> recyclerViewAdapter.replaceDatas(loadMoreData()));

//        binding.refreshLayout.setRefreshHeader(new ClassicsHeader(Objects.requireNonNull(getContext())));// 布局xml中已经设置过了
//        binding.refreshLayout.setRefreshFooter(new ClassicsFooter(Objects.requireNonNull(getContext())));// 布局xml中已经设置过了

        // 设置下拉刷新监听器
//        binding.refreshLayout.setOnRefreshListener(refreshLayout -> refreshData());

        // 设置上拉加载更多监听器
//        binding.refreshLayout.setOnLoadMoreListener(refreshLayout -> loadMoreData());// 进行上拉加载更多的操作

//        logViewModel.setText() = "新数据";// // 更新数据
//        startActivity();
        return root;
    }


    private List<ScanLogOut> noData() {
        List<ScanLogOut> datas = new ArrayList<>();
        datas.add(new ScanLogOut(0L, "", "", 0, 0L, "", ""));
        return datas;
    }

    // 进行上拉加载更多的操作
    private List<ScanLogOut> loadMoreData() {
        List<ScanLogOut> scanLogOuts = DBHelper.getInstance(getContext()).queryScanLog(1);
        System.out.println(JSONObject.toJSONString(scanLogOuts));
        return scanLogOuts;
    }

    // 进行下拉刷新的操作
    private void refreshData() {
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

//        new FragmentTransaction().setMaxLifecycle(this,)
        if (getUserVisibleHint()) {// 应用程序提供的提示，表明该片段当前对用户可见。
            if (!hasLoadData) {
                // 一般是第一次打开TabActivity页面时，当前Fragment会走这里
//                loadData(getContext(), 1);
                hasLoadData = true;
            }
        }
        super.onViewCreated(view, savedInstanceState);
    }

    private void loadData(Context context, int pageNum) {
        int total = DBHelper.getInstance(context).countScanLog();
        if (total > 0) {
            int totalPage = total / 10 + (total % 10 != 0 ? 1 : 0);
            List<ScanLogOut> datas = DBHelper.getInstance(context).queryScanLog(pageNum);
            // 如何把数据加载到fragment的ScrollView中？？

        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}