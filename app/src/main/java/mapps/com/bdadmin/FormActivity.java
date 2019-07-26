package mapps.com.bdadmin;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Pair;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FormActivity extends AppCompatActivity implements OnMapReadyCallback {

    //=====UI variables=====
    MapView mapView;
    ScrollView scrollView;
    EditText name, email, address, mobile_number;
    Spinner blood_type;
    TextView nameLabel, emailLabel, addressLabel, mobileNumberLabel, mapLabel;
    ProgressBar progressBar;

    List<Pair<String, String>> keyLabels = new ArrayList<>();
    TextView[] labelViews;

    GoogleMap map;

    String username, password;

    LatLng location;

    RequestQueue queue;

    boolean edit;

    Data d;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form);

        Toolbar toolbar = findViewById(R.id.form_toolbar);
        setSupportActionBar(toolbar);

        scrollView = findViewById(R.id.scrollView);

        // Form fields
        name = findViewById(R.id.form_name);
        email = findViewById(R.id.form_email);
        mobile_number = findViewById(R.id.form_mobile_number);
        address = findViewById(R.id.form_address);
        blood_type = findViewById(R.id.form_blood_type);

        // Form labels
        nameLabel = findViewById(R.id.name_label);
        addressLabel = findViewById(R.id.address_label);
        emailLabel = findViewById(R.id.email_label);
        mobileNumberLabel = findViewById(R.id.mobile_number_label);
        mapLabel = findViewById(R.id.map_label);

        progressBar = findViewById(R.id.toolbar_progress_bar);

        Bundle data = getIntent().getExtras();

        if (data != null) {
            // Credentials for authenticating requests
            username = data.getString("username", "");
            password = data.getString("password", "");
        }

        if (data != null && data.containsKey("data")) {
            // Populate form in case of edit
            edit = true;
            d = (Data) data.getSerializable("data");

            if (d != null) {
                name.setText(d.getName());
                email.setText(d.getEmail());
                mobile_number.setText(d.getMobile_number());
                address.setText(d.getAddress());

                HashMap<String, Integer> bloodMap = new HashMap<>();
                bloodMap.put("A+", 0);
                bloodMap.put("A-", 1);
                bloodMap.put("B+", 2);
                bloodMap.put("B-", 3);
                bloodMap.put("O+", 4);
                bloodMap.put("O-", 5);
                bloodMap.put("AB+", 6);
                bloodMap.put("AB-", 7);

                blood_type.setSelection(bloodMap.get(d.getBlood_type()));
                location = new LatLng(d.getLatitude(), d.getLongitude());
            }
        }

        // Init error handling views, data
        keyLabels.add(new Pair<>("name", "Name"));
        keyLabels.add(new Pair<>("email", "Email"));
        keyLabels.add(new Pair<>("mobile_number", "Mobile Number"));
        keyLabels.add(new Pair<>("address", "Address"));
        keyLabels.add(new Pair<>("map", "Select location by long clicking on the map"));

        labelViews = new TextView[]{nameLabel, emailLabel, mobileNumberLabel, addressLabel, mapLabel};

        // Init map view
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        queue = Volley.newRequestQueue(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setMyLocationButtonEnabled(false);

        if (edit) {
            // In case of edit, location is known.
            // Put marker at location, move camera
            map.addMarker(new MarkerOptions().position(location));
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 12f));
        }

        // Fix for scroll view interrupting map touch
        map.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() {
                scrollView.requestDisallowInterceptTouchEvent(true);
            }
        });

        map.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                scrollView.requestDisallowInterceptTouchEvent(false);
            }
        });

        // Add marker on long touch
        map.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                location = latLng;
                map.clear();
                map.addMarker(new MarkerOptions().position(latLng));
            }
        });

        // return if don't have permission for location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
            return;
        }

        // If not edit, zoom on current location
        if (!edit) {
            FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location loc) {
                            if (loc != null) {
                                map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(loc.getLatitude(), loc.getLongitude()), 12f));
                            } else {
                                // Default map camera move if location is null
                                map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(20.5937, 78.9629), 3.5f));
                            }
                        }
                    });
        }

        // Show user location
        map.setMyLocationEnabled(true);
    }

    public void submit(final View view) {
        // clear any previous errors
        Helper.clearErrors(keyLabels, labelViews);

        // Location required check
        if (location == null) {
            mapLabel.setText(Html.fromHtml("Select location by long clicking on the map<br>" +
                    "<font color='red'>Location is required</font>"), TextView.BufferType.SPANNABLE);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        try {
            JSONObject data = new JSONObject();
            data.put("username", username);
            data.put("password", password);

            if (edit) {
                // id of object used for editing
                data.put("id", d.getId());
            }

            data.put("name", name.getText().toString());
            data.put("email", email.getText().toString());
            data.put("address", address.getText().toString());
            data.put("mobile_number", mobile_number.getText().toString());
            data.put("blood_type", blood_type.getSelectedItemPosition());
            data.put("latitude", location.latitude);
            data.put("longitude", location.longitude);

            final String url = Helper.apiURL + "add_data";
            final JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, data, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    progressBar.setVisibility(View.GONE);
                    try {
                        if (response.getBoolean("status")) {
                            Toast.makeText(getBaseContext(), "Success", Toast.LENGTH_LONG).show();
                            finish();
                        } else {
                            Helper.handleErrors(response, keyLabels, labelViews);
                        }
                    } catch (JSONException e) {
                        Toast.makeText(getBaseContext(), "Unexpected error occurred, code: 1", Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getBaseContext(), "Error, check internet connection", Toast.LENGTH_LONG).show();
                }
            });

            queue.add(request);
        } catch (JSONException e) {
            Toast.makeText(getBaseContext(), "Unexpected error occurred, code: 1", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        mapView.onResume();
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}
