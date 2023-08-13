package com.chacha.igexperiments;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.coniy.fileprefs.FileSharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;
import eu.chainfire.libsuperuser.Shell;

public class MainActivity extends AppCompatActivity {
    private LinearLayout layoutHeckerMode;
    private EditText customClassName, customMethodName;
    private TextView textHookedClass, textViewError;
    private ImageButton btnDonate;
    private SwitchCompat switchUseHeckerMode;
    private Button btnHook, btnDownload, btnKill;
    private Spinner igVersionsSpinner;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private ArrayList<InfoIGVersion> iGVersionsInfos;

    /**
     * Init views preferences
     */
    private void initViewsPreferences() {
        sharedPreferences = Preferences.loadPreferences(this);
        editor = Preferences.getEditor();

        boolean useHeckerMode = sharedPreferences.getBoolean("useHeckerMode", false);
        switchUseHeckerMode.setChecked(useHeckerMode);
        layoutHeckerMode.setVisibility(useHeckerMode ? View.VISIBLE : View.GONE);

        textHookedClass.setText(String.format(getResources().getString(R.string.hooked_class),
                sharedPreferences.getString("className", Utils.DEFAULT_CLASS_TO_HOOK),
                sharedPreferences.getString("methodName", Utils.DEFAULT_METHOD_TO_HOOK)));
    }

    /**
     * Init the views
     */
    private void initViews(){
        customClassName = findViewById(R.id.editTextClassName);
        customMethodName = findViewById(R.id.editTextMethodName);
        textHookedClass = findViewById(R.id.textView3);
        switchUseHeckerMode = findViewById(R.id.useHeckerMode);
        btnHook = findViewById(R.id.btnHook);
        igVersionsSpinner = findViewById(R.id.igVersionsSpinner);
        layoutHeckerMode = findViewById(R.id.layoutHeckerMode);
        btnDownload = findViewById(R.id.btnDownload);
        btnKill = findViewById(R.id.btnKill);
        textViewError = findViewById(R.id.textViewError);
        btnDonate = findViewById(R.id.btnDonate);
    }

    /**
     * @return true if an error is detected
     */
    private boolean isErrorDetected(){
        return iGVersionsInfos.size()==0;
    }

    /**
     * Init the spinner with the differents IG versions
     */
    private void initIGVersionsSpinner(){
        ArrayAdapter<InfoIGVersion> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, iGVersionsInfos);

