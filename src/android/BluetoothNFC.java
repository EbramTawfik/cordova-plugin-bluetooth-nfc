package io.fornace.BluetoothNFC;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothAdapter;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.acs.bluetooth.Acr1255uj1Reader;
import com.acs.bluetooth.Acr3901us1Reader;
import com.acs.bluetooth.BluetoothReader;
import com.acs.bluetooth.BluetoothReaderGattCallback;
import com.acs.bluetooth.BluetoothReaderManager;

import java.util.ArrayList;
import java.util.Locale;



/**
 * This class echoes a string called from JavaScript.
 */
public class BluetoothNFC extends CordovaPlugin {
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<BluetoothDevice> mLeDevices;
    private boolean bluetoothScanning;
    private Handler bluetoothHandler;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothReaderGattCallback mGattCallback;
    private BluetoothReaderManager mBluetoothReaderManager;
    private int mConnectState = BluetoothReader.STATE_DISCONNECTED;
    private BluetoothReader mBluetoothReader;
    private String readerType;

    private CallbackContext jsCallback = null;

    private static final int REQUEST_ENABLE_BT = 1;
    /* Stops scanning after 10 seconds. */
    private static final long SCAN_PERIOD = 3000;

    private static final byte[] AUTO_POLLING_START = { (byte) 0xE0, 0x00, 0x00,
            0x40, 0x01 };
    private static final byte[] AUTO_POLLING_STOP = { (byte) 0xE0, 0x00, 0x00,
            0x40, 0x00 };

