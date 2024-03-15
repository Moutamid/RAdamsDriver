package com.moutamid.radamsdriver;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fxn.stash.Stash;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.moutamid.radamsdriver.databinding.ActivityHomeBinding;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HomeActivity extends AppCompatActivity {
    private static final String TAG = "HomeActivity";
    private ProgressDialog progressDialog;

    private ActivityHomeBinding b;

    private static final int PICK_IMAGES_REQUEST = 1;
    private static final int PICK_CAMERA_REQUEST = 2222;

    private ArrayList<File> selectedImages;
//    private ArrayList<Uri> uriImagesList;

    File cameraPhotoFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        selectedImages = new ArrayList<>();

        // Launch file picker when the GridView is clicked
        b.addImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog dialog;
                AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
                final CharSequence[] items = {"Gallery", "Camera"};
                builder.setItems(items, (dialog1, position) -> {
                    if (position == 0) {
                        openFilePicker();
                    } else takePicture();
                });

                dialog = builder.create();
                dialog.show();
//                openFilePicker();
//                openCameraPicker();
            }
        });

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(true);
        progressDialog.setMessage("Loading...");

        b.submitBtn.setOnClickListener(v -> {
            if (b.hCodeEditText.getText().toString().isEmpty())
                return;
            progressDialog.show();
            new Thread(() -> {

                try {
                    OkHttpClient client = new OkHttpClient().newBuilder()
                            .build();
                    MediaType mediaType = MediaType.parse("text/plain");
                    // 2024-03-15T17:30:00
                    String date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());
                    MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM)
                            .addFormDataPart("vehicle", Stash.getString(Constants.VEHICLE))
                            .addFormDataPart("driver", Stash.getString(Constants.FULL_NAME))
                            .addFormDataPart("h_code", b.hCodeEditText.getText().toString())
                            .addFormDataPart("date", date);

                    for (File file : selectedImages) {
//                        File file = getFile(uri);

                        RequestBody requestBody = RequestBody.create(MediaType.parse("application/octet-stream"),
                                file);
                        builder.addFormDataPart("images", "image.jpg", requestBody);
                    }

                    MultipartBody requestBody = builder.build();

                    Request request = new Request.Builder()
                            .url("http://app.ra-app.co.uk/api/ticket/create")
                            .method("POST", requestBody)
                            .addHeader("Authorization", "Bearer " + Stash.getString(Constants.TOKEN))
                            .build();
                    Response response = client.newCall(request).execute();

                    String message = response.body().string();
                    if (message.contains("Ticket added successfully!")) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();

                            Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }

                } catch (IOException e) {
                    Log.d(TAG, "onCreate/84:  : " + e.getMessage());
                    Log.d(TAG, "onCreate/84:  : " + e.getStackTrace().toString());
                    Log.d(TAG, "onCreate/84:  : " + e.getLocalizedMessage());
//                    Log.d(TAG, "onCreate/84:  : " + e.getCause().toString());
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                    });
                }

            }).start();

        });

        b.numberPlateTv.setText(Stash.getString(Constants.VEHICLE));
    }

    private static final int REQUEST_CAMERA_PERMISSION = 1;

    public void takePicture() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA);
            shouldShowRequestPermissionRationale(android.Manifest.permission.READ_MEDIA_IMAGES);
            shouldShowRequestPermissionRationale(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA,android.Manifest.permission.READ_MEDIA_IMAGES,android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
        } else {
            // Permission granted, proceed with capturing the image
          //  openCamera();
            ImagePicker.with(this).compress(1024)			//Final image size will be less than 1 MB(Optional)
                    .maxResultSize(1080, 1080)
                    .cameraOnly()
                    .saveDir(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath())
                    .start(PICK_CAMERA_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                // Handle permission denied case (e.g., display a message)
            }
        }
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Create a unique file name for the captured image
        cameraPhotoFile = createImageFile();
        if (cameraPhotoFile != null) {
            Uri uri = FileProvider.getUriForFile(HomeActivity.this,
                    getApplicationContext().getPackageName() + ".provider",
                    new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), imageFileName));

