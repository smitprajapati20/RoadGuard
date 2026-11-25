package com.app.roadguard;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SignUp extends AppCompatActivity {

    SharedPreferences sp;
    EditText Name, Email, Phone, Password, ConfirmPassword, SecurityAnswer;
    Spinner SecurityQuestion;
    Button BtnSignUp;
    TextView Login;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);

        sp = getSharedPreferences(ConstantSP.DataB, MODE_PRIVATE);

        Name = findViewById(R.id.edtName);
        Email = findViewById(R.id.edtEmail);
        Phone = findViewById(R.id.edtPhone);
        Password = findViewById(R.id.edtPassword);
        ConfirmPassword = findViewById(R.id.edtConfirmPassword);
        SecurityQuestion = findViewById(R.id.spnSecurityQuestion);
        SecurityAnswer = findViewById(R.id.edtSecurityAnswer);
        BtnSignUp = findViewById(R.id.btnSignup);
        Login = findViewById(R.id.txtGoLogin);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.security_questions, R.layout.item_spinner);
        adapter.setDropDownViewResource(R.layout.item_spinner);
        SecurityQuestion.setAdapter(adapter);

        BtnSignUp.setOnClickListener(view -> {
            String name_text = Name.getText().toString().trim();
            String email_text = Email.getText().toString().trim().toLowerCase(Locale.ROOT);
            String number_text = Phone.getText().toString().trim();
            String password_text = Password.getText().toString();
            String confirm_password_text = ConfirmPassword.getText().toString();
            String question_text = SecurityQuestion.getSelectedItem().toString();
            String answer_text = PasswordUtil.normalizeAnswer(SecurityAnswer.getText().toString());

            // ---- Validations (same logic as before) ----
            if (name_text.isEmpty()) {
                Name.setError("Enter Name");
                Name.requestFocus();
                return;
            }
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
            if (password_text.isEmpty()) {
                Password.setError("Enter Password");
                Password.requestFocus();
                return;
            }
            if (password_text.length() < 6) {
                Password.setError("Password must be 6 characters long");
                Password.requestFocus();
                return;
            }
            if (confirm_password_text.isEmpty()) {
                ConfirmPassword.setError("Enter Confirm Password");
                ConfirmPassword.requestFocus();
                return;
            }
            if (!confirm_password_text.equals(password_text)) {
                ConfirmPassword.setError("Password does not match");
                ConfirmPassword.requestFocus();
                return;
            }
            if (answer_text.isEmpty()) {
                SecurityAnswer.setError("Enter Security Answer");
                SecurityAnswer.requestFocus();
                return;
            }

            setLoading(true);

            // ---- Duplicate check: email, then contact ----
            UserRepository.findByEmail(email_text, new UserRepository.Callback() {
                @Override
                public void onResult(DataSnapshot existing) {
                    if (existing != null) {
                        fail("User already exists");
                        return;
                    }
                    UserRepository.findByContact(number_text, new UserRepository.Callback() {
                        @Override
                        public void onResult(DataSnapshot existingContact) {
                            if (existingContact != null) {
                                fail("User already exists");
                                return;
                            }
                            createUser(name_text, email_text, number_text,
                                    password_text, question_text, answer_text);
                        }

                        @Override
                        public void onError(String message) {
                            fail("Could not sign up: " + message);
                        }
                    });
                }

                @Override
                public void onError(String message) {
                    fail("Could not sign up: " + message);
                }
            });
        });

        Login.setOnClickListener(v -> {
            startActivity(new Intent(SignUp.this, Login.class));
            finish();
        });
    }

    private void createUser(String name, String email, String number,
                            String password, String question, String answer) {
        String userId = UserRepository.users().push().getKey();
        if (userId == null) {
            fail("Could not create account. Try again.");
            return;
        }

        String passSalt = PasswordUtil.generateSalt();
        String answerSalt = PasswordUtil.generateSalt();

        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("contact", number);
        user.put("passwordSalt", passSalt);
        user.put("passwordHash", PasswordUtil.hash(password, passSalt));
        user.put("securityQuestion", question);
        user.put("answerSalt", answerSalt);
        user.put("answerHash", PasswordUtil.hash(answer, answerSalt));

        UserRepository.users().child(userId).setValue(user)
                .addOnSuccessListener(unused -> {
                    sp.edit()
                            .putString(ConstantSP.userid, userId)
                            .putString(ConstantSP.name, name)
                            .putString(ConstantSP.contact, number)
                            .putString(ConstantSP.email, email)
                            .apply();

                    Toast.makeText(this, "User Added Successfully!", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(SignUp.this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> fail("Could not sign up: " + e.getMessage()));
    }

    private void fail(String message) {
        setLoading(false);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void setLoading(boolean loading) {
        BtnSignUp.setEnabled(!loading);
        BtnSignUp.setText(loading ? "Creating account..." : "Create account");
    }
}
