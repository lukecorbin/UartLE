package com.cmd.blechat;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothGatt.CONNECTION_PRIORITY_HIGH;
import static android.bluetooth.BluetoothGattService.SERVICE_TYPE_PRIMARY;
import static android.bluetooth.BluetoothGattService.SERVICE_TYPE_SECONDARY;

/**
 * Created by Luke.Corbin on 8/3/2016.
 */
public class BluetoothChatActivity extends Activity {
    private final static String TAG = BluetoothChatActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private FileDialog fileDialog;
    private TextView mConnectionState;
    private TextView mDataField;
    private String mDeviceName;
    private String mDeviceAddress;
    private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private int bleMTU = 20;

    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private BluetoothGattCharacteristic btcharTX_DATA_IN;
    private BluetoothGattCharacteristic btcharRX_DATA_OUT;

    private String NordicUARTService = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
    private String CypressUARTService = "0003cdd0-0000-1000-8000-00805f9b0131";

//    private BluetoothGattCharacteristic btcharTX = new BluetoothGattCharacteristic(
//            UUID.fromString("0003CDD2-0000-1000-8000-00805f9b0131"),
//            BluetoothGattCharacteristic.PERMISSION_WRITE,
//            BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE);

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };








    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                Spannable word = new SpannableString("--GATT Connected--\r\n");
                StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);
                word.setSpan(boldSpan, 0, word.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                //word.setSpan(new ForegroundColorSpan(Color.CYAN), 0, word.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                ((TextView)findViewById(R.id.txtRxChatWindow)).append(word);

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                //updateConnectionState(R.string.disconnected);
                //invalidateOptionsMenu();
                //clearUI();
                ExittoScan();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {

                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());

                if (btcharTX_DATA_IN != null && btcharRX_DATA_OUT != null) {
                    mBluetoothLeService.setCharacteristicNotification(btcharTX_DATA_IN, true);

                    findViewById(R.id.txtRxChatWindow).setEnabled(true);


//                    Context mycontext = getApplicationContext();
//                    CharSequence text = "Found Serial Application";
//                    Toast toast = Toast.makeText(context, text,  Toast.LENGTH_LONG);
//                    toast.show();

                    Spannable word = new SpannableString("**Application: Serial Communication Started!**\r\n");
                    StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);
                    word.setSpan(boldSpan, 0, word.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    //word.setSpan(new ForegroundColorSpan(Color.CYAN), 0, word.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    ((TextView)findViewById(R.id.txtRxChatWindow)).append(word);

//                    try
//                    {
//                        Thread.sleep(50,0);
//                    }
//                    catch( Exception e)
//                    {
//
//                    }
//                    if(mBluetoothLeService.requestMtu(75));

//                    try
//                    {
//                        Thread.sleep(100,0);
//                    }
//                    catch( Exception e)
//                    {
//
//                    }
//                    mBluetoothLeService.requestConnectionPriority(CONNECTION_PRIORITY_HIGH);

                    findViewById(R.id.txtTxChatWindows).setEnabled(true);
                    findViewById(R.id.txtTxChatWindows).setActivated(false);
                }
                else {

                    if (mGattCharacteristics.isEmpty()) {

                        Context mycontext = getApplicationContext();
                        CharSequence text = "Did not find any GATT Characteristics";
                        Toast toast = Toast.makeText(context, text,  Toast.LENGTH_LONG);
                        Log.e(TAG, "Woops!  We did not find the Cypress BLE Chat Characteristics! Perhaps you connected to the wrong device?");
                        toast.show();
                        ExittoScan();

                    } else
                    {
                        CharSequence text = "Woops!  We did not find the Cypress BLE Chat Characteristics! Perhaps you connected to the wrong device?";
                        Log.e(TAG, "Woops!  We did not find the Cypress BLE Chat Characteristics! Perhaps you connected to the wrong device?");
                        Toast toast = Toast.makeText(context, text,  Toast.LENGTH_LONG);
                        toast.show();
                        ExittoScan();

                    }
                }

                //
                // check if we found the chat characteristics


            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {

                // We got data, send it to the receive window

                //String myStr = new String(btcharTX_DATA_IN.getValue());
                //displayData(0, myStr );
                displayData(0,intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
            else if (BluetoothLeService.ACTION_DATA_MTUCHANGED.equals(action)) {
                int mymtu;
                mymtu = intent.getIntExtra(BluetoothLeService.EXTRA_MTU,21);
                bleMTU = mymtu - 3;

                Spannable word = new SpannableString("- MTU Set to:" + Integer.toString(bleMTU) + "\r\n" );
                StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);
                word.setSpan(boldSpan, 0, word.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                //word.setSpan(new ForegroundColorSpan(Color.CYAN), 0, word.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                ((TextView)findViewById(R.id.txtRxChatWindow)).append(word);

            }

        }
    };

    private void displayData(String data) {
        if (data != null) {
            //mDataField.setText(data);
            ((TextView)findViewById(R.id.txtRxChatWindow)).setText(data);
        }
    }

    private void ExittoScan() {

        onBackPressed();
    }

    // dir=0, receive data
    // dir=1, send data
    private void displayData(int dir, String data) {
        if (data != null) {


            if (data.length() <= 2 )
            {
                if(data.equals("\n"))
                {
                    data = "";
                    return;
                }
            }

            // append newline
            if(!data.endsWith("\n"))
            {
                data += "\n";
            }


            if (dir==0)
            {

                Spannable word = new SpannableString("Ble:" + data);

                word.setSpan(new ForegroundColorSpan(Color.BLUE), 0, word.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                ((TextView)findViewById(R.id.txtRxChatWindow)).append(word);
            }
            else
            {
                Spannable word = new SpannableString("You:" + data);

                word.setSpan(new ForegroundColorSpan(Color.RED), 0, word.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                ((TextView)findViewById(R.id.txtRxChatWindow)).append(word);

            }
        }
    }


    private void clearUI() {
        //mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
       // mDataField.setText(R.string.no_data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set context of my chat window
        //setContentView(R.layout.gatt_services_characteristics);
        setContentView(R.layout.chatwindow);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

//        // Sets up UI references.
//        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
//        mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
//        mGattServicesList.setOnChildClickListener(servicesListClickListner);
//        mConnectionState = (TextView) findViewById(R.id.connection_state);
//        mDataField = (TextView) findViewById(R.id.data_value);

         //getActionBar().setDisplayShowTitleEnabled(false);
         getActionBar().setTitle(mDeviceName);
         getActionBar().setDisplayHomeAsUpEnabled(true);

        TextView tv = (TextView) findViewById(R.id.txtRxChatWindow);
        tv.setMovementMethod(new ScrollingMovementMethod());

        findViewById(R.id.txtRxChatWindow).setVerticalScrollBarEnabled(true);

        /* Disable Chat and Tx windows until connected */
        findViewById(R.id.txtRxChatWindow).setEnabled(false);
        findViewById(R.id.txtTxChatWindows).setEnabled(false);
        findViewById(R.id.txtTxChatWindows).setActivated(false);

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);


        String myroot = Environment.getRootDirectory().toString();
        String mydata = Environment.getDataDirectory().getAbsolutePath().toString();
        mydata =  Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
        mydata =Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();

        File mPath = new File(mydata );
        if ( !mPath.exists() )
        {
            mydata = Environment.getRootDirectory().toString();
        }

        fileDialog = new FileDialog(this, mPath, ".txt");
        fileDialog.addFileListener(new FileDialog.FileSelectedListener() {
            public void fileSelected(File file) {
                Log.d(getClass().getName(), "selected file " + file.toString());

                try {

                    FileReader reader = new FileReader(file);
                    BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
                    final long bytes = file.length();

                    String receiveString = "";
                    StringBuilder stringBuilder = new StringBuilder();

                    final long startTime = System.currentTimeMillis();
                    String line;
                    int mtu = bleMTU;
                    char[] mybuff = new char[mtu];


                    while ( (line = bufferedReader.readLine()) != null)  {
                        stringBuilder.append(line);
                    }
                    bufferedReader.close();

                    int offset;
                    for(offset = 0;(offset+bleMTU) < stringBuilder.length();offset+=bleMTU) {
                        String SendStr = stringBuilder.substring(offset,offset+bleMTU);
                        mBluetoothLeService.writeValue(SendStr,btcharRX_DATA_OUT);
                    }
                    // get last nibble
                    if(offset < stringBuilder.length() )
                    {
                        String SendStr = stringBuilder.substring(offset,stringBuilder.length());
                        mBluetoothLeService.writeValue(SendStr,btcharRX_DATA_OUT);
                    }

                    final Handler fileTimeHandler = new Handler();

                    fileTimeHandler.postDelayed(new Runnable() {
                        @Override public void run() {

                            if (mBluetoothLeService.isQueueEmpty() == false)
                            {
                                fileTimeHandler.postDelayed(this, 10);
                            }
                            else
                            {
                                long millis = System.currentTimeMillis() - startTime;
                                int seconds = (int) (millis / 1000);
                                int minutes = seconds / 60;
                                seconds = seconds % 60;
                                String timestring = String.format("%d:%02d", minutes, seconds);

                                double bytespersec = ((float) bytes/ (float) millis) ;

                                TextView myView = (TextView)findViewById(R.id.textRate);
                                myView.setText(  String.format("%.2f", bytespersec) + " KB/s");

                            }

                        }
                    }, 10);



                }
                catch (Exception e)
                {
                    return;
                }
            }
        });
        fileDialog.addDirectoryListener(new FileDialog.DirectorySelectedListener() {
          public void directorySelected(File directory) {
              Log.d(getClass().getName(), "selected dir " + directory.toString());
          }
        });
        fileDialog.setSelectDirectoryOption(false);


        EditText editText = (EditText) findViewById(R.id.txtTxChatWindows);
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                    if (actionId == EditorInfo.IME_ACTION_DONE ||
                            actionId == EditorInfo.IME_ACTION_SEND  ) {
                        SendBluetoothText();

                    }

                return true;
            }
        });

        Button button = (Button)findViewById(R.id.btn_sendfile);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Do something in response to button click


                fileDialog.showDialog();


            }
        });