        if(isErrorDetected())
           textViewError.setVisibility(View.VISIBLE);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        igVersionsSpinner.setAdapter(adapter);
        setIGItemPosition();
    }

    /**
     * Init the differents views functions and listeners
     */
    private void initViewsFunctions(){
        customClassName.setText(sharedPreferences.getString("className", Utils.DEFAULT_CLASS_TO_HOOK));
        customMethodName.setText(sharedPreferences.getString("methodName", Utils.DEFAULT_METHOD_TO_HOOK));

        switchUseHeckerMode.setOnCheckedChangeListener((compoundButton, b) -> {
            editor.putBoolean("useHeckerMode", b).commit();
            if(b){
                editor.putString("className", customClassName.getText().toString()).commit();
                editor.putString("methodName", customMethodName.getText().toString()).commit();
            } else {
                editor.putString("className", ((InfoIGVersion) igVersionsSpinner.getSelectedItem()).getClassToHook()).commit();
                editor.putString("methodName", ((InfoIGVersion) igVersionsSpinner.getSelectedItem()).getMethodToHook()).commit();
            }
            FileSharedPreferences.makeWorldReadable(Utils.MY_PACKAGE_NAME, Utils.PREFS_NAME);

            textHookedClass.setText(String.format(getResources().getString(R.string.hooked_class),
                    sharedPreferences.getString("className", Utils.DEFAULT_CLASS_TO_HOOK),
                    sharedPreferences.getString("methodName", Utils.DEFAULT_METHOD_TO_HOOK)));
            layoutHeckerMode.setVisibility(b ? View.VISIBLE : View.GONE);
        });

        btnHook.setOnClickListener(view -> {
            editor.putString("className", customClassName.getText().toString()).commit();
            editor.putString("methodName", customMethodName.getText().toString()).commit();
            FileSharedPreferences.makeWorldReadable(Utils.MY_PACKAGE_NAME, Utils.PREFS_NAME);
            textHookedClass.setText(String.format(getResources().getString(R.string.hooked_class),
                    customClassName.getText().toString(),
                    customMethodName.getText().toString()));
        });

        igVersionsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                editor.putString("className", ((InfoIGVersion) igVersionsSpinner.getSelectedItem()).getClassToHook()).commit();
                editor.putString("methodName", ((InfoIGVersion) igVersionsSpinner.getSelectedItem()).getMethodToHook()).commit();

                if(!switchUseHeckerMode.isChecked())
                    textHookedClass.setText(String.format(getResources().getString(R.string.hooked_class),
                            ((InfoIGVersion) igVersionsSpinner.getSelectedItem()).getClassToHook(),
                            ((InfoIGVersion) igVersionsSpinner.getSelectedItem()).getMethodToHook()));

                FileSharedPreferences.makeWorldReadable(Utils.MY_PACKAGE_NAME, Utils.PREFS_NAME);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        btnDownload.setOnClickListener(view -> {
            if(isErrorDetected()){
                textViewError.setTextSize(textViewError.getTextSize()+0.5f);
                return;
            }

            openUrl(((InfoIGVersion) igVersionsSpinner.getSelectedItem()).getUrl());
        });

        btnKill.setOnClickListener(view -> {
            killAction();
        });

        btnDonate.setOnClickListener(view -> {
            Donation.openDonationLink(this);
        });
    }

    /**
     * Init array of IG versions infos
     */
    private void initIGVersionsInfos(){
        iGVersionsInfos = new ArrayList<>();
        iGVersionsInfos = getIGVersionsInfos();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initViewsPreferences();
        initIGVersionsInfos();
        initIGVersionsSpinner();
        initViewsFunctions();

        Donation.remindDonation(this);

        if(!isModuleActive()){
            textViewError.setText("Module DISABLED!");
            textViewError.setVisibility(View.VISIBLE);
        }
    }

    private static boolean isModuleActive(){
        return false;
    }

    /**
     * Open url in browser
     * @param url url to open
     */
    private void openUrl(String url){
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browserIntent);
    }

    /**
     * Kill and start Instagram app
     */
    private void killAction() {
        if (Shell.SU.available()) {
            try {
                Process su = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(su.getOutputStream());
                os.writeBytes("adb shell" + "\n");
                os.flush();
                os.writeBytes("am force-stop " + Utils.IG_PACKAGE_NAME + "\n");
                os.flush();
                os.writeBytes("am start -n " + Utils.IG_PACKAGE_NAME + "/com.instagram.mainactivity.MainActivity" + "\n");
                os.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else
            Toast.makeText(this, "Root not granted !", Toast.LENGTH_SHORT).show();
        }

    /**
     * @return Return the json of supported IG versions
     */

    private String getJSONContent(){
        try {
            Log.println(Log.INFO, "IGexperiments", "Reading raw content from github file");
            URL url = new URL("https://raw.githubusercontent.com/xHookman/IGexperiments/master/classes_to_hook.json");
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
            Scanner s = new Scanner(url.openStream());
            StringBuilder content = new StringBuilder();
            while (s.hasNextLine()) {
                content.append(s.nextLine());
            }
            return content.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * @return Return an ArrayList of differents supported IG versions
     */
    private ArrayList<InfoIGVersion> getIGVersionsInfos() {
        ArrayList<InfoIGVersion> versions = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(getJSONContent());
            JSONArray jsonArray = jsonObject.getJSONArray("ig_versions");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject infoVersions = jsonArray.getJSONObject(i);
                versions.add(new InfoIGVersion(infoVersions.getString("version"),
                        infoVersions.getString("class_to_hook"),
                        infoVersions.getString("method_to_hook"),
                        infoVersions.getString("download")));
            }
        } catch (JSONException e) {
            Log.e("IGEXPERIMENTS", "Error while parsing JSON");
            e.printStackTrace();
        }
        return versions;
    }

    /**
     * Set the correct position of the spinner depending on the saved version
     */
    private void setIGItemPosition(){
        for (int i = 0; i < iGVersionsInfos.size(); i++) {
            if (iGVersionsInfos.get(i).getClassToHook().equals(sharedPreferences.getString("className", ""))){
                igVersionsSpinner.setSelection(i);
            }
        }
    }
}