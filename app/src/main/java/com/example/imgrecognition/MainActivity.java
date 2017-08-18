package com.example.imgrecognition;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private String[] mNames = {"万磁王", "恩佐斯", "加拉克苏斯大王", "死亡之翼", "伊兰尼库斯"};
    private int[] mImgs = {R.mipmap.a, R.mipmap.b, R.mipmap.c, R.mipmap.d, R.mipmap.e};
    private final static int IMG_RECOGNITION = 0;
    private ImageView iv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        iv = (ImageView) findViewById(R.id.iv);
        findViewById(R.id.bt).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ImgRecognitionActivity.class);
                startActivityForResult(intent, IMG_RECOGNITION);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == IMG_RECOGNITION) {
            int data2 = data.getIntExtra("data", -1);
            Toast.makeText(this, "看到的是" + mNames[data2], Toast.LENGTH_LONG).show();
            iv.setImageResource(mImgs[data2]);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
