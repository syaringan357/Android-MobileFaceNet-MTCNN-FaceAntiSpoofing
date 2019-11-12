package com.zwp.mobilefacenet.facedespoofing;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;

import com.zwp.mobilefacenet.MyUtil;

import org.tensorflow.lite.Interpreter;

import java.io.IOException;

public class FaceDeSpoofing {
    private static final String MODEL_FILE = "FaceDeSpoofing.tflite";

    public static final int INPUT_IMAGE_SIZE = 256; // 需要feed数据的placeholder的图片宽高
    public static final float THRESHOLD = 0.5f; // 设置一个阙值，小于这个值认为是真人

    private Interpreter interpreter;

    public FaceDeSpoofing(AssetManager assetManager) throws IOException {
        interpreter = new Interpreter(MyUtil.loadModelFile(assetManager, MODEL_FILE), new Interpreter.Options());
    }

    /**
     * 活体检测
     * @param bitmap
     * @return 评分
     */
    public float deSpoofing(Bitmap bitmap) {
        // 将人脸resize为256X256大小的，因为下面需要feed数据的placeholder的形状是(1, 256, 256, 3)
        bitmap = Bitmap.createScaledBitmap(bitmap, INPUT_IMAGE_SIZE, INPUT_IMAGE_SIZE, true);

        float[][][] img = normalizeImage(bitmap);
        float[][][][] data = new float[1][][][];
        data[0] = img;
        float[][][][] out = new float[1][32][32][1];
        interpreter.run(data, out);

        float sum = 0;
        for (int i = 0; i < 32; i++) {
            for (int j = 0; j < 32; j++) {
                sum += out[0][i][j][0];
            }
        }


//        System.out.println("[" + img.length + ", " + img[0].length + ", " + img[0][0].length + "]");
//        for (int i = 0; i < INPUT_IMAGE_SIZE; i++) {
//            for (int j = 0; j < INPUT_IMAGE_SIZE; j++) {
//                System.out.println(img[i][j][0] + " " + img[i][j][1] + " " + img[i][j][2] + " " +
//                        img[i][j][3] + " " + img[i][j][4] + " " + img[i][j][5] + " ");
//
//            }
//            break;
//        }


        return sum / 32 / 32;
    }

    /**
     * 归一化图片到[0, 1]，然后转为HSV，然后将HUE归一到[0, 1]，再和RGB一起将图片拼接成6个通道
     * @param bitmap
     * @return
     */
    public static float[][][] normalizeImage(Bitmap bitmap) {
        int h = bitmap.getHeight();
        int w = bitmap.getWidth();
        float[][][] floatValues = new float[h][w][6];

        float imageStd = 256;

        int[] pixels = new int[h * w];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, w, h);
        for (int i = 0; i < h; i++) { // 注意是先高后宽
            for (int j = 0; j < w; j++) {
                final int val = pixels[i * w + j];
                float[] hsv = new float[3];
                Color.colorToHSV(val, hsv);

                float hue = hsv[0] / 360;
                float s = hsv[1];
                float v = hsv[2];

                float r = ((val >> 16) & 0xFF) / imageStd;
                float g = ((val >> 8) & 0xFF) / imageStd;
                float b = (val & 0xFF) / imageStd;

                float[] arr = {hue, s, v, r, g, b};
                floatValues[i][j] = arr;
            }
        }
        return floatValues;
    }
}
