package com.example.itemreminderapp;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

public class AddItemActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_LOCATION_PERMISSION = 101;
    private static final int REQUEST_IMAGE_CAPTURE = 102;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 103;

    private ImageView photoPreviewImageView;
    private EditText itemNameEditText;
    private EditText notesEditText;
    private Spinner categorySpinner;
    private TextView locationStatusTextView;
    private TextView reminderTextView;
    private ProgressBar progressBar;
    private Button saveButton;

    private Uri photoUri;
    private double latitude = 0.0;
    private double longitude = 0.0;
    private boolean hasLocation = false;
    private boolean hasReminder = false;
    private long reminderTimeMillis = 0;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (auth.getCurrentUser() == null) {
            openLogin();
            return;
        }
        FirebaseUtils.ensureUserProfileExists();

        setContentView(R.layout.activity_add_item);

        photoPreviewImageView = findViewById(R.id.photoPreviewImageView);
        itemNameEditText = findViewById(R.id.itemNameEditText);
        notesEditText = findViewById(R.id.notesEditText);
        categorySpinner = findViewById(R.id.categorySpinner);
        locationStatusTextView = findViewById(R.id.locationStatusTextView);
        reminderTextView = findViewById(R.id.reminderTextView);
        progressBar = findViewById(R.id.progressBar);
        saveButton = findViewById(R.id.saveButton);
        Button takePhotoButton = findViewById(R.id.takePhotoButton);
        Button setReminderButton = findViewById(R.id.setReminderButton);
        Button removeReminderButton = findViewById(R.id.removeReminderButton);

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                FirebaseUtils.CATEGORIES);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        takePhotoButton.setOnClickListener(v -> checkCameraPermissionAndOpenCamera());
        setReminderButton.setOnClickListener(v -> showReminderPickers());
        removeReminderButton.setOnClickListener(v -> clearReminder());
        saveButton.setOnClickListener(v -> saveItem());

        checkLocationPermissionAndLoadLocation();
        updateReminderText();
    }

    private void checkCameraPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    private void checkLocationPermissionAndLoadLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            loadLastKnownLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        }
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, "No camera app found.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File imageFile = File.createTempFile("findit_", ".jpg", getExternalCacheDir());
            photoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", imageFile);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            cameraIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);
        } catch (IOException exception) {
            Toast.makeText(this, "Could not prepare photo file.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadLastKnownLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager == null) {
            setLocationUnavailable();
            return;
        }

        try {
            Location location = null;
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
            if (location == null && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            if (location != null) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                hasLocation = true;
                locationStatusTextView.setText("Location found");
            } else {
                setLocationUnavailable();
            }
        } catch (SecurityException exception) {
            setLocationUnavailable();
        }
    }

    private void setLocationUnavailable() {
        hasLocation = false;
        latitude = 0.0;
        longitude = 0.0;
        locationStatusTextView.setText("Location unavailable");
    }

    private void showReminderPickers() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            new TimePickerDialog(this, (timeView, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                reminderTimeMillis = calendar.getTimeInMillis();
                hasReminder = reminderTimeMillis > System.currentTimeMillis();
                if (!hasReminder) {
                    reminderTimeMillis = 0;
                    Toast.makeText(this, "Choose a future reminder time.", Toast.LENGTH_SHORT).show();
                }
                updateReminderText();
                requestNotificationPermissionIfNeeded();
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void clearReminder() {
        hasReminder = false;
        reminderTimeMillis = 0;
        updateReminderText();
    }

    private void updateReminderText() {
        if (hasReminder) {
            reminderTextView.setText("Reminder: " + FirebaseUtils.formatTimestamp(reminderTimeMillis));
        } else {
            reminderTextView.setText("Reminder: none");
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATION_PERMISSION);
        }
    }

    private void saveItem() {
        String itemName = itemNameEditText.getText().toString().trim();
        String notes = notesEditText.getText().toString().trim();
        String category = String.valueOf(categorySpinner.getSelectedItem());

        if (itemName.isEmpty()) {
            Toast.makeText(this, "Please enter an item name.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (photoUri == null) {
            Toast.makeText(this, "Please take a photo before saving.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please sign in again.", Toast.LENGTH_LONG).show();
            openLogin();
            return;
        }

        setSaving(true);

        DocumentReference documentReference = db.collection("items").document();
        String itemId = documentReference.getId();

        String localImagePath;
        try {
            byte[] imageBytes = createCompressedImageBytes(photoUri);
            localImagePath = saveImageBytesToInternalStorage(imageBytes, currentUser.getUid(), itemId);
        } catch (IOException exception) {
            setSaving(false);
            Toast.makeText(this, "Photo could not be prepared. Please take it again.", Toast.LENGTH_LONG).show();
            return;
        }

        saveFirestoreItem(
                documentReference,
                itemId,
                itemName,
                notes,
                localImagePath,
                currentUser,
                category);
    }

    private void saveFirestoreItem(DocumentReference documentReference, String itemId, String itemName,
                                   String notes, String localImagePath,
                                   FirebaseUser currentUser, String category) {
        long timestamp = System.currentTimeMillis();
        Item item = new Item(
                itemId,
                itemName,
                notes,
                localImagePath,
                currentUser.getUid(),
                currentUser.getEmail() == null ? "" : currentUser.getEmail(),
                latitude,
                longitude,
                hasLocation,
                timestamp,
                FirebaseUtils.formatTimestamp(timestamp),
                category,
                hasReminder,
                reminderTimeMillis);

        documentReference.set(item)
                .addOnSuccessListener(unused -> {
                    if (hasReminder) {
                        ReminderScheduler.scheduleReminder(this, itemId, itemName, reminderTimeMillis);
                    }
                    Toast.makeText(this, buildSaveMessage(), Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(error -> {
                    setSaving(false);
                    Toast.makeText(this, "Item could not be saved. Please try again.", Toast.LENGTH_SHORT).show();
                });
    }

    private String buildSaveMessage() {
        if (hasReminder && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return "Item saved. Reminder saved, but notifications are disabled.";
        }
        return "Item saved";
    }

    private byte[] createCompressedImageBytes(Uri imageUri) throws IOException {
        BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
        boundsOptions.inJustDecodeBounds = true;
        try (InputStream boundsStream = getContentResolver().openInputStream(imageUri)) {
            BitmapFactory.decodeStream(boundsStream, null, boundsOptions);
        }

        int sampleSize = 1;
        int largestSide = Math.max(boundsOptions.outWidth, boundsOptions.outHeight);
        while (largestSide / sampleSize > 900) {
            sampleSize *= 2;
        }

        BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        decodeOptions.inSampleSize = sampleSize;
        Bitmap bitmap;
        try (InputStream imageStream = getContentResolver().openInputStream(imageUri)) {
            bitmap = BitmapFactory.decodeStream(imageStream, null, decodeOptions);
        }

        if (bitmap == null) {
            throw new IOException("Could not decode photo");
        }

        bitmap = applyExifOrientation(imageUri, bitmap);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream);
        bitmap.recycle();
        return outputStream.toByteArray();
    }

    private String saveImageBytesToInternalStorage(byte[] imageBytes, String ownerUid, String itemId) throws IOException {
        File userImageDirectory = new File(getFilesDir(), "item_images" + File.separator + ownerUid);
        if (!userImageDirectory.exists() && !userImageDirectory.mkdirs()) {
            throw new IOException("Could not create image directory");
        }

        File imageFile = new File(userImageDirectory, itemId + ".jpg");
        try (FileOutputStream outputStream = new FileOutputStream(imageFile)) {
            outputStream.write(imageBytes);
        }
        return imageFile.getAbsolutePath();
    }

    private Bitmap applyExifOrientation(Uri imageUri, Bitmap bitmap) throws IOException {
        int orientation;
        try (InputStream exifStream = getContentResolver().openInputStream(imageUri)) {
            if (exifStream == null) {
                return bitmap;
            }
            ExifInterface exifInterface = new ExifInterface(exifStream);
            orientation = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
        }

        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.postScale(1, -1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.postRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.postRotate(270);
                matrix.postScale(-1, 1);
                break;
            default:
                return bitmap;
        }

        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        if (rotatedBitmap != bitmap) {
            bitmap.recycle();
        }
        return rotatedBitmap;
    }

    private void setSaving(boolean saving) {
        progressBar.setVisibility(saving ? View.VISIBLE : View.GONE);
        saveButton.setEnabled(!saving);
    }

    private void openLogin() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK && photoUri != null) {
            photoPreviewImageView.setImageURI(photoUri);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission is required to save an item photo.", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadLastKnownLocation();
            } else {
                setLocationUnavailable();
            }
        } else if (requestCode == REQUEST_NOTIFICATION_PERMISSION
                && (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
            Toast.makeText(this, "Reminder saved, but notifications are disabled.", Toast.LENGTH_LONG).show();
        }
    }
}
