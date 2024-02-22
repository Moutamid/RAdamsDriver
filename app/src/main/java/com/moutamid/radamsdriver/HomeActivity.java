package com.moutamid.radamsdriver;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
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

import java.util.ArrayList;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding b;

    private static final int PICK_IMAGES_REQUEST = 1;

    private ArrayList<Uri> selectedImages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());



        selectedImages = new ArrayList<>();

        // Launch file picker when the GridView is clicked
        b.addImageBtn.setOnClickListener(v -> openFilePicker());

        b.submitBtn.setOnClickListener(v -> {
            if (b.hCodeEditText.getText().toString().isEmpty())
                return;

            Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show();
            finish();
        });
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
