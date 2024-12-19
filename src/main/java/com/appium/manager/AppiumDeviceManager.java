package com.appium.manager;

import com.appium.capabilities.DriverSession;
import com.appium.entities.MobilePlatform;
import com.appium.plugin.PluginClI;

import java.io.IOException;

/**
 * Device Manager - Handles all device related information's e.g UDID, Model, etc
 */
public class AppiumDeviceManager {

    private static ThreadLocal<DriverSession> appiumDevice = new ThreadLocal<>();


    public static DriverSession getAppiumDevice() {
        return appiumDevice.get();
    }

    protected static void setDevice(DriverSession device) {
        appiumDevice.set(device);
    }


    public static MobilePlatform getMobilePlatform() {
        return MobilePlatform.valueOf(System.getenv("Platform").toUpperCase());
    }

    public static boolean isPlatform(MobilePlatform expectedPlatform) throws IOException {
        return AppiumDeviceManager.getMobilePlatform().equals(expectedPlatform);
    }
}