    @Override
    protected void pluginInitialize() {
        super.pluginInitialize();

        bluetoothHandler = new Handler();
        mLeDevices = new ArrayList<BluetoothDevice>();

        bluetoothManager = (BluetoothManager) cordova.getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();



        /* Initialize BluetoothReaderGattCallback. */
        mGattCallback = new BluetoothReaderGattCallback();

        /* Register BluetoothReaderGattCallback's listeners */
        mGattCallback
                .setOnConnectionStateChangeListener(new BluetoothReaderGattCallback.OnConnectionStateChangeListener() {

                    @Override
                    public void onConnectionStateChange(
                            final BluetoothGatt gatt, final int state,
                            final int newState) {

                        if (state != BluetoothGatt.GATT_SUCCESS) {
                                    /*
                                     * Show the message on fail to
                                     * connect/disconnect.
                                     */
                            mConnectState = BluetoothReader.STATE_DISCONNECTED;

                            if (newState == BluetoothReader.STATE_CONNECTED) {
                                Log.v("bt", "connect fail");
                            } else if (newState == BluetoothReader.STATE_DISCONNECTED) {
                                Log.v("bt", "disconnect fail");
                            }
                            return;
                        }


                        if (newState == BluetoothProfile.STATE_CONNECTED) {
                                    /* Detect the connected reader. */
                            if (mBluetoothReaderManager != null) {
                                mBluetoothReaderManager.detectReader(
                                        gatt, mGattCallback);
                            }
                        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                            mBluetoothReader = null;
                                    /*
                                     * Release resources occupied by Bluetooth
                                     * GATT client.
                                     */
                            if (mBluetoothGatt != null) {
                                mBluetoothGatt.close();
                                mBluetoothGatt = null;
                            }
                        }
                    }
                });

        /* Initialize mBluetoothReaderManager. */
        mBluetoothReaderManager = new BluetoothReaderManager();

        /* Register BluetoothReaderManager's listeners */
        mBluetoothReaderManager
                .setOnReaderDetectionListener(new BluetoothReaderManager.OnReaderDetectionListener() {

                    @Override
                    public void onReaderDetection(BluetoothReader reader) {

                        if (reader instanceof Acr3901us1Reader) {
                            /* The connected reader is ACR3901U-S1 reader. */
                            Log.v("bt", "On Acr3901us1Reader Detected.");

                            readerType = "Acr3901us1Reader";
                        } else if (reader instanceof Acr1255uj1Reader) {
                            /* The connected reader is ACR1255U-J1 reader. */
                            Log.v("bt", "On Acr1255uj1Reader Detected.");

                            readerType = "Acr1255uj1Reader";
                        } else {
                            Log.v("bt", "On Unknown Detected.");

                            readerType = "";
                            return;
                        }

                        mBluetoothReader = reader;
                        setListener(reader);
                        activateReader(reader);
                    }
                });
    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);

//        scanLeDevice(true);
    }

    @Override
    public void onPause(boolean multitasking) {
        super.onPause(multitasking);

//        scanLeDevice(false);
    }


    private void activateReader(BluetoothReader reader) {
        if (reader == null) {
            return;
        }

        if (reader instanceof Acr3901us1Reader) {
            /* Start pairing to the reader. */
            ((Acr3901us1Reader) mBluetoothReader).startBonding();
        } else if (mBluetoothReader instanceof Acr1255uj1Reader) {
            /* Enable notification. */
            mBluetoothReader.enableNotification(true);
        }
    }

    private void setListener(BluetoothReader reader) {
        /* Update status change listener */
        if (mBluetoothReader instanceof Acr3901us1Reader) {
            ((Acr3901us1Reader) mBluetoothReader)
                    .setOnBatteryStatusChangeListener(new Acr3901us1Reader.OnBatteryStatusChangeListener() {

                        @Override
                        public void onBatteryStatusChange(
                                BluetoothReader bluetoothReader,
                                final int batteryStatus) {

                            Log.v("bt", "mBatteryStatusListener data: " + batteryStatus);

                        }

                    });
        } else if (mBluetoothReader instanceof Acr1255uj1Reader) {
            ((Acr1255uj1Reader) mBluetoothReader)
                    .setOnBatteryLevelChangeListener(new Acr1255uj1Reader.OnBatteryLevelChangeListener() {

                        @Override
                        public void onBatteryLevelChange(
                                BluetoothReader bluetoothReader,
                                final int batteryLevel) {

                            Log.v("bt", "mBatteryLevelListener data: " + batteryLevel);

                        }

                    });
        }
        mBluetoothReader
                .setOnCardStatusChangeListener(new BluetoothReader.OnCardStatusChangeListener() {

                    @Override
                    public void onCardStatusChange(
                            BluetoothReader bluetoothReader, final int sta) {



                                if (sta == BluetoothReader.CARD_STATUS_ABSENT) {
                                    Log.v("bt", "mCardStatusListener sta: absent");

                                    try {
                                        JSONObject parameter = new JSONObject();
                                        parameter.put("callback", "absent");

                                        PluginResult rs = new PluginResult(PluginResult.Status.OK, parameter);
                                        rs.setKeepCallback(true);
                                        jsCallback.sendPluginResult(rs);

                                    }catch (JSONException e)
                                    {
                                        Log.v("bt", e.getMessage());
                                    }

                                } else if (sta == BluetoothReader.CARD_STATUS_PRESENT) {
                                    Log.v("bt", "mCardStatusListener sta: present");

                                    try {
                                        JSONObject parameter = new JSONObject();
                                        parameter.put("callback", "present");

                                        PluginResult rs = new PluginResult(PluginResult.Status.OK, parameter);
                                        rs.setKeepCallback(true);
                                        jsCallback.sendPluginResult(rs);

                                    }catch (JSONException e)
                                    {
                                        Log.v("bt", e.getMessage());
                                    }

                                } else {
                                    Log.v("bt", "mCardStatusListener sta: " + sta);
                                }
                    }

                });

        /* Wait for authentication completed. */
        mBluetoothReader
                .setOnAuthenticationCompleteListener(new BluetoothReader.OnAuthenticationCompleteListener() {

                    @Override
                    public void onAuthenticationComplete(
                            BluetoothReader bluetoothReader, final int errorCode) {

                        if (errorCode == BluetoothReader.ERROR_SUCCESS) {
                            Log.v("bt", "Auth OK");

                            try {
                                JSONObject parameter = new JSONObject();

                                parameter.put("callback", "authenticate");
//                                parameter.put("reader", readerType);

                                PluginResult rs = new PluginResult(PluginResult.Status.OK, parameter);
                                rs.setKeepCallback(true);
                                jsCallback.sendPluginResult(rs);

                            } catch (JSONException e) {
                                Log.v("bt", e.toString());
                            }

                        } else {
                            Log.v("bt", "Auth Fail");
                        }
                    }

                });

        /* Wait for receiving ATR string. */
        mBluetoothReader
                .setOnAtrAvailableListener(new BluetoothReader.OnAtrAvailableListener() {

                    @Override
                    public void onAtrAvailable(BluetoothReader bluetoothReader,
                                               final byte[] atr, final int errorCode) {

                        if (atr == null) {
                            Log.v("bt", "ATR " + getErrorString(errorCode));
                        } else {
                            Log.v("bt", "ATR " + Utils.toHexString(atr));
                        }
                    }

                });

        /* Wait for power off response. */
        mBluetoothReader
                .setOnCardPowerOffCompleteListener(new BluetoothReader.OnCardPowerOffCompleteListener() {

                    @Override
                    public void onCardPowerOffComplete(
                            BluetoothReader bluetoothReader, final int result) {
                        Log.v("bt", "Poweroff " + getErrorString(result));
                    }

                });

        /* Wait for response APDU. */
        mBluetoothReader
                .setOnResponseApduAvailableListener(new BluetoothReader.OnResponseApduAvailableListener() {

                    @Override
                    public void onResponseApduAvailable(
                            BluetoothReader bluetoothReader, final byte[] apdu,
                            final int errorCode) {
                        Log.v("bt", "apdu " + getResponseString(apdu, errorCode));

                        try {
                            JSONObject parameter = new JSONObject();
                            parameter.put("callback", "apdu");
                            parameter.put("result", getResponseString(apdu, errorCode));

                            PluginResult rs = new PluginResult(PluginResult.Status.OK, parameter);
                            rs.setKeepCallback(true);
                            jsCallback.sendPluginResult(rs);

                        }catch (JSONException e)
                        {
                            Log.v("bt", e.getMessage());
                        }
                    }

                });

        /* Wait for escape command response. */
        mBluetoothReader
                .setOnEscapeResponseAvailableListener(new BluetoothReader.OnEscapeResponseAvailableListener() {

                    @Override
                    public void onEscapeResponseAvailable(
                            BluetoothReader bluetoothReader,
                            final byte[] response, final int errorCode) {
                        Log.v("bt", "escape " + getResponseString(response, errorCode));


                        try {
                            JSONObject parameter = new JSONObject();
                            parameter.put("callback", "escape");
                            parameter.put("result", getResponseString(response, errorCode));

                            PluginResult rs = new PluginResult(PluginResult.Status.OK, parameter);
                            rs.setKeepCallback(true);
                            jsCallback.sendPluginResult(rs);

                        }catch (JSONException e)
                        {
                            Log.v("bt", e.getMessage());
                        }
                    }

                });

        /* Wait for device info available. */
        mBluetoothReader
                .setOnDeviceInfoAvailableListener(new BluetoothReader.OnDeviceInfoAvailableListener() {

                    @Override
                    public void onDeviceInfoAvailable(
                            BluetoothReader bluetoothReader, final int infoId,
                            final Object o, final int status) {


                        if (status != BluetoothGatt.GATT_SUCCESS) {
                            Log.v("bt", "info failed");
                            return;
                        }
                        switch (infoId) {
                            case BluetoothReader.DEVICE_INFO_SYSTEM_ID: {
                                Log.v("bt", "id " + Utils
                                        .toHexString((byte[]) o));
                            }
                            break;
                            case BluetoothReader.DEVICE_INFO_MODEL_NUMBER_STRING:
                                Log.v("bt", "model " + (String) o);
                                break;
                            case BluetoothReader.DEVICE_INFO_SERIAL_NUMBER_STRING:
                                Log.v("bt", "sn " + (String) o);
                                break;
                            case BluetoothReader.DEVICE_INFO_FIRMWARE_REVISION_STRING:
                                Log.v("bt", "fw " + (String) o);
                                break;
                            case BluetoothReader.DEVICE_INFO_HARDWARE_REVISION_STRING:
                                Log.v("bt", "hw " + (String) o);
                                break;
                            case BluetoothReader.DEVICE_INFO_MANUFACTURER_NAME_STRING:
                                Log.v("bt", "man " + (String) o);
                                break;
                            default:
                                break;
                        }
                    }

                });

        /* Wait for battery level available. */
        if (mBluetoothReader instanceof Acr1255uj1Reader) {
            ((Acr1255uj1Reader) mBluetoothReader)
                    .setOnBatteryLevelAvailableListener(new Acr1255uj1Reader.OnBatteryLevelAvailableListener() {

                        @Override
                        public void onBatteryLevelAvailable(
                                BluetoothReader bluetoothReader,
                                final int batteryLevel, int status) {
                            Log.v("bt", "mBatteryLevelListener data: " + batteryLevel);


                        }

                    });
        }

        /* Handle on battery status available. */
        if (mBluetoothReader instanceof Acr3901us1Reader) {
            ((Acr3901us1Reader) mBluetoothReader)
                    .setOnBatteryStatusAvailableListener(new Acr3901us1Reader.OnBatteryStatusAvailableListener() {

                        @Override
                        public void onBatteryStatusAvailable(
                                BluetoothReader bluetoothReader,
                                final int batteryStatus, int status) {
                            Log.v("bt", "mBatteryLevelListener data: " + getBatteryStatusString(batteryStatus));
                        }

                    });
        }

        /* Handle on slot status available. */
        mBluetoothReader
                .setOnCardStatusAvailableListener(new BluetoothReader.OnCardStatusAvailableListener() {

                    @Override
                    public void onCardStatusAvailable(
                            BluetoothReader bluetoothReader,
                            final int cardStatus, final int errorCode) {
                        if (errorCode != BluetoothReader.ERROR_SUCCESS) {
                            Log.v("bt", "card status : " + getErrorString(errorCode));

                            try {
                                JSONObject parameter = new JSONObject();

                                parameter.put("callback", "card");
                                parameter.put("result", getErrorString(errorCode));

                                PluginResult rs = new PluginResult(PluginResult.Status.OK, parameter);
                                rs.setKeepCallback(true);
                                jsCallback.sendPluginResult(rs);

                            } catch (JSONException e) {
                                Log.v("bt", e.toString());
                            }
                        } else {
                            Log.v("bt", "card status : " + getCardStatusString(cardStatus));

                            try {
                                JSONObject parameter = new JSONObject();

                                parameter.put("callback", "card");
                                parameter.put("result", getCardStatusString(cardStatus));

                                PluginResult rs = new PluginResult(PluginResult.Status.OK, parameter);
                                rs.setKeepCallback(true);
                                jsCallback.sendPluginResult(rs);

                            } catch (JSONException e) {
                                Log.v("bt", e.toString());
                            }
                        }



                    }

                });

        mBluetoothReader
                .setOnEnableNotificationCompleteListener(new BluetoothReader.OnEnableNotificationCompleteListener() {

                    @Override
                    public void onEnableNotificationComplete(
                            BluetoothReader bluetoothReader, final int result) {
                        if (result != BluetoothGatt.GATT_SUCCESS) {
                                    /* Fail */
                            Log.v("bt", "notification : " + "The device is unable to set notification!");
                        } else {
                            Log.v("bt", "notification : " + "The device is ready to use!");

                            try {
                                JSONObject parameter = new JSONObject();

                                parameter.put("callback", "connect");
                                parameter.put("reader", readerType);

                                PluginResult rs = new PluginResult(PluginResult.Status.OK, parameter);
                                rs.setKeepCallback(true);
                                jsCallback.sendPluginResult(rs);

                            } catch (JSONException e) {
                                Log.v("bt", e.toString());
                            }
                        }

                    }

                });
    }


    private synchronized void scanLeDevice(final boolean enable) {
        if (enable) {
            /* Stops scanning after a pre-defined scan period. */
            bluetoothHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (bluetoothScanning) {
                        bluetoothScanning = false;
                        bluetoothAdapter.stopLeScan(mLeScanCallback);

                        finishLeScan();
                    }
                }
            }, SCAN_PERIOD);

            bluetoothScanning = true;
            bluetoothAdapter.startLeScan(mLeScanCallback);
        } else if (bluetoothScanning) {
            bluetoothScanning = false;
            bluetoothAdapter.stopLeScan(mLeScanCallback);

            finishLeScan();
        }
    }

    private void finishLeScan()
    {
        Log.v("bt", "finish bt scan");
        JSONObject parameter = new JSONObject();
        try {

            parameter.put("callback", "scan");

            JSONArray deviceObjs = new JSONArray();

            for(int i = 0; i < mLeDevices.size(); i++)
            {
                JSONObject deviceObj = new JSONObject();
                BluetoothDevice device = mLeDevices.get(i);
                deviceObj.put("id", device.toString());
                deviceObj.put("name", device.getName());
                deviceObj.put("address", device.getAddress());

                deviceObjs.put(deviceObj);
            }

            parameter.put("devices", deviceObjs);
        }catch(JSONException e)
        {
            Log.v("bt", e.getMessage());
        }




        PluginResult rs = new PluginResult(PluginResult.Status.OK, parameter);
        rs.setKeepCallback(true);
        jsCallback.sendPluginResult(rs);
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext _callbackContext) throws JSONException {

        if (action.equals("init")) {
            jsCallback = _callbackContext;

            mLeDevices = new ArrayList<BluetoothDevice>();
            if(mBluetoothGatt != null) mBluetoothGatt.disconnect();

            scanLeDevice(true);

            return true;
        }else if (action.equals("connect")) {

            connectReader(args.getString(0));

            return true;
        }else if (action.equals("authenticate")) {

            byte masterKey[] = Utils.hexString2Bytes(args.getString(0));

            if (masterKey != null && masterKey.length > 0) {
                if (!mBluetoothReader.authenticate(masterKey)) {
                    Log.v("bt", "auth : " + "Not ready");
                } else {
                    Log.v("bt", "auth: connecting");
                }
            } else {
                Log.v("bt", "auth : " + "invalid key");
            }

            return true;
        }else if (action.equals("enablePolling")) {
            if (!mBluetoothReader.transmitEscapeCommand(AUTO_POLLING_START)) {
                Log.v("bt", "polling : " + "not ready");
            }

            return true;
        }else if (action.equals("sendAPDU")) {
            byte apduCommand[] = Utils.hexString2Bytes(args.getString(0));

            if (apduCommand != null && apduCommand.length > 0) {
                if (!mBluetoothReader.transmitApdu(apduCommand)) {
                    Log.v("bt", "apdu : " + "Not ready");
                }
            } else {
                Log.v("bt", "apdu : " + "invalid command");
            }

            return true;
        }else if (action.equals("getCardStatus")) {
            if (!mBluetoothReader.getCardStatus()) {
                Log.v("bt", "status : " + "not ready");
            }

            return true;
        }
        return false;
    }



    /* Device scan callback. */
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi,
                             byte[] scanRecord) {
            if(!mLeDevices.contains(device))
            {
                mLeDevices.add(device);

//                try {
//                    JSONObject parameter = new JSONObject();
//                    parameter.put("id", device.toString());
//                    parameter.put("name", device.getName());
//                    parameter.put("address", device.getAddress());
////                jsCallback.success(parameter);
//
//                    PluginResult result = new PluginResult(PluginResult.Status.OK, parameter);
//                    result.setKeepCallback(true);
//                    jsCallback.sendPluginResult(result);
//
//                } catch (JSONException e) {
//                    Log.v("onLeScan", e.toString());
//                }
            }


        }
    };

    private boolean connectReader(String address) {
        /*
         * Connect Device.
         */
        /* Clear old GATT connection. */
        if (mBluetoothGatt != null) {
            Log.v("bt", "Clear old GATT connection");
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }

        /* Create a new connection. */
        final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);

        if (device == null) {
            Log.v("bt", "Device not found. Unable to connect.");
            return false;
        }

        /* Connect to GATT server. */
        mBluetoothGatt = device.connectGatt(cordova.getActivity().getApplicationContext(), false, mGattCallback);
        return true;
    }

    private String getErrorString(int errorCode) {
        if (errorCode == BluetoothReader.ERROR_SUCCESS) {
            return "";
        } else if (errorCode == BluetoothReader.ERROR_INVALID_CHECKSUM) {
            return "The checksum is invalid.";
        } else if (errorCode == BluetoothReader.ERROR_INVALID_DATA_LENGTH) {
            return "The data length is invalid.";
        } else if (errorCode == BluetoothReader.ERROR_INVALID_COMMAND) {
            return "The command is invalid.";
        } else if (errorCode == BluetoothReader.ERROR_UNKNOWN_COMMAND_ID) {
            return "The command ID is unknown.";
        } else if (errorCode == BluetoothReader.ERROR_CARD_OPERATION) {
            return "The card operation failed.";
        } else if (errorCode == BluetoothReader.ERROR_AUTHENTICATION_REQUIRED) {
            return "Authentication is required.";
        } else if (errorCode == BluetoothReader.ERROR_LOW_BATTERY) {
            return "The battery is low.";
        } else if (errorCode == BluetoothReader.ERROR_CHARACTERISTIC_NOT_FOUND) {
            return "Error characteristic is not found.";
        } else if (errorCode == BluetoothReader.ERROR_WRITE_DATA) {
            return "Write command to reader is failed.";
        } else if (errorCode == BluetoothReader.ERROR_TIMEOUT) {
            return "Timeout.";
        } else if (errorCode == BluetoothReader.ERROR_AUTHENTICATION_FAILED) {
            return "Authentication is failed.";
        } else if (errorCode == BluetoothReader.ERROR_UNDEFINED) {
            return "Undefined error.";
        } else if (errorCode == BluetoothReader.ERROR_INVALID_DATA) {
            return "Received data error.";
        }
        return "Unknown error.";
    }




    private String getResponseString(byte[] response, int errorCode) {
        if (errorCode == BluetoothReader.ERROR_SUCCESS) {
            if (response != null && response.length > 0) {
                return Utils.toHexString(response);
            }
            return "";
        }
        return getErrorString(errorCode);
    }

    private String getCardStatusString(int cardStatus) {
        if (cardStatus == BluetoothReader.CARD_STATUS_ABSENT) {
            return "Absent";
        } else if (cardStatus == BluetoothReader.CARD_STATUS_PRESENT) {
            return "Present";
        } else if (cardStatus == BluetoothReader.CARD_STATUS_POWERED) {
            return "Powered";
        } else if (cardStatus == BluetoothReader.CARD_STATUS_POWER_SAVING_MODE) {
            return "PowerSaving";
        }
        return "Unknown";
    }
    private String getBatteryStatusString(int batteryStatus) {
        if (batteryStatus == BluetoothReader.BATTERY_STATUS_NONE) {
            return "No battery.";
        } else if (batteryStatus == BluetoothReader.BATTERY_STATUS_FULL) {
            return "The battery is full.";
        } else if (batteryStatus == BluetoothReader.BATTERY_STATUS_USB_PLUGGED) {
            return "The USB is plugged.";
        }
        return "The battery is low.";
    }
}


