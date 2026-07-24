package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
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
    private static final int SEARCH_TEXT_ATTEMPTS = 3;

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
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Arama metni boş olamaz.");
        }

        WebElement collapsedSearchBox = visible(SEARCH_BOX);

        // Hepsiburada'nın arama bileşeninde input'un üstünde tıklamayı yakalayan
        // bir katman bulunabiliyor. Tıklama sonrasında site eski input'u DOM'dan
        // kaldırıp genişletilmiş yeni bir input oluşturuyor.
        scrollTo(collapsedSearchBox);
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].focus(); arguments[0].click();", collapsedSearchBox);

        // Genişleyen React input'u sabitlendikten sonra metni o elementin kendisine yazar.
        // Son değer tam eşleşmezse Enter'a basmadan güncel input ile yeniden dener.
        WebElement verifiedSearchBox = enterVerifiedSearchText(text);
        pauseBetweenActions();
        verifiedSearchBox.sendKeys(Keys.ENTER);
    }

    /**
     * Arama metnini güncel input'a yazar ve değer kısa süre kararlı biçimde tam
     * eşleşene kadar doğrular. React elementi yenilerse işlem baştan denenir.
     */
    private WebElement enterVerifiedSearchText(String text) {
        RuntimeException lastFailure = null;

        for (int attempt = 1; attempt <= SEARCH_TEXT_ATTEMPTS; attempt++) {
            try {
                WebElement searchBox = waitForStableSearchBox();
                searchBox.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE);

                // Temizleme işlemi de input'u yeniden oluşturabileceği için boş değeri
                // doğrulayıp elementi tekrar locator üzerinden alır.
                waitForStableSearchValue("");
                searchBox = waitForStableSearchBox();
                searchBox.sendKeys(text);

                return waitForStableSearchValue(text);
            } catch (StaleElementReferenceException
                     | ElementNotInteractableException
                     | TimeoutException exception) {
                lastFailure = exception;
            }
        }

        // Native tuş olayları üç kez de React yeniden çizimine denk gelirse son
        // çare olarak native input setter ve gerçek input/change olayları kullanılır.
        return setSearchValueWithJavaScript(text, lastFailure);
    }

    /** Görünür arama input'u iki ardışık kontrolde aynı kalana kadar bekler. */
    private WebElement waitForStableSearchBox() {
        WebElement[] previousSearchBox = new WebElement[1];
        int[] consecutiveMatches = {0};

        return until(DEFAULT_TIMEOUT_SECONDS, currentDriver -> {
            try {
                WebElement currentSearchBox = findUsableSearchBox(currentDriver);
                if (currentSearchBox == null) {
                    previousSearchBox[0] = null;
                    consecutiveMatches[0] = 0;
                    return null;
                }

                ((JavascriptExecutor) currentDriver)
                        .executeScript("arguments[0].focus();", currentSearchBox);

                if (currentSearchBox.equals(previousSearchBox[0])) {
                    consecutiveMatches[0]++;
                } else {
                    previousSearchBox[0] = currentSearchBox;
                    consecutiveMatches[0] = 1;
                }

                return consecutiveMatches[0] >= 2 ? currentSearchBox : null;
            } catch (StaleElementReferenceException exception) {
                previousSearchBox[0] = null;
                consecutiveMatches[0] = 0;
                return null;
            }
        });
    }

    /** Input değerinin iki ardışık kontrolde beklenen metne tam eşit olduğunu doğrular. */
    private WebElement waitForStableSearchValue(String expectedValue) {
        WebElement[] previousSearchBox = new WebElement[1];
        int[] consecutiveMatches = {0};

        return until(SHORT_TIMEOUT_SECONDS, currentDriver -> {
            try {
                WebElement currentSearchBox = findUsableSearchBox(currentDriver);
                String currentValue = currentSearchBox == null
                        ? null
                        : currentSearchBox.getDomProperty("value");

                if (!expectedValue.equals(currentValue)) {
                    previousSearchBox[0] = null;
                    consecutiveMatches[0] = 0;
                    return null;
                }

                if (currentSearchBox.equals(previousSearchBox[0])) {
                    consecutiveMatches[0]++;
                } else {
                    previousSearchBox[0] = currentSearchBox;
                    consecutiveMatches[0] = 1;
                }

                return consecutiveMatches[0] >= 2 ? currentSearchBox : null;
            } catch (StaleElementReferenceException exception) {
                previousSearchBox[0] = null;
                consecutiveMatches[0] = 0;
                return null;
            }
        });
    }

    /**
     * Aynı locator'a uyan birden fazla input varsa odakta olanı; yoksa görünür
     * ve etkin adayların sonuncusunu seçer. Genişletilmiş input DOM'a sonradan eklenir.
     */
    private WebElement findUsableSearchBox(WebDriver currentDriver) {
        WebElement fallback = null;

        for (WebElement candidate : currentDriver.findElements(SEARCH_BOX)) {
            try {
                if (!candidate.isDisplayed() || !candidate.isEnabled()) {
                    continue;
                }

                fallback = candidate;
                boolean isActive = (Boolean) ((JavascriptExecutor) currentDriver)
                        .executeScript(
                                "return document.activeElement === arguments[0];", candidate);
                if (isActive) {
                    return candidate;
                }
            } catch (StaleElementReferenceException ignored) {
                // Adaylar taranırken değişen input atlanır; sonraki poll güncel DOM'u okur.
            }
        }

        return fallback;
    }

    /** React kontrollü input'un değerini native setter ile güncelleyip olayları yayınlar. */
    private WebElement setSearchValueWithJavaScript(
            String text, RuntimeException previousFailure) {
        try {
            WebElement searchBox = waitForStableSearchBox();
            ((JavascriptExecutor) driver).executeScript(
                    "const input = arguments[0];"
                            + "const value = arguments[1];"
                            + "const setter = Object.getOwnPropertyDescriptor("
                            + "window.HTMLInputElement.prototype, 'value').set;"
                            + "setter.call(input, value);"
                            + "input.dispatchEvent(new Event('input', {bubbles: true}));"
                            + "input.dispatchEvent(new Event('change', {bubbles: true}));"
                            + "input.focus();",
                    searchBox, text);

            return waitForStableSearchValue(text);
        } catch (RuntimeException fallbackFailure) {
            String actualValue = currentSearchValue();
            IllegalStateException failure = new IllegalStateException(
                    "Arama alanına beklenen metin yazılamadı. Beklenen: '"
                            + text + "', görünen: '" + actualValue + "'",
                    fallbackFailure);
            if (previousFailure != null) {
                failure.addSuppressed(previousFailure);
            }
            throw failure;
        }
    }

    /** Hata mesajı için görünür arama input'undaki son değeri güvenle okur. */
    private String currentSearchValue() {
        try {
            WebElement searchBox = findUsableSearchBox(driver);
            String value = searchBox == null ? null : searchBox.getDomProperty("value");
            return value == null ? "" : value;
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    /** Girişten sonra hesap alanı görünüyorsa true, zaman aşımı olursa false döndürür. */
    public boolean loginCompleted() {
        try {
            // Header, giriş yönlendirmesinden sonra React tarafından yenilenebilir.
            // Her kontrolde elementi yeniden bularak geçici stale durumunda beklemeye devam eder.
            return until(LONG_TIMEOUT_SECONDS, currentDriver -> {
                try {
                    return currentDriver.findElements(LOGGED_IN_ACCOUNT).stream()
                            .anyMatch(WebElement::isDisplayed);
                } catch (StaleElementReferenceException exception) {
                    return false;
                } catch (WebDriverException exception) {
                    // Kimlik doğrulama yönlendirmesi sırasında Chrome eski frame'i kısa
                    // süreliğine ayırabilir. Yeni sayfa hazır olana kadar tekrar denenir.
                    if (exception.getMessage() != null
                            && exception.getMessage().contains("target frame detached")) {
                        return false;
                    }
                    throw exception;
                }
            });
        } catch (TimeoutException exception) {
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
