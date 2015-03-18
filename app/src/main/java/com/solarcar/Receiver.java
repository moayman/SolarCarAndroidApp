package com.solarcar;

import android.os.Handler;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;

public class Receiver implements Runnable
{
    private static final String TAG = "Receiver";
    private InputStream inStream;
    private ProgressBar prgrsbarLeft, prgrsbarRight, prgrsbarForeward, prgrsbarBackward;

    public Receiver(InputStream inputStream, ProgressBar Left, ProgressBar Right, ProgressBar Fore, ProgressBar Back)
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
        prgrsbarForeward = Fore;
        prgrsbarBackward = Back;
        prgrsbarLeft = Left;
        prgrsbarRight = Right;
    }

    @Override
    public void run()
    {
        try
        {
            int received = inStream.read();
            if(received != -1)
            {
                int steeringAngle = (received & 0x0C) >> 2;
                int progress = Math.round((float)steeringAngle/MainActivity.STEERINGOFFSET*100);
                if(((received >> 4) & 1) == 0)
                {
                    prgrsbarLeft.setProgress(progress);
                    prgrsbarRight.setProgress(0);
                }
                else
                {
                    prgrsbarRight.setProgress(progress);
                    prgrsbarLeft.setProgress(0);
                }
                if((received & 1) == 0)
                    prgrsbarForeward.setProgress(0);
                else
                    prgrsbarForeward.setProgress(100);

                if(((received >> 1)& 1) == 0)
                    prgrsbarBackward.setProgress(0);
                else
                    prgrsbarBackward.setProgress(100);
            }
            else
            {
                Log.e(TAG, "-1 received");
            }
        }
        catch (IOException e)
        {
            Log.e(TAG, "inStream.read() error. " + e.getMessage());
        }
    }

}
