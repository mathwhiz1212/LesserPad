package org.pulpdust.lesserpad;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.Manifest;
import android.graphics.Color;
import android.net.Uri;
import android.provider.DocumentsContract;
import androidx.documentfile.provider.DocumentFile;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.core.content.PermissionChecker;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class LesserPadListActivity extends FragmentActivity {
	final static String TAG = "Lesser Pad List";
	static File path;
	String default_dir;
	float font_size;
	int look_style;
	static boolean hide_ext;
	boolean spec_path;
	String path_to;
	String last_dir;
	final static int SORT_OLD = 0;
	final static int SORT_NEW = 1;
	final static int SORT_ABC = 2;
	final static int SORT_ZYX = 3;
	static int sort_by = SORT_NEW;
	final static int REQ_PASS = 4;
	final int RET_SORT = 5;
	String saf_uri_string;
	Uri saf_uri;
	Uri current_saf_uri;
	Spinner ebox;
	TextView label;
	ListView mlist;
	Button newbtn;
	List<String> dirs = new ArrayList<String>();
	ArrayAdapter<String> adirs;
	ArrayAdapter<String> amemos;
	List<String> memos = new ArrayList<String>();
	List<String> ls = new ArrayList<String>();
	libLesserPad llp = new libLesserPad();
	boolean normal_stop = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        readPrefs();
		if (look_style == -1) {
			if (getResources().getConfiguration().isNightModeActive()){
				look_style = 1;
			} else {
				look_style = 0;
			}
		}
    	if (look_style > 0){ 
    		setTheme(R.style.AppTheme_Dark);
    	} else {
    		setTheme(R.style.AppTheme);
			look_style = 0;
    	}
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lesser_pad_list);

		if (saf_uri_string == null || saf_uri_string.isEmpty()){
			Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
			startActivityForResult(intent, 100);
			return;
		}

        ebox = (Spinner) findViewById(R.id.spinner2);
        label = (TextView) findViewById(R.id.textView2);
        mlist = (ListView) findViewById(R.id.listView1);
        newbtn = (Button) findViewById(R.id.button1);
        dirs = new ArrayList<String>();
        adirs = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, dirs);
        adirs.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ebox.setAdapter(adirs);
        if (newbtn != null) {
            newbtn.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v){
                    doNew();
                }
            });
            if (Build.VERSION.SDK_INT >= 11 && Build.VERSION.SDK_INT < 21){
                newbtn.setVisibility(View.GONE);
            }
        }
		if (Build.VERSION.SDK_INT >= 21){
			forLollipop.readyActionBar(findViewById(R.id.toolBar2), this, 0, look_style, false);
		}
		if (look_style > 0 && Build.VERSION.SDK_INT >= 21){
			label.setTextColor(Color.rgb(192, 192, 192));
			mlist.setBackgroundColor(Color.rgb(25, 25, 25));
		} else if (look_style == 0 && Build.VERSION.SDK_INT >= 21){
			label.setTextColor(Color.rgb(50,50,50));
			mlist.setBackgroundColor(Color.rgb(250,250,250));
		}

        amemos = new ArrayAdapter<String>(this, R.layout.list_item, memos){
        	@Override
        	public View getView(int pos, View v, ViewGroup vg){
        		TextView tview = (TextView) super.getView(pos, v, vg);
        		tview.setTextSize(font_size);
				if (Build.VERSION.SDK_INT >= 21){
					tview.setBackgroundColor(getResources().getColor(android.R.color.transparent));
				}
        		return tview;
        	}
        };
        mlist.setAdapter(amemos);
        mlist.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> av, View v, int pos,
					long id) {
				if (ls.get(pos).endsWith(".len")){
					Intent pit = new Intent();
					pit.setClassName("org.pulpdust.lesserpad", "org.pulpdust.lesserpad.ProtectActivity");
					pit.putExtra(libLesserPad.CRYPT_FILE, (ls.get(pos)));
					pit.putExtra(libLesserPad.REQ_PASS_MODE, libLesserPad.REQ_PASS_FOR_OPEN);
					startActivityForResult(pit, REQ_PASS);
				} else {
					Intent intent = new Intent(libLesserPad.LPAD_EDIT);
					intent.setComponent(new ComponentName("org.pulpdust.lesserpad", "org.pulpdust.lesserpad.LesserPadActivity"));
	        		intent.putExtra("PATH", current_saf_uri.toString());
					try {
						DocumentFile root = null;
						if (current_saf_uri.equals(saf_uri)) {
							root = DocumentFile.fromTreeUri(LesserPadListActivity.this, saf_uri);
						} else {
							// For subfolders, fromTreeUri might be risky if not a tree URI
							if (DocumentsContract.isTreeUri(current_saf_uri)) {
								root = DocumentFile.fromTreeUri(LesserPadListActivity.this, current_saf_uri);
							} else {
								// Try to use it as a tree-aware document URI
								root = DocumentFile.fromTreeUri(LesserPadListActivity.this, current_saf_uri);
							}
						}
						if (root != null) {
							DocumentFile file = root.findFile(ls.get(pos));
							if (file != null) {
								intent.setData(file.getUri());
								intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
							}
						}
					} catch (Exception e) {
						Log.e(TAG, "Error in onItemClick", e);
					}
					startActivityForResult(intent, 1);
				}
			}
        });
        mlist.setOnItemLongClickListener(new OnItemLongClickListener(){
			@Override
			public boolean onItemLongClick(AdapterView<?> av, View v,
					int pos, long id) {
				Intent intent = new Intent();
				intent.setAction("android.intent.action.VIEW");
				try {
					DocumentFile root = null;
					if (current_saf_uri.equals(saf_uri)) {
						root = DocumentFile.fromTreeUri(LesserPadListActivity.this, saf_uri);
					} else {
						root = DocumentFile.fromTreeUri(LesserPadListActivity.this, current_saf_uri);
					}
					if (root != null) {
						DocumentFile file = root.findFile(ls.get(pos));
						if (file != null) {
							intent.setDataAndType(file.getUri(), "text/plain");
						}
					}
					startActivityForResult(intent, 3);
				} catch (Exception e){
					Log.e(TAG, e.getClass().getSimpleName());
				}
				return true;
			}
        });
        
        path = null;
		if (current_saf_uri == null) {
			String lastSub = getPreferences(MODE_PRIVATE).getString("last_sub_uri", null);
			if (lastSub != null) {
				try {
					current_saf_uri = Uri.parse(lastSub);
				} catch (Exception e) {
					Log.e(TAG, "Invalid last_sub_uri: " + lastSub, e);
					current_saf_uri = saf_uri;
				}
			} else if (saf_uri != null) {
				current_saf_uri = saf_uri;
			}
		}
        
        listMemos(null);
		listDirs();
        
        if (default_dir.equals("/")){
        	ebox.setEnabled(false);
        }
		ebox.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> av, View v, int pos,
									   long id) {
				doChange(pos);
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
    }

	public void doChange(int pos){
		if (saf_uri != null) {
			if (pos == 0) {
				current_saf_uri = saf_uri;
			} else {
				try {
					DocumentFile root = DocumentFile.fromTreeUri(this, saf_uri);
					if (root != null) {
						String folderName = dirs.get(pos);
						DocumentFile sub = root.findFile(folderName);
						if (sub != null && sub.isDirectory()) {
							current_saf_uri = sub.getUri();
						}
					}
				} catch (Exception e) {
					Log.e(TAG, "Error in doChange", e);
				}
			}
			getPreferences(MODE_PRIVATE).edit().putString("last_sub_uri", current_saf_uri.toString()).apply();
			listMemos(null);
		}
	}

	public void listDirs() {
		String currentName = null;
		if (saf_uri != null) {
			if (current_saf_uri != null && !current_saf_uri.equals(saf_uri)) {
				try {
					// Use fromSingleUri to get the name safely without needing tree permissions for this specific URI
					DocumentFile df = DocumentFile.fromSingleUri(this, current_saf_uri);
					if (df != null) currentName = df.getName();
				} catch (Exception e) {
					Log.e(TAG, "Error in listDirs", e);
				}
			}
			llp.listDir(null, adirs, dirs, ebox, this, currentName);
		}
	}

    public void listMemos(File dummy){
		amemos.setNotifyOnChange(false);
    	amemos.clear();
		ls.clear();
		if (current_saf_uri != null) {
			List<DocumentFile> files = DocumentHelper.listFiles(this, current_saf_uri);
			for (DocumentFile file : files) {
				String fname = file.getName();
				if (fname != null && (fname.toLowerCase().endsWith(".txt") || fname.toLowerCase().endsWith(".len"))) {
					ls.add(fname);
					String displayName = fname;
					if (hide_ext) {
						int lastDot = fname.lastIndexOf('.');
						if (lastDot > 0) {
							displayName = fname.substring(0, lastDot);
						}
					}
					amemos.add(displayName);
				}
			}
		}
		amemos.notifyDataSetChanged();
    }

	public void doNew(){
		if (current_saf_uri == null) {
			if (saf_uri != null) {
				current_saf_uri = saf_uri;
			} else {
				// No storage configured
				return;
			}
		}
		Intent intent = new Intent(libLesserPad.LPAD_NEW);
		intent.setComponent(new ComponentName("org.pulpdust.lesserpad", "org.pulpdust.lesserpad.LesserPadActivity"));
		intent.putExtra("PATH", current_saf_uri.toString());
		intent.setData(current_saf_uri);
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
		startActivityForResult(intent, 0);
	}

    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
    	super.onPrepareOptionsMenu(menu);
    	if (default_dir.equals("/")){
    		MenuItem fld = (MenuItem) menu.findItem(R.id.menu_folder);
    		fld.setVisible(false);
    	}
    	return true;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	if (look_style > 0){
    		getMenuInflater().inflate(R.menu.activity_lesser_pad_list_dark, menu);
    	} else {
    		getMenuInflater().inflate(R.menu.activity_lesser_pad_list, menu);
    	}
    	return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem mi){
    	switch (mi.getItemId()){
		case android.R.id.home:
			doNew();
			return true;
    	case R.id.menu_folder:
    		Intent editfolder = new Intent();
    		editfolder.setClassName("org.pulpdust.lesserpad", "org.pulpdust.lesserpad.CategoryEditor");
    		startActivityForResult(editfolder, 2);
    		return true;
    	case R.id.menu_settings:
    		Intent goprefs = new Intent();
    		goprefs.setClassName("org.pulpdust.lesserpad", "org.pulpdust.lesserpad.LesserPadPrefs");
    		startActivityForResult(goprefs, 0);
    		return true;
    	case R.id.menu_new:
    		doNew();
    		return true;
    	case R.id.menu_sort:
    		DialogFragment sd = SortDialog.newInstance();
    		sd.show(getSupportFragmentManager(), "sort");
    		return true;
    	default:
    		return super.onOptionsItemSelected(mi);
    	}
    }

    public static class SortDialog extends DialogFragment {
    	public static SortDialog newInstance(){
    		return new SortDialog();
    	}
    	@Override
    	public Dialog onCreateDialog(Bundle savedInstanceState){
    		final CharSequence[] items = getResources().getStringArray(R.array.sort_labels);
    		return new AlertDialog.Builder(getActivity())
    			.setSingleChoiceItems(items, sort_by, new DialogInterface.OnClickListener(){
    				public void onClick(DialogInterface dialog, int item){
    					sort_by = item;
						LesserPadListActivity ma = (LesserPadListActivity) getActivity();
    					ma.listMemos(null);
    					dialog.dismiss();
    				}
    			})
    			.setTitle(R.string.menu_sort)
    			.create();
    	}
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
    	switch (requestCode){
    	case 0:
    		readPrefs();
			listMemos(null);
			listDirs();
    		break;
    	case 1:
    		if (resultCode == RESULT_OK && data != null){
				String new_parent = data.getStringExtra("PATH");
				if (new_parent != null) {
					current_saf_uri = Uri.parse(new_parent);
				}
    		}
    		readPrefs();
			listMemos(null);
			listDirs();
    		break;
    	case 2:
			listMemos(null);
			listDirs();
            break;
    	case 3:
    		listMemos(null);
    		break;
    	case REQ_PASS:
    		if (resultCode == RESULT_OK){
				Intent intent = new Intent(libLesserPad.LPAD_EDIT);
				intent.setComponent(new ComponentName("org.pulpdust.lesserpad", "org.pulpdust.lesserpad.LesserPadActivity"));
        		intent.putExtra("PATH", current_saf_uri.toString());
        		intent.putExtra(libLesserPad.CRYPT_PASS, data.getStringExtra(libLesserPad.CRYPT_PASS));
				try {
					DocumentFile root = DocumentFile.fromTreeUri(this, current_saf_uri);
					if (root != null) {
						DocumentFile file = root.findFile(data.getStringExtra(libLesserPad.CRYPT_FILE));
						if (file != null) {
							intent.setData(file.getUri());
							intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
						}
					}
				} catch (Exception e) {
					Log.e(TAG, "Error in REQ_PASS", e);
				}
				startActivityForResult(intent, 1);
    		}
		case RET_SORT:
			if (resultCode == RESULT_OK){
				listMemos(null);
				break;
			}
			break;
		case 100:
			if (resultCode == RESULT_OK && data != null) {
				Uri treeUri = data.getData();
				if (treeUri != null) {
					getContentResolver().takePersistableUriPermission(treeUri,
							Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
					SharedPreferences sprefs = PreferenceManager.getDefaultSharedPreferences(this);
					sprefs.edit().putString("saf_uri", treeUri.toString()).apply();
					llp.restartActivity(getIntent(), this);
				}
			} else {
				Toast.makeText(getApplicationContext(), R.string.mes_nosd, Toast.LENGTH_LONG).show();
				normal_stop = false;
				finish();
			}
			break;
    	}
    }

    public void readPrefs(){
    	SharedPreferences sprefs = PreferenceManager.getDefaultSharedPreferences(this);
		saf_uri_string = sprefs.getString("saf_uri", null);
		if (saf_uri_string != null) {
			try {
				Uri uri = Uri.parse(saf_uri_string);
				if (Build.VERSION.SDK_INT >= 24) {
					if (DocumentsContract.isTreeUri(uri)) {
						saf_uri = uri;
					} else {
						Log.e(TAG, "saf_uri in prefs is NOT a tree URI: " + saf_uri_string);
						saf_uri = null;
					}
				} else {
					saf_uri = uri;
				}
			} catch (Exception e) {
				Log.e(TAG, "Error parsing saf_uri", e);
				saf_uri = null;
			}
		} else {
			saf_uri = null;
		}
    	default_dir = sprefs.getString("default_dir", getString(R.string.app_default_dir));
    	if (default_dir.equals("")) {
    		default_dir = "/";
    	}
    	font_size = Float.parseFloat(sprefs.getString("font_size", "18.0f"));
    	look_style = Integer.parseInt(sprefs.getString("look_style", "0"));
    	hide_ext = sprefs.getBoolean("hide_ext", false);
    	spec_path = sprefs.getBoolean("spec_path", false);
    	path_to = sprefs.getString("path_to", Environment.getExternalStorageDirectory().toString());
    	if (path_to.equals("")){
    		path_to = Environment.getExternalStorageDirectory().toString();
    	}
    	SharedPreferences props = getPreferences(MODE_PRIVATE);
    	sort_by = props.getInt("sort_by", 1);
    	last_dir = props.getString("last_dir", null);
    }

    public void writeProps(){
    	SharedPreferences props = getPreferences(MODE_PRIVATE);
    	SharedPreferences.Editor pedit = props.edit();
    	pedit.putInt("sort_by", sort_by);
		if (current_saf_uri != null) {
			pedit.putString("last_sub_uri", current_saf_uri.toString());
		}
    	pedit.commit();
    }

    @Override
    public void onStop(){
    	super.onStop();
    	if (normal_stop){
    		writeProps();
    	}
    }
}
