package com.github.tcking.giraffe.helper.exmaple;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.tcking.giraffe.helper.PhotoHelper;

import java.io.File;
import java.util.List;

/**
 * Created by tc(mytcking@gmail.com) on 15/8/20.
 */
public class MainActivity extends Activity {
    PhotoHelper photoHelper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //1.get a helper instance
        photoHelper = new PhotoHelper(this,savedInstanceState)
                .maxWidth(300, true)// image max with 300dp
                .autoRotate(true)// autoRotate according to exif information
                .quality(80)//try compress image using the quality 80
                .cropping(true)// cropping it after get a photo
                .maxFileSizeKB(90)//try compress image unless file size < 90KB
                .callback(new PhotoHelper.CallBack() {
                    @Override
                    public void done(File imageFile) {
                        //set imageView
                        ImageView imageView = (ImageView) findViewById(R.id.iv_photo);
                        imageView.setImageBitmap(PhotoHelper.getBitmap(imageFile));
                        ((TextView) findViewById(R.id.tv_fileSize)).setText(String.format("file size:%s",
                                Formatter.formatFileSize(getApplication(),imageFile.length())));
                    }

                    @Override
                    public void error(Exception e) {
                        Toast.makeText(getApplication(), "error:" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.btn_choosePhoto) {
                    photoHelper.cropping(false).choosePhoto();
                }else if (v.getId() == R.id.btn_test) {
//                    photoHelper.cropping(false).choosePhoto();
                    WifiManager wifiManager=(WifiManager)getSystemService(Context.WIFI_SERVICE);
                    wifiManager.startScan();
                    List<ScanResult> scanResults = wifiManager.getScanResults();
                    for (int i = 0; i < scanResults.size(); i++) {
                        ScanResult scanResult = scanResults.get(i);
                        System.out.println("======"+scanResult.toString());
                    }
                } else {
                    photoHelper.takePhoto();
                }
            }
        };
        findViewById(R.id.btn_choosePhoto).setOnClickListener(clickListener);
        findViewById(R.id.btn_takePhoto).setOnClickListener(clickListener);
        findViewById(R.id.btn_test).setOnClickListener(clickListener);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (photoHelper!=null) {
            //2.important,when taking photo,behind activity(fragment) maybe killed
            photoHelper.onSaveInstanceState(outState);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //3.important,don't forget to do this
        photoHelper.onActivityResult(requestCode,resultCode,data);
    }
}
