package com.solarcar;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Typeface;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
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
*       | | | ^ ^ -> Right
*       | ^ ^ -----> Left
*       ^ ---------> Backward = 1 Forward = 0
* */


public class MainActivity extends ActionBarActivity
{

    private final String TAG = "MAIN";
    public static final int LEVELS = 3;

    private BluetoothAdapter btAdapter;
    private BluetoothDevice HC05_BtModule;
    private BluetoothSocket HC05_Socket;
    private OutputStream HC05_outStream;
    private InputStream HC05_inStream;

    private ToggleButton btnCarConnection;

    private byte command = 0;

    private SeekBar skbarLeft;
    private SeekBar skbarRight;
    private TextView txtLeft;
    private TextView txtRight;
    private Switch switchBackward;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Typeface tf = Typeface.createFromAsset(getAssets(), "digital-7.ttf");

        txtLeft = (TextView) findViewById(R.id.txtLeft);
        txtRight = (TextView) findViewById(R.id.txtRight);

        txtLeft.setTypeface(tf);
        txtRight.setTypeface(tf);

        switchBackward = (Switch) findViewById(R.id.switchBackward);

        skbarLeft = (SeekBar) findViewById(R.id.skbarLeft);
        skbarRight = (SeekBar) findViewById(R.id.skbarRight);
        skbarLeft.setMax(LEVELS);
        skbarRight.setMax(LEVELS);

        btnCarConnection = (ToggleButton) findViewById(R.id.btnCarConnection);

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        disableButtons();

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
                        Toast.makeText(getApplicationContext(), "Connected!", Toast.LENGTH_LONG).show();
                    }
                }
                else
                {
                    command = 0;
                    disconnectHC05Module();
                    disableButtons();
                    Toast.makeText(getApplicationContext(), "Disonnected!", Toast.LENGTH_LONG).show();
                }
            }
        });

        skbarLeft.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                command &= 0xF3;

                command |= (progress << 2);

                sendCommand();
                receiveAndUpdateStatus();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
            }
        });

        skbarRight.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                command &= 0xFC;

                command |= progress;

                sendCommand();
                receiveAndUpdateStatus();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
            }
        });

        switchBackward.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                if(isChecked)
                    command |= 0x10;
                else
                    command &= 0xEF;

                sendCommand();
                receiveAndUpdateStatus();
            }
        });

    }

    private void disableButtons()
    {
        skbarLeft.setProgress(0);
        skbarRight.setProgress(0);
        skbarLeft.setEnabled(false);
        skbarRight.setEnabled(false);
        txtLeft.setText("0");
        txtRight.setText("0");
    }

    private void enableButtons()
    {
        skbarLeft.setEnabled(true);
        skbarRight.setEnabled(true);
    }

    private void sendCommand()
    {
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

    private void receiveAndUpdateStatus()
    {
        (new Thread(new Receiver(HC05_inStream, txtLeft, txtRight, switchBackward))).start();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
