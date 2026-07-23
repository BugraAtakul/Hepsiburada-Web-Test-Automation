package pages;

import model.ProductIdentity;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Optional;

/** Sepet sayfasındaki ürün okuma, bulma ve silme davranışlarını temsil eder. */
public class CartPage extends BasePage {

    private static final int CART_CONTENT_TIMEOUT_SECONDS = 20;
    private static final int OPTIONAL_CONFIRM_TIMEOUT_SECONDS = 2;

    // Sepet ürün adı, kaldırma bağlantısı ve opsiyonel onay butonu locator'larıdır.
    private static final By PRODUCT_LINK = By.cssSelector("[class*='product_name_'] a");
    private static final By REMOVE_BUTTON = By.cssSelector("a[aria-label='Sepetten Çıkar']");
    private static final By CONFIRM_REMOVE_BUTTON = By.cssSelector(
            "[role='dialog'] button[kind='secondary'], " +
                    "[aria-modal='true'] button[kind='secondary']");

    /** Aktif tarayıcıyı ortak BasePage işlemlerine iletir. */
    public CartPage(WebDriver driver) {
        super(driver);
    }

    /** Kimliği verilen ürünü sepette bulur, kaldırır ve artık görünmediğini bekler. */
    public void deleteProduct(ProductIdentity expectedProduct) {
        // Sepet React tarafından asenkron yüklendiği için hedef ürün bağlantısı oluşana kadar beklenir.
        WebElement productLink;
        try {
            productLink = until(CART_CONTENT_TIMEOUT_SECONDS,
                    ignored -> findMatchingVisibleProduct(expectedProduct).orElse(null));
        } catch (TimeoutException exception) {
            throw new IllegalStateException(
                    "Sepetten kaldırılacak ürün bulunamadı: " + expectedProduct.name(), exception);
        }

        // XPath ile ürün adından yukarı çıkıp kaldırma butonunu içeren ürün kabı bulunur.
        WebElement productContainer = productLink.findElement(By.xpath(
                "./ancestor::*[.//a[@aria-label='Sepetten Çıkar']][1]"));
        WebElement removeButton = productContainer.findElement(REMOVE_BUTTON);
        javascriptClick(removeButton);

        clickOptionalRemoveConfirmation();

        // Asenkron silme tamamlanıp ürün DOM'dan kalkana kadar beklenir.
        until(DEFAULT_TIMEOUT_SECONDS,
                ignored -> findMatchingVisibleProduct(expectedProduct).isEmpty());
    }

    /** Sepet yüklenip beklenen URL/koda eşleşen ürün görünene kadar bekler. */
    public boolean waitUntilProductIsVisible(ProductIdentity expectedProduct) {
        try {
            until(CART_CONTENT_TIMEOUT_SECONDS,
                    ignored -> findMatchingVisibleProduct(expectedProduct).isPresent());
            return true;
        } catch (TimeoutException exception) {
            return false;
        }
    }

    /** Aynı ürünün sepette görünen ve boş olmayan adını okuyana kadar bekler. */
    public String waitForVisibleProductName(ProductIdentity expectedProduct) {
        try {
            return until(CART_CONTENT_TIMEOUT_SECONDS, ignored ->
                    findMatchingVisibleProduct(expectedProduct)
                            .map(this::readVisibleProductName)
                            .orElse(null));
        } catch (TimeoutException exception) {
            return null;
        }
    }

    /** Silme işleminden sonra hedef ürün bağlantısı kaybolana kadar bekler. */
    public boolean waitUntilProductIsRemoved(ProductIdentity expectedProduct) {
        try {
            until(DEFAULT_TIMEOUT_SECONDS,
                    ignored -> findMatchingVisibleProduct(expectedProduct).isEmpty());
            return true;
        } catch (TimeoutException exception) {
            return false;
        }
    }

    /** Bazı tasarımlarda açılan silme modalını kısa süre bekler; modal yoksa akışı geciktirmez. */
    private void clickOptionalRemoveConfirmation() {
        try {
            WebElement confirmRemoveButton = until(OPTIONAL_CONFIRM_TIMEOUT_SECONDS, ignored -> {
                for (WebElement button : driver.findElements(CONFIRM_REMOVE_BUTTON)) {
                    try {
                        if (button.isDisplayed() && button.isEnabled()) {
                            return button;
                        }
                    } catch (StaleElementReferenceException staleElement) {
                        // Modal yeniden oluşturulursa kısa beklemenin sonraki turunda tekrar aranır.
                    }
                }
                return null;
            });

            try {
                confirmRemoveButton.click();
            } catch (ElementClickInterceptedException exception) {
                javascriptClick(confirmRemoveButton);
            }
        } catch (TimeoutException ignored) {
            // Güncel tasarımda onay modalı gösterilmiyorsa silme işlemi doğrudan devam eder.
        }
    }

    /** Güncellenen DOM içinde hedef ürünün görünür bağlantısını ürün kodu/URL ile arar. */
    private Optional<WebElement> findMatchingVisibleProduct(ProductIdentity expectedProduct) {
        for (WebElement productLink : driver.findElements(PRODUCT_LINK)) {
            try {
                if (productLink.isDisplayed()
                        && expectedProduct.matchesUrl(productLink.getAttribute("href"))) {
                    return Optional.of(productLink);
                }
            } catch (StaleElementReferenceException ignored) {
                // Sepet yüklenirken satır yeniden oluşturulursa sonraki bekleme turunda tekrar aranır.
            }
        }
        return Optional.empty();
    }

    /** Görünür ürün bağlantısının kullanıcıya gösterilen adını güvenli biçimde okur. */
    private String readVisibleProductName(WebElement productLink) {
        try {
            String productName = productLink.getText().trim();
            return productName.isBlank() ? null : productName;
        } catch (StaleElementReferenceException ignored) {
            // Ürün satırı yenilenirse beklemenin sonraki turunda yeni bağlantı okunur.
            return null;
        }
    }
}
