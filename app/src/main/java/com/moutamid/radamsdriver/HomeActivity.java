package com.moutamid.radamsdriver;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fxn.stash.Stash;
import com.moutamid.radamsdriver.databinding.ActivityHomeBinding;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;

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

    private ArrayList<Uri> selectedImages;

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
//                openFilePicker();
                openCameraPicker();
            }
        });

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
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
                    MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM)
                            .addFormDataPart("vehicle", Stash.getString(Constants.VEHICLE))
                            .addFormDataPart("driver", Stash.getString(Constants.FULL_NAME))
                            .addFormDataPart("h_code", b.hCodeEditText.getText().toString())
                            .addFormDataPart("date", new Date().toString());

                    for (Uri uri : selectedImages) {
                        File file = getFile(uri);

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

    private void openCameraPicker() {
        Intent camera_intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(camera_intent, PICK_CAMERA_REQUEST);
    }

    public Uri bitmapToFile(Bitmap bitmap) { // File name like "image.png"
        //create a file to write bitmap data
        File file = null;
        try {
            file = new File(Environment.getExternalStorageDirectory() + File.separator
                    + System.currentTimeMillis() + "image.png");
            file.createNewFile();

//Convert bitmap to byte array
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos); // YOU can also save it in JPEG
            byte[] bitmapdata = bos.toByteArray();

//write the bytes in file
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(bitmapdata);
            fos.flush();
            fos.close();
            return Uri.fromFile(file);
//            return file;
        } catch (Exception e) {
            e.printStackTrace();
            return Uri.fromFile(file);
        }
    }


    /*private Uri bitmapToUri(Bitmap bitmap) {
        File tempDir = Environment.getExternalStorageDirectory();
        tempDir = new File(tempDir.getAbsolutePath());//+ "/.temp/"
        tempDir.mkdir();
        File tempFile = null;
        try {
            tempFile = File.createTempFile("Image-" + System.currentTimeMillis(), ".jpg", tempDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        byte[] bitmapData = bytes.toByteArray();

        //write the bytes in file
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(tempFile);

            fos.write(bitmapData);
            fos.flush();
            fos.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return Uri.fromFile(tempFile);
    }*/

    public File getFile(Uri uri) throws IOException {
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_CAMERA_REQUEST) {
            // BitMap is data structure of image file which store the image in memory
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            // Set the image in imageview for display
            selectedImages.add(bitmapToFile(photo));
            return;
        }

        if (requestCode == PICK_IMAGES_REQUEST && resultCode == RESULT_OK) {
            if (data != null) {
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    for (int i = 0; i < count; i++) {
                        Uri imageUri = data.getClipData().getItemAt(i).getUri();
                        selectedImages.add(imageUri);
                    }
                } else if (data.getData() != null) {
                    Uri imageUri = data.getData();
                    selectedImages.add(imageUri);
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

            holder.imageView.setImageURI(selectedImages.get(position));

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
