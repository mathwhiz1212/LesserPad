package org.pulpdust.lesserpad;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class LesserPadPrefs extends PreferenceActivity {
	int look_style;
	@Override
	public void onCreate(Bundle savedInstanceState){
        readPrefs();
		if (Build.VERSION.SDK_INT >= 30 && look_style == -1) {
			if (getResources().getConfiguration().isNightModeActive()){
				look_style = 1;
			} else {
				look_style = 0;
			}
		}
       if (Build.VERSION.SDK_INT >= 11 && Build.VERSION.SDK_INT <= 13){
    	} else if (look_style > 0 || (Build.VERSION.SDK_INT <= 10 && Build.VERSION.SDK_INT >= 6)){ 
    		setTheme(R.style.AppTheme_Prefs_Dark);
    	} else {
    		setTheme(R.style.AppTheme_Prefs);
    	}
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.prefs);

		Preference safPicker = findPreference("saf_dir_picker");
		if (safPicker != null) {
			safPicker.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
					startActivityForResult(intent, 100);
					return true;
				}
			});
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 100 && resultCode == RESULT_OK) {
			Uri treeUri = data.getData();
			if (treeUri != null) {
				getContentResolver().takePersistableUriPermission(treeUri,
						Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
				SharedPreferences sprefs = PreferenceManager.getDefaultSharedPreferences(this);
				sprefs.edit().putString("saf_uri", treeUri.toString()).apply();
			}
		}
	}
	
    public void readPrefs(){
    	SharedPreferences sprefs = PreferenceManager.getDefaultSharedPreferences(this);
    	look_style = Integer.parseInt(sprefs.getString("look_style", "0"));
    }


}
