package com.moutamid.radamsdriver;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fxn.stash.Stash;
import com.moutamid.radamsdriver.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding b;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        b.loginBtn.setOnClickListener(v -> {

            if (b.nameTextviewCreateProfile.getText().toString().isEmpty())
                return;

            if (b.passwordTextviewCreateProfile.getText().toString().isEmpty())
                return;


            Stash.put(Constants.IS_LOGGED_IN, true);
            Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show();
            finish();
            startActivity(new Intent(LoginActivity.this, WelcomeActivity.class));

        });
    }
}
