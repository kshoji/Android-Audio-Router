package jp.kshoji.audio.util;

import android.os.Build;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

/**
 *{@link android.media.AudioSystem} (hidden API) wrapper.
 *
 * <ul>
 * <li>This class requires a permission 'android.permission.MODIFY_AUDIO_SETTINGS'.</li>
 * <li>This class uses the hidden API, so the function will be broken in the future Android updates.</li>
 * </ul>
 *
 * @author K.Shoji
 */
@SuppressWarnings("JavadocReference")
public final class AudioSystem {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ DEVICE_STATE_UNAVAILABLE, DEVICE_STATE_AVAILABLE})
    public @interface DeviceState{}
    public static final int DEVICE_STATE_UNAVAILABLE = 0;
    public static final int DEVICE_STATE_AVAILABLE = 1;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            DEVICE_OUT_EARPIECE, DEVICE_OUT_SPEAKER, DEVICE_OUT_WIRED_HEADSET, DEVICE_OUT_WIRED_HEADPHONE,
            DEVICE_OUT_BLUETOOTH_SCO, DEVICE_OUT_BLUETOOTH_SCO_HEADSET, DEVICE_OUT_BLUETOOTH_SCO_CARKIT,
            DEVICE_OUT_BLUETOOTH_A2DP, DEVICE_OUT_BLUETOOTH_A2DP_HEADPHONES, DEVICE_OUT_BLUETOOTH_A2DP_SPEAKER,
            DEVICE_OUT_AUX_DIGITAL, DEVICE_OUT_ANLG_DOCK_HEADSET, DEVICE_OUT_DGTL_DOCK_HEADSET,
            DEVICE_OUT_USB_ACCESSORY, DEVICE_OUT_USB_DEVICE, DEVICE_OUT_REMOTE_SUBMIX
    })
    public @interface DeviceOut{}
    public static final int DEVICE_OUT_EARPIECE = 0x1;
    public static final int DEVICE_OUT_SPEAKER = 0x2;
    public static final int DEVICE_OUT_WIRED_HEADSET = 0x4;
    public static final int DEVICE_OUT_WIRED_HEADPHONE = 0x8;
    public static final int DEVICE_OUT_BLUETOOTH_SCO = 0x10;
    public static final int DEVICE_OUT_BLUETOOTH_SCO_HEADSET = 0x20;
    public static final int DEVICE_OUT_BLUETOOTH_SCO_CARKIT = 0x40;
    public static final int DEVICE_OUT_BLUETOOTH_A2DP = 0x80;
    public static final int DEVICE_OUT_BLUETOOTH_A2DP_HEADPHONES = 0x100;
    public static final int DEVICE_OUT_BLUETOOTH_A2DP_SPEAKER = 0x200;
    public static final int DEVICE_OUT_AUX_DIGITAL = 0x400;
    public static final int DEVICE_OUT_ANLG_DOCK_HEADSET = 0x800;
    public static final int DEVICE_OUT_DGTL_DOCK_HEADSET = 0x1000;
    public static final int DEVICE_OUT_USB_ACCESSORY = 0x2000;
    public static final int DEVICE_OUT_USB_DEVICE = 0x4000;
    public static final int DEVICE_OUT_REMOTE_SUBMIX = 0x8000;

    // device categories config for setForceUse, must match AudioSystem::forced_config
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            FORCE_NONE, FORCE_SPEAKER, FORCE_HEADPHONES, FORCE_BT_SCO, FORCE_BT_A2DP,
            FORCE_WIRED_ACCESSORY, FORCE_BT_CAR_DOCK, FORCE_BT_DESK_DOCK, FORCE_ANALOG_DOCK,
            FORCE_DIGITAL_DOCK, FORCE_NO_BT_A2DP
    })
    public @interface CategoryConfig{}
    public static final int FORCE_NONE = 0;
    public static final int FORCE_SPEAKER = 1;
    public static final int FORCE_HEADPHONES = 2;
    public static final int FORCE_BT_SCO = 3;
    public static final int FORCE_BT_A2DP = 4;
    public static final int FORCE_WIRED_ACCESSORY = 5;
    public static final int FORCE_BT_CAR_DOCK = 6;
    public static final int FORCE_BT_DESK_DOCK = 7;
    public static final int FORCE_ANALOG_DOCK = 8;
    public static final int FORCE_DIGITAL_DOCK = 9;
    public static final int FORCE_NO_BT_A2DP = 10;
    public static final int FORCE_DEFAULT = FORCE_NONE;

    // usage for setForceUse, must match AudioSystem::force_use
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            FOR_COMMUNICATION, FOR_MEDIA, FOR_RECORD, FOR_DOCK
    })
    public @interface Usage{}
    public static final int FOR_COMMUNICATION = 0;
    public static final int FOR_MEDIA = 1;
    public static final int FOR_RECORD = 2;
    public static final int FOR_DOCK = 3;

    private static Class<?> audioSystem;

    /**
     * Obtains AudioSystem class
     * @return class object
     */
    private static Class<?> getAudioSystem() {
        if (audioSystem == null) {
            try {
                audioSystem = Class.forName("android.media.AudioSystem");
            } catch (ClassNotFoundException ignored) {
            }
        }

        return audioSystem;
    }

    /**
     * Set the device connection state
     *
     * @param device device kind id
     * @param state DEVICE_STATE_AVAILABLE or DEVICE_STATE_UNAVAILABLE
     * @param deviceAddress device address
     */
    public static void setDeviceConnectionState(@DeviceOut int device, @DeviceState int state, @NonNull String deviceAddress) {
        setDeviceConnectionState(device, state, deviceAddress, "");
    }

    /**
     * Set the device connection state
     *
     * @param device device kind id
     * @param state DEVICE_STATE_AVAILABLE or DEVICE_STATE_UNAVAILABLE
     * @param deviceAddress device address
     * @param deviceName device name(required on Android version >= 6)
     */
    public static void setDeviceConnectionState(@DeviceOut int device, @DeviceState int state, @NonNull String deviceAddress, @Nullable String deviceName) {
        Class<?> audioSystem = getAudioSystem();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR &&
                Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            // Android 2.0 to 5.1
            try {
                Method method = audioSystem.getMethod("setDeviceConnectionState", Integer.TYPE, Integer.TYPE, String.class);
                method.invoke(null, device, state, deviceAddress);
            } catch (Exception ignored) {
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android version >= 6
            try {
                Method method = audioSystem.getMethod("setDeviceConnectionState", Integer.TYPE, Integer.TYPE, String.class, String.class);
                method.invoke(null, device, state, deviceAddress, deviceName);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Forces audio source
     *
     * @param usage audio usage
     * @param config device categories config
     */
    public static void setForceUse(@Usage int usage, @CategoryConfig int config) {
        Class<?> audioSystem = getAudioSystem();

        try {
            Method method = audioSystem.getMethod("setForceUse", Integer.TYPE, Integer.TYPE);
            method.invoke(null, usage, config);
        } catch (Exception ignored) {
        }
    }

    /**
     * Obtains current audio source
     *
     * @param usage audio usage
     * @return device categories config
     */
    public static @CategoryConfig int getForceUse(@Usage int usage) {
        Class<?> audioSystem = getAudioSystem();

        try {
            Method method = audioSystem.getMethod("getForceUse", Integer.TYPE);
            int result = (Integer) method.invoke(null, usage);
            switch (result) {
                case 0:
                    return FORCE_NONE;
                case 1:
                    return FORCE_SPEAKER;
                case 2:
                    return FORCE_HEADPHONES;
                case 3:
                    return FORCE_BT_SCO;
                case 4:
                    return FORCE_BT_A2DP;
                case 5:
                    return FORCE_WIRED_ACCESSORY;
                case 6:
                    return FORCE_BT_CAR_DOCK;
                case 7:
                    return FORCE_BT_DESK_DOCK;
                case 8:
                    return FORCE_ANALOG_DOCK;
                case 9:
                    return FORCE_DIGITAL_DOCK;
                case 10:
                    return FORCE_NO_BT_A2DP;
            }
        } catch (Exception ignored) {
        }

        return FORCE_NONE;
    }
}
