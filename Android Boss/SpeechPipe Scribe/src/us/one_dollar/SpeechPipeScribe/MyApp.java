package us.one_dollar.SpeechPipeScribe;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

public class MyApp {
	public static BluetoothLogService mLogService = null;
	public static BluetoothSocket btSocket = null;
	public static BluetoothAdapter mBluetoothAdapter = null;
	public static boolean started = false;  // Have we started the data connection yet	
}
