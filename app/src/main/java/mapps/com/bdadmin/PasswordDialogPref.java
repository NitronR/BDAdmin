package mapps.com.bdadmin;

import android.app.AlertDialog;
import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
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

/**
 * Created by Bhanu Pratap Singh on 6/12/2018.
 */

public class PasswordDialogPref extends DialogPreference {

    public PasswordDialogPref(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.pass_pref_dialog);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        builder.setPositiveButton(null, null).setNegativeButton(null, null);
        super.onPrepareDialogBuilder(builder);
    }

    @Override
    protected void onBindDialogView(View view) {
        final EditText username = view.findViewById(R.id.username),
                confirmPassword = view.findViewById(R.id.cpassword),
                newPassword = view.findViewById(R.id.npassword),
                cfpassword = view.findViewById(R.id.cfpassword);
        Button ok = view.findViewById(R.id.pass_pref_ok),
                cancel = view.findViewById(R.id.pass_pref_cancel);
        final ProgressBar progressBar = view.findViewById(R.id.progressBar);
        final TextView labelViews[] = {view.findViewById(R.id.nfe),
                view.findViewById(R.id.username_label),
                view.findViewById(R.id.password_label),
                view.findViewById(R.id.new_password_label)};

        final List<Pair<String, String>> keyLabels = new ArrayList<>();
        keyLabels.add(new Pair<>("nfe", ""));
        keyLabels.add(new Pair<>("username", "Username"));
        keyLabels.add(new Pair<>("password", "Current password"));
        keyLabels.add(new Pair<>("npassword", "New password"));

        final RequestQueue queue = Volley.newRequestQueue(getContext());
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Helper.clearErrors(keyLabels, labelViews);
                String usernameVal = username.getText().toString(),
                        cpasswordVal = confirmPassword.getText().toString(),
                        npasswordVal = newPassword.getText().toString(),
                        cfpasswordVal = cfpassword.getText().toString();
                if (npasswordVal.equals(cfpasswordVal)) {
                    progressBar.setVisibility(View.VISIBLE);
                    JSONObject data = new JSONObject();
                    try {
                        data.put("username", usernameVal);
                        data.put("password", cpasswordVal);
                        data.put("npassword", npasswordVal);

                        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, Helper.apiURL + "change_pass",
                                data, new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                progressBar.setVisibility(View.GONE);
                                try {
                                    if (response.getBoolean("status")) {
                                        Toast.makeText(getContext(), "Successfully changed password.", Toast.LENGTH_LONG).show();
                                        getDialog().dismiss();
                                    } else {
                                        Helper.handleErrors(response, keyLabels, labelViews);
                                    }
                                } catch (JSONException e) {
                                    progressBar.setVisibility(View.GONE);
                                    Helper.toastError(getContext(), 1);
                                    e.printStackTrace();
                                }
                            }
                        }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                progressBar.setVisibility(View.GONE);
                                Helper.toastError(getContext(), 0);
                            }
                        });

                        queue.add(request);
                    } catch (JSONException e) {
                        progressBar.setVisibility(View.GONE);
                        Helper.toastError(getContext(), 1);
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(view.getContext(), "New password and confirm password does not match.", Toast.LENGTH_LONG).show();
                }
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDialog().cancel();
            }
        });

        super.onBindDialogView(view);
    }
}