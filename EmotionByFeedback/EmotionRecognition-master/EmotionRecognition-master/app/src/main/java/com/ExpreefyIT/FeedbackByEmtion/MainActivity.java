package com.ExpreefyIT.FeedbackByEmtion;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.ExpreefyIT.FeedbackByEmtion.classifiers.TFLiteImageClassifier;
import com.ExpreefyIT.FeedbackByEmtion.utils.ImageUtils;
import com.ExpreefyIT.FeedbackByEmtion.utils.SortingHelper;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// contribution
public class MainActivity extends AppCompatActivity {

    private static final int GALLERY_REQUEST_CODE = 0;
    private static final int TAKE_PHOTO_REQUEST_CODE = 1;

    private TFLiteImageClassifier mClassifier;

    private ProgressBar mClassificationProgressBar;

    private ImageView mImageView;

    private ImageButton mPickImageButton;
    private ImageButton mTakePhotoButton;
    private ExpandableListView mClassificationExpandableListView;

    private Uri mCurrentPhotoUri;

    int yesEnjoyed = 0;
    int notEnjoyed = 0;


    private Map<String, List<Pair<String, String>>> mClassificationResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mClassificationProgressBar = findViewById(R.id.classification_progress_bar);

        String MODEL_FILE_NAME = "simple_classifier.tflite";
        mClassifier = new TFLiteImageClassifier(
                this.getAssets(),
                MODEL_FILE_NAME,
                getResources().getStringArray(R.array.emotions));

        mClassificationResult = new LinkedHashMap<>();

        mImageView = findViewById(R.id.image_view);

        mPickImageButton = findViewById(R.id.pick_image_button);
        mPickImageButton.setOnClickListener(v -> pickFromGallery());

        mTakePhotoButton = findViewById(R.id.take_photo_button);
        mTakePhotoButton.setOnClickListener(v -> takePhoto());

        mClassificationExpandableListView = findViewById(R.id.classification_expandable_list_view);


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mClassifier.close();

