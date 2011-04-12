package org.example.translate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

public class Translate extends Activity {
    /** Called when the activity is first created. */
	private static final String TAG = "TranslateTask";
	
	private Spinner fromSpinner;
	private Spinner toSpinner;
	private EditText origText;
	private TextView transText;
	private TextView retransText;
	
	private TextWatcher textWatcher;
	private OnItemSelectedListener itemListener;
	
	private Handler guiThread;
	private ExecutorService transThread;
	private Runnable updateTask;
	private Future transPending;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        initThreading();
        findViews();
        setAdapters();
        setListeners();
    }
	
	private void findViews() {
		fromSpinner = (Spinner) findViewById(R.id.from_language);
		origText = (EditText) findViewById(R.id.original_text);
		toSpinner = (Spinner) findViewById(R.id.to_language);
		transText = (TextView) findViewById(R.id.translated_text);
		retransText = (TextView) findViewById(R.id.retranslated_text);
		
	}
	
	private void setAdapters() {
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.languages, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		fromSpinner.setAdapter(adapter);
		toSpinner.setAdapter(adapter);
		
		fromSpinner.setSelection(8);
		toSpinner.setSelection(12);
		
	}
	
	private void setListeners() {
		textWatcher = new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				queueUpdate(1000);
				Log.d(TAG, "New queueUpdate called");
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int arg1, int arg2,
					int arg3) {
				
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				
			}
		};
		
		itemListener = new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView parent, View v,
					int position, long id) {
				queueUpdate(200);
				
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				
			}
			
		};
		
		origText.addTextChangedListener(textWatcher);
		fromSpinner.setOnItemSelectedListener(itemListener);
		toSpinner.setOnItemSelectedListener(itemListener);
		
	}
	private void initThreading() {
		guiThread = new Handler();
		transThread = Executors.newSingleThreadExecutor();
		
		updateTask = new Runnable() {
			
			@Override
			public void run() {
				String original = origText.getText().toString().trim();
				
				if (transPending != null)
					transPending.cancel(true);
				
				if (original.length() == 0){
					transText.setText(R.string.empty);
					retransText.setText(R.string.empty);
				} else {
					transText.setText("Translating...");
					retransText.setText("Translating...");
					
					try{
						TranslateTask translateTask = new TranslateTask(
								Translate.this,
								original,
								getLang(fromSpinner),
								getLang(toSpinner)
								);
						Log.d(TAG, "New TranslateTask:"+ original + ","+ getLang(fromSpinner) + "," + getLang(toSpinner));
						transPending = transThread.submit(translateTask);
					} catch(RejectedExecutionException e) {
						Log.e(TAG, "New translate task not submitted.");
						transText.setText(R.string.translation_error);
						retransText.setText(R.string.translation_error);
					}
				}
			}
		};
	}
	
	private String getLang(Spinner spinner){
		String result = spinner.getSelectedItem().toString();
		int lparen = result.indexOf('(');
		int rparen = result.indexOf(')');
		result = result.substring(lparen+1, rparen);
		return result;
	}
	
	private void queueUpdate(long delayMillis){
		guiThread.removeCallbacks(updateTask);
		boolean done = guiThread.postDelayed(updateTask, delayMillis);
		Log.d(TAG, "new UpdateTask queued. Response: "+done);
	}
	
	public void setTranslated(String text){
		guiSetText(transText,text);
	}
	
	public void setRetranslated(String text){
		guiSetText(retransText,text);
	}
	
	private void guiSetText(final TextView view, final String text){
		guiThread.post(new Runnable() {
			public void run() {
				view.setText(text);
			}
		});
	}
}