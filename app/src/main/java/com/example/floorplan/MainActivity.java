package com.example.floorplan;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
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

import org.opencv.core.Mat;

import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
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
    int x;
    int y;
    private int threshold = 100;


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
                    addOnTouchListener();
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
    public void addOnTouchListener(){
        ImageView temp =imgView;
        temp.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                x =(int)event.getX();
                y = (int)event.getY();
                    String text = "You click at x = " + x + " and y = " + y;
                    Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
                    displayElements();
                return false;
            }
        });
    }

    public void displayElements() {
        Utils.bitmapToMat(imageBitmap, imageMat);
        Mat negImageMat = imageMat;
        Imgproc.threshold(negImageMat, negImageMat, 100, 255, Imgproc.THRESH_BINARY);
        Core.bitwise_not(negImageMat, negImageMat);
        Mat wall = getWall();
        Core.bitwise_xor(negImageMat, wall, wall);
        Core.bitwise_not(wall, wall);


        List<Mat> object = new ArrayList<>();
        try {
            object.add(0, Utils.loadResource(this, R.drawable.bed));
            object.add(1, Utils.loadResource(this, R.drawable.bed1));
            object.add(2, Utils.loadResource(this, R.drawable.bed2));
            object.add(3, Utils.loadResource(this, R.drawable.bed3));
            object.add(4, Utils.loadResource(this, R.drawable.chair));
            object.add(5, Utils.loadResource(this, R.drawable.chair1));
            object.add(6, Utils.loadResource(this, R.drawable.chair2));
            object.add(7, Utils.loadResource(this, R.drawable.chair3));
            object.add(8, Utils.loadResource(this, R.drawable.ctable));
            object.add(9, Utils.loadResource(this, R.drawable.dtable));
            object.add(10, Utils.loadResource(this, R.drawable.dtable1));
            object.add(11, Utils.loadResource(this, R.drawable.lsink));
            object.add(12, Utils.loadResource(this, R.drawable.lsink1));
            object.add(13, Utils.loadResource(this, R.drawable.lsink2));
            object.add(14, Utils.loadResource(this, R.drawable.lsink3));
            object.add(15, Utils.loadResource(this, R.drawable.lsofa));
            object.add(16, Utils.loadResource(this, R.drawable.lsofa1));
            object.add(17, Utils.loadResource(this, R.drawable.lsofa2));
            object.add(18, Utils.loadResource(this, R.drawable.lsofa3));
            object.add(19, Utils.loadResource(this, R.drawable.sink));
            object.add(20, Utils.loadResource(this, R.drawable.sink1));
            object.add(21, Utils.loadResource(this, R.drawable.sink2));
            object.add(22, Utils.loadResource(this, R.drawable.sink3));
            object.add(23, Utils.loadResource(this, R.drawable.ssink));
            object.add(24, Utils.loadResource(this, R.drawable.ssink1));
            object.add(25, Utils.loadResource(this, R.drawable.ssink2));
            object.add(26, Utils.loadResource(this, R.drawable.ssink3));
            object.add(27, Utils.loadResource(this, R.drawable.ssofa));
            object.add(28, Utils.loadResource(this, R.drawable.ssofa1));
            object.add(29, Utils.loadResource(this, R.drawable.ssofa2));
            object.add(30, Utils.loadResource(this, R.drawable.ssofa3));
            object.add(31, Utils.loadResource(this, R.drawable.tsink));
            object.add(32, Utils.loadResource(this, R.drawable.tsink1));
            object.add(33, Utils.loadResource(this, R.drawable.tsink2));
            object.add(34, Utils.loadResource(this, R.drawable.tsink3));
            object.add(35, Utils.loadResource(this, R.drawable.tub));
            object.add(36, Utils.loadResource(this, R.drawable.tub1));
            object.add(37, Utils.loadResource(this, R.drawable.tub2));
            object.add(38, Utils.loadResource(this, R.drawable.tub3));
            //object.add(39,Utils.loadResource(this,R.drawable.rtable));


        } catch (IOException e) {
            Toast t = Toast.makeText(this, "Unable to load object imsges", Toast.LENGTH_SHORT);
            t.show();
        }


        Mat cannyoutput = new Mat();
        Imgproc.Canny(wall, cannyoutput, threshold, threshold * 2);
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(cannyoutput, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        MatOfPoint2f[] contoursPoly = new MatOfPoint2f[contours.size()];
        Rect[] boundRect = new Rect[contours.size()];
        for (int i = 0; i < contours.size(); i++) {
            contoursPoly[i] = new MatOfPoint2f();
            Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(i).toArray()), contoursPoly[i], 3, true);
            boundRect[i] = Imgproc.boundingRect(new MatOfPoint(contoursPoly[i].toArray()));
        }
        Mat drawing = Mat.zeros(wall.size(), CvType.CV_8UC3);
        List<MatOfPoint> contoursPolyList = new ArrayList<>(contoursPoly.length);
        for (MatOfPoint2f poly : contoursPoly) {
            contoursPolyList.add(new MatOfPoint(poly.toArray()));
        }
        for (int i = 0; i < contours.size(); i++) {
            Scalar color = new Scalar(255);
           // Imgproc.drawContours(drawing, contoursPolyList, i, color, -1);
            if(boundRect[i].area()>800) {
                Imgproc.rectangle(drawing, boundRect[i].tl(), boundRect[i].br(), color, -1);
            }

        }
        Mat canny = new Mat();
        Imgproc.Canny(drawing, canny, threshold, threshold * 2);
        List<MatOfPoint> countoursFinal = new ArrayList<>();
        Mat hierarchyFinal = new Mat();
        Imgproc.findContours(canny, countoursFinal, hierarchyFinal, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        MatOfPoint2f[] countoursPolyFinal = new MatOfPoint2f[countoursFinal.size()];
        Rect[] boundRectFinal = new Rect[countoursFinal.size()];
        for (int i = 0; i < countoursFinal.size(); i++) {
            countoursPolyFinal[i] = new MatOfPoint2f();
            Imgproc.approxPolyDP(new MatOfPoint2f(countoursFinal.get(i).toArray()), countoursPolyFinal[i], 3, true);
            boundRectFinal[i] = Imgproc.boundingRect(new MatOfPoint(countoursPolyFinal[i].toArray()));
        }
        Mat finalIm = Mat.zeros(drawing.size(), CvType.CV_8UC3);
        List<MatOfPoint> countoursPolyListFinal = new ArrayList<>(countoursPolyFinal.length);
        for (MatOfPoint2f poly : countoursPolyFinal) {
            countoursPolyListFinal.add(new MatOfPoint(poly.toArray()));
        }
        List<Rect> finalRectangle =new ArrayList<>();
        for (int i = 0; i < countoursFinal.size(); i++) {
            Scalar color = new Scalar(255, 0, 0, 0);
            // Imgproc.drawContours(finalIm,countoursPolyListFinal,i,color);
            if(boundRectFinal[i].area()>2100) {
                finalRectangle.add(boundRectFinal[i]);
                Imgproc.rectangle(finalIm, boundRectFinal[i].tl(), boundRectFinal[i].br(), color, 2);
            }
        }

        List<Mat> detectobj = new ArrayList<>();
        for (int i = 0; i < finalRectangle.size(); i++) {
            Mat temp = new Mat(wall, finalRectangle.get(i));
            temp = temp.clone();
            detectobj.add(i, temp);
        }

        for (int i = 0; i < detectobj.size(); i++) {
            Mat temp = detectobj.get(i);
            double he =temp.height();
            double wi= temp.width();
            double ratio1 = he/wi;
            double ratio2 = wi/he;
            if(ratio1>1.5||ratio2>1.5)
            {
                Imgproc.resize(temp, temp, new Size(200, 100));

            }
            else {
                Imgproc.resize(temp, temp, new Size(100, 100));
            }

            detectobj.set(i, temp);
        }
        List<Mat> squareObjects =new ArrayList<>();
        List<Mat> rectangleObjects =new ArrayList<>();
        squareObjects.add(object.get(4));
        squareObjects.add(object.get(5));
        squareObjects.add(object.get(6));
        squareObjects.add(object.get(7));
        squareObjects.add(object.get(8));
        squareObjects.add(object.get(9));
        squareObjects.add(object.get(10));
        squareObjects.add(object.get(11));
        squareObjects.add(object.get(12));
        squareObjects.add(object.get(13));
        squareObjects.add(object.get(14));
        squareObjects.add(object.get(23));
        squareObjects.add(object.get(24));
        squareObjects.add(object.get(25));
        squareObjects.add(object.get(26));
        squareObjects.add(object.get(27));
        squareObjects.add(object.get(28));
        squareObjects.add(object.get(29));
        squareObjects.add(object.get(30));
        rectangleObjects.add(object.get(0));
        rectangleObjects.add(object.get(1));
        rectangleObjects.add(object.get(2));
        rectangleObjects.add(object.get(3));
        rectangleObjects.add(object.get(15));
        rectangleObjects.add(object.get(16));
        rectangleObjects.add(object.get(17));
        rectangleObjects.add(object.get(18));
        rectangleObjects.add(object.get(19));
        rectangleObjects.add(object.get(20));
        rectangleObjects.add(object.get(21));
        rectangleObjects.add(object.get(22));
        rectangleObjects.add(object.get(31));
        rectangleObjects.add(object.get(32));
        rectangleObjects.add(object.get(33));
        rectangleObjects.add(object.get(34));
        rectangleObjects.add(object.get(35));
        rectangleObjects.add(object.get(36));
        rectangleObjects.add(object.get(37));
        rectangleObjects.add(object.get(38));

        for(int i=0;i<squareObjects.size();i++){
            Mat temp=squareObjects.get(i);
            Imgproc.resize(temp,temp,new Size(100,100));
            squareObjects.set(i,temp);
        }
        for (int i = 0; i < rectangleObjects.size(); i++) {
            Mat temp =rectangleObjects.get(i);
            Imgproc.resize(temp, temp, new Size(200, 100));
            rectangleObjects.set(i, temp);
        }
        

        Mat ma=detectobj.get(12);
        double min=888888888;
        int minIndex=-1;
        if(ma.height()==ma.width()) {
            for (int i = 0; i < squareObjects.size(); i++) {
                Mat temp = squareObjects.get(i);
                Mat diff = new Mat();
                Core.subtract(temp, ma, diff);
                diff.mul(diff);
                Scalar s = Core.sumElems(diff);
                double pa = s.val[0];
                if (pa < min) {
                    min = pa;
                    minIndex = i;
                }
            }
        }
        else{
            for (int i = 0; i < rectangleObjects.size(); i++) {
                Mat temp = rectangleObjects.get(i);
                Mat diff = new Mat();
                Core.subtract(temp, ma, diff);
                diff.mul(diff);
                Scalar s = Core.sumElems(diff);
                double pa = s.val[0];
                if (pa < min) {
                    min = pa;
                    minIndex = i;
                }
            }
        }
        Toast.makeText(this,"Index"+ minIndex,Toast.LENGTH_SHORT).show();





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
