package com.appium.manager;

import com.appium.utils.Api;
import com.appium.utils.CapabilityManager;
import com.appium.utils.OSType;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thoughtworks.device.Device;
import io.appium.java_client.service.local.flags.GeneralServerFlag;
import okhttp3.Response;
import org.json.JSONObject;

import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

public class RemoteAppiumManager implements IAppiumManager {

    private static AppiumDriverManager appiumDriverManager;

    @Override
    public void destroyAppiumNode(String host) throws Exception {
        new Api().getResponse("http://" + host + ":" + getRemoteAppiumManagerPort(host)
                + "/appium/stop").body().string();

    }

    @Override
    public String getRemoteWDHubIP(String host) throws Exception {
        String hostIP = "http://" + host;
        String appiumRunningPort = new JSONObject(new Api().getResponse(hostIP
                + ":" + getRemoteAppiumManagerPort(host)
                + "/appium/isRunning").body().string()).get("port").toString();
        return hostIP + ":" + appiumRunningPort + "/wd/hub";
    }

    @Override
    public void startAppiumServer(String host) throws Exception {
        System.out.println(
                "**************************************************************************\n");
        System.out.println("Starting Appium Server on host " + host);
        System.out.println(
                "**************************************************************************\n");
        String serverPath = CapabilityManager.getInstance().getAppiumServerPath(host);
        String serverPort = CapabilityManager.getInstance().getAppiumServerPort(host);

        String serverUrl = "";

        if (serverPath == null && serverPort == null) {

            System.out.println("Picking Default Path for AppiumServiceBuilder");
            serverUrl = "http://" + host + ":" + getRemoteAppiumManagerPort(host) + "/appium/start";

        } else if (serverPath != null && serverPort != null) {

            System.out.println("Picking UserSpecified Path & Port for AppiumServiceBuilder");
            serverUrl ="http://" + host + ":" + getRemoteAppiumManagerPort(host) + "/appium/start?URL=" + serverPath + "&PORT=" + serverPort;

        } else if (serverPath != null) {

            System.out.println("Picking UserSpecified Path " + "& Using default Port for AppiumServiceBuilder");
            serverUrl = "http://" + host + ":" + getRemoteAppiumManagerPort(host) + "/appium/start?URL=" + serverPath;

        } else if (serverPort != null) {

            System.out.println("Picking Default Path & User Port for AppiumServiceBuilder");
            serverUrl = "http://" + host + ":" + getRemoteAppiumManagerPort(host) + "/appium/start?PORT=" + serverPort;
        }

        addUserSpecifiedServerCapabilities(serverUrl);

        new Api().getResponse(serverUrl).body().string();

        boolean status = Boolean.getBoolean(new JSONObject(new Api()
                .getResponse("http://" + host + ":"
                        + getRemoteAppiumManagerPort(host)
                        + "/appium/isRunning").body().string()).get("status").toString());
        if (status) {
            System.out.println(
                    "***************************************************************\n");
            System.out.println("Appium Server started successfully on  " + host);
            System.out.println(
                    "****************************************************************\n");
        }
    }

    @Override
    public List<Device> getDevices(String machineIP, String platform) throws Exception {
        ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        List<Device> devices = new ArrayList<>();

        if (platform.equalsIgnoreCase(OSType.ANDROID.name())
                || platform.equalsIgnoreCase(OSType.BOTH.name())) {
            List<Device> androidDevices = Arrays.asList(mapper.readValue(new URL(
                            "http://" + machineIP + ":"
                                    + getRemoteAppiumManagerPort(machineIP) + "/devices/android"),
                    Device[].class));
            Optional.ofNullable(androidDevices).ifPresent(devices::addAll);
        }
        if (platform.equalsIgnoreCase(OSType.iOS.name())
                || platform.equalsIgnoreCase(OSType.BOTH.name())) {
            if (CapabilityManager.getInstance().isApp()) {
                if (CapabilityManager.getInstance().isSimulatorAppPresentInCapsJson()) {
                    List<Device> bootedSims = Arrays.asList(mapper.readValue(new URL(
                                    "http://" + machineIP + ":"
                                            + getRemoteAppiumManagerPort(machineIP)
                                            + "/devices/ios/bootedSims"),
                            Device[].class));
                    Optional.ofNullable(bootedSims).ifPresent(devices::addAll);
                }
                if (CapabilityManager.getInstance().isRealDeviceAppPresentInCapsJson()) {
                    List<Device> iOSRealDevices = Arrays.asList(mapper.readValue(new URL(
                                    "http://" + machineIP + ":"
                                            + getRemoteAppiumManagerPort(machineIP)
                                            + "/devices/ios/realDevices"),
                            Device[].class));
                    Optional.ofNullable(iOSRealDevices).ifPresent(devices::addAll);
                }
            } else {
                List<Device> iOSDevices = Arrays.asList(mapper.readValue(new URL(
                                "http://" + machineIP + ":"
                                        + getRemoteAppiumManagerPort(machineIP)
                                        + "/devices/ios/realDevices"),
                        Device[].class));
                Optional.ofNullable(iOSDevices).ifPresent(devices::addAll);
            }
        }
        return devices;
    }

