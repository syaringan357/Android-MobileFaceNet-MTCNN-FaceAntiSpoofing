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
    public static final float THRESHOLD = 0.5f; // 设置一个阙值，大于这个值认为是攻击

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
        Bitmap bitmapScale = Bitmap.createScaledBitmap(bitmap, INPUT_IMAGE_SIZE, INPUT_IMAGE_SIZE, true);

        float[][][] img = normalizeImage(bitmapScale);
        float[][][][] input = new float[1][][][];
        input[0] = img;
        float[][][][] score_fir = new float[1][32][32][1];
        interpreter.run(input, score_fir);

        float sum1 = 0;
        for (int i = 0; i < 32; i++) {
            float sum2 = 0;
            for (int j = 0; j < 32; j++) {
                sum2 += Math.pow(score_fir[0][i][j][0], 2);
            }
            sum1 += sum2 / 32;
        }
        return sum1 / 32;
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
