package com.zwp.mobilefacenet;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class MyUtil {

    /**
     * 从assets中读取图片
     * @param context
     * @param filename
     * @return
     */
    public static Bitmap readFromAssets(Context context, String filename){
        Bitmap bitmap;
        AssetManager asm = context.getAssets();
        try {
            InputStream is = asm.open(filename);
            bitmap = BitmapFactory.decodeStream(is);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return bitmap;
    }

    /**
     * 给rect增加margin
     * @param bitmap
     * @param rect
     * @param marginX
     * @param marginY
     */
    public static void rectExtend(Bitmap bitmap, Rect rect, int marginX, int marginY) {
        rect.left = max(0, rect.left - marginX / 2);
        rect.right = min(bitmap.getWidth() - 1, rect.right + marginX / 2);
        rect.top = max(0, rect.top - marginY / 2);
        rect.bottom = min(bitmap.getHeight() - 1, rect.bottom + marginY / 2);
    }

    /**
     * 加载模型文件
     * @param assetManager
     * @param modelPath
     * @return
     * @throws IOException
     */
    public static MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
}
