package com.airxstudio.calllogger;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CallDetect extends BroadcastReceiver {

    private static String lastPhoneState = "";
    private static String lastPhoneNumber = "";
    private static long callRingingTime = 0;
    private static long callStartTime = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
                        != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, handle accordingly or request permission
            Log.d("CallLogReceiver", "READ_PHONE_STATE permission not granted");
            return;
        }

        String action = intent.getAction();

        if (action != null && action.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
            String phoneState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            String phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

            if (phoneNumber != null) {


                if (phoneState != null && !phoneState.equals(lastPhoneState)) {
                    // Only log when the phone state changes
                    lastPhoneState = phoneState;

                    if (phoneState.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                        // Phone is ringing
                        callRingingTime = System.currentTimeMillis();
                        Log.d("PhoneStateReceiver", "Ringing " + phoneNumber);
                    } else if (phoneState.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                        // Call answered or dialing
                        callStartTime = System.currentTimeMillis();
                        Log.d("PhoneStateReceiver", "Offhook");
                    } else if (phoneState.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                        // Call ended
                        long callEndTime = System.currentTimeMillis();
                        long ringDuration = callRingingTime == 0 ? 0 : (callEndTime - callRingingTime);
                        long callDuration = callStartTime == 0 ? 0 : (callEndTime - callStartTime);

                        // Log the details
                        logCallDetails(phoneNumber, ringDuration, callDuration, context);

                        // Reset variables for the next call
                        phoneNumber = "";
                        callRingingTime = 0;
                        callStartTime = 0;

                        Log.d("PhoneStateReceiver", "Idle");
                    }
                }
            }
        }


    }

    private void logCallDetails(String phoneNumber, long ringDuration, long callDuration, Context context) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String date = dateFormat.format(new Date());

        // Determine call type
        String callType;
        if (ringDuration > 0 && callDuration == 0) {
            callType = "Missed";
        } else if (ringDuration > 0 && callDuration > 0) {
            callType = "Incoming";
        } else if (ringDuration == 0 && callDuration > 0) {
            callType = "Outgoing";
        } else {
            // Default to missed if the type is not clear
            callType = "Missed";
        }
        long ringDurationInSeconds = ringDuration / 1000;
        long callDurationInSeconds = callDuration / 1000;

        long hoursRing = ringDurationInSeconds / 3600;
        long minutesRing = (ringDurationInSeconds % 3600) / 60;
        long secondsRing = ringDurationInSeconds % 60;

        long hoursCall = callDurationInSeconds / 3600;
        long minutesCall = (callDurationInSeconds % 3600) / 60;
        long secondsCall = callDurationInSeconds % 60;

        // Log the call details
//        Log.d("CallDetails", "Date: " + date);
//        Log.d("CallDetails", "Type: " + callType);
//        Log.d("CallDetails", "Device Number: " + getDevicePhoneNumber(context));
//        Log.d("CallDetails", "Client Number: " + phoneNumber);
//        Log.d("CallDetails", "Ring Duration: " + hoursRing + "h " + minutesRing + "m " + secondsRing + "s");
//        Log.d("CallDetails", "Call Duration: " + hoursCall + "h " + minutesCall + "m " + secondsCall + "s");

        String url = "https://script.google.com/macros/s/AKfycbyj39qpBmC7aH-kF6HZcQVjeEOS3tW0NafWcCghtxVMQ-tNgIy1OCMo5Bi4i3Nev7fvtQ/exec?";
        url = url + "action=create" +
                "&date=" + date +
                "&time=" + date +
                "&type=" + callType +
                "&deviceNumber=" + getDevicePhoneNumber(context) +
                "&clientNumber=" + phoneNumber +
                "&ringDuration=" + hoursRing + "h " + minutesRing + "m " + secondsRing + "s" +
                "&callDuration=" + hoursCall + "h " + minutesCall + "m " + secondsCall + "s";

        StringRequest stringRequest;
        stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Toast.makeText(context, response, Toast.LENGTH_SHORT).show();
                        Log.e("ERRORVOLLEY", response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(context, error.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("ERRORVOLLEY", error.getMessage());
                    }
                });

        RequestQueue queue = Volley.newRequestQueue(context);
        queue.add(stringRequest);
        // You can further process the call details as needed (e.g., store in a database, send to a server, etc.)
    }

    private String getDevicePhoneNumber(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager != null) {

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                return null;
            }
        }
        return telephonyManager.getLine1Number();
    }

}
