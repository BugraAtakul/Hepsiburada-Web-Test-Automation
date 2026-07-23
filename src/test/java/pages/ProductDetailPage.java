package pages;

import model.ProductIdentity;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/** Ürün detay sayfasının açılmasını doğrular ve ürünü bu sayfadan sepete ekler. */
public class ProductDetailPage extends BasePage {

    private static final int PRODUCT_PAGE_TIMEOUT_SECONDS = 20;
    private static final int ADD_TO_CART_SCROLL_PIXELS = 600;
    private static final int ADD_TO_CART_CONFIRMATION_TIMEOUT_SECONDS = 10;

    private static final By PRODUCT_TITLE = By.cssSelector(
            "h1[data-test-id='title'], " +
                    "h1[data-test-id='product-name'], " +
                    "h1[id='product-name'], " +
                    "h1[itemprop='name'], " +
                    "main h1");

    private static final By ADD_TO_CART_BUTTON = By.cssSelector(
            "[data-test-id='addToCart'], " +
                    "[data-test-id='add-to-cart-button'], " +
                    "[data-test-id='add-to-cart'], " +
                    "button[id='addToCart']");

    private static final By ADD_TO_CART_CONFIRMATION = By.xpath(
            "//*[self::span or self::div][" +
                    "normalize-space()='Ürün sepetinizde' or " +
                    "normalize-space()='Ürün sepetinize eklendi' or " +
                    "normalize-space()='Ürün sepete eklendi']");

    public ProductDetailPage(WebDriver driver) {
        super(driver);
    }

    /** Açık sayfanın seçilen ürün URL/koduna ait olduğunu ve ürün başlığını gösterdiğini doğrular. */
    public boolean isLoaded(ProductIdentity expectedProduct) {
        try {
            return until(PRODUCT_PAGE_TIMEOUT_SECONDS, d -> {
                try {
                    boolean correctProductUrl = expectedProduct.matchesUrl(d.getCurrentUrl());
                    boolean titleVisible = d.findElements(PRODUCT_TITLE).stream()
                            .anyMatch(WebElement::isDisplayed);
                    return correctProductUrl && titleVisible;
                } catch (StaleElementReferenceException ignored) {
                    // React başlığı yeniden oluşturuyorsa bir sonraki bekleme turunda tekrar denenir.
                    return false;
                }
            });
        } catch (TimeoutException exception) {
            return false;
        }
    }

    /** Ürün detay sayfasındaki Sepete Ekle butonuna basar. */
    public void addToCart() {
        // Ürün sayfasının ilk görünümünde buton ekranın altında kaldığı için önce aşağı kaydırılır.
        // Bu hareket, aşağı kaydırmayla yüklenen satış alanının DOM'a eklenmesini de tetikler.
        scrollDown(ADD_TO_CART_SCROLL_PIXELS);
        pauseBetweenActions();

        WebElement addToCartButton = clickable(ADD_TO_CART_BUTTON);
        scrollTo(addToCartButton);
        pauseBetweenActions();

        try {
            addToCartButton.click();
        } catch (ElementClickInterceptedException exception) {
            javascriptClick(addToCartButton);
        }
    }

    /** Sepete ekleme sonrasında açılan görünür onay mesajını bekler. */
    public boolean isAddToCartConfirmationVisible() {
        try {
            return until(ADD_TO_CART_CONFIRMATION_TIMEOUT_SECONDS, d -> {
                try {
                    return d.findElements(ADD_TO_CART_CONFIRMATION).stream()
                            .anyMatch(WebElement::isDisplayed);
                } catch (StaleElementReferenceException ignored) {
                    // Modal yeniden oluşturulursa bir sonraki bekleme turunda tekrar aranır.
                    return false;
                }
            });
        } catch (TimeoutException exception) {
            return false;
        }
    }
}
