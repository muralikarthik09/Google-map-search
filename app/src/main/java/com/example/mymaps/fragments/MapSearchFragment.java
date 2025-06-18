package com.example.mymaps.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mymaps.R;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.PlaceTypes;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MapSearchFragment extends Fragment implements onItemClickListener {
    private EditText searchEditText;
    private RecyclerView recyclerView;
    private PlacesAdapter placesAdapter;
    private PlacesClient placesClient;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    public MapSearchFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search_places, container, false);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());
        recyclerView = view.findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        searchEditText = view.findViewById(R.id.search_edit_text);
        placesClient = Places.createClient(getContext());
        LinearLayout currentLocCTA = view.findViewById(R.id.current_loc_cta);
        currentLocCTA.setOnClickListener(v -> checkAndFetchLocation());
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) {
                    handler.removeCallbacks(searchRunnable);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                searchRunnable = () -> findSearchResults();
                handler.postDelayed(searchRunnable, 300);
            }
        });

        return view;
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
                setResultBundle(addressString);

            } else {
                Toast.makeText(getContext(), "No address found for the location", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error fetching address: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    private void findSearchResults() {

        AutocompleteSessionToken token = AutocompleteSessionToken.newInstance();
        RectangularBounds bounds = RectangularBounds.newInstance(
                new LatLng(23.63936, 68.14712), new LatLng(28.20453, 97.34466));
        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setLocationBias(bounds)
                //.setLocationRestriction(bounds)
                .setCountries("IND")
                .setTypesFilter(Arrays.asList(PlaceTypes.ADDRESS))
                .setSessionToken(token)
                .setQuery(searchEditText.getText().toString())
                .build();

        placesClient.findAutocompletePredictions(request).addOnSuccessListener(response -> {
            if (placesAdapter == null) {
                placesAdapter = new PlacesAdapter(this);
                recyclerView.setAdapter(placesAdapter);
            }
            placesAdapter.setData(response.getAutocompletePredictions());
            placesAdapter.notifyDataSetChanged();

        }).addOnFailureListener((exception) -> {
            if (exception instanceof ApiException) {
                ApiException apiException = (ApiException) exception;
                Log.e("apiException", apiException.toString());
            }
        });
    }

    @Override
    public void onItemClick(AutocompletePrediction place) {
        String text = String.valueOf(place.getPrimaryText(null));
        if (!TextUtils.isEmpty(place.getSecondaryText(null))) {
            text = text + "\n" + place.getSecondaryText(null);
        }
        setResultBundle(text);
    }

    private void setResultBundle(String text) {
        Bundle resultBundle = new Bundle();
        resultBundle.putString("resultKey", text);
        getParentFragmentManager().popBackStack();
        getParentFragmentManager().findFragmentByTag("MapFragment")
                .setArguments(resultBundle);
    }
}