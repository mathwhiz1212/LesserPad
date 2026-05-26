package org.pulpdust.lesserpad;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;


import android.Manifest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Rect;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toolbar;
import androidx.documentfile.provider.DocumentFile;
import android.widget.Toast;

public class LesserPadActivity extends FragmentActivity implements TextWatcher {
	final static String TAG = "Lesser Pad Main";
	final static int FILE_NEW = 0;
	final static int FILE_OPEN = 1;
	final static int REQ_PASS_FOR_ENC = 2;
	final static int REQ_PASS_FOR_DEC = 3;

	int fmode = FILE_NEW;
	int lmode = 0;
	boolean dontsave = true;
	String name = "";
	float font_size;
	static int look_style;
	static boolean hide_ext;
	boolean abarnotsplit;
	Uri saf_uri;
	Uri current_saf_uri;
	EditText etxt;
	Spinner ebox;
	TextView label;
	List<String> dirs = new ArrayList<String>();
	ArrayAdapter<String> adirs;
	static String disuse = "[\"|:;,'*?<>/\\\\^]";
	String former;
	String action;
	Uri current_uri;
	libLesserPad llp = new libLesserPad();
	boolean priv = false;
	String pass = null;

	public static class EditMemo extends EditText {
		private Rect rt;
		private Paint pt;
		
		public EditMemo(Context cn, AttributeSet as){
			super(cn, as);
			rt = new Rect();
			pt = new Paint();
			pt.setStyle(Paint.Style.STROKE);
			if (look_style > 0){
				pt.setColor(Color.rgb(68,68,68));
			} else {
				pt.setColor(Color.rgb(188,188,188));
			}
			pt.setPathEffect(new DashPathEffect(new float[]{ 2.0f, 2.0f }, 0));
		}
		@Override
		protected void onDraw(Canvas cv){
			int mhp = getMeasuredHeight() - getExtendedPaddingTop();
			int lh = getLineHeight();
			int dlc = mhp / lh;
			int lc = getLineCount();
			int count = Math.max(dlc, lc);
			Rect r = rt;
			Paint p = pt;
			int bl = getLineBounds(0, r);
			for (int i = 0; i < count; i++){
				cv.drawLine(r.left, bl + 1, r.right, bl + 1, p);
				bl = bl + lh;
			}
			super.onDraw(cv);
		}
	}

    private boolean isProgrammaticSelection = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
		Intent intent = getIntent();
        action = intent.getAction();
        readPrefs();
		if (look_style == -1) {
			if (Build.VERSION.SDK_INT >= 30 && getResources().getConfiguration().isNightModeActive()){
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
        
        if (libLesserPad.LPAD_NEW.equals(action) || Intent.ACTION_MAIN.equals(action)){
        	this.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        } else {
        	this.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_UNCHANGED);
        }
        setContentView(R.layout.activity_lesser_pad);

		if (saf_uri != null) {
			String pathExtra = intent.getStringExtra("PATH");
			if (pathExtra != null) {
				try {
					current_saf_uri = Uri.parse(pathExtra);
					// Allow both tree URIs and tree-aware document URIs
					boolean isTree = false;
					if (Build.VERSION.SDK_INT >= 24) {
						isTree = DocumentsContract.isTreeUri(current_saf_uri);
					}
					boolean isTreeAwareDoc = current_saf_uri.getPath() != null && current_saf_uri.getPath().contains("/tree/") && current_saf_uri.getPath().contains("/document/");
					
					if (Build.VERSION.SDK_INT >= 24 && !isTree && !isTreeAwareDoc) {
						Log.w(TAG, "PATH extra is not a valid SAF URI, falling back: " + pathExtra);
						current_saf_uri = saf_uri;
					}
				} catch (Exception e) {
					Log.e(TAG, "Invalid PATH extra: " + pathExtra, e);
					current_saf_uri = saf_uri;
				}
			} else {
				current_saf_uri = intent.getData();
			}
			if (current_saf_uri == null) {
				current_saf_uri = saf_uri;
			}
		} else {
			// This shouldn't happen on Android 15
			Log.e(TAG, "No saf_uri configured!");
		}

        etxt = (EditText) findViewById(R.id.editText1);
    	etxt.setTextSize(font_size);
        etxt.addTextChangedListener(this);

        ebox = (Spinner) findViewById(R.id.spinner1);
        label = (TextView) findViewById(R.id.textView1);
        dirs = new ArrayList<String>();
        adirs = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, dirs);
        adirs.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ebox.setAdapter(adirs);

