package org.pulpdust.lesserpad;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.Settings;

@TargetApi(30)
public class forR {
    public boolean isExternalStorageManager(){
        return Environment.isExternalStorageManager();
    }

    public void requestAllFilesAccess(Activity a, int req){
        Intent i = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
        i.setData(Uri.parse("package:" + a.getPackageName()));
        try {
            a.startActivityForResult(i, req);
        } catch (Exception e){
            Intent fallback = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
            a.startActivityForResult(fallback, req);
        }
    }
}
