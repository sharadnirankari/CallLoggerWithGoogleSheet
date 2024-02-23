package com.airxstudio.calllogger;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.CallLog;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private CallLogAdapter adapter;

    ActivityResultLauncher<String[]> mPermissionResultLauncher;
    private boolean isReadPermissionGranted = false;
    private boolean isLocationPermissionGranted = false;
    private boolean isRecordPermissionGranted = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);

        mPermissionResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), new ActivityResultCallback<Map<String, Boolean>>() {
            @Override
            public void onActivityResult(Map<String, Boolean> result) {

                if (result.get(Manifest.permission.READ_PHONE_STATE) != null) {
                    isReadPermissionGranted = result.get(Manifest.permission.READ_PHONE_STATE);
                }

                if (result.get(Manifest.permission.READ_CALL_LOG) != null) {
                    isLocationPermissionGranted = result.get(Manifest.permission.READ_CALL_LOG);
                }

                if (result.get(Manifest.permission.READ_PHONE_NUMBERS) != null) {
                    isRecordPermissionGranted = result.get(Manifest.permission.READ_PHONE_NUMBERS);
                }

                if (result.get(Manifest.permission.READ_CONTACTS) != null) {
                    isReadPermissionGranted = result.get(Manifest.permission.READ_CONTACTS);
                }

            }
        });

        requestPermission();


        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        if (isReadPermissionGranted) {
            List<CallLogModel> callLogs = getCallLogs();
            adapter = new CallLogAdapter(callLogs);
            recyclerView.setAdapter(adapter);
        }



    }

    private void requestPermission(){
        isReadPermissionGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED;

        isLocationPermissionGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED;

        isRecordPermissionGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_NUMBERS
        ) == PackageManager.PERMISSION_GRANTED;

        List<String> permissionRequest = new ArrayList<>();

        if (!isReadPermissionGranted) {
            permissionRequest.add(Manifest.permission.READ_PHONE_STATE);
        }

        if (!isLocationPermissionGranted) {
            permissionRequest.add(Manifest.permission.READ_CALL_LOG);
        }

        if (!isRecordPermissionGranted) {
            permissionRequest.add(Manifest.permission.READ_PHONE_NUMBERS);
        }

        if (!isReadPermissionGranted) {
            permissionRequest.add(Manifest.permission.READ_CONTACTS);
        }

        if (!permissionRequest.isEmpty()) {
            mPermissionResultLauncher.launch(permissionRequest.toArray(new String[0]));
        }

    }
    private List<CallLogModel> getCallLogs() {
        List<CallLogModel> callLogs = new ArrayList<>();

        String[] projection = new String[]{CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.TYPE, CallLog.Calls.DATE};
        Cursor cursor = getContentResolver().query(CallLog.Calls.CONTENT_URI, projection, null, null, CallLog.Calls.DATE + " DESC");

        if (cursor != null) {
            int numberIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER);
            int nameIndex = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME);
            int typeIndex = cursor.getColumnIndex(CallLog.Calls.TYPE);
            int dateIndex = cursor.getColumnIndex(CallLog.Calls.DATE);

            while (cursor.moveToNext()) {
                String number = cursor.getString(numberIndex);
                String name = cursor.getString(nameIndex);
                String callType = getCallType(cursor.getInt(typeIndex));
                String date = cursor.getString(dateIndex);

                CallLogModel callLog = new CallLogModel();
                callLog.setNumber(number);
                callLog.setName(name);
                callLog.setCallType(callType);
                callLog.setDate(date);

                callLogs.add(callLog);
            }

            cursor.close();
        }

        return callLogs;
    }

    private String getCallType(int callType) {
        switch (callType) {
            case CallLog.Calls.INCOMING_TYPE:
                return "Incoming";
            case CallLog.Calls.OUTGOING_TYPE:
                return "Outgoing";
            case CallLog.Calls.MISSED_TYPE:
                return "Missed";
            default:
                return "Unknown";
        }
    }
}
