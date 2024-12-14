package com.chacha.igexperiments;

import static com.chacha.igexperiments.Module.getJSONContent;

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

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Handles enabling developer options in Instagram.
 */
public class DevOptionsEnable {

    public void handleDevOptions(XC_LoadPackage.LoadPackageParam lpparam, String currentVersion, String mode) {
        try {
            ClassLoader classLoader = lpparam.classLoader;

            switch (mode) {
                case "Normal":
                    handleNormalMode(lpparam, currentVersion);
                    break;
                case "Hecker":
                    handleHeckerMode(classLoader);
                    break;
                case "Auto":
                    handleAutoMode(lpparam, currentVersion);
                    break;
                default:
                    XposedBridge.log("(DevOptionsEnable) Unknown mode. Skipping hook.");
            }
        } catch (Exception e) {
            XposedBridge.log("(DevOptionsEnable) Error handling Dev Options: " + e.getMessage());
        }
    }

    private void handleNormalMode(XC_LoadPackage.LoadPackageParam lpparam, String currentVersion) {
        try {
            ClassLoader classLoader = lpparam.classLoader;

            // Step 1: Check local hooks first
            JSONObject hooksLog = readHooksLog();

            if (hooksLog.has(currentVersion)) {
                JSONObject versionLog = hooksLog.getJSONObject(currentVersion);
                if (versionLog.has("DevOption_enable")) {
                    // Apply hooks from local hooks log
                    JSONArray devOptionsEntries = versionLog.getJSONArray("DevOption_enable");
                    for (int i = 0; i < devOptionsEntries.length(); i++) {
                        JSONObject entryGroup = devOptionsEntries.getJSONObject(i);
                        Iterator<String> it = entryGroup.keys();
                        while (it.hasNext()) {
                            String key = it.next();
                            JSONObject entry = entryGroup.getJSONObject(key);
                            String classToHook = entry.getString("class_to_hook");
                            String methodToHook = "A00"; // Always A00 for DevOptions
                            Class<?> secondTargetClass = classLoader.loadClass("com.instagram.common.session.UserSession");
                            hookDevOptions(XposedHelpers.findClass(classToHook, classLoader), methodToHook, secondTargetClass);
                        }
                    }
                    XposedBridge.log("(DevOptionsEnable) Successfully applied hooks from local log.");
                    return; // Exit after applying hooks from local log
                }
            }

            // Step 2: Check GitHub hooks if no local hooks are found
            JSONArray githubHooks = getGitHubHooks();
            if (githubHooks != null) {
                for (int i = 0; i < githubHooks.length(); i++) {
                    JSONObject hookEntry = githubHooks.getJSONObject(i);
                    String version = hookEntry.getString("version");

                    if (version.contains(currentVersion)) { // Match Instagram version
                        String classToHook = hookEntry.getString("class_to_hook");
                        String methodToHook = hookEntry.getString("method_to_hook");
                        Class<?> secondTargetClass = classLoader.loadClass(hookEntry.getString("second_class_to_hook"));

                        hookDevOptions(XposedHelpers.findClass(classToHook, classLoader), methodToHook, secondTargetClass);

                        // Log the applied hook
                        XposedBridge.log("(DevOptionsEnable) Hook applied from GitHub for version: " + version);

                        return; // Exit after applying hooks from GitHub
                    }
                }
            }

            // No hooks found in either local log or GitHub
            XposedBridge.log("(DevOptionsEnable) No hooks found for Normal mode in local log or GitHub.");
        } catch (Exception e) {
            XposedBridge.log("(DevOptionsEnable) Error in Normal mode: " + e.getMessage());
        }
    }

    private void handleHeckerMode(ClassLoader classLoader) {
        try {
            String classToHook = Utils.DEFAULT_CLASS_TO_HOOK;
            String methodToHook = Utils.DEFAULT_METHOD_TO_HOOK;
            Class<?> secondTargetClass = classLoader.loadClass(Utils.DEFAULT_SECOND_CLASS_TO_HOOK);

            XposedBridge.log("(DevOptionsEnable) Hecker mode: Hooking class " + classToHook);
            hookDevOptions(XposedHelpers.findClass(classToHook, classLoader), methodToHook, secondTargetClass);
        } catch (Exception e) {
            XposedBridge.log("(DevOptionsEnable) Error in Hecker mode: " + e.getMessage());
        }
    }

