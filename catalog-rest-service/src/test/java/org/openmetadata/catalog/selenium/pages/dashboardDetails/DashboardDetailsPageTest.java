/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements. See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.openmetadata.catalog.selenium.pages.dashboardDetails;

import com.github.javafaker.Faker;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.openmetadata.catalog.selenium.properties.Property;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DashboardDetailsPageTest {
    static WebDriver webDriver;
    static String url = Property.getInstance().getURL();
    Integer waitTime = Property.getInstance().getSleepTime();
    static Faker faker = new Faker();
    String dashboardName = "Misc Charts";
    static String enterDescription = "//div[@data-testid='enterDescription']/div/div[2]/div/div/div/div/div/div";
    static Actions actions;
    static WebDriverWait wait;

    @BeforeEach
    public void openMetadataWindow() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        webDriver = new ChromeDriver(options);
        actions = new Actions(webDriver);
        wait = new WebDriverWait(webDriver, Duration.ofSeconds(5));
        webDriver.manage().window().maximize();
        webDriver.get(url);
    }

    @Test
    @Order(1)
    public void openExplorePage() throws InterruptedException {
        webDriver.findElement(By.cssSelector("[data-testid='closeWhatsNew']")).click(); // Close What's new
        webDriver.findElement(By.cssSelector("[data-testid='appbar-item'][id='explore']")).click(); // Explore
        webDriver.findElement(By.xpath("(//button[@data-testid='tab'])[3]")).click(); // Dashboard
        Thread.sleep(waitTime);
    }

    @Test
    @Order(2)
    public void editDescription() throws InterruptedException {
        openExplorePage();
        webDriver.findElement(By.cssSelector("[data-testid='sortBy']")).click(); // Sort By
        webDriver.findElement(By.cssSelector("[data-testid='list-item']")).click(); // Last Updated
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("(//a[@data-testid='table-link'])[last()]")));
        webDriver.findElement(By.xpath("(//a[@data-testid='table-link'])[last()]")).click();
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("[data-testid='edit-description']")));
        webDriver.findElement(By.cssSelector("[data-testid='edit-description']")).click();
        webDriver.findElement(By.xpath(enterDescription)).sendKeys(faker.address().toString());
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("[data-testid='save']")));
        webDriver.findElement(By.cssSelector("[data-testid='save']")).click();
    }

    @Test
    @Order(3)
    public void addTag() throws InterruptedException {
        openExplorePage();
        webDriver.findElement(By.cssSelector("[data-testid='sortBy']")).click(); // Sort By
        webDriver.findElement(By.cssSelector("[data-testid='list-item']")).click(); // Last Updated
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("(//a[@data-testid='table-link'])[last()]")));
        webDriver.findElement(By.xpath("(//a[@data-testid='table-link'])[last()]")).click();
        Thread.sleep(waitTime);
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("[data-testid='tags']")));
        webDriver.findElement(By.cssSelector("[data-testid='tags']")).click();
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("[data-testid='associatedTagName']")));
        webDriver.findElement(By.cssSelector("[data-testid='associatedTagName']")).click();
        for (int i = 1; i <=2; i++){
            wait.until(ExpectedConditions.elementToBeClickable(
                            webDriver.findElement(By.cssSelector("[data-testid='associatedTagName']"))))
                    .sendKeys("P");
            wait.until(ExpectedConditions.elementToBeClickable(
                    webDriver.findElement(By.cssSelector("[data-testid='list-item']")))).click();
        }
        webDriver.findElement(By.cssSelector("[data-testid='saveAssociatedTag']")).click();
        webDriver.navigate().back();
        webDriver.navigate().refresh();
        Thread.sleep(2000);
