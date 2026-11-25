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

public class ChangePassword extends AppCompatActivity {

    SharedPreferences sp;
    EditText OldPassword, NewPassword, ConfirmPassword;
    Button BtnChange;
    TextView Cancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_change_password);

        sp = getSharedPreferences(ConstantSP.DataB, MODE_PRIVATE);

        OldPassword = findViewById(R.id.edtOldPassword);
        NewPassword = findViewById(R.id.edtNewPassword);
        ConfirmPassword = findViewById(R.id.edtConfirmPassword);
        BtnChange = findViewById(R.id.btnChangePassword);
        Cancel = findViewById(R.id.txtCancel);

        BtnChange.setOnClickListener(view -> {
            String old_password_text = OldPassword.getText().toString();
            String new_password_text = NewPassword.getText().toString();
            String confirm_password_text = ConfirmPassword.getText().toString();

            if (old_password_text.isEmpty()) {
                OldPassword.setError("Enter Current Password");
                OldPassword.requestFocus();
                return;
            }
            if (new_password_text.isEmpty()) {
                NewPassword.setError("Enter New Password");
                NewPassword.requestFocus();
                return;
            }
            if (new_password_text.length() < 6) {
                NewPassword.setError("Password must be 6 characters long");
                NewPassword.requestFocus();
                return;
            }
            if (new_password_text.equals(old_password_text)) {
                NewPassword.setError("New password must be different from the current one");
                NewPassword.requestFocus();
                return;
            }
            if (confirm_password_text.isEmpty()) {
                ConfirmPassword.setError("Enter Confirm Password");
                ConfirmPassword.requestFocus();
                return;
            }
            if (!confirm_password_text.equals(new_password_text)) {
                ConfirmPassword.setError("Password does not match");
                ConfirmPassword.requestFocus();
                return;
            }

            String userId = sp.getString(ConstantSP.userid, "");
            if (userId.isEmpty()) {
                Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show();
                return;
            }

            BtnChange.setEnabled(false);
            UserRepository.findById(userId, new UserRepository.Callback() {
                @Override
                public void onResult(DataSnapshot user) {
                    if (user == null || !PasswordUtil.verify(old_password_text,
                            UserRepository.str(user, "passwordSalt"),
                            UserRepository.str(user, "passwordHash"))) {
                        BtnChange.setEnabled(true);
                        OldPassword.setError("Current password is incorrect");
                        OldPassword.requestFocus();
                        return;
                    }

                    String newSalt = PasswordUtil.generateSalt();
                    Map<String, Object> update = new HashMap<>();
                    update.put("passwordSalt", newSalt);
                    update.put("passwordHash", PasswordUtil.hash(new_password_text, newSalt));

                    UserRepository.users().child(userId).updateChildren(update)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(ChangePassword.this, "Password changed successfully!", Toast.LENGTH_LONG).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                BtnChange.setEnabled(true);
                                Toast.makeText(ChangePassword.this, "Could not change password: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                }

                @Override
                public void onError(String message) {
                    BtnChange.setEnabled(true);
                    Toast.makeText(ChangePassword.this, "Something went wrong: " + message, Toast.LENGTH_LONG).show();
                }
            });
        });

        Cancel.setOnClickListener(v -> finish());
    }
}
