package org.pulpdust.lesserpad;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.fragment.app.FragmentActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class ProtectActivity extends FragmentActivity {

	int look_style;
	TextView label;
	TextView guide;
	EditText passbox;
	Button canbtn;
	Button okbtn;

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
		if (look_style > 0){
			setTheme(R.style.AppTheme_Dialog_Dark);
		} else {
			setTheme(R.style.AppTheme_Dialog);
		}
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dialog_protect);
		label = (TextView) findViewById(R.id.textView5);
		guide = (TextView) findViewById(R.id.textView4);
		passbox = (EditText) findViewById(R.id.editText2);
		canbtn = (Button) findViewById(R.id.button5);
		okbtn = (Button) findViewById(R.id.button6);
		switch(getIntent().getIntExtra(libLesserPad.REQ_PASS_MODE, libLesserPad.REQ_PASS_FOR_ENC)){
		case libLesserPad.REQ_PASS_FOR_ENC:
			label.setText(R.string.menu_enc);
			guide.setText(R.string.dialog_enter_enckey);
			break;
		case libLesserPad.REQ_PASS_FOR_DEC:
			label.setText(R.string.menu_dec);
			guide.setText(R.string.dialog_enter_deckey);
			break;
		case libLesserPad.REQ_PASS_FOR_OPEN:
			label.setText(R.string.dialog_protect);
			guide.setText(R.string.dialog_enter_deckey);
			break;
		}
		canbtn.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				finish();
			}
			
		});
		okbtn.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.putExtra(libLesserPad.CRYPT_PASS, passbox.getText().toString());
				if (getIntent().getStringExtra(libLesserPad.CRYPT_FILE) != null){
					intent.putExtra(libLesserPad.CRYPT_FILE, getIntent().getStringExtra(libLesserPad.CRYPT_FILE));
				}
				setResult(RESULT_OK, intent);
				finish();
			}
			
		});
	}
	
    public void readPrefs(){
    	SharedPreferences sprefs = PreferenceManager.getDefaultSharedPreferences(this);
    	look_style = Integer.parseInt(sprefs.getString("look_style", "0"));
    }
}
