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

import java.io.IOException;
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

        try {
            mtcnn = new MTCNN(getAssets());
            mfn = new MobileFaceNet(getAssets());
        } catch (IOException e) {
            e.printStackTrace();
        }

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
        bitmap1 = MyUtil.readFromAssets(this, "zhouwenpeng1.jpg");
        bitmap2 = MyUtil.readFromAssets(this, "zhouwenpeng2.jpg");
        imageView1.setImageBitmap(bitmap1);
        imageView2.setImageBitmap(bitmap2);
    }

    /**
     * 人脸检测并裁减
     */
    private void faceCrop() {
        bitmap1 = Bitmap.createScaledBitmap(bitmap1, MTCNN.IMAGE_WIDTH, MTCNN.IMAGE_HEIGHT, true);
        bitmap2 = Bitmap.createScaledBitmap(bitmap2, MTCNN.IMAGE_WIDTH, MTCNN.IMAGE_HEIGHT, true);

        // 检测出人脸数据
        long start = System.currentTimeMillis();
        Vector<Box> boxes1 = mtcnn.detectFaces(bitmap1, MTCNN.MIN_SIZE);
        long end = System.currentTimeMillis();
        resultTextView.setText("人脸检测前向传播耗时：" + (end - start));

        Vector<Box> boxes2 = mtcnn.detectFaces(bitmap2, MTCNN.MIN_SIZE);
        if (boxes1.size() == 0 || boxes2.size() == 0) {
            Toast.makeText(MainActivity.this, "未检测到人脸", Toast.LENGTH_LONG).show();
            return;
        }
        // 这里因为使用的每张照片里只有一张人脸，所以取第一个值，用来剪裁人脸
        Box box1 = boxes1.get(0);
        Box box2 = boxes2.get(0);

        // 人脸对齐
//        bitmap1 = Align.warpAffine(bitmap1, box1.landmark);
//        bitmap2 = Align.warpAffine(bitmap2, box2.landmark);
//
//        // 重新检测对齐后的人脸数据
//        boxes1 = mtcnn.detectFaces(bitmap1, MIN_SIZE);
//        boxes2 = mtcnn.detectFaces(bitmap2, MIN_SIZE);
//        if (boxes1.size() == 0 || boxes2.size() == 0) {
//            Toast.makeText(MainActivity.this, "未检测到人脸", Toast.LENGTH_LONG).show();
//            return;
//        }
//        box1 = boxes1.get(0);
//        box2 = boxes2.get(0);

        // 剪裁人脸
        bitmap1 = crop(bitmap1, box1);
        bitmap2 = crop(bitmap2, box2);

        imageViewCrop1.setImageBitmap(bitmap1);
        imageViewCrop2.setImageBitmap(bitmap2);
    }

    private Bitmap crop(Bitmap bitmap, Box box) {
        Rect rect = box.transform2Rect();

        // 将rect扩充44像素，把整个头包进来（估计是这个意思吧，因为这个MTCNN是人脸五点检测，眼睛，鼻子，嘴两边）
        // 这里根据比例重新计算了扩充的像素
        int margin = 20;
        int marginX = Math.round(((float) (rect.right - rect.left)) / MobileFaceNet.INPUT_IMAGE_SIZE * margin);
        int marginY = Math.round(((float) (rect.bottom - rect.top)) / MobileFaceNet.INPUT_IMAGE_SIZE * margin);
        MyUtil.rectExtend(bitmap, rect, marginX, marginY);

        // 裁剪出人脸
        return Utils.crop(bitmap, rect);
    }

    /**
     * 人脸比对
     */
    private void faceCompare() {
        long start = System.currentTimeMillis();
        float same = mfn.compare(bitmap1, bitmap2);
        long end = System.currentTimeMillis();

        String text = "比对结果：" + same;
        if (same > MobileFaceNet.THRESHOLD) {
            text = text + "，" + "True";
            resultTextView.setTextColor(getResources().getColor(android.R.color.holo_green_light));
        } else {
            text = text + "，" + "False";
            resultTextView.setTextColor(getResources().getColor(android.R.color.holo_red_light));
        }
        text = text + "，耗时" + (end - start);
        resultTextView.setText(text);
    }
}
