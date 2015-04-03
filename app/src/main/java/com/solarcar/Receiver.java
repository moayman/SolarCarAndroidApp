package com.solarcar;

import android.os.Handler;
import android.util.Log;
import android.widget.Switch;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;

public class Receiver implements Runnable
{
    private static final String TAG = "Receiver";
    private InputStream inStream;
    private Switch switchBackward;
    private TextView txtLeft, txtRight;
    private Handler GuiHandler;
    private volatile boolean receive;

    public Receiver(InputStream inputStream, TextView Left, TextView Right, Switch Backward)
    {
        if (inputStream != null)
        {
            inStream = inputStream;
            Log.e(TAG, "Receiving");
        }
        else
        {
            Log.e(TAG, "inputStream = null");
        }
        switchBackward = Backward;
        txtLeft = Left;
        txtRight = Right;
        GuiHandler = new Handler();
        receive = true;
    }

    @Override
    public void run()
    {
        try
        {
            if (inStream != null)
            {
                while(receive)
                {
                    if(inStream.available()>0)
                    {
                        int received = inStream.read();
                        if (received != -1)
                        {
                            GuiHandler.post(new GuiUpdateHandler(txtLeft, txtRight, switchBackward, received));
                        }
                        else
                        {
                            Log.e(TAG, "-1 received");
                        }
                    }
                }
            }
            else
            {
                Log.e(TAG, "inputStream = null");
            }
        }
        catch (IOException e)
        {
            Log.e(TAG, "inStream.read() error. " + e.getMessage());
        }
    }
    public void stopReceiving() {
        receive = false;
    }
}
