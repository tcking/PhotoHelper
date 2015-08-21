package com.github.tcking.giraffe.helper.exmaple;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.tcking.giraffe.helper.PhotoHelper;

import java.io.File;

/**
 * Created by tc(mytcking@gmail.com) on 15/8/20.
 */
public class MainActivity extends Activity {
    PhotoHelper photoHelper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        photoHelper = new PhotoHelper(this)
                .maxWidth(300, true)
                .autoRotate(true)
                .quality(80)
                .cropping(true)
                .maxFileSizeKB(90)
                .callback(new PhotoHelper.CallBack() {
                    @Override
                    public void done(File imageFile) {
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
                } else {
                    photoHelper.takePhoto();
                }
            }
        };
        findViewById(R.id.btn_choosePhoto).setOnClickListener(clickListener);
        findViewById(R.id.btn_takePhoto).setOnClickListener(clickListener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        photoHelper.onActivityResult(requestCode,resultCode,data);
    }
}
