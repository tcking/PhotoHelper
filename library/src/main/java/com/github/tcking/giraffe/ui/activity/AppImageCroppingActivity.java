package com.github.tcking.giraffe.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.github.tcking.giraffe.helper.PhotoHelper;
import com.github.tcking.giraffe.helper.R;
import com.github.tcking.giraffe.ui.ImageCroppingView;

import java.io.File;
import java.io.IOException;

/**
 * Created by tc(mytcking@gmail.com) on 15/8/20.
 */
public class AppImageCroppingActivity extends Activity {
    private String TAG = "PhotoHelper";
    private File imageFile;
    private ImageCroppingView croppingView;
    private Bitmap bitmap;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_image_cropping);
        imageFile = (File) getIntent().getSerializableExtra("imageFile");
        croppingView = (ImageCroppingView) findViewById(R.id.app_cropping_view);
        croppingView.setCropFactor(getIntent().getFloatExtra("cropFactor",0.7f));
        bitmap = PhotoHelper.getBitmap(imageFile, getResources().getDisplayMetrics().widthPixels);
        croppingView.setImageBitmap(bitmap);
        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageCroppingView croppingView = (ImageCroppingView) findViewById(R.id.app_cropping_view);
                if (v.getId()==R.id.tv_ok) {
                    Intent data = new Intent();
                    try {
                        PhotoHelper.saveBitmap2File(croppingView.getCroppedImage(), imageFile);
                        data.putExtra("imageFile", imageFile);
                        setResult(Activity.RESULT_OK, data);
                        finish();
                    } catch (IOException e) {
                        Log.e(TAG, "error", e);
                    }
                }else if (v.getId() == R.id.iv_rotate_left) {
                    bitmap = PhotoHelper.rotateBitmap(AppImageCroppingActivity.this.bitmap, -90);
                    croppingView.setImageBitmap(bitmap);
                } else if (v.getId() == R.id.iv_rotate_right) {
                    bitmap = PhotoHelper.rotateBitmap(AppImageCroppingActivity.this.bitmap, 90);
                    croppingView.setImageBitmap(bitmap);
                } else {
                    finish();
                }
            }
        };
        findViewById(R.id.tv_ok).setOnClickListener(clickListener);
        findViewById(R.id.iv_rotate_left).setOnClickListener(clickListener);
        findViewById(R.id.iv_rotate_right).setOnClickListener(clickListener);
        findViewById(R.id.tv_cancel).setOnClickListener(clickListener);

    }
}
