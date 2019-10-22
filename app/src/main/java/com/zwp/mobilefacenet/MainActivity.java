package com.zwp.mobilefacenet;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.zwp.mobilefacenet.mobilefacenet.MobileFaceNet;
import com.zwp.mobilefacenet.mtcnn.Box;
import com.zwp.mobilefacenet.mtcnn.MTCNN;
import com.zwp.mobilefacenet.mtcnn.Utils;

import java.util.Vector;

public class MainActivity extends AppCompatActivity {
    private MTCNN mtcnn; // 人脸检测
    private MobileFaceNet mfn; // 人脸比对

    private Bitmap bitmap1;
    private Bitmap bitmap2;

    private ImageView imageView1;
    private ImageView imageView2;
    private ImageView imageViewCrop1;
    private ImageView imageViewCrop2;
    private TextView resultTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView1 = findViewById(R.id.image_view1);
        imageView2 = findViewById(R.id.image_view2);
        imageViewCrop1 = findViewById(R.id.image_view_crop1);
        imageViewCrop2 = findViewById(R.id.image_view_crop2);
        Button cropBtn = findViewById(R.id.crop_btn);
        Button compareBtn = findViewById(R.id.compare_btn);
        resultTextView = findViewById(R.id.result_text_view);

        mtcnn = new MTCNN(getAssets());
        mfn = new MobileFaceNet(getAssets());

        initImage();
        cropBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                faceCrop();
            }
        });
        compareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                faceCompare();
            }
        });
    }

    private void initImage() {
        bitmap1 = MyUtil.readFromAssets(this, "trump1.jpg");
        bitmap2 = MyUtil.readFromAssets(this, "trump2.jpg");
        imageView1.setImageBitmap(bitmap1);
        imageView2.setImageBitmap(bitmap2);
    }

    /**
     * 人脸检测并裁减
     */
    private void faceCrop() {
        // 检测出人脸数据
        Vector<Box> boxes1 = mtcnn.detectFaces(bitmap1, 40);
        Vector<Box> boxes2 = mtcnn.detectFaces(bitmap2, 40);

        if (boxes1.size() == 0 || boxes2.size() == 0) {
            Toast.makeText(MainActivity.this, "未检测到人脸", Toast.LENGTH_LONG).show();
            return;
        }

        // 这里因为使用的每张照片里只有一张人脸，所以取第一个值，用来剪裁人脸
        Rect rect1 = boxes1.get(0).transform2Rect();
        Rect rect2 = boxes2.get(0).transform2Rect();

        // 扩充24像素，把头发耳朵下巴啥的包括进来（估计是这个意思吧，因为这个MTCNN是人脸五点检测，眼睛，鼻子，嘴两边）
        int margin = 24;
        Utils.rectExtend(bitmap1, rect1, margin);
        Utils.rectExtend(bitmap2, rect2, margin);

        // 裁剪出人脸
        bitmap1 = Utils.crop(bitmap1, rect1);
        bitmap2 = Utils.crop(bitmap2, rect2);

        imageViewCrop1.setImageBitmap(bitmap1);
        imageViewCrop2.setImageBitmap(bitmap2);
    }

    /**
     * 人脸比对
     */
    private void faceCompare() {
        float same = mfn.compare(bitmap1, bitmap2);
        String text = "比对结果：" + String.valueOf(same);
        if (same > MobileFaceNet.THRESHOLD) {
            text = text + "，" + "True";
            resultTextView.setText(text);
            resultTextView.setTextColor(getResources().getColor(android.R.color.holo_green_light));
        } else {
            text = text + "，" + "False";
            resultTextView.setText(text);
            resultTextView.setTextColor(getResources().getColor(android.R.color.holo_red_light));
        }
    }
}
