package com.example.fishlogger;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseUser;

import java.time.LocalDateTime;

// This same activity class is used for adding new and editing existing Fish objects. The behaviour
// depends on whether the intent has an existing Fish passed in the launcher of this activity or not.
public class AddFishActivity extends AppCompatActivity {
    private final String TAG = "FishLoggerApp";
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private Location mCurrentLocation;
    private TextView headingTextView;
    private EditText dateEditText;
    private EditText locationEditText;
    private AutoCompleteTextView fishSpeciesAutoCompleteTextView;
    private EditText lengthEditText;
    private EditText weightEditText;
    private LocalDateTime date;
    private String dateString;
    private String fullDateString;
    private int year;
    private int month;
    private int day;
    private int hour;
    private int minute;
    private int second;
    private static String userId;
    private boolean isLocationEnabled;
    private boolean editingFish = false;
    private Fish editedFish;
    private Fish latestFish;
    private Context context;
    private boolean isLocationServicesDialogShown = false;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_fish);
        context = this;
        FirebaseUser user = MainActivity.checkUser();
        if (user != null) {userId = MainActivity.checkUser().getUid();}
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        headingTextView = findViewById(R.id.addFishHeading);
        dateEditText = findViewById(R.id.dateEditText);
        locationEditText = findViewById(R.id.locationEditText);
        fishSpeciesAutoCompleteTextView = findViewById(R.id.addFishAutoCompleteTextView);
        lengthEditText = findViewById(R.id.lengthEditText);
        weightEditText = findViewById(R.id.weightEditText);
        date = LocalDateTime.now();
        year = date.getYear();
        month = date.getMonthValue();
        day = date.getDayOfMonth();
        hour = date.getHour();
        minute = date.getMinute();
        second = date.getSecond();
        fullDateString = String.format("%04d-%02d-%02d %02d:%02d:%02d", year, month, day, hour, minute, second);
        dateString = String.format("%04d-%02d-%02d %02d:%02d", year, month, day, hour, minute);
        dateEditText.setText(dateString);

        // Here is the check if this activity was launched with an existing Fish or not.
        // If yes, the fields are populated with the Fish's values, the passed Fish is saved
        // to variable 'editedFish' and boolean 'editingFish' is set to true for future use. (The 'Save' button's behaviour)
        // If not, the activity start's polling for location through a custom 'getCurrentLocation()'
        // method that takes a LocationAvailableListener interface as a parameter. It sets the
        // latitude and longitude to the appropriate EditText once location is available.
        if (getIntent().hasExtra("fish")){
            editedFish = getIntent().getParcelableExtra("fish");
            headingTextView.setText("Edit your catch");
            dateEditText.setText(editedFish.getDate());
            locationEditText.setText(editedFish.getLatitude() + "," + editedFish.getLongitude());
            fishSpeciesAutoCompleteTextView.setText(editedFish.getSpecies());
            lengthEditText.setText(String.valueOf(editedFish.getLength()));
            weightEditText.setText(String.valueOf(editedFish.getWeight()));
            editingFish = true;
        } else {
            getCurrentLocation(new LocationAvailableListener() {
                @Override
                public void onLocationAvailable(Location location) {
                    locationEditText.setText(String.valueOf(mCurrentLocation.getLatitude()) + ", " + String.valueOf(mCurrentLocation.getLongitude()));
                }
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    // onResume() needs to set the array to the adapter and set the adapter to the
    // AutoCompleteTextView, else the dropdown menu doesn't always show all the options
    @Override
    protected void onResume() {
        super.onResume();
        String[] fishSpeciesArray = getResources().getStringArray(R.array.fish_species_array);
        ArrayAdapter<String> adapterFishSpecies = new ArrayAdapter<>(context, R.layout.fish_species_dropdown_item, fishSpeciesArray);
        fishSpeciesAutoCompleteTextView.setAdapter(adapterFishSpecies);
    }

    // Remove location updates in onPause() to save battery and resources. Might be a better user
    // experience if left on. Then the user wouldn't need to wait for location data for as long...
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "AddFishActivity: onPause()");
        if (isLocationEnabled){
            if (mLocationListener != null){
                mLocationManager.removeUpdates(mLocationListener);
            }
        }
    }

    // This is linked to the 'Save' button in the .xml. Here we decide behaviour based on the boolean
    // 'editingFish'. If we have a Fish that is being edited, the Fish's values are set based on the
    // values in the fields. A dialog then opens via 'confirmEditedFish()' that asks if the user
    // wants to update the Fish.
    // If 'editingFish' is false, we go to 'createAndAddFish()' which creates a new Fish and adds it
    // to the Firestore collection.
    public void saveFish(View view){
        if (editingFish){
            String[] location = locationEditText.getText().toString().split("[, ]", 2);
            String latitude = location[0];
            String longitude = location[1];
            editedFish.setLatitude(latitude);
            editedFish.setLongitude(longitude);
            editedFish.setDate(dateEditText.getText().toString());
            editedFish.setSpecies(fishSpeciesAutoCompleteTextView.getText().toString());
            editedFish.setLength(Float.parseFloat(lengthEditText.getText().toString()));
            editedFish.setWeight(Float.parseFloat(weightEditText.getText().toString()));
            confirmEditedFish(context);
        } else {
            createAndAddFish();
        }
    }

    // This is the method that adds a new Fish. It first checks that the location is not null. Then
    // a new Fish is created (and assigned a unique identifier via the Fish constructor). The other
    // properties are set via setters. The Fish is then saved to a variable for possible future use
    // (passed to the CameraActivity if user chooses to take picture). The fields are emptied and
    // a dialog opens asking if the user wants to take a picture of their catch.
    public void createAndAddFish(){
        if (mCurrentLocation != null) {
            Fish fish = new Fish();
            fish.setUserId(userId);
            fish.setLatitude(String.valueOf(mCurrentLocation.getLatitude()));
            fish.setLongitude(String.valueOf(mCurrentLocation.getLongitude()));
            fish.setDate(fullDateString);
            fish.setSpecies(fishSpeciesAutoCompleteTextView.getText().toString());
            fish.setLength(Float.parseFloat(lengthEditText.getText().toString()));
            fish.setWeight(Float.parseFloat(weightEditText.getText().toString()));
            FishRepository fishRepository = new FishRepository();
            fishRepository.addFish(fish);
            latestFish = fish;
            resetFields();
            Toast.makeText(this, "Added fish!", Toast.LENGTH_SHORT).show();
            addPictureDialog();
        } else {
            Toast.makeText(this, "Location not available yet. Unable to add fish. Try again or check phone settings.", Toast.LENGTH_SHORT).show();
        }
    }

    // Reset the input fields. Empty the species, lenght and weight. Reset the date to current time.
    // Location is left polling like it was.
    private void resetFields(){
        fishSpeciesAutoCompleteTextView.setText("");
        lengthEditText.setText("");
        weightEditText.setText("");
        date = LocalDateTime.now();
        year = date.getYear();
        month = date.getMonthValue();
        day = date.getDayOfMonth();
        hour = date.getHour();
        minute = date.getMinute();
        second = date.getSecond();
        fullDateString = String.format("%04d-%02d-%02d %02d:%02d:%02d", year, month, day, hour, minute, second);
        dateString = String.format("%04d-%02d-%02d %02d:%02d", year, month, day, hour, minute);
        dateEditText.setText(dateString);
    }

    // Method that is shown if user disables location. The positive button opens the appropriate
    // settings from the phone.
    private void promptLocationServices(Context context){
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        dialogBuilder.setMessage("Location services are disabled. Please enable them.");

        dialogBuilder.setPositiveButton("Ok",(locationDialog, okButton) -> {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        });

        dialogBuilder.setNegativeButton("Cancel", null);
        dialogBuilder.show();
    }

    // This opens the dialog for confirming the editing of an existing Fish's values. The positive
    // button put's the 'editedFish' to the 'resultIntent' and sets 'RESULT_OK', then closes this
    // activity. FishListActivity's ActivityResultLauncher called addFishActivityLauncher then checks
    // if a Fish was passed back to that activity. If yes the Fish is updated in that activity
    private void confirmEditedFish(Context context){
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        dialogBuilder.setMessage("Are you sure you want to save changes?");

        dialogBuilder.setPositiveButton("Yes",(editFishDialog, okButton) -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("updatedFish", editedFish);
            setResult(RESULT_OK, resultIntent);
            finish();
        });

        dialogBuilder.setNegativeButton("Cancel", null);
        dialogBuilder.show();
    }


    // The listener interface for setting the location to the appropriate field when it is available
    public interface LocationAvailableListener{
        void onLocationAvailable(Location location);
    }

    // This method starts polling for location if the user is not editing an existing Fish.
    // It takes a custom LocationAvailableListener interface as a parameter.
    // It checks for permissions and that location services are enabled. If yes we set a new
    // LocationListener and override the needed methods. onLocationChanged() uses the listener
    // interface to set the location info to the appropriate EditText
    // (the interfaces onLocationAvailable() is overwritten in the call for getCurrentLocation()).
    // onProviderDisabled() shows a dialog that tells that location services are disabled. Since
    // there are two providers (GPS and network) the method is called twice (once for each provider).
    // To avoid showing the dialog twice, a separate boolean is needed to check if the dialog was
    // already shown.
    // onProviderEnabled() calls the outer getCurrentLocation() and we repeat the steps described here.
    // Lastly, location updates are requested from both GPS and network providers.
    private void getCurrentLocation(LocationAvailableListener locationAvailableListener){
        isLocationEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (isLocationEnabled){
                mLocationListener = new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location location) {
                        mCurrentLocation = location;
                        locationAvailableListener.onLocationAvailable(location);
                    }
                    @Override
                    public void onProviderDisabled(@NonNull String provider) {
                        Log.d(TAG, "AddFishActivity: onProviderDisabled()");
                        //LocationListener.super.onProviderDisabled(provider);
                        mCurrentLocation = null;
                        if (!isLocationServicesDialogShown){
                            promptLocationServices(context);
                            isLocationServicesDialogShown = true;
                        }
                    }
                    @Override
                    public void onProviderEnabled(@NonNull String provider) {
                        getCurrentLocation(new LocationAvailableListener() {
                            @Override
                            public void onLocationAvailable(Location location) {
                                locationEditText.setText(String.valueOf(mCurrentLocation.getLatitude()) + ", " + String.valueOf(mCurrentLocation.getLongitude()));
                            }
                        });
                        Log.d(TAG, "AddFishActivity: onProviderEnabled");
                    }
                };
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
                mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener);
            } else {
                promptLocationServices(context);
            }
        }

    }

    // This function opens a dialog asking if the user wants to take a photo of their catch.
    // Negative button only closes the dialog. Positive button calls a method that opens the
    // CameraActivity.
    public void addPictureDialog(){
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        dialogBuilder.setMessage("Do you want to add a photo of the fish?");

        dialogBuilder.setPositiveButton("Yes",(addPictureDialog, yesButton) -> {
            launchCameraActivityFromAddFish();
        });

        dialogBuilder.setNegativeButton("No", null);
        dialogBuilder.show();
    }

    // This method opens CameraActivity and passes the newly created Fish to that activity.
    public void launchCameraActivityFromAddFish() {
        Intent intent = new Intent(context, CameraActivity.class);
        intent.putExtra("latestFish", latestFish);
        startActivity(intent);
    }
}
