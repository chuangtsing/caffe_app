package edu.psu.cse.caffe_app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import edu.psu.cse.caffe_app.R;

import org.bytedeco.javacv.*;
import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.indexer.*;

import static org.bytedeco.javacpp.avcodec.*;
import static org.bytedeco.javacpp.avformat.*;
import static org.bytedeco.javacpp.avutil.*;
import static org.bytedeco.javacpp.swscale.*;
import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_calib3d.*;
import static org.bytedeco.javacpp.opencv_objdetect.*;
import static org.bytedeco.javacpp.opencv_highgui.*;
import static org.bytedeco.javacpp.opencv_video.*;
import static org.bytedeco.javacpp.opencv_photo.*;
import static org.bytedeco.javacpp.opencv_imgcodecs.*;
import static org.bytedeco.javacpp.opencv_videoio.*;
import static org.bytedeco.javacpp.opencv_videostab.*;


public class MainActivity extends Activity {
    private static final String LOG_TAG = "MainActivity";
    private static final int REQUEST_IMAGE_CAPTURE = 100;
    private static final int REQUEST_IMAGE_SELECT = 200;
    private static final int REQUEST_VIDEO_SELECT = 300;
    private static final int REQUEST_VIDEO_LOCAL = 310;
    private static final int REQUEST_VIDEO_REMOTE = 320;
    private static final int REQUEST_VIDEO_ADAPTIVE = 330;
    public static final int MEDIA_TYPE_IMAGE = 1;
    private static String[] IMAGENET_CLASSES;
    private static final String BIN_FOLDER_NAME = "/data/data/edu.psu.cse.caffe_app/bin/";
    private static final String MODEL_FOLDER_NAME = "/caffe_app/bvlc_reference_caffenet/";			// folder that stores all external files
    private static final String [] MODEL_FILE_LIST = {"bvlc_reference_caffenet.caffemodel", "deploy_mobile.prototxt"};		// file names
    private static final String [] BIN_FILE_LIST = {"ffmpeg"};
    private String modelFolderPath = "";
    private String binFolderPath = "";
    private String appFilePath = "";

    private List<Mat> listMat;

	private Spinner topkSpinner;
    private Spinner fpsSpinner;
    private Spinner modeSpinner;
    private int n_topk;
    private int n_mode;
    private double db_fps;
    private Button btnCamera;
    private Button btnSelect;
    private Button btnVideo;
    private ImageView ivCaptured;
    private TextView tvLabel;
    private Uri fileUri;
    private ProgressDialog dialog;
    private Bitmap bmp;
    private CaffeMobile caffeMobile;
    private CaffeMobile caffeSurrogate;

    static {
        System.loadLibrary("caffe");
        System.loadLibrary("caffe_jni");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        modeSpinner = (Spinner) findViewById(R.id.spinExtract);
        List<String> extractList = new ArrayList<String>();
        extractList.add("Extract frames only");
        extractList.add("Extract and process (in batch)");
        extractList.add("Extract and process (in serial)");

        ArrayAdapter<String> extractAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, extractList);
        extractAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modeSpinner.setAdapter(extractAdapter);
        modeSpinner.setSelection(1);

        fpsSpinner = (Spinner) findViewById(R.id.spinFps);
        List<String> fpsList = new ArrayList<String>();
        fpsList.add(String.valueOf(0.2));
        fpsList.add(String.valueOf(0.5));
        fpsList.add(String.valueOf(1));
        fpsList.add(String.valueOf(2));
        fpsList.add(String.valueOf(3));
        fpsList.add(String.valueOf(4));
        fpsList.add(String.valueOf(4.5));
        fpsList.add(String.valueOf(5));
        fpsList.add(String.valueOf(6));
        fpsList.add(String.valueOf(7));
        fpsList.add(String.valueOf(8));
        fpsList.add(String.valueOf(10));
        fpsList.add(String.valueOf(15));
        fpsList.add(String.valueOf(20));
        fpsList.add("All");

