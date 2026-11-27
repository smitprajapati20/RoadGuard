package com.app.roadguard;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;

import java.util.HashMap;
import java.util.Map;

public class EditProfile extends AppCompatActivity {

    SharedPreferences sp;
    EditText Name, Email, Phone;
    Button Save;
    TextView Cancel;
    String userId = "";
    String oldName = "", oldContact = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile);

        sp = getSharedPreferences(ConstantSP.DataB, MODE_PRIVATE);
        userId = sp.getString(ConstantSP.userid, "");

        Name = findViewById(R.id.edtName);
        Email = findViewById(R.id.edtEmail);
        Phone = findViewById(R.id.edtPhone);
        Save = findViewById(R.id.btnSaveProfile);
        Cancel = findViewById(R.id.txtCancel);

        oldName = sp.getString(ConstantSP.name, "");
        oldContact = sp.getString(ConstantSP.contact, "");
        Name.setText(oldName);
        Email.setText(sp.getString(ConstantSP.email, ""));
        Email.setEnabled(false);
        Phone.setText(oldContact);

        Cancel.setOnClickListener(v -> finish());

        Save.setOnClickListener(view -> {
            String name_text = Name.getText().toString().trim();
            String number_text = Phone.getText().toString().trim();

            if (name_text.isEmpty()) {
                Name.setError("Enter Name");
                Name.requestFocus();
                return;
            }
            if (number_text.isEmpty()) {
                Phone.setError("Enter Number");
                Phone.requestFocus();
                return;
            }
            if (number_text.length() != 10) {
                Phone.setError("Contact number must be 10 digits");
                Phone.requestFocus();
                return;
            }
            if (userId.isEmpty()) {
                Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show();
                return;
            }
            if (name_text.equals(oldName) && number_text.equals(oldContact)) {
                Toast.makeText(this, "No changes to save", Toast.LENGTH_SHORT).show();
                return;
            }

            setLoading(true);

            if (!number_text.equals(oldContact)) {
                UserRepository.findByContact(number_text, new UserRepository.Callback() {
                    @Override
                    public void onResult(DataSnapshot other) {
                        if (other != null && !userId.equals(other.getKey())) {
                            setLoading(false);
                            Phone.setError("This number is already used by another account");
                            Phone.requestFocus();
                            return;
                        }
                        saveToFirebase(name_text, number_text);
                    }

                    @Override
                    public void onError(String message) {
                        setLoading(false);
                        Toast.makeText(EditProfile.this, "Something went wrong: " + message, Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                saveToFirebase(name_text, number_text);
            }
        });
    }

    private void saveToFirebase(String name, String contact) {
        Map<String, Object> update = new HashMap<>();
        update.put("name", name);
        update.put("contact", contact);

        UserRepository.users().child(userId).updateChildren(update)
                .addOnSuccessListener(unused -> {
                    sp.edit()
                            .putString(ConstantSP.name, name)
                            .putString(ConstantSP.contact, contact)
                            .apply();
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Could not update profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean loading) {
        Save.setEnabled(!loading);
        Save.setText(loading ? "Saving..." : "Save changes");
    }
}