        File picturesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        for (File tempFile : Objects.requireNonNull(picturesDir.listFiles())) {
            //noinspection ResultOfMethodCallIgnored
            tempFile.delete();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                // When an image from the gallery was successfully selected
                case GALLERY_REQUEST_CODE:
                    clearClassificationExpandableListView();
                    // We can immediately get an image URI from an intent data
                    Uri pickedImageUri = data.getData();
                    processImageRequestResult(pickedImageUri);
                    break;
                // When a photo was taken successfully
                case TAKE_PHOTO_REQUEST_CODE:
                    clearClassificationExpandableListView();
                    processImageRequestResult(mCurrentPhotoUri);
                    break;
                default:
                    break;
            }
        }
    }

    private void clearClassificationExpandableListView() {
        Map<String, List<Pair<String, String>>> emptyMap = new LinkedHashMap<>();
        ClassificationExpandableListAdapter adapter =
                new ClassificationExpandableListAdapter(emptyMap);

        mClassificationExpandableListView.setAdapter(adapter);
    }

    // Function to handle successful new image acquisition
    private void processImageRequestResult(Uri resultImageUri) {
        Bitmap scaledResultImageBitmap = getScaledImageBitmap(resultImageUri);

        mImageView.setImageBitmap(scaledResultImageBitmap);

        // Clear the result of a previous classification
        mClassificationResult.clear();

        setCalculationStatusUI();

        detectFaces();
    }

    // Function to create an intent to take an image from the gallery
    private void pickFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, GALLERY_REQUEST_CODE);
    }

    // Function to create an intent to take a photo
    @SuppressLint("QueryPermissionsNeeded")
    private void takePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Make sure that there is activity of the camera that processes the intent
        if (intent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (photoFile != null) {
                mCurrentPhotoUri = FileProvider.getUriForFile(
                        this,
                        BuildConfig.APPLICATION_ID + ".fileprovider",
                        photoFile);

                intent.putExtra(MediaStore.EXTRA_OUTPUT, mCurrentPhotoUri);
                startActivityForResult(intent, TAKE_PHOTO_REQUEST_CODE);
            }
        }
    }

    private Bitmap getScaledImageBitmap(Uri imageUri) {
        Bitmap scaledImageBitmap = null;

        try {
            Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(
                    this.getContentResolver(),
                    imageUri);

            int scaledHeight;
            int scaledWidth;

            // How many times you need to change the sides of an image
            float scaleFactor;

            // Get larger side and start from exactly the larger side in scaling
            int SCALED_IMAGE_BIGGEST_SIZE = 480;
            if (imageBitmap.getHeight() > imageBitmap.getWidth()) {
                scaledHeight = SCALED_IMAGE_BIGGEST_SIZE;
                scaleFactor = scaledHeight / (float) imageBitmap.getHeight();
                scaledWidth = (int) (imageBitmap.getWidth() * scaleFactor);

            } else {
                scaledWidth = SCALED_IMAGE_BIGGEST_SIZE;
                scaleFactor = scaledWidth / (float) imageBitmap.getWidth();
                scaledHeight = (int) (imageBitmap.getHeight() * scaleFactor);
            }

            scaledImageBitmap = Bitmap.createScaledBitmap(
                    imageBitmap,
                    scaledWidth,
                    scaledHeight,
                    true);

            // An image in memory can be rotated
            scaledImageBitmap = ImageUtils.rotateToNormalOrientation(
                    getContentResolver(),
                    scaledImageBitmap,
                    imageUri);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return scaledImageBitmap;
    }

    private void detectFaces() {


    }

    @SuppressLint("SetTextI18n")
    private void classifyEmotions(Bitmap imageBitmap, int faceId) {
        Map<String, Float> result = mClassifier.classify(imageBitmap, true);

        // Sort by increasing probability
        LinkedHashMap<String, Float> sortedResult =
                (LinkedHashMap<String, Float>) SortingHelper.sortByValues(result);

        ArrayList<String> reversedKeys = new ArrayList<>(sortedResult.keySet());
        // Change the order to get a decrease in probabilities
        Collections.reverse(reversedKeys);

        ArrayList<Pair<String, String>> faceGroup = new ArrayList<>();
        for (String key : reversedKeys) {
            @SuppressLint("DefaultLocale") String percentage = String.format("%.1f%%",
                    sortedResult.get(key) * 100);
            faceGroup.add(new Pair<>(key, percentage));
        }

        String groupName = getString(R.string.face) + " " + faceId;
        mClassificationResult.put(groupName, faceGroup);

        yesEnjoyed = 0;
        notEnjoyed = 0;
        for (String k : mClassificationResult.keySet()) {


            List<Pair<String, String>> data = mClassificationResult.get(k);
            assert data != null;
            for (Pair<String, String> i : data) {
                if (i.first.equalsIgnoreCase("happy") || i.first.equalsIgnoreCase("neutral") || i.first.equalsIgnoreCase("surprise")) {
                    String j = i.second.replace("%", "");
                    j = j.replace(" ", "");
                    yesEnjoyed = yesEnjoyed + (int) Float.parseFloat(j);
                } else {
                    String j = i.second.replace("%", "");
                    j = j.replace(" ", "");
                    notEnjoyed = notEnjoyed + (int) Float.parseFloat(j);
                }
            }
            TextView responses = findViewById(R.id.messege);

            int finalResult = (yesEnjoyed / (yesEnjoyed + notEnjoyed)) * 100;

            Log.d("hello !!", Integer.toString(yesEnjoyed));

            if (finalResult < 35) {
                responses.setText("Result : " + "" + finalResult);
            } else if (finalResult < 70) {
                responses.setText("Result : " + (finalResult));
            } else if (finalResult < 100) {
                responses.setText("Result : " + (finalResult));
            }
        }


    }

    // Create a temporary file for the image
    private File createImageFile() throws IOException {
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "ER_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        return File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
    }

    //Change the interface depending on the status of calculations
    private void setCalculationStatusUI() {
        mClassificationProgressBar.setVisibility(ProgressBar.VISIBLE);
        mTakePhotoButton.setEnabled(false);
        mPickImageButton.setEnabled(false);
    }
}

