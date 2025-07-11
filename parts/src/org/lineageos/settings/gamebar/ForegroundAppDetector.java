/*
 * Copyright (C) 2025 kenway214
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.settings.gamebar;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.List;

public class ForegroundAppDetector {

    private static final String TAG = "ForegroundAppDetector";
    private static String sLastKnownPackage = "Unknown";
    private static long sLastUpdateTime = 0;
    private static final long CACHE_TIMEOUT = 500; // Reduce cache timeout
    
    // Simple reflection caching
    private static boolean sReflectionSetupFailed = false;

    public static String getForegroundPackageName(Context context) {
        // Use cached result if still valid
        long currentTime = System.currentTimeMillis();
        if (currentTime - sLastUpdateTime < CACHE_TIMEOUT && !"Unknown".equals(sLastKnownPackage)) {
            return sLastKnownPackage;
        }

        String pkg = tryGetRunningTasks(context);
        if (pkg != null) {
            sLastKnownPackage = pkg;
            sLastUpdateTime = currentTime;
            return pkg;
        }
        
        if (!sReflectionSetupFailed) {
            pkg = tryReflectActivityTaskManager();
            if (pkg != null) {
                sLastKnownPackage = pkg;
                sLastUpdateTime = currentTime;
                return pkg;
            }
        }
        
        // Return cached value if available, otherwise "Unknown"
        return sLastKnownPackage;
    }

    private static String tryGetRunningTasks(Context context) {
        try {
            if (context.checkSelfPermission("android.permission.GET_TASKS")
                == PackageManager.PERMISSION_GRANTED) {

                ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
                if (tasks != null && !tasks.isEmpty()) {
                    ActivityManager.RunningTaskInfo top = tasks.get(0);
                    if (top.topActivity != null) {
                        return top.topActivity.getPackageName();
                    }
                }
            } else {
                Log.w(TAG, "GET_TASKS permission not granted to this system app?");
            }
        } catch (Exception e) {
            Log.e(TAG, "tryGetRunningTasks error: ", e);
        }
        return null;
    }

    private static String tryReflectActivityTaskManager() {
        try {
            if (sReflectionSetupFailed) {
                return null;
            }
            
            Class<?> atmClass = Class.forName("android.app.ActivityTaskManager");
            Method getServiceMethod = atmClass.getDeclaredMethod("getService");
            getServiceMethod.setAccessible(true);
            Object atmService = getServiceMethod.invoke(null);
            Method getTasksMethod = atmService.getClass().getMethod("getTasks", int.class);
            
            @SuppressWarnings("unchecked")
            List<?> taskList = (List<?>) getTasksMethod.invoke(atmService, 1);
            if (taskList != null && !taskList.isEmpty()) {
                Object firstTask = taskList.get(0);
                Class<?> rtiClass = firstTask.getClass();
                Method getTopActivityMethod = rtiClass.getDeclaredMethod("getTopActivity");
                Object compName = getTopActivityMethod.invoke(firstTask);
                if (compName != null) {
                    Method getPackageNameMethod = compName.getClass().getMethod("getPackageName");
                    String pkgName = (String) getPackageNameMethod.invoke(compName);
                    return pkgName;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "tryReflectActivityTaskManager error: ", e);
            sReflectionSetupFailed = true; // Disable reflection on error
        }
        return null;
    }
}
