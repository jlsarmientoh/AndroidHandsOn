package com.globant.mobile.handson;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends Activity {

	public final static String 	EXTRA_MESSAGE = "com.globant.mobile.handson.MESSAGE";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		//Handle presses on the action bar items
		switch(item.getItemId()){
			case R.id.action_settings:{
				//openSettings();
				return true;
			}
			case R.id.action_search:{
				//openSearch();
				return true;
			}
			default:{
				return super.onOptionsItemSelected(item);
			}
		}
	}
	
	public void sendMessage(View view){
		String message = null;
		
		Intent intent = new Intent(this, DisplayMessageActivity.class);
		
		EditText editText = (EditText)findViewById(R.id.edit_message);
		message = editText.getText().toString();
		intent.putExtra(EXTRA_MESSAGE, message);
		
		startActivity(intent);
	}

}
