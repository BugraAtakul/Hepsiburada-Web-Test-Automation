package config;

/**
 * Testin çalıştığı işletim sistemi ortamından güvenli yapılandırma değerleri okur.
 *
 * <p>E-posta ve parola gibi gizli bilgiler kaynak koda yazılmaz. Bunun yerine
 * {@link System#getenv(String)} ile ortam değişkenlerinden alınır. Böylece bu
 * bilgiler Git deposuna yanlışlıkla gönderilmez.</p>
 */
public final class EnvironmentConfig {

    /**
     * Bu sınıf yalnızca statik yardımcı metot içerir; nesnesinin oluşturulmasına
     * ihtiyaç yoktur. Private constructor, {@code new EnvironmentConfig()}
     * kullanımını derleme aşamasında engeller.
     */
    private EnvironmentConfig() {
    }

    /**
     * Verilen isimdeki zorunlu ortam değişkenini okur.
     *
     * @param key okunacak ortam değişkeninin adı; örneğin HEPSIBURADA_EMAIL
     * @return ortam değişkeninde saklanan, boş olmayan değer
     * @throws IllegalStateException değişken tanımlı değilse veya değeri boşsa
     */
    public static String require(String key) {
        // getenv null döndürürse bu isimde bir ortam değişkeni tanımlanmamıştır.
        String value = System.getenv(key);

        // isBlank; boş metni, yalnızca boşluk içeren metni ve benzerlerini reddeder.
        if (value == null || value.isBlank()) {
            // Testin eksik bilgiyle ilerleyip daha belirsiz bir giriş hatası vermesini önler.
            throw new IllegalStateException(
                    "Gerekli ortam değişkeni tanımlanmamış: " + key);
        }

        // Kontrollerden geçen ortam değişkeni çağıran metoda teslim edilir.
        return value;
    }
}
