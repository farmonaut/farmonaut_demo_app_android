package com.farmonautsdk.nativelibrary;

public class NativeLib {

    // Used to load the 'nativelibrary' library on application startup.
    static {
        System.loadLibrary("nativelibrary");
    }

    /**
     * A native method that is implemented by the 'nativelibrary' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}