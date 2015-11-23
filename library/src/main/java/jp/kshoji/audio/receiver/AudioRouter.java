package jp.kshoji.audio.receiver;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.annotation.NonNull;

import java.util.Collection;
import java.util.HashSet;

import jp.kshoji.audio.util.AudioSystem;

/**
 * <h2>Routing the audio output</h2>
 * When the headphone connected, audio output will switch to the headphone, and speaker will be turned off. This is the default behaviour.<br />
 * With using {@link #setRouteMode(AudioRouteMode)} method or {@link #AudioRouter(Context, AudioRouteMode)} constructor, this class forces the audio output to the specified audio route.<br />
 * <ul>
 * <li>This class requires a permission 'android.permission.MODIFY_AUDIO_SETTINGS' and 'android.permission.BLUETOOTH'.</li>
 * <li>This class uses the hidden API, so the function will be broken in the future Android updates.</li>
 * </ul>
 *
 * @author K.Shoji
 */
public final class AudioRouter extends BroadcastReceiver {
    private final Context context;
    private AudioRouteMode routeMode = AudioRouteMode.NO_ROUTING;
    private Collection<BluetoothDevice> connectedBluetoothDevices = new HashSet<>();
    private Collection<Headset> connectedHeadsets = new HashSet<>();
    private Collection<UsbAudio> connectedUsbAudios = new HashSet<>();
    private final int initialRoute;
    private AudioManager audioManager;

    /**
     * Represents headset connection information
     */
    private final class Headset {
        private String address;
        private String portName;
        private int microphone;

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getPortName() {
            return portName;
        }

        public void setPortName(String portName) {
            this.portName = portName;
        }

        public int getMicrophone() {
            return microphone;
        }

        public void setMicrophone(int microphone) {
            this.microphone = microphone;
        }

