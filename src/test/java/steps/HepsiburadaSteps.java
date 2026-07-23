package steps;

import com.thoughtworks.gauge.Step;
import config.EnvironmentConfig;
import driver.Driver;
import model.ProductIdentity;
import pages.CartPage;
import pages.HomePage;
import pages.LoginPage;
import pages.ProductDetailPage;
import pages.SearchResultsPage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Gauge spec dosyasındaki okunabilir adımları çalıştırılabilir Java koduna bağlar.
 *
 * <p>{@link Step} anotasyonundaki metin spec satırıyla eşleştiğinde Gauge ilgili
 * metodu çağırır. Bu sınıf iş akışını yönetir; HTML ayrıntılarını Page Object
 * sınıflarına bırakır.</p>
 */
public class HepsiburadaSteps {

    // static final sabit sınıfa aittir ve çalışma sırasında değiştirilemez.
    private static final String BASE_URL = "https://www.hepsiburada.com";

    // Bir adımda üretilen test verilerini sonraki doğrulama adımlarına taşırlar.
    private ProductIdentity selectedProduct;
    private Integer cartCountBeforeAdding;

    // Aktif Driver ile ilgili Page Object'i oluşturan kısa yardımcı metotlardır.
    private HomePage homePage() { return new HomePage(Driver.driver); }
    private LoginPage loginPage() { return new LoginPage(Driver.driver); }
    private SearchResultsPage resultsPage() { return new SearchResultsPage(Driver.driver); }
    private ProductDetailPage productDetailPage() { return new ProductDetailPage(Driver.driver); }
    private CartPage cartPage() { return new CartPage(Driver.driver); }

    @Step("Hepsiburada ana sayfasını aç")
    /** Ana adresi WebDriver ile tarayıcıda açar. */
    public void openHomePage() {
        // WebDriver.get verilen URL'ye gider.
        Driver.driver.get(BASE_URL);
    }

    @Step("Varsa çerez bildirimini kapat")
    /** Varsa çerez bildirimini kapatma işini HomePage'e devreder. */
    public void closeCookies() {
        homePage().closeCookieBanner();
    }

    @Step("Giriş ekranını aç")
    /** Üst menü üzerinden giriş sayfasını açar. */
    public void openLogin() {
        homePage().openLoginPage();
    }

    @Step("Kayıtlı kullanıcı bilgileriyle giriş yap")
    /** Kaynak koda yazılmayan ortam değişkenleriyle giriş yapar. */
    public void login() {
        // require, eksik veya boş gizli bilgi varsa testi açıklayıcı hatayla durdurur.
        loginPage().signIn(
                EnvironmentConfig.require("HEPSIBURADA_EMAIL"),
                EnvironmentConfig.require("HEPSIBURADA_PASSWORD")
        );
    }

    @Step("Kullanıcının giriş yaptığını doğrula")
    /** Hesap alanının görünmesini AssertJ ile doğrular. */
    public void checkLogin() {
        // assertThat gerçek sonucu beklenen koşulla karşılaştırır.
        assertThat(homePage().loginCompleted())
                .as("Kullanıcı girişi tamamlanamadı")
                .isTrue();
    }

    @Step("Arama alanında <keyword> kelimesini ara")
    /** Spec içindeki &lt;keyword&gt; parametresini alıp arama yapar. */
    public void search(String keyword) {
        homePage().searchFor(keyword);
    }

    @Step("Arama sonucunun <keyword> ile ilgili olduğunu doğrula")
    /** Sonuç başlığının aranan kelimeyi içerdiğini doğrular. */
    public void checkSearch(String keyword) {
        // İki metni de küçük harfe çevirmek harf büyüklüğü farkını önemsiz yapar.
        assertThat(homePage().searchHeading().toLowerCase())
                .contains(keyword.toLowerCase());
    }

    @Step("Sonuçların ikinci satırındaki ilk ürünün sayfasını aç")
    /** Sepet sayısını kaydeder, hedef ürünü seçer ve ürün detay sayfasını açar. */
    public void openProductDetail() {
        // Önceki değer, detay sayfasından ekleme sonrası sayacın bir arttığını kanıtlamak içindir.
        cartCountBeforeAdding = homePage().cartCount();
        selectedProduct = resultsPage().openSecondRowFirstProduct();
    }