//        wait.until(ExpectedConditions.elementToBeClickable(
//                By.cssSelector("[data-testid='checkbox'][id='PersonalData.Personal']")));
        webDriver.findElement(By.cssSelector("[data-testid='checkbox'][id='PersonalData.Personal']")).click();
    }

    @Test
    @Order(4)
    public void removeTag() throws InterruptedException {
        openExplorePage();
        webDriver.findElement(By.cssSelector("[data-testid='sortBy']")).click(); // Sort By
        webDriver.findElement(By.cssSelector("[data-testid='list-item']")).click(); // Last Updated
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("(//a[@data-testid='table-link'])[1]")));
        webDriver.findElement(By.xpath("(//a[@data-testid='table-link'])[1]")).click();
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("[data-testid='tag-conatiner']")));
        webDriver.findElement(By.cssSelector("[data-testid='tag-conatiner']")).click();
        wait.until(ExpectedConditions.elementToBeClickable(
                webDriver.findElement(By.cssSelector("[data-testid='remove']")))).click();
        wait.until(ExpectedConditions.elementToBeClickable(
                webDriver.findElement(By.cssSelector("[data-testid='remove']")))).click();
        wait.until(ExpectedConditions.elementToBeClickable(
                webDriver.findElement(By.cssSelector("[data-testid='saveAssociatedTag']")))).click();
    }

    @Test
    @Order(5)
    public void editChartDescription() throws InterruptedException {
        openExplorePage();
        webDriver.findElement(By.cssSelector("[data-testid='sortBy']")).click(); // Sort By
        webDriver.findElement(By.cssSelector("[data-testid='list-item']")).click(); // Last Updated
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("(//a[@data-testid='table-link'])[last()]")));
        webDriver.findElement(By.xpath("(//a[@data-testid='table-link'])[last()]")).click();
        Thread.sleep(waitTime);
        actions.moveToElement(webDriver.findElement(By.xpath("//div[@data-testid='description']/button"))).perform();
        webDriver.findElement(By.xpath("//div[@data-testid='description']/button")).click();
        webDriver.findElement(By.xpath(enterDescription)).sendKeys(faker.address().toString());
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("[data-testid='save']")));
    }

    @Test
    @Order(6)
    public void addChartTags() throws InterruptedException {
        openExplorePage();
        webDriver.findElement(By.cssSelector("[data-testid='searchBox']")).sendKeys(dashboardName);
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("[data-testid='data-name']")));
        webDriver.findElement(By.cssSelector("[data-testid='data-name']")).click();
        Thread.sleep(waitTime);
        actions.moveToElement(webDriver.findElement(
                By.xpath("//table[@data-testid='schema-table']//div[@data-testid='tag-conatiner']//span"))).perform();
        webDriver.findElement(
                By.xpath("//table[@data-testid='schema-table']//div[@data-testid='tag-conatiner']//span")).click();
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("[data-testid='associatedTagName']")));
        webDriver.findElement(By.cssSelector("[data-testid='associatedTagName']")).click();
        for (int i = 0; i <=1; i++){
            wait.until(ExpectedConditions.elementToBeClickable(
                            webDriver.findElement(By.cssSelector("[data-testid='associatedTagName']"))))
                    .sendKeys("P");
            wait.until(ExpectedConditions.elementToBeClickable(
                    webDriver.findElement(By.cssSelector("[data-testid='list-item']")))).click();
        }
        webDriver.findElement(By.cssSelector("[data-testid='saveAssociatedTag']")).click();
    }

    @Test
    @Order(7)
    public void removeChartTag() throws InterruptedException {
        openExplorePage();
        webDriver.findElement(By.cssSelector("[data-testid='searchBox']")).sendKeys(dashboardName);
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("[data-testid='data-name']")));
        webDriver.findElement(By.cssSelector("[data-testid='data-name']")).click();
        Thread.sleep(waitTime);
        wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//table[@data-testid='schema-table']//div[@data-testid='tag-conatiner']//span")));
        actions.moveToElement(webDriver.findElement(
                By.xpath("//table[@data-testid='schema-table']//div[@data-testid='tag-conatiner']//span"))).perform();
        webDriver.findElement(
                By.xpath("//table[@data-testid='schema-table']//div[@data-testid='tag-conatiner']//span")).click();
        wait.until(ExpectedConditions.elementToBeClickable(
                webDriver.findElement(By.cssSelector("[data-testid='remove']")))).click();
        wait.until(ExpectedConditions.elementToBeClickable(
                webDriver.findElement(By.cssSelector("[data-testid='remove']")))).click();
        wait.until(ExpectedConditions.elementToBeClickable(
                webDriver.findElement(By.cssSelector("[data-testid='saveAssociatedTag']")))).click();
    }

    @Test
    @Order(8)
    public void checkManage() throws InterruptedException {
        openExplorePage();
        webDriver.findElement(By.cssSelector("[data-testid='sortBy']")).click(); // Sort By
        webDriver.findElement(By.cssSelector("[data-testid='list-item']")).click(); // Last Updated
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("(//a[@data-testid='table-link'])[last()]")));
        webDriver.findElement(By.xpath("(//a[@data-testid='table-link'])[last()]")).click();
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("(//button[@data-testid='tab'])[2]"))); // Manage
        webDriver.findElement(By.xpath("(//button[@data-testid='tab'])[2]")).click();
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("[data-testid='owner-dropdown']"))); // Owner
        webDriver.findElement(By.cssSelector("[data-testid='owner-dropdown']")).click(); // Owner
        wait.until(ExpectedConditions.elementToBeClickable(
                webDriver.findElement(By.cssSelector("[data-testid='searchInputText']"))));
        webDriver.findElement(By.cssSelector("[data-testid='searchInputText']")).sendKeys("Cloud");
        webDriver.findElement(By.cssSelector("[data-testid='list-item']")).click(); // Select User/Team
        webDriver.findElement(By.cssSelector("[data-testid='card-list']")).click(); // Select Tier
        webDriver.findElement(By.cssSelector("[data-testid='saveManageTab']")).click(); // Save
