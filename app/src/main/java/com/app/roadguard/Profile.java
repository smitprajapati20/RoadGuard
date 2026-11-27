package com.app.roadguard;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;

import java.util.Locale;

public class Profile extends AppCompatActivity {

    SharedPreferences sp;
    TextView Back, Initial, Name, EmailTop, ProfileName, ProfileEmail, ProfilePhone;
    Button EditProfileBtn;
    String userId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        sp = getSharedPreferences(ConstantSP.DataB, MODE_PRIVATE);
        userId = sp.getString(ConstantSP.userid, "");

        Back = findViewById(R.id.txtBack);
        Initial = findViewById(R.id.txtInitial);
        Name = findViewById(R.id.txtName);
        EmailTop = findViewById(R.id.txtEmailTop);
        ProfileName = findViewById(R.id.txtProfileName);
        ProfileEmail = findViewById(R.id.txtProfileEmail);
        ProfilePhone = findViewById(R.id.txtProfilePhone);
        EditProfileBtn = findViewById(R.id.btnEditProfile);

        Back.setOnClickListener(v -> finish());
        EditProfileBtn.setOnClickListener(v ->
                startActivity(new Intent(Profile.this, EditProfile.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();

        showProfile(
                sp.getString(ConstantSP.name, ""),
                sp.getString(ConstantSP.email, ""),
                sp.getString(ConstantSP.contact, ""));

        if (userId.isEmpty()) {
            return;
        }
        UserRepository.findById(userId, new UserRepository.Callback() {
            @Override
            public void onResult(DataSnapshot user) {
                if (user == null) {
                    Toast.makeText(Profile.this, "Profile not found", Toast.LENGTH_SHORT).show();
                    return;
                }
                String name = UserRepository.str(user, "name");
                String email = UserRepository.str(user, "email");
                String contact = UserRepository.str(user, "contact");

                sp.edit()
                        .putString(ConstantSP.name, name)
                        .putString(ConstantSP.email, email)
                        .putString(ConstantSP.contact, contact)
                        .apply();

                showProfile(name, email, contact);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(Profile.this, "Could not refresh profile: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showProfile(String name, String email, String contact) {
        Name.setText(name);
        EmailTop.setText(email);
        ProfileName.setText(name);
        ProfileEmail.setText(email);
        ProfilePhone.setText(contact);
        Initial.setText(name.isEmpty() ? "R" : name.substring(0, 1).toUpperCase(Locale.ROOT));
    }
}
