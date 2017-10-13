/* Copyright (c) 2014 OpenBCI
 * See the file license.txt for copying permission.
 * */

package krishna.example.com.myapplication;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class SaveBytesToFile extends AsyncTask<byte[], Void, Void> {

	@Override
	protected Void doInBackground(byte[]... arduinoData) {
		// Get file name for current session
		String filename = new String(arduinoData[0]);
		// Store data from RFduino
		byte[] packet = arduinoData[1];
        Log.w("bytelength",""+packet.length);
		// Store packet data
		// The first byte is the sample counter
		byte sampleCounter = packet[0];
		// Next we have 6 data values 3 bytes each
		byte[] packetDataChannel1 = Arrays.copyOfRange(packet, 1, 4);
		byte[] packetDataChannel2 = Arrays.copyOfRange(packet, 4, 7);
//		byte[] packetDataChannel3 = Arrays.copyOfRange(packet, 7, 10);
//		byte[] packetDataChannel4 = Arrays.copyOfRange(packet, 10, 13);
//		byte[] packetDataChannel5 = Arrays.copyOfRange(packet, 13, 16);
//		byte[] packetDataChannel6 = Arrays.copyOfRange(packet, 16, 19);
//		The last byte is an Auxiliary byte
//		byte packetDataAux = packet[4];

		// Data Conversion for File Writing
		// Get the sample number
		int sNumber = sampleCounter & 0xFF;
		String sampleNumberString = Integer.valueOf(sNumber).toString();

		// Get Channel data in micro Volts
		float[] channel = new float[2];
		channel[0] = OpenBCIDataConversion.convertByteToMicroVolts(packetDataChannel1);
		Log.w("Channel0 data",String.valueOf(channel[0]));
		Log.w("sample",sampleNumberString);
		channel[1] = OpenBCIDataConversion
				.convertByteToMicroVolts(packetDataChannel2);
		Log.w("Channel1 data",String.valueOf(channel[1]));
//		channel[2] = OpenBCIDataConversion
//				.convertByteToMicroVolts(packetDataChannel3);
//		channel[3] = OpenBCIDataConversion
//				.convertByteToMicroVolts(packetDataChannel4);
//		channel[4] = OpenBCIDataConversion
//				.convertByteToMicroVolts(packetDataChannel5);
//		channel[5] = OpenBCIDataConversion
//				.convertByteToMicroVolts(packetDataChannel6);

		// Get Auxiliary byte
//		int auxNumber = packetDataAux & 0xFF;
//		String auxNumberString = Integer.valueOf(auxNumber).toString();

		// Create the file for the current session
		File directory = new File(Environment.getExternalStorageDirectory(), "OpenBCI");
		File file = new File(directory, filename);
		DataOutputStream dos;
		try {
			dos = new DataOutputStream(new FileOutputStream(file.getPath(), true));
			Log.w("filename",filename);
			dos.writeChars(sampleNumberString);
			dos.writeChars(", ");
			dos.writeChars(FormatDataForFile.convertFloatArrayToString(channel));
			dos.writeChars("\n");
			Log.w("File",file.getPath());
//			dos.writeChars(auxNumberString);
//			dos.write(System.getProperty("line.separator").getBytes());
			dos.close();
		} catch (FileNotFoundException e) {
			Log.e("Save to File AsyncTask", "Error finding file");
		} catch (IOException e) {
			Log.e("Save to File AsyncTask", "Error saving data");
		}
		return null;
	}

}
