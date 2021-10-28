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

package org.openmetadata.catalog.selenium.pages.messagingService;

import com.github.javafaker.Faker;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openmetadata.catalog.selenium.properties.Property;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;

import java.time.Duration;
import java.util.ArrayList;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MessagingServicePageTest {
    static WebDriver webDriver;
    static String url = Property.getInstance().getURL();
    Integer waitTime = Property.getInstance().getSleepTime();
    static Faker faker = new Faker();
    static String serviceName = faker.name().firstName();
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
    public void openMessagingServicePage() throws InterruptedException {
        webDriver.findElement(By.cssSelector("[data-testid='closeWhatsNew']")).click(); // Close What's new
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("[data-testid='menu-button'][id='menu-button-Settings']")));
        webDriver.findElement(
                By.cssSelector("[data-testid='menu-button'][id='menu-button-Settings']")).click(); // Setting
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("[data-testid='menu-item-Services']")));
        webDriver.findElement(By.cssSelector("[data-testid='menu-item-Services']")).click(); // Setting/Services
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("(//button[@data-testid='tab'])[2]")));
        webDriver.findElement(By.xpath("(//button[@data-testid='tab'])[2]")).click();
        Thread.sleep(waitTime);
    }

    @Test
    @Order(2)
    public void addMessagingService() throws InterruptedException {
        openMessagingServicePage();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("[data-testid='add-new-user-button']")));
        webDriver.findElement(By.cssSelector("[data-testid='add-new-user-button']")).click();
        webDriver.findElement(By.cssSelector("[value='Kafka']")).click();
        webDriver.findElement(By.cssSelector("[data-testid='name']")).sendKeys(serviceName);
        webDriver.findElement(By.cssSelector("[data-testid='broker-url']")).sendKeys("localhost:9092");
        webDriver.findElement(
                By.cssSelector("[data-testid='schema-registry']")).sendKeys("http://localhost:8081");

        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("[data-testid='boldButton']")));
        webDriver.findElement(By.cssSelector("[data-testid='boldButton']")).click();
        wait.until(ExpectedConditions.elementToBeClickable(
                webDriver.findElement(By.xpath(enterDescription)))).sendKeys(faker.address().toString());
        wait.until(ExpectedConditions.elementToBeClickable(webDriver.findElement(By.xpath(enterDescription)))).click();
        wait.until(ExpectedConditions.elementToBeClickable(
                webDriver.findElement(By.xpath(enterDescription)))).sendKeys(Keys.ENTER);
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("[data-testid='italicButton']")));
        webDriver.findElement(By.cssSelector("[data-testid='italicButton']")).click();
        wait.until(ExpectedConditions.elementToBeClickable(
                webDriver.findElement(By.xpath(enterDescription)))).sendKeys(faker.address().toString());
        wait.until(ExpectedConditions.elementToBeClickable(webDriver.findElement(By.xpath(enterDescription)))).click();
        wait.until(ExpectedConditions.elementToBeClickable(
                webDriver.findElement(By.xpath(enterDescription)))).sendKeys(Keys.ENTER);
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("[data-testid='linkButton']")));
        webDriver.findElement(By.cssSelector("[data-testid='linkButton']")).click();
        wait.until(ExpectedConditions.elementToBeClickable(
                webDriver.findElement(By.xpath(enterDescription)))).sendKeys(faker.address().toString());
        webDriver.findElement(By.cssSelector("[data-testid='ingestion-switch']")).click();
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("[data-testid='save-button']")));
        webDriver.findElement(By.cssSelector("[data-testid='save-button']")).click();
    }

    @Test
    @Order(3)
    public void editMessagingService() throws InterruptedException {
        openMessagingServicePage();
        webDriver.findElement(By.xpath("(//button[@data-testid='edit-service'])[2]")).click();
        wait.until(ExpectedConditions.elementToBeClickable(webDriver.findElement(By.xpath(enterDescription)))).click();
        wait.until(ExpectedConditions.elementToBeClickable(
                webDriver.findElement(By.xpath(enterDescription)))).sendKeys(Keys.ENTER);
        wait.until(ExpectedConditions.elementToBeClickable(
                webDriver.findElement(By.xpath(enterDescription)))).sendKeys(faker.address().toString());
        webDriver.findElement(By.cssSelector("[data-testid='ingestion-switch']")).click();
        webDriver.findElement(By.cssSelector("[data-testid='save-button']")).click();
    }

    @Test
    @Order(4)
    public void checkMessagingServiceDetails() throws InterruptedException {
        openMessagingServicePage();
        webDriver.findElement(By.xpath("(//h6[@data-testid='service-name'])[2]")).click();
        Thread.sleep(waitTime);
        webDriver.findElement(By.cssSelector("[data-testid='description-edit']")).click();
        wait.until(ExpectedConditions.elementToBeClickable(webDriver.findElement(By.xpath(enterDescription)))).click();
        wait.until(ExpectedConditions.elementToBeClickable(
                webDriver.findElement(By.xpath(enterDescription)))).sendKeys(Keys.ENTER);
        wait.until(ExpectedConditions.elementToBeClickable(
                webDriver.findElement(By.xpath(enterDescription)))).sendKeys(faker.address().toString());
        webDriver.findElement(By.cssSelector("[data-testid='save']")).click();
    }

    @Test
    @Order(5)
    public void searchMessagingService() throws InterruptedException {
        openMessagingServicePage();
        webDriver.findElement(By.cssSelector("[data-testid='searchbar']")).sendKeys(serviceName);
        webDriver.findElement(By.cssSelector("[data-testid='service-name']")).click();
    }

    @Test
    @Order(6)
    public void deleteMessagingService() throws InterruptedException {
        openMessagingServicePage();
        webDriver.findElement(By.cssSelector("[data-testid='searchbar']")).sendKeys(serviceName);
        webDriver.findElement(By.xpath("(//button[@data-testid='delete-service'])[2]")).click();
        webDriver.findElement(By.cssSelector("[data-testid='save-button']")).click();
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
