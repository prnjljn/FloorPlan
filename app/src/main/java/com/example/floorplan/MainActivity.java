package com.example.floorplan;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.ORB;
import org.opencv.imgproc.Imgproc;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.opencv.calib3d.Calib3d;

public class MainActivity extends AppCompatActivity {

    public static final int Image_Gallery_Request = 20;
    private ImageView imgView;
    Bitmap imageBitmap;
    Bitmap bitmap;
    Mat imageMat;
    private MenuItem displayBoundary;
    private  MenuItem displayElement;
    private  MenuItem segmentRoom;
    private  MenuItem reset;
    private int threshold = 100;
    ORB detector;
    DescriptorMatcher matcher;
    Mat descriptors2,descriptors1;
    MatOfKeyPoint keypoints1,keypoints2;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i("OpenCV", "OpenCV loaded successfully");
                    imageMat = new Mat();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imgView = (ImageView) findViewById(R.id.imgview);

    }

    public void LoadImage(View view) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        File PictureDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        String Path = PictureDirectory.getPath();
        Uri data = Uri.parse(Path);
        intent.setDataAndType(data, "image/*");
        startActivityForResult(intent, Image_Gallery_Request);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {

            if (requestCode == Image_Gallery_Request) {

                Uri ImageUri = data.getData();
                InputStream inputStream;
                try {
                    inputStream = getContentResolver().openInputStream(ImageUri);

                    imageBitmap = BitmapFactory.decodeStream(inputStream);
                    bitmap=imageBitmap;
                    imgView.setImageBitmap(imageBitmap);
                    Button b = findViewById(R.id.button);
                    b.setVisibility(View.INVISIBLE);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Unable to open imageBitmap", Toast.LENGTH_SHORT);
                }

            }
        }
    }
    @Override
    public  boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_main, menu);
        displayBoundary =menu.add("Display Boundary");
        displayElement=menu.add("Display Elements");
        segmentRoom=menu.add("Segment Room");
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        if(item==displayBoundary){

            displayBoundary();
        }
        if(item==displayElement){
            displayElements();
        }
        if(item==segmentRoom){
            segmentRoom();
        }



        return true;
    }


    public void displayBoundary(){
        Mat boun =getWall();
        Core.bitwise_not(boun,boun);
        Bitmap tempImageBitmap =imageBitmap;
        Utils.matToBitmap(boun,tempImageBitmap);
        imgView.setImageBitmap(tempImageBitmap);

    }

    public void displayElements() {
        Utils.bitmapToMat(imageBitmap,imageMat);
        Mat negImageMat = imageMat;
        Imgproc.threshold(negImageMat, negImageMat, 100, 255, Imgproc.THRESH_BINARY);
        Core.bitwise_not(negImageMat, negImageMat);
        Mat wall= getWall();
        Core.bitwise_xor(negImageMat,wall,wall);
        Core.bitwise_not(wall,wall);
        Mat img1 =new Mat();
        try {
            img1 = Utils.loadResource(this, R.drawable.arm);
        }
        catch (IOException e){
            Toast t =Toast.makeText(this,"Image diiferent type",Toast.LENGTH_SHORT);
            t.show();


        }
        Imgproc.cvtColor(wall,wall,Imgproc.COLOR_RGB2GRAY);
        Imgproc.cvtColor(img1,img1,Imgproc.COLOR_RGB2GRAY);
        descriptors1 = new Mat();
        keypoints1 = new MatOfKeyPoint();
        detector.detectAndCompute(img1,new Mat(),keypoints1,descriptors1);
        descriptors2 = new Mat();
        keypoints2 = new MatOfKeyPoint();
        detector.detectAndCompute(wall,new Mat(),keypoints2,descriptors2);
        List<KeyPoint> kp1 =  keypoints1.toList();
        List<KeyPoint> kp2 =  keypoints2.toList();
        List<MatOfDMatch> knnMatches = new ArrayList<>();
        matcher.knnMatch(descriptors1,descriptors2,knnMatches,2);
        float ratioThresh = 0.75f;
        List<DMatch> listOfGoodMatches = new ArrayList<>();
        for (int i = 0; i < knnMatches.size(); i++) {
            if (knnMatches.get(i).rows() > 1) {
                DMatch[] matches = knnMatches.get(i).toArray();
                if (matches[0].distance < ratioThresh * matches[1].distance) {
                    listOfGoodMatches.add(matches[0]);
                }
            }
        }
        MatOfDMatch goodMatches = new MatOfDMatch();
        goodMatches.fromList(listOfGoodMatches);

        List<Point> obj = new ArrayList<>();
        List<Point> scene = new ArrayList<>();

        for(int i=0;i<listOfGoodMatches.size();i++){
            int img1_idx= listOfGoodMatches.get(i).queryIdx;
            int img2_idx=listOfGoodMatches.get(i).trainIdx;
            obj.add(kp1.get(img1_idx).pt);
            scene.add(kp2.get(img2_idx).pt);
        }
        MatOfPoint2f objMat = new MatOfPoint2f();
        objMat.fromList(obj);
        MatOfPoint2f sceneMat = new MatOfPoint2f();
        sceneMat.fromList(scene);
        double thr =3.0;
        Mat H = Calib3d.findHomography(objMat, sceneMat,Calib3d.RANSAC,thr);
        Mat objCorners = new Mat(4, 1, CvType.CV_32FC2), sceneCorners = new Mat();
        float[] objCornersData = new float[(int) (objCorners.total() * objCorners.channels())];objCorners.get(0, 0, objCornersData);
        objCornersData[0] = 0;
        objCornersData[1] = 0;
        objCornersData[2] = img1.cols();
        objCornersData[3] = 0;
        objCornersData[4] = img1.cols();
        objCornersData[5] = img1.rows();
        objCornersData[6] = 0;
        objCornersData[7] = img1.rows();
        objCorners.put(0, 0, objCornersData);
        Mat imgMatches = new Mat();
        Core.perspectiveTransform(objCorners, sceneCorners, H);
        float[] sceneCornersData = new float[(int) (sceneCorners.total() * sceneCorners.channels())];
        sceneCorners.get(0, 0, sceneCornersData);
        Imgproc.line(wall, new Point(sceneCornersData[0] + img1.cols(), sceneCornersData[1]),
                new Point(sceneCornersData[2] + img1.cols(), sceneCornersData[3]), new Scalar(0, 255, 0), 4);
        Imgproc.line(wall, new Point(sceneCornersData[2] + img1.cols(), sceneCornersData[3]),
                new Point(sceneCornersData[4] + img1.cols(), sceneCornersData[5]), new Scalar(0, 255, 0), 4);
        Imgproc.line(wall, new Point(sceneCornersData[4] + img1.cols(), sceneCornersData[5]),
                new Point(sceneCornersData[6] + img1.cols(), sceneCornersData[7]), new Scalar(0, 255, 0), 4);
        Imgproc.line(wall, new Point(sceneCornersData[6] + img1.cols(), sceneCornersData[7]),
                new Point(sceneCornersData[0] + img1.cols(), sceneCornersData[1]), new Scalar(0, 255, 0), 4);
       Bitmap b= imageBitmap;
       Utils.matToBitmap(wall,b);
       imgView.setImageBitmap(b);

    }


    public Mat getWall() {
        Mat tempImageMat = new Mat();
        Utils.bitmapToMat(imageBitmap,tempImageMat);
        Imgproc.threshold(tempImageMat, tempImageMat, 100, 255, Imgproc.THRESH_BINARY);
        Imgproc.dilate(tempImageMat, tempImageMat, new Mat());
        Imgproc.dilate(tempImageMat, tempImageMat, new Mat());
        Imgproc.erode(tempImageMat, tempImageMat, new Mat());
        Imgproc.erode(tempImageMat, tempImageMat, new Mat());
        Core.bitwise_not(tempImageMat, tempImageMat);
        return tempImageMat;
    }
    public void segmentRoom(){
        Mat tempImageMat =new Mat();
        Utils.bitmapToMat(imageBitmap,tempImageMat);
        Imgproc.threshold(tempImageMat, tempImageMat, 100, 255, Imgproc.THRESH_BINARY);
        Imgproc.dilate(tempImageMat, tempImageMat, new Mat());
        Imgproc.dilate(tempImageMat, tempImageMat, new Mat());
        Mat cannyoutput =new Mat();
        Imgproc.Canny(tempImageMat,cannyoutput,threshold,threshold*2);
        List<MatOfPoint> contours =new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(cannyoutput,contours,hierarchy,Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_SIMPLE);
        MatOfPoint2f[] contoursPoly  = new MatOfPoint2f[contours.size()];
        Rect[] boundRect = new Rect[contours.size()];
        Point[] centers = new Point[contours.size()];
        float[][] radius = new float[contours.size()][1];
        for (int i = 0; i < contours.size(); i++) {
            contoursPoly[i] = new MatOfPoint2f();
            Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(i).toArray()), contoursPoly[i], 3, true);
            boundRect[i] = Imgproc.boundingRect(new MatOfPoint(contoursPoly[i].toArray()));
            centers[i] = new Point();
            Imgproc.minEnclosingCircle(contoursPoly[i], centers[i], radius[i]);
        }
            Mat drawing = Mat.zeros(cannyoutput.size(), CvType.CV_8UC3);
            List<MatOfPoint> contoursPolyList = new ArrayList<>(contoursPoly.length);
            for (MatOfPoint2f poly : contoursPoly) {
                contoursPolyList.add(new MatOfPoint(poly.toArray()));
            }
        for (int i = 0; i < contours.size(); i++) {
            Scalar color = new Scalar(255);
            Imgproc.drawContours(drawing, contoursPolyList, i, color);
            Imgproc.rectangle(drawing, boundRect[i].tl(), boundRect[i].br(), color, 2);
        }
            Bitmap temp= imageBitmap;
            Utils.matToBitmap(drawing,temp);
            imgView.setImageBitmap(temp);



    }
}