class Utils {

    /**
     * Creates a hexadecimal <code>String</code> representation of the
     * <code>byte[]</code> passed. Each element is converted to a
     * <code>String</code> via the {@link Integer#toHexString(int)} and
     * separated by <code>" "</code>. If the array is <code>null</code>, then
     * <code>""<code> is returned.
     *
     * @param array
     *            the <code>byte</code> array to convert.
     * @return the <code>String</code> representation of <code>array</code> in
     *         hexadecimal.
     */
    public static String toHexString(byte[] array) {

        String bufferString = "";

        if (array != null) {
            for (int i = 0; i < array.length; i++) {
                String hexChar = Integer.toHexString(array[i] & 0xFF);
                if (hexChar.length() == 1) {
                    hexChar = "0" + hexChar;
                }
                bufferString += hexChar.toUpperCase(Locale.US) + " ";
            }
        }
        return bufferString;
    }

    private static boolean isHexNumber(byte value) {
        if (!(value >= '0' && value <= '9') && !(value >= 'A' && value <= 'F')
                && !(value >= 'a' && value <= 'f')) {
            return false;
        }
        return true;
    }

    /**
     * Checks a hexadecimal <code>String</code> that is contained hexadecimal
     * value or not.
     *
     * @param string
     *            the string to check.
     * @return <code>true</code> the <code>string</code> contains Hex number
     *         only, <code>false</code> otherwise.
     * @throws NullPointerException
     *             if <code>string == null</code>.
     */
    public static boolean isHexNumber(String string) {
        if (string == null)
            throw new NullPointerException("string was null");

        boolean flag = true;

        for (int i = 0; i < string.length(); i++) {
            char cc = string.charAt(i);
            if (!isHexNumber((byte) cc)) {
                flag = false;
                break;
            }
        }
        return flag;
    }

