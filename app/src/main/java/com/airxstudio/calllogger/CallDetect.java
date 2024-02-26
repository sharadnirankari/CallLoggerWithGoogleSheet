package com.airxstudio.calllogger;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CallDetect extends BroadcastReceiver {

    private static String lastPhoneState = "";
    private static String lastPhoneNumber = "";
    private static long callRingingTime = 0;
    private static long callStartTime = 0;
    private static long outgoingCallEndedTime = 0;

    public static String[] getPhoneNumbers(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        if (telephonyManager == null || ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
            List<SubscriptionInfo> subscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();

            if (subscriptionInfoList != null) {
                String[] phoneNumbers = new String[subscriptionInfoList.size()];

                for (int i = 0; i < subscriptionInfoList.size(); i++) {
                    SubscriptionInfo subscriptionInfo = subscriptionInfoList.get(i);
                    phoneNumbers[i] = subscriptionInfo.getNumber();
                }
                return phoneNumbers;
            }
        }
        return null;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();

        if (action != null && action.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
            String phoneState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            String phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

            if (phoneNumber != null) {
                if (phoneState != null && !phoneState.equals(lastPhoneState)) {
                    lastPhoneState = phoneState;

                    if (phoneState.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                        callRingingTime = System.currentTimeMillis();
                        Log.d("PhoneStateReceiver", "Ringing " + phoneNumber);
                    } else if (phoneState.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                        callStartTime = System.currentTimeMillis();
                        Log.d("PhoneStateReceiver", "OFF HOCK");
                    } else if (phoneState.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                        long callEndTime = System.currentTimeMillis();
                        long ringDuration = callRingingTime == 0 ? 0 : (callEndTime - callRingingTime) -(callEndTime - callStartTime);
                        long callDuration = callStartTime == 0 ? 0 : (callEndTime - callStartTime);
                        logCallDetailsLocally(phoneNumber, ringDuration, callDuration, context);

                        phoneNumber = "";
                        callRingingTime = 0;
                        callStartTime = 0;

                        Log.d("PhoneStateReceiver", "Idle");
                    }
                }
            }
        }
    }

    private void logCallDetailsLocally(String phoneNumber, long ringDuration, long callDuration, Context context) {
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String dateTimeStr = dateTimeFormat.format(new Date());

        Date parsedDateTime;
        try {
            parsedDateTime = dateTimeFormat.parse(dateTimeStr);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

        String date = dateFormat.format(parsedDateTime);
        String time = timeFormat.format(parsedDateTime);

        String callType;
        if (ringDuration > 0 && callDuration == 0) {
            callType = "Missed";
        } else if (ringDuration > 0 && callDuration > 0) {
            callType = "Incoming";
        } else if (ringDuration == 0 && callDuration > 0) {
            callType = "Outgoing";
        } else {
            callType = "Missed";
        }

        long ringDurationInSeconds = ringDuration / 1000;
        long callDurationInSeconds = callDuration / 1000;

        // Save call details locally
        saveCallLogLocally(context, date, time, callType, phoneNumber, ringDurationInSeconds, callDurationInSeconds);

        // Check for internet availability
        if (isInternetAvailable(context)) {
            // If internet is available, upload all locally saved call log data
            uploadLocalCallLogs(context);
        }
    }

    private void saveCallLogLocally(Context context, String date, String time, String callType, String phoneNumber, long ringDuration, long callDuration) {
        // Save the call log locally using SharedPreferences or a local database
        SharedPreferences preferences = context.getSharedPreferences("CallLogPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        // Incremental index to uniquely identify each call log entry
        int index = preferences.getInt("index", 0);

        // Save the call log entry
        editor.putString("date_" + index, date);
        editor.putString("time_" + index, time);
        editor.putString("type_" + index, callType);
        editor.putString("number_" + index, phoneNumber);
        editor.putLong("ringDuration_" + index, ringDuration);
        editor.putLong("callDuration_" + index, callDuration);

        // Increment the index for the next call log entry
        editor.putInt("index", index + 1);

        editor.apply();
    }

    private void uploadLocalCallLogs(Context context) {
        List<CallLogEntry> localCallLogs = getLocallySavedCallLogs(context);

        if (localCallLogs != null && localCallLogs.size() > 0) {
            // Iterate through local call logs and upload each entry
            for (CallLogEntry entry : localCallLogs) {
                // Upload the entry to the API using Volley
                uploadCallLogToApi(context, entry);
            }

            // After successful synchronization, clear the local call log
            clearLocallySavedCallLogs(context);
        }
    }

    private List<CallLogEntry> getLocallySavedCallLogs(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("CallLogPrefs", Context.MODE_PRIVATE);
        int index = preferences.getInt("index", 0);

        List<CallLogEntry> localCallLogs = new ArrayList<>();

        for (int i = 0; i < index; i++) {
            String date = preferences.getString("date_" + i, "");
            String time = preferences.getString("time_" + i, "");
            String type = preferences.getString("type_" + i, "");
            String number = preferences.getString("number_" + i, "");
            long ringDuration = preferences.getLong("ringDuration_" + i, 0);
            long callDuration = preferences.getLong("callDuration_" + i, 0);

            localCallLogs.add(new CallLogEntry(date, time, type, number, ringDuration, callDuration));
        }

        return localCallLogs;
    }

    private void clearLocallySavedCallLogs(Context context) {
        // Clear the locally saved call logs using SharedPreferences or a local database
        SharedPreferences preferences = context.getSharedPreferences("CallLogPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        // Reset the index to 0 and remove all entries
        editor.putInt("index", 0);
        editor.clear();
        editor.apply();
    }

    private void uploadCallLogToApi(Context context, CallLogEntry entry) {
        String scriptUrl = "https://script.google.com/macros/s/AKfycbyj39qpBmC7aH-kF6HZcQVjeEOS3tW0NafWcCghtxVMQ-tNgIy1OCMo5Bi4i3Nev7fvtQ/exec?";
        String url = scriptUrl +
                "action=create" +
                "&date=" + entry.getDate() +
                "&time=" + entry.getTime() +
                "&type=" + entry.getCallType() +
                "&deviceNumber=" + cleanPhoneNumber(Arrays.toString(getPhoneNumbers(context))) +
                "&clientNumber=" + cleanPhoneNumber(entry.getPhoneNumber()) +
                "&ringDuration=" + entry.getRingDuration() + "s" +
                "&callDuration=" + entry.getCallDuration() + "s";

        // Initialize a StringRequest
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Handle the API response if needed
                        Log.d("VolleyResponse", response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Handle the error if needed
                        Log.e("VolleyError", error.toString());
                    }
                });
        RequestQueue queue = Volley.newRequestQueue(context);
        queue.add(stringRequest);
    }

    private boolean isInternetAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                return networkInfo != null && networkInfo.isConnected();
            } else {
                NetworkInfo wifiNetwork = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                NetworkInfo mobileNetwork = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
                return (wifiNetwork != null && wifiNetwork.isConnected()) || (mobileNetwork != null && mobileNetwork.isConnected());
            }
        }
        return false;
    }

    public String cleanPhoneNumber(String phoneNumber) {
        String cleanedNumber = phoneNumber.replaceAll("[^0-9+]", "");
        String withoutPlus = cleanedNumber.startsWith("+") ? cleanedNumber.substring(1) : cleanedNumber;
        String withoutLeadingZero = withoutPlus.startsWith("0") ? withoutPlus.substring(1) : withoutPlus;
        String withCountryCode = withoutLeadingZero.startsWith("91") ? withoutLeadingZero : "91" + withoutLeadingZero;
        if (withCountryCode.length() < 12) {
            withCountryCode = "91" + withCountryCode;
        }
        return withCountryCode;
    }
}
