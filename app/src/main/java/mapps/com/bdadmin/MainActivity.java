package mapps.com.bdadmin;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@SuppressLint("SetTextI18n")
public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    ListView dataListView;
    TextView noDataView, navUsername;
    ProgressBar progressBar;
    LinearLayout filterBar, filters;

    String username, password;

    List<Data> dataItems;

    static String[] filter = {"", "", "-1"};
    String[] bloodTypes = {"A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-"};

    RequestQueue queue;

    boolean gettingData = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Bundle data = getIntent().getExtras();

        if (data != null) {
            username = data.getString("username", "");
            password = data.getString("password", "");
        }

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getBaseContext(), FormActivity.class);
                intent.putExtra("username", username);
                intent.putExtra("password", password);
                startActivityForResult(intent, 0);
            }
        });

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        dataListView = findViewById(R.id.data_list);
        noDataView = findViewById(R.id.no_data);
        progressBar = findViewById(R.id.progressBar2);
        filterBar = findViewById(R.id.filter_bar);
        filters = findViewById(R.id.filters);
        navUsername = navigationView.getHeaderView(0).findViewById(R.id.nav_username);
        navUsername.setText(username);

        queue = Volley.newRequestQueue(this);

        preGetData(false);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
        }
    }

    void checkFilters() {
        filters.removeAllViews();

        if (filter[2].equals("-1")) {
            filter[2] = "";
        }

        String[] labels = {"Name: ", "Address: ", "Type: "};

        for (int i = 0; i < 3; i++) {
            if (!filter[i].trim().equals("")) {
                @SuppressLint("InflateParams") TextView filterView = (TextView) getLayoutInflater().inflate(R.layout.filter_view, null);

                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                layoutParams.setMargins(15, 15, 15, 15);
                filterView.setLayoutParams(layoutParams);
                filterView.setText(labels[i] + (i == 2 ? bloodTypes[Integer.parseInt(filter[i])] : filter[i]));
                filters.addView(filterView);
            }
        }

        if (filter[2].equals("")) {
            filter[2] = "-1";
        }

        if (filters.getChildCount() > 0)
            filterBar.setVisibility(View.VISIBLE);
        else
            filterBar.setVisibility(View.GONE);
    }

    void preGetData(boolean search) {
        if (!search && gettingData)
            return;

        if (!Helper.isConnected(this)) {
            noDataView.setText("Not connected to the internet.");
            noDataView.setVisibility(View.VISIBLE);
            return;
        }
        gettingData = true;
        checkFilters();
        dataListView.setVisibility(View.GONE);
        noDataView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        getData();
    }

    public void getData() {
        final String url = Helper.apiURL + "get_data";
        final JSONArray arData = new JSONArray();
        final JSONObject data = new JSONObject();
        try {
            JSONArray criteria = new JSONArray();
            for (int i = 0; i < 3; i++)
                criteria.put(filter[i]);
            criteria.put("");
            data.put("criteria", criteria);
            arData.put(data);

            JsonArrayRequest dataRequest = new JsonArrayRequest(Request.Method.POST, url, arData, new Response.Listener<JSONArray>() {
                @Override
                public void onResponse(JSONArray response) {
                    postGetData();
                    try {
                        response.remove(0);
                        setData(response);
                    } catch (JSONException e) {
                        Helper.toastError(getBaseContext(), 1);
                        e.printStackTrace();
                    }
                }

            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Helper.toastError(getBaseContext(), 0);
                    postGetData();
                }
            });
            queue.add(dataRequest);
        } catch (JSONException e) {
            Helper.toastError(getBaseContext(), 1);
            postGetData();
            e.printStackTrace();
        }
    }

    void postGetData() {
        gettingData = false;
        progressBar.setVisibility(View.GONE);
    }

    void setData(JSONArray items) throws JSONException {
        if (items.length() == 0) {
            noDataView.setVisibility(View.VISIBLE);
            return;
        } else
            noDataView.setVisibility(View.GONE);

        dataItems = new ArrayList<>();

        JSONObject item;
        for (int i = 0; i < items.length(); i++) {
            item = items.getJSONObject(i);
            dataItems.add(new Data(item.getLong("id"),
                    bloodTypes[item.getInt("blood_type")],
                    item.getString("name"),
                    item.getString("address"),
                    item.getString("email"),
                    item.getString("mobile_number"),
                    item.getDouble("latitude"),
                    item.getDouble("longitude"),
                    item.getBoolean("is_user_data")));
        }

        dataListView.setVisibility(View.VISIBLE);
        dataListView.setAdapter(new DataAdapter(getBaseContext(), dataItems));
    }

    void deleteData(long id) {
        dataListView.setVisibility(View.GONE);
        noDataView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        JSONObject data = new JSONObject();
        try {
            data.put("username", username);
            data.put("password", password);
            data.put("id", id);

            final String url = Helper.apiURL + "delete_data";
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, data, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    preGetData(true);
                    Toast.makeText(getBaseContext(), "Success", Toast.LENGTH_LONG).show();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    preGetData(true);
                    Helper.toastError(getBaseContext(), 0);
                }
            });

            queue.add(request);
        } catch (JSONException e) {
            Helper.toastError(getBaseContext(), 1);
            e.printStackTrace();
        }
    }

    public void clearFilters(View view) {
        filter[0] = "";
        filter[1] = "";
        filter[2] = "-1";
        preGetData(true);
    }

    class DataAdapter extends BaseAdapter {
        private Context context;
        private LayoutInflater inflater;
        private List<Data> items;

        DataAdapter(Context context, List<Data> items) {
            this.context = context;
            this.items = items;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int i) {
            return items.get(i);
        }

        @Override
        public long getItemId(int i) {
            return items.get(i).getId();
        }

        @SuppressLint("InflateParams")
        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (inflater == null)
                inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
            if (view == null)
                view = inflater.inflate(R.layout.list_item, null);

            final Data d = items.get(i);

            TextView item_title = view.findViewById(R.id.item_title),
                    content = view.findViewById(R.id.content);

            final Button edit = view.findViewById(R.id.edit), delete = view.findViewById(R.id.delete);

            if (d.isUserData()) {
                // Restrict editing of user data
                edit.setVisibility(View.GONE);
            } else {
                edit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Send data of current item to FormActivity for editing
                        Intent intent = new Intent(getBaseContext(), FormActivity.class);
                        intent.putExtra("username", username);
                        intent.putExtra("password", password);
                        intent.putExtra("data", d);
                        startActivityForResult(intent, 1);
                    }
                });
            }

            delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Show delete dialog for confirming delete operation
                    DeleteDialog deleteDialog = new DeleteDialog();
                    Bundle args = new Bundle();
                    args.putString("msg", "Name: " + d.getName());
                    args.putLong("id", d.getId());
                    deleteDialog.setArguments(args);
                    deleteDialog.show(getFragmentManager(), "delete");
                }
            });

            String userData = "";

            if (d.isUserData()) {
                userData = "<br><br>User data cannot be edited";
            }

            item_title.setText("Blood type: " + d.getBlood_type());
            content.setText(Html.fromHtml("<b>Name:</b> " + d.getName() + "<br>" +
                    "<b>Mobile Number:</b> " + d.mobile_number + "<br>" +
                    "<b>Address:</b> " + d.getAddress() + "<br>" +
                    "<b>Email:</b> " + d.getEmail() +
                    userData), TextView.BufferType.SPANNABLE);
            return view;
        }
    }

    @Override
    protected void onResume() {
        preGetData(false);
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_filter) {
            FilterDialog filterDialog = new FilterDialog();
            filterDialog.show(getFragmentManager(), "filter");
            return true;
        } else if (id == R.id.action_refresh) {
            preGetData(false);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static class FilterDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = getActivity().getLayoutInflater();

            @SuppressLint("InflateParams") final View dialogView = inflater.inflate(R.layout.filter_dialog, null);
            final TextView filterName = dialogView.findViewById(R.id.filter_name),
                    filterAddress = dialogView.findViewById(R.id.filter_address);
            final Spinner filterBloodType = dialogView.findViewById(R.id.filter_blood_type);

            filterName.setText(filter[0]);
            filterAddress.setText(filter[1]);
            filterBloodType.setSelection(Integer.parseInt(filter[2]) + 1);

            builder.setTitle("Filter by")
                    .setView(dialogView)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            filter[0] = filterName.getText().toString();
                            filter[1] = filterAddress.getText().toString();
                            filter[2] = String.valueOf(filterBloodType.getSelectedItemPosition() - 1);
                            ((MainActivity) getActivity()).preGetData(true);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            FilterDialog.this.getDialog().cancel();
                        }
                    });
            return builder.create();
        }
    }

    public static class DeleteDialog extends DialogFragment {
        String msg;
        long id;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            msg = getArguments().getString("msg");
            id = getArguments().getLong("id");
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            builder.setTitle("Delete data?")
                    .setMessage("Are you sure you want to delete this data?\n" + msg)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ((MainActivity) getActivity()).deleteData(id);
                        }
                    });

            return builder.create();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_about) {
            Intent intent = new Intent(getBaseContext(), AboutActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_settings) {
            Intent intent = new Intent(getBaseContext(), SettingsActivity.class);
            startActivity(intent);
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}

class Data implements Serializable {
    private long id;
    private String blood_type;
    private String name;
    private String address;
    private String email;
    String mobile_number;
    private double latitude, longitude;
    private boolean isUserData;

    Data(long id, String blood_type, String name, String address, String email, String mobile_number, double latitude, double longitude, boolean isUserData) {
        this.id = id;
        this.blood_type = blood_type;
        this.name = name;
        this.address = address;
        this.email = email;
        this.mobile_number = mobile_number;
        this.latitude = latitude;
        this.longitude = longitude;
        this.isUserData = isUserData;
    }

    public long getId() {
        return id;
    }

    String getBlood_type() {
        return blood_type;
    }

    String getName() {
        return name;
    }

    String getAddress() {
        return address;
    }

    String getEmail() {
        return email;
    }

    String getMobile_number() {
        return mobile_number;
    }

    double getLatitude() {
        return latitude;
    }

    double getLongitude() {
        return longitude;
    }

    boolean isUserData() {
        return isUserData;
    }
}