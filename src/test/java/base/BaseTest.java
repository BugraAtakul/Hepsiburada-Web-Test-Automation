package base;

import driver.Driver;
import org.openqa.selenium.WebDriver;
import pages.CartPage;
import pages.HomePage;
import pages.LoginPage;
import pages.ProductDetailPage;
import pages.SearchResultsPage;

/** Aktif WebDriver için Page nesnelerini tek kez üretip bütün step sınıflarına sunar. */
public abstract class BaseTest {

    private WebDriver pageDriver;
    private HomePage homePage;
    private LoginPage loginPage;
    private SearchResultsPage searchResultsPage;
    private ProductDetailPage productDetailPage;
    private CartPage cartPage;

    protected final WebDriver driver() {
        initializePagesIfRequired();
        return pageDriver;
    }

    protected final HomePage homePage() {
        initializePagesIfRequired();
        return homePage;
    }

    protected final LoginPage loginPage() {
        initializePagesIfRequired();
        return loginPage;
    }

    protected final SearchResultsPage resultsPage() {
        initializePagesIfRequired();
        return searchResultsPage;
    }

    protected final ProductDetailPage productDetailPage() {
        initializePagesIfRequired();
        return productDetailPage;
    }

    protected final CartPage cartPage() {
        initializePagesIfRequired();
        return cartPage;
    }

    private void initializePagesIfRequired() {
        WebDriver activeDriver = Driver.driver;
        if (activeDriver == null) {
            throw new IllegalStateException("WebDriver henüz başlatılmadı.");
        }

        if (activeDriver == pageDriver) {
            return;
        }

        pageDriver = activeDriver;
        homePage = new HomePage(activeDriver);
        loginPage = new LoginPage(activeDriver);
        searchResultsPage = new SearchResultsPage(activeDriver);
        productDetailPage = new ProductDetailPage(activeDriver);
        cartPage = new CartPage(activeDriver);
    }
}
