package com.chacha.igexperiments;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Handles Ghost Mode for Direct Messages (DM) in Instagram.
 */
public class GhostModeDM {
    public void handleGhostMode(XC_LoadPackage.LoadPackageParam lpparam, String currentVersion) {
        try {
            // Load UserSession class
            ClassLoader classLoader = lpparam.classLoader;
            Class<?> UserSessionClass = classLoader.loadClass("com.instagram.common.session.UserSession");

            // Read the hooks log
            JSONObject hooksLog = readHooksLog();

            if (hooksLog.has(currentVersion)) {
                JSONObject versionLog = hooksLog.getJSONObject(currentVersion);
                if (versionLog.has("GhostMode_DM")) {
                    // If hooks for GhostMode_DM already exist, apply them directly
                    JSONArray ghostModeEntries = versionLog.getJSONArray("GhostMode_DM");
                    for (int i = 0; i < ghostModeEntries.length(); i++) {
                        JSONObject entryGroup = ghostModeEntries.getJSONObject(i);
                        for (Iterator<String> it = entryGroup.keys(); it.hasNext(); ) {
                            String key = it.next();
                            JSONObject entry = entryGroup.getJSONObject(key);
                            String classToHook = entry.getString("class_to_hook");
                            String classAsInput = entry.getString("class_as_input");
                            hookGhostMode(classToHook, classAsInput, UserSessionClass, classLoader);
                        }
                    }
                    return; // No need for further processing
                }
            }

            // Perform dynamic analysis and log the results if not already logged
            performDynamicAnalysis(lpparam, UserSessionClass, currentVersion);
        } catch (Exception e) {
            XposedBridge.log("(GhostModeDM) Error handling Ghost Mode: " + e.getMessage());
        }
    }

    private void hookGhostMode(String classToHook, String classAsInput, Class<?> UserSessionClass, ClassLoader classLoader) {
        try {
            Class<?> param0Class = XposedHelpers.findClass(classAsInput, classLoader);
            XposedHelpers.findAndHookMethod(classToHook, classLoader, "A00",
                    param0Class, UserSessionClass, String.class, String.class,
                    String.class, String.class, long.class, boolean.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            param.setResult(null); // Block the "seen" status update
                            showToast("Ghost Mode: Seen status not updated.");
                            XposedBridge.log("(GhostModeDM) Successfully blocked 'mark as seen'.");
                        }
                    });

