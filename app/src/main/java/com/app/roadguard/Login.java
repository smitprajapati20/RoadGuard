package com.app.roadguard;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;

import java.util.Locale;

public class Login extends AppCompatActivity {

    SharedPreferences sp;
    EditText Email, Password;
    Button Login;
    TextView TxtForgot, GoSignup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        sp = getSharedPreferences(ConstantSP.DataB, MODE_PRIVATE);

        Email = findViewById(R.id.edtEmail);
        Password = findViewById(R.id.edtPassword);
        Login = findViewById(R.id.btnLogin);
        TxtForgot = findViewById(R.id.txtForgotPassword);
        GoSignup = findViewById(R.id.txtGoSignup);

        Login.setOnClickListener(view -> {
            String email_text = Email.getText().toString().trim().toLowerCase(Locale.ROOT);
            String password_text = Password.getText().toString();

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
            if (password_text.isEmpty()) {
                Password.setError("Enter Password");
                Password.requestFocus();
                return;
            }

            setLoading(true);
            UserRepository.findByEmail(email_text, new UserRepository.Callback() {
                @Override
                public void onResult(DataSnapshot user) {
                    setLoading(false);

                    if (user == null || !PasswordUtil.verify(password_text,
                            UserRepository.str(user, "passwordSalt"),
                            UserRepository.str(user, "passwordHash"))) {
                        Toast.makeText(Login.this, "Invalid email or password", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    sp.edit()
                            .putString(ConstantSP.userid, user.getKey())
                            .putString(ConstantSP.name, UserRepository.str(user, "name"))
                            .putString(ConstantSP.contact, UserRepository.str(user, "contact"))
                            .putString(ConstantSP.email, UserRepository.str(user, "email"))
                            .apply();

                    Toast.makeText(Login.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(Login.this, Dashboard.class));
                    finish();
                }

                @Override
                public void onError(String message) {
                    setLoading(false);
                    Toast.makeText(Login.this, "Could not log in: " + message, Toast.LENGTH_LONG).show();
                }
            });
        });

        TxtForgot.setOnClickListener(v ->
                startActivity(new Intent(Login.this, ForgotPassword.class)));

        GoSignup.setOnClickListener(v -> {
            startActivity(new Intent(Login.this, SignUp.class));
            finish();
        });
    }

    private void setLoading(boolean loading) {
        Login.setEnabled(!loading);
        Login.setText(loading ? "Logging in..." : "Log in");
    }
}
