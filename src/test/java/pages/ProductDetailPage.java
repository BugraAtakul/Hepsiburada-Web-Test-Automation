package pages;

import model.ProductIdentity;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import utils.ElementHelper;

/** Ürün detay sayfasının açılmasını doğrular ve ürünü bu sayfadan sepete ekler. */
public class ProductDetailPage extends ElementHelper {

    private static final int PRODUCT_PAGE_TIMEOUT_SECONDS = 20;
    private static final int ADD_TO_CART_SCROLL_PIXELS = 600;
    private static final int ADD_TO_CART_CONFIRMATION_TIMEOUT_SECONDS = 10;

    public ProductDetailPage(WebDriver driver) {
        super(driver, "productPage");
    }

    /** Açık sayfanın seçilen ürün URL/koduna ait olduğunu ve ürün başlığını gösterdiğini doğrular. */
    public boolean isLoaded(ProductIdentity expectedProduct) {
        return waitUntilTrue(PRODUCT_PAGE_TIMEOUT_SECONDS, d -> {
            try {
                return expectedProduct.matchesUrl(d.getCurrentUrl())
                        && findElements(d, "productTitle").stream()
                        .anyMatch(WebElement::isDisplayed);
            } catch (StaleElementReferenceException ignored) {
                return false;
            }
        });
    }

    /** Ürün detay sayfasındaki Sepete Ekle butonuna basar. */
    public void addToCart() {
        scrollDown(ADD_TO_CART_SCROLL_PIXELS);
        pauseBetweenActions();

        WebElement addToCartButton = clickable("addToCartButton");
        scrollTo(addToCartButton);
        pauseBetweenActions();

        click(addToCartButton);
    }

    /** Sepete ekleme sonrasında açılan görünür onay mesajını bekler. */
    public boolean isAddToCartConfirmationVisible() {
        return waitUntilTrue(ADD_TO_CART_CONFIRMATION_TIMEOUT_SECONDS, d -> {
            try {
                return findElements(d, "addToCartConfirmation").stream()
                        .anyMatch(WebElement::isDisplayed);
            } catch (StaleElementReferenceException ignored) {
                return false;
            }
        });
    }
}
