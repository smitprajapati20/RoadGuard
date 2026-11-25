package com.app.roadguard;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ForgotPassword extends AppCompatActivity {

    EditText Email, SecurityAnswer, OldPassword, NewPassword, ConfirmPassword;
    Button FindAccount, ResetPassword;
    TextView SecurityQuestion, BackToLogin;
    View VerifyLayout;

    // Loaded after the account is found
    String userId = "";
    String passwordSalt = "", passwordHash = "";
    String answerSalt = "", answerHash = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);

        Email = findViewById(R.id.edtEmail);
        FindAccount = findViewById(R.id.btnFindAccount);
        VerifyLayout = findViewById(R.id.layoutVerify);
        SecurityQuestion = findViewById(R.id.txtSecurityQuestion);
        SecurityAnswer = findViewById(R.id.edtSecurityAnswer);
        OldPassword = findViewById(R.id.edtOldPassword);
        NewPassword = findViewById(R.id.edtNewPassword);
        ConfirmPassword = findViewById(R.id.edtConfirmPassword);
        ResetPassword = findViewById(R.id.btnResetPassword);
        BackToLogin = findViewById(R.id.txtBackToLogin);

        FindAccount.setOnClickListener(view -> {
            String email_text = Email.getText().toString().trim().toLowerCase(Locale.ROOT);

            if (email_text.isEmpty()) {
                Email.setError("Enter Email");
                Email.requestFocus();
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email_text).matches()) {
                Email.setError("Enter Valid Email");
                Email.requestFocus();
                return;
            }

            FindAccount.setEnabled(false);
            UserRepository.findByEmail(email_text, new UserRepository.Callback() {
                @Override
                public void onResult(DataSnapshot user) {
                    FindAccount.setEnabled(true);
                    if (user == null) {
                        Email.setError("No account found with this email");
                        Email.requestFocus();
                        return;
                    }

                    userId = user.getKey();
                    passwordSalt = UserRepository.str(user, "passwordSalt");
                    passwordHash = UserRepository.str(user, "passwordHash");
                    answerSalt = UserRepository.str(user, "answerSalt");
                    answerHash = UserRepository.str(user, "answerHash");
                    SecurityQuestion.setText(UserRepository.str(user, "securityQuestion"));

                    Email.setEnabled(false);
                    FindAccount.setVisibility(View.GONE);
                    VerifyLayout.setVisibility(View.VISIBLE);
                }

                @Override
                public void onError(String message) {
                    FindAccount.setEnabled(true);
                    Toast.makeText(ForgotPassword.this, "Something went wrong: " + message, Toast.LENGTH_LONG).show();
                }
            });
        });

        ResetPassword.setOnClickListener(view -> {
            String answer_text = PasswordUtil.normalizeAnswer(SecurityAnswer.getText().toString());
            String old_password_text = OldPassword.getText().toString();
            String new_password_text = NewPassword.getText().toString();
            String confirm_password_text = ConfirmPassword.getText().toString();

            if (answer_text.isEmpty() && old_password_text.isEmpty()) {
                SecurityAnswer.setError("Answer the question or enter your old password");
                SecurityAnswer.requestFocus();
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

            // Verified if EITHER the security answer OR the old password is correct
            boolean answerOk = !answer_text.isEmpty()
                    && PasswordUtil.verify(answer_text, answerSalt, answerHash);
            boolean oldPasswordOk = !old_password_text.isEmpty()
                    && PasswordUtil.verify(old_password_text, passwordSalt, passwordHash);

            if (!answerOk && !oldPasswordOk) {
                Toast.makeText(this, "Verification failed. Check your answer or old password.", Toast.LENGTH_LONG).show();
                return;
            }
            if (PasswordUtil.verify(new_password_text, passwordSalt, passwordHash)) {
                NewPassword.setError("New password must be different from the old one");
                NewPassword.requestFocus();
                return;
            }

            String newSalt = PasswordUtil.generateSalt();
            Map<String, Object> update = new HashMap<>();
            update.put("passwordSalt", newSalt);
            update.put("passwordHash", PasswordUtil.hash(new_password_text, newSalt));

            ResetPassword.setEnabled(false);
            UserRepository.users().child(userId).updateChildren(update)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Password reset successfully. Please log in.", Toast.LENGTH_LONG).show();
                        goToLogin();
                    })
                    .addOnFailureListener(e -> {
                        ResetPassword.setEnabled(true);
                        Toast.makeText(this, "Could not reset password: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });

        BackToLogin.setOnClickListener(v -> goToLogin());
    }

    private void goToLogin() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
