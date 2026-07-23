package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.util.List;

/**
 * Hepsiburada ana sayfası ve üst menüsündeki kullanıcı işlemlerini temsil eder.
 * Page Object Model sayesinde locator'lar ve sayfa davranışları test adımlarından ayrılır.
 */
public class HomePage extends BasePage {

    // Kısa süre opsiyonel alanlarda, uzun süre giriş gibi yavaş işlemlerde kullanılır.
    private static final int SHORT_TIMEOUT_SECONDS = 5;
    private static final int LONG_TIMEOUT_SECONDS = 20;

    // By nesneleri, sayfadaki elementlerin id/CSS özellikleriyle adresleridir.
    private static final By COOKIE_HOST = By.cssSelector("efilli-layout-dynamic");
    private static final By ACCOUNT_AREA = By.cssSelector("a[title='Hesabım'], span[title='Giriş Yap']");
    private static final By LOGIN_LINK = By.id("login");
    private static final By SEARCH_BOX = By.cssSelector("input[data-test-id='search-bar-input']");
    private static final By RESULT_HEADING = By.cssSelector("h1[data-test-id='header-h1']");
    private static final By LOGGED_IN_ACCOUNT = By.cssSelector("a[title='Hesabım']");
    private static final By CART_LINK = By.cssSelector("a[href*='checkout.hepsiburada.com/sepetim']");
    private static final By CART_COUNT = By.id("cartItemCount");

    /** Aktif tarayıcıyı ortak BasePage işlemlerine iletir. */
    public HomePage(WebDriver driver) {
        super(driver);
    }

    /** Varsa Shadow DOM içinde açılan çerez bildirimini kabul edip kapatır. */
    public void closeCookieBanner() {
        try {
            // Shadow DOM, ana HTML ağacından ayrı kapsüllenmiş bir element ağacıdır.
            WebElement host = present(COOKIE_HOST, SHORT_TIMEOUT_SECONDS);
            SearchContext shadowRoot = host.getShadowRoot();

            // Shadow root içindeki olası buton/div elementleri yazılarına göre taranır.
            for (WebElement element : shadowRoot.findElements(By.cssSelector("button, div"))) {
                if ("Kabul Et".equalsIgnoreCase(element.getText().trim())) {
                    javascriptClick(element);
                    break;
                }
            }
        } catch (Exception ignored) {
            // Banner her açılışta görünmeyebilir.
        }
    }

    /** Hesap alanının üstüne gelir ve menüde oluşan giriş bağlantısına tıklar. */
    public void openLoginPage() {
        // Actions.moveToElement gerçek kullanıcının fareyle hover yapmasını taklit eder.
        new Actions(driver).moveToElement(visible(ACCOUNT_AREA)).perform();
        pauseBetweenActions();
        click(LOGIN_LINK);
    }

    /** Arama kutusunu etkinleştirir, aranan metni yazar ve Enter'a basar. */
    public void searchFor(String text) {
        WebElement collapsedSearchBox = visible(SEARCH_BOX);

        // Hepsiburada'nın arama bileşeninde input'un üstünde tıklamayı yakalayan
        // bir katman bulunabiliyor. Tıklama sonrasında site eski input'u DOM'dan
        // kaldırıp genişletilmiş yeni bir input oluşturuyor.
        scrollTo(collapsedSearchBox);
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].focus(); arguments[0].click();", collapsedSearchBox);

        try {
            // React eski input'u yenisiyle değiştirirse eski WebElement stale olur.
            until(SHORT_TIMEOUT_SECONDS, ignored -> {
                try {
                    collapsedSearchBox.isDisplayed();
                    return false;
                } catch (StaleElementReferenceException e) {
                    return true;
                }
            });
        } catch (TimeoutException ignored) {
            // Bileşen input'u değiştirmediyse mevcut locator ile devam edilir.
        }

        // React bileşeni yazma başlarken input'u tekrar oluşturabildiği için
        // WebElement referansına sendKeys göndermek yeniden stale hatası üretir.
        // Klavye olaylarını o anda odakta olan input'a göndererek DOM yenilemesinden
        // bağımsız ilerliyoruz.
        // JavaScript ile odaktaki elementin doğru arama input'u olduğu doğrulanır.
        until(DEFAULT_TIMEOUT_SECONDS, ignored ->
                (Boolean) ((JavascriptExecutor) driver).executeScript(
                        "return document.activeElement != null && " +
                                "document.activeElement.matches(\"input[data-test-id='search-bar-input']\");"));

        pauseBetweenActions();
        // Arama alanında eski değer varsa Ctrl+A ile tamamı seçilir.
        new Actions(driver)
                .keyDown(Keys.CONTROL)
                .sendKeys("a")
                .keyUp(Keys.CONTROL)
                .perform();
        typeIntoActiveElementLikeUser(text);
        pauseBetweenActions();
        // Enter tuşu arama formunu gönderir.
        new Actions(driver).sendKeys(Keys.ENTER).perform();
    }

    /** Girişten sonra hesap alanı görünüyorsa true, zaman aşımı olursa false döndürür. */
    public boolean loginCompleted() {
        try {
            return visible(LOGGED_IN_ACCOUNT, LONG_TIMEOUT_SECONDS).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /** Arama sonuç sayfasındaki ana başlığı okur. */
    public String searchHeading() {
        return visible(RESULT_HEADING).getText();
    }

    /** Üst menüdeki mevcut sepet ürün sayısını döndürür. */
    public int cartCount() {
        visible(CART_LINK);
        return readCartCount();
    }

    /** Sepet sayacı beklenen değere ulaşana kadar bekler ve son değeri döndürür. */
    public int waitForCartCount(int expectedCount) {
        try {
            return until(DEFAULT_TIMEOUT_SECONDS, ignored -> {
                int currentCount = readCartCount();
                // until koşulunda null, beklemeye devam et anlamına gelir.
                return currentCount == expectedCount ? currentCount : null;
            });
        } catch (TimeoutException e) {
            return readCartCount();
        }
    }

    /** Sepet bağlantısına tıklar ve sepet URL'sinin açıldığını doğrular. */
    public void goToCart() {
        WebElement cartLink = visible(CART_LINK);
        try {
            cartLink.click();
        } catch (ElementClickInterceptedException e) {
            // Başka bir katman normal tıklamayı keserse JavaScript tıklaması denenir.
            javascriptClick(cartLink);
        }

        waitUntilUrlContains("sepetim", DEFAULT_TIMEOUT_SECONDS);
    }

    /** Sayaç metnini temizleyip int türünde sayıya dönüştürür. */
    private int readCartCount() {
        List<WebElement> counters = driver.findElements(CART_COUNT);
        // Element yoksa site boş sepet için sayaç göstermiyor kabul edilir.
        if (counters.isEmpty()) {
            return 0;
        }

        WebElement counter = counters.get(0);
        String counterText = counter.getText().trim();
        if (counterText.isBlank()) {
            // getText boşsa DOM'daki ham textContent niteliği yedek olarak okunur.
            String textContent = counter.getAttribute("textContent");
            counterText = textContent == null ? "" : textContent.trim();
        }

        if (counterText.isBlank()) {
            return 0;
        }

        // Rakam dışındaki rozet yazıları/boşluklar atılır.
        String numericValue = counterText.replaceAll("[^0-9]", "");
        if (numericValue.isBlank()) {
            throw new IllegalStateException(
                    "Sepet sayacı sayıya dönüştürülemedi: " + counterText);
        }

        // String türündeki rakamları int türüne çevirir.
        return Integer.parseInt(numericValue);
    }
}
