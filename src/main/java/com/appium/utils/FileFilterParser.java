package com.appium.utils;


import com.appium.manager.AppiumDeviceManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.testng.ITestResult;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileFilterParser extends Helpers {

    private Map<String, List<String>> getFilesFromDirectory(String directoryLocation,
                                                            String[] fileTypes,
                                                            String udid,
                                                            ITestResult iTestResult)
            throws Exception {
        File file = new File(directoryLocation);
        if (!file.exists())
            throw new Exception(directoryLocation + " does not exist");
        if (!file.isDirectory())
            throw new Exception(directoryLocation + " is not a directory");

        FileFilter filter = inputFile -> {
            if (inputFile.isDirectory()) return true;
            for (String fileType : fileTypes) {
                if (inputFile.getAbsolutePath().toLowerCase().endsWith(fileType))
                    return true;
            }
            return false;
        };

        HashMap<String, List<String>> results = new HashMap<>();
        getFilesFromDirectory(file, filter, results, udid, iTestResult);
        return results;
    }

    private void getFilesFromDirectory(File inputDirectory, FileFilter filter, Map<String,
            List<String>> results, String deviceUDID, ITestResult iTestResult) {
        File[] files = inputDirectory.listFiles(filter);
        for (File file : files) {
            if (file.isDirectory()) {
                getFilesFromDirectory(file, filter, results, deviceUDID, iTestResult);
            } else {
                if (file.getAbsolutePath().contains(deviceUDID)
                        && file.getAbsolutePath().contains(iTestResult
                        .getMethod().getMethodName())) {
                    if (!results.containsKey(inputDirectory.getName())) {
                        results.put(inputDirectory.getName(), new ArrayList<>());
                    }
                    List<String> values = results.get(inputDirectory.getName());
                    values.add(file.getAbsolutePath());
                    results.put(inputDirectory.getName(), values);
                }
            }
        }
    }

    public JSONObject getScreenShotPaths(String udid, ITestResult iTestResult) {
        String directoryLocation = System.getProperty("user.dir") + "/target/screenshot";
        String[] fileTypes = {"png", "jpeg"};

        FileFilterParser fileFilterParser = new FileFilterParser();
        Map<String, List<String>> filesFromDirectory = null;
        try {
            filesFromDirectory = fileFilterParser.getFilesFromDirectory(directoryLocation,
                    fileTypes, udid, iTestResult);
        } catch (Exception e) {
            e.printStackTrace();
        }

        JSONObject obj = new JSONObject();
        filesFromDirectory.forEach((key, values) -> {
            JSONArray list = new JSONArray();
            values.forEach(s -> {
                if (s.contains("results")) {
                    String path = s.split("target")[1];
                    try {
                        list.add("http://" + getHostMachineIpAddress() + ":"
                                + getRemoteAppiumManagerPort("127.0.0.1") + path);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    System.out.print(s + ", ");
                }
            });
            if (list.size() > 0)
                obj.put(key, list);
        });
        return obj;
    }
}