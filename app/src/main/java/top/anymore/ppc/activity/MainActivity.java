package top.anymore.ppc.activity;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import top.anymore.ppc.R;
import top.anymore.ppc.dataprocess.DataProcess;
import top.anymore.ppc.logutil.LogUtil;

public class MainActivity extends AppCompatActivity {
    private static final String tag = "MainActivity";
    private static final int ACTION_TACK_PHOTO = 1;
    private static final int ACTION_FROM_ALBUM = 2;
    private static final int ACTION_CROP_PHOTO = 3;
    private Button btnSelectPic,btnStartDetect;
    private ImageView ivPic;
    private TextView tvResult;
    private Dialog selectPicDialog;
    private Uri imageUri;//获取的照片保存路径
    private ProgressDialog mProgressDialog;

    //加载opencv的回调监听
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case BaseLoaderCallback.SUCCESS:
                    LogUtil.i(tag, "成功加载");
                    break;
                default:
                    super.onManagerConnected(status);
                    LogUtil.i(tag, "加载失败");
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initViews();//初始化布局
    }

    private void initViews() {
        setContentView(R.layout.activity_main);
        btnSelectPic = (Button) findViewById(R.id.btn_select_pic);
        btnStartDetect = (Button) findViewById(R.id.btn_start_detect);
        tvResult = (TextView) findViewById(R.id.tv_result);
        ivPic = (ImageView) findViewById(R.id.iv_pic);
        selectPicDialog = new Dialog(MainActivity.this,R.style.ActionSheetDialogStyle);
        View selectDiagView = LayoutInflater.from(MainActivity.this).inflate(R.layout.diaglog_select_pic,null,false);
        selectDiagView.findViewById(R.id.btn_take_photo).setOnClickListener(listener);
        selectDiagView.findViewById(R.id.btn_album).setOnClickListener(listener);
        selectDiagView.findViewById(R.id.btn_cancel_diag).setOnClickListener(listener);
        Window diagWindow = selectPicDialog.getWindow();
        diagWindow.setGravity(Gravity.BOTTOM);
        diagWindow.setContentView(selectDiagView);
        diagWindow.setLayout(WindowManager.LayoutParams.MATCH_PARENT
                ,WindowManager.LayoutParams.WRAP_CONTENT);
        selectPicDialog.setContentView(selectDiagView);
        selectPicDialog.setCanceledOnTouchOutside(true);
        selectPicDialog.setCancelable(true);
        btnStartDetect.setOnClickListener(listener);
        btnSelectPic.setOnClickListener(listener);
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("请稍后....");
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setCancelable(false);
    }

    private View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.btn_select_pic:
                    selectPicDialog.show();
                    break;
                case R.id.btn_take_photo:
                    //运行时权限处理
                    if (Build.VERSION.SDK_INT > 22){
                        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.CAMERA},2);
                        }else {
                            takePhoto();
                        }
                    }else{
                        takePhoto();
                    }
                    break;
                case R.id.btn_album:
                    //运行时权限处理
                    if (Build.VERSION.SDK_INT > 22){
                        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
                        }else {
                            openAlbum();
                        }
                    }else{
                        openAlbum();
                    }
                    break;
                case R.id.btn_cancel_diag:
                    selectPicDialog.dismiss();
                    break;
                case R.id.btn_start_detect:
                    new ImageProcessTask().execute();
                    break;
            }
        }
    };

    /**
     * 使用系统自带应用程序打开相册
     */
    private void openAlbum() {
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        startActivityForResult(intent,ACTION_FROM_ALBUM);
    }

    /**
     * 使用系统自带应用程序打开相机拍照
     */
    private void takePhoto() {
        //拍照后得到的照片存储位置
        File outputImage = new File(getExternalCacheDir(),"output_image.jpg");
        try {
            if (outputImage.exists()){
                outputImage.delete();
            }
            outputImage.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //通过文件获取其URI
        if (Build.VERSION.SDK_INT >= 24){
            imageUri = FileProvider.getUriForFile(MainActivity.this,"top.anymore.ppc.fileprovider",outputImage);
        }else {
            imageUri = Uri.fromFile(outputImage);
        }
        //调用系统相机
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
        startActivityForResult(intent,ACTION_TACK_PHOTO);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            //授权访问相册
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    openAlbum();
                }else {
                    Toast.makeText(MainActivity.this,"您取消了授权,无法访问相册",Toast.LENGTH_SHORT).show();
                }
                break;
            //授权打开相机
            case 2:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    takePhoto();
                }else {
                    Toast.makeText(MainActivity.this,"您取消了授权,无法打开摄像头",Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }

    /**
     * 将来自相机或者是相册的照片进行剪切处理，
     * 使用系统的裁剪程序
     */
    private void cropPhoto(){
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(imageUri,"image/*");
        intent.putExtra("scale",true);//允许缩放
        intent.putExtra("return-data",false);//不将结果在intent中返回，因为这将返回缩略图
        intent.putExtra("crop",true);//允许裁剪
//        intent.putExtra("aspectX",1);//比例
//        intent.putExtra("aspectY",1);
//        intent.putExtra("outputX",60);//固定大小
//        intent.putExtra("outputY",60);
        intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);//裁剪结果保存位置
        startActivityForResult(intent,ACTION_CROP_PHOTO);//跳转
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        selectPicDialog.dismiss();
        switch (requestCode){
            case ACTION_TACK_PHOTO:
                if (resultCode == RESULT_OK){
                    cropPhoto();
                }
                break;
            case ACTION_FROM_ALBUM:
                if (resultCode == RESULT_OK){
                    if (Build.VERSION.SDK_INT >= 19){
                        handleImageOnKitKat(data);
                    }else {
                        handleImageBeforeKitKat(data);
                    }
                    cropPhoto();
                }
                break;
            case ACTION_CROP_PHOTO:
                if (resultCode == RESULT_OK){
                    try {
                        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                        ivPic.setImageBitmap(bitmap);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }

    private void handleImageBeforeKitKat(Intent data) {
        Uri uri = data.getData();
        String imagePath = getImagePath(uri,null);
        if (imagePath != null){
            imageUri = Uri.fromFile(new File(imagePath));
        }
    }
    @TargetApi(19)
    private void handleImageOnKitKat(Intent data) {
        String imagePath = null;
        Uri uri = data.getData();
        if (DocumentsContract.isDocumentUri(MainActivity.this,uri)){
            //如果是document类型的Uri，则通过document id处理
            String docId = DocumentsContract.getDocumentId(uri);
            if ("com.android.providers.media.documents".equals(uri.getAuthority())){
                String id = docId.split(":")[1];
                String selection = MediaStore.Images.Media._ID+"="+id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,selection);
            }else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())){
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),Long.valueOf(docId));
                imagePath = getImagePath(contentUri,null);
            }
        }else if ("content".equalsIgnoreCase(uri.getScheme())){
            //如果是content类型的Uri则使用普通方式
            imagePath = getImagePath(uri,null);
        }else if ("file".equalsIgnoreCase(uri.getScheme())){
            //如果是file类型的Uri,则直接获取路径
            imagePath = uri.getPath();
        }
        if (imagePath != null){
            imageUri = Uri.fromFile(new File(imagePath));
        }
    }

    private String getImagePath(Uri uri, String selection) {
        String path = null;
        Cursor cursor = getContentResolver().query(uri,null,selection,null,null);
        if (cursor != null){
            if (cursor.moveToFirst()){
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

    @Override
    public void onResume()
    {
        super.onResume();
//        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        if (!OpenCVLoader.initDebug()) {
            LogUtil.d(tag, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_11, this, mLoaderCallback);
        } else {
            LogUtil.d(tag, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }
    private Bitmap srcBitmap,blurSrcBitmap,grayBitmap,binaryBitmap,scaleBinaryBitmap;//存储原图,灰度图，二值图的位图对象
    private int result;
    /**
     * 采用异步处理，这里将图片二值化
     */
    private class ImageProcessTask extends AsyncTask<Void,Void,Void>{
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            File grayImage = new File(getExternalCacheDir(),"grayImage.jpg");//灰度图存储位置
            File binaryImage = new File(getExternalCacheDir(),"binaryImage.jpg");//二值图存储位置
            Mat srcMat = new Mat();
            Mat blurSrcMat = new Mat();
            Mat grayMat = new Mat();
            Mat binaryMat = new Mat();
            int blockSize = 25;
            int constValue = 10;
            double scale = 0.5;
            try {
                srcBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                grayBitmap = Bitmap.createBitmap(srcBitmap.getWidth(), srcBitmap.getHeight(), Bitmap.Config.RGB_565);
                binaryBitmap = Bitmap.createBitmap(srcBitmap.getWidth(),srcBitmap.getHeight(),Bitmap.Config.RGB_565);
//                scale = Bitmap.createBitmap(srcBitmap.getWidth()*scale,s)
//                scaleBinaryBitmap = Bitmap.createBitmap()
                Utils.bitmapToMat(srcBitmap, srcMat);//convert original bitmap to Mat, R G B.
//                Imgproc.blur(srcMat,blurSrcMat,new Size(3,3));//均值模糊处理
                Imgproc.cvtColor(srcMat, grayMat, Imgproc.COLOR_RGB2GRAY);//rgbMat to gray grayMat
                Imgproc.adaptiveThreshold(grayMat,binaryMat,255,Imgproc.ADAPTIVE_THRESH_MEAN_C,Imgproc.THRESH_BINARY_INV,blockSize,constValue);
                Utils.matToBitmap(grayMat, grayBitmap); //convert mat to bitmap
                Utils.matToBitmap(binaryMat,binaryBitmap);
                LogUtil.i(tag, "procSrc2Gray sucess...");
                //test
                LogUtil.v(tag,"first:"+binaryMat.rows()+"-"+binaryMat.cols());

                Size size=new Size(binaryMat.width()*scale,binaryMat.height()*scale);
                Mat scaleBinaryMat = new Mat(size,CvType.CV_8UC1);
                Imgproc.resize(binaryMat,scaleBinaryMat,size);
//                Utils.matToBitmap(scaleBinaryMat,scaleBinaryBitmap);
//                LogUtil.v(tag,"second:"+scaleBinaryMat.rows()+"-"+scaleBinaryMat.cols());
                Highgui.imwrite(getExternalCacheDir()+"/scale.jpg",scaleBinaryMat);
                DataProcess dataProcess = new DataProcess();
                dataProcess.setMatrix(scaleBinaryMat);
                result = dataProcess.solve();

                //test

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            saveBitmap2File(grayBitmap,grayImage);
            saveBitmap2File(binaryBitmap,binaryImage);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mProgressDialog.dismiss();
            ivPic.setImageBitmap(binaryBitmap);
            tvResult.setText("分析结果是:"+result);
        }
    }
    private void saveBitmap2File(Bitmap bitmap,File dstFile){
        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(dstFile));
            bitmap.compress(Bitmap.CompressFormat.JPEG,80,bos);
            bos.flush();
            bos.close();
            LogUtil.v(tag,dstFile.getAbsolutePath().toString()+"保存成功!");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
