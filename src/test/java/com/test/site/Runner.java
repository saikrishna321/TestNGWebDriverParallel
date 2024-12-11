package com.test.site;

import com.appium.manager.ATDRunner;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Runner {
    @Test
    public static void testApp() throws Exception {
        List<String> tests = new ArrayList<>();
        tests.add("SliderTest");
        try {
            Path apps = FileSystems.getDefault().getPath("apps").toAbsolutePath();
            ZipFile zipFile = new ZipFile(apps.toFile() + "/WebDriverAgent-Test.zip");
            zipFile.extractAll(apps.toFile().toString());
        } catch (ZipException e) {
            e.printStackTrace();
        }
        ATDRunner atdRunner = new ATDRunner();
        boolean hasFailures = atdRunner.runner("com.test.site", tests);
        Assert.assertFalse(hasFailures, "Testcases have failed in parallel execution");
    }
}
