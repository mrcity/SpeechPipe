package us.one_dollar.SpeechPipeScribe;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


public class SpeechActivity extends Activity {

	private static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;

	public Context ctx = this;
	public Activity a = this;
	private MicButton mic_button;
	private Button btn_resend;
	private EditText edit_speech;
	protected EditText txt_IP;
	public Socket socket;
	private int lastStringLen;
	
	DefaultHttpClient httpclient = new DefaultHttpClient();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_speech);
		
		edit_speech = (EditText) findViewById(R.id.edit_speech);
		socket = null;
		
	    mic_button = (MicButton) findViewById(R.id.mic_button);
	    mic_button.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
			    mic_button.startVoiceRecognitionActivity(a);
			}
		});
	    
		btn_resend = (Button) findViewById(R.id.btn_resend);
	    btn_resend.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
		    	// First, use the Backspace key to erase what was sent previously
				// Be considerate that some terminals like Ctrl+H or Delete better than Backspace
				char[] bspArray = new char[lastStringLen + 2];
				for (int i = 0; i < lastStringLen + 2; i++) {
					bspArray[i] = (char) 0x08;
				}
				String whatToErase = String.copyValueOf(bspArray);
				NetworkThread bkspTask = new NetworkThread();
	    		bkspTask.execute(whatToErase);
	    		// Now send the new data
	    		String whatToWrite = edit_speech.getText().toString();
	    		NetworkThread writeTask = new NetworkThread();
	    		writeTask.execute(whatToWrite);
	    		lastStringLen = whatToWrite.length();
			}
		});
	    
		connectBluetooth = (Button) findViewById(R.id.button1);
		connectedTo = (TextView) findViewById(R.id.lblConnectedTo);
		
		// Do Bluetooth
		Log.e("M360PICKUPMGR", "+++ ON CREATE +++");
		MyApp.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (MyApp.mBluetoothAdapter == null) {
			AlertDialog deleteAlert = new AlertDialog.Builder(ctx).create();
			deleteAlert.setTitle("Error");
			deleteAlert.setMessage("Bluetooth is not available on your device.");
			deleteAlert.setButton(DialogInterface.BUTTON_NEUTRAL, "OK", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			deleteAlert.show();
			finish();
		}
		if (!MyApp.mBluetoothAdapter.isEnabled()) {
			MyApp.mBluetoothAdapter.enable();
		}
		Log.e("M360PICKUPMGR", "+++ DONE IN ON CREATE, GOT LOCAL BT ADAPTER +++");
		
		MyApp.mLogService = new BluetoothLogService(this, this.mHandler);

		/* ************************************************************
		 * CONNECT TO BLUETOOTH
		 * ************************************************************/
		connectBluetooth.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (MyApp.started) {
					MyApp.started = false;
					MyApp.mLogService.stop();
					connectBluetooth.setText("Connect to Bluetooth");
				} else {
					Intent intent = new Intent(SpeechActivity.this, DeviceListActivity.class);
					startActivityForResult(intent, DEVICE_SELECT);
				}
			}			
		});

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.speech, menu);
		return true;
	}

    /**
     * Called when a menu item is selected.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	// Get the menu item selected
        switch (item.getItemId()) {
        case R.id.action_settings:
        	// User selected the Settings menu item, so show the Settings screen
            startActivity(new Intent(this, PreferencesActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    
	/**
	 * Handle the results from the speech-to-text recognition activity.
	 * This class (StreetBeats) must handle the result because it is a FragmentActivity.
	 * The fragments themselves are not set up to handle it, and neither is the MicButton.
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
    		// Fill the list view with the strings the recognizer thought it could have heard
    		ArrayList<String> matches = data.getStringArrayListExtra(
    				RecognizerIntent.EXTRA_RESULTS);
    		String result = "";
    		for (String s : matches)
    			result += s + " ";
    		result = result.trim();
    		edit_speech.setText(result);
    		if (MyApp.started) {
				DirectDrive.writeSingle( result );
    		} else {
	    		NetworkThread task = new NetworkThread();
	    		task.execute(result);
	    		lastStringLen = result.length();
    		}
    	} else {
    		System.out.println("M360PICKUPMGR onActivityResult: (" + requestCode + ", " + resultCode + ")");
    		if (requestCode == 1)
    			return;
    		else if (requestCode == DEVICE_SELECT)
    			if (resultCode == DeviceListActivity.DEVICE_SELECTED)
    				connectDevice(data, false);
    		else if (requestCode == 3)
    			if (resultCode == -1)
    				connectDevice(data, false);
    		else if (requestCode == 4) {
    			if (resultCode == -1) {
    				//setupLog();
    			} else {
    				System.out.println("M360PICKUPMGR BT not enabled");
    				Toast.makeText(this, 0x7f040004, 0).show();
    				finish();
    			}
    		} else if (requestCode == 5) {
    			MyApp.mLogService.stop();
    		} else if (requestCode == 6) {
    			// mConversationArrayAdapter.clear();
    		}

    	}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	public void authenticate() {
        try {
            //httpclient.getCredentialsProvider().setCredentials(
            //        new AuthScope(null, -1),
            //        new UsernamePasswordCredentials("114042449736049687152", "7ca3f7c3-0d48-4d8e-a453-5924eb68e686"));
 
        	UsernamePasswordCredentials creds = new UsernamePasswordCredentials("114042449736049687152", "7ca3f7c3-0d48-4d8e-a453-5924eb68e686");
            HttpGet httpget = new HttpGet("http://api.mq.tt/2/account");
            httpget.addHeader(new BasicScheme().authenticate(creds, httpget));
 
            Log.d("SpeechPipe", "executing request" + httpget.getRequestLine());
            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
 
            Log.d("SpeechPipe", "----------------------------------------");
            Log.d("SpeechPipe", response.getStatusLine().toString());
            if (entity != null) {
                System.out.println("Response content length: " + entity.getContentLength());
                System.out.println(EntityUtils.toString(entity));
            }
        } catch(Exception e){
        	e.printStackTrace();
        } finally {
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
        }
		
	}
	
	private class NetworkThread extends AsyncTask<String, Integer, Integer> {

		private String targetIP = null;
		
		@Override
		protected void onPreExecute() {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
			targetIP = prefs.getString("ipPref", "127.0.0.1");
		}
		
		@Override
		protected Integer doInBackground(String... speechResult) {
			// TODO Auto-generated method stub
			try {
				Looper.prepare();
			} catch (Exception e) {
				try {
					Thread.sleep(500);
				} catch (Exception e2) {
					Log.d("SpeechPipe", "Operation cancelled - thread interrupted.");
				}
			}
        	try {
        		/*
        		// Do output
        		//android.content.SharedPreferences settings = getSharedPreferences("NetflixRemoteSettings", MODE_WORLD_WRITEABLE);
        		//if (socket == null) {
	                //String clientIP = "192.168.1.9"; // settings.getString("nflxClient", "10.0.0.1");
	        		InetAddress serverAddr = InetAddress.getByName(targetIP);
	    			socket = new Socket(serverAddr, 2323);
        		//}
        		// hook up to the computer running the daemon/background process to receive the text
    			String message = speechResult[0];
            	PrintWriter out = new PrintWriter( new BufferedWriter( new OutputStreamWriter(socket.getOutputStream())),true);
            	// print the message
            	out.print(message);
            	// take into consideration just exactly how the user wants line breaks done
            	android.content.SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
            	String lineBreakStylePref = settings.getString("lineBreakStylePref", "CR+LF");
            	if (lineBreakStylePref.equals("CR+LF") || lineBreakStylePref.equals("CR")) {
            		out.print((char) 0xD);
            	}
            	if (lineBreakStylePref.equals("CR+LF") || lineBreakStylePref.equals("LF")) {
            		out.print((char) 0xA);
            	}
            	// Do input
            	//BufferedReader b = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            	//System.out.println(b.readLine());
            	socket.close();
            	*/
        		
        		authenticate();
        		

    	        try {
    	            //httpclient.getCredentialsProvider().setCredentials(
    	            //        new AuthScope(null, -1),
    	            //        new UsernamePasswordCredentials("114042449736049687152", "7ca3f7c3-0d48-4d8e-a453-5924eb68e686"));
    	 
    	            HttpPost httppost = new HttpPost("http://api.mq.tt/2/account/domain/114042449736049687152/stuff/TestStuff/thing/TestThing/publish");
    	        	UsernamePasswordCredentials creds = new UsernamePasswordCredentials("114042449736049687152", "7ca3f7c3-0d48-4d8e-a453-5924eb68e686");
    	        	String message = speechResult[0];
    	        	
    	            httppost.addHeader(new BasicScheme().authenticate(creds, httppost));
    	            httppost.setEntity(new StringEntity("payload={\"serial\":\"" + message + "\"}"));
        httppost.addHeader("content-type", "application/x-www-form-urlencoded");
    	            Log.d("SpeechPipe", "executing request" + httppost.getRequestLine());
    	            HttpResponse response = httpclient.execute(httppost);
    	            HttpEntity entity = response.getEntity();
    	 
    	            Log.d("SpeechPipe", "----------------------------------------");
    	            Log.d("SpeechPipe", response.getStatusLine().toString());
    	            if (entity != null) {
    	                System.out.println("Response content length: " + entity.getContentLength());
    	                System.out.println(EntityUtils.toString(entity));
    	            }
    	        } catch(Exception e){
    	        	e.printStackTrace();
    	        } finally {
    	            // When HttpClient instance is no longer needed,
    	            // shut down the connection manager to ensure
    	            // immediate deallocation of all system resources
    	            //httpclient.getConnectionManager().shutdown();
    	            //Log.d("SpeechPipe", "Connection closed successfully.");
    	        }

        	} catch (Exception e) {
        		Toast.makeText(ctx, "TCP problem: " + e.getMessage(), Toast.LENGTH_LONG).show();
        	} 
			return 1;
		}	
	}
	
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;
	public static final int DEVICE_SELECT = 2;

	public static final String DEVICE_NAME = "device_name";
	private ArrayAdapter<String> mConversationArrayAdapter;
	private String mConnectedDeviceName = null;
	private Button connectBluetooth;
	private Button send;
	public TextView connectedTo;

	// Here's a handler that handles Bluetooth events
	private Handler mHandler = new Handler() {
		public void handleMessage(Message m) {
			if (m.what == MESSAGE_STATE_CHANGE) {
				Log.i("M360PICKUPMGR", "MESSAGE_STATE_CHANGE: " + m.arg1);
				if (m.arg1 == 0 || m.arg1 == 1 || m.arg2 == 1)
					connectedTo.setText(R.string.title_not_connected);
				else if (m.arg1 == 2)
					connectedTo.setText(R.string.title_connecting);
				else if (m.arg1 == 3) {
					connectedTo.setText(R.string.title_connected_to);
					MyApp.started = true;
					connectBluetooth.setText("Disconnect");
					if (mConnectedDeviceName == null)
						mConnectedDeviceName = "(No Name)";
					connectedTo.append(" - " + mConnectedDeviceName);
				}
			} else if (m.what == MESSAGE_WRITE) {
				String s2 = new String((byte[]) m.obj);
				// mConversationArrayAdapter.add("Me:  " + s2);
			} else if (m.what == MESSAGE_READ) {
				// There will not be any reads taking place from the BT serial adapter yet
			} else if (m.what == MESSAGE_DEVICE_NAME) {
				mConnectedDeviceName = m.getData().getString("device_name");
			} else if (m.what == MESSAGE_TOAST) {
				String t = m.getData().getString("toast");
				Toast.makeText(ctx, t, Toast.LENGTH_SHORT).show();
			}
			super.handleMessage(m);
		}
	};
	
	private void connectDevice(Intent i, boolean flag) {
		String str = i.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
		BluetoothDevice localBluetoothDevice = MyApp.mBluetoothAdapter.getRemoteDevice(str);
		MyApp.mLogService.connect(localBluetoothDevice, flag);
	}

}
