package utils;

/** Test için zorunlu değerleri işletim sistemi ortam değişkenlerinden okur. */
public final class EnvironmentReader {

    private EnvironmentReader() {
    }

    /** Verilen isimdeki zorunlu ve boş olmayan ortam değişkenini döndürür. */
    public static String require(String key) {
        String value = System.getenv(key);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Gerekli ortam değişkeni tanımlanmamış: " + key);
        }

        return value;
    }
}
