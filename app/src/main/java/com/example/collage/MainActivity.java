package com.example.collage;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MAIN_ACTIVITY";

    private ImageButton mImageButton1, mImageButton2, mImageButton3, mImageButton4;

    // Store image buttons and file paths in lists
    private List<ImageButton> mImageButtons;
    private ArrayList<String> mImageFilePaths;

    private String mCurrentImagePath;

    // Bundle keys
    private final static String BUNDLE_KEY_IMAGE_FILE_PATHS = "bundle key image file paths";
    private final static String BUNDLE_KEY_MOST_RECENT_FILE_PATH = "bundle key most recent file path";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Finds ImageButtons by resource ID
        mImageButton1 = findViewById(R.id.imageButton1);
        mImageButton2 = findViewById(R.id.imageButton2);
        mImageButton3 = findViewById(R.id.imageButton3);
        mImageButton4 = findViewById(R.id.imageButton4);

        mImageButtons = new ArrayList<>(Arrays.asList(mImageButton1, mImageButton2, mImageButton3, mImageButton4));

        // Click listener set for all image buttons
        for (ImageButton button: mImageButtons) {
            button.setOnClickListener(this);
        }

        // Restores image file paths from Bundle key
        if (savedInstanceState != null) {
            mCurrentImagePath = savedInstanceState.getString(BUNDLE_KEY_MOST_RECENT_FILE_PATH);
            mImageFilePaths = savedInstanceState.getStringArrayList(BUNDLE_KEY_IMAGE_FILE_PATHS);
        }

        if (mCurrentImagePath == null) {
            mCurrentImagePath = "";
        }

        if (mImageFilePaths == null) {
            mImageFilePaths = new ArrayList<>(Arrays.asList( "", "", "", ""));
        }

    }

    // Saves current image file path(s)
    @Override
    public void onSaveInstanceState(Bundle outBundle) {
        super.onSaveInstanceState(outBundle);
        outBundle.putString(BUNDLE_KEY_MOST_RECENT_FILE_PATH, mCurrentImagePath);
        outBundle.putStringArrayList(BUNDLE_KEY_IMAGE_FILE_PATHS, mImageFilePaths);
    }

    @Override
    public void onClick(View view) {
        // Handle ImageButton click

        // Uses position of image buttons in list for requestCode
        int requestCodeButtonIndex = mImageButtons.indexOf(view);

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            try {
                File imageFile = createImageFile();
                if (imageFile != null) {
                    // Uri = path to file on your device
                    Uri imageURI = FileProvider.getUriForFile(this, "com.example.collage.fileprovider", imageFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageURI);
                    startActivityForResult(takePictureIntent, requestCodeButtonIndex);
                } else {
                    Log.e(TAG, "Image file is null");
                }
            } catch (IOException e) {
                Log.e(TAG, "Error creating image file " + e);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create unique filename with timestamp
        String imageFilename = "COLLAGE_" + new Date().getTime();
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(
                imageFilename,
                ".jpg",
                storageDir
        );

        // Save the file path globally, when the take picture Intent returns
        // this location will be where the image is saved
        mCurrentImagePath = imageFile.getAbsolutePath();
        return imageFile;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Log.d(TAG, "onActivityResult for request code " + requestCode +
                    " and current path " + mCurrentImagePath);
            // Sets code to be consistent with position of image clicked
            mImageFilePaths.set(requestCode, mCurrentImagePath);
            requestSaveImageToMediaStore();
        }

        else if (resultCode == RESULT_CANCELED) {
            mCurrentImagePath = "";
        }
    }

    // A lifecycle method called when device is rotated but AFTER UI has been inflated
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        // The view hasn't been loaded in onActivityResult if the device
        // is rotated when the picture is taken.
        // onWindowsFocusChanged is called after the onActivityResult
        // and after the view has loaded, so override this
        // method to display the image in the ImageView.

        Log.d(TAG, "focus changed " + hasFocus);
        if (hasFocus) {
            // Loads all four image buttons
            for (int index = 0; index < mImageButtons.size() ; index++) {
                loadImage(index);
            }
        }
    }

    private void loadImage(int index) {

        // Image's list index is passed in for loading
        ImageButton imageButton = mImageButtons.get(index);
        // Image's list index used to fetch path
        String path = mImageFilePaths.get(index);

        if (path != null && !path.isEmpty()) {
            Picasso.get()
                    .load(new File(path))
                    .error(android.R.drawable.stat_notify_error) // built-in error icon
                    .fit()
                    .centerCrop() // completely fills image to screen size
                    .into(imageButton, new Callback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Image loaded");
                        }

                        // If images are not loading...
                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "error loading image", e);
                        }
                    });

        }
    }

    // Methods to request permission from user before inserting
    private void requestSaveImageToMediaStore() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED) {
            saveImage();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            saveImage();
        } else {
            Toast.makeText(this, "Images will NOT be saved to media store", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveImage() {
        try {
            MediaStore.Images.Media.insertImage(getContentResolver(), mCurrentImagePath, "Collage", "Collage");
        } catch (IOException e) {
            Log.e(TAG, "Image file not found", e);
        }
    }
}