    private void handleAutoMode(XC_LoadPackage.LoadPackageParam lpparam, String currentVersion) {
        String characters = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Map<String, JSONObject> foundHooks = new HashMap<>();
        int count = 1;

        try {
            // Step 1: Check if hooks already exist in the local log
            JSONObject hooksLog = readHooksLog();
            if (hooksLog.has(currentVersion)) {
                JSONObject versionLog = hooksLog.getJSONObject(currentVersion);
                if (versionLog.has("DevOption_enable")) {
                    // Apply existing hooks from local log
                    JSONArray devOptionsEntries = versionLog.getJSONArray("DevOption_enable");
                    for (int i = 0; i < devOptionsEntries.length(); i++) {
                        JSONObject entryGroup = devOptionsEntries.getJSONObject(i);
                        Iterator<String> it = entryGroup.keys();
                        while (it.hasNext()) {
                            String key = it.next();
                            JSONObject entry = entryGroup.getJSONObject(key);
                            String classToHook = entry.getString("class_to_hook");
                            String methodToHook = "A00"; // Always A00 for DevOptions
                            Class<?> secondTargetClass = lpparam.classLoader.loadClass("com.instagram.common.session.UserSession");
                            hookDevOptions(XposedHelpers.findClass(classToHook, lpparam.classLoader), methodToHook, secondTargetClass);
                        }
                    }
                    XposedBridge.log("(DevOptionsEnable) Applied existing hooks from local log in Auto mode.");
                    return; // Exit early since hooks are already available
                }
            }

            // Step 2: Perform dynamic search if no hooks exist in the local log
            XposedBridge.log("(DevOptionsEnable) No existing hooks found in local log. Starting dynamic search...");
            for (char first : characters.toCharArray()) {
                for (char second : characters.toCharArray()) {
                    for (char third : characters.toCharArray()) {
                        String classToHook = "X." + first + second + third;

                        try {
                            Class<?> cls = XposedHelpers.findClass(classToHook, lpparam.classLoader);

                            for (Method method : cls.getDeclaredMethods()) {
                                try {
                                    if (method.getName().equals("A00") && method.getReturnType() == Boolean.TYPE &&
                                            method.getParameterCount() == 1) {
                                        Class<?> secondTargetClass = lpparam.classLoader.loadClass("com.instagram.common.session.UserSession");
                                        hookDevOptions(cls, "A00", secondTargetClass);

                                        JSONObject entry = new JSONObject();
                                        entry.put("class_to_hook", classToHook);

                                        foundHooks.put("Entry" + count++, entry);
                                    }
                                } catch (NoClassDefFoundError | XposedHelpers.ClassNotFoundError e) {
                                    //XposedBridge.log("(DevOptionsEnable) Skipping method due to missing dependency: " + e.getMessage());
                                } catch (Exception e) {
                                    //XposedBridge.log("(DevOptionsEnable) General exception while inspecting method: " + e.getMessage());
                                }
                            }
                        } catch (NoClassDefFoundError | XposedHelpers.ClassNotFoundError e) {
                            //XposedBridge.log("(DevOptionsEnable) Skipping class " + classToHook + " due to missing dependency: " + e.getMessage());
                        } catch (Exception e) {
                            //XposedBridge.log("(DevOptionsEnable) General exception while inspecting class: " + e.getMessage());
                        }
                    }
                }
            }

            // Step 3: Save dynamically found hooks to local log
            if (!foundHooks.isEmpty()) {
                saveHooksToLog(currentVersion, "DevOption_enable", foundHooks);
                XposedBridge.log("(DevOptionsEnable) Saved dynamically found hooks to local log.");
            } else {
                XposedBridge.log("(DevOptionsEnable) No suitable classes found during dynamic search.");
            }
        } catch (Exception e) {
            XposedBridge.log("(DevOptionsEnable) Error in Auto mode: " + e.getMessage());
        }
    }



    private JSONArray getGitHubHooks() {
        try {
            String jsonContent = getJSONContent(); // Fetch JSON from GitHub
            JSONObject jsonObject = new JSONObject(jsonContent);
            return jsonObject.getJSONArray("ig_versions"); // Return the hooks array
        } catch (Exception e) {
            XposedBridge.log("(DevOptionsEnable) Failed to fetch GitHub hooks: " + e.getMessage());
            return null;
        }
    }

    private void hookDevOptions(Class<?> targetClass, String methodToHook, Class<?> secondTargetClass) {
        try {
            XposedHelpers.findAndHookMethod(
                    targetClass,
                    methodToHook,
                    secondTargetClass, // Second class parameter (UserSessionClass)
                    new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) {
                            XposedBridge.log("(DevOptionsEnable) Successfully Hooked into method: " + methodToHook + " in class: " + targetClass.getName());
                            return true; // Ensure the method always returns true
                        }
                    }
            );
            XposedBridge.log("(DevOptionsEnable) Successfully hooked method: " + methodToHook + " in class: " + targetClass.getName());
        } catch (NoSuchMethodError e) {
            XposedBridge.log("(DevOptionsEnable) No such method: " + methodToHook + " in class: " + targetClass.getName() + " - " + e.getMessage());
        } catch (NoClassDefFoundError e) {
            XposedBridge.log("(DevOptionsEnable) No such class definition found for: " + targetClass.getName() + " or parameter class: " + secondTargetClass.getName() + " - " + e.getMessage());
        } catch (XposedHelpers.ClassNotFoundError e) {
            XposedBridge.log("(DevOptionsEnable) XposedHelpers couldn't find class: " + targetClass.getName() + " or parameter class: " + secondTargetClass.getName() + " - " + e.getMessage());
        } catch (Exception e) {
            XposedBridge.log("(DevOptionsEnable) General exception while hooking method: " + methodToHook + " in class: " + targetClass.getName() + " - " + e.getMessage());
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



    private void saveHooksToLog(String version, String feature, Map<String, JSONObject> hooks) {
        try {
            // Read existing log or create a new one
            JSONObject hooksLog = readHooksLog();

            // Add new hooks for the version and feature
            if (!hooksLog.has(version)) {
                hooksLog.put(version, new JSONObject());
            }
            JSONObject versionLog = hooksLog.getJSONObject(version);

            JSONArray featureLog = new JSONArray();
            for (Map.Entry<String, JSONObject> entry : hooks.entrySet()) {
                JSONObject entryGroup = new JSONObject();
                entryGroup.put(entry.getKey(), entry.getValue());
                featureLog.put(entryGroup);
            }
            versionLog.put(feature, featureLog);

            // Save the updated log to SharedPreferences
            writeHooksLog(hooksLog);

            XposedBridge.log("(DevOptionsEnable) Hooks log updated successfully.");

        } catch (Exception e) {
            XposedBridge.log("(DevOptionsEnable) Error saving hooks to log: " + e.getMessage());
        }
    }


    private void showToast(final String text) {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> Toast.makeText(android.app.AndroidAppHelper.currentApplication().getApplicationContext(), text, Toast.LENGTH_LONG).show());
    }
}
