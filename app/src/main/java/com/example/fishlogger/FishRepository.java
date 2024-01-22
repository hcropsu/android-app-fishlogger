package com.example.fishlogger;

import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;


// This class is a centralized place to poll for the data from Firestore.
public class FishRepository {
    private static final String TAG = "FishLoggerApp";
    private FirebaseFirestore firestore;
    private final String COLLECTION = "fishlogger";

    public FishRepository(){
        firestore = FirebaseFirestore.getInstance();
    }

    // Simple method that adds a Fish to the collection
    public void addFish(Fish fish){
        firestore.collection(COLLECTION).add(fish);
    }

    // Method with a listener to delete a Fish from the collection. The userId needs to match the
    // current user and fishId needs to match the fishId from the Fish that was clicked. The listener's
    // method is implemented in FishListActivity which is the calling activity
    public void deleteFish(Fish fish, OnFishDeletedListener onFishDeletedListener, int position) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String userId = user.getUid();
        String fishId = fish.getFishId();
        Query query = firestore.collection(COLLECTION).whereEqualTo("userId", userId).whereEqualTo("fishId", fishId);
        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        document.getReference().delete()
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Log.d(TAG, "Deleted successfully");
                                        onFishDeletedListener.onFishDeleted(position);
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Error deleting Fish object
                                        Log.d(TAG, "Error deleting object");
                                    }
                                });
                    }
                } else {
                    // Error retrieving data
                    Log.d(TAG, "Error retrieving data");
                }
            }
        });
    }

    // Method to update a Fish. Very similar to deleteFish() method above.
    public void updateFish(Fish fish, OnFishUpdatedListener onFishUpdatedListener) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String userId = user.getUid();
        String fishId = fish.getFishId();
        Query query = firestore.collection(COLLECTION).whereEqualTo("userId", userId).whereEqualTo("fishId", fishId);
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    document.getReference().set(fish)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Updated successfully");
                                onFishUpdatedListener.onFishUpdated();
                                // Handle success, if needed
                            })
                            .addOnFailureListener(e -> {
                                // Error updating Fish object
                                Log.d(TAG, "Error updating object");
                                // Handle failure, if needed
                            });
                }
            } else {
                // Error retrieving data
                Log.d(TAG, "Error retrieving data");
                // Handle failure, if needed
            }
        });
    }

    // Method to get all the Fish from the Firestore database where 'userId' matches the
    // current user's Uid.
    public void getAllFish(OnCompleteListener<QuerySnapshot> listener){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null){
            String userId = user.getUid();
            firestore.collection(COLLECTION).whereEqualTo("userId", userId).orderBy("date", Query.Direction.DESCENDING).get().addOnCompleteListener(listener);
        } else {
            Log.d(TAG, "user == null");
            //Toast.makeText(MyApplication.getAppContext(),"Not logged in", Toast.LENGTH_SHORT).show();
        }
    }

    // The listener interfaces for deleting and updating a Fish.
    public interface OnFishDeletedListener{
        void onFishDeleted(int position);
    }

    public interface OnFishUpdatedListener{
        void onFishUpdated();
    }

}
