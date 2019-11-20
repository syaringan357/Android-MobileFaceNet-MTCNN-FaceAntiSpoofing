package com.zwp.mobilefacenet.facedespoofing;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;

import com.zwp.mobilefacenet.MyUtil;

import org.tensorflow.lite.Interpreter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FaceDeSpoofing {
    private static final String MODEL_FILE = "FaceDeSpoofing.tflite";

    public static final int INPUT_IMAGE_SIZE = 256; // 需要feed数据的placeholder的图片宽高
    public static final float THRESHOLD = 0; // 设置一个阙值，大于这个值认为是攻击

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
        float[][][][] conv11_fir = new float[1][32][32][1];
        float[][][][] conv11 = new float[1][32][32][1];
        float[][][][] conv11_new = new float[1][32][32][1];

        Map<Integer, Object> outputs = new HashMap<>();
        outputs.put(interpreter.getOutputIndex("conv11_fir"), conv11_fir);
        outputs.put(interpreter.getOutputIndex("conv11"), conv11);
        outputs.put(interpreter.getOutputIndex("conv11_new"), conv11_new);
        interpreter.runForMultipleInputsOutputs(new Object[]{input}, outputs);

        float sum1_0 = 0;
        float sum1_1 = 0;
        float sum1_2 = 0;
        for (int i = 0; i < 32; i++) {
            float sum2_0 = 0;
            float sum2_1 = 0;
            float sum2_2 = 0;
            for (int j = 0; j < 32; j++) {
                sum2_0 += conv11_fir[0][i][j][0];
                sum2_1 += conv11[0][i][j][0];
                sum2_2 += conv11_new[0][i][j][0];
            }
            sum1_0 += sum2_0 / 32;
            sum1_1 += sum2_1 / 32;
            sum1_2 += sum2_2 / 32;
        }
        float sum0 = sum1_0 / 32;
        float sum1 = sum1_1 / 32;
        float sum2 = sum1_2 / 32;
        return sum0 - sum1 - sum2;
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
