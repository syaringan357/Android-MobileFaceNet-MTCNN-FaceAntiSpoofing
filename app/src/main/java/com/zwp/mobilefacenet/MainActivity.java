package com.zwp.mobilefacenet;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.zwp.mobilefacenet.facedespoofing.FaceDeSpoofing;
import com.zwp.mobilefacenet.mobilefacenet.MobileFaceNet;
import com.zwp.mobilefacenet.mtcnn.Box;
import com.zwp.mobilefacenet.mtcnn.MTCNN;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

public class MainActivity extends AppCompatActivity {

    private MTCNN mtcnn; // 人脸检测
    private FaceDeSpoofing fds; // 活体检测
    private MobileFaceNet mfn; // 人脸比对

    private Bitmap bitmap1;
    private Bitmap bitmap2;
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
            fds = new FaceDeSpoofing(getAssets());
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
                deSpoofing();
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

        Bitmap bitmapTemp1 = Bitmap.createScaledBitmap(bitmap1, MTCNN.IMAGE_WIDTH, MTCNN.IMAGE_HEIGHT, true);
        Bitmap bitmapTemp2 = Bitmap.createScaledBitmap(bitmap2, MTCNN.IMAGE_WIDTH, MTCNN.IMAGE_HEIGHT, true);

        // 检测出人脸数据
        Vector<Box> boxes1 = null;
        Vector<Box> boxes2 = null;
        // 如果没检测到人脸会抛出异常
        try {
            long start = System.currentTimeMillis();
            boxes1 = mtcnn.detectFaces(bitmapTemp1, MTCNN.MIN_SIZE); // 只有这句代码检测人脸，下面都是根据Box在图片中裁减出人脸
            long end = System.currentTimeMillis();
            resultTextView.setText("人脸检测前向传播耗时：" + (end - start));
            resultTextView2.setText("");
            boxes2 = mtcnn.detectFaces(bitmapTemp2, MTCNN.MIN_SIZE); // 只有这句代码检测人脸，下面都是根据Box在图片中裁减出人脸
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (boxes1 == null || boxes2 == null) {
            Toast.makeText(MainActivity.this, "未检测到人脸", Toast.LENGTH_LONG).show();
            return;
        }
        // 这里因为使用的每张照片里只有一张人脸，所以取第一个值，用来剪裁人脸
        Box box1 = boxes1.get(0);
        Box box2 = boxes2.get(0);

        // 剪裁人脸
        bitmapCrop1 = MyUtil.crop(bitmapTemp1, box1.transform2Rect());
        bitmapCrop2 = MyUtil.crop(bitmapTemp2, box2.transform2Rect());

        imageViewCrop1.setImageBitmap(bitmapCrop1);
        imageViewCrop2.setImageBitmap(bitmapCrop2);
    }

    /**
     * 活体检测
     */
    private void deSpoofing() {
        if (bitmapCrop1 == null || bitmapCrop2 == null) {
            Toast.makeText(this, "请先检测人脸", Toast.LENGTH_LONG).show();
            return;
        }

        long start = System.currentTimeMillis();
        float score1 = fds.deSpoofing(bitmapCrop1); // 就这一句有用代码，其他都是UI
        long end = System.currentTimeMillis();
        float score2 = fds.deSpoofing(bitmapCrop2); // 就这一句有用代码，其他都是UI

        String text = "活体检测结果left：" + score1;
        if (score1 < FaceDeSpoofing.THRESHOLD) {
            text = text + "，" + "True";
            resultTextView.setTextColor(getResources().getColor(android.R.color.holo_green_light));
        } else {
            text = text + "，" + "False";
            resultTextView.setTextColor(getResources().getColor(android.R.color.holo_red_light));
        }
        text = text + "。耗时" + (end - start);
        resultTextView.setText(text);


        String text2 = "活体检测结果right：" + score2;
        if (score2 < FaceDeSpoofing.THRESHOLD) {
            text2 = text2 + "，" + "True";
            resultTextView2.setTextColor(getResources().getColor(android.R.color.holo_green_light));
        } else {
            text2 = text2 + "，" + "False";
            resultTextView2.setTextColor(getResources().getColor(android.R.color.holo_red_light));
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
    private static final int REQUEST_PERMISSION_CODE = 101;
    private static final int REQUEST_TAKE_PHOTO_CODE = 102;
    private Uri mUri;
    private ImageButton currentBtn;

    private void initCamera() {
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!checkPermission()) {
                        requestPermissions();
                    } else {
                        takePhoto();
                    }
                } else {
                    takePhoto();
                }
                currentBtn = (ImageButton) v;
            }
        };
        imageButton1.setOnClickListener(listener);
        imageButton2.setOnClickListener(listener);
    }

    private boolean checkPermission() {
        boolean haveCameraPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean haveWritePermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        return haveCameraPermission && haveWritePermission;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestPermissions() {
        requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                REQUEST_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISSION_CODE:
                boolean allowAllPermission = false;
                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                        allowAllPermission = false;
                        break;
                    }
                    allowAllPermission = true;
                }
                if (allowAllPermission) {
                    takePhoto();
                } else {
                    Toast.makeText(this, "需要相机权限", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private void takePhoto() {
        String path = getFilesDir() + File.separator + "images" + File.separator;
        File file = new File(path, "cache.jpg");
        if(!file.getParentFile().exists())
            file.getParentFile().mkdirs();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mUri = FileProvider.getUriForFile(this, "com.zwp.mobilefacenet", file);
        } else {
            mUri = Uri.fromFile(file);
        }
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mUri);
        startActivityForResult(intent, REQUEST_TAKE_PHOTO_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == REQUEST_TAKE_PHOTO_CODE) {
            Bitmap bm = null;
            try {
                bm = getBitmapFormUri(mUri);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            bm = rotateBitmap(bm, getBitmapDegree(getFilePathForN(this, mUri)));
            if (currentBtn.getId() == R.id.image_button1) {
                bitmap1 = bm;
            } else {
                bitmap2 = bm;
            }
            currentBtn.setImageBitmap(bm);
        }
    }

    public Bitmap getBitmapFormUri(Uri uri) throws IOException {
        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inSampleSize = 1;
        bitmapOptions.inDither = true;
        bitmapOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        InputStream input = getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);
        input.close();
        return bitmap;
    }

    public static int getBitmapDegree(String path) {
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }

    public static Bitmap rotateBitmap(Bitmap bitmap, int degrees) {
        if (degrees == 0 || null == bitmap) {
            return bitmap;
        }
        Matrix matrix = new Matrix();
        matrix.setRotate(degrees, bitmap.getWidth() / 2, bitmap.getHeight() / 2);
        Bitmap bmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        bitmap.recycle();
        return bmp;
    }

    // 如果是android 7.0以下，自己去把这个url转路径的方法改了
    private static String getFilePathForN(Context context, Uri uri) {
        try {
            Cursor returnCursor = context.getContentResolver().query(uri, null, null, null, null);
            int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            returnCursor.moveToFirst();
            String name = (returnCursor.getString(nameIndex));
            File file = new File(context.getFilesDir(), name);
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(file);
            int read = 0;
            int maxBufferSize = 1 * 1024 * 1024;
            int bytesAvailable = inputStream.available();
            int bufferSize = Math.min(bytesAvailable, maxBufferSize);
            final byte[] buffers = new byte[bufferSize];
            while ((read = inputStream.read(buffers)) != -1) {
                outputStream.write(buffers, 0, read);
            }
            returnCursor.close();
            inputStream.close();
            outputStream.close();
            return file.getPath();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