        @Override
        public int hashCode() {
            return (address + portName).hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || !(o instanceof Headset)) {
                return false;
            }
            return this.hashCode() == o.hashCode();
        }
    }

    /**
     * Represents USB Audio connection information
     */
    private final class UsbAudio {
        private String address;
        private String portName;


        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getPortName() {
            return portName;
        }

        public void setPortName(String portName) {
            this.portName = portName;
        }

        @Override
        public int hashCode() {
            return (address + portName).hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || !(o instanceof Headset)) {
                return false;
            }
            return this.hashCode() == o.hashCode();
        }
    }

    /**
     * Audio route mode
     */
    public enum AudioRouteMode {
        WIRED_HEADPHONE,
        SPEAKER,
        USB_AUDIO,
        BLUETOOTH_A2DP,
        NO_ROUTING
    }

    /**
     * Set the route mode
     *
     * @param routeMode route mode
     */
    public void setRouteMode(AudioRouteMode routeMode) {
        this.routeMode = routeMode;
        setupRoute();
    }

    /**
     * Intent actions
     */
    public static final String INTENT_ACTION_ANALOG_AUDIO_DOCK_PLUG = "android.intent.action.ANALOG_AUDIO_DOCK_PLUG";
    public static final String MEDIA_ACTION_ANALOG_AUDIO_DOCK_PLUG = "android.media.action.ANALOG_AUDIO_DOCK_PLUG";
    public static final String BLUETOOTH_A2DP_CONNECTION_STATE_CHANGED = "android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED";

    /**
     * Constructor, initialize and attach this BroadcastReceiver to the specified context
     *
     * @param context the context
     */
    public AudioRouter(@NonNull Context context) {
        this(context, AudioRouteMode.NO_ROUTING);
    }

    /**
     * Constructor, initialize and attach this BroadcastReceiver to the specified context
     *
     * @param context   the context
     * @param routeMode route mode
     */
    public AudioRouter(@NonNull Context context, AudioRouteMode routeMode) {
        this.context = context;
        this.routeMode = routeMode;

        initialRoute = AudioSystem.getForceUse(AudioSystem.FOR_MEDIA);

        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        setupRoute();

        IntentFilter filter = new IntentFilter();
        filter.addAction(INTENT_ACTION_ANALOG_AUDIO_DOCK_PLUG);
        filter.addAction(MEDIA_ACTION_ANALOG_AUDIO_DOCK_PLUG);
        filter.addAction(BLUETOOTH_A2DP_CONNECTION_STATE_CHANGED);
        filter.addAction(Intent.ACTION_HEADSET_PLUG);
        context.registerReceiver(this, filter);
    }

    /**
     * Reset the route to initial route
     */
    public void resetToInitialRoute() {
        AudioSystem.setForceUse(AudioSystem.FOR_MEDIA, initialRoute);
    }

    /**
     * Must be called on Activity.onDestroy()
     */
    public void terminate() {
        context.unregisterReceiver(this);
    }

    /**
     * Obtains value for key, returns defaultValue if value is null
     *
     * @param bundle       the bundle
     * @param key          key string
     * @param defaultValue default value
     * @return the value
     */
    private String getStringFromBundle(Bundle bundle, String key, String defaultValue) {
        final String result = bundle.getString(key);
        return (result == null) ? defaultValue : result;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Bundle extras = intent.getExtras();

        if (routeMode == AudioRouteMode.NO_ROUTING) {
            return;
        }

        // update connecting devices
        int connectionState = extras.getInt("state");
        switch (action) {
            case INTENT_ACTION_ANALOG_AUDIO_DOCK_PLUG:
            case MEDIA_ACTION_ANALOG_AUDIO_DOCK_PLUG:
                if ("usb_audio".equals(extras.getString("name"))) {
                    UsbAudio usbAudio = new UsbAudio();
                    usbAudio.setAddress(getStringFromBundle(extras, "address", ""));
                    usbAudio.setPortName(getStringFromBundle(extras, "name", ""));

                    if (connectionState == 1) {
                        connectedUsbAudios.add(usbAudio);
                    } else if (connectionState == 0) {
                        connectedUsbAudios.remove(usbAudio);
                    }
                    setupRoute();
                }
                break;
            case Intent.ACTION_HEADSET_PLUG:
                Headset headset = new Headset();
                headset.setAddress(getStringFromBundle(extras, "address", ""));
                headset.setPortName(getStringFromBundle(extras, "portName", ""));
                headset.setMicrophone(extras.getInt("microphone", 0));

                if (connectionState == 1) {
                    connectedHeadsets.add(headset);
                } else if (connectionState == 0) {
                    connectedHeadsets.remove(headset);
                }
                setupRoute();
                break;
            case BLUETOOTH_A2DP_CONNECTION_STATE_CHANGED:
                BluetoothDevice bluetoothDevice = (BluetoothDevice) extras.get("android.bluetooth.device.extra.DEVICE");
                if (bluetoothDevice != null) {
                    int bluetoothConnectionState = extras.getInt("android.bluetooth.profile.extra.STATE");
                    if (bluetoothConnectionState == 2) {
                        connectedBluetoothDevices.add(bluetoothDevice);
                    } else if (bluetoothConnectionState == 0) {
                        connectedBluetoothDevices.remove(bluetoothDevice);
                    }
                }
                setupRoute();
                break;
        }
    }

    /**
     * Set the audio output to the specified route
     */
    private void setupRoute() {
        switch (routeMode) {
            case WIRED_HEADPHONE:
                // disable other sources
                setupSpeaker(false);
                setupUsbAudio(false);
                setupBluetoothA2DP(false);

                // enable headphone
                setupHeadphone(true);
                break;
            case SPEAKER:
                // disable other sources
                setupHeadphone(false);
                setupUsbAudio(false);
                setupBluetoothA2DP(false);

                // enable speaker
                setupSpeaker(true);
                break;
            case USB_AUDIO:
                // disable other sources
                setupHeadphone(false);
                setupSpeaker(false);
                setupBluetoothA2DP(false);

                // enable USB Audio
                setupUsbAudio(true);
                break;
            case BLUETOOTH_A2DP:
                // disable other sources
                setupHeadphone(false);
                setupSpeaker(false);
                setupUsbAudio(false);

                // enable bluetooth A2DP
                setupBluetoothA2DP(true);
                break;
            case NO_ROUTING:
                // do nothing
                break;
        }
    }

    /**
     * Sets up headphone routing
     *
     * @param enabled headphone status
     */
    private void setupHeadphone(boolean enabled) {
        if (enabled) {
            AudioSystem.setForceUse(AudioSystem.FOR_MEDIA, AudioSystem.FORCE_HEADPHONES);
            if (connectedHeadsets.isEmpty()) {
                AudioSystem.setDeviceConnectionState(AudioSystem.DEVICE_OUT_WIRED_HEADPHONE, AudioSystem.DEVICE_STATE_AVAILABLE, "");
            } else {
                for (Headset headset : connectedHeadsets) {
                    int deviceOut = (headset.getMicrophone() == 0) ? AudioSystem.DEVICE_OUT_WIRED_HEADPHONE : AudioSystem.DEVICE_OUT_WIRED_HEADSET;

                    AudioSystem.setDeviceConnectionState(deviceOut, AudioSystem.DEVICE_STATE_AVAILABLE, headset.getAddress(), headset.getPortName());
                }
            }
        } else {
            if (connectedHeadsets.isEmpty()) {
                AudioSystem.setDeviceConnectionState(AudioSystem.DEVICE_OUT_WIRED_HEADPHONE, AudioSystem.DEVICE_STATE_UNAVAILABLE, "");
            } else {
                for (Headset headset : connectedHeadsets) {
                    int deviceOut = (headset.getMicrophone() == 0) ? AudioSystem.DEVICE_OUT_WIRED_HEADPHONE : AudioSystem.DEVICE_OUT_WIRED_HEADSET;

                    AudioSystem.setDeviceConnectionState(deviceOut, AudioSystem.DEVICE_STATE_UNAVAILABLE, headset.getAddress(), headset.getPortName());
                }
            }
        }
    }

    /**
     * Sets up speaker routing
     *
     * @param enabled speaker status
     */
    private void setupSpeaker(boolean enabled) {
        if (enabled) {
            AudioSystem.setForceUse(AudioSystem.FOR_MEDIA, AudioSystem.FORCE_SPEAKER);
            AudioSystem.setDeviceConnectionState(AudioSystem.DEVICE_OUT_SPEAKER, AudioSystem.DEVICE_STATE_AVAILABLE, "", "");
        } else {
            AudioSystem.setDeviceConnectionState(AudioSystem.DEVICE_OUT_SPEAKER, AudioSystem.DEVICE_STATE_UNAVAILABLE, "", "");
        }
    }

    /**
     * Sets up Bluetooth A2DP routing
     *
     * @param enabled Bluetooth A2DP status
     */
    private void setupBluetoothA2DP(boolean enabled) {
        int status;
        if (enabled) {
            AudioSystem.setForceUse(AudioSystem.FOR_MEDIA, AudioSystem.FORCE_BT_A2DP);
            audioManager.setParameters("A2dpSuspended=false");
            status = AudioSystem.DEVICE_STATE_AVAILABLE;
        } else {
            AudioSystem.setForceUse(AudioSystem.FOR_MEDIA, AudioSystem.FORCE_NO_BT_A2DP);
            audioManager.setParameters("A2dpSuspended=true");
            status = AudioSystem.DEVICE_STATE_UNAVAILABLE;
        }

        for (BluetoothDevice bluetoothDevice : connectedBluetoothDevices) {
            AudioSystem.setDeviceConnectionState(AudioSystem.DEVICE_OUT_BLUETOOTH_A2DP, status, bluetoothDevice.getAddress(), bluetoothDevice.getName());

            switch (bluetoothDevice.getBluetoothClass().getDeviceClass()) {
                case BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES:
                case BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET:
                    AudioSystem.setDeviceConnectionState(AudioSystem.DEVICE_OUT_BLUETOOTH_A2DP_HEADPHONES, status, bluetoothDevice.getAddress(), bluetoothDevice.getName());
                    break;
                case BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO:
                case BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE:
                case BluetoothClass.Device.AUDIO_VIDEO_HIFI_AUDIO:
                case BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER:
                case BluetoothClass.Device.AUDIO_VIDEO_PORTABLE_AUDIO:
                case BluetoothClass.Device.AUDIO_VIDEO_SET_TOP_BOX:
                    AudioSystem.setDeviceConnectionState(AudioSystem.DEVICE_OUT_BLUETOOTH_A2DP_SPEAKER, status, bluetoothDevice.getAddress(), bluetoothDevice.getName());
                    break;
            }
        }
    }

    /**
     * Sets up USB Audio routing
     *
     * @param enabled USB Audio status
     */
    private void setupUsbAudio(boolean enabled) {
        if (enabled) {
            AudioSystem.setForceUse(AudioSystem.FOR_MEDIA, AudioSystem.FORCE_ANALOG_DOCK);
        }

        for (UsbAudio usbAudio : connectedUsbAudios) {
            if (enabled) {
                AudioSystem.setDeviceConnectionState(AudioSystem.DEVICE_OUT_ANLG_DOCK_HEADSET, AudioSystem.DEVICE_STATE_AVAILABLE, usbAudio.getAddress(), usbAudio.getPortName());
            } else {
                AudioSystem.setDeviceConnectionState(AudioSystem.DEVICE_OUT_ANLG_DOCK_HEADSET, AudioSystem.DEVICE_STATE_UNAVAILABLE, usbAudio.getAddress(), usbAudio.getPortName());
            }
        }
    }
}