//        ImageButton sendbutton = (ImageButton)findViewById(R.id.btnSend);
//        sendbutton.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                // Do something in response to button click
//
//                SendBluetoothText();
//
//
//            }
//        });




    }


    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }

//        unregisterReceiver(mGattUpdateReceiver);
//        unbindService(mServiceConnection);
//        mBluetoothLeService = null;
//        onBackPressed();
        //ExittoScan();

    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(mGattUpdateReceiver);

//        unbindService(mServiceConnection);
//        mBluetoothLeService = null;
//        onBackPressed();
        //ExittoScan();
    }

    @Override
    protected void onStop() {
        super.onPause();

        //unregisterReceiver(mGattUpdateReceiver);

//        unbindService(mServiceConnection);
//        mBluetoothLeService = null;
//        onBackPressed();
        //ExittoScan();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

         if(mConnected)
        {
            mBluetoothLeService.disconnect();
        }

        unbindService(mServiceConnection);

        mBluetoothLeService = null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                ExittoScan();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //mConnectionState.setText(resourceId);
            }
        });
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_MTUCHANGED);
        return intentFilter;
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        //String uuid = null;
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();


        // See if we have nordic
        //BluetoothGattService nordicService = new BluetoothGattService(UUID.fromString(NordicUARTService),SERVICE_TYPE_PRIMARY);

        for (BluetoothGattService gattService : gattServices) {

            if (gattService.getUuid().toString().equals(NordicUARTService)) {

                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();

                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    //                /* Find Nordic Data */
                    if (gattCharacteristic.getUuid().toString().equals("6e400003-b5a3-f393-e0a9-e50e24dcca9e")) // TX_Data_IN
                    {
                        btcharTX_DATA_IN = (gattCharacteristic);
//                    btcharTX_DATA_IN = new BluetoothGattCharacteristic(
//                            gattCharacteristic.getUuid(),
//                            gattCharacteristic.getProperties(),
//                            gattCharacteristic.getPermissions());

                        //mBluetoothLeService.setCharacteristicNotification(gattCharacteristic,true);
                    } else if (gattCharacteristic.getUuid().toString().equals("6e400002-b5a3-f393-e0a9-e50e24dcca9e")) // RX_Data_OUT
                    {
                        btcharRX_DATA_OUT = (gattCharacteristic);
                    }
                }

            }

            if (gattService.getUuid().toString().equals(CypressUARTService)) {

                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();

                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    //                /* Find Nordic Data */
                    if (gattCharacteristic.getUuid().toString().equals("0003cdd1-0000-1000-8000-00805f9b0131")) // TX_Data_IN
                    {
                        btcharTX_DATA_IN = (gattCharacteristic);
//                    btcharTX_DATA_IN = new BluetoothGattCharacteristic(
//                            gattCharacteristic.getUuid(),
//                            gattCharacteristic.getProperties(),
//                            gattCharacteristic.getPermissions());

                        //mBluetoothLeService.setCharacteristicNotification(gattCharacteristic,true);
                    } else if (gattCharacteristic.getUuid().toString().equals("0003cdd2-0000-1000-8000-00805f9b0131")) // RX_Data_OUT
                    {
                        btcharRX_DATA_OUT = (gattCharacteristic);
                    }
                }

            }
        }

