package steps;

import base.BaseTest;
import com.thoughtworks.gauge.Step;
import model.ProductIdentity;
import pages.CartPage;
import utils.EnvironmentReader;

import static org.assertj.core.api.Assertions.assertThat;

/** Gauge adımlarını Page Object işlemlerine bağlar. */
public class HepsiburadaSteps extends BaseTest {

    private static final String BASE_URL = "https://www.hepsiburada.com";

    private ProductIdentity selectedProduct;
    private Integer cartCountBeforeAdding;

    @Step("Hepsiburada ana sayfasını aç")
    public void openHomePage() {
        driver().get(BASE_URL);
    }

    @Step("Varsa çerez bildirimini kapat")
    public void closeCookies() {
        homePage().closeCookieBanner();
    }

    @Step("Giriş ekranını aç")
    public void openLogin() {
        homePage().openLoginPage();
    }

    @Step("Kayıtlı kullanıcı bilgileriyle giriş yap")
    public void login() {
        loginPage().signIn(
                EnvironmentReader.require("HEPSIBURADA_EMAIL"),
                EnvironmentReader.require("HEPSIBURADA_PASSWORD")
        );
    }

    @Step("Kullanıcının giriş yaptığını doğrula")
    public void checkLogin() {
        assertThat(homePage().loginCompleted())
                .as("Kullanıcı girişi tamamlanamadı")
                .isTrue();
    }

    @Step("Arama alanında <keyword> kelimesini ara")
    public void search(String keyword) {
        homePage().searchFor(keyword);
    }

    @Step("Arama sonucunun <keyword> ile ilgili olduğunu doğrula")
    public void checkSearch(String keyword) {
        assertThat(homePage().searchHeading().toLowerCase())
                .contains(keyword.toLowerCase());
    }

    @Step("Sonuçların ikinci satırındaki ilk ürünün sayfasını aç")
    public void openProductDetail() {
        cartCountBeforeAdding = homePage().cartCount();
        selectedProduct = resultsPage().openSecondRowFirstProduct();
    }

    @Step("Seçilen ürünün detay sayfasının açıldığını doğrula")
    public void checkProductDetail() {
        ProductIdentity product = requireSelectedProduct();

        assertThat(productDetailPage().isLoaded(product))
                .as("Seçilen ürünün detay sayfası açılamadı: %s", product.name())
                .isTrue();
    }

    @Step("Ürünü detay sayfasından sepete ekle")
    public void addProductFromDetail() {
        requireSelectedProduct();
        productDetailPage().addToCart();
    }

    @Step("Ürünün sepete eklendiğine dair onay mesajını doğrula")
    public void checkAddToCartConfirmation() {
        assertThat(productDetailPage().isAddToCartConfirmationVisible())
                .as("Sepete ekleme onay mesajı görünmedi")
                .isTrue();
    }

    @Step("Sepet sayacının arttığını doğrula")
    public void checkCartCounter() {
        int previousCount = requirePreviousCartCount();
        int expectedCount = previousCount + 1;
        int actualCount = homePage().waitForCartCount(expectedCount);

        assertThat(actualCount)
                .as("Sepet sayacı bir artmadı. Önceki: %d, beklenen: %d",
                        previousCount, expectedCount)
                .isEqualTo(expectedCount);
    }

    @Step("Sepet sayfasını aç")
    public void openCart() {
        homePage().goToCart();
    }

    @Step("Eklenen ürünün adı ve temel detaylarının sepette eşleştiğini doğrula")
    public void checkProductInCart() {
        ProductIdentity product = requireSelectedProduct();
        CartPage cart = cartPage();

        assertThat(cart.waitUntilProductIsVisible(product))
                .as("Sepette beklenen ürün bulunamadı. Beklenen ürün: %s", product.name())
                .isTrue();

        String visibleCartProductName = cart.waitForVisibleProductName(product);
        assertThat(visibleCartProductName)
                .as("Sepetteki ürünün görünen adı okunamadı")
                .isNotBlank();
        assertThat(product.matchesName(visibleCartProductName))
                .as("Sepetteki ürün adı seçilen ürünle eşleşmiyor. Beklenen: '%s', görünen: '%s'",
                        product.name(), visibleCartProductName)
                .isTrue();
    }

    @Step("Sepetteki ürünü kaldır")
    public void removeProduct() {
        cartPage().deleteProduct(requireSelectedProduct());
    }

    @Step("Eklenen ürünün sepetten kaldırıldığını doğrula")
    public void checkProductRemoved() {
        ProductIdentity product = requireSelectedProduct();
        assertThat(cartPage().waitUntilProductIsRemoved(product))
                .as("Eklenen ürün sepetten kaldırılamadı: %s", product.name())
                .isTrue();
    }

    private ProductIdentity requireSelectedProduct() {
        if (selectedProduct == null) {
            throw new IllegalStateException("Ürün kimliği henüz kaydedilmedi.");
        }
        return selectedProduct;
    }

    private int requirePreviousCartCount() {
        if (cartCountBeforeAdding == null) {
            throw new IllegalStateException("Önceki sepet sayacı henüz kaydedilmedi.");
        }
        return cartCountBeforeAdding;
    }
}
