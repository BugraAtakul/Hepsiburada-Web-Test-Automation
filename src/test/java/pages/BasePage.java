package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

/**
 * Bütün Page Object sınıflarının kullandığı ortak Selenium işlemlerini barındırır.
 *
 * <p>Abstract olduğu için doğrudan oluşturulamaz; HomePage ve LoginPage gibi
 * gerçek sayfa sınıfları tarafından miras alınır. Böylece bekleme, tıklama ve
 * yazma kodları her sayfa sınıfında tekrar edilmez.</p>
 */
public abstract class BasePage {

    // Çoğu element işleminin en fazla kaç saniye bekleneceğini belirler.
    protected static final int DEFAULT_TIMEOUT_SECONDS = 10;

    // Yazma ve adım gecikmeleri, etkileşimlerin insan hızına yakın ilerlemesini sağlar.
    private static final int MIN_TYPING_DELAY_MS = 40;
    private static final int MAX_TYPING_DELAY_MS = 70;
    private static final int MIN_ACTION_DELAY_MS = 150;
    private static final int MAX_ACTION_DELAY_MS = 250;

    // protected: alt Page Object sınıfları driver'a erişebilir.
    protected final WebDriver driver;

    // final: nesne oluşturulduktan sonra başka bir bekleme nesnesi atanamaz.
    private final WebDriverWait defaultWait;

    /** Alt sınıftan gelen WebDriver'ı kaydeder ve varsayılan açık beklemeyi oluşturur. */
    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.defaultWait = createWait(DEFAULT_TIMEOUT_SECONDS);
    }

    /** Locator ile bulunan tek element görünür olana kadar bekler. */
    protected WebElement visible(By locator) {
        return defaultWait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /** Varsayılandan farklı süreyle bir elementin görünmesini bekler. */
    protected WebElement visible(By locator, int seconds) {
        return createWait(seconds).until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /** Elementin görünmesi gerekmeksizin HTML DOM içinde oluşmasını bekler. */
    protected WebElement present(By locator, int seconds) {
        return createWait(seconds).until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    /** Element hem görünür hem tıklanabilir olana kadar bekler. */
    protected WebElement clickable(By locator) {
        return defaultWait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    /** Locator'a uyan bütün elementlerin görünmesini bekleyip liste döndürür. */
    protected List<WebElement> visibleElements(By locator) {
        return defaultWait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
    }

    /** Element hazır olduğunda normal Selenium tıklaması yapar. */
    protected void click(By locator) {
        clickable(locator).click();
    }

    /** Input'u temizler, değeri karakter karakter yazar ve son değeri doğrular. */
    protected void typeLikeUser(By locator, String value) {
        WebElement input = clickable(locator);
        input.click();
        // Ctrl+A mevcut metnin tamamını seçer; Backspace seçilen metni siler.
        input.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        input.sendKeys(Keys.BACK_SPACE);
        typeIntoActiveElementLikeUser(value);
        // Yazılan değerin gerçekten input'un value niteliğine geçtiğini bekler.
        defaultWait.until(ExpectedConditions.attributeToBe(locator, "value", value));
    }

    /** Lambda ile verilen özel koşulu belirtilen süre boyunca kontrol eder. */
    protected <T> T until(int seconds, Function<WebDriver, T> condition) {
        return createWait(seconds).until(condition);
    }

    /** Tarayıcı adresinde beklenen metin görünene kadar bekler. */
    protected void waitUntilUrlContains(String text, int seconds) {
        createWait(seconds).until(ExpectedConditions.urlContains(text));
    }

    /** Metni odaktaki elemente insan benzeri aralıklarla karakter karakter yazar. */
    protected void typeIntoActiveElementLikeUser(String value) {
        for (char character : value.toCharArray()) {
            new Actions(driver).sendKeys(String.valueOf(character)).perform();
            pause(MIN_TYPING_DELAY_MS, MAX_TYPING_DELAY_MS);
        }
    }

    /** İki kullanıcı işlemi arasına kısa ve rastgele bir bekleme ekler. */
    protected void pauseBetweenActions() {
        pause(MIN_ACTION_DELAY_MS, MAX_ACTION_DELAY_MS);
    }

    /** Normal click engellendiğinde elementi JavaScript ile tıklar. */
    protected void javascriptClick(WebElement element) {
        scrollTo(element);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }

    /** Elementi sayfanın görünür alanının ortasına kaydırır. */
    protected void scrollTo(WebElement element) {
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center'});", element);
    }

    /** Sayfayı dikey eksende verilen piksel kadar aşağı kaydırır. */
    protected void scrollDown(int pixel) {
        ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, arguments[0]);", pixel);
    }

    /** Saniye cinsinden süreyle explicit wait (açık bekleme) üretir. */
    private WebDriverWait createWait(int seconds) {
        return new WebDriverWait(driver, Duration.ofSeconds(seconds));
    }

    /** Verilen alt ve üst sınırlar arasında rastgele süre bekler. */
    private void pause(int minMilliseconds, int maxMilliseconds) {
        int delay = ThreadLocalRandom.current().nextInt(
                minMilliseconds, maxMilliseconds + 1);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            // Kesilme bilgisini kaybetmemek için thread'in interrupt bayrağını yeniden işaretler.
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Test beklemesi kesintiye uğradı.", e);
        }
    }
}
