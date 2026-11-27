package com.app.roadguard;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

/** Home page. Shows a welcome message and links to Profile, Change password and Log out. */
public class Dashboard extends AppCompatActivity {

    SharedPreferences sp;
    TextView WelcomeName;
    View CardBattery, CardProfile, CardChangePassword, CardLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        sp = getSharedPreferences(ConstantSP.DataB, MODE_PRIVATE);

        // Not logged in -> back to the login screen
        if (sp.getString(ConstantSP.userid, "").isEmpty()) {
            goToLogin();
            return;
        }

        setContentView(R.layout.activity_dashboard);

        WelcomeName = findViewById(R.id.txtWelcomeName);
        CardBattery = findViewById(R.id.cardBattery);
        CardProfile = findViewById(R.id.cardProfile);
        CardChangePassword = findViewById(R.id.cardChangePassword);
        CardLogout = findViewById(R.id.cardLogout);

        CardBattery.setOnClickListener(v ->
                startActivity(new Intent(Dashboard.this, RequestBatteryHelp.class)));

        CardProfile.setOnClickListener(v ->
                startActivity(new Intent(Dashboard.this, Profile.class)));

        CardChangePassword.setOnClickListener(v ->
                startActivity(new Intent(Dashboard.this, ChangePassword.class)));

        CardLogout.setOnClickListener(v ->
                new AlertDialog.Builder(Dashboard.this)
                        .setTitle("Log out")
                        .setMessage("Are you sure you want to log out?")
                        .setPositiveButton("Log out", (dialog, which) -> {
                            sp.edit().clear().apply();
                            goToLogin();
                        })
                        .setNegativeButton("Cancel", null)
                        .show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Name may have changed on the Edit profile screen
        if (WelcomeName != null) {
            String name = sp.getString(ConstantSP.name, "");
            WelcomeName.setText(name.isEmpty() ? "Driver" : name);
        }
    }

    private void goToLogin() {
        Intent intent = new Intent(this, Login.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
