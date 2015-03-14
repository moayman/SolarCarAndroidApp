package com.solarcar;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/*
* Command
* 0 0 0 0 0 0 0 0
*       | | | | ^ -> Forward
*       | | | ^ ---> Backward
*       | ^ ^ -----> SteeringAngle
*       ^ ---------> Right = 1 Left = 0
* */


public class MainActivity extends ActionBarActivity
{

    private final String TAG = "MAIN";
    public static final byte STEERINGOFFSET = 3;

    private BluetoothAdapter btAdapter;
    private BluetoothDevice HC05_BtModule;
    private BluetoothSocket HC05_Socket;
    private OutputStream HC05_outStream;
    private InputStream HC05_inStream;

    private ImageButton btnBackward;
    private ImageButton btnForward;
    private ImageButton btnRight;
    private ImageButton btnLeft;
    private ImageButton btnReset;

    private ToggleButton btnCarConnection;

    private ProgressBar prgrsbarLeft;
    private ProgressBar prgrsbarRight;
    private ProgressBar prgrsbarForward;
    private ProgressBar prgrsbarBackward;

    private byte steeringAngle = 0;
    private byte command = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnForward = (ImageButton) findViewById(R.id.btnForward);
        btnBackward = (ImageButton) findViewById(R.id.btnBackward);
        btnRight = (ImageButton) findViewById(R.id.btnRight);
        btnLeft = (ImageButton) findViewById(R.id.btnLeft);
        btnReset = (ImageButton) findViewById(R.id.btnReset);

        btnCarConnection = (ToggleButton) findViewById(R.id.btnCarConnection);

        prgrsbarLeft = (ProgressBar) findViewById(R.id.progressBarLeft);
        prgrsbarRight = (ProgressBar) findViewById(R.id.progressBarRight);
        prgrsbarForward = (ProgressBar) findViewById(R.id.progressBarForward);
        prgrsbarBackward = (ProgressBar) findViewById(R.id.progressBarBackward);

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

        btnForward.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View arg0, MotionEvent arg1)
            {
                if (arg1.getAction() == MotionEvent.ACTION_DOWN)
                {
                    btnForward.setBackgroundResource(R.drawable.forward_pressed);
                    btnBackward.setClickable(false);
                    command |= 0x01;

                    sendCommand();
                    receiveAndUpdateStatus();

                    return true;
                }
                else if (arg1.getAction() == MotionEvent.ACTION_UP)
                {
                    btnForward.setBackgroundResource(R.drawable.forward);
                    btnBackward.setEnabled(true);
                    command &= 0xFE;

                    sendCommand();
                    receiveAndUpdateStatus();

                    return true;
                }
                return false;
            }
        });

        btnBackward.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View arg0, MotionEvent arg1)
            {
                if (arg1.getAction() == MotionEvent.ACTION_DOWN)
                {
                    btnBackward.setBackgroundResource(R.drawable.backward_pressed);
                    btnForward.setClickable(false);
                    command |= 0x02;

                    sendCommand();
                    receiveAndUpdateStatus();

                    return true;
                }
                else if (arg1.getAction() == MotionEvent.ACTION_UP)
                {
                    btnBackward.setBackgroundResource(R.drawable.backward);
                    btnForward.setEnabled(true);
                    command &= 0xFD;

                    sendCommand();
                    receiveAndUpdateStatus();

                    return true;
                }
                return false;
            }
        });

        btnRight.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View arg0, MotionEvent arg1)
            {
                if (arg1.getAction() == MotionEvent.ACTION_DOWN)
                {
                    btnRight.setBackgroundResource(R.drawable.right_pressed);
                    btnLeft.setClickable(false);
                    if (((command >> 4) & 1) != 0 || steeringAngle == 0)
                    {
                        command |= 0x10;
                        if (steeringAngle < STEERINGOFFSET)
                            steeringAngle++;
                    }
                    else if (((command >> 4) & 1) == 0 && steeringAngle > 0)
                        steeringAngle--;

                    if ((steeringAngle & 1) == 0)
                        command |= 0x04;
                    else
                        command &= 0xFB;

                    if ((steeringAngle & 2) == 0)
                        command |= 0x08;
                    else
                        command &= 0xF7;

                    sendCommand();
                    receiveAndUpdateStatus();

                    return true;
                }
                else if (arg1.getAction() == MotionEvent.ACTION_UP)
                {
                    btnRight.setBackgroundResource(R.drawable.right);
                    btnLeft.setEnabled(true);
                    return true;
                }
                return false;
            }
        });

        btnLeft.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View arg0, MotionEvent arg1)
            {
                if (arg1.getAction() == MotionEvent.ACTION_DOWN)
                {
                    btnLeft.setBackgroundResource(R.drawable.left_pressed);
                    btnRight.setClickable(false);
                    if (((command >> 4) & 1) == 0 || steeringAngle == 0)
                    {
                        command &= 0xEF;
                        if (steeringAngle < STEERINGOFFSET)
                            steeringAngle++;
                    }
                    else if (((command >> 4) & 1) != 0 && steeringAngle > 0)
                        steeringAngle--;

                    if ((steeringAngle & 1) == 0)
                        command |= 0x04;
                    else
                        command &= 0xFB;

                    if ((steeringAngle & 2) == 0)
                        command |= 0x08;
                    else
                        command &= 0xF7;

                    sendCommand();
                    receiveAndUpdateStatus();

                    return true;
                }
                else if (arg1.getAction() == MotionEvent.ACTION_UP)
                {
                    btnLeft.setBackgroundResource(R.drawable.left);
                    btnRight.setEnabled(true);
                    return true;
                }
                return false;
            }
        });

        btnReset.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View arg0, MotionEvent arg1)
            {
                btnLeft.setClickable(false);
                btnRight.setClickable(false);
                btnLeft.setEnabled(true);
                btnRight.setEnabled(true);
                btnLeft.setBackgroundResource(R.drawable.left);
                btnRight.setBackgroundResource(R.drawable.right);
                prgrsbarLeft.setProgress(0);
                prgrsbarRight.setProgress(0);
                command &= 0x03;

                sendCommand();
                receiveAndUpdateStatus();

                return true;
            }
        });

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

    }


    private void disableButtons()
    {
        btnBackward.setClickable(false);
        btnForward.setClickable(false);
        btnRight.setClickable(false);
        btnLeft.setClickable(false);
        btnReset.setClickable(false);
        btnBackward.setBackgroundResource(R.drawable.backward);
        btnForward.setBackgroundResource(R.drawable.forward);
        btnRight.setBackgroundResource(R.drawable.right);
        btnLeft.setBackgroundResource(R.drawable.left);
        prgrsbarLeft.setProgress(0);
        prgrsbarRight.setProgress(0);
        prgrsbarBackward.setProgress(0);
        prgrsbarForward.setProgress(0);
    }

    private void enableButtons()
    {
        btnBackward.setClickable(true);
        btnForward.setClickable(true);
        btnRight.setClickable(true);
        btnLeft.setClickable(true);
        btnReset.setClickable(true);
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
        (new Thread(new Receiver(HC05_inStream, prgrsbarLeft, prgrsbarRight, prgrsbarForward, prgrsbarBackward))).start();
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