//        // Loops through available GATT Services.
//        for (BluetoothGattService gattService : gattServices) {
//            HashMap<String, String> currentServiceData = new HashMap<String, String>();
//            uuid = gattService.getUuid().toString();
//            currentServiceData.put(
//                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
//            currentServiceData.put(LIST_UUID, uuid);
//            gattServiceData.add(currentServiceData);
//
//            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
//                    new ArrayList<HashMap<String, String>>();
//            List<BluetoothGattCharacteristic> gattCharacteristics =
//                    gattService.getCharacteristics();
//            ArrayList<BluetoothGattCharacteristic> charas =
//                    new ArrayList<BluetoothGattCharacteristic>();
//
//            // Loops through available Characteristics.
//            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
//                charas.add(gattCharacteristic);
//                HashMap<String, String> currentCharaData = new HashMap<String, String>();
//                uuid = gattCharacteristic.getUuid().toString();
//                //uuid = uuid.toUpperCase();
//
//                /* Find Cypress Data */
//                if(uuid.equals("0003cdd1-0000-1000-8000-00805f9b0131")) // TX_Data_IN
//                {
//                    btcharTX_DATA_IN = (gattCharacteristic);
////                    btcharTX_DATA_IN = new BluetoothGattCharacteristic(
////                            gattCharacteristic.getUuid(),
////                            gattCharacteristic.getProperties(),
////                            gattCharacteristic.getPermissions());
//
//                    //mBluetoothLeService.setCharacteristicNotification(gattCharacteristic,true);
//                }
//                else if(uuid.equals("0003cdd2-0000-1000-8000-00805f9b0131")) // RX_Data_OUT
//                {
//                    btcharRX_DATA_OUT = (gattCharacteristic);
//                }
//
//                /* Find Nordic Data */
//                if(uuid.equals("6e400003-b5a3-f393-e0a9-e50e24dcca9e")) // TX_Data_IN
//                {
//                    btcharTX_DATA_IN = (gattCharacteristic);
////                    btcharTX_DATA_IN = new BluetoothGattCharacteristic(
////                            gattCharacteristic.getUuid(),
////                            gattCharacteristic.getProperties(),
////                            gattCharacteristic.getPermissions());
//
//                    //mBluetoothLeService.setCharacteristicNotification(gattCharacteristic,true);
//                }
//                else if(uuid.equals("6e400002-b5a3-f393-e0a9-e50e24dcca9e")) // RX_Data_OUT
//                {
//                    btcharRX_DATA_OUT = (gattCharacteristic);
//                }
//
//                currentCharaData.put(
//                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
//                currentCharaData.put(LIST_UUID, uuid);
//                gattCharacteristicGroupData.add(currentCharaData);
//            }
//            mGattCharacteristics.add(charas);
//            gattCharacteristicData.add(gattCharacteristicGroupData);
//        }
    }

    void SendBluetoothText()
    {


        TextView v = (TextView)findViewById(R.id.txtTxChatWindows);
        String mystr = v.getText().toString();
        mystr = mystr + "\r\n";

        v.setText(""); // clear text


        displayData(1, mystr );

//                    byte[] data = mystr.getBytes();
//
//                    btcharRX_DATA_OUT.setValue(data);
//                        mBluetoothLeService.writeCharacteristic(btcharRX_DATA_OUT);
//                    handled =   true;

        int MTU = bleMTU;

        if ( btcharRX_DATA_OUT != null && mConnected ) {

            // We probably need to buffer this to cap at MTU size
            // we could use - Wait for onCharacteristicWrite()
            if (mystr.length() > MTU)
            {
                String shortstr;
                int start = 0;
                int end = mystr.length();
                int loops = 0;
                boolean success;

                int idx = 0;
                while ( mystr.length() - start > MTU )
                {
                    shortstr = mystr.substring(start,start+MTU);
                    mBluetoothLeService.writeValue(shortstr,btcharRX_DATA_OUT);
//                                btcharRX_DATA_OUT.setValue(shortstr.getBytes());
//                                btcharRX_DATA_OUT.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
//                                //btcharRX_DATA_OUT.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
//                                success = mBluetoothLeService.writeCharacteristic(btcharRX_DATA_OUT);
//                                while((mBluetoothLeService.LastWriteCompleted !=
//                                        BluetoothLeService.lastwritecompleted.WRITE_COMPLETED) && loops < 2 )
//                                {
//                                    try {
//                                        Thread.sleep(300);
//                                        // Do some stuff
//                                    } catch (Exception e) {
//                                        e.getLocalizedMessage();
//                                    }
//                                    loops++;
//                                }
//                                if ( !success || loops >= 2 )
//                                {
//                                    Context context = getApplicationContext();
//                                    CharSequence text = "Writing Long data failed after " + idx + " tries";
//                                    Log.e(TAG,"Writing Long data failed after " + idx + " tries," +
//                                    "LastWrite:" + mBluetoothLeService.LastWriteCompleted +
//                                    " success: " + success);
//                                    Toast toast = Toast.makeText(context, text,  Toast.LENGTH_LONG);
//                                    toast.show();
//                                    ExittoScan();
                    //}
                    start = start + MTU;
//                                idx++;
                }
                // send last chunk
                if ( (mystr.length() - start) > 0 )
                {
                    //SystemClock.sleep(200);
                    shortstr = mystr.substring(start,mystr.length());
                    mBluetoothLeService.writeValue(shortstr,btcharRX_DATA_OUT);
//                                btcharRX_DATA_OUT.setValue(shortstr.getBytes());
//                                btcharRX_DATA_OUT.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
//                                //btcharRX_DATA_OUT.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
//                                success = mBluetoothLeService.writeCharacteristic(btcharRX_DATA_OUT);
//                                while((mBluetoothLeService.LastWriteCompleted !=
//                                        BluetoothLeService.lastwritecompleted.WRITE_COMPLETED) && loops < 2 )
//                                {
//                                    try {
//                                        Thread.sleep(300);
//                                        // Do some stuff
//                                    } catch (Exception e) {
//                                        e.getLocalizedMessage();
//                                    }
//                                    loops++;
//                                }
//
//                                if ( !success || loops >= 2)                                {
//                                    Context context = getApplicationContext();
//                                    CharSequence text = "Writing Long data failed after " + idx + " tries on LAST WRITE";
//                                    Log.e(TAG,"Writing Long data failed after " + idx + " tries," +
//                                            "LastWrite:" + mBluetoothLeService.LastWriteCompleted +
//                                            " success: " + success);
//
//                                    Toast toast = Toast.makeText(context, text,  Toast.LENGTH_LONG);
//                                    toast.show();
//                                    ExittoScan();
//                                }
                }
            }
            else
            {
                mBluetoothLeService.writeValue(mystr,btcharRX_DATA_OUT);
//                            btcharRX_DATA_OUT.setValue(mystr);
//                            mBluetoothLeService.writeCharacteristic(btcharRX_DATA_OUT);
            }
        }

    }


}

