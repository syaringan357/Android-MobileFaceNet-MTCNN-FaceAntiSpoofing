package com.zwp.mobilefacenet.mobilefacenet;

import android.content.res.AssetManager;
import android.graphics.Bitmap;

import com.zwp.mobilefacenet.MyUtil;

import org.tensorflow.lite.Interpreter;

import java.io.IOException;

/**
 * 人脸比对
 */
public class MobileFaceNet {
    private static final String MODEL_FILE = "MobileFaceNet.tflite";

    public static final int INPUT_IMAGE_SIZE = 112; // 需要feed数据的placeholder的图片宽高
    private static final int EMBEDDING_SIZE = 192; // 输出的embedding维度
    public static final float THRESHOLD = 0.88f; // 设置一个阙值，大于这个值认为是同一个人

    private Interpreter interpreter;

    public MobileFaceNet(AssetManager assetManager) throws IOException {
        interpreter = new Interpreter(MyUtil.loadModelFile(assetManager, MODEL_FILE), new Interpreter.Options());
    }

    public float compare(Bitmap bitmap1, Bitmap bitmap2) {
        // 将人脸resize为112X112大小的，因为下面需要feed数据的placeholder的形状是(?, 112, 112, 3)
        bitmap1 = Bitmap.createScaledBitmap(bitmap1, INPUT_IMAGE_SIZE, INPUT_IMAGE_SIZE, true);
        bitmap2 = Bitmap.createScaledBitmap(bitmap2, INPUT_IMAGE_SIZE, INPUT_IMAGE_SIZE, true);

        float[][][][] datasets = getTwoImageDatasets(bitmap1, bitmap2);
        float[][] embeddings = new float[2][EMBEDDING_SIZE];
        interpreter.run(datasets, embeddings);
        return evaluate(embeddings);
    }

    /**
     * 计算两张图片的相似度
     * @param embeddings
     * @return
     */
    private float evaluate(float[][] embeddings) {
        float[] embeddings1 = embeddings[0];
        float[] embeddings2 = embeddings[1];
        float dist = 0;
        for (int i = 0; i < EMBEDDING_SIZE; i++) {
            dist += Math.pow(embeddings1[i] - embeddings2[i], 2);
        }
        float same = 0;
        for (int i = 0; i < 400; i++) {
            float threshold = 100 * (i + 1);
            if (dist < threshold) {
                same += 1.0 / 400;
            }
        }
        return same;
    }


    private float[][][][] getTwoImageDatasets(Bitmap bitmap1, Bitmap bitmap2) {
        Bitmap[] bitmaps = {bitmap1, bitmap2};

        int[] ddims = {bitmaps.length, INPUT_IMAGE_SIZE, INPUT_IMAGE_SIZE, 3};
        float[][][][] datasets = new float[ddims[0]][ddims[1]][ddims[2]][3];

        float imageMean = 127.5f;
        float imageStd = 128;

        //把原图缩放成我们需要的图片大小
        for (int i = 0; i < ddims[0]; i++) {
            Bitmap bitmap = bitmaps[i];
            int[] pixels = new int[ddims[1] * ddims[2]];
            bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, ddims[1], ddims[2]);
            for (int j = 0; j < ddims[1]; j++) {
                for (int k = 0; k < ddims[2]; k++) {
                    final int val = pixels[j * ddims[2] + k];
                    float r = (((val >> 16) & 0xFF) - imageMean) / imageStd;
                    float g = (((val >> 8) & 0xFF) - imageMean) / imageStd;
                    float b = ((val & 0xFF) - imageMean) / imageStd;
                    float[] arr = {r, g, b};
                    datasets[i][j][k] = arr;
                }
            }
        }
        return datasets;
    }
}
