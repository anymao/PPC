package top.anymore.ppc.activity;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import top.anymore.ppc.R;
import top.anymore.ppc.logutil.LogUtil;

public class MainActivity extends AppCompatActivity {
    private static final String tag = "MainActivity";
    private static final int ACTION_TACK_PHOTO = 1;
    private static final int ACTION_FROM_ALBUM = 2;
    private static final int ACTION_CROP_PHOTO = 3;
    private Button btnSelectPic,btnStartDetect;
    private ImageView ivPic;
    private Dialog selectPicDialog;
    private Uri imageUri;//获取的照片保存路径
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_main);
        btnSelectPic = (Button) findViewById(R.id.btn_select_pic);
        btnStartDetect = (Button) findViewById(R.id.btn_start_detect);
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
    }

    private View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.btn_select_pic:
                    selectPicDialog.show();
                    break;
                case R.id.btn_take_photo:
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
            }
        }
    };

    private void openAlbum() {
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        startActivityForResult(intent,ACTION_FROM_ALBUM);
    }

    private void takePhoto() {
        File outputImage = new File(getExternalCacheDir(),"output_image.jpg");
        try {
            if (outputImage.exists()){
                outputImage.delete();
            }
            outputImage.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (Build.VERSION.SDK_INT >= 24){
            imageUri = FileProvider.getUriForFile(MainActivity.this,"top.anymore.ppc.fileprovider",outputImage);
        }else {
            imageUri = Uri.fromFile(outputImage);
        }
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
        startActivityForResult(intent,ACTION_TACK_PHOTO);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    openAlbum();
                }else {
                    Toast.makeText(MainActivity.this,"您取消了授权,无法访问相册",Toast.LENGTH_SHORT).show();
                }
                break;
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

    private void cropPhoto(){
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(imageUri,"image/*");
        intent.putExtra("scale",true);
        intent.putExtra("return-data",false);
//        intent.putExtra("crop",true);
        intent.putExtra("aspectX",1);
        intent.putExtra("aspectY",1);
//        intent.putExtra("outputX",60);
//        intent.putExtra("outputY",60);
        intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
        startActivityForResult(intent,ACTION_CROP_PHOTO);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        selectPicDialog.dismiss();
        switch (requestCode){
            case ACTION_TACK_PHOTO:
                cropPhoto();
                break;
            case ACTION_FROM_ALBUM:
                if (resultCode == RESULT_OK){
                    if (Build.VERSION.SDK_INT >= 19){
                        handleImageOnKitKat(data);
                    }else {
                        handleImageBeforeKitKat(data);
                    }
                }
                cropPhoto();
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
}
