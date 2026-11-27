package com.app.roadguard;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;

/** Post-service feedback: saves rating + comment on the request and marks it "completed". */
public class Feedback extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_feedback);

        String requestId = getIntent().getStringExtra("requestId");
        RatingBar rating = findViewById(R.id.ratingBar);
        EditText comment = findViewById(R.id.edtComment);
        Button submit = findViewById(R.id.btnSubmitFeedback);

        findViewById(R.id.txtBack).setOnClickListener(v -> finish());

        submit.setOnClickListener(v -> {
            if (rating.getRating() == 0) {
                Toast.makeText(this, "Please give a rating", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> m = new HashMap<>();
            m.put("status", "completed");
            m.put("rating", (int) rating.getRating());
            m.put("comment", comment.getText().toString().trim());
            m.put("feedbackAt", ServerValue.TIMESTAMP);

            submit.setEnabled(false);
            UserRepository.users().getParent().child("requests").child(requestId).updateChildren(m)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Thank you for your feedback!", Toast.LENGTH_LONG).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        submit.setEnabled(true);
                        Toast.makeText(this, "Could not submit: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });
    }
}
