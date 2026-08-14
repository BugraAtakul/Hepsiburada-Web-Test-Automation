package pages;

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
import utils.ElementHelper;

import java.util.List;
import java.util.function.Predicate;

/** Hepsiburada ana sayfası ve üst menü işlemleri. */
public class HomePage extends ElementHelper {

    private static final int SHORT_TIMEOUT_SECONDS = 5;
    private static final int LONG_TIMEOUT_SECONDS = 20;
    private static final int SEARCH_TEXT_ATTEMPTS = 3;

    public HomePage(WebDriver driver) {
        super(driver, "homePage");
    }

    /** Varsa Shadow DOM içinde açılan çerez bildirimini kabul edip kapatır. */
    public void closeCookieBanner() {
        try {
            WebElement host = present("cookieHost", SHORT_TIMEOUT_SECONDS);
            SearchContext shadowRoot = host.getShadowRoot();

            for (WebElement element : findElements(shadowRoot, "cookieElements")) {
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
        new Actions(driver).moveToElement(visible("accountArea")).perform();
        pauseBetweenActions();
        click("loginLink");
    }

    /** Arama kutusunu etkinleştirir, aranan metni yazar ve Enter'a basar. */
    public void searchFor(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Arama metni boş olamaz.");
        }

        WebElement collapsedSearchBox = visible("searchBox");

        scrollTo(collapsedSearchBox);
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].focus(); arguments[0].click();", collapsedSearchBox);

        WebElement verifiedSearchBox = enterVerifiedSearchText(text);
        pauseBetweenActions();
        verifiedSearchBox.sendKeys(Keys.ENTER);
    }

    private WebElement enterVerifiedSearchText(String text) {
        RuntimeException lastFailure = null;

        for (int attempt = 1; attempt <= SEARCH_TEXT_ATTEMPTS; attempt++) {
            try {
                WebElement searchBox = waitForStableSearchBox();
                searchBox.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE);

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

        return setSearchValueWithJavaScript(text, lastFailure);
    }

    private WebElement waitForStableSearchBox() {
        return waitForStableSearchBox(DEFAULT_TIMEOUT_SECONDS, searchBox -> {
            ((JavascriptExecutor) driver)
                    .executeScript("arguments[0].focus();", searchBox);
            return true;
        });
    }

    private WebElement waitForStableSearchValue(String expectedValue) {
        return waitForStableSearchBox(SHORT_TIMEOUT_SECONDS,
                searchBox -> expectedValue.equals(searchBox.getDomProperty("value")));
    }

    private WebElement waitForStableSearchBox(
            int seconds, Predicate<WebElement> acceptableSearchBox) {
        WebElement[] previousSearchBox = new WebElement[1];
        int[] consecutiveMatches = {0};

        return until(seconds, currentDriver -> {
            try {
                WebElement currentSearchBox = findUsableSearchBox(currentDriver);
                if (currentSearchBox == null || !acceptableSearchBox.test(currentSearchBox)) {
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

    private WebElement findUsableSearchBox(WebDriver currentDriver) {
        WebElement fallback = null;

        for (WebElement candidate : findElements(currentDriver, "searchBox")) {
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
            }
        }

        return fallback;
    }

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

    private String currentSearchValue() {
        try {
            WebElement searchBox = findUsableSearchBox(driver);
            String value = searchBox == null ? null : searchBox.getDomProperty("value");
            return value == null ? "" : value;
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    public boolean loginCompleted() {
        return waitUntilTrue(LONG_TIMEOUT_SECONDS, currentDriver -> {
            try {
                return findElements(currentDriver, "loggedInAccount").stream()
                        .anyMatch(WebElement::isDisplayed);
            } catch (StaleElementReferenceException exception) {
                return false;
            } catch (WebDriverException exception) {
                if (exception.getMessage() != null
                        && exception.getMessage().contains("target frame detached")) {
                    return false;
                }
                throw exception;
            }
        });
    }

    /** Arama sonuç sayfasındaki ana başlığı okur. */
    public String searchHeading() {
        return visible("resultHeading").getText();
    }

    /** Üst menüdeki mevcut sepet ürün sayısını döndürür. */
    public int cartCount() {
        visible("cartLink");
        return readCartCount();
    }

    /** Sepet sayacı beklenen değere ulaşana kadar bekler ve son değeri döndürür. */
    public int waitForCartCount(int expectedCount) {
        try {
            return until(DEFAULT_TIMEOUT_SECONDS, ignored -> {
                int currentCount = readCartCount();
                return currentCount == expectedCount ? currentCount : null;
            });
        } catch (TimeoutException e) {
            return readCartCount();
        }
    }

    /** Sepet bağlantısına tıklar ve sepet URL'sinin açıldığını doğrular. */
    public void goToCart() {
        WebElement cartLink = visible("cartLink");
        click(cartLink);
        waitUntilUrlContains("sepetim", DEFAULT_TIMEOUT_SECONDS);
    }

    /** Sayaç metnini temizleyip int türünde sayıya dönüştürür. */
    private int readCartCount() {
        List<WebElement> counters = findElements("cartCount");
        if (counters.isEmpty()) {
            return 0;
        }

        WebElement counter = counters.get(0);
        String counterText = counter.getText().trim();
        if (counterText.isBlank()) {
            String textContent = counter.getAttribute("textContent");
            counterText = textContent == null ? "" : textContent.trim();
        }

        if (counterText.isBlank()) {
            return 0;
        }

        String numericValue = counterText.replaceAll("[^0-9]", "");
        if (numericValue.isBlank()) {
            throw new IllegalStateException(
                    "Sepet sayacı sayıya dönüştürülemedi: " + counterText);
        }

        return Integer.parseInt(numericValue);
    }
}
