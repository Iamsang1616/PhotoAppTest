package sarin_i.photoapptest;

import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity{

    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private ImageView imageView;
    



    private Button btnCapture;
    private Size previewSize;



    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d("onCreate","CREATING THE INSTANCE NOW" );
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);


        imageView = findViewById(R.id.imageView);
        btnCapture = (Button) findViewById(R.id.capture_button);


        btnCapture.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v){
                Log.i("captureBtn", "CAPTURE BUTTON PRESSED");
                onCaptureButtonClick();
            }
        });
    }

    private void onCaptureButtonClick() {
        dispatchTakePictureIntent();
    }


    private void dispatchTakePictureIntent(){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null){
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK){
            Bundle extras = data.getExtras();
            Bitmap imageBitMap = (Bitmap) extras.get("data");
            imageView.setImageBitmap(imageBitMap);
        }
    }


}
