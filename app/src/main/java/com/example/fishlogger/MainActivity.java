package com.example.fishlogger;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "FishLoggerApp";
    // Permission request properties
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };
    private static final String[] REQUIRED_PERMISSIONS_API33 = {
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.READ_MEDIA_IMAGES
    };
    private static final int PERMISSION_REQUEST_CODE = 123;
    private int permissionRequestCount = 0;
    private static final int MAX_PERMISSION_REQUEST_COUNT = 1;

    // Authorization properties
    private FirebaseFirestore mFirestore;
    private static FirebaseUser user;
    private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new FirebaseAuthUIActivityResultContract(),
            new ActivityResultCallback<FirebaseAuthUIAuthenticationResult>() {
                @Override
                public void onActivityResult(FirebaseAuthUIAuthenticationResult result) {
                    onSignInResult(result);
                }
            }
    );

    // Properties for OpenStreetMap
    private boolean isLocationServicesDialogShown = false;
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private Location mCurrentLocation;
    private MapView mapView = null;
    private IMapController mapController;
    private FishRepository fishRepository;
    private boolean mIsLocationEnabled;
    private List<Marker> fishMarkerList = new ArrayList<>();
    private Marker mUserMarker;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate()");
        context = this;
        mapView = findViewById(R.id.mapView);
        fishRepository = new FishRepository();
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        // Disable zoom buttons, so only pinching works for zooming. Comment following line
        // for emulator testing
        mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        mapController = mapView.getController();
        mapController.setZoom(20.0);
        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);


        // This is a bit messy, but this if-else checks if the required permissions are granted.
        // If yes: it checks if a user is signed in. If not: Firebase sign-in intent is launched
        // via 'createSignInIntent()
        // If permissions are not granted we enter 'requestPermissions()' to request them
        if (allPermissionsGranted()){
            Log.d(TAG, "Entered if(allPermissionsGranted())");
            user = checkUser();
            if (user == null){
                createSignInIntent();
            }
        } else {
            Log.d(TAG, "Entered else: requestPermissions()");
            requestPermissions();
        }
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart()");
        super.onStart();
        mLocationListener = new LocationListener() {
            // onLocationChanged() sets a marker to the user's current location and removes the old one-
            // It sets 'mCurrentLocation' to the location provided. If 'mUserMarker' is null,
            // a new Marker is created. The old marker at the old location is removed from the MapView,
            // current location is saved, and the marker is set to the new location. InfoWindow is disabled
            // for the user's location marker and the marker is assigned a different icon.
            @Override
            public void onLocationChanged(@NonNull Location location) {
                mCurrentLocation = location;
                if (mUserMarker == null){
                    mUserMarker = new Marker(mapView);
                }
                mapView.getOverlays().remove(mUserMarker);
                GeoPoint userLocation = new GeoPoint(mCurrentLocation.getLatitude(),mCurrentLocation.getLongitude());
                mUserMarker.setPosition(userLocation);
                mUserMarker.setInfoWindow(null);
                mUserMarker.setIcon(getDrawable(R.drawable.person_pin_circle_24));
                mapView.getOverlays().add(mUserMarker);
                //mapController.setCenter(userLocation);
                mapView.invalidate();
            }
            // If the user disables location 'mCurrentLocation' is set to null and a dialog that
            // sets the positive button to lead to location settings is shown. A separate boolean
            // is used to prevent the dialog from showing twice (once for GPS and once for network provider)
            @Override
            public void onProviderDisabled(@NonNull String provider) {
                Log.d(TAG, "MainActivity: onProviderDisabled()");
                //LocationListener.super.onProviderDisabled(provider);
                mCurrentLocation = null;
                if (!isLocationServicesDialogShown) {
                    isLocationServicesDialogShown = true;
                    promptLocationServices(context);
                }
            }
            // If location is set back on, startPollingLocation() is called to start showing the user's
            // location on the MapView again. The boolean used for preventing the dialog described above
            // is set back to 'false'.
            @Override
            public void onProviderEnabled(@NonNull String provider) {
                Log.d(TAG, "MainActivity: onProviderEnabled()");
                startPollingLocation();
                isLocationServicesDialogShown = false;
            }
        };
    }

    // Request required permissions. Permission requesting was very frustrating to build but it
    // seems to work ok now...
    private void requestPermissions(){
        Log.d(TAG, "Inside function requestPermissions()");
        if (android.os.Build.VERSION.SDK_INT <= 32){
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
        }
        if (android.os.Build.VERSION.SDK_INT >= 33){
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS_API33, PERMISSION_REQUEST_CODE);
        }
    }

    // Check if required permissions granted and return 'true' if yes, otherwise return 'false'
    private boolean allPermissionsGranted(){
        Log.d(TAG, "Inside function allPermissionsGranted()");
        if (android.os.Build.VERSION.SDK_INT <= 32){
            Log.d(TAG, "Inside if: SDK_INT <= 32");
            for(String permission : REQUIRED_PERMISSIONS){
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                    Log.d(TAG ,"allPermissionsGranted() returned false on first for-loop");
                    return false;
                }
            }
        }
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            Log.d(TAG, "Inside if: SDK_INT >= 33");
            for (String permission : REQUIRED_PERMISSIONS_API33) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                    Log.d(TAG ,"allPermissionsGranted() returned false on first for-loop");
                    return false;
                }
            }
        }
        return true;
    }

    // This method checks what the result of requesting the permissions was. This could be simplified
    // as it is messy and I'm not sure it is implemented totally correctly.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        Log.d(TAG, "Inside function onRequestPermissionsResult()");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && android.os.Build.VERSION.SDK_INT <= 32) {
            Log.d(TAG, "SDK <= 32, requestCode == PERMISSION_REQUEST_CODE");
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED ||
                    grantResults[1] != PackageManager.PERMISSION_GRANTED ||
                    grantResults[2] != PackageManager.PERMISSION_GRANTED ||
                    grantResults[3] != PackageManager.PERMISSION_GRANTED ||
                    grantResults[4] != PackageManager.PERMISSION_GRANTED ||
                    grantResults[5] != PackageManager.PERMISSION_GRANTED ||
                    grantResults[6] != PackageManager.PERMISSION_GRANTED){
                Log.d(TAG, "grantResults[i] != PackageManager.PERMISSION_GRANTED aka permission denied");
                // Permission denied. Ask again until under threshold
                permissionRequestCount++;
                if (permissionRequestCount < MAX_PERMISSION_REQUEST_COUNT){
                    Log.d(TAG, "permissionRequestCount < MAX_PERMISSION_REQUEST_COUNT");
                    requestPermissions();
                } else {
                    // Open a dialog that explains permissions are necessary
                    showPermissionExplanationDialog();
                }
            } else {
                createSignInIntent();
            }
        }
        if (requestCode == PERMISSION_REQUEST_CODE && android.os.Build.VERSION.SDK_INT >= 33) {
            Log.d(TAG, "SDK >= 33, requestCode == PERMISSION_REQUEST_CODE");
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED ||
                grantResults[1] != PackageManager.PERMISSION_GRANTED ||
                    grantResults[2] != PackageManager.PERMISSION_GRANTED ||
                    grantResults[3] != PackageManager.PERMISSION_GRANTED ||
                    grantResults[4] != PackageManager.PERMISSION_GRANTED ||
                    grantResults[5] != PackageManager.PERMISSION_GRANTED){
                Log.d(TAG, "grantResults[i] != PackageManager.PERMISSION_GRANTED aka permission denied");
                // Permission denied. Ask again until under threshold
                permissionRequestCount++;
                if (permissionRequestCount < MAX_PERMISSION_REQUEST_COUNT){
                    Log.d(TAG, "permissionRequestCount < MAX_PERMISSION_REQUEST_COUNT");
                    requestPermissions();
                } else {
                    // Open a dialog that explains permissions are necessary
                    showPermissionExplanationDialog();
                }
            } else {
                createSignInIntent();
            }
        }
    }

    // This opens a dialog that explains that the app needs to have permission to use location and
    // storage (also camera which was added later...). The positive button leads to the app's
    // settings in the phone
    private void showPermissionExplanationDialog() {
        Log.d(TAG, "showPermissionExplanationDialog()");
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        dialogBuilder.setMessage("The app needs to have permission to use camera, location and storage to work correctly");

        // Open app settings for user to allow permission for location manually
        dialogBuilder.setPositiveButton("Open settings",(appSettingsDialog, openSettingsButton) -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri appUri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(appUri);
            startActivity(intent);
        });

        dialogBuilder.setNegativeButton("Cancel", null);
        dialogBuilder.show();
    }

    // This opens a dialog that prompts the user to set location services on. The positive button
    // leads to the location settings in the phone
    private void promptLocationServices(Context context){
        Log.d(TAG, "promptLocationServices()");
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        dialogBuilder.setMessage("Location services are disabled. Please enable them.");

        dialogBuilder.setPositiveButton("Ok",(locationDialog, okButton) -> {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        });

        dialogBuilder.setNegativeButton("Cancel", null);
        dialogBuilder.show();
        isLocationServicesDialogShown = true;
    }

    // This is tied to 'Login' button in the .xml. It opens the Firebase sign-in intent.
    public void login(View view){
        createSignInIntent();
    }

    // Method that sets email as the authentication method and launches the sign-in intent.
    public void createSignInIntent() {
        Log.d(TAG, "createSignInIntent()");
        // [START auth_fui_create_intent]
        // Choose authentication providers
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build());

        // Create and launch sign-in intent
        Intent signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build();
        signInLauncher.launch(signInIntent);
        // [END auth_fui_create_intent]
    }

    // This method checks the result from the sign-in process. If sign in succeeded the user is
    // saved to a variable and polling for the location of the user is started. At the moment
    // this method only checks for canceling the sign in and shows a toast message. No other errors
    // are checked at this moment.
    private void onSignInResult(FirebaseAuthUIAuthenticationResult result) {
        Log.d(TAG, "onSignInResult()");
        IdpResponse response = result.getIdpResponse();
        if (result.getResultCode() == RESULT_OK) {
            Log.d(TAG, "onSignInResult() resultcode is OK");
            // Successfully signed in
            user = checkUser();
            String userID = user.getUid();
            Log.d(TAG, userID);
            mFirestore = FirebaseFirestore.getInstance();
            startPollingLocation();
        } else {
            Log.d(TAG, "onSignInResult() sign-in failed");
            if (response == null){
                Toast.makeText(context, "Canceled login.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // This method returns the currently signed in user
    public static FirebaseUser checkUser(){
        user = FirebaseAuth.getInstance().getCurrentUser();
        return user;
    }

    // Method to launch AddFishActivity
    public void launchAddFishActivity(View view) {
        Log.d(TAG, "launchAddFishActivity");
        Intent intent = new Intent(this, AddFishActivity.class);
        startActivity(intent);
    }

    // Method to launch FishListActivity
    public void launchFishListActivity(View view) {
        Log.d(TAG, "launchFishListActivity");
        Intent intent = new Intent(this, FishListActivity.class);
        startActivity(intent);
    }

    // Method that gets the current user's logged catches from Firestore and adds them to a list.
    // For each Fish in that list a Marker is put on the MapView. The marker's icon is set to non-default
    // icon and clicking the marker shows that Fish's species, length and weight in the info window.
    public void setFishMarkersOnMap(){
        Log.d(TAG, "setFishMarkersOnMap()");
        fishRepository.getAllFish(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                Log.d(TAG, "onComplete()");
                if (task.isSuccessful()) {
                    List<Fish> fishList = new ArrayList<>();

                    // Remove all "old" markers from MapView in case a fish was deleted
                    // and close all infowindows in case a fish's data was updated
                    // so no outdated info is shown (no "auto-update" implemented atm...)
                    for (Marker fishMarker : fishMarkerList) {
                        fishMarker.getInfoWindow().close();
                        mapView.getOverlays().remove(fishMarker);
                    }

                    // Clear the whole list in case it contains markers for deleted fish
                    fishMarkerList.clear();

                    // Query Firebase and add all fish to fishList
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Fish fish = document.toObject(Fish.class);
                        fishList.add(fish);
                    }

                    // Create markers for each fish in fishList and add them to the map
                    for (Fish fish : fishList) {
                        Marker fishMarker = new Marker(mapView);
                        fishMarker.setIcon(getDrawable(R.drawable.push_pin));
                        String info = fish.getLength() + " cm<br>" + fish.getWeight() + " g";
                        fishMarker.setTitle(fish.getSpecies());
                        fishMarker.setSnippet(info);
                        fishMarker.setId(fish.getFishId());
                        fishMarker.setPosition(new GeoPoint(Double.parseDouble(fish.getLatitude()), Double.parseDouble(fish.getLongitude())));

                        // Add the fish marker to the map and to the list
                        mapView.getOverlays().add(fishMarker);
                        fishMarkerList.add(fishMarker);
                    }

                    // Update the map view
                    mapView.invalidate();
                } else {
                    Log.d(TAG, "task was not successful");
                }
            }
        });
    }

    // Method to start requesting location updates from the providers. If location services are
    // disabled show a dialog that prompts to enable location.
    public void startPollingLocation(){
        Log.d(TAG, "startPollingLocation()");
        try {
            mIsLocationEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    || mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            if (mIsLocationEnabled){
                Log.d(TAG, "Location provider is enabled");
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
                mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener);
            } else {
                Log.d(TAG, "Location provider not enabled");
                if (!isLocationServicesDialogShown){
                    Log.d(TAG, "!isLocationServicesDialogShown");
                    isLocationServicesDialogShown = true;
                    promptLocationServices(context);
                }

            }
        } catch (SecurityException e){
            Log.d(TAG, "Error: App does not have permission to access location data");
            Toast.makeText(this,"App doesn't have permission to access location data", Toast.LENGTH_SHORT).show();
        }
    }

    // Start requesting the location updates again when this Activity is active again. Set the
    // Markers for all the Fish on the MapView again in case a Fish was deleted or removed in
    // another activity
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"onResume()");
        startPollingLocation();
        setFishMarkersOnMap();
        //isLocationServicesDialogShown = false;
    }

    // Stop requesting location updates when this activity is not active to save battery and
    // other resources
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "MainActivity: onPause()");
        if (mIsLocationEnabled){
            mLocationManager.removeUpdates(mLocationListener);
        }
    }

    // Method to center the MapView to user's current location. This is tied to 'Locate' button in
    // the .xml file. There was some reason to call startPollingLocation() in the 'else'-branch
    // but I don't remember what it exactly was... Probably to then call promptLocationServices()
    // via startPollingLocation()'s 'else' branch to get the user to set location services on
    public void centerToCurrentLocation(View view){
        if (mCurrentLocation != null){
            GeoPoint locationToCenterTo = new GeoPoint(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
            mapController.setCenter(locationToCenterTo);
            mapController.setZoom(15.0);
        } else {
            Toast.makeText(this, "Location not yet available. Try again.", Toast.LENGTH_SHORT).show();
            startPollingLocation();
        }
    }
}