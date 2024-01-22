package com.example.fishlogger;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

// This is the activity that shows the Fish objects stored in Firestore in a RecyclerView
public class FishListActivity extends AppCompatActivity implements FishRepository.OnFishDeletedListener, FishRepository.OnFishUpdatedListener {
    public static final String TAG = "FishLoggerApp";
    private RecyclerView fishRecyclerView;
    private FishAdapter fishAdapter;
    private FishViewModel fishViewModel;
    private Context context;
    private ActivityResultLauncher<Intent> addFishActivityLauncher;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "FishListActivity onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fish_list);
        context = this;
        // The launcher is needed so that when editing an existing Fish, it can be retrieved back
        // to this activity from the AddFishActivity. Then FishRepository's updateFish() is called so
        // the changes affect the Firestore database also. The updateFish() has a listener so once the
        // update is finished, the changes are also updated in the UI.
        addFishActivityLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
            if (result.getResultCode() == RESULT_OK){
                Intent data = result.getData();
                if (data != null){
                    Fish updatedFish = data.getParcelableExtra("updatedFish");
                    FishRepository fishRepository = new FishRepository();
                    fishRepository.updateFish(updatedFish, FishListActivity.this);
                }
            }
        });

        fishRecyclerView = findViewById(R.id.fishRecyclerView);
        fishRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // The adapter's constructor takes an OnClickListenerInterface as a parameter. It then gets
        // passed on to the FishViewHolder's constructor also.
        // The interface's methods are overwritten here: onItemClick() saves the clicked Fish to
        // a variable and passes that on to the AddFishActivity that is then launched
        fishAdapter = new FishAdapter(new ArrayList<>(), new OnClickListenerInterface() {
            @Override
            public void onItemClick(int position) {
                Fish fish = fishAdapter.getFishAtPosition(position);
                Intent intent = new Intent(context, AddFishActivity.class);
                intent.putExtra("fish", fish);
                addFishActivityLauncher.launch(intent);
            }
            // onItemLongClick() also saves the clicked Fish to a variable. A dialog is then launched
            // asking for confirmation to delete the Fish. deleteFish() also has a listener that
            // updates the UI once the deletion of the Fish is complete.
            @Override
            public void onItemLongClick(int position) {
                FishRepository fishRepository = new FishRepository();
                Fish fish = fishAdapter.getFishAtPosition(position);
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
                dialogBuilder.setMessage("Are you sure you want to delete selected fish?");

                dialogBuilder.setPositiveButton("Delete",(locationDialog, okButton) -> {
                    fishRepository.deleteFish(fish, FishListActivity.this, position);
                });

                dialogBuilder.setNegativeButton("Cancel", null);
                dialogBuilder.show();
            }
            // onImageClick also saves the clicked Fish to a variable. It checks if the Fish has a
            // custom image. If not, the drawable is saved to a variable and passed on to a custom
            // dialog that pops up and shows the drawable in larger size.
            // If the Fish is tagged as having a custom image, the fishId (which is also the name of
            // the image) is passed to the dialog's constructor. Then the image is displayed in the
            // dialog.
            @Override
            public void onImageClick(int position){
                Fish fish = fishAdapter.getFishAtPosition(position);
                boolean hasCustomImage = fish.isHasCustomImage();
                FishViewHolder fishViewHolder = (FishViewHolder) fishRecyclerView.findViewHolderForAdapterPosition(position);
                if (fishViewHolder != null) {
                    ImageView fishImageView = fishViewHolder.getFishImageView();
                    if (!hasCustomImage) {
                        Drawable drawable = fishImageView.getDrawable();
                        Log.d(TAG, "onImageClick(): drawable != null");
                        FishLargeImageDialog fishLargeImageDialog = new FishLargeImageDialog(context, drawable);
                        fishLargeImageDialog.show();
                    } else {
                        Log.d(TAG, "onImageClick(): else");
                        String fishId = fish.getFishId();
                        FishLargeImageDialog fishLargeImageDialog = new FishLargeImageDialog(context, fishId);
                        fishLargeImageDialog.show();
                    }
                }
            }
        });
        fishRecyclerView.setAdapter(fishAdapter);
        fishViewModel = new ViewModelProvider(this).get(FishViewModel.class);
        fishViewModel.getFishListLiveData().observe(this, new Observer<List<Fish>>() {
            @Override
            public void onChanged(List<Fish> fishList) {
                fishAdapter.setFishList(fishList);
            }
        });
        FirebaseUser user = MainActivity.checkUser();
        if (user != null){
            fishViewModel.loadFishList();
        } else {
            Toast.makeText(context, "Log in to show cathces", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "FishListActivity onStart()");
    }


    // Avoiding retrieving the whole list of Fish just to update the UI was easier when deleting
    // than when updating a Fish
    @Override
    public void onFishDeleted(int position) {
        List<Fish> updatedFishList = new ArrayList<>(fishAdapter.getFishList());
        updatedFishList.remove(position);
        fishAdapter.setFishList(updatedFishList);
        Toast.makeText(context, "Fish deleted", Toast.LENGTH_SHORT).show();
    }

    // TODO: figure out a way to update a Fish so that the UI reflects that change
    //  without loading the whole list from Firestore again
    @Override
    public void onFishUpdated() {
        fishViewModel.loadFishList();
        Toast.makeText(context, "Fish updated", Toast.LENGTH_SHORT).show();
    }
}
