package com.zwp.mobilefacenet;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.zwp.mobilefacenet.faceantispoofing.FaceAntiSpoofing;
import com.zwp.mobilefacenet.mobilefacenet.MobileFaceNet;
import com.zwp.mobilefacenet.mtcnn.Align;
import com.zwp.mobilefacenet.mtcnn.Box;
import com.zwp.mobilefacenet.mtcnn.MTCNN;

import java.io.IOException;
import java.util.Vector;

public class MainActivity extends AppCompatActivity {

    private MTCNN mtcnn; // 人脸检测
    private FaceAntiSpoofing fas; // 活体检测
    private MobileFaceNet mfn; // 人脸比对

    public static Bitmap bitmap1;
    public static Bitmap bitmap2;
    private Bitmap bitmapCrop1;
    private Bitmap bitmapCrop2;

    private ImageButton imageButton1;
    private ImageButton imageButton2;
    private ImageView imageViewCrop1;
    private ImageView imageViewCrop2;
    private TextView resultTextView;
    private TextView resultTextView2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageButton1 = findViewById(R.id.image_button1);
        imageButton2 = findViewById(R.id.image_button2);
        imageViewCrop1 = findViewById(R.id.image_view_crop1);
        imageViewCrop2 = findViewById(R.id.image_view_crop2);
        Button cropBtn = findViewById(R.id.crop_btn);
        Button deSpoofingBtn = findViewById(R.id.de_spoofing_btn);
        Button compareBtn = findViewById(R.id.compare_btn);
        resultTextView = findViewById(R.id.result_text_view);
        resultTextView2 = findViewById(R.id.result_text_view2);

        try {
            mtcnn = new MTCNN(getAssets());
            fas = new FaceAntiSpoofing(getAssets());
            mfn = new MobileFaceNet(getAssets());
        } catch (IOException e) {
            e.printStackTrace();
        }

