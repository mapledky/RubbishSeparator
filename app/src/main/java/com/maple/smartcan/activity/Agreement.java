package com.maple.smartcan.activity;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.maple.smartcan.R;
import com.maple.smartcan.util.ViewControl;
import com.maple.smartcan.util.WXShare;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;


public class Agreement extends PermissionActivity implements View.OnClickListener {

    //相关UI
    private Button bt_sharetext;
    private Button bt_sharemini;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agreement);
        registerToWechat();
        init();
    }

    private void init() {
        bt_sharetext = findViewById(R.id.agreement_sharetexttofriend);
        bt_sharemini = findViewById(R.id.agreement_sharemini);

        bt_sharemini.setOnClickListener(this);
        bt_sharetext.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (ViewControl.avoidRetouch()) {
            switch (view.getId()) {
                case R.id.agreement_sharetexttofriend:
                    shareText(getResources().getString(R.string.three_applaud));
                    break;

                case R.id.agreement_sharemini:
                    shareMini();
                    break;
                default:
                    break;

            }
        }
    }

    private IWXAPI api;

    //向微信注册api
    private void registerToWechat() {
        if (api == null) {
            api = WXAPIFactory.createWXAPI(this, WXShare.AppID, true);
            api.registerApp(WXShare.AppID);
        }
    }

    //分享文字信息
    private void shareText(String text) {
        WXShare.shareText(this, text);
    }
    //跳转到测试小程序
    private void shareMini(){
        @SuppressLint("UseCompatLoadingForDrawables") Drawable image = getResources().getDrawable(R.drawable.sanlian);
        BitmapDrawable bd = (BitmapDrawable) image;
        Bitmap bitmap = bd.getBitmap();
        WXShare.shareWechatMini(this,getResources().getString(R.string.three_applaud),getResources().getString(R.string.three_applaud),bitmap,1);
    }
}