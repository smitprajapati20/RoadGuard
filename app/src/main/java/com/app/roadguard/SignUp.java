package com.app.roadguard;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SignUp extends AppCompatActivity {

    SharedPreferences sp;
    SQLiteDatabase db;
    EditText Name, Email, Phone, Password, ConfirmPassword;
    Button SignUp;
    TextView Login;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);

        db = openOrCreateDatabase(ConstantSP.DataB, MODE_PRIVATE, null);
        sp = getSharedPreferences(ConstantSP.DataB, MODE_PRIVATE);

        String userTable = "CREATE TABLE IF NOT EXISTS user(userid INTEGER PRIMARY KEY AUTOINCREMENT,name VARCHAR(50), email VARCHAR(30), contact VARCHAR(10), password VARCHAR(20))";
        db.execSQL(userTable);

        Name = findViewById(R.id.edtName);
        Email = findViewById(R.id.edtEmail);
        Phone = findViewById(R.id.edtPhone);
        Password = findViewById(R.id.edtPassword);
        ConfirmPassword = findViewById(R.id.edtConfirmPassword);
        SignUp = findViewById(R.id.btnSignup);
        Login = findViewById(R.id.txtGoLogin);

        SignUp.setOnClickListener(view -> {
            String name_text = Name.getText().toString().trim();
            String email_text = Email.getText().toString().trim();
            String number_text = Phone.getText().toString().trim();
            String password_text = Password.getText().toString();
            String confirm_password_text = ConfirmPassword.getText().toString();

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

            // ---- Duplicate check (parameterized query) ----
            Cursor cursor = db.rawQuery(
                    "SELECT userid FROM user WHERE email = ? OR contact = ?",
                    new String[]{email_text, number_text});
            boolean exists = cursor.getCount() > 0;
            cursor.close();

            if (exists) {
                Toast.makeText(this, "User already exists", Toast.LENGTH_SHORT).show();
                return;
            }

            // ---- Insert user (parameterized query) ----
            db.execSQL("INSERT INTO user VALUES(NULL, ?, ?, ?, ?)",
                    new Object[]{name_text, email_text, number_text, password_text});

            Cursor cursor2 = db.rawQuery("SELECT userid FROM user WHERE email = ?",
                    new String[]{email_text});
            if (cursor2.moveToFirst()) {
                int userId = cursor2.getInt(0);

                sp.edit()
                        .putInt(ConstantSP.userid, userId)
                        .putString(ConstantSP.name, name_text)
                        .putString(ConstantSP.contact, number_text)
                        .putString(ConstantSP.email, email_text)
                        .putString(ConstantSP.password, password_text)
                        .apply();
            }
            cursor2.close();

            Toast.makeText(this, "User Added Successfully!", Toast.LENGTH_LONG).show();
            startActivity(new Intent(SignUp.this, MainActivity.class));
            finish();
        });

        Login.setOnClickListener(v -> {
            startActivity(new Intent(SignUp.this, Login.class));
            finish();
        });
    }
}