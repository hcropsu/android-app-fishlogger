package com.example.fishlogger;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import java.io.File;

// This class is a custom Dialog that shows either the drawable matching the Fish's species
// or a custom image if it is found on the device.
public class FishLargeImageDialog extends Dialog {
    private ImageView fishLargeImage;
    private Context context;

    // Constructor for drawables if Fish.hasCustomImage is false
    // (drawable is then passed in FishListActivity)
    public FishLargeImageDialog(Context context, Drawable drawable){
        super(context);
        this.context = context;
        init(drawable);
    }

    // Other constructor if Fish.hasCustomImage is true
    // (fishId is then passed in FishListActivity)
    public FishLargeImageDialog(Context context, String fishId){
        super(context);
        this.context = context;
        init(fishId);
    }

    // One init() for drawables. The drawable is set to the ImageView.
    private void init(Drawable drawable) {
        setContentView(R.layout.dialog_fish_large_image);
        fishLargeImage = findViewById(R.id.fishLargeImage);
        fishLargeImage.setImageDrawable(drawable);
    }

    // Other init() for Strings.
    private void init(String fishId) {
        setContentView(R.layout.dialog_fish_large_image);
        fishLargeImage = findViewById(R.id.fishLargeImage);
        // Get the screen width and save it to a variable to make the image be of that size
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        // Load the image file to the ImageView with Glide to avoid boiler plate code
        // which would be needed to set the rotation correctly
        Glide.with(MyApplication.getAppContext()).load(new File("/storage/emulated/0/Pictures/FishLogger/"+fishId+".jpg")).override(screenWidth,screenWidth).into(fishLargeImage);

    }
}
