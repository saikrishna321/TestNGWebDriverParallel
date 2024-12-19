package com.test.site;

import com.appium.manager.AppiumDriverManager;
import org.openqa.selenium.json.JsonOutput;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;



@Test(groups = { "end-to-end-test" })
public class SliderTest extends UserBaseTest {
    @Test
    public void dragNDrop() {
        login("login").click();
        waitForElement("dragAndDrop").click();
        waitForElement("dragMe").click();
    }

    @Test
    public void dragNDrop1() {
        login("login").click();
        waitForElement("dragAndDrop").click();
        waitForElement("dragMe").click();
    }
}