        initCamera();
        cropBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                faceCrop();
            }
        });
        deSpoofingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                antiSpoofing();
            }
        });
        compareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                faceCompare();
            }
        });
    }

    /**
     * 人脸检测并裁减
     */
    private void faceCrop() {
        if (bitmap1 == null || bitmap2 == null) {
            Toast.makeText(this, "请拍摄两张照片", Toast.LENGTH_LONG).show();
            return;
        }

        Bitmap bitmapTemp1 = bitmap1.copy(bitmap1.getConfig(), false);
        Bitmap bitmapTemp2 = bitmap2.copy(bitmap1.getConfig(), false);

        // 检测出人脸数据
        long start = System.currentTimeMillis();
        Vector<Box> boxes1 = mtcnn.detectFaces(bitmapTemp1, bitmapTemp1.getWidth() / 5); // 只有这句代码检测人脸，下面都是根据Box在图片中裁减出人脸
        long end = System.currentTimeMillis();
        resultTextView.setText("人脸检测前向传播耗时：" + (end - start));
        resultTextView2.setText("");
        Vector<Box> boxes2 = mtcnn.detectFaces(bitmapTemp2, bitmapTemp2.getWidth() / 5); // 只有这句代码检测人脸，下面都是根据Box在图片中裁减出人脸
        if (boxes1.size() == 0 || boxes2.size() == 0) {
            Toast.makeText(MainActivity.this, "未检测到人脸", Toast.LENGTH_LONG).show();
            return;
        }

        // 这里因为使用的每张照片里只有一张人脸，所以取第一个值，用来剪裁人脸
        Box box1 = boxes1.get(0);
        Box box2 = boxes2.get(0);

        // 人脸矫正
        bitmapTemp1 = Align.face_align(bitmapTemp1, box1.landmark);
        bitmapTemp2 = Align.face_align(bitmapTemp2, box2.landmark);
        boxes1 = mtcnn.detectFaces(bitmapTemp1, bitmapTemp1.getWidth() / 5);
        boxes2 = mtcnn.detectFaces(bitmapTemp2, bitmapTemp2.getWidth() / 5);
        box1 = boxes1.get(0);
        box2 = boxes2.get(0);

        box1.toSquareShape();
        box2.toSquareShape();
        box1.limitSquare(bitmapTemp1.getWidth(), bitmapTemp1.getHeight());
        box2.limitSquare(bitmapTemp2.getWidth(), bitmapTemp2.getHeight());
        Rect rect1 = box1.transform2Rect();
        Rect rect2 = box2.transform2Rect();

        // 剪裁人脸
        bitmapCrop1 = MyUtil.crop(bitmapTemp1, rect1);
        bitmapCrop2 = MyUtil.crop(bitmapTemp2, rect2);

        // 绘制人脸框和五点
//        Utils.drawBox(bitmapTemp1, box1, 10);
//        Utils.drawBox(bitmapTemp2, box2, 10);

//        bitmapCrop1 = MyUtil.readFromAssets(this, "42.png");
//        bitmapCrop2 = MyUtil.readFromAssets(this, "52.png");
        imageViewCrop1.setImageBitmap(bitmapCrop1);
        imageViewCrop2.setImageBitmap(bitmapCrop2);
    }

    /**
     * 活体检测
     */
    private void antiSpoofing() {
        if (bitmapCrop1 == null || bitmapCrop2 == null) {
            Toast.makeText(this, "请先检测人脸", Toast.LENGTH_LONG).show();
            return;
        }

        // 活体检测前先判断图片清晰度
        int laplace1 = fas.laplacian(bitmapCrop1);

        String text = "清晰度检测结果left：" + laplace1;
        if (laplace1 < FaceAntiSpoofing.LAPLACIAN_THRESHOLD) {
            text = text + "，" + "False";
            resultTextView.setTextColor(getResources().getColor(android.R.color.holo_red_light));
        } else {
            long start = System.currentTimeMillis();

            // 活体检测
            float score1 = fas.antiSpoofing(bitmapCrop1);

            long end = System.currentTimeMillis();

            text = "活体检测结果left：" + score1;
            if (score1 < FaceAntiSpoofing.THRESHOLD) {
                text = text + "，" + "True";
                resultTextView.setTextColor(getResources().getColor(android.R.color.holo_green_light));
            } else {
                text = text + "，" + "False";
                resultTextView.setTextColor(getResources().getColor(android.R.color.holo_red_light));
            }
            text = text + "。耗时" + (end - start);
        }
        resultTextView.setText(text);

        // 第二张图片活体检测前先判断图片清晰度
        int laplace2 = fas.laplacian(bitmapCrop2);

        String text2 = "清晰度检测结果left：" + laplace2;
        if (laplace2 < FaceAntiSpoofing.LAPLACIAN_THRESHOLD) {
            text2 = text2 + "，" + "False";
            resultTextView2.setTextColor(getResources().getColor(android.R.color.holo_red_light));
        } else {
            // 活体检测
            float score2 = fas.antiSpoofing(bitmapCrop2);
            text2 = "活体检测结果right：" + score2;
            if (score2 < FaceAntiSpoofing.THRESHOLD) {
                text2 = text2 + "，" + "True";
                resultTextView2.setTextColor(getResources().getColor(android.R.color.holo_green_light));
            } else {
                text2 = text2 + "，" + "False";
                resultTextView2.setTextColor(getResources().getColor(android.R.color.holo_red_light));
            }
        }
        resultTextView2.setText(text2);
    }

    /**
     * 人脸比对
     */
    private void faceCompare() {
        if (bitmapCrop1 == null || bitmapCrop2 == null) {
            Toast.makeText(this, "请先检测人脸", Toast.LENGTH_LONG).show();
            return;
        }

        long start = System.currentTimeMillis();
        float same = mfn.compare(bitmapCrop1, bitmapCrop2); // 就这一句有用代码，其他都是UI
        long end = System.currentTimeMillis();

        String text = "人脸比对结果：" + same;
        if (same > MobileFaceNet.THRESHOLD) {
            text = text + "，" + "True";
            resultTextView.setTextColor(getResources().getColor(android.R.color.holo_green_light));
        } else {
            text = text + "，" + "False";
            resultTextView.setTextColor(getResources().getColor(android.R.color.holo_red_light));
        }
        text = text + "，耗时" + (end - start);
        resultTextView.setText(text);
        resultTextView2.setText("");
    }

    /*********************************** 以下是相机部分 ***********************************/
    public static ImageButton currentBtn;

    private void initCamera() {
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentBtn = (ImageButton) v;
                startActivity(new Intent(MainActivity.this, CameraActivity.class));
            }
        };
        imageButton1.setOnClickListener(listener);
        imageButton2.setOnClickListener(listener);
    }
}
