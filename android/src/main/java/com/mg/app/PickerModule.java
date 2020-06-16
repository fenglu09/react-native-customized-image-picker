package com.mg.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import androidx.core.content.FileProvider;
import android.util.Base64;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropActivity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import cn.finalteam.rxgalleryfinal.RxGalleryFinal;
import cn.finalteam.rxgalleryfinal.bean.ImageCropBean;
import cn.finalteam.rxgalleryfinal.bean.MediaBean;
import cn.finalteam.rxgalleryfinal.imageloader.ImageLoaderType;
import cn.finalteam.rxgalleryfinal.rxbus.RxBus;
import cn.finalteam.rxgalleryfinal.rxbus.RxBusResultDisposable;
import cn.finalteam.rxgalleryfinal.rxbus.event.ImageMultipleResultEvent;
import cn.finalteam.rxgalleryfinal.rxbus.event.ImageRadioResultEvent;
import cn.finalteam.rxgalleryfinal.ui.RxGalleryListener;
import cn.finalteam.rxgalleryfinal.ui.base.IRadioImageCheckedListener;
import cn.finalteam.rxgalleryfinal.utils.FileUtils;
import cn.finalteam.rxgalleryfinal.utils.Logger;
import cn.finalteam.rxgalleryfinal.utils.ThemeUtils;

class PickerModule extends ReactContextBaseJavaModule implements ActivityEventListener {
    private static final String E_ACTIVITY_DOES_NOT_EXIST = "E_ACTIVITY_DOES_NOT_EXIST";

    private Promise mPickerPromise;
    private boolean isWaterMark = false;
    private boolean isSaveAlbum = true;
    private String address = "";
    private String name = "";


    private boolean cropping = false;
    private boolean multiple = false;
    private boolean isCamera = false;
    private boolean includeBase64 = false;
    private boolean openCameraOnStart = false;
    private boolean isVideo = false;
    private boolean isHidePreview = false;
    private boolean isPlayGif = false;
    private boolean isHideVideoPreview = false;
    private String title = null;
    private String imageLoader = null;
    //Light Blue 500
    private int width = 200;
    private int height = 200;
    private int maxSize = 9;
    private int maxImageSize;

    private int compressQuality = -1;
    private boolean returnAfterShot = false;
    private final ReactApplicationContext mReactContext;

    private Compression compression = new Compression();
    private ReadableMap options;

    PickerModule(ReactApplicationContext reactContext) {
        super(reactContext);
        mReactContext = reactContext;
        mReactContext.addActivityEventListener(this);


    }

    @Override
    public String getName() {
        return "ImageCropPicker";
    }

    private void setConfiguration(final ReadableMap options) {
        //add by david 手机拍照后自动保存照片到系统相册  start
        isSaveAlbum = options.hasKey("isSaveAlbum") && options.getBoolean("isSaveAlbum");
        //add by david 手机拍照后自动保存照片到系统相册 end
        maxImageSize = options.hasKey("maxImageSize") ? options.getInt("maxImageSize") : -1;
        isWaterMark = options.hasKey("isWaterMark") && options.getBoolean("isWaterMark");
        address = options.hasKey("address") ? options.getString("address") : "";
        name = options.hasKey("name") ? options.getString("name") : "";


        multiple = options.hasKey("multiple") && options.getBoolean("multiple");
        isCamera = options.hasKey("isCamera") && options.getBoolean("isCamera");
        openCameraOnStart = options.hasKey("openCameraOnStart") && options.getBoolean("openCameraOnStart");
        width = options.hasKey("width") ? options.getInt("width") : width;
        height = options.hasKey("height") ? options.getInt("height") : height;
        maxSize = options.hasKey("maxSize") ? options.getInt("maxSize") : maxSize;
        cropping = options.hasKey("cropping") ? options.getBoolean("cropping") : cropping;
        includeBase64 = options.hasKey("includeBase64") && options.getBoolean("includeBase64");
        compressQuality = options.hasKey("compressQuality") ? options.getInt("compressQuality") : compressQuality;
        title = options.hasKey("title") ? options.getString("title") : title;
        returnAfterShot = options.hasKey("returnAfterShot") && options.getBoolean("returnAfterShot");
        isVideo = options.hasKey("isVideo") && options.getBoolean("isVideo");
        isHidePreview = options.hasKey("isHidePreview") && options.getBoolean("isHidePreview");
        isHideVideoPreview = options.hasKey("isHideVideoPreview") && options.getBoolean("isHideVideoPreview");
        isPlayGif = options.hasKey("isPlayGif") && options.getBoolean("isPlayGif");

        imageLoader = options.hasKey("imageLoader") ? options.getString("imageLoader") : imageLoader;
        this.options = options;
    }