//            Uri photoURI = Uri.fromFile(photoFile);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            startActivityForResult(cameraIntent, PICK_CAMERA_REQUEST);
        }
    }

    String imageFileName;

    private File createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        imageFileName = "JPEG_" + timeStamp + "_";
//        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        try {
            File imageFile = File.createTempFile(imageFileName, ".jpg", storageDir);
            return imageFile;
        } catch (IOException e) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(HomeActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show());
            return null;
        }
    }


    /*private void openCameraPicker() {
        Intent intent = new Intent(this, FilePickerActivity.class);
        intent.putExtra(FilePickerActivity.CONFIGS, new Configurations.Builder()
//                .setCheckPermission(true)
                .setShowImages(true)
                .setShowVideos(false)
                .enableImageCapture(true)
                .setSkipZeroSizeFiles(true)
                .build());
        startActivityForResult(intent, PICK_CAMERA_REQUEST);

//        Intent camera_intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//        startActivityForResult(camera_intent, PICK_CAMERA_REQUEST);

//        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

//        cameraPhotoFile = createImageFile();

//        if (cameraPhotoFile != null) {
//            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.parse(cameraPhotoFile.getAbsolutePath()));
//            startActivityForResult(takePictureIntent, PICK_CAMERA_REQUEST);
//        }

    }*/

    private File createImageFilee() {

        long timeStamp = System.currentTimeMillis();
        String imageFileName = timeStamp + "-image";
        File storageDir = getFilesDir();
//                + File.separator + getString(R.string.app_name));
        if (!storageDir.exists())
            storageDir.mkdirs();

        File image = null;
        try {
            image = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );
        } catch (IOException e) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(HomeActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show());
        }

        return image;
    }


    public File bitmapToFile(Bitmap bitmap) { // File name like "image.png"
        //create a file to write bitmap data
        File file = null;
        try {
            file = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    + File.separator
                    + System.currentTimeMillis() + "image.png");
            file.createNewFile();

//Convert bitmap to byte array
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos); // YOU can also save it in JPEG
            byte[] bitmapdata = bos.toByteArray();

//write the bytes in file
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(bitmapdata);
            fos.flush();
            fos.close();
//            return Uri.fromFile(file);
            return file;
        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(HomeActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show());
            return file;
