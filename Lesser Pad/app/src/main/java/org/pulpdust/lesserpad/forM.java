package org.pulpdust.lesserpad;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import androidx.core.content.PermissionChecker;

@TargetApi(23)
public class forM {
    public int selfCheckPermission(Context c, String p){
        return PermissionChecker.checkSelfPermission(c, p);
    }

    public void selfRequestPermissions(Activity a, String[] p, int r){
        a.requestPermissions(p, r);
    }
}