            XposedBridge.log("(GhostModeDM) Successfully hooked class: " + classToHook);
        } catch (Exception e) {
            XposedBridge.log("(GhostModeDM) Failed to hook class: " + classToHook + " - " + e.getMessage());
        }
    }

    private void performDynamicAnalysis(XC_LoadPackage.LoadPackageParam lpparam, Class<?> UserSessionClass, String currentVersion) {
        String characters = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Map<String, JSONObject> foundHooks = new HashMap<>();
        int count = 1;

        // Dynamic search for the appropriate classes
        for (char first : characters.toCharArray()) {
            for (char second : characters.toCharArray()) {
                for (char third : characters.toCharArray()) {
                    String classToHook = "X." + first + second + third;

                    try {
                        Class<?> cls = XposedHelpers.findClass(classToHook, lpparam.classLoader);

                        for (Method method : cls.getDeclaredMethods()) {
                            try {
                                if (method.getName().equals("A00") && method.getParameterCount() == 8) {
                                    Class<?>[] paramTypes = method.getParameterTypes();
                                    if (paramTypes[1] == UserSessionClass && paramTypes[2] == String.class &&
                                            paramTypes[3] == String.class && paramTypes[4] == String.class &&
                                            paramTypes[5] == String.class && paramTypes[6] == long.class &&
                                            paramTypes[7] == boolean.class) {

                                        String param0ClassName = paramTypes[0].getName();
                                        hookGhostMode(classToHook, param0ClassName, UserSessionClass, lpparam.classLoader);

                                        // Log the found class under a named key (FirstOne, SecondOne, etc.)
                                        JSONObject entry = new JSONObject();
                                        entry.put("class_to_hook", classToHook);
                                        entry.put("class_as_input", param0ClassName);

                                        foundHooks.put("Entry" + count++, entry);
                                    }
                                }
                            }catch (NoClassDefFoundError | XposedHelpers.ClassNotFoundError e) {
                               // XposedBridge.log("(IGExperiments) Skipping method due to missing dependency: " + e.getMessage());
                            } catch (Exception e) {
                               // XposedBridge.log("(IGExperiments) General exception while inspecting method: " + e.getMessage());
                            }
                        }
                    } catch (NoClassDefFoundError | XposedHelpers.ClassNotFoundError e) {
                       // XposedBridge.log("(IGExperiments) Skipping class " + classToHook + " due to missing dependency: " + e.getMessage());
                    } catch (Exception e) {
                       // XposedBridge.log("(IGExperiments) General exception while inspecting class: " + e.getMessage());
                    }
                }
            }
        }

        // Save found hooks to the log
        try {
            // Retrieve existing hooks log or create a new one
            JSONObject hooksLog = readHooksLog();

            // Check if the current version already exists in the hooks log
            JSONObject versionLog = hooksLog.has(currentVersion) ? hooksLog.getJSONObject(currentVersion) : new JSONObject();

            // Retrieve or create the "GhostMode_DM" log for the current version
            JSONArray ghostModeEntries = versionLog.has("GhostMode_DM") ? versionLog.getJSONArray("GhostMode_DM") : new JSONArray();

            // Add the new found hooks to the "GhostMode_DM" entries
            ghostModeEntries.put(new JSONObject(foundHooks));

            // Update the version log with the new "GhostMode_DM" entries
            versionLog.put("GhostMode_DM", ghostModeEntries);

            // Update the hooks log with the new version log
            hooksLog.put(currentVersion, versionLog);

            // Save the updated hooks log to Shared Preferences
            writeHooksLog(hooksLog);

            XposedBridge.log("(GhostModeDM) Successfully saved hooks log for version: " + currentVersion);
        } catch (Exception e) {
            XposedBridge.log("(GhostModeDM) Error saving hooks log: " + e.getMessage());
        }

    }

    private static final String HOOKS_LOG_PREF = "hooks_log"; // Shared Preferences name
    private static final String HOOKS_LOG_KEY = "hooks_log_data"; // Key for storing hooks log

    private Context getSafeContext() {
        try {
            Context context = AndroidAppHelper.currentApplication();
            if (context == null) {
                XposedBridge.log("(DevOptionsEnable) AndroidAppHelper returned null context. Trying fallback.");
                context = (Context) XposedHelpers.callStaticMethod(
                        XposedHelpers.findClass("android.app.ActivityThread", null),
                        "currentApplication"
                );
            }
            return context;
        } catch (Exception e) {
            XposedBridge.log("(DevOptionsEnable) Failed to retrieve application context: " + e.getMessage());
            return null;
        }
    }
    private JSONObject readHooksLog() {
        try {
            Context context = getSafeContext();
            if (context == null) {
                throw new NullPointerException("(DevOptionsEnable) Context is null. Cannot access SharedPreferences.");
            }

            SharedPreferences sharedPreferences = context.getSharedPreferences(HOOKS_LOG_PREF, Context.MODE_PRIVATE);
            String hooksLogString = sharedPreferences.getString(HOOKS_LOG_KEY, "{}"); // Default to empty JSON

            return new JSONObject(hooksLogString);
        } catch (Exception e) {
            XposedBridge.log("(DevOptionsEnable) Failed to read hooks log from SharedPreferences: " + e.getMessage());
            return new JSONObject(); // Return an empty JSON object on failure
        }
    }

    private void writeHooksLog(JSONObject json) {
        try {
            Context context = getSafeContext();
            if (context == null) {
                throw new NullPointerException("(DevOptionsEnable) Context is null. Cannot access SharedPreferences.");
            }

            SharedPreferences sharedPreferences = context.getSharedPreferences(HOOKS_LOG_PREF, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(HOOKS_LOG_KEY, json.toString(4)); // Format JSON for readability
            editor.apply();

            XposedBridge.log("(DevOptionsEnable) Successfully wrote hooks log to SharedPreferences.");
        } catch (Exception e) {
            XposedBridge.log("(DevOptionsEnable) Failed to write hooks log to SharedPreferences: " + e.getMessage());
        }
    }


    private void showToast(final String text) {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> Toast.makeText(android.app.AndroidAppHelper.currentApplication().getApplicationContext(), text, Toast.LENGTH_LONG).show());
    }
}
