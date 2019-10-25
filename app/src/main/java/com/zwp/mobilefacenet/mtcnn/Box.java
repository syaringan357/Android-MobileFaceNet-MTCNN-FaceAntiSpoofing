package com.zwp.mobilefacenet.mtcnn;

import android.graphics.Point;
import android.graphics.Rect;

import static java.lang.Math.max;

/**
 * 人脸框
 */
public class Box {
    public float[] box;           // left:box[0],top:box[1],right:box[2],bottom:box[3]
    public float score;         // probability
    public float[] bbr;         // bounding box regression
    public boolean deleted;
    public float[] landmark;    // facial landmark.只有ONet输出Landmark

    public Box() {
        box = new float[4];
        bbr = new float[4];
        deleted = false;
        landmark = new float[10];
    }

    public float left() {
        return box[0];
    }

    public float right() {
        return box[2];
    }

    public float top() {
        return box[1];
    }

    public float bottom() {
        return box[2];
    }

    public float width() {
        return box[2] - box[0] + 1;
    }

    public float height() {
        return box[3] - box[1] + 1;
    }

    // 转为rect
    public Rect transform2Rect() {
        Rect rect = new Rect();
        rect.left = Math.round(box[0]);
        rect.top = Math.round(box[1]);
        rect.right = Math.round(box[2]);
        rect.bottom = Math.round(box[3]);
        return rect;
    }

    // 转为points
    public Point[] transform2Point() {
        Point[] points = new Point[5];
        for (int i = 0; i < 5; i++) {
            int x = Math.round(left() + (landmark[i] * width()));
            int y = Math.round(top() + (landmark[i + 5] * height()));
            points[i] = new Point(x, y);
        }
        return points;
    }

    // 面积
    public float area() {
        return width() * height();
    }

    // Bounding Box Regression
    public void calibrate() {
        float w = box[2] - box[0] + 1;
        float h = box[3] - box[1] + 1;
        box[0] = box[0] + w * bbr[0];
        box[1] = box[1] + h * bbr[1];
        box[2] = box[2] + w * bbr[2];
        box[3] = box[3] + h * bbr[3];
        for (int i = 0; i < 4; i++) bbr[i] = 0.0f;
    }

    // 当前box转为正方形
    public void toSquareShape() {
        float w = width();
        float h = height();
        if (w > h) {
            box[1] -= (w - h) / 2;
            box[3] += (w - h + 1) / 2;
        } else {
            box[0] -= (h - w) / 2;
            box[2] += (h - w + 1) / 2;
        }
    }

    // 防止边界溢出，并维持square大小
    public void limit_square(int w, int h) {
        if (box[0] < 0 || box[1] < 0) {
            float len = max(-box[0], -box[1]);
            box[0] += len;
            box[1] += len;
        }
        if (box[2] >= w || box[3] >= h) {
            float len = max(box[2] - w + 1, box[3] - h + 1);
            box[2] -= len;
            box[3] -= len;
        }
    }
}
