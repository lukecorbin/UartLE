/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cmd.blechat;


import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;

import java.util.List;

import java.util.ArrayList;
import java.util.concurrent.RunnableFuture;

//import java.lang.annotation.Annotation;
//import android.support.annotation.RequiresApi;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class DeviceScanActivity extends ListActivity implements SwipeRefreshLayout.OnRefreshListener {

    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    //private Runnable RunScanTimeout;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private ScanCallback mScanCallback;
    private ListView    ListViewVar;
    private String NordicUARTService = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
    private String CypressUARTService = "0003cdd0-0000-1000-8000-00805f9b0131";

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    SwipeRefreshLayout mySwipeRefreshLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setTitle("Scanning BLE Devices");
        mHandler = new Handler();


        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (Build.VERSION.SDK_INT >= 21) {
            mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
            settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            filters = new ArrayList<ScanFilter>();
        }

        setContentView(R.layout.list_ble);
        //SwipeRefreshLayout mySwipeRefreshLayout = new SwipeRefreshLayout(this)
        mySwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeblelist);
        mySwipeRefreshLayout.setOnRefreshListener(this);

//        mySwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener()
//        {
//            @Override
//            public void onRefresh()
//            {
//                // do nothing
//            }
//        });

        ListViewVar = (ListView)findViewById(android.R.id.list);
    }


    @Override public void onRefresh() {

        scanLeDevice(false);
        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        //setListAdapter(mLeDeviceListAdapter);
        ListViewVar.setAdapter(mLeDeviceListAdapter);
        //mLeDeviceListAdapter.clear();
        //mLeDeviceListAdapter.notifyDataSetChanged();

        new Handler().postDelayed(new Runnable() {
            @Override public void run() {

                scanLeDevice(true);
                invalidateOptionsMenu();
                mySwipeRefreshLayout.setRefreshing(false);

            }
        }, 500);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        ActionBar bar = getActionBar();
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);

//                findViewById(R.id.menu_scan).setVisibility(View.VISIBLE);
//                menu.findItem(R.id.menu_scan).setVisible(false);
//                menu.findItem(R.id.menu_refresh).setActionView(
//                        R.layout.actionbar_indeterminate_progress);

                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
        }
        invalidateOptionsMenu();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        //setListAdapter(mLeDeviceListAdapter);
        ListViewVar.setAdapter(mLeDeviceListAdapter);
        //scanLeDevice(true);
        invalidateOptionsMenu();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
    }



    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        if (device == null) return;
//        final Intent intent = new Intent(this, DeviceControlActivity.class);
//        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
//        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());

        final Intent intent = new Intent(this, BluetoothChatActivity.class);
        intent.putExtra(BluetoothChatActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(BluetoothChatActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());

        if (mScanning) {

            scanLeDevice(false);
            mScanning = false;
        }

        // Stop Runnable
        mHandler.removeCallbacks(RunScanTimeout);
        // Luke - This is where BluetoothChat Activity begins
        startActivity(intent);
    }

    Runnable RunScanTimeout = new Runnable() {
        @Override
        public void run () {

            mScanning = false;
            if (Build.VERSION.SDK_INT < 21) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            } else {
                //mLEScanner.stopScan(mScanCallback);
                scanLeDevice21(false);
            }
            invalidateOptionsMenu(); // refresh options

        }
    };

    private void scanLeDevice(final boolean enable) {
        //mHandler = new Handler();
        //RunScanTimeout = null;

        if (enable) {

            if (!mScanning) {

                mScanning = true;

                if (Build.VERSION.SDK_INT < 21) {
                    mBluetoothAdapter.startLeScan(mLeScanCallback);
                } else {
                    //mLEScanner.startScan(filters, settings, mScanCallback);
                    scanLeDevice21(true);
                }

                mHandler.postDelayed(RunScanTimeout,SCAN_PERIOD);
            }
        } else {
            if (Build.VERSION.SDK_INT < 21) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            } else {
                //mLEScanner.stopScan(mScanCallback);
                scanLeDevice21(false);
            }
            mScanning = false;

        }
        invalidateOptionsMenu(); // refresh options
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private ArrayList<Boolean> mUuidfound;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mUuidfound = new ArrayList<Boolean>();
            mInflator = DeviceScanActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public void addDevice(BluetoothDevice device, boolean found) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
                mUuidfound.add(found);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.devicelist, null);
                //view = mInflator.inflate(R.layout.list_ble, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.serialFound = (TextView) view.findViewById(R.id.serial_service);

                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);

            boolean fnd = mUuidfound.get(i);

            if (fnd) {
                viewHolder.serialFound.setText("Serial Service Found");
            }
            else
            {
                viewHolder.serialFound.setText("");
            }

            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);

            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {


//                    //List<ParcelUuid> myUuids = scanRecord.getScanRecord().getServiceUuids();
//
//                    scanRecord
//
//                    boolean tempbool = false;
//                    if (myUuids != null) {
//
//                        for (ParcelUuid uuid : myUuids ) {
//                            if (uuid.toString().equals(NordicUARTService) ||
//                                    uuid.toString().equals(CypressUARTService)) {
//                                tempbool = true;
//
//                            }
//                        }
//                    }
//

                    if(scanRecord.length > 0)
                    {
                        Log.i("Scan Result ", scanRecord.toString() );
                    }
                    mLeDeviceListAdapter.addDevice(device,false);
                    mLeDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }
    };


    @TargetApi(21)
    private void scanLeDevice21(final boolean enable) {

            mScanCallback = new ScanCallback() {
                @Override
                public void onScanResult ( int callbackType, ScanResult result){
                    //Log.i("callbackType", String.valueOf(callbackType));
                    //Log.i("result", result.toString());
                    final BluetoothDevice device = result.getDevice();


                    //ParcelUuid[] myUuids = result.getScanRecord().getServiceUuids();

                    List<ParcelUuid> myUuids = result.getScanRecord().getServiceUuids();

                    boolean tempbool = false;
                    if (myUuids != null) {

                        for (ParcelUuid uuid : myUuids ) {
                            if (uuid.toString().equals(NordicUARTService) ||
                                    uuid.toString().equals(CypressUARTService)) {
                                tempbool = true;

                            }
                        }
                    }

                    final boolean found = tempbool;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mLeDeviceListAdapter.addDevice(device,found);
                            mLeDeviceListAdapter.notifyDataSetChanged();
                        }
                    });


                }

                @Override
                public void onBatchScanResults (List < ScanResult > results) {
                    for (ScanResult sr : results) {
                        Log.i("ScanResult - Results", sr.toString());
                    }
                }

                @Override
                public void onScanFailed ( int errorCode){
                    Log.e("Scan Failed", "Error Code: " + errorCode);
                }


        };

        //final BluetoothLeScanner bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        if (enable) {
//            mHandler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    mLEScanner.stopScan(mScanCallback);
//                }
//            }, SCAN_PERIOD);

            mLEScanner.startScan(filters, settings, mScanCallback);

        } else {

            mLEScanner.stopScan(mScanCallback);
        }

    }

    void ParseScanData ( BluetoothDevice MyBtDevice)
    {

//        ListViewVar.add
//
//        mySwipeRefreshLayout.setEnabled(true);

    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView serialFound;
    }
}