package pages;

import model.ProductIdentity;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import utils.ElementHelper;
import utils.ProductGridHelper;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Arama sonuç kartlarını satır/sütun konumlarına göre seçip ürün sayfasını açar. */
public class SearchResultsPage extends ElementHelper {

    // İş kuralı: ikinci satırın birinci sütunundaki ürün hedeflenir.
    private static final int TARGET_ROW = 2;
    private static final int TARGET_COLUMN = 1;

    // Sayfa hareketi ve ürün sayfası süre ayarlarıdır.
    private static final int PRODUCT_SCROLL_PIXELS = 650;
    private static final int PRODUCT_PAGE_TIMEOUT_SECONDS = 20;

    private final ProductGridHelper productGridHelper;

    /** Aktif tarayıcıyı ortak yardımcı sınıflara iletir. */
    public SearchResultsPage(WebDriver driver) {
        super(driver, "searchPage");
        this.productGridHelper = new ProductGridHelper(driver);
    }

    /** Hedef ürünü bulur, ürün sayfasını açar ve doğrulanacak ürün kimliğini döndürür. */
    public ProductIdentity openSecondRowFirstProduct() {
        // İlk ürün kartlarının sayfada yüklenmesi/görünmesi için aşağı kaydırılır.
        scrollDown(PRODUCT_SCROLL_PIXELS);

        StaleElementReferenceException lastStaleElement = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                WebElement productLink = productGridHelper.waitForProductLink(
                        TARGET_ROW, TARGET_COLUMN);
                scrollTo(productLink);

                ProductIdentity selectedProductFromCard = ProductIdentity.from(
                        productLink.getText(), productLink.getAttribute("href"));
                System.out.println("Seçilen ürün: " + selectedProductFromCard.name());
                pauseBetweenActions();

                String openedProductUrl = openProductPage(productLink);
                return ProductIdentity.from(selectedProductFromCard.name(), openedProductUrl);
            } catch (StaleElementReferenceException exception) {
                lastStaleElement = exception;
            }
        }

        throw new IllegalStateException(
                "İkinci satırdaki ilk ürün güncel DOM üzerinden seçilemedi.",
                lastStaleElement);
    }

    /** Ürün bağlantısına tıklar; yeni sekme açılırsa doğru ürün sekmesine geçer. */
    private String openProductPage(WebElement productLink) {
        Set<String> windowHandlesBeforeClick = driver.getWindowHandles();
        String searchResultsUrl = driver.getCurrentUrl();

        click(productLink);

        // Ürünler aynı sekmede veya target=_blank ile yeni sekmede açılabilir.
        until(PRODUCT_PAGE_TIMEOUT_SECONDS, d ->
                d.getWindowHandles().size() > windowHandlesBeforeClick.size()
                        || !searchResultsUrl.equals(d.getCurrentUrl()));

        Set<String> newWindowHandles = new HashSet<>(driver.getWindowHandles());
        newWindowHandles.removeAll(windowHandlesBeforeClick);
        if (!newWindowHandles.isEmpty()) {
            driver.switchTo().window(newWindowHandles.iterator().next());
        }

        // Reklam yönlendirme adresi yerine -p-/-pm- kodunu taşıyan nihai ürün URL'si beklenir.
        return until(PRODUCT_PAGE_TIMEOUT_SECONDS, d -> {
            String currentUrl = d.getCurrentUrl();
            if (currentUrl == null || currentUrl.isBlank()) {
                return null;
            }

            String normalizedUrl = currentUrl.toLowerCase(Locale.ROOT);
            boolean isProductUrl = normalizedUrl.contains("-p-")
                    || normalizedUrl.contains("-pm-");
            return isProductUrl ? currentUrl : null;
        });
    }
}
