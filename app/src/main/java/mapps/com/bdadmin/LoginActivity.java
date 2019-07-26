package mapps.com.bdadmin;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Pair;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class LoginActivity extends AppCompatActivity {
    EditText usernameView, passwordView;
    ProgressBar progressBar;

    List<Pair<String, String>> keyLabels = new ArrayList<>();
    TextView[] labelViews = new TextView[3];

    RequestQueue queue;

    final String VERSION = "0.1.0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        usernameView = findViewById(R.id.username);
        passwordView = findViewById(R.id.password);
        progressBar = findViewById(R.id.progressBar);

        labelViews[0] = findViewById(R.id.nfe);
        labelViews[1] = findViewById(R.id.username_label);
        labelViews[2] = findViewById(R.id.password_label);

        keyLabels.add(new Pair<>("non_field_errors", ""));
        keyLabels.add(new Pair<>("username", "User name"));
        keyLabels.add(new Pair<>("password", "Password"));

        queue = Volley.newRequestQueue(this);
    }

    public void login(View view) {
        Helper.clearErrors(keyLabels, labelViews);
        progressBar.setVisibility(View.VISIBLE);

        final String url = Helper.apiURL + "admin_login",
                username = usernameView.getText().toString(),
                password = passwordView.getText().toString();

        try {
            final JSONObject data = new JSONObject();
            data.put("username", username);
            data.put("password", password);

            JsonObjectRequest loginRequest = new JsonObjectRequest(Request.Method.POST, url, data, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    progressBar.setVisibility(View.GONE);
                    try {
                        boolean loggedIn = response.getBoolean("status");

                        if (!response.getString("version").equals(VERSION)) {
                            UpdateDialog updateDialog = new UpdateDialog();
                            updateDialog.show(getFragmentManager(), "updateDialog");
                        } else if (loggedIn) {
                            Intent intent = new Intent(getBaseContext(), MainActivity.class);
                            intent.putExtra("username", username);
                            intent.putExtra("password", password);

                            startActivity(intent);
                            finish();
                        } else {
                            Helper.handleErrors(response, keyLabels, labelViews);
                        }
                    } catch (JSONException e) {
                        Toast.makeText(getBaseContext(), "Unexpected error occurred, code: 1", Toast.LENGTH_LONG).show();
                        progressBar.setVisibility(View.GONE);
                        e.printStackTrace();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    progressBar.setVisibility(View.GONE);
                    Helper.toastError(getBaseContext(), 0);
                }
            });
            queue.add(loginRequest);
        } catch (JSONException e) {
            Toast.makeText(getBaseContext(), "Unexpected error occurred, code: 1", Toast.LENGTH_LONG).show();
            progressBar.setVisibility(View.GONE);
            e.printStackTrace();
        }
    }

    @SuppressLint("ValidFragment")
    public static class UpdateDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            builder.setTitle("Update Available")
                    .setMessage("Please get the latest version of the app to continue.")
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            getActivity().finish();
                        }
                    });
            return builder.create();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            getActivity().finish();
        }
    }
}
