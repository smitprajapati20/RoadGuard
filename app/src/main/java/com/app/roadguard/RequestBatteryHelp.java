package com.app.roadguard;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Task 1: form to request battery help (vehicle type, issue, location).
 * Task 2: stores the request in Firebase with status "pending".
 * Also used to EDIT a pending request (when started with a "requestId" extra).
 *
 * Realtime Database: requests/{requestId}
 *   requestId, userId, userName, userContact, serviceType,
 *   vehicleType, issue, description, location, status, createdAt
 */
public class RequestBatteryHelp extends AppCompatActivity {

    public static final String STATUS_PENDING = "pending";

    SharedPreferences sp;
    Spinner VehicleType, Issue;
    EditText Description, Location;
    Button Submit;
    TextView Back;
    String editId; // null = new request, otherwise the request being edited

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_request_battery_help);

        sp = getSharedPreferences(ConstantSP.DataB, MODE_PRIVATE);

        Back = findViewById(R.id.txtBack);
        VehicleType = findViewById(R.id.spnVehicleType);
        Issue = findViewById(R.id.spnIssue);
        Description = findViewById(R.id.edtDescription);
        Location = findViewById(R.id.edtLocation);
        Submit = findViewById(R.id.btnSubmitRequest);

        VehicleType.setAdapter(makeAdapter(R.array.vehicle_types));
        Issue.setAdapter(makeAdapter(R.array.battery_issues));

        // Edit mode: pre-fill the form with the existing request
        editId = getIntent().getStringExtra("requestId");
        if (editId != null) {
            VehicleType.setSelection(((ArrayAdapter<String>) VehicleType.getAdapter()).getPosition(getIntent().getStringExtra("vehicle")));
            Issue.setSelection(((ArrayAdapter<String>) Issue.getAdapter()).getPosition(getIntent().getStringExtra("issue")));
            Description.setText(getIntent().getStringExtra("description"));
            Location.setText(getIntent().getStringExtra("location"));
            Submit.setText("Update request");
        }

        Back.setOnClickListener(v -> finish());

        Submit.setOnClickListener(view -> {
            // ---- Validations ----
            if (VehicleType.getSelectedItemPosition() == 0) {
                Toast.makeText(this, "Select vehicle type", Toast.LENGTH_SHORT).show();
                return;
            }
            if (Issue.getSelectedItemPosition() == 0) {
                Toast.makeText(this, "Select the issue", Toast.LENGTH_SHORT).show();
                return;
            }

            String vehicle_text = VehicleType.getSelectedItem().toString();
            String issue_text = Issue.getSelectedItem().toString();
            String description_text = Description.getText().toString().trim();
            String location_text = Location.getText().toString().trim();

            if (issue_text.startsWith("Other") && description_text.isEmpty()) {
                Description.setError("Describe the problem");
                Description.requestFocus();
                return;
            }
            if (location_text.isEmpty()) {
                Location.setError("Enter your location");
                Location.requestFocus();
                return;
            }
            if (location_text.length() < 5) {
                Location.setError("Enter a more detailed location");
                Location.requestFocus();
                return;
            }

            String userId = sp.getString(ConstantSP.userid, "");
            if (userId.isEmpty()) {
                Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show();
                return;
            }

            // ---- Save to Firebase: new request (pending) or update the edited one ----
            DatabaseReference requests = UserRepository.users().getParent().child("requests");
            String id = editId != null ? editId : requests.push().getKey();

            Map<String, Object> request = new HashMap<>();
            request.put("vehicleType", vehicle_text);
            request.put("issue", issue_text);
            request.put("description", description_text);
            request.put("location", location_text);
            if (editId == null) {
                request.put("requestId", id);
                request.put("userId", userId);
                request.put("userName", sp.getString(ConstantSP.name, ""));
                request.put("userContact", sp.getString(ConstantSP.contact, ""));
                request.put("serviceType", "battery");
                request.put("status", STATUS_PENDING);
                request.put("createdAt", ServerValue.TIMESTAMP);
            }

            setLoading(true);
            (editId == null ? requests.child(id).setValue(request) : requests.child(id).updateChildren(request))
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, editId == null ? "Request sent! Status: Pending" : "Request updated", Toast.LENGTH_LONG).show();
                        if (editId == null) startActivity(new Intent(this, MyRequest.class));
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        setLoading(false);
                        Toast.makeText(this, "Could not save request: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });
    }

    private void setLoading(boolean loading) {
        Submit.setEnabled(!loading);
        Submit.setText(loading ? "Sending..." : editId == null ? "Request help" : "Update request");
    }

    /** Spinner adapter that greys out the first (placeholder) item. */
    private ArrayAdapter<String> makeAdapter(int arrayRes) {
        String[] items = getResources().getStringArray(arrayRes);
        return new ArrayAdapter<String>(this, R.layout.item_spinner, items) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                return tint((TextView) super.getView(position, convertView, parent), position);
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                return tint((TextView) super.getDropDownView(position, convertView, parent), position);
            }

            private View tint(TextView tv, int position) {
                tv.setTextColor(ContextCompat.getColor(getContext(),
                        position == 0 ? R.color.hint_text : R.color.primary_text));
                return tv;
            }
        };
    }
}
