package com.github.tcking.giraffe.helper;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;

import com.github.tcking.giraffe.ui.activity.AppImageCroppingActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * <pre>
 * very easy  to get a photo in android,how to use:
 * 1.create a helper instance and set callback then call takePhoto() or choosePhoto(): new PhotoHelper(activity).callback(...).takePhoto();
 * 2.call photoHelper.onActivityResult in Activity or fragment onActivityResult method;
 *
 * more features:
 * new PhotoHelper(activity) //create a helper instance
 *      .quality(80) //compress image using quality 80
 *      .maxWidth(120,true) //try scale image unless with < 120dp,default is screen width
 *      .maxFileSizeKB(80) //try compress image unless file size < 80KB
 *      .cropping(true) //cropping the target image
 *      .autoRotate(true) //try rotate the image according to photo exif information (some samsung devices need to rotate)
 *      .callback(...) //implement PhotoHelper.CallBack call done() when action is done or error() where something is wrong
 *      .takePhoto() //or choosePhoto(), just do the job for you
 *
 * some util method:
 * 1.PhotoHelper.saveBitmap2File(Bitmap bitmap,File targetFile,int quality) //save bitmap to file
 * 2.PhotoHelper.getBitmap(File imageFile,int maxWidthInPx) //get Bitmap from a file and try scale image with max width
 * 3.PhotoHelper.rotateBitmap(Bitmap bitmap , int angle) //rotate image
 * 4.PhotoHelper.rotateBitmap(String src , Bitmap bitmap) //rotate image according to photo exif information
 * </pre>
 * Created by tc(mytcking@gmail.com) on 15/8/19.
 */
public class PhotoHelper {
    private static final String TAG = "PhotoHelper";
    private static final String KEY_TEMPFILE = "com.github.tcking.giraffe.helper.PhotoHelper.tempFile";

    private static boolean autoRotate;
    private final Activity context;
    private Fragment fragment;
    private Float cropFactor = 0.7f;
    private String dir = "/giraffe/images";
    private final String FROM_CAMERA = "CAMERA";
    private final String FROM_GALLERY = "GALLERY";
    private String from = FROM_GALLERY;
    private int quality = 80;
    private final int REQUESTCODE_TAKEPHOTO = 6211;
    private final int REQUESTCODE_CHOOSEPHOTO = 6212;
    private final int REQUESTCODE_CROPPING = 6213;
    private File tempFile;
    private float maxWidth;
    private float maxHeight;
    private int maxFileSizeKB;
    private CallBack callback = new CallBack() {
        @Override
        public void done(File imageFile) {
            Log.d(TAG, imageFile.getAbsolutePath());
        }

        @Override
        public void error(Exception e) {
            Log.e(TAG, "CallBack.error ", e);
        }
    };
    private File home;
    private boolean cropping;

    public PhotoHelper maxFileSizeKB(int maxFileSizeKB) {
        this.maxFileSizeKB = maxFileSizeKB;
        return this;
    }

    public PhotoHelper cropping(boolean cropping) {
        this.cropping = cropping;
        return this;
    }

    public PhotoHelper autoRotate(boolean autoRotate) {
        this.autoRotate = autoRotate;
        return this;
    }

    public PhotoHelper cropFactor(Float cropFactor) {
        this.cropFactor = cropFactor;
        return this;
    }


    public PhotoHelper dir(String dir) {
        this.dir = dir;
        return this;
    }


    public PhotoHelper(Activity context) {
        this.context = context;
    }

    public PhotoHelper(Activity context, Bundle savedInstanceState) {
        this.context = context;
        recover(savedInstanceState);
    }

    private void recover(Bundle savedInstanceState) {
        if (savedInstanceState != null && !TextUtils.isEmpty(savedInstanceState.getString(KEY_TEMPFILE))) {
            tempFile = new File(savedInstanceState.getString(KEY_TEMPFILE));
        }
    }

