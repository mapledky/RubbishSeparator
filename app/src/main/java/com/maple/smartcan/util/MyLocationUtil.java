package com.maple.smartcan.util;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public static void getLocationByGaode(Context context) {
        AMapLocationListener mLocationListener;
        mLocationListener = aMapLocation -> {
            if (aMapLocation != null) {
                if (aMapLocation.getErrorCode() == 0) {
                    //定位失败时，可通过ErrCode（错误码）信息来确定失败的原因，errInfo是错误信息，详见错误码表。
                    Intent intent = new Intent();
                    intent.setAction("com.maple.locationReceiver");
                    intent.putExtra("latitude", String.valueOf(aMapLocation.getLatitude()));
                    intent.putExtra("longitude", String.valueOf(aMapLocation.getLongitude()));
                    context.sendBroadcast(intent);
                }
            }
        };
        AMapLocationClient mLocationClient = new AMapLocationClient(context);
        AMapLocationClientOption mapLocationClientOption = new AMapLocationClientOption();
        mapLocationClientOption.setOnceLocationLatest(true);

        if (null != mLocationClient) {
            mLocationClient.setLocationOption(mapLocationClientOption);
            mLocationClient.setLocationListener(mLocationListener);
            mLocationClient.startLocation();
        }
    }
}