//            return Uri.fromFile(file);
        }
    }


    public File getFile(Uri uri) {
        File destinationFilename = new File(getFilesDir().getPath() + File.separatorChar + queryName(HomeActivity.this, uri));
        try (InputStream ins = getContentResolver().openInputStream(uri)) {
            createFileFromStream(ins, destinationFilename);
        } catch (Exception ex) {
            Log.e("Save File", ex.getMessage());
            ex.printStackTrace();
        }
        return destinationFilename;
    }

    public static void createFileFromStream(InputStream ins, File destination) {
        try (OutputStream os = new FileOutputStream(destination)) {
            byte[] buffer = new byte[4096];
            int length;
            while ((length = ins.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            os.flush();
        } catch (Exception ex) {
            Log.e("Save File", ex.getMessage());
            ex.printStackTrace();
        }
    }

    private static String queryName(Context context, Uri uri) {
        Cursor returnCursor =
                context.getContentResolver().query(uri, null, null, null, null);
        assert returnCursor != null;
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        returnCursor.moveToFirst();
        String name = returnCursor.getString(nameIndex);
        returnCursor.close();
        return name;
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, PICK_IMAGES_REQUEST);
    }

    private void rotateImage(Uri imageUri, int orientation) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

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
                default:
                    break;
            }

            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            File rotatedFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "rotated_image.jpg");
            FileOutputStream outputStream = new FileOutputStream(rotatedFile);
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();

            selectedImages.add(rotatedFile);
            initRecyclerView();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_CAMERA_REQUEST && resultCode == RESULT_OK) {
            // Image captured successfully, get the file path
//            File imageFile = new File(getIntent().getStringExtra(MediaStore.EXTRA_OUTPUT));
            if (data != null & data.getData() != null){
                Log.d(TAG, "onActivityResult: " + data.getData().getPath());
                // b.testImage.setImageBitmap(photo);
                // Use the imageFile for your POST request
                Uri imageUri = data.getData();

                if (imageUri != null) {
                    ExifInterface exif = null;
                    try {
                        exif = new ExifInterface(imageUri.getPath());
                        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                        if (orientation != ExifInterface.ORIENTATION_NORMAL) {
                            Log.d(TAG, "onActivityResult: NOT NORMAL");
                            rotateImage(imageUri, orientation);
                        } else {
                            File rotatedFile = new File(imageUri.getPath());
                            selectedImages.add(rotatedFile);
                            initRecyclerView();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    // Handle null imageUri
                    Log.e("ImageProcessing", "Image URI is null");
                }

//            b.testImg.setImageURI(Uri.fromFile(cameraPhotoFile));

//            selectedImages.add(bitmapToFile(photo));
            }
            return;
        }

        /*if (requestCode == PICK_CAMERA_REQUEST) {
            // BitMap is data structure of image file which store the image in memory
//            Bitmap photo = (Bitmap) data.getExtras().get("data");
            ArrayList<MediaFile> files = data.getParcelableArrayListExtra(FilePickerActivity.MEDIA_FILES);

            for (MediaFile mediaFile : files) {
                File file = new File(mediaFile.getUri().getPath());
                cameraPhotoFile = file;
                selectedImages.add(file);
            }


            b.testImg.setImageURI(Uri.fromFile(cameraPhotoFile));

//            selectedImages.add(bitmapToFile(photo));
            initRecyclerView();
            return;
        }*/

        if (requestCode == PICK_IMAGES_REQUEST && resultCode == RESULT_OK) {
            if (data != null) {
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    for (int i = 0; i < count; i++) {
                        Uri imageUri = data.getClipData().getItemAt(i).getUri();
                        File file = getFile(imageUri);

                        selectedImages.add(file);
                    }
                } else if (data.getData() != null) {
                    Uri imageUri = data.getData();
                    File file = getFile(imageUri);
                    selectedImages.add(file);
                }

                initRecyclerView();
            }
        }
    }


    private RecyclerView conversationRecyclerView;
    private RecyclerViewAdapterMessages adapter;

    private void initRecyclerView() {

        conversationRecyclerView = findViewById(R.id.imagesRv);
        adapter = new RecyclerViewAdapterMessages();
        int mNoOfColumns = calculateNoOfColumns(HomeActivity.this, 100);
        b.imagesRv.setLayoutManager(new GridLayoutManager(this, mNoOfColumns));
        conversationRecyclerView.setHasFixedSize(true);
        conversationRecyclerView.setNestedScrollingEnabled(false);

        conversationRecyclerView.setAdapter(adapter);

    }

    public static int calculateNoOfColumns(Context context, float columnWidthDp) { // For example columnWidthdp=180
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float screenWidthDp = displayMetrics.widthPixels / displayMetrics.density;
        int noOfColumns = (int) (screenWidthDp / columnWidthDp + 0.5); // +0.5 for correct rounding to int.
        return noOfColumns;
    }

    private class RecyclerViewAdapterMessages extends RecyclerView.Adapter
            <RecyclerViewAdapterMessages.ViewHolderRightMessage> {

        @NonNull
        @Override
        public ViewHolderRightMessage onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.grid_item, parent, false);
            return new ViewHolderRightMessage(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final ViewHolderRightMessage holder, int position) {

            holder.imageView.setImageURI(Uri.fromFile(selectedImages.get(position)));
//            b.testImg2.setImageURI(Uri.fromFile(selectedImages.get(position)));

        }

        @Override
        public int getItemCount() {
            if (selectedImages == null)
                return 0;
            return selectedImages.size();
        }

        public class ViewHolderRightMessage extends RecyclerView.ViewHolder {

            ImageView imageView;

            public ViewHolderRightMessage(@NonNull View v) {
                super(v);
                imageView = v.findViewById(R.id.imageViewww);

            }
        }

    }

}
