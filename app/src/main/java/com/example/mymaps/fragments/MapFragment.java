package com.example.mymaps.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.mymaps.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapFragment extends Fragment implements OnMapReadyCallback {
    private final AutocompletePrediction place;

    public MapFragment(AutocompletePrediction place) {
        this.place = place;
    }

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    TextView searchResultTV;
    private ActivityResultLauncher<String> requestPermissionLauncher;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());
        TextView searchEditText = view.findViewById(R.id.search_edit_text);
        searchResultTV = view.findViewById(R.id.search_result_text);
        TextView chnageCta = view.findViewById(R.id.change_cta);
        LinearLayout currentLocCTA = view.findViewById(R.id.current_loc_cta);

        if (!Places.isInitialized()) {
            Places.initialize(getContext(), getString(R.string.map_api_key));
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);

        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.map, mapFragment)
                    .commit();
        }
        mapFragment.getMapAsync(this);

        if (place != null) {
            searchResultTV.setText(place.getPrimaryText(null));
        }
        searchEditText.setOnClickListener(v -> navigateToSearchPlacePage());
        chnageCta.setOnClickListener(v -> navigateToSearchPlacePage());
        currentLocCTA.setOnClickListener(v -> checkAndFetchLocation());
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        checkAndFetchLocation();
                    } else {
                        Toast.makeText(getActivity(), "Location permission denied", Toast.LENGTH_SHORT).show();
                    }
                }
        );


        return view;

    }

    @Override
    public void onResume() {
        super.onResume();
        Bundle args = getArguments();
        if (args != null && args.containsKey("resultKey")) {
            String primaryText = args.getString("resultKey");
            searchResultTV.setText(primaryText);
            // Clear arguments to avoid processing them again
            setArguments(null);
        } else {
            checkAndFetchLocation();
        }
    }

    private void checkAndFetchLocation() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Permission is already granted, fetch the location
            fusedLocationClient.getLastLocation()
                    .addOnCompleteListener(new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                Location location = task.getResult();
                                LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                                mMap.addMarker(new MarkerOptions().position(currentLatLng).title("Current Location"));
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
                                double latitude = location.getLatitude();
                                double longitude = location.getLongitude();

                                getAddressFromLocation(latitude, longitude);
                            } else {
                                Toast.makeText(getActivity(), "Failed to get location", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

        } else {
            // Request location permission
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void navigateToSearchPlacePage() {

        MapSearchFragment mapFragment = new MapSearchFragment();
        FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
        //fragmentTransaction.replace(R.id.fragment_container, mapFragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }


    private void getAddressFromLocation(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);

                // Fetch the address details
                String addressString = address.getAddressLine(0); // Full address
                String city = address.getLocality();              // City name
                String state = address.getAdminArea();            // State name
                String country = address.getCountryName();        // Country name
                String postalCode = address.getPostalCode();      // Postal code

                searchResultTV.setText(addressString);
            } else {
                Toast.makeText(getContext(), "No address found for the location", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error fetching address: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

    }
}