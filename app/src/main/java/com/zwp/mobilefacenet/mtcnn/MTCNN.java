package com.zwp.mobilefacenet.mtcnn;
/*
  MTCNN For Android
  by cjf@xmu 20180625
 */

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.util.Log;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.util.Vector;

public class MTCNN {
    //参数
    private float factor=0.709f;
    private float PNetThreshold=0.6f;
    private float RNetThreshold=0.7f;
    private float ONetThreshold=0.7f;
    //MODEL PATH
    private static final String MODEL_FILE  = "file:///android_asset/mtcnn_freezed_model.pb";
    //tensor name
    private static final String PNetInName  ="pnet/input:0";
    private static final String[] PNetOutName =new String[]{"pnet/prob1:0","pnet/conv4-2/BiasAdd:0"};
    private static final String RNetInName  ="rnet/input:0";
    private static final String[] RNetOutName =new String[]{ "rnet/prob1:0","rnet/conv5-2/conv5-2:0",};
    private static final String ONetInName  ="onet/input:0";
    private static final String[] ONetOutName =new String[]{ "onet/prob1:0","onet/conv6-2/conv6-2:0","onet/conv6-3/conv6-3:0"};
    //安卓相关
    public  long lastProcessTime;   //最后一张图片处理的时间ms
    private static final String TAG="MTCNN";
    private AssetManager assetManager;
    private TensorFlowInferenceInterface inferenceInterface;
    public MTCNN(AssetManager mgr){
        assetManager=mgr;
        loadModel();
    }
    private void loadModel() {
        inferenceInterface = new TensorFlowInferenceInterface(assetManager, MODEL_FILE);
    }
    //读取Bitmap像素值，预处理(-127.5 /128)，转化为一维数组返回
    private float[] normalizeImage(Bitmap bitmap){
        int w=bitmap.getWidth();
        int h=bitmap.getHeight();
        float[] floatValues=new float[w*h*3];
        int[]   intValues=new int[w*h];
        bitmap.getPixels(intValues,0,bitmap.getWidth(),0,0,bitmap.getWidth(),bitmap.getHeight());
        float imageMean=127.5f;
        float imageStd=128;

        for (int i=0;i<intValues.length;i++){
            final int val=intValues[i];
            floatValues[i * 3 + 0] = (((val >> 16) & 0xFF) - imageMean) / imageStd;
            floatValues[i * 3 + 1] = (((val >> 8) & 0xFF) - imageMean) / imageStd;
            floatValues[i * 3 + 2] = ((val & 0xFF) - imageMean) / imageStd;
        }
        return floatValues;
    }
    /*
       检测人脸,minSize是最小的人脸像素值
     */
    private Bitmap bitmapResize(Bitmap bm, float scale) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        // CREATE A MATRIX FOR THE MANIPULATION。matrix指定图片仿射变换参数
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scale, scale);
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, true);
        return resizedBitmap;
    }
    //输入前要翻转，输出也要翻转
    private  int PNetForward(Bitmap bitmap, float [][]PNetOutProb, float[][][]PNetOutBias){
        int w=bitmap.getWidth();
        int h=bitmap.getHeight();

        float[] PNetIn=normalizeImage(bitmap);
        Utils.flip_diag(PNetIn,h,w,3); //沿着对角线翻转
        inferenceInterface.feed(PNetInName,PNetIn,1,w,h,3);
        inferenceInterface.run(PNetOutName,false);
        int PNetOutSizeW=(int) Math.ceil(w*0.5-5);
        int PNetOutSizeH=(int) Math.ceil(h*0.5-5);
        float[] PNetOutP=new float[PNetOutSizeW*PNetOutSizeH*2];
        float[] PNetOutB=new float[PNetOutSizeW*PNetOutSizeH*4];
        inferenceInterface.fetch(PNetOutName[0],PNetOutP);
        inferenceInterface.fetch(PNetOutName[1],PNetOutB);
        //【写法一】先翻转，后转为2/3维数组
        Utils.flip_diag(PNetOutP,PNetOutSizeW,PNetOutSizeH,2);
        Utils.flip_diag(PNetOutB,PNetOutSizeW,PNetOutSizeH,4);
        Utils.expand(PNetOutB,PNetOutBias);
        Utils.expandProb(PNetOutP,PNetOutProb);
        /*
        *【写法二】这个比较快，快了3ms。意义不大，用上面的方法比较直观
        for (int y=0;y<PNetOutSizeH;y++)
            for (int x=0;x<PNetOutSizeW;x++){
               int idx=PNetOutSizeH*x+y;
               PNetOutProb[y][x]=PNetOutP[idx*2+1];
               for(int i=0;i<4;i++)
                   PNetOutBias[y][x][i]=PNetOutB[idx*4+i];
            }
        */
        return 0;
    }
    //Non-Maximum Suppression
    //nms，不符合条件的deleted设置为true
    private void nms(Vector<Box> boxes, float threshold, String method){
        //NMS.两两比对
        //int delete_cnt=0;
        for(int i=0;i<boxes.size();i++) {
            Box box = boxes.get(i);
            if (!box.deleted) {
                //score<0表示当前矩形框被删除
                for (int j = i + 1; j < boxes.size(); j++) {
                    Box box2=boxes.get(j);
                    if (!box2.deleted) {
                        int x1 = Math.max(box.box[0], box2.box[0]);
                        int y1 = Math.max(box.box[1], box2.box[1]);
                        int x2 = Math.min(box.box[2], box2.box[2]);
                        int y2 = Math.min(box.box[3], box2.box[3]);
                        if (x2 < x1 || y2 < y1) continue;
                        int areaIoU = (x2 - x1 + 1) * (y2 - y1 + 1);
                        float iou=0f;
                        if (method.equals("Union"))
                            iou = 1.0f*areaIoU / (box.area() + box2.area() - areaIoU);
                        else if (method.equals("Min"))
                            iou= 1.0f*areaIoU / (Math.min(box.area(),box2.area()));
                        if (iou >= threshold) { //删除prob小的那个框
                            if (box.score>box2.score)
                                box2.deleted=true;
                            else
                                box.deleted=true;
                            //delete_cnt++;
                        }
                    }
                }
            }
        }
        //Log.i(TAG,"[*]sum:"+boxes.size()+" delete:"+delete_cnt);
    }
    private int generateBoxes(float[][] prob, float[][][]bias, float scale, float threshold, Vector<Box> boxes){
        int h=prob.length;
        int w=prob[0].length;
        //Log.i(TAG,"[*]height:"+prob.length+" width:"+prob[0].length);
        for (int y=0;y<h;y++)
            for (int x=0;x<w;x++){
                float score=prob[y][x];
                //only accept prob >threadshold(0.6 here)
                if (score>PNetThreshold){
                    Box box=new Box();
                    //score
                    box.score=score;
                    //box
                    box.box[0]= Math.round(x*2/scale);
                    box.box[1]= Math.round(y*2/scale);
                    box.box[2]= Math.round((x*2+11)/scale);
                    box.box[3]= Math.round((y*2+11)/scale);
                    //bbr
                    for(int i=0;i<4;i++)
                        box.bbr[i]=bias[y][x][i];
                    //add
                    boxes.addElement(box);
                }
            }
        return 0;
    }
    private void BoundingBoxReggression(Vector<Box> boxes){
        for (int i=0;i<boxes.size();i++)
            boxes.get(i).calibrate();
    }
    //Pnet + Bounding Box Regression + Non-Maximum Regression
    /* NMS执行完后，才执行Regression
     * (1) For each scale , use NMS with threshold=0.5
     * (2) For all candidates , use NMS with threshold=0.7
     * (3) Calibrate Bounding Box
     * 注意：CNN输入图片最上面一行，坐标为[0..width,0]。所以Bitmap需要对折后再跑网络;网络输出同理.
     */
    private Vector<Box> PNet(Bitmap bitmap, int minSize){
        int whMin=Math.min(bitmap.getWidth(),bitmap.getHeight());
        float currentFaceSize=minSize;  //currentFaceSize=minSize/(factor^k) k=0,1,2... until excced whMin
        Vector<Box> totalBoxes=new Vector<Box>();
        //【1】Image Paramid and Feed to Pnet
        while (currentFaceSize<=whMin){
            float scale=12.0f/currentFaceSize;
            //(1)Image Resize
            Bitmap bm=bitmapResize(bitmap,scale);
            int w=bm.getWidth();
            int h=bm.getHeight();
            //(2)RUN CNN
            int PNetOutSizeW=(int)(Math.ceil(w*0.5-5)+0.5);
            int PNetOutSizeH=(int)(Math.ceil(h*0.5-5)+0.5);
            float[][]   PNetOutProb=new float[PNetOutSizeH][PNetOutSizeW];;
            float[][][] PNetOutBias=new float[PNetOutSizeH][PNetOutSizeW][4];
            PNetForward(bm,PNetOutProb,PNetOutBias);
            //(3)数据解析
            Vector<Box> curBoxes=new Vector<Box>();
            generateBoxes(PNetOutProb,PNetOutBias,scale,PNetThreshold,curBoxes);
            //Log.i(TAG,"[*]CNN Output Box number:"+curBoxes.size()+" Scale:"+scale);
            //(4)nms 0.5
            nms(curBoxes,0.5f,"Union");
            //(5)add to totalBoxes
            for (int i=0;i<curBoxes.size();i++)
                if (!curBoxes.get(i).deleted)
                    totalBoxes.addElement(curBoxes.get(i));
            //Face Size等比递增
            currentFaceSize/=factor;
        }
        //NMS 0.7
        nms(totalBoxes,0.7f,"Union");
        //BBR
        BoundingBoxReggression(totalBoxes);
        return Utils.updateBoxes(totalBoxes);
    }
    //截取box中指定的矩形框(越界要处理)，并resize到size*size大小，返回数据存放到data中。
    public Bitmap tmp_bm;
    private void crop_and_resize(Bitmap bitmap, Box box, int size, float[] data){
        //(2)crop and resize
        Matrix matrix = new Matrix();
        float scale=1.0f*size/box.width();
        matrix.postScale(scale, scale);
        Bitmap croped= Bitmap.createBitmap(bitmap, box.left(),box.top(),box.width(), box.height(),matrix,true);
        //(3)save
        int[] pixels_buf=new int[size*size];
        croped.getPixels(pixels_buf,0,croped.getWidth(),0,0,croped.getWidth(),croped.getHeight());
        float imageMean=127.5f;
        float imageStd=128;
        for (int i=0;i<pixels_buf.length;i++){
            final int val=pixels_buf[i];
            data[i * 3 + 0] = (((val >> 16) & 0xFF) - imageMean) / imageStd;
            data[i * 3 + 1] = (((val >> 8) & 0xFF) - imageMean) / imageStd;
            data[i * 3 + 2] = ((val & 0xFF) - imageMean) / imageStd;
        }
    }
    /*
     * RNET跑神经网络，将score和bias写入boxes
     */
    private void RNetForward(float[] RNetIn, Vector<Box> boxes){
        int num=RNetIn.length/24/24/3;
        //feed & run
        inferenceInterface.feed(RNetInName,RNetIn,num,24,24,3);
        inferenceInterface.run(RNetOutName,false);
        //fetch
        float[] RNetP=new float[num*2];
        float[] RNetB=new float[num*4];
        inferenceInterface.fetch(RNetOutName[0],RNetP);
        inferenceInterface.fetch(RNetOutName[1],RNetB);
        //转换
        for (int i=0;i<num;i++) {
            boxes.get(i).score = RNetP[i * 2 + 1];
            for (int j=0;j<4;j++)
                boxes.get(i).bbr[j]=RNetB[i*4+j];
        }
    }
    //Refine Net
    private Vector<Box> RNet(Bitmap bitmap, Vector<Box> boxes){
        //RNet Input Init
        int num=boxes.size();
        float[] RNetIn=new float[num*24*24*3];
        float[] curCrop=new float[24*24*3];
        int RNetInIdx=0;
        for (int i=0;i<num;i++){
            crop_and_resize(bitmap,boxes.get(i),24,curCrop);
            Utils.flip_diag(curCrop,24,24,3);
            //Log.i(TAG,"[*]Pixels values:"+curCrop[0]+" "+curCrop[1]);
            for (int j=0;j<curCrop.length;j++) RNetIn[RNetInIdx++]= curCrop[j];
        }
        //Run RNet
        RNetForward(RNetIn,boxes);
        //RNetThreshold
        for (int i=0;i<num;i++)
            if (boxes.get(i).score<RNetThreshold)
                boxes.get(i).deleted=true;
        //Nms
        nms(boxes,0.7f,"Union");
        BoundingBoxReggression(boxes);
        return Utils.updateBoxes(boxes);
    }
    /*
     * ONet跑神经网络，将score和bias写入boxes
     */
    private void ONetForward(float[] ONetIn, Vector<Box> boxes){
        int num=ONetIn.length/48/48/3;
        //feed & run
        inferenceInterface.feed(ONetInName,ONetIn,num,48,48,3);
        inferenceInterface.run(ONetOutName,false);
        //fetch
        float[] ONetP=new float[num*2]; //prob
        float[] ONetB=new float[num*4]; //bias
        float[] ONetL=new float[num*10]; //landmark
        inferenceInterface.fetch(ONetOutName[0],ONetP);
        inferenceInterface.fetch(ONetOutName[1],ONetB);
        inferenceInterface.fetch(ONetOutName[2],ONetL);
        //转换
        for (int i=0;i<num;i++) {
            //prob
            boxes.get(i).score = ONetP[i * 2 + 1];
            //bias
            for (int j=0;j<4;j++)
                boxes.get(i).bbr[j]=ONetB[i*4+j];

            //landmark
            for (int j=0;j<5;j++) {
                int x=boxes.get(i).left()+(int) (ONetL[i * 10 + j]*boxes.get(i).width());
                int y= boxes.get(i).top()+(int) (ONetL[i * 10 + j +5]*boxes.get(i).height());
                boxes.get(i).landmark[j] = new Point(x,y);
                //Log.i(TAG,"[*] landmarkd "+x+ "  "+y);
            }
        }
    }
    //ONet
    private Vector<Box> ONet(Bitmap bitmap, Vector<Box> boxes){
        //ONet Input Init
        int num=boxes.size();
        float[] ONetIn=new float[num*48*48*3];
        float[] curCrop=new float[48*48*3];
        int ONetInIdx=0;
        for (int i=0;i<num;i++){
            crop_and_resize(bitmap,boxes.get(i),48,curCrop);
            Utils.flip_diag(curCrop,48,48,3);
            for (int j=0;j<curCrop.length;j++) ONetIn[ONetInIdx++]= curCrop[j];
        }
        //Run ONet
        ONetForward(ONetIn,boxes);
        //ONetThreshold
        for (int i=0;i<num;i++)
            if (boxes.get(i).score<ONetThreshold)
                boxes.get(i).deleted=true;
        BoundingBoxReggression(boxes);
        //Nms
        nms(boxes,0.7f,"Min");
        return Utils.updateBoxes(boxes);
    }
    private void square_limit(Vector<Box> boxes, int w, int h){
        //square
        for (int i=0;i<boxes.size();i++) {
            boxes.get(i).toSquareShape();
            boxes.get(i).limit_square(w,h);
        }
    }
    /*
     * 参数：
     *   bitmap:要处理的图片
     *   minFaceSize:最小的人脸像素值.(此值越大，检测越快)
     * 返回：
     *   人脸框
     */
    public Vector<Box> detectFaces(Bitmap bitmap, int minFaceSize) {
        long t_start = System.currentTimeMillis();
        //【1】PNet generate candidate boxes
        Vector<Box> boxes=PNet(bitmap,minFaceSize);
        square_limit(boxes,bitmap.getWidth(),bitmap.getHeight());
        //【2】RNet
        boxes=RNet(bitmap,boxes);
        square_limit(boxes,bitmap.getWidth(),bitmap.getHeight());
        //【3】ONet
        boxes=ONet(bitmap,boxes);
        //return
        Log.i(TAG,"[*]Mtcnn Detection Time:"+(System.currentTimeMillis()-t_start));
        lastProcessTime=(System.currentTimeMillis()-t_start);
        return  boxes;
    }
}
