package com.solarcar;

import android.util.Log;
import android.widget.Switch;
import android.widget.TextView;

public class GuiUpdateHandler implements Runnable
{
    private final String[] Speeds = { "-7", "-6", "-5", "-4", "-3", "-2", "-1",
                                      "0",
                                      "1", "2", "3", "4", "5", "6", "7" };
    private final String[] Angles = { "-7", "-6", "-5", "-4", "-3", "-2", "-1",
                                      "0",
                                      "1", "2", "3", "4", "5", "6", "7" };
    private final String TAG = "GuiUpdateHandler";
    private TextView txtSpeed, txtAngle;
    private int received;

    public GuiUpdateHandler(TextView Speed, TextView Angle, int Received)
    {
        txtSpeed = Speed;
        txtAngle = Angle;
        received = Received;
    }

    @Override
    public void run()
    {
        Log.e(TAG,"Updating GUI");
        txtSpeed.setText(Speeds[received & 0x0F]);
        txtAngle.setText(Angles[received >> 4]);
    }
}