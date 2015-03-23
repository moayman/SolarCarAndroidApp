package com.solarcar;

import android.util.Log;
import android.widget.Switch;
import android.widget.TextView;

public class GuiUpdateHandler implements Runnable
{

    private final String TAG = "GuiUpdateHandler";
    private Switch switchBackward;
    private TextView txtLeft, txtRight;
    private int received;

    public GuiUpdateHandler(TextView Left, TextView Right, Switch Backward, int Received)
    {
        switchBackward = Backward;
        txtLeft = Left;
        txtRight = Right;
        received = Received;
    }

    @Override
    public void run()
    {
        switchBackward.setChecked(((received >> 4) & 1) == 1);
        txtLeft.setText(String.valueOf((received & 0x0C) >> 2));
        txtRight.setText(String.valueOf(received & 0x03));
    }
}