//        webDriver.findElement(By.cssSelector("[data-testid='appbar-item'][id='explore']")).click(); // Explore
//        webDriver.findElement(By.xpath("(//button[@data-testid='tab'])[3]")).click(); // Topics
        webDriver.navigate().back();
        webDriver.navigate().refresh();
        Thread.sleep(waitTime);
        wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("[data-testid='checkbox'][id='Tier.Tier1']")));
        webDriver.findElement(By.cssSelector("[data-testid='checkbox'][id='Tier.Tier1']")).click();
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("[data-testid='table-link']")));
        webDriver.findElement(By.cssSelector("[data-testid='table-link']")).click();
    }

    @Test
    @Order(9)
    public void checkBreadCrumb() throws InterruptedException {
        openExplorePage();
        webDriver.findElement(By.cssSelector("[data-testid='searchBox']")).sendKeys(dashboardName);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("[data-testid='data-name']")));
        webDriver.findElement(By.cssSelector("[data-testid='data-name']")).click();
        Thread.sleep(waitTime);
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("[data-testid='breadcrumb-link']")));
        webDriver.findElement(By.cssSelector("[data-testid='breadcrumb-link']")).click();
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("[data-testid='description-edit']")));
        webDriver.findElement(By.cssSelector("[data-testid='description-edit']")).click(); // edit description
        webDriver.findElement(By.xpath(enterDescription)).sendKeys(faker.address().toString());
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("[data-testid='save']")));
        webDriver.findElement(By.cssSelector("[data-testid='save']")).click();
        for (int i = 1; i <= 3; i++) { //check topics in service
            wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("(//tr[@data-testid='column']//td[1]/a)" + "[" + i + "]")));
            webDriver.findElement(
                    By.xpath("(//tr[@data-testid='column']//td[1]/a)" + "[" + i + "]")).click(); // dashboards
            Thread.sleep(waitTime);
            webDriver.navigate().back();
        }
    }

    @AfterEach
    public void closeTabs() {
        ArrayList<String> tabs = new ArrayList<>(webDriver.getWindowHandles());
        String originalHandle = webDriver.getWindowHandle();
        for (String handle : webDriver.getWindowHandles()) {
            if (!handle.equals(originalHandle)) {
                webDriver.switchTo().window(handle);
                webDriver.close();
            }
        }
        webDriver.switchTo().window(tabs.get(0)).close();
    }
}
