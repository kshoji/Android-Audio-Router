# Setup the library
Add these lines to `build.gradle` file.

```gradle
repositories {
    maven {
        // TODO Currently, the library is under developing, so the master branch has no code.
        // url 'https://github.com/kshoji/Android-Audio-Router/raw/master/library/repository'

        // if you need the latest library, uncomment the line below and comment the line above.
        url 'https://github.com/kshoji/Android-Audio-Router/raw/develop/library/repository'
    }
    mavenCentral()
}

dependencies {
    // TODO if you need the latest library, specify `0.0.1-SNAPSHOT` instead of `0.0.xxx`
    compile 'jp.kshoji:audio-router:0.0.1-SNAPSHOT:@aar'
}
```

Add these lines to `AndroidManifest.xml` file.

```xml
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
```

Add these lines to the app's main Activity `onCreate` and `onDestroy` method.

```java
AudioRouter audioRouter;
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    audioRouter = new AudioRouter(this);
}

void onDestroy() {
    if (audioRouter != null) {
        audioRouter.terminate();
        audioRouter = null;
    }
}
```

# Change the audio source
Use method `AudioRouter#setRouteMode()` with AudioRouteMode enums.

```java
audioRouter.setRouteMode(AudioRouter.AudioRouteMode.SPEAKER);
```

AudioRouteMode has these values.

```java
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
```