        Uri curi = intent.getData();
        if (intent.getStringExtra(libLesserPad.CRYPT_PASS) != null){
        	pass = intent.getStringExtra(libLesserPad.CRYPT_PASS);
        }
        if (savedInstanceState != null){
        	name = savedInstanceState.getString("name");
			fmode = savedInstanceState.getInt("fmode");
			if (savedInstanceState.getString("current_saf_uri") != null) {
				current_saf_uri = Uri.parse(savedInstanceState.getString("current_saf_uri"));
			}
			if (savedInstanceState.getString("current_uri") != null) {
				current_uri = Uri.parse(savedInstanceState.getString("current_uri"));
			}
        	if (savedInstanceState.getString("pass") != null) pass = savedInstanceState.getString("pass");
        	if (savedInstanceState.getString("former") != null) former = savedInstanceState.getString("former");
        	priv = savedInstanceState.getBoolean("priv");
        }
        
        if (Intent.ACTION_EDIT.equals(action) || 
        		Intent.ACTION_VIEW.equals(action) || 
        		libLesserPad.LPAD_EDIT.equals(action)){
        	if (savedInstanceState != null && current_uri != null){
        		// Already loaded from savedInstanceState
        	} else if (curi != null) {
                if (DocumentHelper.isDirectory(this, curi)) {
                    // It's a directory, treat as NEW in this directory
                    current_saf_uri = curi;
                    fmode = FILE_NEW;
                    name = "";
                } else if (fileOpen(curi)) {
                    // Opened successfully
                } else {
                    Log.e(TAG, "fileOpen failed. action=" + action + " curi=" + curi);
                    Toast.makeText(this, R.string.mes_not_open, Toast.LENGTH_SHORT).show();
                }
            } else {
        		Log.e(TAG, "No URI provided for edit/view action");
        		Toast.makeText(this, R.string.mes_not_open, Toast.LENGTH_SHORT).show();
        	}
        	if (libLesserPad.LPAD_EDIT.equals(action)){
        		lmode = 1;
        	} else {
        		lmode = 0;
        	}
        } else if (libLesserPad.LPAD_NEW.equals(action) || Intent.ACTION_SEND.equals(action) || Intent.ACTION_MAIN.equals(action)){
        	if (savedInstanceState != null){
				// Keep current state
			} else if (curi != null) {
                if (DocumentHelper.isDirectory(this, curi)) {
                    // Correctly identified as directory for new file
                    current_saf_uri = curi;
                    fmode = FILE_NEW;
                    name = "";
                } else if (fileOpen(curi)) {
                    // It's a file, open it (e.g. shortcut to specific file)
                } else {
                    fmode = FILE_NEW;
                    name = "";
                }
            } else {
        		fmode = FILE_NEW;
        		name = "";
        		if (Intent.ACTION_SEND.equals(action)) setSharedText(intent.getExtras());
        	}
			lmode = 1;
        }

		if (Build.VERSION.SDK_INT >= 21){
			forLollipop.readyActionBar(findViewById(R.id.toolBar1), this, 1, look_style, abarnotsplit);
		}
        
        if (fmode == FILE_NEW){
        	label.setText(R.string.label_new);
        } else if (fmode == FILE_OPEN && name != null && !name.equals("")){
        	if (hide_ext == true){
        		label.setText(name.replaceFirst("(\\.txt$)|(\\.len$)", ""));
        	} else {
        		label.setText(name);
        	}
        }

		if (saf_uri != null) {
			ebox.setEnabled(true);
			ebox.setVisibility(View.VISIBLE);
			label.setVisibility(View.VISIBLE);
			listDirs();
		}
        ebox.setOnItemSelectedListener(new Mover());
        
