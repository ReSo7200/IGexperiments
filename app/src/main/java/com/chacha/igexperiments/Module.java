package com.chacha.igexperiments;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import android.annotation.SuppressLint;
import android.app.AndroidAppHelper;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * @noinspection ALL
 */
public class Module implements IXposedHookLoadPackage, IXposedHookZygoteInit {


    private String className, methodName, secondClassName;

    /**
     * Init the preferences
     */
    private String initPreferences() {
        try {
            XposedPreferences.loadPreferences();
            XposedPreferences.reloadPrefs();

            String mode = XposedPreferences.getPrefs().getString("Mode", "Normal");

            if (mode.equals("Normal")) {
                XposedBridge.log("(IGExperiments) Using class name from Github");
                return "Normal";
            } else if (mode.equals("Auto")) {
                XposedBridge.log("(IGExperiments) Dynamic searching");
                return "Auto";
            }

            XposedBridge.log("(IGExperiments) Using class name from preferences");
            return "Hecker";

        } catch (Exception e) {
            XposedBridge.log("(IGExperiments) Exception in initPreferences, defaulting to 'Normal': " + e.getMessage());
            return "Normal"; // Default to "Normal" if an exception occurs
        }
    }


    /**
     * Initialize the class and method to hook
     */
    private void initElemToHook() {
        try {
            className = XposedPreferences.getPrefs().getString("className", "");
            methodName = XposedPreferences.getPrefs().getString("methodName", "");
            secondClassName = XposedPreferences.getPrefs().getString("secondClassName", "");


            if (className.equals("")) {
                XposedBridge.log("(IGExperiments) No class name found, using default");
                className = Utils.DEFAULT_CLASS_TO_HOOK;
                methodName = Utils.DEFAULT_METHOD_TO_HOOK;
                secondClassName = Utils.DEFAULT_SECOND_CLASS_TO_HOOK;
            }
        } catch (Exception ignored) {

        }

    }

    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) {
        XposedPreferences.loadPreferences();
    }


    @SuppressLint("DefaultLocale")
    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws ClassNotFoundException, InstantiationException, IllegalAccessException {


        if (lpparam.packageName.equals(Utils.MY_PACKAGE_NAME)) {
            findAndHookMethod(Utils.MY_PACKAGE_NAME + ".MainActivity", lpparam.classLoader,
                    "isModuleActive", XC_MethodReplacement.returnConstant(true));
        }

        if (lpparam.packageName.equals(Utils.IG_PACKAGE_NAME)) {
            ClassLoader classLoader = lpparam.classLoader;
            Class<?> UserSessionClass = classLoader.loadClass("com.instagram.common.session.UserSession");
            Class<?> Do0Class = classLoader.loadClass("X.Do0");


            boolean success = false;
            try {
                String type = initPreferences();
                initElemToHook();


                // TESTING --- HIDE SEEN STATE ON DIRECT --- WORKS --- VERSION 356.0.0.0.43


//                XposedHelpers.findAndHookMethod("X.40q", classLoader, "A00", Do0Class, UserSessionClass, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, long.class, boolean.class, new XC_MethodHook() {
//                    @Override
//                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//                        // Immediately prevent the method from executing further and block the "seen" status update
//                        param.setResult(null);
//                        showToast("Ghost Mode: Seen status not updated.");
//                        XposedBridge.log("Ghost Mode Active: Blocked 'mark as seen'.");
//                    }
//                });

                // Retrive current insta version
                Class<?> parserCls = XposedHelpers.findClass("android.content.pm.PackageParser", lpparam.classLoader);
                Object parser = parserCls.newInstance();
                File apkPath = new File(lpparam.appInfo.sourceDir);
                Object pkg = XposedHelpers.callMethod(parser, "parsePackage", apkPath, 0);
                String versionName = (String) XposedHelpers.getObjectField(pkg, "mVersionName");

                // Testing for HIDE SEEN STATE ON DIRECT:

                GhostModeDM ghostModeDM = new GhostModeDM();
                ghostModeDM.handleGhostMode(lpparam, versionName);


                // Testing for new DevOptions
                // Get the selected mode
                String mode = initPreferences();
                DevOptionsEnable devOptionsEnable = new DevOptionsEnable();
                devOptionsEnable.handleDevOptions(lpparam, versionName, mode);


            } catch (Exception ignored) {
            }

        }


    }

    public static String getJSONContent() {
        try {
            Log.println(Log.INFO, "IGexperiments", "Reading raw content from github file");
            URL url = new URL("https://raw.githubusercontent.com/ReSo7200/IGExperimentsUpdates/master/hooks.json");
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

    private static ArrayList<InfoIGVersion> versions;

    public static void loadIGVersions() {
        versions = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(getJSONContent());
            JSONArray jsonArray = jsonObject.getJSONArray("ig_versions");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject infoVersions = jsonArray.getJSONObject(i);
                InfoIGVersion versionInfo = new InfoIGVersion(
                        infoVersions.getString("version"),
                        infoVersions.getString("class_to_hook"),
                        infoVersions.getString("method_to_hook"),
                        infoVersions.getString("second_class_to_hook"),
                        infoVersions.getString("download")
                );
                versions.add(versionInfo);
            }
        } catch (JSONException e) {
            Log.e("IGEXPERIMENTS", "Error while parsing JSON");
            e.printStackTrace();
        }
    }

    // Retrieve InfoIGVersion by version
    public InfoIGVersion getInfoByVersion(String version) {
        loadIGVersions();
        for (InfoIGVersion info : versions) {
            if (info.getVersion().contains(version)) {
                return info;
            }

        }
        // if the installed Instagram version wasn't supported:
        showToast("Version is not supported, Use Hecker mode or use a supported version!");

        return null; // Version not found
    }

    private void showToast(final String text) {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(AndroidAppHelper.currentApplication().getApplicationContext(), text, Toast.LENGTH_LONG).show());
    }

    public String getTime() {
        // Format for displaying the current date and time
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // Get current date and time
        String currentTime = sdf.format(new Date());

        // Log the current time
        return "Time: " + currentTime + " - ";
    }

}
