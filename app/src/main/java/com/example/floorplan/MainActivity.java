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
import org.opencv.core.Size;
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
    public int getMinimumIndex(List<Double> s){
        int minIndex=0;
        for(int i=0;i<s.size();i++){
            if(s.get(minIndex)>s.get(i)){
                minIndex=i;
            }

        }
        return minIndex;
    }



    public void displayElements() {
        Utils.bitmapToMat(imageBitmap,imageMat);
        Mat negImageMat = imageMat;
        Imgproc.threshold(negImageMat, negImageMat, 100, 255, Imgproc.THRESH_BINARY);
        Core.bitwise_not(negImageMat, negImageMat);
        Mat wall= getWall();
        Core.bitwise_xor(negImageMat,wall,wall);
        Core.bitwise_not(wall,wall);


        List<Mat> object =new ArrayList<>();
        try {
            object.add(0,Utils.loadResource(this, R.drawable.arm));
            object.add(1,Utils.loadResource(this, R.drawable.bed));
            object.add(2,Utils.loadResource(this, R.drawable.cofee));
            object.add(3,Utils.loadResource(this, R.drawable.rtable));
            object.add(4,Utils.loadResource(this, R.drawable.lsofa));
            object.add(5,Utils.loadResource(this, R.drawable.ssofa));
            object.add(6,Utils.loadResource(this, R.drawable.sink));
            object.add(7,Utils.loadResource(this, R.drawable.twinsink));
            object.add(8,Utils.loadResource(this, R.drawable.ssink));
            object.add(9,Utils.loadResource(this, R.drawable.lsink));
            object.add(10,Utils.loadResource(this, R.drawable.tub));
            object.add(11,Utils.loadResource(this, R.drawable.dtable));


        }
        catch (IOException e){
            Toast t =Toast.makeText(this,"Unable to load object imsges",Toast.LENGTH_SHORT);
            t.show();
        }


        Mat cannyoutput =new Mat();
        Imgproc.Canny(wall,cannyoutput,threshold,threshold*2);
        List<MatOfPoint> contours =new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(cannyoutput,contours,hierarchy,Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_SIMPLE);
        MatOfPoint2f[] contoursPoly  = new MatOfPoint2f[contours.size()];
        Rect[] boundRect = new Rect[contours.size()];
        for(int i=0;i<contours.size();i++){
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
            Imgproc.drawContours(drawing, contoursPolyList, i, color,-1);
            Imgproc.rectangle(drawing, boundRect[i].tl(), boundRect[i].br(), color, -1);

        }
       Mat canny =new Mat();
        Imgproc.Canny(drawing,canny,threshold,threshold*2);
        List<MatOfPoint> countoursFinal=new ArrayList<>();
        Mat hierarchyFinal =new Mat();
        Imgproc.findContours(canny,countoursFinal,hierarchyFinal,Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_SIMPLE);
        MatOfPoint2f[] countoursPolyFinal=new MatOfPoint2f[countoursFinal.size()];
        Rect[] boundRectFinal = new Rect[countoursFinal.size()];
        for(int i=0;i<countoursFinal.size();i++){
            countoursPolyFinal[i]=new MatOfPoint2f();
            Imgproc.approxPolyDP(new MatOfPoint2f(countoursFinal.get(i).toArray()),countoursPolyFinal[i],3,true);
            boundRectFinal[i]=Imgproc.boundingRect(new MatOfPoint(countoursPolyFinal[i].toArray()));
        }
        Mat finalIm = Mat.zeros(drawing.size(), CvType.CV_8UC3);
        List<MatOfPoint> countoursPolyListFinal =new ArrayList<>(countoursPolyFinal.length);
        for(MatOfPoint2f poly : countoursPolyFinal){
            countoursPolyListFinal.add(new MatOfPoint(poly.toArray()));
        }
        for(int i=0;i<countoursFinal.size();i++){
            Scalar color =new Scalar(255,0,0,0);
           // Imgproc.drawContours(finalIm,countoursPolyListFinal,i,color);
            Imgproc.rectangle(finalIm,boundRectFinal[i].tl(),boundRectFinal[i].br(),color,2);
        }
        List<Mat> detectobj =new ArrayList<>();
        for(int i=0;i<boundRectFinal.length;i++){
               Mat temp = new Mat(wall,boundRectFinal[i]);
               temp=temp.clone();
               detectobj.add(i,temp);
        }
        Mat t =detectobj.get(0);
        Bitmap m =Bitmap.createBitmap(t.cols(),t.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(t,m);
        imgView.setImageBitmap(m);
        DescriptorMatcher matcher=DescriptorMatcher.create(DescriptorMatcher.FLANNBASED);
        Mat descriptors2=new Mat(),descriptors1=new Mat();
        MatOfKeyPoint keypoints1=new MatOfKeyPoint();
        MatOfKeyPoint keypoints2 =new MatOfKeyPoint();
        ORB detecctor=ORB.create();
        int[] arr= new int [12];
        for(int j=0;j<12;j++){
            detecctor.detectAndCompute(t, new Mat(), keypoints1, descriptors1);
            detecctor.detectAndCompute(object.get(j), new Mat(), keypoints2, descriptors2);
            List<MatOfDMatch> knnMatches = new ArrayList<>();
            matcher.knnMatch(descriptors1, descriptors2, knnMatches, 2);
            float ratioThresh = 0.7f;
            List<DMatch> listOfGoodMatches = new ArrayList<>();
            for (int k = 0; k < knnMatches.size(); k++) {
                if (knnMatches.get(k).rows() > 1) {
                    DMatch[] matches = knnMatches.get(k).toArray();
                    if (matches[0].distance < ratioThresh * matches[1].distance) {
                        listOfGoodMatches.add(matches[0]);
                    }
                }
            }
        }








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