        ArrayAdapter<String> fpsAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, fpsList);
        fpsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fpsSpinner.setAdapter(fpsAdapter);
        fpsSpinner.setSelection(2);


        topkSpinner = (Spinner) findViewById(R.id.spinTopk);
    	List<String> list = new ArrayList<String>();
    	for(int i=1; i<=50; i++)
    	{
    		String s = String.format("%d",i);
    		list.add(s);
    	}
    	
    	appFilePath = this.getFilesDir().getParent();
 
    	ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
    		android.R.layout.simple_spinner_item, list);
    	dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	topkSpinner.setAdapter(dataAdapter);

        ivCaptured = (ImageView) findViewById(R.id.ivCaptured);
        tvLabel = (TextView) findViewById(R.id.tvLabel);

        btnCamera = (Button) findViewById(R.id.btnCamera);
        btnCamera.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                initPrediction();
                fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
                Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                i.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
                startActivityForResult(i, REQUEST_IMAGE_CAPTURE);
            }
        });

        btnSelect = (Button) findViewById(R.id.btnSelect);
        btnSelect.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                initPrediction();
                Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, REQUEST_IMAGE_SELECT);
            }
        });
        
        btnVideo = (Button) findViewById(R.id.btnVideo);
        btnVideo.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	CharSequence options[] = new CharSequence[] {"All local", "All remote", "Local extraction"};       	
            	AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            	//builder.setCancelable(false);
            	builder.setTitle("Choose an option");
            	builder.setItems(options, new DialogInterface.OnClickListener() {
            	    @Override
            	    public void onClick(DialogInterface dialog, int which) {
            	    	 initPrediction();
                         Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                         if(which == 0)
                        	 startActivityForResult(i, REQUEST_VIDEO_LOCAL);
                         else if(which == 1)
                        	 startActivityForResult(i, REQUEST_VIDEO_REMOTE);
                         else
                        	 startActivityForResult(i, REQUEST_VIDEO_ADAPTIVE);
            	    }
            	});
            	builder.show();
            }
        });
        
        modelFolderPath = Environment.getExternalStorageDirectory().toString() + MODEL_FOLDER_NAME;
        File file = new File(modelFolderPath);
		if (!file.exists()) {
			file.mkdirs();
			Log.i("on create","file folder created");
			
			 for(int i=0; i<MODEL_FILE_LIST.length; i++){
		        File f = new File(modelFolderPath+MODEL_FILE_LIST[i]);
		        if(!f.exists())
		        	copyFile(MODEL_FILE_LIST[i],i,modelFolderPath+MODEL_FILE_LIST[i]);		// copy needed files to the folder
		     }
		}
		
        binFolderPath = BIN_FOLDER_NAME;
        file = new File(binFolderPath);
		if (!file.exists()) {
			file.mkdirs();
			Log.i("on create","file folder created");
			
			for(int i=0; i<BIN_FILE_LIST.length; i++){
		        	File f = new File(binFolderPath+BIN_FILE_LIST[i]);
		        	if(!f.exists()){
		        		copyFile(BIN_FILE_LIST[i],i,binFolderPath+BIN_FILE_LIST[i]);
		        		runCommand("chmod" + " 0700 " + binFolderPath+BIN_FILE_LIST[i]);
		        	}// copy needed files to the folder
		    }
		}

        Runtime rt = Runtime.getRuntime();
        Log.d(LOG_TAG, "App heap max memory  " + String.valueOf(rt.maxMemory()) + " Bytes");

        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        Log.d(LOG_TAG, "App heap memory class " + String.valueOf(activityManager.getMemoryClass()) + " MB");

        caffeMobile = new CaffeMobile();
        caffeMobile.enableLog(true);
        caffeMobile.loadModel(modelFolderPath+MODEL_FILE_LIST[1],
                			  modelFolderPath+MODEL_FILE_LIST[0]);

//        caffeSurrogate = new CaffeMobile();
//        caffeSurrogate.enableLog(true);
//        caffeSurrogate.loadModel(modelFolderPath+MODEL_FILE_LIST[1],
//                modelFolderPath+MODEL_FILE_LIST[0]);

