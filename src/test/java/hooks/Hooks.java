package hooks;

import com.thoughtworks.gauge.AfterScenario;
import com.thoughtworks.gauge.BeforeScenario;
import driver.Driver;

/**
 * Gauge senaryolarının yaşam döngüsüne bağlanan hazırlık ve temizlik işlemleridir.
 * Hook (kanca), senaryo adımlarına yazılmadan otomatik çalışan özel metot demektir.
 */
public class Hooks {

    // @BeforeScenario sayesinde her senaryodan hemen önce Gauge tarafından çağrılır.
    @BeforeScenario
    public void startBrowser() {
        System.out.println("Test senaryosu başlatılıyor...");
        // Her senaryonun kullanacağı Chrome oturumunu hazırlar.
        Driver.init();
    }

    // @AfterScenario, adımlar başarılı olsa da hata verse de temizlik için çağrılır.
    @AfterScenario
    public void closeBrowser() {
        // AfterScenario başarılı veya hatalı her çalıştırmanın sonunda çağrılır.
        System.out.println("Senaryo çalışması sona erdi, tarayıcı kapatılıyor...");
        // Tarayıcının arka planda açık kalmasını ve kaynak tüketmesini önler.
        Driver.quit();
    }
}
