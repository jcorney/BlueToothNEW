package com.boomgaarden_corney.android.bluetooth;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class BlueToothMainActivity extends Activity {

	private static String errorMsg;

	private final static String DEBUG_TAG = "DEBUG_BLUETOOTH";
	private final static String SERVER_URL = "http://54.86.68.241/bluetooth/test.php";

	private static TextView txtResults;
	private static String blueToothAddress;
	private static String blueToothName;
	private static String blueToothBondStateStr;
	private static String blueToothEnabled;
	
	private static BluetoothClass blueToothClass;

	private static int blueToothBondState;
	private static int blueToothType;
	private final static int REQUEST_ENABLE_BT = 1; 


	private static ParcelUuid[] blueToothUUIDS;

	static BluetoothAdapter mBlueToothAdapter;

	private static List<NameValuePair> paramsDevice = new ArrayList<NameValuePair>();
	private static List<NameValuePair> paramsErrorMsg = new ArrayList<NameValuePair>();
	private static List<NameValuePair> paramsBlueTooth = new ArrayList<NameValuePair>();
	private static List<NameValuePair> paramsBlueToothEnabled = new ArrayList<NameValuePair>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_blue_tooth_main);

		txtResults = (TextView) this.findViewById(R.id.txtResults);
		mBlueToothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		if (!(mBlueToothAdapter.isEnabled())) {
			paramsDevice.add(new BasicNameValuePair("Bluetooth was NOT Enabled", " "));
			mBlueToothAdapter.enable();
			Intent intent = getIntent();
			finish();
			startActivity(intent);
		}
		
		mBlueToothAdapter.startDiscovery();
		setDeviceData();
		showDeviceData();
		sendDeviceData();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.blue_tooth_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private static String buildPostRequest(List<NameValuePair> params)
			throws UnsupportedEncodingException {
		StringBuilder result = new StringBuilder();
		boolean first = true;

		for (NameValuePair pair : params) {
			if (first)
				first = false;
			else
				result.append("&");

			result.append(URLEncoder.encode(pair.getName(), "UTF-8"));
			result.append("=");
			result.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
		}

		return result.toString();
	}

	private String sendHttpRequest(String myURL, String postParameters)
			throws IOException {

		URL url = new URL(myURL);

		// Setup Connection
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setReadTimeout(10000); /* in milliseconds */
		conn.setConnectTimeout(15000); /* in milliseconds */
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);

		// Setup POST query params and write to stream
		OutputStream ostream = conn.getOutputStream();
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				ostream, "UTF-8"));

		if (postParameters.equals("DEVICE")) {
			writer.write(buildPostRequest(paramsDevice));
		} else if (postParameters.equals("BLUETOOTH")) {
			writer.write(buildPostRequest(paramsBlueTooth));
			paramsBlueTooth = new ArrayList<NameValuePair>();
		} else if (postParameters.equals("ERROR_MSG")) {
			writer.write(buildPostRequest(paramsErrorMsg));
			paramsErrorMsg = new ArrayList<NameValuePair>();
		}

		writer.flush();
		writer.close();
		ostream.close();

		// Connect and Log response
		conn.connect();
		int response = conn.getResponseCode();
		Log.d(DEBUG_TAG, "The response is: " + response);

		conn.disconnect();

		return String.valueOf(response);

	}

	private class SendHttpRequestTask extends AsyncTask<String, Void, String> {

		// @params come from SendHttpRequestTask.execute() call
		@Override
		protected String doInBackground(String... params) {
			// params comes from the execute() call: params[0] is the url,
			// params[1] is type POST
			// request to send - i.e. whether to send Device or Telephony
			// parameters.
			try {
				return sendHttpRequest(params[0], params[1]);
			} catch (IOException e) {
				setErrorMsg("Unable to retrieve web page. URL may be invalid.");
				showErrorMsg();
				return errorMsg;
			}
		}
	}

	private void setDeviceData() {

		paramsDevice.add(new BasicNameValuePair("Device", Build.DEVICE));
		paramsDevice.add(new BasicNameValuePair("Brand", Build.BRAND));
		paramsDevice.add(new BasicNameValuePair("Manufacturer",
				Build.MANUFACTURER));
		paramsDevice.add(new BasicNameValuePair("Model", Build.MODEL));
		paramsDevice.add(new BasicNameValuePair("Product", Build.PRODUCT));
		paramsDevice.add(new BasicNameValuePair("Board", Build.BOARD));
		paramsDevice.add(new BasicNameValuePair("Android API", String
				.valueOf(Build.VERSION.SDK_INT)));

	}

	private void setErrorMsg(String error) {
		errorMsg = error;
		paramsErrorMsg.add(new BasicNameValuePair("Error", errorMsg));
	}

	private void showDeviceData() {
		// Display and store (for sending via HTTP POST query) device
		// information
		txtResults.append("Device: " + Build.DEVICE + "\n");
		txtResults.append("Brand: " + Build.BRAND + "\n");
		txtResults.append("Manufacturer: " + Build.MANUFACTURER + "\n");
		txtResults.append("Model: " + Build.MODEL + "\n");
		txtResults.append("Product: " + Build.PRODUCT + "\n");
		txtResults.append("Board: " + Build.BOARD + "\n");
		txtResults.append("Android API: "
				+ String.valueOf(Build.VERSION.SDK_INT) + "\n");
	}

	private void showErrorMsg() {
		Log.d(DEBUG_TAG, errorMsg);
		txtResults.append(errorMsg + "\n");
	}

	private void sendDeviceData() {
		ConnectivityManager connectMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectMgr.getActiveNetworkInfo();

		// Verify network connectivity is working; if not add note to TextView
		// and Logcat file
		if (networkInfo != null && networkInfo.isConnected()) {
			// Send HTTP POST request to server which will include POST
			// parameters with Telephony info
			new SendHttpRequestTask().execute(SERVER_URL, "DEVICE");
		} else {
			setErrorMsg("No Network Connectivity");
			showErrorMsg();
		}
	}

	private void sendErrorMsg() {
		ConnectivityManager connectMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectMgr.getActiveNetworkInfo();

		// Verify network connectivity is working; if not add note to TextView
		// and Logcat file
		if (networkInfo != null && networkInfo.isConnected()) {
			// Send HTTP POST request to server which will include POST
			// parameters with Telephony info
			new SendHttpRequestTask().execute(SERVER_URL, "ERROR_MSG");
		} else {
			setErrorMsg("No Network Connectivity");
			showErrorMsg();
		}
	}

	public static class BlueToothBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			BluetoothDevice device = intent
					.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

			blueToothName = device.getName();
			blueToothAddress = device.getAddress();
			blueToothClass = device.getBluetoothClass();
			blueToothBondState = device.getBondState();
			GetBondStateStr(blueToothBondState);
			blueToothUUIDS = device.getUuids();

			paramsBlueTooth
					.add(new BasicNameValuePair("Name: ", blueToothName));
			paramsBlueTooth.add(new BasicNameValuePair("Address: ",
					blueToothAddress));
			paramsBlueTooth.add(new BasicNameValuePair("Class: ", String
					.valueOf(blueToothClass)));
			paramsBlueTooth.add(new BasicNameValuePair("Bond State: ",
					blueToothBondStateStr));
			paramsBlueTooth.add(new BasicNameValuePair("UUIDS: ", String
					.valueOf(blueToothUUIDS)));

			showBlueToothData();
			sendBlueToothData(context);

		}

		private void sendBlueToothData(Context context) {
			ConnectivityManager connectMgr = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo networkInfo = connectMgr.getActiveNetworkInfo();

			// Verify network connectivity is working; if not add note to
			// TextView
			// and Logcat file
			if (networkInfo != null && networkInfo.isConnected()) {
				// Send HTTP POST request to server which will include POST
				// parameters with Telephony info
				new SendHttpRequestTask().execute(SERVER_URL, "BLUETOOTH");
			} else {
				setErrorMsg("No Network Connectivity");
				showErrorMsg();
			}
		}

		private void setErrorMsg(String error) {
			errorMsg = error;
			paramsErrorMsg.add(new BasicNameValuePair("Error", errorMsg));
		}

		private void showErrorMsg() {
			Log.d(DEBUG_TAG, errorMsg);
			txtResults.append(errorMsg + "\n");
		}

		private void showBlueToothData() {
			StringBuilder results = new StringBuilder();

			results.append("-----BLUETOOTH DEVICE INFORMATION-----\n");
			results.append("Name: " + blueToothName + "\n");
			results.append("Address: " + blueToothAddress + "\n");
			results.append("Class: " + blueToothClass + "\n");
			results.append("Bond State: " + blueToothBondStateStr + "\n");
			results.append("UUIDS: " + blueToothUUIDS + "\n");

			txtResults.append(new String(results));
			txtResults.append("\n");
		}

		private String sendHttpRequest(String myURL, String postParameters)
				throws IOException {

			URL url = new URL(myURL);

			// Setup Connection
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setReadTimeout(10000); /* in milliseconds */
			conn.setConnectTimeout(15000); /* in milliseconds */
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);

			// Setup POST query params and write to stream
			OutputStream ostream = conn.getOutputStream();
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					ostream, "UTF-8"));

			if (postParameters.equals("DEVICE")) {
				writer.write(buildPostRequest(paramsDevice));
			} else if (postParameters.equals("BLUETOOTH")) {
				writer.write(buildPostRequest(paramsBlueTooth));
				paramsBlueTooth = new ArrayList<NameValuePair>();
			} else if (postParameters.equals("ERROR_MSG")) {
				writer.write(buildPostRequest(paramsErrorMsg));
				paramsErrorMsg = new ArrayList<NameValuePair>();
			}

			writer.flush();
			writer.close();
			ostream.close();

			// Connect and Log response
			conn.connect();
			int response = conn.getResponseCode();
			Log.d(DEBUG_TAG, "The response is: " + response);

			conn.disconnect();

			return String.valueOf(response);

		}

		private class SendHttpRequestTask extends
				AsyncTask<String, Void, String> {

			// @params come from SendHttpRequestTask.execute() call
			@Override
			protected String doInBackground(String... params) {
				// params comes from the execute() call: params[0] is the url,
				// params[1] is type POST
				// request to send - i.e. whether to send Device or
				// Telephony
				// parameters.
				try {
					return sendHttpRequest(params[0], params[1]);
				} catch (IOException e) {
					setErrorMsg("Unable to retrieve web page. URL may be invalid.");
					showErrorMsg();
					return errorMsg;
				}
			}
		}

		private void GetBondStateStr(int state) {
			switch (state) {
			case 10:
				blueToothBondStateStr = "10 - Device is not bonded (paired).";
				break;

			case 11:
				blueToothBondStateStr = "11 - Bonding (pairing) is in progress with the remote device.";
				break;

			case 12:
				blueToothBondStateStr = "12 - Indicates the remote device is bonded (paired).";
				break;

			default:
				break;
			}
		}
	}
	
	protected void onResume() {
		mBlueToothAdapter.startDiscovery();
		super.onResume();
		
		
	}
}
