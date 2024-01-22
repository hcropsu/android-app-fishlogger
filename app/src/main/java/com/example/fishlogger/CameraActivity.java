package com.example.fishlogger;


import android.content.ContentValues;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.core.ViewPort;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import com.example.fishlogger.databinding.ActivityCameraBinding;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// This activity has a PreviewView for taking a picture and a button that takes the picture. It uses
// CameraX. To be honest using the existing cameraApi would probably lead to a better UX, but since
// this is an exercise app I chose to go this route...
public class CameraActivity extends AppCompatActivity implements FishRepository.OnFishUpdatedListener{
    private @NonNull ActivityCameraBinding viewBinding;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private static final String TAG = "FishLoggerApp";
    private Fish imageFish;
    private String fishId;
    private FishRepository fishRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fishRepository = new FishRepository();

        // Check that an existing Fish was passed when launching this activity. Save that
        // Fish to a variable and save also the unique identifier fishId for naming the picture
        // that will be taken later.
        if (getIntent().hasExtra("latestFish")){
            imageFish = getIntent().getParcelableExtra("latestFish");
            fishId = imageFish.getFishId();
        }
        viewBinding = ActivityCameraBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());
        // Start the camera
        startCamera();

        // Set up the listeners for take photo button
        viewBinding.imageCaptureButton.setOnClickListener(view -> takePhoto());
        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        ImageCapture imageCapture = this.imageCapture;
        if (imageCapture == null) return;
        // Set the fishId as 'name' which will be the image file's name
        String name = fishId;
        // Put the name, file type and file path
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FishLogger");
        }

        // Create output options object which contains file + metadata
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(
                getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
                .build();

        // Set up image capture listener, which is triggered after photo has been taken
        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        // Update fish with new value of boolean hasCustomImage and call updateFish()
                        // with this activity as the listener, then close this activity
                        imageFish.setHasCustomImage(true);
                        fishRepository.updateFish(imageFish, CameraActivity.this);
                        CameraActivity.this.finish();
                    }

                    // Log error if picture capture fails
                    @Override
                    public void onError(@NonNull ImageCaptureException exc) {
                        Log.e(TAG, "Photo capture failed: " + exc.getMessage(), exc);
                    }
                }
        );
    }


    // This function starts the camera
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();


                // Preview
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewBinding.viewFinder.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder().build();

                // For WYSIWYG images, i.e. rectangles in this case. This I had to add myself
                // using the documentation as help
                ViewPort viewPort = ((PreviewView) findViewById(R.id.viewFinder)).getViewPort();
                UseCaseGroup useCaseGroup = new UseCaseGroup.Builder()
                        .addUseCase(preview)
                        .addUseCase(imageCapture)
                        .setViewPort(viewPort).build();

                // Select back camera as a default
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                // Unbind use cases before rebinding
                cameraProvider.unbindAll();

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                        this, cameraSelector, useCaseGroup);

            } catch (Exception exc) {
                Log.e(TAG, "Use case binding failed", exc);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // Stop the cameraExecutor
    @Override
    public void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }


    // Override the onFishUpdated(): show a toast when picture capture succeeds.
    @Override
    public void onFishUpdated() {
        Toast.makeText(this, "Picture saved.", Toast.LENGTH_SHORT).show();
    }
}
