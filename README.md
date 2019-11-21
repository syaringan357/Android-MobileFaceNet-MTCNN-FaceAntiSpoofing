# MobileFaceNet-Android
Use tensorflow Lite on Android platform, integrated face detection (MTCNN), face anti spoofing (ECCV2018-FaceDeSpoofing) and face comparison (MobileFaceNet uses InsightFace loss).

https://github.com/vcvycy/MTCNN4Android  
This project is the Android implementaion of MTCNN face detection.

https://github.com/davidsandberg/facenet  
Use the MTCNN here to convert .tflite, so that you can adapt to any shape.  
  
https://github.com/jiangxiluning/facenet_mtcnn_to_mobile  
Here's how to convert .tflite.  
  
https://github.com/yaojieliu/ECCV2018-FaceDeSpoofing  
Face Anti-spoofing.  
  
https://github.com/sirius-ai/MobileFaceNet_TF  
Use this model for face comparison on mobile phones because it is very small.  
  
# BUILD
After putting .tflite in your assets directory, remember to add this code to your gradle:  
aaptOptions {  
　　noCompress "tflite"  
}  
