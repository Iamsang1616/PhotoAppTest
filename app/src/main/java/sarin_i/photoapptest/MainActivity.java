package sarin_i.photoapptest;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity{

    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private ImageView imageView;
    
    private ExifInterface exifInterface;


    private Button btnCapture;
    private Size previewSize;


    private String currentImagePath;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d("onCreate","CREATING THE INSTANCE NOW" );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= 23){
            ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2
            );
        }

        btnCapture = (Button) findViewById(R.id.capture_button);
        btnCapture.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v){
                Log.i("captureBtn", "CAPTURE BUTTON PRESSED");
                //Make the picture intent as seen below
                dispatchTakePictureIntent();
            }
        });

        imageView = findViewById(R.id.imageView);

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK){ Log.d("activityResult", "CAMERA CAME BACK ALRIGHT");

            Bitmap imageBitMap = null;
            //File imgFile = new File(currentImagePath);
            //if (imgFile.exists()){
            imageBitMap = BitmapFactory.decodeFile(currentImagePath);

            //}

            try {
                exifInterface = new ExifInterface(currentImagePath);

                int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);

            } catch (IOException e) {
                e.printStackTrace();
            }


            imageView.setImageBitmap(imageBitMap);
        }
    }




    private void dispatchTakePictureIntent() {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        //Make sure there's a camera
        if (takePictureIntent.resolveActivity(getPackageManager()) != null){

            try{
                Log.d("dispatchIntent", "CREATING IMAGE FILE");
                //Make a file object for the photo
                File photoFile = null;
                photoFile = createImageFile();

                //Get the photo's URI
                if (photoFile != null){
                    Log.d("dispatchIntent", "FILE NOT NULL");


                    currentImagePath = photoFile.getAbsolutePath();
                    Uri photoURI = FileProvider.getUriForFile(this, "sarin_i.photoapptest.fileprovider", photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

                    Log.d("dispatchIntent", "STARTING CAMERA APP");
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }

            } catch (IOException e){
                Log.d("dispatchIntent", "ERROR MAKING IMAGE FILE");
                e.printStackTrace();
            }

        }
    }

    //Make the name of the file so that it doesn't collide with any existing photo in the storage
    private File createImageFile() throws IOException{
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageName = "JPEG_" + timeStamp + "_";

        File image = null;

        //Get a directory to the public storage so other apps can look at the images?
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        //Get a directory to a special folder to keep images private to the app
        //File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        Log.d("imageFile creation", "THE DIRECTORY IS: " + storageDir);

        //Make a temporary file to store in there
        image = File.createTempFile(imageName, ".jpg", storageDir);

        return image;
    }


}
