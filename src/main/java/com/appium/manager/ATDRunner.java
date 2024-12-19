package com.appium.manager;

import com.appium.capabilities.Capabilities;
import com.appium.device.Device;
import com.appium.device.Devices;
import com.appium.executor.ATDExecutor;
import com.appium.filelocations.FileLocations;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.appium.filelocations.FileLocations.SERVER_CONFIG;
import static com.appium.utils.ConfigFileManager.FRAMEWORK;
import static com.appium.utils.ConfigFileManager.RUNNER;
import static com.appium.utils.FigletHelper.figlet;
import static com.appium.utils.OverriddenVariable.getOverriddenStringValue;
import static java.lang.System.getProperty;

public class ATDRunner {
    private static final String ANDROID = "android";
    private static final String BOTH = "both";
    private static final String IOS = "iOS";
    public static final String USER_DIR = "user.dir";

    private final ATDExecutor ATDExecutor;
    private final Capabilities capabilities;
    private static final Logger LOGGER = Logger.getLogger(ATDRunner.class.getName());


    public ATDRunner() throws Exception {
        setLog4jCompatibility();
        capabilities = Capabilities.getInstance();
        writeServiceConfig();
        AppiumServerManager appiumServerManager = new AppiumServerManager();
        appiumServerManager.startAppiumServer("127.0.0.1"); //Needs to be removed
        List<Device> devices = Devices.getConnectedDevices();
        ATDExecutor = new ATDExecutor(devices);
        createOutputDirectoryIfNotExist();
    }

    private void setLog4jCompatibility() {
        // Migrating from Log4j 1.x to 2.x - https://logging.apache.org/log4j/2.x/manual/migration.html
        System.setProperty("log4j1.compatibility", "true");
    }


    private void writeServiceConfig() {
        JSONObject serverConfig = Capabilities.getInstance()
                .getCapabilityObjectFromKey("serverConfig");
        try (FileWriter writer = new FileWriter(new File(
                getProperty("user.dir") + SERVER_CONFIG))) {
            writer.write(serverConfig.toString());
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createOutputDirectoryIfNotExist() {
        File file = new File(System.getProperty(USER_DIR), FileLocations.OUTPUT_DIRECTORY);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    public boolean runner(String pack, List<String> tests) throws Exception {
        figlet(RUNNER.get());
        return parallelExecution(pack, tests);
    }

    public boolean runner(String pack) throws Exception {
        return parallelExecution(pack, new ArrayList<>());
    }

    private boolean parallelExecution(String pack, List<String> tests) throws Exception {
        int deviceCount = Devices.getConnectedDevices().size();

        if (deviceCount == 0) {
            figlet("No Devices Connected");
            System.exit(0);
        }

        LOGGER.info(LOGGER.getName()
                + "Total Number of devices detected::" + deviceCount + "\n");

        createAppiumLogsFolder();
        createSnapshotDirectoryFor();
        String platform = getOverriddenStringValue("Platform");
        if (platform.equalsIgnoreCase("android")) {
            if (!capabilities.getCapabilityObjectFromKey("android").has("appium:automationName")) {
                throw new IllegalArgumentException("Please set automationName "
                        + "as UIAutomator2 or Espresso to create Android driver");
            }
            generateDirectoryForAdbLogs();
        }

        boolean result = false;
        String runner = RUNNER.get();
        String framework = FRAMEWORK.get();

        if (framework.equalsIgnoreCase("testng")) {
            String executionType = runner.equalsIgnoreCase("distribute")
                    ? "distribute" : "parallel";
            result = ATDExecutor.constructXMLAndTriggerParallelRunner(tests, pack, deviceCount,
                    executionType);
        }
        return result;
    }

    private void generateDirectoryForAdbLogs() {
        File adb_logs = new File(System.getProperty(USER_DIR) + FileLocations.ADB_LOGS_DIRECTORY);
        if (!adb_logs.exists()) {
            try {
                adb_logs.mkdir();
            } catch (SecurityException se) {
                se.printStackTrace();
            }
        }
    }

    private void createAppiumLogsFolder() {
        File f = new File(System.getProperty(USER_DIR) + FileLocations.APPIUM_LOGS_DIRECTORY);
        if (!f.exists()) {
            try {
                f.mkdir();
            } catch (SecurityException se) {
                se.printStackTrace();
            }
        }
    }

    private void createSnapshotDirectoryFor() {
        List<Device> udids = Devices.getConnectedDevices();
        for (Device udid : udids) {
            String os = udid.getPlatform().equalsIgnoreCase(IOS) ? "iOS" : "Android";
            createPlatformDirectory(os);
            String deviceId = udid.getUdid();
            File file = new File(
                    System.getProperty(USER_DIR)
                            + FileLocations.SCREENSHOTS_DIRECTORY
                            + os
                            + File.separator
                            + deviceId);
            if (!file.exists()) {
                file.mkdir();
            }
        }
    }

    private void createPlatformDirectory(String platform) {
        File platformDirectory = new File(System.getProperty(USER_DIR)
                + FileLocations.SCREENSHOTS_DIRECTORY + platform);
        if (!platformDirectory.exists()) {
            platformDirectory.mkdirs();
        }
    }

}