package pages;

import model.ProductIdentity;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import utils.ElementHelper;

import java.util.Optional;

/** Sepet sayfasındaki ürün okuma, bulma ve silme davranışlarını temsil eder. */
public class CartPage extends ElementHelper {

    private static final int CART_CONTENT_TIMEOUT_SECONDS = 20;
    private static final int OPTIONAL_CONFIRM_TIMEOUT_SECONDS = 5;

    /** Aktif tarayıcıyı ortak ElementHelper işlemlerine iletir. */
    public CartPage(WebDriver driver) {
        super(driver, "cartPage");
    }

    /** Kimliği verilen ürünü sepette bulur, kaldırır ve artık görünmediğini bekler. */
    public void deleteProduct(ProductIdentity expectedProduct) {
        WebElement productLink;
        try {
            productLink = until(CART_CONTENT_TIMEOUT_SECONDS,
                    ignored -> findMatchingVisibleProduct(expectedProduct).orElse(null));
        } catch (TimeoutException exception) {
            throw new IllegalStateException(
                    "Sepetten kaldırılacak ürün bulunamadı: " + expectedProduct.name(), exception);
        }

        WebElement productContainer = findElement(productLink, "productContainer");
        WebElement removeButton = findElement(productContainer, "removeButton");
        javascriptClick(removeButton);

        clickOptionalRemoveConfirmation(expectedProduct);

        try {
            until(DEFAULT_TIMEOUT_SECONDS,
                    ignored -> findMatchingVisibleProduct(expectedProduct).isEmpty());
        } catch (TimeoutException exception) {
            throw new IllegalStateException(
                    "Silme onayından sonra ürün sepetten kaldırılamadı: " + expectedProduct.name(),
                    exception);
        }
    }

    /** Sepet yüklenip beklenen URL/koda eşleşen ürün görünene kadar bekler. */
    public boolean waitUntilProductIsVisible(ProductIdentity expectedProduct) {
        return waitUntilTrue(CART_CONTENT_TIMEOUT_SECONDS,
                ignored -> findMatchingVisibleProduct(expectedProduct).isPresent());
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
        return waitUntilTrue(DEFAULT_TIMEOUT_SECONDS,
                ignored -> findMatchingVisibleProduct(expectedProduct).isEmpty());
    }

    /** Bazı tasarımlarda açılan silme modalını kısa süre bekler; modal yoksa akışı geciktirmez. */
    private void clickOptionalRemoveConfirmation(ProductIdentity expectedProduct) {
        try {
            WebElement confirmRemoveButton = until(OPTIONAL_CONFIRM_TIMEOUT_SECONDS, ignored -> {
                for (WebElement button : findElements("confirmRemoveButton")) {
                    try {
                        if (button.isDisplayed() && button.isEnabled()) {
                            return button;
                        }
                    } catch (StaleElementReferenceException ignoredStaleElement) {
                    }
                }
                return null;
            });

            click(confirmRemoveButton);
        } catch (TimeoutException exception) {
            if (findMatchingVisibleProduct(expectedProduct).isPresent()) {
                throw new IllegalStateException(
                        "Sepetten silme onayındaki 'Sil' butonu bulunamadı.",
                        exception);
            }
        }
    }

    /** Güncellenen DOM içinde hedef ürünün görünür bağlantısını ürün kodu/URL ile arar. */
    private Optional<WebElement> findMatchingVisibleProduct(ProductIdentity expectedProduct) {
        for (WebElement productLink : findElements("productLink")) {
            try {
                if (productLink.isDisplayed()
                        && expectedProduct.matchesUrl(productLink.getAttribute("href"))) {
                    return Optional.of(productLink);
                }
            } catch (StaleElementReferenceException ignored) {
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
            return null;
        }
    }
}
