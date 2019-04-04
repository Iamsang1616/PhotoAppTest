package sarin_i.photoapptest;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity{

    private Button btnCapture;
    private TextureView textureView;

    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private String cameraID;

    private Handler backgroundHandler;
    private HandlerThread backgroundThread;

    private CaptureRequest.Builder captureRequestBuilder;
    private CameraCaptureSession cameraSession;


    private TextureView.SurfaceTextureListener surfaceTextureListener;
    private int cameraFacing;
    private Size previewSize;

    private CameraDevice.StateCallback stateCallback;

    private CaptureRequest captureRequest;
    private File galleryFolder;


    //Do this when the app is being made/built
    @Override
    public void onCreate(Bundle savedInstanceState) {

        Log.d("onCreate","CREATING THE INSTANCE NOW" );

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);

        
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        cameraFacing = CameraCharacteristics.LENS_FACING_BACK;

        textureView = findViewById(R.id.textureView);
        btnCapture = (Button) findViewById(R.id.capture_button);



        //Make a new StateCallBack
        stateCallback = new CameraDevice.StateCallback() {

            //When the state is opened, reassign the camera and make the preview session
            @Override
            public void onOpened(@NonNull CameraDevice cameraDevice) {
                MainActivity.this.cameraDevice = cameraDevice;
                createPreviewSession();
                createGallery();
            }

            //When the state is disconnected or errors out, close the camera and deassign the cameraDevice
            @Override
            public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                cameraDevice.close();
                MainActivity.this.cameraDevice = null;
            }

            @Override
            public void onError(@NonNull CameraDevice cameraDevice, int i) {
                cameraDevice.close();
                MainActivity.this.cameraDevice = null;
            }
        };



        //Among other things, listen for the TextureView to be loaded before starting the camera
        surfaceTextureListener = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
                setUpCamera();
                openCamera();

            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

            }
        };




        btnCapture.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v){
                Log.i("captureBtn", "CAPTURE BUTTON PRESSED");
                onCaptureButtonClick();
            }
        });
    }

    //Do this when the capture button is pressed
    private void onCaptureButtonClick() {
        //Make a file output stream
        FileOutputStream outputPhotoStream = null;

        //Attempt to make a new picture using the preview uh... view.
        try{
            outputPhotoStream = new FileOutputStream(createImageFile(galleryFolder));
            textureView.getBitmap().compress(Bitmap.CompressFormat.PNG, 100, outputPhotoStream);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {

            try{
                if(outputPhotoStream != null){
                    outputPhotoStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }



    }

    //Make the public directory to store all the images in
    private void createGallery() {
        File photoDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        galleryFolder = new File(photoDirectory, getResources().getString(R.string.app_name));

        if (!galleryFolder.exists()){
            boolean galleryExists = galleryFolder.mkdirs();
            if (!galleryExists){
                Log.e("createGallery", "ERROR: FAILED TO MAKE DIRECTORY");

            }
            else{
                Log.d("createGallery", "DIRECTORY HAS BEEN MADE");
            }
        }
    }

    //Assemble an image file using a timestamp, return to calling function
    private File createImageFile(File galleryFolder) throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "image_" + timestamp + "_";

        File image = null;

        try{
            image = File.createTempFile(imageFileName, ".jpg", this.galleryFolder);
        }
        catch (IOException e) {
            Log.e("createImageFile", "ERROR: COULD NOT MAKE IMAGE FILE");
        }


        return image;
    }

    //Things to do when the app resumes from suspension
    @Override
    public void onResume() {

        Log.d("onResume","====NOW RESUMING=====" );
        super.onResume();

        //open the background thread for operations
        openBackgroundThread();

        //Reassign the textureView to display the preview to if needed
        if (textureView.isAvailable()) {

            Log.d("onResume","FOUND THE TEXTUREVIEW" );


            setUpCamera();
            openCamera();
        } else {
            Log.d("onResume","REASSIGNING TEXTUREVIEW" );
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    public void onStop() {
        Log.d("onStop","=====APPLICATION HAS STOPPED=====" );
        super.onStop();

        closeCamera();
        closeBackgroundThread();
    }

    //check permission to use the camera, then open it
    private void openCamera() {

        Log.d("openCamera","=====OPENING CAMERA=====" );
        try{
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                cameraManager.openCamera(cameraID, stateCallback, backgroundHandler);
            }
        }
        catch(CameraAccessException a){
            a.printStackTrace();
        }
    }

    private void closeCamera() {
        if (cameraSession != null) {
            cameraSession.close();
            cameraSession = null;
        }

        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    //Making a background thread for operations to not overburden the main thread, also a Looper
    private void openBackgroundThread() {
        backgroundThread = new HandlerThread("camera_background_thread");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void closeBackgroundThread() {
        if (backgroundHandler != null) {
            backgroundThread.quitSafely();
            backgroundThread = null;
            backgroundHandler = null;
        }
    }



    //Get the backward-facing camera from the device
    private void setUpCamera() {
        try{
            for(String cameraID : cameraManager.getCameraIdList()){
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraID);

                if (characteristics.get(CameraCharacteristics.LENS_FACING) == cameraFacing){
                    StreamConfigurationMap SCM = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    assert SCM != null;

                    //get the highest resolution output
                    previewSize = SCM.getOutputSizes(SurfaceTexture.class)[0];
                    this.cameraID = cameraID;
                }
            }
        }
        catch(CameraAccessException a){
            a.printStackTrace();
        }
    }

    private void createPreviewSession() {
        try {
            SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface previewSurface = new Surface(surfaceTexture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(previewSurface);

            cameraDevice.createCaptureSession(Collections.singletonList(previewSurface),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                            if (cameraDevice == null) {
                                return;
                            }

                            try {
                                captureRequest = captureRequestBuilder.build();
                                MainActivity.this.cameraSession = cameraCaptureSession;
                                MainActivity.this.cameraSession.setRepeatingRequest(captureRequest,
                                        null, backgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {

                        }
                    }, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

}
