package io.kontakt.kbujak.bait2017;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.kontakt.proximity.KontaktProvider;
import com.kontakt.proximity.KontaktProximitySDK;
import com.kontakt.proximity.callback.InitCallback;
import com.kontakt.proximity.error.ErrorCause;
import com.kontakt.proximity.exception.TriggerExecutionException;
import com.kontakt.proximity.listener.TriggerListener;
import com.kontakt.sdk.android.ble.configuration.ActivityCheckConfiguration;
import com.kontakt.sdk.android.ble.configuration.ScanMode;
import com.kontakt.sdk.android.ble.configuration.ScanPeriod;
import com.kontakt.sdk.android.ble.connection.OnServiceReadyListener;
import com.kontakt.sdk.android.ble.manager.ProximityManager;
import com.kontakt.sdk.android.ble.manager.ProximityManagerFactory;
import com.kontakt.sdk.android.ble.manager.listeners.IBeaconListener;
import com.kontakt.sdk.android.ble.rssi.RssiCalculator;
import com.kontakt.sdk.android.ble.rssi.RssiCalculators;
import com.kontakt.sdk.android.ble.spec.EddystoneFrameType;
import com.kontakt.sdk.android.common.KontaktSDK;
import com.kontakt.sdk.android.common.model.Trigger;
import com.kontakt.sdk.android.common.profile.IBeaconDevice;
import com.kontakt.sdk.android.common.profile.IBeaconRegion;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "XYZ";
    private static final String uniqueId = "mabs";
    private static final String apiKey = "YourSecretAPI_KEY";
    private ProximityManager kontaktManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkPermissionAndStart();
        initializeSDK();
        initializeProximityManagerFactory();
    }

    @Override
    protected void onStop() {
        super.onStop();
        kontaktManager.stopScanning();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        kontaktManager.disconnect();
        kontaktManager = null;
    }

    private void initializeProximityManagerFactory() {
        kontaktManager = ProximityManagerFactory.create(this);
        kontaktManager.configuration()
                .scanMode(ScanMode.BALANCED)
                .scanPeriod(ScanPeriod.RANGING)
                .activityCheckConfiguration(ActivityCheckConfiguration.DEFAULT)
                .deviceUpdateCallbackInterval(TimeUnit.SECONDS.toMillis((1)))
                .eddystoneFrameTypes(EnumSet.of(EddystoneFrameType.URL))
                .rssiCalculator(RssiCalculators.newLimitedMeanRssiCalculator(5));

        kontaktManager.setIBeaconListener(new IBeaconListener() {
            @Override
            public void onIBeaconDiscovered(IBeaconDevice iBeacon, IBeaconRegion region) {
                if (uniqueId.equals(iBeacon.getUniqueId())) {
                    Log.e(TAG, "Beacon " +iBeacon.getUniqueId() +  "discovered with distance: " + iBeacon.getDistance());
                }
            }

            @Override
            public void onIBeaconsUpdated(List<IBeaconDevice> iBeacons, IBeaconRegion region) {
                for (IBeaconDevice iBeacon : iBeacons) {
                    if (uniqueId.equals(iBeacon.getUniqueId())) {
                        Log.e(TAG, "Beacon " +iBeacon.getUniqueId() +  "updated distance: " + iBeacon.getDistance());
                    }
                }

            }

            @Override
            public void onIBeaconLost(IBeaconDevice iBeacon, IBeaconRegion region) {

            }
        });
    }

    private void initializeSDK() {
        KontaktSDK.initialize(apiKey);
        if (KontaktSDK.isInitialized())
            Log.d(TAG, "SDK initialized");
    }

    private void checkPermissionAndStart() {
        int checkSelfPermissionResult = ContextCompat.checkSelfPermission(this, Arrays.toString(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}));
        if (PackageManager.PERMISSION_GRANTED == checkSelfPermissionResult) {
            //already granted
            Log.d(TAG,"Permission already granted");
            startScan();
        }
        else {
            //request permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 100);
            Log.d(TAG,"Permission request called");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (100 == requestCode) {
                Log.d(TAG,"Permission granted");
                startScan();
            }
        } else
        {
            Log.d(TAG,"Permission not granted");
            checkPermissionAndStart();
        }
    }

    private void startScan() {
        kontaktManager.connect(new OnServiceReadyListener() {
            @Override
            public void onServiceReady() {
                kontaktManager.startScanning();
                useTriggers();
            }
        });
    }

    private void useTriggers() {
        Context context = getApplicationContext();
        KontaktProximitySDK sdk = KontaktProvider.provideProximitySDK(context, apiKey);
        InitCallback initCallback = new InitCallback() {
            @Override
            public void onSuccess() { }

            @Override
            public void onFailure(ErrorCause cause) { }
        };

        sdk.triggers(initCallback, new TriggerListener() {
            @Override
            public void onHandled(Trigger trigger) { }

            @Override
            public void onExecutionFailed(Trigger trigger, TriggerExecutionException e) { }
        });
    }
}