//        caffeMobile.loadModel("/sdcard/caffe_mobile/bvlc_reference_caffenet/deploy_mobile.prototxt",
//                "/sdcard/caffe_mobile/bvlc_reference_caffenet/bvlc_reference_caffenet.caffemodel");
        
        AssetManager am = this.getAssets();
        try {
            InputStream is = am.open("synset_words.txt");
            Scanner sc = new Scanner(is);
            List<String> lines = new ArrayList<String>();
            while (sc.hasNextLine()) {
                final String temp = sc.nextLine();
                lines.add(temp.substring(temp.indexOf(" ") + 1));
            }
            IMAGENET_CLASSES = lines.toArray(new String[0]);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == REQUEST_IMAGE_CAPTURE || requestCode == REQUEST_IMAGE_SELECT 
        		|| requestCode == REQUEST_VIDEO_SELECT || requestCode == REQUEST_VIDEO_LOCAL
        		||requestCode == REQUEST_VIDEO_REMOTE || requestCode == REQUEST_VIDEO_ADAPTIVE )
        		&& resultCode == RESULT_OK) {
            
        	String imgPath = "";
            String videoPath = "";
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                imgPath = fileUri.getPath();
            } else if(requestCode == REQUEST_IMAGE_SELECT){
                Uri selectedImage = data.getData();
                String[] filePathColumn = { MediaStore.Images.Media.DATA };
                Cursor cursor = MainActivity.this.getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                imgPath = cursor.getString(columnIndex);
                cursor.close();
            } else {
            	Uri selectedVideo = data.getData();
            	String[] filePathColumn = { MediaStore.Video.Media.DATA };
            	Cursor cursor = MainActivity.this.getContentResolver().query(selectedVideo, filePathColumn, null, null, null);
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                videoPath = cursor.getString(columnIndex);
                cursor.close();
            }

            n_topk = (int)topkSpinner.getSelectedItemId()+1;
            n_mode = (int)modeSpinner.getSelectedItemId();
            if(fpsSpinner.getSelectedItemId() != fpsSpinner.getAdapter().getCount()-1){
                db_fps = Double.parseDouble(fpsSpinner.getSelectedItem().toString());
            }else{
                db_fps = 0.;
            }

	        if(requestCode == REQUEST_IMAGE_CAPTURE || requestCode == REQUEST_IMAGE_SELECT ){
	            bmp = BitmapFactory.decodeFile(imgPath);
	            Log.d(LOG_TAG, imgPath);
	            Log.d(LOG_TAG, String.valueOf(bmp.getHeight()));
	            Log.d(LOG_TAG, String.valueOf(bmp.getWidth()));
	
	            dialog = ProgressDialog.show(MainActivity.this, "Predicting...", "Wait for one sec...", true);
	
	            ImageTask imageTask = new ImageTask();
	            imageTask.execute(imgPath);
            } else{
            	if(requestCode == REQUEST_VIDEO_LOCAL){
                    if(n_mode == 0)
                        dialog = ProgressDialog.show(MainActivity.this, "Extracting...", "Wait for one sec...", true);
                    else
                        dialog = ProgressDialog.show(MainActivity.this, "Extracting and predicting...", "Wait for one sec...", true);
            		cvExtractDetection videoTask = new cvExtractDetection();
            		videoTask.execute(videoPath);
//                    ExtractDetection detector = new ExtractDetection(videoPath);
//                    detector.Detection();
            	}else if(requestCode == REQUEST_VIDEO_REMOTE){
                    dialog = ProgressDialog.show(MainActivity.this, "Extracting and sending...", "Wait for one sec...", true);
            		VideoTask videoTask = new VideoTask();
    	            videoTask.execute(videoPath);
            	}else if(requestCode == REQUEST_VIDEO_ADAPTIVE){
                    dialog = ProgressDialog.show(MainActivity.this, "Extracting and predicting or sending...", "Wait for one sec...", true);
            		VideoTask videoTask = new VideoTask();
    	            videoTask.execute(videoPath);
            	}   
            }
        } else {
            finishPrediction();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void initPrediction() {
        btnCamera.setEnabled(false);
        btnSelect.setEnabled(false);
        btnVideo.setEnabled(false);
        fpsSpinner.setEnabled(false);
        topkSpinner.setEnabled(false);
        modeSpinner.setEnabled(false);
        tvLabel.setText("");
    }

    private void finishPrediction(){
        btnCamera.setEnabled(true);
        btnSelect.setEnabled(true);
        btnVideo.setEnabled(true);
        fpsSpinner.setEnabled(true);
        topkSpinner.setEnabled(true);
        modeSpinner.setEnabled(true);
    }

    private class ImageTask extends AsyncTask<String, Void, Integer> {
        
        long processtime;
        int[] topK;
        
        @Override
        protected Integer doInBackground(String... strings) {
            Mat mat = imread(strings[0]);
            processtime = System.currentTimeMillis();
            topK = caffeMobile.predictImageMat(mat.address(), n_topk);
            processtime = System.currentTimeMillis() - processtime;
            return 1;
        }

        @Override
        protected void onPostExecute(Integer integer) {

        	ivCaptured.setImageBitmap(bmp);
        	String text = "";
        	for(int i=0; i<n_topk; i++)
        		text += IMAGENET_CLASSES[topK[i]]+"---\n";
        	text += String.valueOf(processtime) + " ms";
            tvLabel.setText(text);
            Log.d(LOG_TAG, "Image done !!");
            finishPrediction();
            if (dialog != null) {
                dialog.dismiss();
            }
            super.onPostExecute(integer);
        }
    }
    
    private class VideoTask extends AsyncTask<String, Void, Integer> {
        
        long processtime;
        long extracttime;
        int[] topK;
        SortedMap<String, Integer> mapText = new TreeMap<String, Integer>();

        @Override
        protected Integer doInBackground(String... strings) {

        	extracttime = System.currentTimeMillis();
        	String cmd = BIN_FOLDER_NAME+"ffmpeg -i "+strings[0]+" -r 1 " + " -vf transpose=1 " +
   	    		BIN_FOLDER_NAME + "image-%d.jpeg";
//       	    String cmd = BIN_FOLDER_NAME+"ffmpeg -i "+ "/sdcard/DCIM/Camera/10s.MOV" +" -r 1 " + " -vf transpose=1 " +
//   	    		BIN_FOLDER_NAME + "image-%d.jpeg";
        	runCommandGetOutput(cmd);
        	extracttime = System.currentTimeMillis() - extracttime;
        	
        	File folder = new File(BIN_FOLDER_NAME);
        	String strTemp = "";
        	
        	processtime = System.currentTimeMillis();
        	if(folder.isDirectory())
        	{
        		
        		String[] files = folder.list();
        		int length = folder.listFiles().length;
                for (int i = 0; i < length; i++) {
                	if(!files[i].contains(".jpeg")) continue;
                    topK = caffeMobile.predictImage(BIN_FOLDER_NAME+files[i], n_topk);
                    for(int j=0; j<n_topk; j++){
                		strTemp = IMAGENET_CLASSES[topK[j]];
                		if(null == mapText.get(strTemp))
                			mapText.put(strTemp, 1);
                		else
                			mapText.put(strTemp, mapText.get(strTemp)+1);
                    }
                    File deletefile = new File(folder.getAbsolutePath(), files[i]);
                    deletefile.delete();
                }        
        	}    
        	processtime = System.currentTimeMillis() - processtime;	
        	
            return 1;
        }

        @Override
        protected void onPostExecute(Integer integer) {
        	
        	String text = "";
        	
        	List<Map.Entry<String, Integer>> list = new ArrayList<Map.Entry<String, Integer>>(
        		    mapText.entrySet());
        	Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
                public int compare(Map.Entry<String, Integer> e1, Map.Entry<String, Integer> e2) {
                    return e2.getValue().compareTo(e1.getValue());
                }
            });
        	
            for (Map.Entry<String, Integer> entry : list) {
        		text = text + entry.getKey() + "---";
        	}
            
            text += String.valueOf(processtime) + "ms";
            tvLabel.setText(text);
            ivCaptured.setImageResource(0);
            Log.d(LOG_TAG, "Video done !!");
            finishPrediction();
            if (dialog != null) {
                dialog.dismiss();
            }
            super.onPostExecute(integer);
        }
    }
    
    
    private class cvExtractDetection extends AsyncTask<String, Void, Integer> {
        
        long processtime = 0;
        long extractime = 0;
        SortedMap<String, Integer> mapText = new TreeMap<String, Integer>();
        SortedMap<String, Integer> mapText2 = new TreeMap<String, Integer>();

        int n_frame_extracted = 0;
        int n_completed = 0;
        Mat mat;
        Vector<Mat> vecMat = new Vector<Mat>();
        long[] longArray1;
        long[] longArray2;


        @Override
        protected Integer doInBackground(String... strings) {

        	FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(strings[0]);
            try {
                long extract_start = System.currentTimeMillis();
                OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();
                grabber.start();
                int frame_count = grabber.getLengthInFrames();
                long length_time = grabber.getLengthInTime();
                double frame_rate = grabber.getFrameRate();

                int increment;
                if(db_fps == 0. || db_fps >= frame_rate)
                    increment = 1;
                else
                    increment = (int) (frame_rate/db_fps);

//                long time_incre = (long)(1000000/db_fps);
//                for(long i=0; i<length_time; i+=time_incre){
//                    if(i == 0){
//                        Frame frame = grabber.grabFrame(true,false,false);
//                        if(frame == null) break;
//                        if(frame.image == null) continue;
//                    }else{
//                        grabber.setTimestamp(i);
//                        Frame frame = grabber.grabFrame(true,false,false);
//                        if(frame == null) break;
//                        if(frame.image == null) continue;
//                    }
//                    n_frame_extracted++;
//                    Log.d(LOG_TAG, "Extracted picture " + String.valueOf(n_frame_extracted)
//                            +  " at " + String.valueOf(System.currentTimeMillis() - extract_start) + "ms");
//                }

                for(int i=0; i<frame_count; i+=increment){
                    if(i > 0 && increment > 1)
                        grabber.setFrameNumber(i);
//                    Frame frame = grabber.grabFrame(true,false,false);
                    Frame frame = grabber.grabImage();
                    if(frame == null) break;
                    if(frame.image == null) continue;
                    mat = converterToMat.convert(frame);
                    if(mat == null) continue;
//                    Size sz = new Size(256,256);
//                    resize(mat, mat, sz);
                    transpose(mat, mat);
                    flip(mat, mat, 1);
//                    vecMat.add(mat.clone());
                    n_frame_extracted++;
                    Log.d(LOG_TAG, "Extracted picture " + String.valueOf(n_frame_extracted)
                            +  " at " + String.valueOf(System.currentTimeMillis() - extract_start) + "ms");
                    String img_path = "/sdcard/Download/" + "frame-" + String.valueOf(i) +".jpg";
                    imwrite(img_path, mat);
//                    IplImage image = converterToIplImage.convert(frame);
//                    cvSaveImage(img_path, image);
//                    Log.d(LOG_TAG, "Saved picture " + String.valueOf(i/((int)frame_rate)) +  " !!");

//                    if(n_mode == 0) continue;
//                    long process_start = System.currentTimeMillis();
//                    int[] topK = caffeMobile.predictImageMat(mat.address(), n_topk);
//                    processtime += System.currentTimeMillis() - process_start;
//                    for(int j=0; j<n_topk; j++) {
//                        String strTemp = IMAGENET_CLASSES[topK[j]];
//                        if (null == mapText.get(strTemp))
//                            mapText.put(strTemp, 1);
//                        else
//                            mapText.put(strTemp, mapText.get(strTemp) + 1);
//                    }
//                    ++n_completed;

                }

                extractime = System.currentTimeMillis() - extract_start;

                if(n_mode != 0) {
//                    if(vecMat.size() < 1) return 1;
//                    if(vecMat.size() == 1){
//                        long [] longArray = new long[vecMat.size()];
//                        int[] topK = caffeMobile.predictImageMatArray(longArray, n_topk);
//                        for (int j = 0; j < vecMat.size(); j++) {
//                            for (int k = 0; k < n_topk; k++) {
//                                String strTemp = IMAGENET_CLASSES[topK[j * n_topk + k]];
//                                if (null == mapText.get(strTemp))
//                                    mapText.put(strTemp, 1);
//                                else
//                                    mapText.put(strTemp, mapText.get(strTemp) + 1);
//                            }
//                        }
//                    }
//                    if(vecMat.size() > 1) {
//                        longArray1 = new long[vecMat.size()/2];
//                        longArray2 = new long[vecMat.size()-vecMat.size()/2];
//                        for (int k = 0; k < vecMat.size(); k++) {
//                            if(k >= vecMat.size()/2)
//                                longArray2[k-vecMat.size()/2] = (vecMat.elementAt(k)).address();
//                            else
//                                longArray1[k] = (vecMat.elementAt(k)).address();
//                        }
//
//                        Thread thread1 = new Thread(new Runnable() {
//                            ReentrantLock lock = new ReentrantLock();
//                            int n_image = vecMat.size()/2;
//                            int n_class = n_topk;
//                            public void run() {
//                                long start1 = System.currentTimeMillis();
//                                int[] topK = caffeMobile.predictImageMatArray(longArray1, n_class);
//                                Log.d(LOG_TAG, "caffeMobile finished, duration " + String.valueOf(System.currentTimeMillis() - start1) + "ms.");
//                                lock.lock();
//                                for (int j = 0; j < n_image; j++) {
//                                    for (int k = 0; k < n_class; k++) {
//                                        String strTemp = IMAGENET_CLASSES[topK[j * n_class + k]];
//                                        if (null == mapText.get(strTemp))
//                                            mapText.put(strTemp, 1);
//                                        else
//                                            mapText.put(strTemp, mapText.get(strTemp) + 1);
//                                    }
//                                }
//                                n_completed += n_image;
//                                lock.unlock();
//                            }
//                        });
//                        thread1.start();
//
//                        Thread thread2 = new Thread(new Runnable() {
//                            ReentrantLock lock = new ReentrantLock();
//                            int n_image = vecMat.size() - vecMat.size()/2;
//                            int n_class = n_topk;
//                            public void run() {
//                                long start2 = System.currentTimeMillis();
//                                int[] topK = caffeSurrogate.predictImageMatArray(longArray2, n_class);
//                                Log.d(LOG_TAG, "caffeSurrogate finished, duration " + String.valueOf(System.currentTimeMillis() - start2) + "ms.");
//                                lock.lock();
//                                for (int j = 0; j < n_image; j++) {
//                                    for (int k = 0; k < n_class; k++) {
//                                        String strTemp = IMAGENET_CLASSES[topK[j * n_class + k]];
//                                        if (null == mapText2.get(strTemp))
//                                            mapText2.put(strTemp, 1);
//                                        else
//                                            mapText2.put(strTemp, mapText2.get(strTemp) + 1);
//                                    }
//                                }
//                                n_completed += n_image;
//                                lock.unlock();
//                            }
//                        });
//                        thread2.start();
//                    }
                    if(n_mode == 2) {
                        for (int i = 0; i < vecMat.size(); i++) {
                            long process_start = System.currentTimeMillis();
                            long[] longArray = new long[1];
                            longArray[0] = (vecMat.elementAt(i)).address();
                            int[] topK = caffeMobile.predictImageMatArray(longArray, n_topk);
                            processtime += System.currentTimeMillis() - process_start;
                            for (int j = 0; j < n_topk; j++) {
                                String strTemp = IMAGENET_CLASSES[topK[j]];
                                if (null == mapText.get(strTemp))
                                    mapText.put(strTemp, 1);
                                else
                                    mapText.put(strTemp, mapText.get(strTemp) + 1);
                            }
                        }
                    }
                    if(n_mode == 1) {
                        processtime = System.currentTimeMillis();
                        long[] longArray = new long[vecMat.size()];
                        for (int k = 0; k < vecMat.size(); k++) {
                            longArray[k] = (vecMat.elementAt(k)).address();
                        }
                        int[] topK = caffeMobile.predictImageMatArray(longArray, n_topk);
                        processtime = System.currentTimeMillis() - processtime;

                        for (int j = 0; j < vecMat.size(); j++) {
                            for (int k = 0; k < n_topk; k++) {
                                String strTemp = IMAGENET_CLASSES[topK[j * n_topk + k]];
                                if (null == mapText.get(strTemp))
                                    mapText.put(strTemp, 1);
                                else
                                    mapText.put(strTemp, mapText.get(strTemp) + 1);
                            }
                        }
                    }

                    processtime /= vecMat.size();
                    n_completed = n_frame_extracted;
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        
            return 1;
        }

        @Override
        protected void onPostExecute(Integer integer) {

            try {
                String text = "";

//                if(n_extract != 0) {
//                    while (n_completed != n_frame_extracted) {
//                    }
//                }

                for(Map.Entry<String,Integer> entry : mapText2.entrySet()) {
                    if (null == mapText.get(entry.getKey()))
                        mapText.put(entry.getKey(), entry.getValue());
                    else
                        mapText.put(entry.getKey(), mapText.get(entry.getKey()) + entry.getValue());
                }


                List<Map.Entry<String, Integer>> list = new ArrayList<Map.Entry<String, Integer>>(
                        mapText.entrySet());
                Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
                    public int compare(Map.Entry<String, Integer> e1, Map.Entry<String, Integer> e2) {
                        return e2.getValue().compareTo(e1.getValue());
                    }
                });

                for (Map.Entry<String, Integer> entry : list) {
                    text = text + entry.getKey() + "---";
                }

                if(n_mode != 0)
                    text += "ExtractTime " + String.valueOf(extractime)+ "ms---ProcessTime " +
                        String.valueOf(processtime) + "ms---Extracted and Processed "
                        + String.valueOf(n_frame_extracted) + " frames";
                else
                    text += "ExtractTime " + String.valueOf(extractime)+ "ms---Extracted "
                            + String.valueOf(n_frame_extracted) + " frames";

                tvLabel.setText(text);
                ivCaptured.setImageResource(0);
                Log.d(LOG_TAG, "Video done !!");
                finishPrediction();
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            super.onPostExecute(integer);

        }
    }
    
    private class cvExtractSend extends AsyncTask<String, Void, Integer> {
        
        long processtime;
        long extracttime;
        int[] topK;
        SortedMap<String, Integer> mapText = new TreeMap<String, Integer>();

        @Override
        protected Integer doInBackground(String... strings) {
        	
        	extracttime = System.currentTimeMillis();
        	
        	VideoCapture cap = new VideoCapture(strings[0]);
    		double framerate  =  cap.get(CV_CAP_PROP_FPS);
    		double framecount = cap.get(CV_CAP_PROP_FRAME_COUNT);
    		int cnt = 0;
    		while(cap.get(CV_CAP_PROP_POS_FRAMES) < framecount){
    			cap.grab();
    			if(cap.get(CV_CAP_PROP_POS_FRAMES) % framerate == 0){
    				Mat img = new Mat();
    				Mat resize_img = new Mat();
    				Size sz = new Size(256,256);
    				cap.retrieve(img);
    				resize(img, resize_img, sz);
    				String img_path = BIN_FOLDER_NAME + "frame-" + String.valueOf(cnt++) +".jpeg";
    				imwrite(img_path, resize_img);
    			}
    		}
    		
    		extracttime = System.currentTimeMillis() - extracttime;
        
            return 1;
        }

        @Override
        protected void onPostExecute(Integer integer) {
        	
        	String text = "";
            
            text += String.valueOf(extracttime) + "ms";
            tvLabel.setText(text);
            ivCaptured.setImageResource(0);
            Log.d(LOG_TAG, "Video done !!");
            finishPrediction();
            if (dialog != null) {
                dialog.dismiss();
            }
            super.onPostExecute(integer);
        }
    }
   
    
    private static boolean runCommand(String command) {
		try {
	    	Process process = Runtime.getRuntime().exec(command);
	    	return process.waitFor() == 0;
		} catch (Exception e) {
			return false;
		}
    }
    
    private static String runCommandGetOutput(String command) {
		String output = "";
    	try {
			Process process = Runtime.getRuntime().exec(command);
			
			// we must empty the output and error stream to end the process
			EmptyStreamThread emptyInputStreamThread = 
				new EmptyInputStreamThread(process.getInputStream());
			EmptyStreamThread emptyErrorStreamThread = 
				new EmptyErrorStreamThread(process.getErrorStream());
			emptyInputStreamThread.start();
			emptyErrorStreamThread.start();
			
			if (process.waitFor() == 0) {
				// System.out.println("Successfully executed: " + command);
				emptyInputStreamThread.join();
				emptyErrorStreamThread.join();
				output = emptyErrorStreamThread.getOutput(); // DEBUG
				output = emptyInputStreamThread.getOutput();
			} else {
				System.err.println(emptyErrorStreamThread.getOutput());
				System.err.println(emptyInputStreamThread.getOutput());
			}
			
			// close streams
			process.getOutputStream().close();
			process.getInputStream().close();
			process.getErrorStream().close(); 
		} catch (Exception e) {
			e.printStackTrace();
		}
		return output;
	}

    /** Create a file Uri for saving an image or video */
    private static Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Caffe-App");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()) {
            if (! mediaStorageDir.mkdirs()) {
                Log.d("CaffeApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else {
            return null;
        }

        return mediaFile;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @SuppressLint("ShowToast")
	public void copyFile(String oldPath, int num, String newPath) {   
        try {   
            int bytesum = 0;   
            int byteread = 0;      
            File oldfile = new File(oldPath);    
            InputStream inStream = getResources().getAssets().open(oldPath); 		// input xml in assests folder
            FileOutputStream fs = new FileOutputStream(newPath);   
            byte[] buffer = new byte[1444];   
            while ( (byteread = inStream.read(buffer)) != -1) {   
                bytesum += byteread; 				// file size    
                fs.write(buffer, 0, byteread);   
            }   
            inStream.close();
            fs.close();
        }   
        catch (Exception e) {   
            e.printStackTrace(); 
        }   
    }
    
    private static abstract class EmptyStreamThread extends Thread {
		
		private InputStream istream = null;
		private CircularStringBuffer buff = new CircularStringBuffer();
		
		public EmptyStreamThread(InputStream istream) {
			this.istream = istream;
		}
		
		public String getOutput() {
			return buff.toString().trim();
		}
		
		protected abstract void handleLine(String line);
		
		public void run() {
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(istream));
				String line = null;
				while ((line = reader.readLine()) != null) { 
					handleLine(line);
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					istream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private static class EmptyInputStreamThread extends EmptyStreamThread {
		
		public EmptyInputStreamThread(InputStream istream) {
			super(istream);
		}
		
		protected void handleLine(String line) {
			super.buff.append(line).append("\n");
			// Log.d(TAG, "OUTPUT: " + line); // DEBUG
		}
	}
	
	private static class EmptyErrorStreamThread extends EmptyStreamThread {
		
		public EmptyErrorStreamThread(InputStream istream) {
			super(istream);
		}
		
		protected void handleLine(String line) {
			super.buff.append(line).append("\n");
			// Log.d(TAG, "ERROR: " + line); // DEBUG
		}
	}

}
