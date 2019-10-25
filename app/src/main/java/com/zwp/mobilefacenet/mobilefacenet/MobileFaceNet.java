package com.zwp.mobilefacenet.mobilefacenet;

import android.content.res.AssetManager;
import android.graphics.Bitmap;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

/**
 * 人脸比对
 */
public class MobileFaceNet {
    private static final String MODEL_FILE = "file:///android_asset/MobileFaceNet_9925_9680.pb";
    private static final String INPUT_NAME = "input:0";
    private static final String OUTPUT_NAME = "embeddings:0";

    public static final int INPUT_IMAGE_SIZE = 112; // 需要feed数据的placeholder的图片宽高
    private static final int EMBEDDING_SIZE = 128; // 输出的embedding维度
    public static final float THRESHOLD = 0.8f; // 设置一个阙值，大于这个值认为是同一个人

    private AssetManager am;
    private TensorFlowInferenceInterface tii;

    public MobileFaceNet(AssetManager assetManager) {
        this.am = assetManager;
        loadModel();
    }

    private void loadModel() {
        tii = new TensorFlowInferenceInterface(am, MODEL_FILE);
    }

    public float compare(Bitmap bitmap1, Bitmap bitmap2) {
        // 将人脸resize为112X112大小的，因为下面需要feed数据的placeholder的形状是(?, 112, 112, 3)
        bitmap1 = Bitmap.createScaledBitmap(bitmap1, INPUT_IMAGE_SIZE, INPUT_IMAGE_SIZE, true);
        bitmap2 = Bitmap.createScaledBitmap(bitmap2, INPUT_IMAGE_SIZE, INPUT_IMAGE_SIZE, true);

        float[] datasets = getTwoImageDatasets(bitmap1, bitmap2);
        tii.feed(INPUT_NAME, datasets, 2, INPUT_IMAGE_SIZE, INPUT_IMAGE_SIZE, 3);
        tii.run(new String[] {OUTPUT_NAME});

        // 返回结果是128维的embedding
        float[] embeddings = new float[EMBEDDING_SIZE * 2];
        tii.fetch(OUTPUT_NAME, embeddings);

        return evaluate(embeddings);
    }

    /**
     * 计算两张图片的相似度
     * @param embeddings
     * @return
     */
    private float evaluate(float[] embeddings) {
        float[] embeddings1 = new float[EMBEDDING_SIZE];
        float[] embeddings2 = new float[EMBEDDING_SIZE];
        System.arraycopy(embeddings, 0, embeddings1, 0, EMBEDDING_SIZE);
        System.arraycopy(embeddings, EMBEDDING_SIZE, embeddings2, 0, EMBEDDING_SIZE);
        float dist = 0;
        for (int i = 0; i < EMBEDDING_SIZE; i++) {
            dist += Math.pow(embeddings1[i] - embeddings2[i], 2);
        }
        float same = 0;
        for (int i = 0; i < 400; i++) {
            float threshold = 0.01f * (i + 1);
            if (dist < threshold) {
                same += 1.0 / 400;
            }
        }
        return same;
    }

    /**
     * 获取两张图片-1~1区间的数据
     * @param bitmap1
     * @param bitmap2
     * @return
     */
    private float[] getTwoImageDatasets(Bitmap bitmap1, Bitmap bitmap2) {
        float[] floatValues = new float[2 * INPUT_IMAGE_SIZE * INPUT_IMAGE_SIZE * 3];
        int[] intValues = new int[INPUT_IMAGE_SIZE * INPUT_IMAGE_SIZE];
        Bitmap[] bitmaps = {bitmap1, bitmap2};

        // 0~255的像素值映射到-1~1区间
        float imageMean = 127.5f;
        float imageStd = 128;

        for (int i = 0; i < bitmaps.length; i++) {
            Bitmap bitmap = bitmaps[i];
            bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0,
                    bitmap.getWidth(), bitmap.getHeight());
            for (int j = 0; j < intValues.length; j++) {
                final int val=intValues[j];
                floatValues[i * INPUT_IMAGE_SIZE * INPUT_IMAGE_SIZE * 3 + j * 3] =
                        (((val >> 16) & 0xFF) - imageMean) / imageStd;
                floatValues[i * INPUT_IMAGE_SIZE * INPUT_IMAGE_SIZE * 3 + j * 3 + 1] =
                        (((val >> 8) & 0xFF) - imageMean) / imageStd;
                floatValues[i * INPUT_IMAGE_SIZE * INPUT_IMAGE_SIZE * 3 + j * 3 + 2] =
                        ((val & 0xFF) - imageMean) / imageStd;
            }
        }
        return floatValues;
    }
}
