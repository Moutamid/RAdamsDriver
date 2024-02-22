package com.moutamid.radamsdriver;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.fxn.stash.Stash;
import com.moutamid.radamsdriver.databinding.ActivityWelcomeBinding;

public class WelcomeActivity extends AppCompatActivity {

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

        b.ticketsView.setOnClickListener(v -> {
            startActivity(new Intent(WelcomeActivity.this, HomeActivity.class));
        });

    }
}
