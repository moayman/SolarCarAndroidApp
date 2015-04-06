package com.solarcar;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Typeface;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/*
* Command
* 0 0 0 0 0 0 0 0
* | | | | ^ ^ ^ ^ -> Speed
* ^ ^ ^ ^ ---------> Angle
* */


public class MainActivity extends ActionBarActivity
{
    private final String TAG = "MAIN";
    public static final int SPEEDLEVELS = 7;
    public static final int ANGLELEVELS = 7;

    private BluetoothAdapter btAdapter;
    private BluetoothDevice HC05_BtModule;
    private BluetoothSocket HC05_Socket;
    private OutputStream HC05_outStream;
    private InputStream HC05_inStream;

    private Receiver receiverThread;

    private ToggleButton btnCarConnection;

    private byte command = 0;

    private SeekBar skbarSpeed;
    private SeekBar skbarAngle;
    private TextView txtSpeed;
    private TextView txtAngle;

    @Override
    protected void onStop()
    {
        btnCarConnection.setChecked(false);
        super.onStop();
    }

    @Override
    protected void onDestroy()
    {
        btnCarConnection.setChecked(false);
        super.onDestroy();
    }

    @Override
    protected void onPause()
    {
        btnCarConnection.setChecked(false);
        super.onPause();
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        disableButtons();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        disableButtons();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Typeface tf = Typeface.createFromAsset(getAssets(), "digital-7.ttf");

        txtSpeed = (TextView) findViewById(R.id.txtSpeed);
        txtAngle = (TextView) findViewById(R.id.txtAngle);

        txtSpeed.setTypeface(tf);
        txtAngle.setTypeface(tf);

        skbarSpeed = (SeekBar) findViewById(R.id.skbarSpeed);
        skbarAngle = (SeekBar) findViewById(R.id.skbarAngle);

        skbarSpeed.setMax(2*SPEEDLEVELS);
        skbarAngle.setMax(2*ANGLELEVELS);

        skbarSpeed.setProgress(SPEEDLEVELS);
        skbarAngle.setProgress(ANGLELEVELS);

        btnCarConnection = (ToggleButton) findViewById(R.id.btnCarConnection);

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        if (btAdapter != null)
        {
            Log.d(TAG, "Bluetooth adapter supported");
            HC05_BtModule = btAdapter.getRemoteDevice("20:13:05:06:44:58");
        }
        else
        {
            Log.d(TAG, "Bluetooth adapter not supported");
            Toast.makeText(getApplicationContext(), "Device does not support Bluetooth.", Toast.LENGTH_LONG).show();
        }

        btnCarConnection.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                if (isChecked)
                {
                    if (!connectHC05Module())
                    {
                        btnCarConnection.setChecked(false);
                        Toast.makeText(getApplicationContext(), "Connection failed.", Toast.LENGTH_LONG).show();
                    }
                    else
                    {
                        enableButtons();
                        receiverThread = new Receiver(HC05_inStream, txtSpeed, txtAngle);
                        new Thread(receiverThread).start();
                        Toast.makeText(getApplicationContext(), "Connected!", Toast.LENGTH_LONG).show();
                    }
                }
                else
                {
                    command = 0;
                    sendCommand();
                    if(receiverThread != null)
                        receiverThread.stopReceiving();
                    disconnectHC05Module();
                    disableButtons();
                    Toast.makeText(getApplicationContext(), "Disonnected!", Toast.LENGTH_LONG).show();
                }
            }
        });

        skbarSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                command &= 0xF0;
                command |= progress;
                sendCommand();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
                ((VerticalSeekBar)skbarSpeed).setProgressAndThumb(SPEEDLEVELS);
            }
        });

        skbarAngle.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                command &= 0x0F;
                command |= (progress << 4);
                sendCommand();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
                skbarAngle.setProgress(ANGLELEVELS);
            }
        });
    }

    private void disableButtons()
    {
        skbarSpeed.setProgress(SPEEDLEVELS);
        skbarAngle.setProgress(ANGLELEVELS);
        skbarSpeed.setEnabled(false);
        skbarAngle.setEnabled(false);
        txtSpeed.setText("0");
        txtAngle.setText("0");
    }

    private void enableButtons()
    {
        skbarSpeed.setEnabled(true);
        skbarAngle.setEnabled(true);
    }

    private void sendCommand()
    {
        Log.e(TAG, "Sending " + String.format("%8s", Integer.toBinaryString(command & 0xFF)).replace(' ', '0'));
        try
        {
            if (HC05_outStream != null)
            {
                HC05_outStream.write(command);
            }
        }
        catch (IOException e)
        {
            Log.e(TAG, "Send Command failed. " + e.getMessage());
            btnCarConnection.setChecked(false);
        }
    }

    private void disconnectHC05Module()
    {
        try
        {
            if (HC05_Socket != null)
            {
                if (HC05_outStream != null)
                {
                    HC05_outStream.close();
                    HC05_outStream = null;
                }
                if (HC05_inStream != null)
                {
                    HC05_inStream.close();
                    HC05_inStream = null;
                }
                HC05_Socket.close();
                HC05_Socket = null;
            }
        }
        catch (IOException e)
        {
            Log.e(TAG, "Disconnecting HC05 module failed. " + e.getMessage());
        }
    }

    private boolean connectHC05Module()
    {
        try
        {
            while (!btAdapter.isEnabled())
            {
                btAdapter.enable();
            }
            if (btAdapter.isDiscovering())
            {
                btAdapter.cancelDiscovery();
            }

            HC05_Socket = HC05_BtModule.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            HC05_Socket.connect();
            HC05_outStream = HC05_Socket.getOutputStream();
            HC05_inStream = HC05_Socket.getInputStream();

            return true;
        }
        catch (IOException e)
        {
            Log.e(TAG, "Connection to HC05 failed. " + e.getMessage());
            disconnectHC05Module();
            return false;
        }
    }
}
