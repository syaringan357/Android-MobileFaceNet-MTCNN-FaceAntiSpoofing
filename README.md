# MobileFaceNet-Android
This project includes three models.  
  
MTCNN(pnet.tflite, rnet.tflite, onet.tflite), input: one Bitmap, output: Box. Use this model to detect faces from an image.  
  
FaceAntiSpoofing(FaceAntiSpoofing.tflite), input: one Bitmap, output: float score. Use this model to determine whether the image is an attack.  
  
MobileFaceNet(MobileFaceNet.tflite), input: two Bitmaps, output: float score. Use this model to judge whether two face images are one person.  

iOS platform implementation: https://github.com/syaringan357/iOS-MobileFaceNet-MTCNN-FaceAntiSpoofing
  
# References
https://github.com/vcvycy/MTCNN4Android  
This project is the Android implementaion of MTCNN face detection.

https://github.com/davidsandberg/facenet  
Use the MTCNN here to convert .tflite, so that you can adapt to any shape.  
  
https://github.com/jiangxiluning/facenet_mtcnn_to_mobile  
Here's how to convert .tflite.  
  
https://github.com/yaojieliu/CVPR2019-DeepTreeLearningForZeroShotFaceAntispoofing  
Face Anti-spoofing. I trained FaceAntiSpoofing.tflite, which only supports print attack and replay attack. If you have other requirements, please use this source code to retrain.  
  
https://github.com/sirius-ai/MobileFaceNet_TF  
Use this model for face comparison on mobile phones because it is very small.  
  
# BUILD
After putting .tflite in your assets directory, remember to add this code to your gradle:  
aaptOptions {  
　　noCompress "tflite"  
}  
  
# SCREEN SHOT
<img src="https://github.com/syaringan357/Android-MobileFaceNet-MTCNN-FaceAntiSpoofing/blob/master/ScreenShot/Screen_Shot1.png" width=375/>
<img src="https://github.com/syaringan357/Android-MobileFaceNet-MTCNN-FaceAntiSpoofing/blob/master/ScreenShot/Screen_Shot2.png" width=375/>
<img src="https://github.com/syaringan357/Android-MobileFaceNet-MTCNN-FaceAntiSpoofing/blob/master/ScreenShot/Screen_Shot3.png" width=375/>
<img src="https://github.com/syaringan357/Android-MobileFaceNet-MTCNN-FaceAntiSpoofing/blob/master/ScreenShot/Screen_Shot4.png" width=375/>
