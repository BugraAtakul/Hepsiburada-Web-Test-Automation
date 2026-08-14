package utils;

import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Arama sonuçlarındaki ürün kartlarını görsel satır ve sütunlarına göre düzenler. */
public final class ProductGridHelper extends ElementHelper {

    private static final int PRODUCT_LAYOUT_TIMEOUT_SECONDS = 15;
    private static final int ROW_DISTANCE_PIXELS = 60;

    public ProductGridHelper(WebDriver driver) {
        super(driver, "searchPage");
    }

    public WebElement waitForProductLink(int targetRow, int targetColumn) {
        try {
            return until(PRODUCT_LAYOUT_TIMEOUT_SECONDS, ignored ->
                    findProductLink(targetRow - 1, targetColumn - 1));
        } catch (TimeoutException exception) {
            throw new IllegalStateException(
                    targetRow + ". satırdaki " + targetColumn + ". ürün bulunamadı.",
                    exception);
        }
    }

    private WebElement findProductLink(int rowIndex, int columnIndex) {
        try {
            List<WebElement> productCards = new ArrayList<>();
            for (WebElement card : findElements("productCards")) {
                if (firstUsableProductLink(card) != null) {
                    productCards.add(card);
                }
            }

            List<List<WebElement>> rows = splitIntoRows(productCards);
            if (rows.size() <= rowIndex || rows.get(rowIndex).size() <= columnIndex) {
                return null;
            }
            return firstUsableProductLink(rows.get(rowIndex).get(columnIndex));
        } catch (StaleElementReferenceException exception) {
            return null;
        }
    }

    private List<List<WebElement>> splitIntoRows(List<WebElement> cards) {
        cards.sort(Comparator.comparingInt((WebElement card) -> card.getLocation().getY())
                .thenComparingInt(card -> card.getLocation().getX()));

        List<List<WebElement>> rows = new ArrayList<>();
        List<WebElement> currentRow = null;
        int currentRowY = 0;
        for (WebElement card : cards) {
            int y = card.getLocation().getY();
            if (currentRow == null || Math.abs(currentRowY - y) > ROW_DISTANCE_PIXELS) {
                currentRow = new ArrayList<>();
                rows.add(currentRow);
                currentRowY = y;
            }
            currentRow.add(card);
        }

        rows.forEach(row ->
                row.sort(Comparator.comparingInt(card -> card.getLocation().getX())));
        return rows;
    }

    private WebElement firstUsableProductLink(WebElement card) {
        if (!card.isDisplayed()) {
            return null;
        }

        for (WebElement link : findElements(card, "productLink")) {
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
}
