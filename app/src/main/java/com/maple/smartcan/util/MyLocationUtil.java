package com.maple.smartcan.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.util.List;

public class MyLocationUtil {
    private static String provider;

    public static Location getMyLocation(Context context) {
        //获取当前位置信息
        //获取定位服务
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        //获取当前可用的位置控制器
        List<String> list = locationManager.getProviders(true);
        if (list.contains(LocationManager.GPS_PROVIDER)) {
            //GPS位置控制器
            provider = LocationManager.GPS_PROVIDER;//GPS定位
        } else if (list.contains(LocationManager.NETWORK_PROVIDER)) {
            //网络位置控制器
            provider = LocationManager.NETWORK_PROVIDER;//网络定位
        }

        if (provider != null) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return null;
            }
            return locationManager.getLastKnownLocation(provider);
        } else {
            Toast.makeText(context, "请检查网络或GPS是否打开", Toast.LENGTH_SHORT).show();
        }
        return null;
    }

}