    public PhotoHelper(Fragment fragment) {
        this.context = fragment.getActivity();
        this.fragment = fragment;
    }

    public PhotoHelper(Fragment fragment, Bundle savedInstanceState) {
        this.context = fragment.getActivity();
        this.fragment = fragment;
        recover(savedInstanceState);
    }


    public PhotoHelper quality(int quality) {
        this.quality = quality;
        return this;
    }

    private void doIt() {
        insureDirs();
        if (maxWidth == 0 && maxHeight == 0) {
            maxScreenWidth();
        }
        try {
            if (FROM_CAMERA.equals(from)) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.Images.Media.ORIENTATION, 0);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(createTempFile()));
                startActivityForResult(intent, REQUESTCODE_TAKEPHOTO);
            } else {
                Intent chooser = Intent.createChooser(new Intent(Intent.ACTION_PICK).setType("image/*"), "选择图片");
                chooser.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(createTempFile()));
                startActivityForResult(chooser, REQUESTCODE_CHOOSEPHOTO);
            }
        } catch (Exception e) {
            callback.error(e);
        }
    }

    private void insureDirs() {
        File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), this.dir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        home = dir;
    }


    private File createTempFile() throws IOException {
        File tempDir = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) ?
                context.getExternalCacheDir() : context.getCacheDir();
        File file = new File(tempDir, "takePhoto.tmp");
        if (file.exists()) {
            file.delete();
        } else {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
        }

        tempFile = file;
        tempFile.createNewFile();
        return file;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            if (resultCode == Activity.RESULT_OK) {
                if (requestCode == REQUESTCODE_TAKEPHOTO) {
                    if (tempFile != null && tempFile.exists()) {
                        tryCompress(tempFile);
                    } else {
                        callback.error(new Exception("tempFile is not exists"));
                    }
                } else if (requestCode == REQUESTCODE_CHOOSEPHOTO) {
                    if (data != null) {
                        if (data.getData() != null) {
                            File inputImage = new File(data.getData().getPath());
                            if (!inputImage.exists()) {
                                inputImage = new File(getFilePath(data.getData()));
                            }
                            tryCompress(inputImage);
                        } else {

                        }
                    } else if (tempFile != null && tempFile.exists()) {
                        tryCompress(tempFile);
                    } else {
                        callback.error(new Exception("tempFile is not exists"));
                    }
                } else if (requestCode == REQUESTCODE_CROPPING) {
                    if (data != null) {
                        File inputImage = (File) data.getSerializableExtra("imageFile");
                        File outputFile = createImageFile();
                        compress(inputImage, outputFile, quality, maxWidth, maxHeight, maxFileSizeKB);
                        callback.done(outputFile);
                    } else {
                        callback.error(new Exception("data is null"));
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "onActivityResult error", e);
            callback.error(e);
        }
    }

    private void tryCompress(File tempFile) {
        if (!tempFile.exists()) {
            throw new RuntimeException("image file not exists:" + tempFile.getAbsolutePath());
        }
        File outputFile = createImageFile();
        if (cropping) {
            compress(tempFile, outputFile, quality, context.getResources().getDisplayMetrics().widthPixels, 0, maxFileSizeKB);
            Intent intent = new Intent(context, AppImageCroppingActivity.class);
            intent.putExtra("imageFile", outputFile);
            intent.putExtra("cropFactor", cropFactor);
            startActivityForResult(intent, REQUESTCODE_CROPPING);
        } else {
            compress(tempFile, outputFile, quality, maxWidth, maxHeight, maxFileSizeKB);
            callback.done(outputFile);
        }
    }

    private void startActivityForResult(Intent intent, int requestCode) {
        if (fragment != null) {
            fragment.startActivityForResult(intent, requestCode);
        } else {
            context.startActivityForResult(intent, requestCode);
        }
    }


    private File createImageFile() {
        insureDirs();
        return new File(home, UUID.randomUUID().toString() + ".jpeg");
    }


    /**
     * 先按照width, hight设置分辨率，再压缩到100k以下
     *
     * @param inputImage
     * @param maxWidth      最大宽度
     * @param maxHeight     最大高度
     * @param maxFileSizeKB 文件最大大小(单位KB)
     * @return
     */
    public static void compress(File inputImage, File outputImage, int quality, float maxWidth, float maxHeight, int maxFileSizeKB) {
        try {
            BitmapFactory.Options op = new BitmapFactory.Options();
            if (maxWidth > 0 || maxHeight > 0) {//resize
                op.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(inputImage.getAbsolutePath(), op);
                int wRatio = 1;
                int hRatio = 1;
                if (maxWidth > 0) {
                    wRatio = (int) Math.ceil(op.outWidth / maxWidth); //计算宽度比例
                }
                if (maxHeight > 0) {
                    hRatio = (int) Math.ceil(op.outHeight / maxHeight); //计算高度比例
                }
                int scale = Math.max(wRatio, hRatio);
                op.inSampleSize = scale;
            }

            op.inJustDecodeBounds = false;
            Bitmap resizeBitmap = BitmapFactory.decodeFile(inputImage.getAbsolutePath(), op);
            if (autoRotate) {
                resizeBitmap = rotateBitmap(inputImage.getAbsolutePath(), resizeBitmap);
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resizeBitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            if (maxFileSizeKB > 0) {
                int nextQuality = 100;
                while ((nextQuality = nextQuality - 10) >= 0 && baos.size() / 1024 > maxFileSizeKB) { // 循环判断如果压缩后图片是否大于100kb,大于继续压缩
                    if (nextQuality >= 0) {
                        baos.reset();// 重置baos即清空baos
                        resizeBitmap.compress(Bitmap.CompressFormat.JPEG, nextQuality, baos);// 这里压缩options%，把压缩后的数据存放到baos中
                    }
                }
            }
            resizeBitmap.recycle();
            FileOutputStream fos = new FileOutputStream(outputImage);
            baos.writeTo(fos);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            Log.e(TAG, "PhotoHelper.compressImage error", e);
            throw new RuntimeException(e);
        }
    }

    public PhotoHelper maxWidth(float _maxWidth) {
        return maxWidth(_maxWidth, false);
    }

    public PhotoHelper maxWidth(float _maxWidth, boolean dip) {
        this.maxWidth = dip ? db2px(_maxWidth) : _maxWidth;
        return this;
    }

    public int db2px(float dip) {
        return (int) (dip * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    public PhotoHelper maxScreenWidth() {
        this.maxWidth = context.getResources().getDisplayMetrics().widthPixels;
        return this;
    }

    public PhotoHelper maxHeight(float _maxHeight) {
        return maxHeight(_maxHeight, false);
    }

    public PhotoHelper maxHeight(float _maxHeight, boolean dip) {
        this.maxHeight = dip ? db2px(_maxHeight) : _maxHeight;
        return this;
    }


    public void takePhoto() {
        this.from = FROM_CAMERA;
        doIt();
    }

    public static Bitmap getBitmap(File imageFile) {
        return getBitmap(imageFile, 0);
    }

    public static Bitmap getBitmap(File imageFile, int maxWidthInPx) {
        BitmapFactory.Options op = new BitmapFactory.Options();
        if (maxWidthInPx > 0) {//resize
            op.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imageFile.getAbsolutePath(), op);
            int wRatio = (int) Math.ceil(op.outWidth / maxWidthInPx); //计算宽度比例
            op.inSampleSize = wRatio;
        }
        op.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(imageFile.getAbsolutePath(), op);
    }

    public static void saveBitmap2File(Bitmap bitmap, File targetFile) throws IOException {
        saveBitmap2File(bitmap, targetFile, 100);
    }

    public static void saveBitmap2File(Bitmap bitmap, File targetFile, int quality) throws IOException {
        if (!targetFile.getParentFile().exists()) {
            targetFile.getParentFile().mkdirs();
        }
        targetFile.createNewFile();
        FileOutputStream fos = new FileOutputStream(targetFile);
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos);
        fos.flush();
        fos.close();
    }

    public PhotoHelper callback(CallBack callBack) {
        this.callback = callBack;
        return this;
    }

    public void choosePhoto() {
        from = FROM_GALLERY;
        doIt();
    }

    public void onSaveInstanceState(Bundle outState) {
        if (tempFile != null) {
            outState.putString(KEY_TEMPFILE, tempFile.getAbsolutePath());
        }
    }


    public interface CallBack {
        void done(File imageFile);

        void error(Exception e);
    }

    /**
     * @see http://sylvana.net/jpegcrop/exif_orientation.html
     */
    public static Bitmap rotateBitmap(String src, Bitmap bitmap) {
        try {
            int orientation = getExifOrientation(src);

            if (orientation == 1) {
                return bitmap;
            }

            Matrix matrix = new Matrix();
            switch (orientation) {
                case 2:
                    matrix.setScale(-1, 1);
                    break;
                case 3:
                    matrix.setRotate(180);
                    break;
                case 4:
                    matrix.setRotate(180);
                    matrix.postScale(-1, 1);
                    break;
                case 5:
                    matrix.setRotate(90);
                    matrix.postScale(-1, 1);
                    break;
                case 6:
                    matrix.setRotate(90);
                    break;
                case 7:
                    matrix.setRotate(-90);
                    matrix.postScale(-1, 1);
                    break;
                case 8:
                    matrix.setRotate(-90);
                    break;
                default:
                    return bitmap;
            }

            try {
                Bitmap oriented = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                bitmap.recycle();
                return oriented;
            } catch (OutOfMemoryError e) {
                Log.e(TAG, "rotateBitmap error", e);
                return bitmap;
            }
        } catch (IOException e) {
            Log.e(TAG, "rotateBitmap error", e);
        }

        return bitmap;
    }

    private static int getExifOrientation(String src) throws IOException {
        int orientation = 1;

        try {
            /**
             * if your are targeting only api level >= 5
             * ExifInterface exif = new ExifInterface(src);
             * orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
             */
            if (Build.VERSION.SDK_INT >= 5) {
                Class<?> exifClass = Class.forName("android.media.ExifInterface");
                Constructor<?> exifConstructor = exifClass.getConstructor(new Class[]{String.class});
                Object exifInstance = exifConstructor.newInstance(new Object[]{src});
                Method getAttributeInt = exifClass.getMethod("getAttributeInt", new Class[]{String.class, int.class});
                Field tagOrientationField = exifClass.getField("TAG_ORIENTATION");
                String tagOrientation = (String) tagOrientationField.get(null);
                orientation = (Integer) getAttributeInt.invoke(exifInstance, new Object[]{tagOrientation, 1});
            }
        } catch (Exception e) {
            Log.e(TAG, "getExifOrientation error", e);
        }

        return orientation;
    }

    public static Bitmap rotateBitmap(Bitmap bitmap, int angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return resizedBitmap;
    }

    /**
     * Gets the corresponding path to a file from the given content:// URI
     *
     * @param uri The content:// URI to find the file path from
     * @return the file path as a string
     */
    public String getFilePath(Uri uri) {
//        if (uri.getScheme().equals("file")) {
//            return uri.getPath();
//        }
//        String filePath;
//        String[] filePathColumn = {MediaStore.MediaColumns.DATA};
//        Cursor cursor = context.getContentResolver().query(uri, filePathColumn, null, null, null);
//        if (cursor == null) {
//            throw new RuntimeException("can't get path of content:" + uri.toString());
//        }
//        cursor.moveToFirst();
//        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
//        filePath = cursor.getString(columnIndex);
//        cursor.close();
//        return filePath;
        return getPath(context, uri);
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri     The Uri to query.
     * @author paulburke
     */
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    private static String getDataColumn(Context context, Uri uri, String selection,String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
}
