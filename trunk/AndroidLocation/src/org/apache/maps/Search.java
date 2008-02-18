package org.apache.maps;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.view.View;

public class Search extends Activity {
    public Search() {}
    /**
     * Called with the activity is first created.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Set the layout for this activity.  You can find it in /res/layout/main.xml
        setContentView(R.layout.search);

        Button addButton = (Button) findViewById(R.id.searchButton);
        addButton.setOnClickListener(
        		new View.OnClickListener() {

	        		public void onClick(View v) {
	        			// return RESULT_OK and Textbox Text to its caller
	        			setResult(RESULT_OK, ((EditText)findViewById(R.id.searchText)).getText().toString());
	        			finish(); // Return to Main Activity Method OnActivityResult
	        		} 
        		}
        );
    }
}
