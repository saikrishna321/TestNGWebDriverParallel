package com.appium.capabilities;

import com.appium.plugin.PluginClI;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.nio.file.FileSystems;
import java.nio.file.Path;

import static com.appium.utils.ConfigFileManager.CAPS;

/**
 * Created by saikrisv on 20/05/17.
 */
public class DesiredCapabilityBuilder {

    private static final Logger LOGGER = Logger.getLogger(DesiredCapabilityBuilder.class.getName());

    public DesiredCapabilities buildDesiredCapability(String capabilityFilePath) {
        String platform = System.getenv("Platform");
        DesiredCapabilities desiredCapabilities = new DesiredCapabilities();
        JSONObject platFormCapabilities;
        JSONObject fullCapabilities;
        if (CAPS.get().equalsIgnoreCase(capabilityFilePath)) {
            LOGGER.info("Capabilities file is not specified. Using default capabilities file");
            fullCapabilities = Capabilities.getInstance()
                    .getCapabilities();
        } else {
            LOGGER.info("Capabilities file is specified. Using specified capabilities file: "
                    + capabilityFilePath);
            fullCapabilities = Capabilities.getInstance()
                    .createInstance(capabilityFilePath);
        }
        platFormCapabilities = fullCapabilities
                .getJSONObject(platform);
        JSONObject finalPlatformCapabilities = platFormCapabilities;
        platFormCapabilities.keySet().forEach(key -> desiredCapabilities.setCapability(key,
                finalPlatformCapabilities.get(key)));
        desiredCapabilities.setCapability("app",
                getAppPathInCapabilities(platform, fullCapabilities));
        return desiredCapabilities;
    }

    private String getAppPathInCapabilities(String platform, JSONObject fullCapabilities) {
        String appPath = null;
        if (fullCapabilities.getJSONObject(platform).has("app")) {
            Object app = fullCapabilities.getJSONObject(platform).get("app");
            if ((PluginClI.getInstance().getPlugin().getDeviceFarm().getCloud() == null)
                    && !(new UrlValidator()).isValid(app.toString())) {
                Path path = FileSystems.getDefault().getPath(app.toString());
                appPath = path.normalize().toAbsolutePath().toString();
            } else {
                appPath = app.toString();
            }
        }
        return appPath;
    }
}
