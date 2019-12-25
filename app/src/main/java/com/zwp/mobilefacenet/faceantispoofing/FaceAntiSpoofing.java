package com.zwp.mobilefacenet.faceantispoofing;

import android.content.res.AssetManager;
import android.graphics.Bitmap;

import com.zwp.mobilefacenet.MyUtil;

import org.tensorflow.lite.Interpreter;

import java.io.IOException;

public class FaceAntiSpoofing {
    private static final String MODEL_FILE = "FaceAntiSpoofing.tflite";

    public static final int INPUT_IMAGE_SIZE = 256; // 需要feed数据的placeholder的图片宽高
    public static final float THRESHOLD = 0.5f; // 设置一个阙值，大于这个值认为是攻击

    private Interpreter interpreter;

    public FaceAntiSpoofing(AssetManager assetManager) throws IOException {
        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(4);
        interpreter = new Interpreter(MyUtil.loadModelFile(assetManager, MODEL_FILE), options);
    }

    /**
     * 活体检测
     * @param bitmap
     * @return 评分
     */
    public float antiSpoofing(Bitmap bitmap) {
        // 将人脸resize为256X256大小的，因为下面需要feed数据的placeholder的形状是(1, 256, 256, 3)
        Bitmap bitmapScale = Bitmap.createScaledBitmap(bitmap, INPUT_IMAGE_SIZE, INPUT_IMAGE_SIZE, true);

        float[][][] img = normalizeImage(bitmapScale);
        float[][][][] input = new float[1][][][];
        input[0] = img;
        float[][] clss_pred = new float[1][8];
        interpreter.run(input, clss_pred);
        float score = 0;
        for (int i = 0; i < 8; i++) {
            score += Math.pow(clss_pred[0][i], 2);
        }
        return score / 8;
    }

    /**
     * 归一化图片到[0, 1]
     * @param bitmap
     * @return
     */
    public static float[][][] normalizeImage(Bitmap bitmap) {
        int h = bitmap.getHeight();
        int w = bitmap.getWidth();
        float[][][] floatValues = new float[h][w][3];

        float imageStd = 255;
        int[] pixels = new int[h * w];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, w, h);
        for (int i = 0; i < h; i++) { // 注意是先高后宽
            for (int j = 0; j < w; j++) {
                final int val = pixels[i * w + j];
                float r = ((val >> 16) & 0xFF) / imageStd;
                float g = ((val >> 8) & 0xFF) / imageStd;
                float b = (val & 0xFF) / imageStd;

                float[] arr = {r, g, b};
                floatValues[i][j] = arr;
            }
        }
        return floatValues;
    }
}
