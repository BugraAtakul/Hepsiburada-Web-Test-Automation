package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** Giriş sayfasındaki kullanıcı adı, parola ve giriş butonu işlemlerini temsil eder. */
public class LoginPage extends BasePage {

    // Locator, Selenium'un HTML içindeki elementi nasıl bulacağını tarif eder.
    private static final By EMAIL_INPUT = By.id("txtUserName");
    private static final By PASSWORD_INPUT = By.id("txtPassword");
    private static final By LOGIN_BUTTON = By.id("btnLogin");

    /** Aktif tarayıcıyı üst sınıfa iletir. */
    public LoginPage(WebDriver driver) {
        super(driver);
    }

    /** Kullanıcı bilgilerini insan benzeri şekilde yazar ve giriş butonuna basar. */
    public void signIn(String email, String password) {
        // typeLikeUser önce alanı temizler, sonra değeri karakter karakter yazar.
        typeLikeUser(EMAIL_INPUT, email);
        pauseBetweenActions();
        typeLikeUser(PASSWORD_INPUT, password);
        pauseBetweenActions();
        // BasePage.click, buton tıklanabilir olana kadar açık bekleme uygular.
        click(LOGIN_BUTTON);
    }
}
