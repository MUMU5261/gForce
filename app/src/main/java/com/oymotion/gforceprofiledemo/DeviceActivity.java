package com.oymotion.gforceprofiledemo;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.oymotion.gforceprofile.CommandResponseCallback;
import com.oymotion.gforceprofile.DataNotificationCallback;
import com.oymotion.gforceprofile.GForceProfile;

import java.util.Arrays;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class DeviceActivity extends AppCompatActivity {
    @BindView(R.id.connect)
    Button btn_conncet;
    @BindView(R.id.start)
    Button btn_start;
    @BindView(R.id.get_firmware_version)
    Button btn_getFirmwareVersion;
    @BindView(R.id.set)
    Button btn_set;
    public static final String EXTRA_DEVICE_NAME = "extra_device_name";
    public static final String EXTRA_MAC_ADDRESS = "extra_mac_address";

    private GForceProfile.BluetoothDeviceStateEx state = GForceProfile.BluetoothDeviceStateEx.disconnected;
    private boolean setSucceeded = false;
    private boolean returnSucceeded = false;
    private String macAddress;
    private TextView textViewState;
    private TextView textViewQuaternion;
    private TextView textViewEMG;
    private TextView textFirmwareVersion;
    private String textErrorMsg = "";
    private Handler handler;
    private Runnable runnable;
    private boolean notifying = false;

    private GForceProfile gForceProfile;


    @OnClick(R.id.connect)
    public void onConnectClick() {
        Log.i("DeviceActivity", "[onConnectClick] state=" + state);

        if (state != GForceProfile.BluetoothDeviceStateEx.connected &&
            state != GForceProfile.BluetoothDeviceStateEx.connecting &&
            state != GForceProfile.BluetoothDeviceStateEx.ready) {

            GForceProfile.GF_RET_CODE ret_code = gForceProfile.connect(macAddress,false);

            if (ret_code != GForceProfile.GF_RET_CODE.GF_SUCCESS) {
                Log.e("DeviceActivity", "Connect failed, ret_code: " + ret_code);
                textViewState.setText("Connect failed, ret_code: " + ret_code);
                return;
            }

            handler.removeCallbacks(runnable);
            handler.postDelayed(runnable, 1000);
        } else {

            boolean success = gForceProfile.disconnect();
            if(success){
                btn_getFirmwareVersion.setEnabled(false);
                btn_set.setEnabled(false);
                btn_start.setEnabled(false);

                setSucceeded = false;
                notifying = false;

                runOnUiThread(new Runnable() {
                    public void run() {
                        btn_start.setText("Start Data Notification");
                        textViewQuaternion.setText("W: " + "\nX: " + "\nY: " + "\nZ: ");
                        textViewEMG.setText("C1: " + "\nC2: " + "\nC3: " + "\nC4: ");
                        textFirmwareVersion.setText("FirmwareVersion: ");
                    }
                });

            }
        }

    }

    @OnClick(R.id.set)
    public void onSetClick() {
        if (state != GForceProfile.BluetoothDeviceStateEx.ready || setSucceeded) return;
        //DNF_QUATERNION | DNF_EMG_RAW |DNF_EULERANGLE
        int flags = GForceProfile.DataNotifFlags.DNF_EMG_RAW | GForceProfile.DataNotifFlags.DNF_QUATERNION | GForceProfile.DataNotifFlags.DNF_EULERANGLE;

        GForceProfile.GF_RET_CODE result = gForceProfile.setDataNotifSwitch(flags, new CommandResponseCallback() {
            @Override
            public void onSetCommandResponse(int resp) {
                Log.i("DeviceActivity", "onSetCommandResponse: " + resp);

                if (resp == GForceProfile.ResponseResult.RSP_CODE_SUCCESS) {
                    returnSucceeded = true;

                    runOnUiThread(new Runnable() {
                        public void run() {
                            textViewState.setText("Device State: " + "Set Data Switch succeeded");
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            textViewState.setText("Device State: " + "Set Data Switch failed, resp code: " + resp);
                        }
                    });
                }
            }
        }, 5000);

        Log.i("DeviceActivity", " result " + result);
        if (result == GForceProfile.GF_RET_CODE.GF_SUCCESS) {
            setSucceeded = true;
        } else {
            runOnUiThread(new Runnable() {
                public void run() {
                    textViewState.setText("Device State: " + "Set Data Switch failed: " + result.toString());
                }
            });
        }
    }

    @OnClick(R.id.start)
    public void onStartClick() {

        if (notifying) {
            btn_start.setText("Start Data Notification");

            gForceProfile.stopDataNotification();

            notifying = false;
        } else {
            if (state != GForceProfile.BluetoothDeviceStateEx.ready || returnSucceeded == false) return;

            gForceProfile.startDataNotification(new DataNotificationCallback() {
                @Override
                public void onData(byte[] data) {
//                    NTF_QUAT_FLOAT_DATA | NTF_EMG_ADC_DATA |NTF_EULER_DATA
                    if (data[0] == GForceProfile.NotifDataType.NTF_QUAT_FLOAT_DATA && data.length == 17) {  //NTF_QUAT_FLOAT_DATA | NTF_EMG_ADC_DATA
                        System.out.println(data [0]);
                        System.out.println(data.length);
                        Log.i("DeviceActivity","Quat data: " + Arrays.toString(data));

                        byte[] W = new byte[4];
                        byte[] X = new byte[4];
                        byte[] Y = new byte[4];
                        byte[] Z = new byte[4];

                        System.arraycopy(data, 1, W, 0, 4);
                        System.arraycopy(data, 5, X, 0, 4);
                        System.arraycopy(data, 9, Y, 0, 4);
                        System.arraycopy(data, 13, Z, 0, 4);

                        float w = getFloat(W);
                        float x = getFloat(X);
                        float y = getFloat(Y);
                        float z = getFloat(Z);


                        runOnUiThread(new Runnable() {
                            public void run() {
                                textViewQuaternion.setText("W: " + w + "\nX: " + x + "\nY: " + y + "\nZ: " + z);
//                              textViewEMG.setText(data.toString());
                            }
                        });

//

//                         Handler handler1 = new Handler();
//                         Runnable runnable = new Runnable() {
//                             @Override
//                             public void run() {
//                                 System.out.println("runinsert");
//                                 handler1.postDelayed(this, Time);
//                                 SQLiteDatabase db = dbHelper.getWritableDatabase();
//                                 ContentValues values = new ContentValues();
//                                 values.put("w",w);
//                                 values.put("x",x);
//                                 values.put("y",y);
//                                 values.put("z",z);
//                                 values.put("user_id", "1");
//                                 values.put("section", "test");
//                                 values.put("timestamp",df.format(new Date()));
//                                 db.insert("Quaternion", null, values);
//                                 values.clear();
//                             }
//                         };
//                         handler1.postDelayed(runnable, Time);


                    } else if(data[0] == GForceProfile.NotifDataType.NTF_EMG_ADC_DATA && data.length == 129){
                        System.out.println("EMG");
                        System.out.println(data [0]);
                        System.out.println(data.length);

                    }else if(data[0] == GForceProfile.NotifDataType.NTF_EULER_DATA && data.length == 13){
                        System.out.println("NTF_EULER_DATA");
                        System.out.println(data [0]);
                        System.out.println(data.length);

                    }
                }
            });

            btn_start.setText("Stop Data Notification");
            notifying = true;
        }
    }

    @OnClick(R.id.get_firmware_version)
    public void onGetFirmwareVersionClick() {
        GForceProfile.GF_RET_CODE result = gForceProfile.getControllerFirmwareVersion(new CommandResponseCallback() {
            @Override
            public void onGetControllerFirmwareVersion(int resp, String firmwareVersion) {
            Log.i("DeviceActivity", "\nfirmwareVersion: " + firmwareVersion);

            runOnUiThread(new Runnable() {
                public void run() {
                    textFirmwareVersion.setText("FirmwareVersion: " + firmwareVersion);
                }
            });
            }
        }, 50000);
        Log.i("DeviceActivity", " result " + result);
        if (result != GForceProfile.GF_RET_CODE.GF_SUCCESS) {
            textFirmwareVersion.setText("FirmwareVersion: Error : " + result);
        }
    }

    public static float getFloat(byte[] b) {
        int accum = 0;
        accum = accum | (b[0] & 0xff) << 0;
        accum = accum | (b[1] & 0xff) << 8;
        accum = accum | (b[2] & 0xff) << 16;
        accum = accum | (b[3] & 0xff) << 24;
        System.out.println(accum);
        return Float.intBitsToFloat(accum);
    }
    void updateState() {
        GForceProfile.BluetoothDeviceStateEx newState = gForceProfile.getState();

        if (state != newState) {
            runOnUiThread(new Runnable() {
                public void run() {
                    textViewState.setText("Device State: " + newState.toString());
                }
            });

            state = newState;

            if (state == GForceProfile.BluetoothDeviceStateEx.disconnected) {
                btn_conncet.setText("Connect");
            }else if (state == GForceProfile.BluetoothDeviceStateEx.connected) {
                btn_conncet.setText("Disconnect");
            }else if (state == GForceProfile.BluetoothDeviceStateEx.connecting) {
                btn_conncet.setText("Disconnect");
            }else if (state == GForceProfile.BluetoothDeviceStateEx.disconnecting) {
                btn_conncet.setText("Connect");
            }else if (state == GForceProfile.BluetoothDeviceStateEx.ready) {
                btn_conncet.setText("Disconnect");

                btn_getFirmwareVersion.setEnabled(true);
                btn_set.setEnabled(true);
                btn_start.setEnabled(true);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        ButterKnife.bind(this);
        macAddress = getIntent().getStringExtra(EXTRA_MAC_ADDRESS);
        getSupportActionBar().setSubtitle(getString(R.string.dev_name_with_mac, getIntent().getStringExtra(EXTRA_DEVICE_NAME), macAddress));
        handler = new Handler();

        runnable = new Runnable() {
            public void run() {
                updateState();
                handler.postDelayed(this, 1000);
            }
        };

        gForceProfile = new GForceProfile(this);
        textViewState = this.findViewById(R.id.text_device_state);
        textViewQuaternion = this.findViewById(R.id.text_quaternion);
        textViewEMG = this.findViewById(R.id.text_emg);
        textFirmwareVersion = this.findViewById(R.id.text_firmware_version);

        btn_getFirmwareVersion.setEnabled(false);
        btn_set.setEnabled(false);
        btn_start.setEnabled(false);
    }

    @Override
    protected void onStop() {
        super.onStop();
        handler.removeCallbacks(runnable);
        gForceProfile.disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
