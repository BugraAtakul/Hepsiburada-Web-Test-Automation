package driver;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Selenium WebDriver'ın oluşturulmasını ve kapatılmasını tek merkezden yönetir.
 *
 * <p>WebDriver, Java kodu ile gerçek Chrome tarayıcısı arasındaki köprüdür.
 * Sınıfın statik yapısı sayesinde senaryodaki bütün Page Object'ler aynı
 * tarayıcı oturumunu paylaşır.</p>
 */
public class Driver {

    // Aktif tarayıcı oturumu. Page Object ve step sınıfları bu referansı kullanır.
    public static WebDriver driver;

    /** Test için Chrome sürücüsünü hazırlar ve tek bir tarayıcı açar. */
    public static void init() {
        // Aynı senaryo içinde init ikinci kez çağrılsa bile yeni tarayıcı açılmaz.
        if (driver == null) {
            // Kurulu Chrome ile uyumlu ChromeDriver sürümünü otomatik olarak hazırlar.
            WebDriverManager.chromedriver().setup();

            // ChromeOptions, tarayıcının hangi başlangıç ayarlarıyla açılacağını belirler.
            ChromeOptions options = new ChromeOptions();

            // Test için ayrı ve büyük bir pencere açarak elementlerin görünürlüğünü artırır.
            options.addArguments("--new-window");
            options.addArguments("--start-maximized");

            // Açılır pencere ve bildirimlerin test adımlarını engellemesini azaltır.
            options.addArguments("--disable-popup-blocking");
            options.addArguments("--disable-infobars");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-notifications");

            // Chrome'un otomasyon uyarılarını ve webdriver işaretini azaltan ayarlardır.
            options.addArguments("--disable-blink-features=AutomationControlled");
            options.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
            options.setExperimentalOption("useAutomationExtension", false);

            // Şifre kaydet / şifre yöneticisi popup'ını engelle
            Map<String, Object> prefs = new HashMap<>();
            prefs.put("credentials_enable_service", false);
            prefs.put("profile.password_manager_enabled", false);
            options.setExperimentalOption("prefs", prefs);

            // Yukarıdaki seçeneklerle gerçek Chrome tarayıcı oturumunu oluşturur.
            driver = new ChromeDriver(options);

            // JavaScriptExecutor, sayfanın içinde JavaScript çalıştırmamızı sağlar.
            ((JavascriptExecutor) driver).executeScript(
                    "Object.defineProperty(navigator, 'webdriver', {get: () => undefined})"
            );
        }
    }

    /** Aktif tarayıcıyı bütün pencereleriyle kapatır ve referansı temizler. */
    public static void quit() {
        if (driver != null) {
            // close yalnızca bir pencereyi; quit ise tüm WebDriver oturumunu kapatır.
            driver.quit();

            // Sonraki senaryonun temiz bir oturum açabilmesi için referans sıfırlanır.
            driver = null;
        }
    }
}
