package com.appium.capabilities;

import com.appium.device.AtdEnvironment;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static com.appium.utils.ConfigFileManager.CAPS;
import static com.appium.utils.OverriddenVariable.getOverriddenStringValue;

public class Capabilities {
    private static final Logger LOGGER = Logger.getLogger(Capabilities.class.getName());

    private static Capabilities instance;
    private JSONObject capabilities;
    private final AtdEnvironment atdEnvironment;

    private Capabilities() {
        atdEnvironment = new AtdEnvironment();
        capabilities = createInstance(getCapabilityLocation());
    }

    public JSONObject createInstance(String capabilitiesJson) {
        String fileName = FilenameUtils.removeExtension(new File(capabilitiesJson).getName());
        byte[] fileContent;
        try {
            fileContent = Files.readAllBytes(Paths.get(capabilitiesJson));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        JSONObject capabilitiesJsonObject = new JSONObject(new String(fileContent));
        String defaultFileName = FilenameUtils.removeExtension(new File(CAPS.get()).getName());
        StringBuilder varParsing = new StringBuilder(200);
        if (!fileName.equals(defaultFileName)) {
            varParsing.append("atd_" + fileName).append("_");
        } else {
            varParsing.append("atd").append("_");
        }
        JSONObject loadedCapabilities = loadAndOverrideFromEnvVars(
                capabilitiesJsonObject,
                new JSONObject(),
                getAllATDOverrideEnvVars(),
                varParsing);
        //validateCapabilitySchema(loadedCapabilities);
        return loadedCapabilities;
    }

    public static Capabilities getInstance() {
        if (instance == null) {
            instance = new Capabilities();
        }
        return instance;
    }

    private Map<String, Object> getAllATDOverrideEnvVars() {
        Map<String, Object> atdOverrideEnv = new HashMap<>();
        System.getenv().forEach((key, value) -> {
            if (key.startsWith("atd")) {
                atdOverrideEnv.put(key, value);
            }
        });
        Properties properties = System.getProperties();
        properties.keySet().forEach(key -> {
            if (key.toString().startsWith("atd")) {
                atdOverrideEnv.put(key.toString(), properties.getProperty(key.toString()));
            }
        });
        return atdOverrideEnv;
    }

    private JSONObject loadAndOverrideFromEnvVars(JSONObject originalObject,
                                                  JSONObject objectToUpdate,
                                                  Map<String, Object> allATDOverrideEnvVars,
                                                  StringBuilder currentPath) {
        Set<String> keys = originalObject.keySet();
        keys.forEach(keyStr -> {
            Object keyvalue = originalObject.get(keyStr);
            if (keyvalue instanceof JSONObject) {
                processJSONObject(objectToUpdate,
                        allATDOverrideEnvVars,
                        currentPath,
                        keyStr,
                        (JSONObject) keyvalue);
            } else if (keyvalue instanceof JSONArray) {
                processJSONArray(objectToUpdate,
                        allATDOverrideEnvVars,
                        currentPath,
                        keyStr,
                        (JSONArray) keyvalue);
            } else {
                processJSONObject(objectToUpdate,
                        currentPath,
                        keyStr,
                        keyvalue);
            }
        });
        return objectToUpdate;
    }

    private void processJSONObject(JSONObject objectToUpdate,
                                   StringBuilder currentPath,
                                   String keyStr,
                                   Object keyvalue) {
        currentPath.append(keyStr);
        String getFromEnv = getOverriddenStringValue(currentPath.toString());
        Object updatedValue = (null == getFromEnv) ? keyvalue : getFromEnv;
        objectToUpdate.put(keyStr, updatedValue);
        currentPath.delete(currentPath.lastIndexOf("_") + 1, currentPath.length());
    }

    private void processStringInArrayItem(String objectToUpdate,
                                          JSONArray arrayValues, StringBuilder currentPath,
                                          int arrayIndex) {
        currentPath.append(arrayIndex);
        String getFromEnv = getOverriddenStringValue(currentPath.toString());
        objectToUpdate = (null == getFromEnv) ? objectToUpdate : getFromEnv;
        arrayValues.put(arrayIndex, objectToUpdate);
        currentPath.delete(currentPath.lastIndexOf("_") + 1, currentPath.length());
    }

    private void processJSONArray(JSONObject objectToUpdate,
                                  Map<String, Object> allATDOverrideEnvVars,
                                  StringBuilder currentPath,
                                  String keyStr,
                                  JSONArray keyvalue) {
        JSONArray jsonArray = new JSONArray();
        objectToUpdate.put(keyStr, jsonArray);
        currentPath.append(keyStr).append("_");
        for (int arrIndex = 0; arrIndex < keyvalue.length(); arrIndex++) {
            processJSONArrayItem(allATDOverrideEnvVars,
                    currentPath,
                    jsonArray,
                    keyvalue,
                    arrIndex);
        }
        currentPath.delete(currentPath.lastIndexOf(keyStr), currentPath.length());
    }

    private void processJSONArrayItem(Map<String, Object> allATDOverrideEnvVars,
                                      StringBuilder currentPath,
                                      JSONArray jsonArray,
                                      JSONArray arrayValues,
                                      int arrIndex) {
        Object arrayItem = arrayValues.get(arrIndex);
        currentPath.append(arrIndex).append("_");
        if (arrayItem instanceof JSONObject) {
            JSONObject jsonObject = new JSONObject();
            jsonArray.put(jsonObject);
            loadAndOverrideFromEnvVars((JSONObject) arrayItem,
                    jsonObject,
                    allATDOverrideEnvVars,
                    currentPath);
        } else if (arrayItem instanceof String) {
            processStringInArrayItem(arrayItem.toString(), arrayValues, currentPath, arrIndex);
            jsonArray.put(arrayValues.get(arrIndex));
        }
        currentPath.delete(currentPath.lastIndexOf(String.valueOf(arrIndex)), currentPath.length());
    }

    private void processJSONObject(JSONObject objectToUpdate,
                                   Map<String, Object> allATDOverrideEnvVars,
                                   StringBuilder currentPath,
                                   String keyStr,
                                   JSONObject keyvalue) {
        JSONObject jsonObject = new JSONObject();
        objectToUpdate.put(keyStr, jsonObject);
        currentPath.append(keyStr).append("_");
        loadAndOverrideFromEnvVars(keyvalue, jsonObject, allATDOverrideEnvVars, currentPath);
        currentPath.delete(currentPath.lastIndexOf(keyStr), currentPath.length());
    }

    private String getCapabilityLocation() {
        String path = System.getProperty("user.dir") + "/caps/"
                + "capabilities.json";
        String caps = CAPS.get();
        if (caps != null) {
            Path userDefinedCapsPath = FileSystems.getDefault().getPath(caps);
            if (!userDefinedCapsPath.getParent().isAbsolute()) {
                path = userDefinedCapsPath.normalize()
                        .toAbsolutePath().toString();
            } else {
                path = userDefinedCapsPath.toString();
            }
        }
        return path;
    }


    public JSONObject getCapabilityObjectFromKey(String key) {
        boolean hasKey = getCapabilities().has(key);
        if (hasKey) {
            return (JSONObject) getCapabilities().get(key);
        }
        return null;
    }

    private JSONArray getCapabilitiesArrayFromKey(String key, JSONObject loadedCapabilities) {
        return loadedCapabilities.getJSONArray(key);
    }

    private Boolean getCapabilityBoolean(String key, JSONObject capabilities) {
        if (capabilities.has(key)) {
            return capabilities.getBoolean(key);
        }
        return false;
    }

    public HashMap<String, String> getMongoDbHostAndPort() {
        HashMap<String, String> params = new HashMap<>();
        if (getCapabilities().has("ATDServiceHost")
                && getCapabilities().has("ATDServicePort")) {
            params.put("atdHost", getCapabilities().getString("ATDServiceHost"));
            params.put("atdPort", getCapabilities().getString("ATDServicePort"));
        }
        return params;
    }

    public JSONArray getHostMachineObject() {
        return getCapabilitiesArrayFromKey("hostMachines", capabilities);
    }

    public boolean isApp() {
        return getCapabilityObjectFromKey("iOS").has("app");
    }

    public boolean isWindowsApp() {
        return getCapabilityObjectFromKey("windows").has("app");
    }

    private <T> T appiumServerProp(String host, String arg) {
        JSONArray hostMachineObject = Capabilities.getInstance().getHostMachineObject();
        List<Object> hostIP = hostMachineObject.toList();
        Object machineIP = hostIP.stream().filter(object -> ((Map) object).get("machineIP")
                        .toString().equalsIgnoreCase(host)
                        && ((Map) object).get(arg) != null)
                .findFirst().orElse(null);
        return (T) ((Map) machineIP).get(arg);
    }

    public String getRemoteAppiumManangerPort(String host) {
        try {
            return appiumServerProp(host, "remoteAppiumManagerPort");
        } catch (Exception e) {
            return "4567";
        }
    }

    public JSONObject getCapabilities() {
        return capabilities;
    }

    public void validateCapabilitySchema(JSONObject loadedCapabilities) {
        try {
            isPlatformInEnv();
            LOGGER.debug("Validating capabilities schema: " + loadedCapabilities);
            InputStream inputStream = getClass().getResourceAsStream(getPlatform());
            JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
            Schema schema = SchemaLoader.load(rawSchema);
            schema.validate(loadedCapabilities);
            //validateRemoteHosts(loadedCapabilities);
        } catch (ValidationException e) {
            if (e.getCausingExceptions().size() > 1) {
                e.getCausingExceptions().stream()
                        .map(ValidationException::getMessage)
                        .forEach(System.out::println);
            } else {
                LOGGER.info(e.getErrorMessage());
            }

            throw new ValidationException("Capability json provided is missing the above schema");
        }
    }

    private String getPlatform() {
        String platform = atdEnvironment.get("Platform");
        String schema = null;
        switch (platform.toLowerCase()) {
            case "android":
                schema = "/androidSchema.json";
                break;
            case "ios":
                schema = "/iOSSchema.json";
                break;
            case "windows":
                schema = "/windowsSchema.json";
                break;
            default:
                LOGGER.info("Just for codacy!!");
                break;

        }
        return schema;
    }

    private void isPlatformInEnv() {
        if (atdEnvironment.get("Platform") == null) {
            throw new IllegalArgumentException("Please execute with Platform environment"
                    + ":: Platform=android/ios/both/windows mvn clean -Dtest=Runner test");
        }
    }
}