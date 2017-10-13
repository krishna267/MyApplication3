package krishna.example.com.myapplication;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class ble extends AppCompatActivity implements LeScanCallback {

	private final String TAG = ble.class.getSimpleName();

	// Bluetooth State
	final private static int STATE_BLUETOOTH_OFF = 1;
	final private static int STATE_DISCONNECTED = 2;
	final private static int STATE_CONNECTING = 3;
	final private static int STATE_CONNECTED = 4;
	final private static byte[] START = { 'b' };
	final private static byte[] STOP = { 's' };

	// Filename for each session
	private String mFilenamePrefix = "openbci";
	private String mExtention = ".html";
	private String mFilenameSuffix = "";
	private String mFilename = "openbci.txt"; // Default Filename

	private int mBluetoothState;
	private boolean mScanStarted;
	private boolean mScanning;

	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothDevice mBluetoothDevice;

	private RFduinoService mRFduinoService=null;
	private Button mEnableBluetoothButton;
	private TextView mScanStatusText;
	private Button mScanButton;
	private TextView mDeviceInfoText;
	private TextView mConnectionStatusText;
	private Button mConnectButton;
	private EditText mCommandText;
	private Button mSendButton;
	private Button mStartButton;
	private Button mStopButton;
    private Button mEnableLocationButton;
	private ProgressBar mProgressBar;
	private TextView mReceiving,debug;
	private Button mViewFileButton;

	private Intent openTextFileIntent = new Intent(Intent.ACTION_VIEW);
	private File directory = new File(
			Environment.getExternalStorageDirectory(), "OpenBCI");

	// Broadcast receiver to monitor BLE connection
	private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);

			if (state == BluetoothAdapter.STATE_ON) {
				upgradeState(STATE_DISCONNECTED);
			} else if (state == BluetoothAdapter.STATE_OFF) {
				downgradeState(STATE_BLUETOOTH_OFF);
			}

		}
	};

	// BroadcastReceiver to receive Bluetooth scan intents
	private final BroadcastReceiver scanModeReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			mScanning = (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_NONE);
			mScanStarted &= mScanning;
			updateUi();
		}
	};

	// BroadcastReceiver to receive data from RFduino intenets
	private final BroadcastReceiver rfduinoReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (RFduinoService.ACTION_CONNECTED.equals(action)) {
				upgradeState(STATE_CONNECTED);
			} else if (RFduinoService.ACTION_DISCONNECTED.equals(action)) {
				downgradeState(STATE_DISCONNECTED);
			} else if (RFduinoService.ACTION_DATA_AVAILABLE.equals(action)) {
				byte[] data = intent.getByteArrayExtra(RFduinoService.EXTRA_DATA);
				byte[] packetData = Arrays.copyOfRange(data, 1, data.length);
				int packetNumber = data[0] & 0xFF;
				Log.i(TAG, "RFduino Data: " + packetNumber + ": " + FormatDataForFile.convertBytesToHex(packetData));
				byte[][] dataForAsyncTask = { mFilename.getBytes(), data };
				debug.setText(data.toString());
				new SaveBytesToFile().execute(dataForAsyncTask);
			}
		}
	};

	// ServiceConnection to monitor connections with the RFduino
	private final ServiceConnection rfduinoServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mRFduinoService = null;
			downgradeState(STATE_DISCONNECTED);
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mRFduinoService = ((RFduinoService.LocalBinder) service).getService();
			if (mRFduinoService.initialize()) {
				if (mRFduinoService.connect(mBluetoothDevice.getAddress())) {
					upgradeState(STATE_CONNECTING);
				}
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ble);

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		debug = (TextView)findViewById(R.id.debug);

		// Bluetooth Button
		mEnableBluetoothButton = (Button) findViewById(R.id.enableBluetooth);
		mEnableBluetoothButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				if (mBluetoothState == STATE_BLUETOOTH_OFF) {
					AlertDialog.Builder confirmOnBluetooth = new Builder(ble.this);
					confirmOnBluetooth.setTitle("Turn on Bluetooth?");
					confirmOnBluetooth.setPositiveButton("Yes",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									mEnableBluetoothButton.setText(mBluetoothAdapter
											.enable() ? "Enabling Bluetooth"
											: "Enable Failed!");
								}
							});
					confirmOnBluetooth.setNegativeButton("No",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.cancel();
								}
							});
					confirmOnBluetooth.show();
				} else {

					AlertDialog.Builder confirmOffBluetooth = new Builder(ble.this);
					String title = mBluetoothState > 2 ? "Bluetooth is being accessed by a device. Sure you want to turn it off?"
							: "Turn Off Bluetooth?";
					confirmOffBluetooth.setTitle(title);
					confirmOffBluetooth.setPositiveButton("Yes",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									mEnableBluetoothButton.setText(mBluetoothAdapter
											.disable() ? "Disabling Bluetooth"
											: "Disable Failed!");
								}
							});
					confirmOffBluetooth.setNegativeButton("No",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.cancel();
								}
							});
					confirmOffBluetooth.show();
				}

			}
		});

        //Enable Location
        mEnableLocationButton = (Button)findViewById(R.id.enableLocation);
        mEnableLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
				v.setEnabled(false);
				displayLocationSettingsRequest(ble.this);
            }
        });

		// Scan for RFDuino
		mScanStatusText = (TextView) findViewById(R.id.scanStatus);

		mScanButton = (Button) findViewById(R.id.scan);
		mScanButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mBluetoothAdapter.startLeScan(
						new UUID[] { RFduinoService.UUID_SERVICE },
						ble.this);
			}
		});

		// Connected RFDuino Info
		mDeviceInfoText = (TextView) findViewById(R.id.deviceInfo);

		// Connect to RFDuino
		mConnectionStatusText = (TextView) findViewById(R.id.connectionStatus);
		mConnectButton = (Button) findViewById(R.id.connect);
		mConnectButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				v.setEnabled(false);
				mConnectionStatusText.setText("Connecting...");
				Intent rfduinoIntent = new Intent(ble.this, RFduinoService.class);
				bindService(rfduinoIntent, rfduinoServiceConnection, BIND_AUTO_CREATE);
			}
		});

		// Send
		mCommandText = (EditText) findViewById(R.id.sendValue);
		mSendButton = (Button) findViewById(R.id.sendButton);
		mSendButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mRFduinoService.send(mCommandText.getText().toString()
						.getBytes());
			}
		});

		// ProgressBar
		mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
		mReceiving = (TextView) findViewById(R.id.receivingLabel);
		mViewFileButton = (Button) findViewById(R.id.viewFileButton);

		// Button to send "b" to the RFduino to begin data transfer
		mStartButton = (Button) findViewById(R.id.startButton);
		mStartButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mRFduinoService.send(START);
				mReceiving.setVisibility(View.VISIBLE);
				mProgressBar.setVisibility(View.VISIBLE);
			}

		});

		// Button to send "s" to the RFduino to begin data transfer
		mStopButton = (Button) findViewById(R.id.stopButton);
		mStopButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				mReceiving.setVisibility(View.INVISIBLE);
				mProgressBar.setVisibility(View.INVISIBLE);
				mRFduinoService.send(STOP);
				mViewFileButton.setEnabled(true);
			}
		});
	}

	private int REQUEST_CHECK_SETTINGS = 5;
	private void displayLocationSettingsRequest(Context context) {
		GoogleApiClient googleApiClient = new GoogleApiClient.Builder(context)
				.addApi(LocationServices.API).build();
		googleApiClient.connect();

		LocationRequest locationRequest = LocationRequest.create();
		locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		locationRequest.setInterval(10000);
		locationRequest.setFastestInterval(10000 / 2);

		LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
		builder.setAlwaysShow(true);

		PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
		result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
			@Override
			public void onResult(LocationSettingsResult result) {
				final Status status = result.getStatus();
				switch (status.getStatusCode()) {
					case LocationSettingsStatusCodes.SUCCESS:
						Log.i(TAG, "All location settings are satisfied.");
						mEnableLocationButton.setEnabled(false);
						break;
					case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
						Log.i(TAG, "Location settings are not satisfied. Show the user a dialog to upgrade location settings ");

						try {
							// Show the dialog by calling startResolutionForResult(), and check the result
							// in onActivityResult().
							status.startResolutionForResult(ble.this, REQUEST_CHECK_SETTINGS);
						} catch (IntentSender.SendIntentException e) {
							Log.i(TAG, "PendingIntent unable to execute request.");
						}
						break;
					case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
						Log.i(TAG, "Location settings are inadequate, and cannot be fixed here. Dialog not created.");
						break;
				}
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == REQUEST_CHECK_SETTINGS){
			if(resultCode == RESULT_OK){
				mEnableLocationButton.setEnabled(false);
			}else {
				Toast.makeText(ble.this,"Location must be enabled",Toast.LENGTH_SHORT).show();
				mEnableLocationButton.setEnabled(true);
			}
		}
	}

	@Override
	protected void onStart() {
		super.onStart();

		// Get filename for each session
		mFilename = getFileNameForSession();
		// Button to view the data file for current session
		mViewFileButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Uri uri = Uri.fromFile(new File(directory, mFilename));
				openTextFileIntent.setDataAndType(uri, "text/plain");
				// openTextFileIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(openTextFileIntent);
			}
		});

		registerReceiver(scanModeReceiver, new IntentFilter(
				BluetoothAdapter.ACTION_SCAN_MODE_CHANGED));
		registerReceiver(bluetoothStateReceiver, new IntentFilter(
				BluetoothAdapter.ACTION_STATE_CHANGED));
		registerReceiver(rfduinoReceiver, RFduinoService.getIntentFilter());

		updateState(mBluetoothAdapter.isEnabled() ? STATE_DISCONNECTED
				: STATE_BLUETOOTH_OFF);
	}

	@Override
	protected void onStop() {
		super.onStop();

		mBluetoothAdapter.stopLeScan(this);

		unregisterReceiver(scanModeReceiver);
		unregisterReceiver(bluetoothStateReceiver);
		unregisterReceiver(rfduinoReceiver);
		try {
			unbindService(rfduinoServiceConnection);
		}catch (Exception e){
			e.printStackTrace();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		mFilename = getFileNameForSession();
	}

	private void upgradeState(int newState) {
		if (newState > mBluetoothState) {
			updateState(newState);
		}
	}

	private void downgradeState(int newState) {
		if (newState < mBluetoothState) {
			updateState(newState);
		}
	}

	private void updateState(int newState) {
		mBluetoothState = newState;
		updateUi();
	}

	private void updateUi() {
		// Enable Bluetooth
		boolean on = mBluetoothState > STATE_BLUETOOTH_OFF;
		mEnableBluetoothButton.setText(on ? "Disable Bluetooth"
				: "Enable Bluetooth");
		mScanButton.setEnabled(on);

		// Scan
		if (mScanStarted && mScanning) {
			mScanStatusText.setText("Scanning...");
			mScanButton.setText("Stop Scanning");
			mScanButton.setEnabled(true);
		} else if (mScanStarted) {
			mScanStatusText.setText("Scan started...");
			mScanButton.setEnabled(false);
		} else {
			mScanStatusText.setText("");
			mScanButton.setText("Scan");
			mScanButton.setEnabled(on);
		}

		// Connect
		boolean connected = false;
		String connectionText = "Disconnected";
		if (mBluetoothState == STATE_CONNECTING) {
			connectionText = "Connecting...";
		} else if (mBluetoothState == STATE_CONNECTED) {
			connected = true;
			connectionText = "Connected";
		}
		mConnectionStatusText.setText(connectionText);
		mConnectButton.setEnabled(mBluetoothDevice != null
				&& mBluetoothState == STATE_DISCONNECTED);

		mCommandText.setEnabled(connected);
		mSendButton.setEnabled(connected);
		mStartButton.setEnabled(connected);
		mStopButton.setEnabled(connected);

	}

	// Calculate filename for session
	private String getFileNameForSession() {
		directory.mkdir();
		SimpleDateFormat formatter = new SimpleDateFormat(
				"yyyy_MM_dd_HH_mm_ss", Locale.getDefault());
		Date now = new Date();
		mFilenameSuffix = formatter.format(now);
		String filename = mFilenamePrefix + mFilenameSuffix + mExtention;
		return filename;
	}

	// Bluetooth callback method used to deliver LE scan results
	@Override
	public void onLeScan(BluetoothDevice device, final int rssi,
                         final byte[] scanRecord) {
		mBluetoothAdapter.stopLeScan(this);
		mBluetoothDevice = device;

		ble.this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mDeviceInfoText.setText(BluetoothHelper.getDeviceInfoText(
						mBluetoothDevice, rssi, scanRecord));
				updateUi();
			}
		});

	}
}
