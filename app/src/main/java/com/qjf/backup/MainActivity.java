package com.qjf.backup;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.qjf.backup.databinding.ActivityMainBinding;
import com.qjf.backup.ui.home.UploadTimerService;
import com.qjf.backup.ui.log.LogClearService;
import com.qjf.backup.util.Permission;
import com.qjf.backup.util.SspUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        registerNetworkCallback(getApplicationContext());

        // 启动后台服务
        if (SspUtil.getAutoBackUp(getApplicationContext()).equals("Y")) {
            startService(new Intent(MainActivity.this, UploadTimerService.class));// 自动备份，因为息屏状态下网络不通，因此自动备份没有太大意义了
            startService(new Intent(MainActivity.this, LogClearService.class));// 自动清理日志
        }
        // todo 启动一个服务 清理日志

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_log, R.id.navigation_setting)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
       new Permission().checkPermissions(this);
    }

    private void registerNetworkCallback(Context context) {
        if (Objects.nonNull(context)) {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            mConnectivityManager.registerNetworkCallback(request, mNetworkCallback);
        }
    }

    @Override
    protected void onDestroy() {
        if (Objects.nonNull(getApplicationContext())) {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) getApplicationContext()
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);// 解除
        }

        super.onDestroy();
        getDelegate().onDestroy();
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Permission.RequestCode) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Log.e("p","拒绝的权限名称：" + permissions[i]);
                    Log.e("p","拒绝的权限结果：" + grantResults[i]);
                    Log.e("p","有权限未授权，可以弹框出来，让客户去手机设置界面授权。。。");
                }else {
                    Log.e("p","授权的权限名称：" + permissions[i]);
                    Log.e("p","授权的权限结果：" + grantResults[i]);
                }
            }
        }
    }

    final ConnectivityManager.NetworkCallback mNetworkCallback = new ConnectivityManager.NetworkCallback(ConnectivityManager.NetworkCallback.FLAG_INCLUDE_LOCATION_INFO) {
        @Override
        public void onAvailable(@NonNull Network network) {
            super.onAvailable(network);
        }

        @Override
        public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities);
            WifiInfo wifiInfo = (WifiInfo) networkCapabilities.getTransportInfo();
            if (Objects.nonNull(wifiInfo)) {// 可以监听到，但是怎么使用啊
                String ssid = wifiInfo.getSSID().replace("\"", "").replace("<", "").replace(">", "");
                if (StringUtils.isNotBlank(ssid) && !"unknownssid".equals(ssid)) {
                    Log.v("SSID回调", "currentSSID_NOW=" + ssid);
                }
            }
        }
    };

    final NetworkRequest request =
            new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .build();
}