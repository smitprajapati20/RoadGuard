package com.app.roadguard;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/** Shows the user's latest request. Edit / Cancel are allowed only while status is "pending". */
public class MyRequest extends AppCompatActivity {

    // Predefined technicians: {name, phone}
    private static final String[][] TECHNICIANS = {
            {"Rahul Sharma", "9876543210"},
            {"Amit Patel", "9876501234"},
            {"Suresh Kumar", "9898989898"},
            {"Imran Khan", "9123456780"}};
    private static final long ASSIGN_AFTER_MS = 60000; // demo: technician assigned after 1 minute

    SharedPreferences sp;
    TextView Status, Details;
    Button Edit, Cancel, BtnFeedback;
    DatabaseReference requests;
    DataSnapshot cur; // latest request (null if none)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_my_request);

        sp = getSharedPreferences(ConstantSP.DataB, MODE_PRIVATE);
        requests = UserRepository.users().getParent().child("requests");
        Status = findViewById(R.id.txtStatus);
        Details = findViewById(R.id.txtDetails);
        Edit = findViewById(R.id.btnEditRequest);
        Cancel = findViewById(R.id.btnCancelRequest);
        BtnFeedback = findViewById(R.id.btnFeedback);

        findViewById(R.id.txtBack).setOnClickListener(v -> finish());

        // Edit: reopen the request form with the current values
        Edit.setOnClickListener(v -> startActivity(new Intent(this, RequestBatteryHelp.class)
                .putExtra("requestId", cur.getKey())
                .putExtra("vehicle", get("vehicleType"))
                .putExtra("issue", get("issue"))
                .putExtra("description", get("description"))
                .putExtra("location", get("location"))));

        // Feedback: after the service (technician assigned)
        BtnFeedback.setOnClickListener(v -> startActivity(new Intent(this, Feedback.class)
                .putExtra("requestId", cur.getKey())));

        // Cancel: set status to "cancelled"
        Cancel.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Cancel request")
                .setMessage("Do you want to cancel this request?")
                .setPositiveButton("Yes, cancel", (d, w) ->
                        requests.child(cur.getKey()).child("status").setValue("cancelled")
                                .addOnSuccessListener(u -> load()))
                .setNegativeButton("No", null)
                .show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        requests.orderByChild("userId").equalTo(sp.getString(ConstantSP.userid, "")).limitToLast(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot s) {
                        cur = s.exists() ? s.getChildren().iterator().next() : null;
                        show();
                    }

                    @Override
                    public void onCancelled(DatabaseError e) {
                        Toast.makeText(MyRequest.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String get(String key) {
        return UserRepository.str(cur, key);
    }

    private void show() {
        Edit.setVisibility(View.GONE);
        Cancel.setVisibility(View.GONE);
        BtnFeedback.setVisibility(View.GONE);
        if (cur == null) {
            Status.setText("No requests yet");
            Details.setText("Request battery help from the home screen.");
            return;
        }

        String status = get("status"), tName = get("technicianName"), tPhone = get("technicianPhone");
        Long created = cur.child("createdAt").getValue(Long.class);
        Long rating = cur.child("rating").getValue(Long.class);

        // Demo: assign a technician from the predefined list once the request is old enough
        if (status.equals("pending") && created != null
                && System.currentTimeMillis() - created > ASSIGN_AFTER_MS) {
            String[] t = TECHNICIANS[new Random().nextInt(TECHNICIANS.length)];
            status = "assigned";
            tName = t[0];
            tPhone = t[1];
            Map<String, Object> m = new HashMap<>();
            m.put("status", status);
            m.put("technicianName", tName);
            m.put("technicianPhone", tPhone);
            requests.child(cur.getKey()).updateChildren(m);
        }

        boolean pending = status.equals("pending");
        Edit.setVisibility(pending ? View.VISIBLE : View.GONE);
        Cancel.setVisibility(pending ? View.VISIBLE : View.GONE);
        BtnFeedback.setVisibility(status.equals("assigned") ? View.VISIBLE : View.GONE);

        Status.setText("Status: " + status.toUpperCase());
        Details.setText(get("vehicleType") + " - " + get("issue")
                + "\nLocation: " + get("location")
                + (get("description").isEmpty() ? "" : "\nNote: " + get("description"))
                + (!tName.isEmpty() ? "\n\nTechnician: " + tName + "\nPhone: " + tPhone
                : pending ? "\n\nA technician will be assigned soon." : "")
                + (rating == null ? "" : "\n\nYour rating: " + rating + "/5"
                + (get("comment").isEmpty() ? "" : "\nComment: " + get("comment"))));
    }
}
