package org.apache.maps;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class Settings extends Activity {
    public Settings() {}
    /**
     * Called with the activity is first created.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        SharedPreferences settings = getSharedPreferences(getString(R.string.app_name), 0);
        
        setContentView(R.layout.settings);

        Button saveButton = (Button) findViewById(R.id.saveButton);

        EditText username = (EditText) findViewById(R.id.usernameText);
        EditText password = (EditText) findViewById(R.id.passwordText);
        EditText refresh = (EditText) findViewById(R.id.refreshText);

        // Get preferences values
        username.setText(settings.getString("username", null));
        password.setText(settings.getString("password", null));
        refresh.setText(settings.getString("refresh", null));
        
        saveButton.setOnClickListener(
        		new View.OnClickListener() {

	        		public void onClick(View v) {
	        		   SharedPreferences settings = getSharedPreferences(getString(R.string.app_name), 0);
        		       SharedPreferences.Editor editor = settings.edit();
        		        // Set preferences values
        		       editor.putString("username", ((EditText) findViewById(R.id.usernameText)).getText().toString());
        		       editor.putString("password", ((EditText) findViewById(R.id.passwordText)).getText().toString());
        		       editor.putString("refresh", ((EditText) findViewById(R.id.refreshText)).getText().toString());
        		       editor.commit();
	        			
	        			// return RESULT_OK and Textbox Text to its caller
	        			setResult(RESULT_OK, null);
	        			finish(); // Return to Main Activity Method OnActivityResult
	        		} 
        		}
        );
    }
}
