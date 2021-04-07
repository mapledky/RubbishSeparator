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

public class MyLocationUtil implements AMapLocationListener {

    private AMapLocationClient mlocationClient;
    private final Context context;

    public MyLocationUtil(Context context) {
        this.context = context;
    }

    public void getLocationByGaode() {
        if (mlocationClient == null) {
            mlocationClient = new AMapLocationClient(context);
            AMapLocationClientOption mLocationOption = new AMapLocationClientOption();
            //设置定位监听
            mlocationClient.setLocationListener(this);
            //设置为高精度定位模式
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            mLocationOption.setInterval(3000);
            //设置定位参数
            mlocationClient.setLocationOption(mLocationOption);
        }

        mlocationClient.stopLocation();
        mlocationClient.startLocation();
    }

    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
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
    }
}