    private static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }

        return type;
    }


    public static Bitmap zoomImage(Bitmap bgimage, double newWidth, double newHeight) {
        // 获取这个图片的宽和高
        float width = bgimage.getWidth();
        float height = bgimage.getHeight();
        // 创建操作图片用的matrix对象
        Matrix matrix = new Matrix();
        // 计算宽高缩放率
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // 缩放图片动作
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap bitmap = Bitmap.createBitmap(bgimage, 0, 0, (int) width, (int) height, matrix, true);
        return bitmap;
    }

    private WritableMap getAsyncSelection(final Activity activity, String path) throws Exception {
        String mime = getMimeType(path);
        if (mime != null && mime.startsWith("video/")) {
            return getVideo(activity, path, mime);
        }


        //add-----质量处理
//        Log.d("aaa", path+"--质量处理");
//        try {
//            Integer maxWidth = options.hasKey("width") ? options.getInt("width") : null;
//            Integer maxHeight = options.hasKey("height") ? options.getInt("height") : null;
//            Integer quality = options.hasKey("compressQuality") ? options.getInt("compressQuality") : null;
//
//
//            String pathout = PickerModule.img_savepath.substring(0, PickerModule.img_savepath.length() - 1);
//            FileInputStream fis = new FileInputStream(path);
//            FileOutputStream fiout = new FileOutputStream(pathout+"/11.jpg");
//            Bitmap bitmap = BitmapFactory.decodeStream(fis);
//            zoomImage(bitmap, maxWidth, maxHeight).compress(Bitmap.CompressFormat.JPEG, quality, fiout);
//
//        } catch (Exception e) {
//        }

        return getImage(activity, path);

    }

    private WritableMap getAsyncSelection(final Activity activity, ImageCropBean result) throws Exception {

        String path = result.getOriginalPath();
        return getAsyncSelection(activity, path);
    }

    private WritableMap getAsyncSelection(final Activity activity, MediaBean result) throws Exception {

        String path = result.getOriginalPath();
        return getAsyncSelection(activity, path);


    }

    private WritableMap getAsyncSelection2(final Activity activity, String path) throws Exception {
        return getImage(activity, path);
    }

    private WritableMap getVideo(Activity activity, String path, String mime) throws Exception {
        Bitmap bmp = validateVideo(path);

        WritableMap video = new WritableNativeMap();
        video.putInt("width", bmp.getWidth());
        video.putInt("height", bmp.getHeight());
        video.putString("mime", mime);
        video.putInt("size", (int) new File(path).length());
        video.putString("path", "file://" + path);

        return video;
    }

    private WritableMap getImage(final Activity activity, String path) throws Exception {
        if (path.startsWith("http://") || path.startsWith("https://")) {
            throw new Exception("Cannot select remote files");
        }
        validateImage(path);
        File f = new File(path);

        File compressedImage;
        if (f.length() / 1024 > maxImageSize) {
            compressedImage = compression.compressImage(activity, options, path);
            if (path.contains("mogu/CameraPic")) {
                f.delete();
            }
        } else {
            compressedImage = f;
        }
//
//
        String compressedImagePath = compressedImage.getPath();
        BitmapFactory.Options options = validateImage(compressedImagePath);

        WritableMap image = new WritableNativeMap();
        image.putString("path", "file://" + compressedImage.getAbsolutePath());
        image.putInt("width", options.outWidth);
        image.putInt("height", options.outHeight);
        image.putString("mime", options.outMimeType);
        image.putInt("size", (int) new File(compressedImagePath).length());
        if (includeBase64) {
            image.putString("data", getBase64StringFromFile(compressedImagePath));
        }

        return image;


    }

    private WritableMap getImage(final Activity activity, ImageCropBean result) throws Exception {

        String path = result.getOriginalPath();
        return getImage(activity, path);
    }

    private WritableMap getImage(final Activity activity, MediaBean result) throws Exception {

        String path = result.getOriginalPath();
        return getImage(activity, path);
    }

    private void initImageLoader(Activity activity) {

        ImageLoaderConfiguration.Builder config = new ImageLoaderConfiguration.Builder(activity);
        config.threadPriority(Thread.NORM_PRIORITY - 2);
        config.denyCacheImageMultipleSizesInMemory();
        config.diskCacheFileNameGenerator(new Md5FileNameGenerator());
        config.diskCacheSize(50 * 1024 * 1024); // 50 MiB
        config.tasksProcessingOrder(QueueProcessingType.LIFO);
        ImageLoader.getInstance().init(config.build());
    }

    ImageCropBean result;

