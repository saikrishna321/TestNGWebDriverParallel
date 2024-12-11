package com.test.site;

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
