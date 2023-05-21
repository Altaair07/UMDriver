package id.ac.umn.umndrive.ride;

import static android.view.View.GONE;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;

import id.ac.umn.umndrive.R;
import id.ac.umn.umndrive.Utils;
import id.ac.umn.umndrive.databinding.ActivityRideBinding;
import id.ac.umn.umndrive.riderDetail.RideDetailActivity;

public class RideActivity extends FragmentActivity implements OnMapReadyCallback {
    Location currentLocation;
    FusedLocationProviderClient fusedLocationProviderClient;
    LocationManager manager;

    private GoogleMap mMap;
    private LatLng latLng;
    private HashMap<String, Object> currentOrder;
    private ActivityRideBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
        }

        binding = ActivityRideBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        binding.btnOrder.setOnClickListener(view -> {
            if (!binding.etPickupPoint.getText().toString().isEmpty()) {
                binding.llOrder.setVisibility(GONE);
                binding.llRide.setVisibility(View.VISIBLE);
            }
        });
        binding.btnCancel.setOnClickListener(view -> cancelOrder());
        binding.llCar.setOnClickListener(view -> setOrder("mobil"));
        binding.llMotor.setOnClickListener(view -> setOrder("motor"));
        binding.btnDetail.setOnClickListener(view -> {
            Intent i = new Intent(this, RideDetailActivity.class);
            i.putExtra("data", currentOrder);
            startActivity(i);
            finish();
        });

        getCurrentOrder();
    }

    @Override
    protected void onStart() {
        super.onStart();
        fetchLastLocation();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        fetchLastLocation();
    }

    private void fetchLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]
                    {Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            return;
        }
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                currentLocation = location;
                LatLng latLng = new LatLng(currentLocation.getLatitude(),
                        currentLocation.getLongitude());

                this.latLng = latLng;
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 20));
                mMap.setMyLocationEnabled(true);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Utils.showToast(RideActivity.this, e.getMessage());
            }
        });
    }

    private void getCurrentOrder() {
        String id = Utils.getStringFromPref(this, "id");
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("orders").document(id).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.getData() != null) {
                        if (documentSnapshot.getData().get("status") == "finding") {
                            binding.llOrder.setVisibility(GONE);
                            binding.llWaiting.setVisibility(View.VISIBLE);
                        } else if (documentSnapshot.getData().get("status") == "foundDriver") {
                            binding.llOrder.setVisibility(GONE);
                            binding.llPickup.setVisibility(View.VISIBLE);
                        }

                        listenStatus();
                    }
                });
    }

    private void setOrder(String type) {
        String id = Utils.getStringFromPref(this, "id");
        String name = Utils.getStringFromPref(this, "name");
        String nim = Utils.getStringFromPref(this, "nim");

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("orders").document(id).set(new HashMap<String, Object>() {{
            put("custId", id);
            put("custName", name);
            put("custNim", nim);
            put("destination", binding.etPickupPoint.getText().toString());
            put("orderDate", new Date().getTime());
            put("status", "finding");
            put("lat", latLng.latitude);
            put("lng", latLng.longitude);
            put("type", type);
        }}).addOnSuccessListener(unused -> {
            binding.llRide.setVisibility(GONE);
            binding.llWaiting.setVisibility(View.VISIBLE);

            listenStatus();
        }).addOnFailureListener(e -> Utils.showToast(RideActivity.this, e.getMessage()));
    }

    private void cancelOrder() {
        String id = Utils.getStringFromPref(this, "id");

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("orders").document(id).update(new HashMap<String, Object>(){{
            put("status", "canceled");
        }}).addOnSuccessListener(unused -> {
            finish();
            Utils.showToast(RideActivity.this, "Order canceled!");
        }).addOnFailureListener(e -> Utils.showToast(RideActivity.this, e.getMessage()));
    }

    private void listenStatus() {
        String id = Utils.getStringFromPref(this, "id");
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("orders").document(id).addSnapshotListener((value, error) -> {
            if (value != null) {
                currentOrder = (HashMap<String, Object>) value.getData();

                String status = value.getData().get("status").toString();

                switch (status) {
                    case "finding":
                        binding.llOrder.setVisibility(GONE);
                        binding.llWaiting.setVisibility(View.VISIBLE);
                        break;
                    case "foundDriver":
                        binding.llWaiting.setVisibility(GONE);
                        binding.llPickup.setVisibility(View.VISIBLE);
                        binding.driverStatus.setText((String) value.get("description"));
                        String driverName = value.getData().get("driverName").toString();
                        String imageUrl = value.getData().get("driverImage").toString();
                        String driverNim = value.getData().get("driverNim").toString();

                        binding.driverName.setText(driverName);
                        Glide.with(getApplicationContext()).load(imageUrl).into(binding.ivDriverAvatar);
                        break;
                    case "done":
                        binding.btnDetail.setVisibility(View.VISIBLE);
                        binding.driverStatus.setText((String) value.get("description"));
                        break;
                }
            }

            if (error != null) {
                Utils.showToast(RideActivity.this, error.getMessage());
            }
        });
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", (dialog, id) -> startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                .setNegativeButton("No", (dialog, id) -> finish());
        final AlertDialog alert = builder.create();
        alert.show();
    }
}