//    public void openCamera(boolean watermark) {
//
//        File f = new File(img_savepath);
//        if (!f.exists()) {
//            f.mkdirs();
//        }
//
//        Uri myimageUri = Uri.parse("file://" + img_savepath + System.currentTimeMillis()
//                + ".jpg");
//        imageUri = myimageUri.toString();
//        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, myimageUri);
//
//        activityhide();
//        activity.startActivityForResult(cameraIntent, TAKE_IMAGE_REQUEST_CODE);
//    }

    private File createOriImageFile() throws Exception {
        File image = new File(img_savepath + System.currentTimeMillis() + ".jpg");
        image.getParentFile().mkdirs();
        return image;
    }

    public void openCamera(boolean watermark) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);// 打开相机
        File oriPhotoFile = null;
        Uri uri = null;
        try {
            oriPhotoFile = createOriImageFile();
            if (oriPhotoFile != null) {
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.N) {
                    uri = Uri.fromFile(oriPhotoFile);
                } else {
                    /** modify by david at 2019-8-6 start */
                    // authorities 更改为当前包名
                    String authorities = AppUtils.getAppProcessName(activity);
                    // uri = FileProvider.getUriForFile(activity, "com.mglink.mgcircle",
                    // oriPhotoFile);
                    uri = FileProvider.getUriForFile(activity, authorities, oriPhotoFile);
                    /** modify by david at 2019-8-6 start */
                    }
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);//私有目录读写权限
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                activity.startActivityForResult(intent, TAKE_IMAGE_REQUEST_CODE);

                imageUri = oriPhotoFile.getAbsolutePath();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void activityhide() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //此时已在主线程中，可以更新UI了
                activity.setVisible(false);
            }
        });

        new Handler().postDelayed(new Runnable() {
            public void run() {
                //execute the task
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //此时已在主线程中，可以更新UI了
                        activity.setVisible(true);
                    }
                });

            }
        }, 1000);

    }

    String imageUri;
    private final int TAKE_IMAGE_REQUEST_CODE = 1001;
    private final int crop_REQUEST_CODE = 1002;

    public static final String SDCARD = Environment
            .getExternalStorageDirectory().toString();
    public static final String appDirName = "mogu";
    public static final String cacheDir = SDCARD + "/" + appDirName + "/"
            + "CameraPic";
    public static String img_savepath = cacheDir + "/";

    public int dip2px(Context context, float dpValue) {
//        float scale = context.getResources().getDisplayMetrics().density;
        float scale = texts / 500;
        Log.d("aaa", scale + "----scale");
        return (int) (dpValue * scale + 0.5f);
//        return (int) (dpValue * 1 + 0.5f);

    }


    public int gettextWidth(Paint p, String str) {

        int textWidth = 0;
        Rect bounds = new Rect();
        p.getTextBounds(str, 0, str.length(), bounds);
        textWidth = bounds.right - bounds.left;
        return textWidth;

    }

    public int gettextHeight(Paint p) {

        int textHeight = 0;
        Paint.FontMetricsInt fontMetrics = p.getFontMetricsInt();
        textHeight = fontMetrics.bottom - fontMetrics.top;
        return textHeight;
    }

    public String getWeek() {
        Calendar cal = Calendar.getInstance();
        int i = cal.get(Calendar.DAY_OF_WEEK);
        switch (i) {
            case 1:
                return "星期日";
            case 2:
                return "星期一";
            case 3:
                return "星期二";
            case 4:
                return "星期三";
            case 5:
                return "星期四";
            case 6:
                return "星期五";
            case 7:
                return "星期六";
            default:
                return "";
        }
    }

    float texts;

    private Bitmap createWatermark(Bitmap target, String mark) {
        int w = target.getWidth();
        int h = target.getHeight();


        /*add by david 内存溢出处理 start*/
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
        /*add by david 内存溢出处理 end*/
        //Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        Paint p = new Paint();

        // 水印的颜色
        p.setColor(Color.WHITE);

        // 水印的字体大小
        texts = w;
        if (h < w) {
            texts = h;
        }
        p.setTextSize(texts / 20);

        p.setAntiAlias(true);// 去锯齿

        canvas.drawBitmap(target, 0, 0, p);

        // 在左边的中间位置开始添加水印
        float drawy = h - texts / 15;
        float drawx = w / 30;
//        if(h<w){
//            drawx=w/10;
//        }


//        canvas.drawText(address, drawx, drawy, p);
        //水印日期
        int baseLineY = dip2px(mReactContext, 17);
        int baseLineX = dip2px(mReactContext, 17);
        //字体大小
        float txtsize = dip2px(mReactContext, 45);
        float paddingnum = dip2px(mReactContext, 10);
        float line1y = baseLineX + txtsize - dip2px(mReactContext, 5);

        String time = mark.substring(mark.length() - 5, mark.length());//时分
        Paint p_time = new Paint();
        p_time.setAntiAlias(true);// 去锯齿
        p_time.setFakeBoldText(true);//加粗
        p_time.setColor(Color.WHITE);
        p_time.setTextSize(txtsize);
        canvas.drawText(time, baseLineX + paddingnum, line1y, p_time);

        int p_timey = gettextHeight(p_time);
        Log.d("aaa", p_timey + "----p_timey");

        //日期
        String data = mark.substring(0, mark.length() - 6);
        String datas[] = data.split("-");
        String datanew = datas[0] + "." + datas[1] + "." + datas[2];

        float datex = baseLineX + paddingnum + gettextWidth(p_time, time) + dip2px(mReactContext, 15);

        float datasize = dip2px(mReactContext, 20);
        Paint p_data = new Paint();
        p_data.setAntiAlias(true);// 去锯齿
        p_data.setFakeBoldText(true);
        p_data.setColor(Color.WHITE);
        p_data.setTextSize(datasize);
        canvas.drawText(datanew, datex, line1y, p_data);

        //星期
        float weekend = datex + gettextWidth(p_data, datanew) + dip2px(mReactContext, 10);
        String weekvalue = getWeek();
        float weeksize = dip2px(mReactContext, 20);

        Paint p_weekend = new Paint();
        p_weekend.setAntiAlias(true);// 去锯齿
        p_weekend.setFakeBoldText(true);
        p_weekend.setColor(Color.WHITE);
        p_weekend.setTextSize(weeksize);
        canvas.drawText(weekvalue, weekend, line1y, p_weekend);

        float line2y = line1y + dip2px(mReactContext, 30);
        float txtaddress = dip2px(mReactContext, 19);
        Paint p_address = new Paint();
        p_address.setAntiAlias(true);// 去锯齿
        p_address.setFakeBoldText(true);//加粗
        p_address.setColor(Color.WHITE);
        p_address.setTextSize(txtaddress);
        if (!address.equals("")) {
            line2y = line2y + paddingnum;

            //地址图标
            //在画布上绘制水印图片
            Resources res = mReactContext.getResources();
            Bitmap watermark = BitmapFactory.decodeResource(res, R.drawable.location);
            watermark = createnewWatermark(watermark);
            canvas.drawBitmap(watermark, baseLineX + paddingnum, line2y - dip2px(mReactContext, 17), null);


            //地址
            int addressnum = address.length();
            int endnum = 0;
            List<String> txts = new ArrayList<String>();

            for (int i = 0; i < addressnum; i++) {
                endnum = i + 16;
                if (endnum >= addressnum) {
                    endnum = addressnum;
                }
                String txt = address.substring(i, endnum);
                txts.add(txt);
                i = endnum;

            }
            for (int i = 0; i < txts.size(); i++) {
                String txt = txts.get(i);
                if (i != 0) {
                    line2y = line2y + txtaddress + paddingnum;
                }
                canvas.drawText(txt, baseLineX + paddingnum + dip2px(mReactContext, 30), line2y, p_address);
            }
        }
        //name
        if (!name.equals("")) {
            line2y = line2y + paddingnum;
            Resources res = mReactContext.getResources();
            Bitmap watermark = BitmapFactory.decodeResource(res, R.drawable.person);
            watermark = createnewWatermark(watermark);
            canvas.drawBitmap(watermark, baseLineX + paddingnum, line2y + dip2px(mReactContext, 10), null);

            line2y = line2y + txtaddress + paddingnum;
            canvas.drawText(name, baseLineX + paddingnum + dip2px(mReactContext, 30), line2y, p_address);
        }


        //水印竖线
        Paint p_line = new Paint();
        p_line.setAntiAlias(true);// 去锯齿
        p_line.setFakeBoldText(true);
        p_line.setColor(Color.WHITE);
        p_line.setStrokeWidth(6.0f);
        canvas.drawLine(baseLineX, baseLineX, baseLineX, line2y + paddingnum, p_line);

        canvas.save();
//        canvas.save(Canvas.ALL_SAVE_FLAG);
        canvas.restore();

        return bmp;
    }

    public Bitmap createnewWatermark(Bitmap b) {
        Bitmap bitMap = b;
        int width = bitMap.getWidth();
        int height = bitMap.getHeight();
        // 设置想要的大小
        int newWidth = (int) texts / 25;
        int newHeight = (int) texts / 25;
        // 计算缩放比例
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // 取得想要缩放的matrix参数
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        // 得到新的图片
        bitMap = Bitmap.createBitmap(bitMap, 0, 0, width, height, matrix, true);
        return bitMap;
    }

    private void setnewpath2(Bitmap newbitmap, String path) {
        File avaterFile = new File(path);//设置文件名称
        if (avaterFile.exists()) {
            avaterFile.delete();
        }
        try {
            avaterFile.createNewFile();
            FileOutputStream fos = new FileOutputStream(avaterFile);
            newbitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void setnewpath(ByteArrayInputStream bis, String path) {

        File avaterFile = new File(path);//设置文件名称

        if (avaterFile.exists()) {
            avaterFile.delete();
        }
        try {
            avaterFile.createNewFile();
            FileOutputStream fos = new FileOutputStream(avaterFile);


            byte[] buf = new byte[1024];
            long len = 0;
            long start = System.currentTimeMillis();
            while ((len = bis.read(buf, 0, buf.length)) != -1) {
                fos.write(buf);
            }
//            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void posturl(String path) {
        try {
            WritableArray resultArr = new WritableNativeArray();
            resultArr.pushMap(getAsyncSelection2(activity, path));
            mPickerPromise.resolve(resultArr);
        } catch (Exception e) {
        }


    }


    public void setWaterMark(String path) {
        try {

            if (path.startsWith("file://")) {
                path = path.substring(7, path.length());
            }

            FileInputStream fis = new FileInputStream(path);
            Bitmap bitmap = BitmapFactory.decodeStream(fis);

            if (bitmap == null) {
                return;
            }

            //水印----日期生成
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            Date curDate = new Date(System.currentTimeMillis());
            String str = formatter.format(curDate);
            //水印----日期生成 end

            //带水印Bitmap
            Bitmap newbitmap = createWatermark(bitmap, str);

            String path2 = com.mg.app.PickerModule.img_savepath.substring(0, com.mg.app.PickerModule.img_savepath.length() - 1);
            String p = path2 + "/" + System.currentTimeMillis() + ".jpg";
            FileOutputStream fisout = new FileOutputStream(p);
            newbitmap.compress(Bitmap.CompressFormat.JPEG, 70, fisout);
            new File(path).delete();
            imageUri = "file://" + p;
            //取代原照片
//            setnewpath2(newbitmap, path);


        } catch (Exception e) {
        }
    }

    public void getBitmap(String path) {

        try {
            if (path.startsWith("file://")) {
                path = path.substring(7, path.length());
                Log.d("aaa", "--p-" + path);
            }

//            if (is) {
//                posturl(path);
//                return;
//            }

            FileInputStream fis = new FileInputStream(path);
            Bitmap bitmap = BitmapFactory.decodeStream(fis);

            if (bitmap == null) {
                return;
            }
            //水印----日期生成
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy年MM月dd日   HH:mm:ss");
            Date curDate = new Date(System.currentTimeMillis());
            String str = formatter.format(curDate);
            //水印----日期生成 end

            //带水印Bitmap
            Bitmap newbitmap = createWatermark(bitmap, str);
            //质量处理
            ByteArrayInputStream isBm = compressImage(newbitmap);
            //取代原照片
            setnewpath(isBm, path);

            Log.d("aaa", path);
            posturl(path);

        } catch (Exception e) {
        }

    }


    public ByteArrayInputStream compressImage(Bitmap image) {
        Log.d("aaa", maxImageSize + "");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int options = 100;
        image.compress(Bitmap.CompressFormat.JPEG, options, baos);// 质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        while (baos.toByteArray().length / 1024 > maxImageSize) { // 循环判断如果压缩后图片是否大于100kb,大于继续压缩
            baos.reset(); // 重置baos即清空baos
            image.compress(Bitmap.CompressFormat.JPEG, options, baos);// 这里压缩options%，把压缩后的数据存放到baos中
            options -= 10;// 每次都减少10
        }
        Log.d("aaa", baos.toByteArray().length / 1024 + "----" + options);


        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());// 把压缩后的数据baos存放到ByteArrayInputStream中
//        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);// 把ByteArrayInputStream数据生成图片
        return isBm;
    }

    private void cropImageUri(Uri uri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("scale", false);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        intent.putExtra("return-data", false);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        intent.putExtra("noFaceDetection", true); // no face detection
        activity.startActivityForResult(intent, crop_REQUEST_CODE);
    }

//    @Override
//    public void getURL(String url) {
//        mPickerPromise.resolve(url);
//
//    }


    /**
     * init主题
     * end
     *
     * @return
     */
    private int uCropStatusColor;
    private int uCropToolbarColor;
    private int uCropActivityWidgetColor;
    private int uCropToolbarWidgetColor;
    private String uCropTitle;
    private String requestStorageAccessPermissionTips;

    public void setTheme() {
        uCropStatusColor = ThemeUtils.resolveColor(activity, cn.finalteam.rxgalleryfinal.R.attr.gallery_ucrop_status_bar_color, cn.finalteam.rxgalleryfinal.R.color.gallery_default_ucrop_color_widget_active);
        uCropToolbarColor = ThemeUtils.resolveColor(activity, cn.finalteam.rxgalleryfinal.R.attr.gallery_ucrop_toolbar_color, cn.finalteam.rxgalleryfinal.R.color.gallery_default_ucrop_color_widget_active);
        uCropActivityWidgetColor = ThemeUtils.resolveColor(activity, cn.finalteam.rxgalleryfinal.R.attr.gallery_ucrop_activity_widget_color, cn.finalteam.rxgalleryfinal.R.color.gallery_default_ucrop_color_widget);
        uCropToolbarWidgetColor = ThemeUtils.resolveColor(activity, cn.finalteam.rxgalleryfinal.R.attr.gallery_ucrop_toolbar_widget_color, cn.finalteam.rxgalleryfinal.R.color.gallery_default_toolbar_widget_color);
//        uCropTitle = ThemeUtils.resolveString(activity, cn.finalteam.rxgalleryfinal.R.attr.gallery_ucrop_toolbar_title, cn.finalteam.rxgalleryfinal.R.string.gallery_title_cut);
        uCropTitle="裁剪";
        int pageColor = ThemeUtils.resolveColor(activity, cn.finalteam.rxgalleryfinal.R.attr.gallery_page_bg, cn.finalteam.rxgalleryfinal.R.color.gallery_default_page_bg);
//        mRlRootView.setBackgroundColor(pageColor);
        requestStorageAccessPermissionTips = ThemeUtils.resolveString(activity, cn.finalteam.rxgalleryfinal.R.attr.gallery_request_camera_permission_tips, cn.finalteam.rxgalleryfinal.R.string.gallery_default_camera_access_permission_tips);
    }

    private void setPostMediaBean(MediaBean mediaBean) {
        ImageCropBean bean = new ImageCropBean();
        bean.copyMediaBean(mediaBean);
        RxBus.getDefault().post(new ImageRadioResultEvent(bean));
    }

    private final String IMAGE_STORE_FILE_NAME = "IMG_%s.jpg";
    private static File mCropPath = null;
    private static File mImageStoreDir;
    private static File mImageStoreCropDir; //裁剪目录

    /**
     * init裁剪目录
     *
     * @return
     */
    public static File getImageStoreDirByFile() {
        return mImageStoreDir;
    }

    /**
     * getImageStoreDir
     *
     * @return 存储路径
     */
    public static String getImageStoreDirByStr() {
        if (mImageStoreDir != null)
            return mImageStoreDir.getPath();
        else
            return null;
    }

    public static void setImageStoreCropDir(File imgFile) {
        mImageStoreCropDir = imgFile;
        Logger.i("设置图片裁剪保存路径为：" + mImageStoreCropDir.getAbsolutePath());
    }

    public static File getImageStoreCropDirByFile() {
        return mImageStoreCropDir;
    }

    public static String getImageStoreCropDirByStr() {
        if (mImageStoreCropDir != null)
            return mImageStoreCropDir.getPath();
        else
            return null;
    }

    public void onLoadFile() {
        //没有的话就默认路径
        if (getImageStoreDirByFile() == null && getImageStoreDirByStr() == null) {
            mImageStoreDir = new File(Environment.getExternalStorageDirectory(), "/DCIM/IMMQY/");
            setImageStoreCropDir(mImageStoreDir);
        }
        if (!mImageStoreDir.exists()) {
            mImageStoreDir.mkdirs();
        }
        if (getImageStoreCropDirByFile() == null && getImageStoreCropDirByStr() == null) {
            mImageStoreCropDir = new File(mImageStoreDir, "crop");
            if (!mImageStoreCropDir.exists()) {
                mImageStoreCropDir.mkdirs();
            }
            setImageStoreCropDir(mImageStoreCropDir);
        }
    }

    private void radioNext(MediaBean mediaBean) {
//        mConfiguration=rxGalleryFinal.getConfiguration();
        setTheme();

//        Uri inputUri = Uri.fromFile(new File(mediaBean.getOriginalPath()));
//        Intent intent = new Intent(activity, UCropActivity.class);
//
//
//        // UCrop 参数 start
//        Bundle bundle = new Bundle();
//
//        bundle.putParcelable(UCrop.EXTRA_OUTPUT_URI, outUri);
//        bundle.putParcelable(UCrop.Options.EXTRA_ASPECT_RATIO_OPTIONS, mediaBean);
//        bundle.putInt(UCrop.Options.EXTRA_STATUS_BAR_COLOR, uCropStatusColor);
//        bundle.putInt(UCrop.Options.EXTRA_TOOL_BAR_COLOR, uCropToolbarColor);
//        bundle.putString(UCrop.Options.EXTRA_UCROP_TITLE_TEXT_TOOLBAR, uCropTitle);
//        bundle.putInt(UCrop.Options.EXTRA_UCROP_COLOR_WIDGET_ACTIVE, uCropActivityWidgetColor);
//        bundle.putInt(UCrop.Options.EXTRA_UCROP_WIDGET_COLOR_TOOLBAR, uCropToolbarWidgetColor);
////            bundle.putn(UCrop.Options.EXTRA_FREE_STYLE_CROP, mConfiguration.isFreestyleCropEnabled());
//        bundle.putParcelable(UCrop.EXTRA_INPUT_URI, inputUri);
//        // UCrop 参数 end
//
//        int bk = FileUtils.existImageDir(inputUri.getPath());
//        Logger.i("--->" + inputUri.getPath());
//        Logger.i("--->" + outUri.getPath());
//        ArrayList<AspectRatio> aspectRatioList = new ArrayList<>();
////            Aspo[]aspectRatios =  mConfiguration.getAspectRatio();
//        bundle.putParcelableArrayList(UCrop.Options.EXTRA_ASPECT_RATIO_OPTIONS, aspectRatioList);//EXTRA_CONFIGURATION
//        intent.putExtras(bundle);
//        if (bk != -1) {
//            //裁剪
//            activity.startActivity(intent);
////                startActivityForResult(intent, CROP_IMAGE_REQUEST_CODE);
//        } else {
//            Logger.w("点击图片无效");
//        }
//        }
    }

    Activity activity;

    @ReactMethod
    public void openCamera(final ReadableMap options, final Promise promise) {

        activity = getCurrentActivity();
        Log.d("aaa", activity.getLocalClassName() + "---");

        if (activity == null) {
            promise.reject(E_ACTIVITY_DOES_NOT_EXIST, "Activity doesn't exist");
            return;
        }

        setConfiguration(options);
//        initImageLoader(activity);
        mPickerPromise = promise;

        openCamera(isWaterMark);
    }

    @ReactMethod
    public void openPicker(final ReadableMap options, final Promise promise) {
        activity = getCurrentActivity();

        if (activity == null) {
            promise.reject(E_ACTIVITY_DOES_NOT_EXIST, "Activity doesn't exist");
            return;
        }

        File f = new File(img_savepath);
        if (!f.exists()) {
            f.mkdirs();
        }

        setConfiguration(options);
        initImageLoader(activity);
        mPickerPromise = promise;

        RxGalleryFinal rxGalleryFinal = RxGalleryFinal.with(activity);
//        if (openCameraOnStart) {
//            rxGalleryFinal.openCameraOnStart();
//        } else
        if (!isCamera) {
            rxGalleryFinal.hideCamera();
        }
        if (compressQuality > 0) {
            rxGalleryFinal.cropropCompressionQuality(compressQuality);
        }
//        if (title != null) {
//            rxGalleryFinal.setTitle(title);
//        }
//        if (returnAfterShot) {
//            rxGalleryFinal.returnAfterShot();
//        }
        if (isVideo) {
            rxGalleryFinal.video();
        } else {
            rxGalleryFinal.image();
        }
        if (isHidePreview) {
            rxGalleryFinal.hidePreview();
        }
        if (isHideVideoPreview) {
            rxGalleryFinal.videoPreview();
        }
        if (isPlayGif) {
            rxGalleryFinal.gif();
        }
        if (imageLoader != null) {
            switch (imageLoader) {
                case "PICASSO":
                    rxGalleryFinal.imageLoader(ImageLoaderType.PICASSO);
                    break;
                case "GLIDE":
                    rxGalleryFinal.imageLoader(ImageLoaderType.GLIDE);
                    break;
                case "FRESCO":
                    rxGalleryFinal.imageLoader(ImageLoaderType.FRESCO);
                    break;
                case "UNIVERSAL":
                    rxGalleryFinal.imageLoader(ImageLoaderType.UNIVERSAL);
                    break;
                default:
                    break;
            }
        } else {
            rxGalleryFinal.imageLoader(ImageLoaderType.GLIDE);
        }
//        if (openCameraOnStart) {
//            //拍照处理,直接跳去原生拍照
//            openCamera();
//            return;
//        }
        activityhide();
        if (!this.multiple) {
            if (cropping) {
                rxGalleryFinal.crop();
                rxGalleryFinal.cropMaxResultSize(this.width, this.height);
                //裁剪图片的回调
                RxGalleryListener
                        .getInstance()
                        .setRadioImageCheckedListener(
                                new IRadioImageCheckedListener() {
                                    @Override
                                    public void cropAfter(Object t) {
                                        WritableArray resultArr = new WritableNativeArray();
                                        try {
                                            resultArr.pushMap(getAsyncSelection(activity, t.toString()));
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                        mPickerPromise.resolve(resultArr);
                                    }

                                    @Override
                                    public boolean isActivityFinish() {
                                        return true;
                                    }
                                });
            }
            rxGalleryFinal
                    .radio()
                    .subscribe(new RxBusResultDisposable<ImageRadioResultEvent>() {
                        @Override
                        protected void onEvent(ImageRadioResultEvent imageRadioResultEvent) throws Exception {
                            if (!cropping) {

                                ImageCropBean result = imageRadioResultEvent.getResult();
                                WritableArray resultArr = new WritableNativeArray();
                                resultArr.pushMap(getAsyncSelection(activity, result));
                                mPickerPromise.resolve(resultArr);
                            }
                        }
                    })
                    .openGallery();
        } else {
            rxGalleryFinal
                    .multiple()
                    .maxSize(maxSize)
                    .subscribe(new RxBusResultDisposable<ImageMultipleResultEvent>() {
                        @Override
                        protected void onEvent(ImageMultipleResultEvent imageMultipleResultEvent) throws Exception {
                            List<MediaBean> list = imageMultipleResultEvent.getResult();
                            WritableArray resultArr = new WritableNativeArray();
                            for (MediaBean bean : list) {
                                resultArr.pushMap(getAsyncSelection(activity, bean));
                            }
                            mPickerPromise.resolve(resultArr);
                        }

                        @Override
                        public void onComplete() {
                            super.onComplete();
                        }
                    })
                    .openGallery();
        }
    }

    private String getBase64StringFromFile(String absoluteFilePath) {
        InputStream inputStream;

        try {
            inputStream = new FileInputStream(new File(absoluteFilePath));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        byte[] bytes;
        byte[] buffer = new byte[8192];
        int bytesRead;
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        bytes = output.toByteArray();
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }


    private BitmapFactory.Options validateImage(String path) throws Exception {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        options.inDither = true;

        BitmapFactory.decodeFile(path, options);

        if (options.outMimeType == null || options.outWidth == 0 || options.outHeight == 0) {
            throw new Exception("Invalid image selected");
        }

        return options;
    }

    private Bitmap validateVideo(String path) throws Exception {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(path);
        Bitmap bmp = retriever.getFrameAtTime();

        if (bmp == null) {
            throw new Exception("Cannot retrieve video data");
        }

        return bmp;
    }

    @Override
    public void onNewIntent(Intent intent) {

    }

    public void setmaxImageSize(String path) {
        if (path.startsWith("file://")) {
            path = path.substring(7, path.length());
        }
        posturl(path);
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {

        try {
            if (requestCode == TAKE_IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
                String path = imageUri;
                //水印处理
                if (isWaterMark) {
                    setWaterMark(path);
                    path = imageUri;
                }
                //裁剪处理
                if (cropping) {
                    crop2(path);
                    return;
                }
                //add by david 手机拍照后自动保存照片到系统相册  start
                if(isSaveAlbum){
                    saveCameraImage(path);
                }
                //add by david 手机拍照后自动保存照片到系统相册  end
                //压缩处理
                setmaxImageSize(path);
                return;
            }

            if (requestCode == crop_REQUEST_CODE) {

                String path = imageUri;
                if (path.startsWith("file://")) {
                    path = path.substring(7, path.length());
                }


                File avaterFile = new File(path);//设置文件名称
                if (avaterFile.exists()) {
                    avaterFile.delete();
                }

                //压缩处理
                setmaxImageSize(OUTUri.toString());
                return;
            }
        } catch (Exception e) {

        }
    }


    /**
     * 拍照后保存到图册
     *
     * @param path
     */

    public void saveCameraImage(final String path) {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return;
        }

        try {

            File appDir = new File(Environment.getExternalStorageDirectory(), "mogu");
            if (!appDir.exists()) {
                appDir.mkdir();
            }

            String fileName = System.currentTimeMillis() + ".jpg";
            File file = new File(appDir, fileName);
            FileOutputStream fos = new FileOutputStream(file);

//            String[] arr = path.split("file://");
            Bitmap bmp = BitmapFactory.decodeFile(path);//filePath
            if (bmp != null) {
                bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.flush();
                fos.close();
                getCurrentActivity().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + file.getAbsolutePath())));
            }
        } catch (Exception e) {

        }
    }


    Uri OUTUri;

    public void crop2(String path) {
        if (path == null) {
            return;
        }
        if (!path.startsWith("file://")) {
            path = "file://" + path;
        }
        setTheme();
//
        Uri inputUri = Uri.parse(path);
        OUTUri = Uri.parse("file://" + img_savepath + System.currentTimeMillis()
                + ".jpg");

        Intent intent = new Intent(activity, UCropActivity.class);
        Bundle bundle = new Bundle();


        bundle.putParcelable(UCrop.EXTRA_OUTPUT_URI, OUTUri);
        bundle.putParcelable(UCrop.EXTRA_INPUT_URI, inputUri);

        bundle.putInt(UCrop.Options.EXTRA_STATUS_BAR_COLOR, uCropStatusColor);
        bundle.putInt(UCrop.Options.EXTRA_TOOL_BAR_COLOR, uCropToolbarColor);
        bundle.putString(UCrop.Options.EXTRA_UCROP_TITLE_TEXT_TOOLBAR, uCropTitle);
        bundle.putInt(UCrop.Options.EXTRA_UCROP_COLOR_WIDGET_ACTIVE, uCropActivityWidgetColor);
        bundle.putInt(UCrop.Options.EXTRA_UCROP_WIDGET_COLOR_TOOLBAR, uCropToolbarWidgetColor);
        // UCrop 参数 end

        int bk = FileUtils.existImageDir(inputUri.getPath());
//        ArrayList<AspectRatio> aspectRatioList = new ArrayList<>();
//        bundle.putParcelableArrayList(UCrop.Options.EXTRA_ASPECT_RATIO_OPTIONS, aspectRatioList);//EXTRA_CONFIGURATION
        intent.putExtras(bundle);
        if (bk != -1) {
            //裁剪
            activity.startActivityForResult(intent, crop_REQUEST_CODE);
        } else {
            Logger.w("点击图片无效");
        }
    }


}