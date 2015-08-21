# PhotoHelper
very easy to get a photo by specifing max with or max image file size in android,after that you can clipping it

## how to use:

1.create a helper instance and set callback then call `takePhoto()` or `choosePhoto()`: `new PhotoHelper(activity).callback(...).takePhoto()`;

2.call photoHelper.onActivityResult in Activity or fragment onActivityResult method;

## more features:
```
   new PhotoHelper(activity) //create a helper instance
        .quality(80) //compress image using quality 80
        .maxWidth(120,true) //try scale image unless with < 120dp,default is screen width
        .maxFileSizeKB(80) //try compress image unless file size < 80KB
        .cropping(true) //cropping the target image
        .autoRotate(true) //try rotate the image according to photo exif information (some samsung devices need to rotate)
        .callback(...) //implement PhotoHelper.CallBack call done() when action is done or error() where something is wrong
        .takePhoto() //or choosePhoto(), just do the job for you
```
  
## some util method:
```
   1.PhotoHelper.saveBitmap2File(Bitmap bitmap,File targetFile,int quality) //save bitmap to file
   2.PhotoHelper.getBitmap(File imageFile,int maxWidthInPx) //get Bitmap from a file and try scale image with max width
   3.PhotoHelper.rotateBitmap(Bitmap bitmap , int angle) //rotate image
   4.PhotoHelper.rotateBitmap(String src , Bitmap bitmap) //rotate image according to photo exif information
```
