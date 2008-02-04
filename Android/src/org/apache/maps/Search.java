package org.apache.maps;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.view.View;

public class Search extends Activity {
    public Search() {
    }

    /**
     * Called with the activity is first created.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Set the layout for this activity.  You can find it
        // in res/layout/hello_activity.xml
        setContentView(R.layout.main);

        Button addButton = (Button) findViewById(R.id.searchButton);
        addButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                setResult(RESULT_OK, ((EditText)findViewById(R.id.searchText)).getText().toString());
                finish();
            } });

    }
}
