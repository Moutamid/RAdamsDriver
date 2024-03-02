package com.moutamid.radamsdriver;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fxn.stash.Stash;
import com.moutamid.radamsdriver.databinding.ActivityWelcomeBinding;

public class WelcomeActivity extends AppCompatActivity {
    private static final String TAG = "WelcomeActivity";

    private ActivityWelcomeBinding b;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityWelcomeBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        if (Stash.getBoolean(Constants.IS_LOGGED_IN, false)) {

        } else {
            finish();
            startActivity(new Intent(WelcomeActivity.this, LoginActivity.class));
        }

        b.userNameTextView.setText("Welcome "+Stash.getString(Constants.FULL_NAME));

        Log.d(TAG, "onCreate/29: NAME : " +Stash.getString(Constants.FULL_NAME));
        Log.d(TAG, "onCreate/29: TOKEN : " +Stash.getString(Constants.TOKEN));
        Log.d(TAG, "onCreate/29:  VEHICLE: " +Stash.getString(Constants.VEHICLE));

        b.ticketsView.setOnClickListener(v -> {
            startActivity(new Intent(WelcomeActivity.this, HomeActivity.class));
        });

    }
}