    @Override
    public Device getSimulator(String machineIP, String deviceName, String os) throws Exception {
        ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String url = String.format("http://%s:"
                        + getRemoteAppiumManagerPort(machineIP)
                        + "/device/ios/simulator"
                        + "?simulatorName=%s&simulatorOSVersion=%s",
                machineIP, URLEncoder.encode(deviceName, "UTF-8"),
                URLEncoder.encode(os, "UTF-8"));
        Device device = mapper.readValue(new URL(url),
                Device.class);
        return device;
    }

    @Override
    public int getAvailablePort(String hostMachine) throws Exception {
        String url = String.format("http://%s:"
                + getRemoteAppiumManagerPort(hostMachine)
                + "/machine/availablePort", hostMachine);
        Response response = new Api().getResponse(url);
        return Integer.parseInt(response.body().string());
    }

    @Override
    public int startIOSWebKitProxy(String host) throws Exception {
        int port = getAvailablePort(host);
        ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String url = String.format("http://%s:" + getRemoteAppiumManagerPort(host)
                        + "/devices/ios/webkitproxy/start"
                        + "?udid=%s&port=%s", host,
                AppiumDeviceManager.getAppiumDevice().getDevice().getUdid(),
                port);
        Response response = new Api().getResponse(url);
        AppiumDeviceManager.getAppiumDevice().setWebkitProcessID(response.body().string());
        return port;
    }

    @Override
    public void destoryIOSWebKitProxy(String host) throws Exception {
        if (AppiumDeviceManager.getAppiumDevice().getWebkitProcessID() != null) {
            ObjectMapper mapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            String url = String.format("http://%s:" + getRemoteAppiumManagerPort(host)
                            + "/devices/ios/webkitproxy/stop"
                            + "?processID=%s", host,
                    AppiumDeviceManager.getAppiumDevice().getWebkitProcessID());
            new Api().getResponse(url);
            AppiumDeviceManager.getAppiumDevice().setWebkitProcessID(null);
        }
    }

    private String getRemoteAppiumManagerPort(String host) throws Exception {
        String serverPort = CapabilityManager.getInstance()
                .getRemoteAppiumManangerPort(host);
        if (serverPort == null) {
            return "4567";
        } else {
            return serverPort;
        }
    }

    private String addUserSpecifiedServerCapabilities ( String serverUrl ) throws Exception {

        appiumDriverManager = new AppiumDriverManager();

        JSONObject serverCaps = appiumDriverManager.getDesiredServerCapabilities();

        Iterator<?> keys = serverCaps.keys();

        if (serverCaps.length() != 0) {

            do {
                String key = (String) keys.next();

                GeneralServerFlag serverArgument = appiumDriverManager.getServerArgument(key);

                if (serverArgument != null && serverCaps.get(key).toString() != "null") {

                    serverUrl = serverUrl + "&" + serverArgument.getArgument() + ":" + serverCaps.get(key);

                } else if (serverArgument != null) {

                    serverUrl = serverUrl + "&" + serverArgument.getArgument();
                }
            } while (keys.hasNext());
        }

        return serverUrl;
    }

}

