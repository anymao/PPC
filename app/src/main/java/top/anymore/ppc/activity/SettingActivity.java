package top.anymore.ppc.activity;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;

import top.anymore.ppc.R;
import top.anymore.ppc.dataprocess.CacheValue;
import top.anymore.ppc.logutil.LogUtil;

public class SettingActivity extends AppCompatActivity implements View.OnClickListener {
    private EditText etThreshold;
    private Button btnSetting,btnPre;
    private ImageView ivPreview;
    private int threshold;
    private ProgressDialog mProgressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        initView();
        generate();
    }

    private void generate() {
        threshold = Integer.parseInt(etThreshold.getText().toString());
        if (threshold < 0){
            Toast.makeText(SettingActivity.this,"设置像素块不能为负,请重新设置!",Toast.LENGTH_LONG).show();
            return;
        }
        new GeneratePreviewTask().execute(threshold);
    }

    private void initView() {
        etThreshold = (EditText) findViewById(R.id.et_threshold);
        btnPre = (Button) findViewById(R.id.btn_pre);
        btnSetting = (Button) findViewById(R.id.btn_setting);
        ivPreview = (ImageView) findViewById(R.id.iv_preview);
        etThreshold.setText(CacheValue.threshold+"");
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle("请稍后");
        mProgressDialog.setMessage("正在生成预览图块...");
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setCancelable(false);
        btnPre.setOnClickListener(this);
        btnSetting.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_pre:
                generate();
                break;
            case R.id.btn_setting:
                settingThreshold();
                break;
        }
    }

    private void settingThreshold() {
        threshold = Integer.parseInt(etThreshold.getText().toString());
        if (threshold < 0){
            Toast.makeText(SettingActivity.this,"设置像素块不能为负,请重新设置!",Toast.LENGTH_LONG).show();
            return;
        }
        SharedPreferences.Editor editor = getSharedPreferences("PPC_SP",MODE_PRIVATE).edit();
        editor.putInt("threshold",threshold);
        editor.commit();
        CacheValue.threshold = this.threshold;
        Toast.makeText(SettingActivity.this,"设置成功..",Toast.LENGTH_LONG).show();

    }

    private class GeneratePreviewTask extends AsyncTask<Integer,Void,Bitmap>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog.show();
        }



        @Override
        protected Bitmap doInBackground(Integer... integers) {
            long a = Math.round(Math.sqrt(integers[0]));
            LogUtil.v("lym",integers[0]+":"+a);
            Size size = new Size(a,a);
            Mat preMat = new Mat(size, CvType.CV_8UC1);
            int count = 0;
            for (int i = 0; i < a; i++) {
                for (int j = 0; j < a; j++) {
                    if (count++<integers[0]){
                        preMat.put(i,j,0);
                    }else {
                        preMat.put(i,j,255);
                    }
                }
            }
            Bitmap bitmap = Bitmap.createBitmap(preMat.width(),preMat.height(), Bitmap.Config.RGB_565);
            Utils.matToBitmap(preMat,bitmap);
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            if (mProgressDialog.isShowing()){
                mProgressDialog.dismiss();
            }
            ivPreview.setImageBitmap(bitmap);
        }
    }
}