    	if (look_style > 0){
    		etxt.setTextColor(Color.rgb(192,192,192));
    		etxt.setHighlightColor(Color.rgb(0,0,255));
    		etxt.setBackgroundColor(Color.rgb(25,25,25));
			if (Build.VERSION.SDK_INT >= 21) label.setTextColor(Color.rgb(190,190,190));
    	} else if (look_style == 0){
    		etxt.setTextColor(Color.rgb(50,50,50));
    		etxt.setHighlightColor(Color.rgb(255,255,0));
    		etxt.setBackgroundColor(Color.rgb(250,250,250));
			if (Build.VERSION.SDK_INT >= 21) label.setTextColor(Color.rgb(50,50,50));
    	}
    }

    @Override
    public void onPause(){
    	super.onPause();
    	if (fmode == FILE_OPEN && former != null && former.equals(etxt.getText().toString())){
    		dontsave = true;
    	}
        if (fmode == FILE_NEW && etxt.getText().toString().length() == 0){
        	dontsave = true;
        }
    	if (!dontsave){
    		if (priv){
    			doSave(toEncrypt(pass, etxt.getText().toString()));
    		} else {
    			doSave(etxt.getText().toString());
    		}
    		dontsave = true;
    	}
    }

    @Override
    public void onSaveInstanceState(Bundle sis){
		if (current_saf_uri != null) sis.putString("current_saf_uri", current_saf_uri.toString());
		if (current_uri != null) sis.putString("current_uri", current_uri.toString());
    	sis.putString("name", name);
		sis.putInt("fmode", fmode);
    	if (pass != null) sis.putString("pass", pass);
    	if (former != null) sis.putString("former", former);
    	sis.putBoolean("priv", priv);
    	super.onSaveInstanceState(sis);
    }
    
    public class Mover implements OnItemSelectedListener {
		@Override
		public void onItemSelected(AdapterView<?> av, View v, int pos,
				long id) {
			if (!isProgrammaticSelection) {
				doMove(getApplicationContext(), pos);
			}
		}
		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
		}
    }

    void setSharedText(Bundle extras){
		if (extras != null){
			CharSequence text = extras.getCharSequence(Intent.EXTRA_TEXT);
			if (text != null){
				etxt.setText(text);
			}
		}
	}

    public void doMove(Context context, int pos){
		if (saf_uri != null) {
			Uri target_saf_uri;
			if (pos == 0) {
				target_saf_uri = saf_uri;
			} else {
				try {
					DocumentFile root = DocumentFile.fromTreeUri(this, saf_uri);
					if (root != null) {
						DocumentFile sub = root.findFile(dirs.get(pos));
						if (sub != null) {
							target_saf_uri = sub.getUri();
						} else {
							return;
						}
					} else {
						return;
					}
				} catch (Exception e) {
					Log.e(TAG, "Error in doMove", e);
					return;
				}
			}

			// Normalize for comparison
			Uri currentDocUri = current_saf_uri;
			if (currentDocUri != null && !DocumentsContract.isDocumentUri(this, currentDocUri)) {
				currentDocUri = DocumentsContract.buildDocumentUriUsingTree(currentDocUri, DocumentsContract.getTreeDocumentId(currentDocUri));
			}
			Uri targetDocUri = target_saf_uri;
			if (targetDocUri != null && !DocumentsContract.isDocumentUri(this, targetDocUri)) {
				targetDocUri = DocumentsContract.buildDocumentUriUsingTree(targetDocUri, DocumentsContract.getTreeDocumentId(targetDocUri));
			}

			if (targetDocUri != null && targetDocUri.equals(currentDocUri)) {
				return;
			}

			if (fmode == FILE_OPEN && current_uri != null) {
				Uri result = DocumentHelper.moveFile(this, current_uri, current_saf_uri, target_saf_uri);
				if (result != null) {
					current_uri = result;
					current_saf_uri = target_saf_uri;
					Toast.makeText(getApplicationContext(), R.string.mes_save, Toast.LENGTH_SHORT).show();
				} else {
					Log.e(TAG, "moveFile failed: current_uri=" + current_uri + ", current_saf_uri=" + current_saf_uri + ", target_saf_uri=" + target_saf_uri);
					Toast.makeText(getApplicationContext(), R.string.mes_move_fail, Toast.LENGTH_SHORT).show();
					// Reset spinner to current location since move failed
					listDirs();
					return;
				}
			} else {
				current_saf_uri = target_saf_uri;
			}
			listDirs();
		}
    }

    public boolean fileOpen(Uri uri){
		current_uri = uri;
		Log.d(TAG, "fileOpen: Attempting to open " + uri);
    	try {
			// Grant persistable permission if possible
			try {
				getContentResolver().takePersistableUriPermission(uri,
						Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
			} catch (SecurityException se) {
				Log.w(TAG, "fileOpen: Could not take persistable permission for " + uri + " - " + se.getMessage());
			}

			String content = DocumentHelper.readFile(this, uri);
			DocumentFile df = DocumentFile.fromSingleUri(this, uri);
			if (df != null) {
				name = df.getName();
				label.setText(name);
			}
			etxt.removeTextChangedListener(this);
			etxt.setText(content);
			etxt.addTextChangedListener(this);
			former = content;
			fmode = FILE_OPEN;
			listDirs();
			return true;
		} catch (Exception e) {
			Log.e(TAG, "fileOpen: Failed for " + uri, e);
			Toast.makeText(getApplicationContext(), R.string.mes_not_open, Toast.LENGTH_LONG).show();
			return false;
		}
    }

	void fileOpened(Uri curi){
		fmode = FILE_OPEN;
		current_uri = curi;
		DocumentFile df = DocumentFile.fromSingleUri(this, curi);
		if (df != null) {
			name = df.getName();
		}
    }

    public void doSave(String text){
	    if (textFiling(text)){
	    	Toast.makeText(getApplicationContext(), R.string.mes_save, Toast.LENGTH_SHORT).show();
	    	fmode = FILE_OPEN;
	    	if (hide_ext){
	    		label.setText(name.replaceFirst("(\\.txt$)|(\\.len$)", ""));
	    	} else {
	    		label.setText(name);
	    	}
	    } else {
	    	Log.d(TAG, "textFiling failed.");
	    	Toast.makeText(getApplicationContext(), R.string.mes_not_save, Toast.LENGTH_SHORT).show();
	    }
    }

    public boolean textFiling(String text){
		if (saf_uri != null) {
			if (name == null || name.equals("")) {
				name = enTitle(0, etxt.getText().toString().split("\n")[0]);
				if (!name.endsWith(".txt")) name += ".txt";
			}
			if (DocumentHelper.writeFile(this, current_saf_uri, name, text + "\n")) {
				former = etxt.getText().toString();
				fmode = FILE_OPEN;
				// After writing, we need to update current_uri to the actual file URI
				try {
					DocumentFile root = DocumentFile.fromTreeUri(this, current_saf_uri);
					if (root != null) {
						DocumentFile df = root.findFile(name);
						if (df != null) {
							current_uri = df.getUri();
							// Take persistable permission for the new/updated file
							try {
								getContentResolver().takePersistableUriPermission(current_uri,
										Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
							} catch (SecurityException se) {
								Log.w(TAG, "Could not take persistable permission for " + current_uri);
							}
						}
					}
				} catch (Exception e) {
					Log.e(TAG, "Error updating current_uri", e);
				}
				return true;
			}
			return false;
		}
		return false;
    }

    public String enTitle(int mode, String line){
    	String newname;
    	String ext;
    	if (priv == true){
    		ext = ".len";
    	} else {
    		ext = ".txt";
    	}
    	SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-HHmmss");
    	if (mode == 1){
    		newname = format.format(new Date()) + ext;
    	} else if (line == null){
    		newname = format.format(new Date()) + ext;
        } else if (line.equals("")){
        	newname = format.format(new Date()) + ext;
        } else {
        	newname = line.replaceAll(disuse, "").trim();
			if (newname.length() > 20) newname = newname.substring(0, 20);
			if (newname.isEmpty()) newname = format.format(new Date());
			newname += ext;
        }
    	return newname;
    }

	public static boolean doDelete(File path, String name){
		File object;
		if (name == null){
			object = new File(path.toString());
		} else {
			object = new File(path, name);
		}
		if (object.delete()){
			return true;
		} else {
			return false;
		}
	}

	public void listDirs() {
		if (saf_uri != null) {
			String currentName = null;
			if (current_saf_uri != null && !current_saf_uri.equals(saf_uri)) {
				try {
					DocumentFile df = DocumentFile.fromSingleUri(this, current_saf_uri);
					if (df != null) currentName = df.getName();
				} catch (Exception e) {
					Log.e(TAG, "Error getting current folder name", e);
				}
			}
			isProgrammaticSelection = true;
			llp.listDir(null, adirs, dirs, ebox, this, currentName);
			isProgrammaticSelection = false;
		}
	}

    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
    	super.onPrepareOptionsMenu(menu);
		MenuItem mdel = (MenuItem) menu.findItem(R.id.menu_delete);
		mdel.setEnabled(fmode != FILE_NEW);
    	return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	if (look_style > 0){
    		getMenuInflater().inflate(R.menu.activity_lesser_pad_dark, menu);
    	} else {
    		getMenuInflater().inflate(R.menu.activity_lesser_pad, menu);
    	}
    	return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem mi){
    	switch (mi.getItemId()){
		case android.R.id.home:
			if (!dontsave) {
				if (priv) {
					doSave(toEncrypt(pass, etxt.getText().toString()));
				} else {
					doSave(etxt.getText().toString());
				}
				dontsave = true;
			}
			finish();
			return true;
    	case R.id.menu_save:
    		dontsave = true;
    		if (priv){
    			doSave(toEncrypt(pass, etxt.getText().toString()));
    		} else {
    			doSave(etxt.getText().toString());
    		}
    		return true;
    	case R.id.menu_delete:
    		sureDelete(this).show();
    		return true;
        case R.id.menu_paste:
            etxt.requestFocus();
            etxt.onTextContextMenuItem(android.R.id.paste);
            return true;
        case R.id.menu_selectall:
            etxt.requestFocus();
            etxt.selectAll();
            return true;
        case R.id.menu_copy:
            etxt.requestFocus();
            etxt.onTextContextMenuItem(android.R.id.copy);
            return true;
        case R.id.menu_cut:
            etxt.requestFocus();
            etxt.onTextContextMenuItem(android.R.id.cut);
            return true;
        case R.id.menu_undo:
            // Very basic undo if 'former' was set
            if (former != null) {
                String current = etxt.getText().toString();
                etxt.setText(former);
                former = current;
                etxt.setSelection(etxt.getText().length());
            }
            return true;
        case R.id.menu_search:
            llp.goSearch(this, etxt);
            return true;
        case R.id.menu_share:
            Intent sit = new Intent(Intent.ACTION_SEND);
            sit.setType("text/plain");
            sit.putExtra(Intent.EXTRA_TEXT, etxt.getText().toString());
            startActivity(Intent.createChooser(sit, getString(R.string.menu_share)));
            return true;
        case R.id.menu_detail:
            if (current_uri != null) {
                DocumentFile df = DocumentFile.fromSingleUri(this, current_uri);
                if (df != null) {
                    String info = getString(R.string.dialog_name) + ": " + df.getName() + "\n" +
                                 getString(R.string.dialog_update) + ": " + new java.util.Date(df.lastModified()).toString() + "\n" +
                                 getString(R.string.dialog_length) + ": " + df.length() + " B";
                    new AlertDialog.Builder(this)
                        .setTitle(R.string.dialog_detail)
                        .setMessage(info)
                        .setPositiveButton(R.string.dialog_ok, null)
                        .show();
                }
            }
            return true;
        case R.id.menu_settings:
            Intent pit = new Intent(this, LesserPadPrefs.class);
            startActivityForResult(pit, 0);
            return true;
    	default:
    		return super.onOptionsItemSelected(mi);
    	}
    }

    public AlertDialog sureDelete(final Activity av){
    	return new AlertDialog.Builder(av)
    		.setMessage(R.string.dialog_delete_sure)
    		.setTitle(R.string.dialog_delete)
    		.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					if (saf_uri != null && current_uri != null) {
						if (DocumentHelper.deleteFile(av, current_uri)) {
							dontsave = true;
							av.finish();
						} else {
							Toast.makeText(av, R.string.mes_del_fail, Toast.LENGTH_SHORT).show();
						}
					}
				}
			})
    		.setNegativeButton(R.string.dialog_cancel, null)
    		.create();
    }

	@Override
	public void finish() {
		Intent data = new Intent();
		if (current_saf_uri != null) {
			data.putExtra("PATH", current_saf_uri.toString());
		}
		setResult(RESULT_OK, data);
		super.finish();
	}

    public void readPrefs(){
    	SharedPreferences sprefs = PreferenceManager.getDefaultSharedPreferences(this);
		String saf_uri_string = sprefs.getString("saf_uri", null);
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
		}
    	font_size = Float.parseFloat(sprefs.getString("font_size", "18.0f"));
    	look_style = Integer.parseInt(sprefs.getString("look_style", "0"));
    	hide_ext = sprefs.getBoolean("hide_ext", false);
    }

	// Placeholders for encryption logic which would be in libLesserPad or similar
	private String toEncrypt(String pass, String text) { return text; }

    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
    @Override public void onTextChanged(CharSequence s, int start, int before, int count) { dontsave = false; }
    @Override public void afterTextChanged(Editable s) {}
}
