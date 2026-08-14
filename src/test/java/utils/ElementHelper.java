package utils;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Predicate;

/** Bütün Page sınıflarının kullandığı ortak Selenium fonksiyonlarını tek yerde tutar. */
public class ElementHelper {

    protected static final int DEFAULT_TIMEOUT_SECONDS = 10;

    private static final int MIN_TYPING_DELAY_MS = 40;
    private static final int MAX_TYPING_DELAY_MS = 70;
    private static final int MIN_ACTION_DELAY_MS = 150;
    private static final int MAX_ACTION_DELAY_MS = 250;

    protected final WebDriver driver;

    private final String locatorFile;
    private final WebDriverWait defaultWait;

    protected ElementHelper(WebDriver driver, String locatorFile) {
        this.driver = driver;
        this.locatorFile = locatorFile;
        this.defaultWait = createWait(DEFAULT_TIMEOUT_SECONDS);
    }

    /** Page sınıfında yalnız anahtar kullanılarak ilgili JSON locator'ına erişilmesini sağlar. */
    private By locator(String key) {
        return LocatorReader.getLocator(locatorFile, key);
    }

    protected WebElement visible(String key) {
        return defaultWait.until(ExpectedConditions.visibilityOfElementLocated(locator(key)));
    }

    protected WebElement present(String key, int seconds) {
        return createWait(seconds).until(
                ExpectedConditions.presenceOfElementLocated(locator(key)));
    }

    protected WebElement clickable(String key) {
        return defaultWait.until(ExpectedConditions.elementToBeClickable(locator(key)));
    }

    protected List<WebElement> findElements(String key) {
        return driver.findElements(locator(key));
    }

    protected List<WebElement> findElements(SearchContext context, String key) {
        return context.findElements(locator(key));
    }

    protected WebElement findElement(SearchContext context, String key) {
        return context.findElement(locator(key));
    }

    protected void click(String key) {
        click(clickable(key));
    }

    protected void click(WebElement element) {
        try {
            element.click();
        } catch (ElementClickInterceptedException exception) {
            javascriptClick(element);
        }
    }

    protected void typeLikeUser(String key, String value) {
        WebElement input = clickable(key);
        input.click();
        input.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        input.sendKeys(Keys.BACK_SPACE);
        typeIntoActiveElementLikeUser(value);
        defaultWait.until(ExpectedConditions.attributeToBe(locator(key), "value", value));
    }

    protected <T> T until(int seconds, Function<WebDriver, T> condition) {
        return createWait(seconds).until(condition);
    }

    protected boolean waitUntilTrue(int seconds, Predicate<WebDriver> condition) {
        try {
            until(seconds, currentDriver -> condition.test(currentDriver));
            return true;
        } catch (TimeoutException exception) {
            return false;
        }
    }

    protected void waitUntilUrlContains(String text, int seconds) {
        createWait(seconds).until(ExpectedConditions.urlContains(text));
    }

    private void typeIntoActiveElementLikeUser(String value) {
        for (char character : value.toCharArray()) {
            new Actions(driver).sendKeys(String.valueOf(character)).perform();
            pause(MIN_TYPING_DELAY_MS, MAX_TYPING_DELAY_MS);
        }
    }

    protected void pauseBetweenActions() {
        pause(MIN_ACTION_DELAY_MS, MAX_ACTION_DELAY_MS);
    }

    protected void javascriptClick(WebElement element) {
        scrollTo(element);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }

    protected void scrollTo(WebElement element) {
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center'});", element);
    }

    protected void scrollDown(int pixel) {
        ((JavascriptExecutor) driver).executeScript(
                "window.scrollBy(0, arguments[0]);", pixel);
    }

    private WebDriverWait createWait(int seconds) {
        return new WebDriverWait(driver, Duration.ofSeconds(seconds));
    }

    private void pause(int minMilliseconds, int maxMilliseconds) {
        int delay = ThreadLocalRandom.current().nextInt(
                minMilliseconds, maxMilliseconds + 1);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Test beklemesi kesintiye uğradı.", exception);
        }
    }
}
