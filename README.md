# PhotoHelper
very easy to get a photo by specifing max with or max image file size in android,after that you can clipping it

![Sample Image](https://github.com/tcking/PhotoHelper/raw/master/ScreenShots/screenShot1.gif "ScreenShots")

# how to import library
## method 1: using gradle
 1. add `maven { url "https://jitpack.io" }` to your root project build file allprojects->repositories
 2. add `compile 'com.github.tcking.PhotoHelper:library:1.2'` to your app build file

## method 2: clone project
 1. git clone https://github.com/tcking/PhotoHelper.git
 2. android studio->file->New->Import module->select `library`

# how to use([**example code**](https://github.com/tcking/PhotoHelper/blob/master/exmaple/src/main/java/com/github/tcking/giraffe/helper/exmaple/MainActivity.java)):

1.create a helper instance and set callback then call `takePhoto()` or `choosePhoto()`: `new PhotoHelper(activity).callback(...).takePhoto()`;

2.call `photoHelper.onSaveInstanceState` in Activity or fragment `onSaveInstanceState` method,**never forget to do this**;

3.call `photoHelper.onActivityResult` in Activity or fragment `onActivityResult` method,**never forget to do this**;


# more features:

``` java
   new PhotoHelper(activity) //create a helper instance
        .quality(80) //try compress image using the quality 80 图片的质量,100为最高
        .maxWidth(120,true) //try scale image unless with < 120dp,default is screen width 图片的最大宽度
        .maxFileSizeKB(80) //try compress image unless file size < 80KB 图片文件的最大值,如果文件大于此值会进行压缩(牺牲清晰度)
        .cropping(true) //cropping the target image 是否进行剪裁,如果为true则选择完图片后会进入剪裁页
        .cropFactor(0.8f)// crop area factor 剪裁边框的大小,0.8表示宽的80%
        .autoRotate(true) //try rotate the image according to photo exif information (some samsung devices need to rotate)
        .callback(new PhotoHelper.CallBack() { // set callback 完成后的回调
                    @Override
                    public void done(File imageFile) {
                        //do something
                    }

                    @Override
                    public void error(Exception e) {
                        //error
                    }
                }) 
        .takePhoto() //or choosePhoto(), just do the job for you
```
  
# some util method:
``` java
PhotoHelper.saveBitmap2File(Bitmap bitmap,File targetFile,int quality) //save bitmap to file
PhotoHelper.getBitmap(File imageFile,int maxWidthInPx) //get Bitmap from a file and try scale image with max width
PhotoHelper.rotateBitmap(Bitmap bitmap , int angle) //rotate image
PhotoHelper.rotateBitmap(String src , Bitmap bitmap) //rotate image according to photo exif information
```
