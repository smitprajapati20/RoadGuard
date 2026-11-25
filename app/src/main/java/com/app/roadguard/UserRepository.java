package com.app.roadguard;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * Realtime Database structure:
 * users/{userId}: name, email, contact, passwordHash, passwordSalt,
 *                 securityQuestion, answerHash, answerSalt
 */
public class UserRepository {

    public interface Callback {
        /** user is null when nothing was found. */
        void onResult(DataSnapshot user);

        void onError(String message);
    }

    public static DatabaseReference users() {
        return FirebaseDatabase.getInstance("https://roadguard-c7ddd-default-rtdb.firebaseio.com/").getReference("users");
    }

    public static void findByEmail(String email, Callback cb) {
        findByField("email", email, cb);
    }

    public static void findByContact(String contact, Callback cb) {
        findByField("contact", contact, cb);
    }

    private static void findByField(String field, String value, Callback cb) {
        users().orderByChild(field).equalTo(value).limitToFirst(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            cb.onResult(snapshot.getChildren().iterator().next());
                        } else {
                            cb.onResult(null);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        cb.onError(error.getMessage());
                    }
                });
    }

    public static void findById(String userId, Callback cb) {
        users().child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                cb.onResult(snapshot.exists() ? snapshot : null);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                cb.onError(error.getMessage());
            }
        });
    }

    public static String str(DataSnapshot snapshot, String key) {
        String value = snapshot.child(key).getValue(String.class);
        return value == null ? "" : value;
    }
}
