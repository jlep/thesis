package fi.hiit.complesense.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import fi.hiit.complesense.Constants;
import fi.hiit.complesense.R;

/**
 * Created by hxguo on 24.11.2014.
 */
public class TakePhotoActivity extends Activity
{
    private final static String TAG = TakePhotoActivity.class.getSimpleName();
    public static final String IMAGE_NAMES = "image_names";
    private Camera mCamera;
    private int mCameraId = 0;
    private Button mButton;
    private CameraPreview preview;
    private int imgCount;
    private File localDir;
    private ArrayList<String> imageNames = new ArrayList<String>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_preview);
        preview = new CameraPreview(this);
        ((FrameLayout) findViewById(R.id.camera_preview)).addView(preview);

        mButton = (Button)findViewById(R.id.front_take_photos);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                preview.mCamera.takePicture(shutterCallback, rawCallback,
                        jpegCallback);
                mButton.setEnabled(false);
            }
        });

        localDir = new File(Constants.ROOT_DIR, Constants.LOCAL_SENSOR_DATA_DIR);
    }

    /** Check if this device has a camera */
    private boolean checkCameraHardware() {
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            mCameraId = findBackFacingCamera();
            if (mCameraId < 0) {
                Toast.makeText(this, "No front facing camera found.",
                        Toast.LENGTH_LONG).show();
            } else {
                mCamera = Camera.open(mCameraId);
            }
            return true;
        } else {
            // no camera on this device
            Toast.makeText(this, "No camera on this device", Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private int findBackFacingCamera() {
        int cameraId = -1;
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            CameraInfo info = new CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
                Log.d(TAG, "Camera found");
                cameraId = i;
                break;
            }
        }
        return cameraId;
    }

    @Override
    protected void onPause() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
        super.onPause();
    }

    Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
        public void onShutter() {
            Log.d(TAG, "onShutter'd");
        }
    };

    Camera.PictureCallback rawCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.i(TAG, "onPictureTaken - raw with data = " + ((data != null) ? data.length : " NULL"));
        }
    };


    Camera.PictureCallback jpegCallback = new Camera.PictureCallback()
    {
        public void onPictureTaken(byte[] data, Camera camera) {
            FileOutputStream outStream = null;
            String fname = String.format("%d.jpg", System.currentTimeMillis());
            File imgFile = new File(localDir, fname);

            try {
                // write to local sandbox file system
                imageNames.add(fname);

                outStream = new FileOutputStream(imgFile);
                outStream.write(data);
                outStream.close();

                Log.i(TAG, "onPictureTaken - wrote bytes: " + data.length);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
            }
            Log.i(TAG, "onPictureTaken - jpeg");
            try {
                imgCount++;
                camera.startPreview();
                if (imgCount < Constants.NUM_IMG_TAKE) {
                    preview.mCamera.takePicture(shutterCallback, rawCallback,
                            jpegCallback);
                } else {
                    imgCount = 0;
                    mButton.setEnabled(true);

                    //Log.i(TAG, "imageNames: " + imageNames);
                    Intent intent = new Intent();
                    intent.putStringArrayListExtra(IMAGE_NAMES, imageNames);
                    setResult(Activity.RESULT_OK, intent);
                    finish();
                }
            } catch (Exception e) {
                Log.d(TAG, "Error starting preview: " + e.toString());
            }
        }
    };
}