package pages;

import model.ProductIdentity;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Arama sonuç kartlarını satır/sütun konumlarına göre seçip ürün sayfasını açar. */
public class SearchResultsPage extends BasePage {

    // İş kuralı: ikinci satırın birinci sütunundaki ürün hedeflenir.
    private static final int TARGET_ROW = 2;
    private static final int TARGET_COLUMN = 1;

    // Sayfa hareketi, aynı satır toleransı ve ürün sayfası süre ayarlarıdır.
    private static final int PRODUCT_SCROLL_PIXELS = 650;
    private static final int PRODUCT_LAYOUT_TIMEOUT_SECONDS = 15;
    private static final int ROW_DISTANCE_PIXELS = 60;
    private static final int PRODUCT_PAGE_TIMEOUT_SECONDS = 20;

    // CSS locator'ları ürün kartını ve ürün sayfasına giden başlık bağlantısını bulur.
    private static final By PRODUCT_CARDS =
            By.cssSelector("div[class*='productCard-module_productCardRoot']");
    private static final By PRODUCT_LINK = By.cssSelector("a[class*='titleText']");

    /** Aktif tarayıcıyı ortak BasePage işlemlerine iletir. */
    public SearchResultsPage(WebDriver driver) {
        super(driver);
    }

    /** Hedef ürünü bulur, ürün sayfasını açar ve doğrulanacak ürün kimliğini döndürür. */
    public ProductIdentity openSecondRowFirstProduct() {
        // İlk ürün kartlarının sayfada yüklenmesi/görünmesi için aşağı kaydırılır.
        scrollDown(PRODUCT_SCROLL_PIXELS);

        StaleElementReferenceException lastStaleElement = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                // React/lazy-loading tamamlanıp hedef kart oluşana kadar güncel DOM tekrar okunur.
                WebElement productLink = waitForTargetProductLink();
                scrollTo(productLink);

                // Ürün adı ve bağlantısı, detay ve sepet sayfalarında aynı ürünü doğrulamak için kaydedilir.
                ProductIdentity selectedProductFromCard = readProductIdentity(productLink);
                System.out.println("Seçilen ürün: " + selectedProductFromCard.name());
                pauseBetweenActions();

                // Kart bağlantısı reklam/takip adresi içerebilir. Doğrulama kimliği açılan gerçek
                // ürün sayfasının nihai URL'sinden yeniden üretilir.
                String openedProductUrl = openProductPage(productLink);
                return ProductIdentity.from(selectedProductFromCard.name(), openedProductUrl);
            } catch (StaleElementReferenceException exception) {
                // Kaydırma sırasında kart yenilenirse hedef bir kez güncel DOM'dan tekrar bulunur.
                lastStaleElement = exception;
            }
        }

        throw new IllegalStateException(
                "İkinci satırdaki ilk ürün güncel DOM üzerinden seçilemedi.",
                lastStaleElement);
    }

    /** Hedef satır ve sütun hazır olana kadar güncel DOM'daki ürün bağlantısını tekrar arar. */
    private WebElement waitForTargetProductLink() {
        int rowIndex = TARGET_ROW - 1;
        int columnIndex = TARGET_COLUMN - 1;

        try {
            return until(PRODUCT_LAYOUT_TIMEOUT_SECONDS, ignored -> {
                try {
                    List<WebElement> cards = new ArrayList<>();
                    for (WebElement card : driver.findElements(PRODUCT_CARDS)) {
                        if (firstUsableProductLink(card) != null) {
                            cards.add(card);
                        }
                    }

                    List<List<WebElement>> rows = splitIntoRows(cards);
                    boolean targetExists = rows.size() > rowIndex
                            && rows.get(rowIndex).size() > columnIndex;
                    if (!targetExists) {
                        return null;
                    }

                    return firstUsableProductLink(rows.get(rowIndex).get(columnIndex));
                } catch (StaleElementReferenceException exception) {
                    // React kartları yeniden oluşturursa güncel DOM bir sonraki turda tekrar okunur.
                    return null;
                }
            });
        } catch (TimeoutException exception) {
            throw new IllegalStateException("İkinci satırdaki ilk ürün bulunamadı.", exception);
        }
    }

    /** Kartları yakın Y koordinatlarına göre satır listeleri halinde gruplar. */
    private List<List<WebElement>> splitIntoRows(List<WebElement> cards) {
        cards.sort(Comparator.comparingInt((WebElement card) -> card.getLocation().getY())
                .thenComparingInt(card -> card.getLocation().getX()));

        // LinkedHashMap eklenme sırasını koruduğu için üst satırlar önce kalır.
        Map<Integer, List<WebElement>> rowMap = new LinkedHashMap<>();

        for (WebElement card : cards) {
            int y = card.getLocation().getY();
            // Y değerleri tolerans içindeyse kart aynı görsel satıra kabul edilir.
            Integer rowKey = rowMap.keySet().stream()
                    .filter(existingY -> Math.abs(existingY - y) <= ROW_DISTANCE_PIXELS)
                    .findFirst()
                    .orElse(y);

            rowMap.computeIfAbsent(rowKey, key -> new ArrayList<>()).add(card);
        }

        List<List<WebElement>> rows = new ArrayList<>(rowMap.values());
        // Her satır kendi içinde X koordinatına göre soldan sağa sıralanır.
        rows.forEach(row -> row.sort(Comparator.comparingInt(card -> card.getLocation().getX())));
        return rows;
    }

    /** Görünür adı ve bağlantısı olan ilk gerçek ürün linkini döndürür. */
    private WebElement firstUsableProductLink(WebElement card) {
        if (!card.isDisplayed()) {
            return null;
        }

        for (WebElement link : card.findElements(PRODUCT_LINK)) {
            String href = link.getAttribute("href");
            if (link.isDisplayed()
                    && !link.getText().isBlank()
                    && href != null
                    && !href.isBlank()) {
                return link;
            }
        }
        return null;
    }

    /** Ürün bağlantısının görünen adını ve adresini okuyup kararlı bir ürün kimliği üretir. */
    private ProductIdentity readProductIdentity(WebElement productLink) {
        return ProductIdentity.from(
                productLink.getText(),
                productLink.getAttribute("href"));
    }

    /** Ürün bağlantısına tıklar; yeni sekme açılırsa doğru ürün sekmesine geçer. */
    private String openProductPage(WebElement productLink) {
        Set<String> windowHandlesBeforeClick = driver.getWindowHandles();
        String searchResultsUrl = driver.getCurrentUrl();

        try {
            productLink.click();
        } catch (ElementClickInterceptedException exception) {
            // Kartın üzerindeki başka bir katman tıklamayı keserse bağlantı JavaScript ile açılır.
            javascriptClick(productLink);
        }

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
