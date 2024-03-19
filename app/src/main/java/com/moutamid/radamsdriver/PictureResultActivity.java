package com.moutamid.radamsdriver;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.fxn.stash.Stash;
import com.moutamid.radamsdriver.databinding.ActivityPictureResultBinding;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

public class PictureResultActivity extends AppCompatActivity {
    ActivityPictureResultBinding binding;
    String path;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPictureResultBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        path = Stash.getString("img");
        Log.d("PATH123", "Saved  " + path);
        Glide.with(this).load(path)
                .skipMemoryCache(true) // Skip memory cache
                .diskCacheStrategy(DiskCacheStrategy.NONE) // Skip disk cache
                .into(binding.image);

        int capturedImageOrientation = Constants.rotateImage(path);
        Log.d("PATH123", "capturedImageOrientation  " + capturedImageOrientation);

//        if (capturedImageOrientation == 90 || capturedImageOrientation == 270) {
//            binding.image.setRotation(-90); // Rotate the ImageView for horizontal images
//        } else {
//            binding.image.setRotation(0);  // Reset rotation for portrait images
//        }

        binding.save.setOnClickListener(v -> {
            //startActivity(new Intent(this, HomeActivity.class));
            finish();
        });

        binding.retake.setOnClickListener(v -> {
            Stash.clear("img");
            binding.image.setImageResource(0);
            Glide.get(PictureResultActivity.this).clearMemory();
            new Thread(() -> Glide.get(PictureResultActivity.this).clearDiskCache()).start();
            onBackPressed();
        });

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        File fileToDelete = new File(path);
        if (fileToDelete.exists()) {
            boolean isDeleted = fileToDelete.delete();
            if (isDeleted) {
                startActivity(new Intent(this, CameraActivity.class));
                finish();
            } else {
                // Failed to delete file
            }
        }
    }
}