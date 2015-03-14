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

    public void run()
    {
        try
        {
            Thread.sleep(10);
            int received = inStream.read();
            if(received != -1)
            {
                int steeringAngle = (received & 0xEF) >> 2;
                if(((received >> 4) & 1) == 0)
                {
                    prgrsbarLeft.setProgress((steeringAngle/MainActivity.STEERINGOFFSET)*100);
                    prgrsbarRight.setProgress(0);
                }
                else
                {
                    prgrsbarRight.setProgress((steeringAngle/MainActivity.STEERINGOFFSET)*100);
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
        catch (InterruptedException e)
        {
            Log.e(TAG, "Thread sleep error. " + e.getMessage());
        }
    }

}