    private static byte uniteBytes(byte src0, byte src1) {
        byte _b0 = Byte.decode("0x" + new String(new byte[] { src0 }))
                .byteValue();
        _b0 = (byte) (_b0 << 4);
        byte _b1 = Byte.decode("0x" + new String(new byte[] { src1 }))
                .byteValue();
        byte ret = (byte) (_b0 ^ _b1);
        return ret;
    }

    /**
     * Creates a <code>byte[]</code> representation of the hexadecimal
     * <code>String</code> passed.
     *
     * @param string
     *            the hexadecimal string to be converted.
     * @return the <code>array</code> representation of <code>String</code>.
     * @throws IllegalArgumentException
     *             if <code>string</code> length is not in even number.
     * @throws NullPointerException
     *             if <code>string == null</code>.
     * @throws NumberFormatException
     *             if <code>string</code> cannot be parsed as a byte value.
     */
    public static byte[] hexString2Bytes(String string) {
        if (string == null)
            throw new NullPointerException("string was null");

        int len = string.length();

        if (len == 0)
            return new byte[0];
        if (len % 2 == 1)
            throw new IllegalArgumentException(
                    "string length should be an even number");

        byte[] ret = new byte[len / 2];
        byte[] tmp = string.getBytes();

        for (int i = 0; i < len; i += 2) {
            if (!isHexNumber(tmp[i]) || !isHexNumber(tmp[i + 1])) {
                throw new NumberFormatException(
                        "string contained invalid value");
            }
            ret[i / 2] = uniteBytes(tmp[i], tmp[i + 1]);
        }
        return ret;
    }

    /**
     * Creates a <code>byte[]</code> representation of the hexadecimal
     * <code>String</code> in the EditText control.
     *
     * @param editText
     *            the EditText control which contains hexadecimal string to be
     *            converted.
     * @return the <code>array</code> representation of <code>String</code> in
     *         the EditText control. <code>null</code> if the string format is
     *         not correct.
     */
    public static byte[] getEditTextinHexBytes(EditText editText) {
        Editable edit = editText.getText();

        if (edit == null) {
            return null;
        }

        String rawdata = edit.toString();

        if (rawdata == null || rawdata.isEmpty()) {
            return null;
        }

        String command = rawdata.replace(" ", "").replace("\n", "");

        if (command.isEmpty() || command.length() % 2 != 0
                || isHexNumber(command) == false) {
            return null;
        }

        return hexString2Bytes(command);
    }
}
