package pages;

import org.openqa.selenium.WebDriver;
import utils.ElementHelper;

/** Giriş sayfasındaki kullanıcı adı, parola ve giriş butonu işlemlerini temsil eder. */
public class LoginPage extends ElementHelper {

    /** Aktif tarayıcıyı üst sınıfa iletir. */
    public LoginPage(WebDriver driver) {
        super(driver, "loginPage");
    }

    /** Kullanıcı bilgilerini insan benzeri şekilde yazar ve giriş butonuna basar. */
    public void signIn(String email, String password) {
        // typeLikeUser önce alanı temizler, sonra değeri karakter karakter yazar.
        typeLikeUser("emailInput", email);
        pauseBetweenActions();
        typeLikeUser("passwordInput", password);
        pauseBetweenActions();
        // ElementHelper.click, buton tıklanabilir olana kadar açık bekleme uygular.
        click("loginButton");
    }
}
