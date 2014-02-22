package us.one_dollar.SpeechPipeScribe;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.Toast;

public class MicButton extends Button {
	private static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;
	boolean blockOnSizeChanged = false;
	
	int btnState;
	Context ctx;
	public MicButton(Context context) {
		super(context);
		ctx = context;
		Log.d("SpeechPipe", "New MicButton created!");
		this.setBackground(getResources().getDrawable(R.drawable.mic));
	}
	
	public MicButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		ctx = context;
		Log.d("SpeechPipe", "New MicButton created!");
		this.setBackground(getResources().getDrawable(R.drawable.mic));
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		// Set the background of our button to the Mic drawable
		if (blockOnSizeChanged)
			return;
		try {
			// Indicate that the size of our button has changed
			Log.d("SpeechPipe", "Size Changed.  W: " + w + ", H: " + h + ", btnState = " + btnState);
		    // Now get to the settings
		    android.content.SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
		    int s = (w > h ? h : w);
	    	
	    	RelativeLayout.LayoutParams params = (LayoutParams) this.getLayoutParams();
		    params.height = s;
		    params.width = (int) (0.629 * (float) s);
		    params.leftMargin = (w - params.width) / 2;
		    Log.d("SpeechPipe", "New Size.  W: " + params.width + ", H: " + params.height + ", btnState = " + btnState);
		    this.setLayoutParams(params);
	        android.content.SharedPreferences.Editor editor = settings.edit();
	        editor.putInt("micH", params.height);
	        editor.putInt("micW", params.width);
	        editor.commit();
	        blockOnSizeChanged = true;
		} catch (Exception e) {
			Log.d("SpeechPipe", e.getMessage().toString());
			e.printStackTrace();
		}
	}
	
	public void startVoiceRecognitionActivity(Activity a) {
    	Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
    	intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
    			RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
    	// intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your query now.  You can mention artists, song titles, genres, album names, or even years.");
    	intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
    	try {
    		a.startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
    	} catch (Exception e) {
    		Toast.makeText(ctx, "Could not find a Voice Recognition handler.  Please make sure your Input Settings are configured properly.", Toast.LENGTH_SHORT).show();
    	}
    }
}
