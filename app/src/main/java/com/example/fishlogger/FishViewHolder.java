package com.example.fishlogger;

import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FishViewHolder extends RecyclerView.ViewHolder {
    private final ImageView fishImageView;
    private final TextView speciesTextView;
    private final TextView dateTextView;
    private final TextView locationTextView;
    private final TextView lengthTextView;
    private final TextView weightTextView;


    // As per ChatGPT's suggestion we set the OnClickInterface in the ViewHolder's constructor
    // rather than in the Adapter's onCreateViewHolder() or onBindViewHolder() like I did in
    // Harjoitus 6-8 for example
    public FishViewHolder(@NonNull View itemView, OnClickListenerInterface onClickListenerInterface) {
        super(itemView);
        fishImageView = itemView.findViewById(R.id.item_fish_imageViewFish);
        speciesTextView = itemView.findViewById(R.id.item_fish_textViewSpecies);
        dateTextView = itemView.findViewById(R.id.item_fish_textViewDate);
        locationTextView = itemView.findViewById(R.id.item_fish_textViewLocation);
        lengthTextView = itemView.findViewById(R.id.item_fish_textViewLength);
        weightTextView = itemView.findViewById(R.id.item_fish_textViewWeight);

        // The whole clicked View gets a clickListener and a longClickListener plus a separate
        // clickListener for the ImageView. Each listener then calls the appropriate method from
        // the OnCLickListenerInterface
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    onClickListenerInterface.onItemClick(position);
                }
            }
        });
        itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    onClickListenerInterface.onItemLongClick(position);
                }
                return true;
            }
        });
        fishImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                        onClickListenerInterface.onImageClick(position);
                }
            }
        });
    }

    public void bind(Fish fish) {
        String species = fish.getSpecies();
        File picturesDirectory = new File("/storage/emulated/0/Pictures/FishLogger");
        File file = new File(picturesDirectory, fish.getFishId()+".jpg");
        if (file.exists()){
            // Setting setHasCustomImage(true) does not actually update the value in firestore
            // but makes the onImageClick() display the drawable/custom image properly regardless
            // of if the image is on this device or another device
            fish.setHasCustomImage(true);
            Glide.with(MyApplication.getAppContext()).load(file).into(fishImageView);
            Log.d("FishLoggerApp", "file.exists() for " + fish.getFishId());
        } else {
            // Setting setHasCustomImage(false) does not actually update the value in firestore
            // but makes the onImageClick() display the drawable/custom image properly regardless
            // of if the image is on this device or another device
            fish.setHasCustomImage(false);
            switch (species){
                case "Ahven":
                    fishImageView.setImageResource(R.drawable.ahven);
                    break;
                case "Hauki":
                    fishImageView.setImageResource(R.drawable.hauki);
                    break;
                case "Kuha":
                    fishImageView.setImageResource(R.drawable.kuha);
                    break;
                case "Siika":
                    fishImageView.setImageResource(R.drawable.siika);
                    break;
                case "Kuore":
                    fishImageView.setImageResource(R.drawable.kuore);
                    break;
                case "Muikku":
                    fishImageView.setImageResource(R.drawable.muikku);
                    break;
                case "Made":
                    fishImageView.setImageResource(R.drawable.made);
                    break;
                case "Taimen":
                    fishImageView.setImageResource(R.drawable.taimen);
                    break;
                case "Lohi":
                    fishImageView.setImageResource(R.drawable.lohi);
                    break;
                case "Lahna":
                    fishImageView.setImageResource(R.drawable.lahna);
                    break;
            }
        }

        String fullDateString = fish.getDate();
        String dateString = parseDate(fullDateString);
        speciesTextView.setText(species);
        dateTextView.setText(dateString);
        locationTextView.setText(fish.getLatitude() + "," + fish.getLongitude());
        lengthTextView.setText(String.valueOf(fish.getLength()));
        weightTextView.setText(String.valueOf(fish.getWeight()));
    }

    // In reviewing my code with ChatGPT it suggested that any images still loading while
    // the RecyclerView items go off screen should be cleared to avoid memory leaks.
    // So this method is called in the overridden FishAdapter.onViewDetachedFromWindow()
    public void clearGlideLoading(){
        Glide.with(itemView.getContext()).clear(fishImageView);
    }


    // Helper function to hide seconds from catch date. (Could be removed for simplicity's sake
    // in the future. The editing of a fish displays seconds anyway and looks less cluttered than I thought)
    public String parseDate(String fullDateString) {
        String formatWithSeconds = "yyyy-MM-dd HH:mm:ss";
        String formatWithoutSeconds = "yyyy-MM-dd HH:mm";

        SimpleDateFormat inputFormat = new SimpleDateFormat(formatWithSeconds);
        SimpleDateFormat outputFormat = new SimpleDateFormat(formatWithoutSeconds);

        try {
            Date date = inputFormat.parse(fullDateString);
            return outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Function to retrieve the ImageView so it can be accessed in FishListActivity's
    // implementation of OnClickListenerInterface.onImageClick()
    public ImageView getFishImageView() {
        return fishImageView;
    }
}