    @Step("Seçilen ürünün detay sayfasının açıldığını doğrula")
    /** Açılan detay sayfasının sonuç listesinden seçilen ürüne ait olduğunu doğrular. */
    public void checkProductDetail() {
        assertThat(selectedProduct)
                .as("Detay sayfası açılacak ürünün kimliği kaydedilmedi")
                .isNotNull();

        assertThat(productDetailPage().isLoaded(selectedProduct))
                .as("Seçilen ürünün detay sayfası açılamadı: %s", selectedProduct.name())
                .isTrue();
    }

    @Step("Ürünü detay sayfasından sepete ekle")
    /** Sepete ekleme işlemini arama kartından değil ürün detay sayfasından yapar. */
    public void addProductFromDetail() {
        assertThat(selectedProduct)
                .as("Sepete eklenecek ürünün kimliği kaydedilmedi")
                .isNotNull();

        productDetailPage().addToCart();
    }

    @Step("Ürünün sepete eklendiğine dair onay mesajını doğrula")
    /** Ürün detay sayfasında açılan sepete ekleme onay mesajını doğrular. */
    public void checkAddToCartConfirmation() {
        assertThat(productDetailPage().isAddToCartConfirmationVisible())
                .as("Sepete ekleme onay mesajı görünmedi")
                .isTrue();
    }

    @Step("Sepet sayacının arttığını doğrula")
    /** Sepet sayacının ürün eklenmesinden sonra bir arttığını doğrular. */
    public void checkCartCounter() {
        // Önce ürün ekleme adımının gerekli başlangıç verisini kaydettiği kontrol edilir.
        assertThat(cartCountBeforeAdding)
                .as("Ürün eklenmeden önceki sepet sayacı kaydedilmedi")
                .isNotNull();

        // Beklenen yeni değer, önceki sepet sayısından tam olarak bir fazladır.
        int expectedCount = cartCountBeforeAdding + 1;
        int actualCount = homePage().waitForCartCount(expectedCount);

        // Gerçek sayaç ile hesaplanan beklenen sayaç eşit olmalıdır.
        assertThat(actualCount)
                .as("Sepet sayacı bir artmadı. Önceki: %d, beklenen: %d",
                        cartCountBeforeAdding, expectedCount)
                .isEqualTo(expectedCount);
    }

    @Step("Sepet sayfasını aç")
    /** Sepet sayfasına gider. */
    public void openCart() {
        homePage().goToCart();
    }

    @Step("Eklenen ürünün adı ve temel detaylarının sepette eşleştiğini doğrula")
    /** Seçilen ürünün kimliğini ve ekranda görünen adını sepette ayrı ayrı doğrular. */
    public void checkProductInCart() {
        assertThat(selectedProduct)
                .as("Sepete eklenen ürünün kimliği kaydedilmedi")
                .isNotNull();

        CartPage cart = cartPage();

        // Ürün kodu veya takip parametrelerinden arındırılmış URL yolu temel kimliği doğrular.
        assertThat(cart.waitUntilProductIsVisible(selectedProduct))
                .as("Sepette beklenen ürün bulunamadı. Beklenen ürün: %s", selectedProduct.name())
                .isTrue();

        // Aynı ürün bağlantısının ekranda kullanıcıya gösterilen adı ayrıca okunur ve karşılaştırılır.
        String visibleCartProductName = cart.waitForVisibleProductName(selectedProduct);
        assertThat(visibleCartProductName)
                .as("Sepetteki ürünün görünen adı okunamadı")
                .isNotBlank();
        assertThat(selectedProduct.matchesName(visibleCartProductName))
                .as("Sepetteki ürün adı seçilen ürünle eşleşmiyor. Beklenen: '%s', görünen: '%s'",
                        selectedProduct.name(), visibleCartProductName)
                .isTrue();
    }

    @Step("Sepetteki ürünü kaldır")
    /** Kaydedilen hedef ürünü sepetten kaldırır. */
    public void removeProduct() {
        assertThat(selectedProduct)
                .as("Sepetten kaldırılacak ürünün kimliği kaydedilmedi")
                .isNotNull();

        cartPage().deleteProduct(selectedProduct);
    }

    @Step("Eklenen ürünün sepetten kaldırıldığını doğrula")
    /** Hedef ürünün artık sepette bulunmadığını doğrular. */
    public void checkProductRemoved() {
        assertThat(cartPage().waitUntilProductIsRemoved(selectedProduct))
                .as("Eklenen ürün sepetten kaldırılamadı: %s", selectedProduct.name())
                .isTrue();
    }
}
