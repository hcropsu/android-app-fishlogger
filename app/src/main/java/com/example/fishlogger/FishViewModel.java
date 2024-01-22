package com.example.fishlogger;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class FishViewModel extends ViewModel {
    private final String TAG = "FishLoggerApp_FishViewModel";
    private FishRepository fishRepository;
    private MutableLiveData<List<Fish>> fishListLiveData;

    public FishViewModel(){
        fishListLiveData = new MutableLiveData<>();
        fishRepository = new FishRepository();
    }

    public LiveData<List<Fish>> getFishListLiveData(){
        return fishListLiveData;
    }

    public void loadFishList(){
        fishRepository.getAllFish(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isComplete()){
                    List<Fish> fishList = new ArrayList<>();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Fish fish = document.toObject(Fish.class);
                        fishList.add(fish);
                    }
                    fishListLiveData.setValue(fishList);
                } else {
                    Log.d(TAG, "getAllFish() task was not complete");
                }
            }
        });
    }
}
