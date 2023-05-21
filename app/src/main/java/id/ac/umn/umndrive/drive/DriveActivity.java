package id.ac.umn.umndrive.drive;

import static android.view.View.GONE;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import id.ac.umn.umndrive.R;
import id.ac.umn.umndrive.Utils;
import id.ac.umn.umndrive.databinding.ActivityDriveBinding;
import id.ac.umn.umndrive.driveDetail.DriveDetailActivity;

public class DriveActivity extends FragmentActivity implements OnMapReadyCallback {
    Location currentLocation;
    FusedLocationProviderClient fusedLocationProviderClient;
    LocationManager manager;

    private DriveAdapter adapter;
    private GoogleMap mMap;
    private ActivityDriveBinding binding;
    private boolean isOnline = false;
    private String type = "";
    private String selectedOrderId = "";
    private HashMap<String, Object> currentOrder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
        }

        binding = ActivityDriveBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        adapter = new DriveAdapter((id, latLng, data) -> {
            selectedOrderId = id;
            mMap.clear();

            MarkerOptions options = new MarkerOptions();
            options.position(latLng);
            mMap.addMarker(options);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 20));

            currentOrder = data;
        });

        binding.botSheet.rvCustomers.setAdapter(adapter);
        binding.botSheet.rvCustomers.setLayoutManager(new LinearLayoutManager(this));
        binding.botSheet.rvCustomers.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));

        binding.botSheet.btnChooseCustomer.setOnClickListener(view -> {
            binding.botSheet.llChooseCustomer.setVisibility(View.GONE);
            binding.botSheet.llCustomer.setVisibility(View.VISIBLE);

            currentOrder.put("status", "foundDriver");
            binding.botSheet.tvName.setText("Nama penumpang: " + currentOrder.get("custName"));
            binding.botSheet.tvDestination.setText("Tujuan " + currentOrder.get("destination"));

            updateDesc("Driver ditemukan dan akan menjemputmu! ", "Pergi menjemput");
        });

        binding.botSheet.btnAction.setOnClickListener(view -> {
            String currentText = binding.botSheet.btnAction.getText().toString();

            if (isOnline) {
                if (currentText.equals("Pergi menjemput")) {
                    currentOrder.put("status", "foundDriver");
                    updateDesc("Pergi menjemput", "Sudah sampai di titik jemput");

                    binding.botSheet.llChooseCustomer.setVisibility(GONE);
                    binding.botSheet.llCustomer.setVisibility(View.VISIBLE);
                } else if (currentText.equals("Sudah sampai di titik jemput")) {
                    updateDesc("Sudah sampai di titik jemput", "Sudah sampai di tujuan");
                } else {
                    int total = Objects.equals(type, "mobil") ? 25000 : 15000;
                    currentOrder.put("total", total);
                    currentOrder.put("status", "done");
                    currentOrder.put("orderDate", new Date().getTime());

                    storeInHistory();
                    updateDesc("Sudah sampai di tujuan", "Selesai");
                    updateUserData();
                }
            }
        });

        binding.fabOnline.setOnClickListener(view -> {
            if (!type.isEmpty()) {
                if (!isOnline) {
                    binding.botSheet.llCar.setEnabled(false);
                    binding.botSheet.llMotor.setEnabled(false);
                    binding.botSheet.rvCustomers.setVisibility(View.VISIBLE);
                    binding.botSheet.btnChooseCustomer.setVisibility(View.VISIBLE);
                    binding.fabOnline.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_red_light)));
                } else {
                    binding.botSheet.llCar.setEnabled(true);
                    binding.botSheet.llMotor.setEnabled(true);
                    binding.botSheet.rvCustomers.setVisibility(View.GONE);
                    binding.botSheet.btnChooseCustomer.setVisibility(View.GONE);
                    binding.fabOnline.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.teal_200)));
                }

                isOnline = !isOnline;
            }
        });

        binding.botSheet.llMotor.setOnClickListener(view -> {
            type = "motor";
            getCustomers();

            binding.botSheet.ivMotor.setBackground(AppCompatResources.getDrawable(this, R.drawable.green_circle));
            binding.botSheet.ivCar.setBackground(AppCompatResources.getDrawable(this, R.drawable.gray_circler));
        });
        binding.botSheet.llCar.setOnClickListener(view -> {
            type = "mobil";
            getCustomers();

            binding.botSheet.ivCar.setBackground(AppCompatResources.getDrawable(this, R.drawable.green_circle));
            binding.botSheet.ivMotor.setBackground(AppCompatResources.getDrawable(this, R.drawable.gray_circler));
        });

        getDriverData();
        getCurrentOrder();
    }

    @Override
    protected void onStart() {
        super.onStart();
        fetchLastLocation();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        fetchLastLocation();
    }

    private void getCurrentOrder() {
        onLoading(true);

        String id = Utils.getStringFromPref(this, "id");
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("orders")
                .whereEqualTo("driverId", id)
                .whereEqualTo("status", "foundDriver")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    onLoading(false);

                    if (documentSnapshot.size() > 0) {
                        DocumentChange doc = documentSnapshot.getDocumentChanges().get(0);
                        Map<String, Object> data = doc.getDocument().getData();
                        currentOrder = (HashMap<String, Object>) data;
                        selectedOrderId = (String) data.get("custId");

                        binding.botSheet.llChooseCustomer.setVisibility(GONE);
                        binding.botSheet.llCustomer.setVisibility(View.VISIBLE);
                        binding.botSheet.llCar.setEnabled(false);
                        binding.botSheet.llMotor.setEnabled(false);

                        type = (String) data.get("type");
                        if (data.get("type").equals("motor")) {
                            binding.botSheet.ivMotor.setBackground(AppCompatResources.getDrawable(this, R.drawable.green_circle));
                        } else {
                            binding.botSheet.ivCar.setBackground(AppCompatResources.getDrawable(this, R.drawable.green_circle));
                        }

                        binding.botSheet.tvName.setText("Nama Penumpang " + (String) data.get("custName"));
                        binding.botSheet.tvDestination.setText("Tujuan " + (String) data.get("destination"));
                        binding.botSheet.btnAction.setText((String) data.get("nextDesc"));
                    }
                });
    }

    private void getDriverData() {
        onLoading(true);

        String id = Utils.getStringFromPref(this, "id");
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("drivers").document(id).get()
                .addOnSuccessListener(documentSnapshot -> {
                    onLoading(false);

                    Map<String, Object> data = documentSnapshot.getData();
                    if(data != null) {
                        boolean isCar = (boolean) data.get("isCar");
                        boolean isMotor = (boolean) data.get("isMotor");

                        if(!isCar) {
                            binding.botSheet.separator.setVisibility(GONE);
                            binding.botSheet.llCar.setVisibility(GONE);
                        }

                        if (!isMotor) {
                            binding.botSheet.separator.setVisibility(GONE);
                            binding.botSheet.llMotor.setVisibility(GONE);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    onLoading(false);
                    Utils.showToast(DriveActivity.this, e.getMessage());
                });
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

                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 20));
                mMap.setMyLocationEnabled(true);
            }
        }).addOnFailureListener(e -> Utils.showToast(DriveActivity.this, e.getMessage()));
    }

    private void getCustomers() {
        onLoading(true);

        String id = Utils.getStringFromPref(this, "id");
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("orders")
                .whereEqualTo("status", "finding")
                .whereEqualTo("type", type)
                .addSnapshotListener((value, error) -> {
                    onLoading(false);

                    List<HashMap<String, Object>> orders = new ArrayList<>();

                    if (value != null) {
                        if(!value.getDocumentChanges().isEmpty()) {

                            for (DocumentChange doc : value.getDocumentChanges()) {
                                Map<String, Object> data = doc.getDocument().getData();

                                if (!Objects.equals(data.get("custId"), id)) {
                                    data.put("id", doc.getDocument().getId());
                                    orders.add((HashMap<String, Object>) data);
                                }
                            }

                            if(!orders.isEmpty()) {
                                HashMap<String, Object> firstItem = orders.get(0);
                                selectedOrderId = (String) firstItem.get("id");
                                currentOrder = firstItem;

                                LatLng latLng = new LatLng((double) currentOrder.get("lat"), (double) currentOrder.get("lng"));
                                MarkerOptions options = new MarkerOptions();
                                options.position(latLng);
                                mMap.addMarker(options);
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 20));

                            }

                            adapter.setOrders(orders);
                        }
                    }
                });
    }

    private void updateDesc(String desc, String nextDesc) {
        onLoading(true);

        String id = Utils.getStringFromPref(this, "id");
        String name = Utils.getStringFromPref(this, "name");
        String nim = Utils.getStringFromPref(this, "nim");
        String imageUrl = Utils.getStringFromPref(this, "imageUrl");

        currentOrder.put("driverId", id);
        currentOrder.put("driverName", name);
        currentOrder.put("driverNim", nim);
        currentOrder.put("driverImage", imageUrl);
        currentOrder.put("description", desc);
        currentOrder.put("nextDesc", nextDesc);

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("orders").document(selectedOrderId).set(currentOrder)
                .addOnSuccessListener(unused -> {
                    onLoading(false);
                    binding.botSheet.btnAction.setText(nextDesc);
                }).addOnFailureListener(e -> {
                    onLoading(false);
                    Utils.showToast(DriveActivity.this, e.getMessage());
                });
    }

    private void storeInHistory() {
        onLoading(true);

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("histories").add(currentOrder)
                .addOnSuccessListener(documentReference -> {
                    onLoading(false);
                    Intent i = new Intent(DriveActivity.this, DriveDetailActivity.class);
                    i.putExtra("data", currentOrder);
                    startActivity(i);
                    finish();
                }).addOnFailureListener(e -> {
                    onLoading(false);
                    Utils.showToast(DriveActivity.this, e.getMessage());
                });
    }

    private void updateUserData() {
        String id = Utils.getStringFromPref(this, "id");
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        firestore.collection("users").document(id).update(new HashMap<String, Object>() {{
            put("lastDrive", new Date().getTime());
        }});

        firestore.collection("users").document((String) currentOrder.get("custId")).update(new HashMap<String, Object>() {{
            put("lastRide", new Date().getTime());
        }});
    }

    private void onLoading(boolean value) {
        if (value) {
            binding.loading.loadingScreen.setVisibility(View.VISIBLE);
        } else {
            binding.loading.loadingScreen.setVisibility(View.GONE);
        }
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