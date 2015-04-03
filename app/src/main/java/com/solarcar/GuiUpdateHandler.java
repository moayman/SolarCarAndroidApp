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
        Log.e(TAG,"updating");
        switchBackward.setChecked(((received >> 7) & 1) == 1);
        txtLeft.setText(String.valueOf((received & 0x38) >> 3));
        txtRight.setText(String.valueOf(received & 0x07));
    }
}