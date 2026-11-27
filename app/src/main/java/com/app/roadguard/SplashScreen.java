package com.app.roadguard;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AnimationUtils;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SplashScreen extends AppCompatActivity {

    SharedPreferences sp;
    private static final long SPLASH_DURATION = 2400L;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable goNext = () -> {
        if (!sp.getString(ConstantSP.userid, "").isEmpty()) {
            startActivity(new Intent(this, Dashboard.class));
            finish();
            return;
        }
        else{
        startActivity(new Intent(SplashScreen.this, SignUp.class));
        finish();}
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        sp = getSharedPreferences(ConstantSP.DataB, MODE_PRIVATE);

        View logo = findViewById(R.id.logoBadge);
        View pulse = findViewById(R.id.viewPulse);
        View title = findViewById(R.id.txtAppName);
        View tagline = findViewById(R.id.txtTagline);
        View progress = findViewById(R.id.progressSplash);
        pulse.setVisibility(View.VISIBLE);

        logo.startAnimation(AnimationUtils.loadAnimation(this, R.anim.logo_pop_in));
        pulse.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pulse_ring));
        title.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in));
        tagline.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in_delayed));
        progress.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in_delayed));

        handler.postDelayed(goNext, SPLASH_DURATION);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(goNext);
        super.onDestroy();
    }
}