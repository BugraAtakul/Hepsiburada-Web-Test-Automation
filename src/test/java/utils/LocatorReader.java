package utils;

import org.openqa.selenium.By;
import org.openqa.selenium.json.Json;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** JSON dosyalarındaki key, type ve value alanlarından Selenium locator üretir. */
public final class LocatorReader {

    private static final Map<String, Map<String, By>> LOCATOR_CACHE =
            new ConcurrentHashMap<>();

    private LocatorReader() {
    }

    /** İstenen sayfa dosyasındaki locator'ı anahtarıyla döndürür. */
    public static By getLocator(String locatorFile, String key) {
        Map<String, By> locators = LOCATOR_CACHE.computeIfAbsent(
                locatorFile, LocatorReader::readLocatorFile);
        By locator = locators.get(key);

        if (locator == null) {
            throw new IllegalArgumentException(
                    "Locator anahtarı bulunamadı: " + locatorFile + ".json -> " + key);
        }

        return locator;
    }

    private static Map<String, By> readLocatorFile(String locatorFile) {
        String resourcePath = "locators/" + locatorFile + ".json";
        InputStream inputStream = LocatorReader.class.getClassLoader()
                .getResourceAsStream(resourcePath);

        if (inputStream == null) {
            throw new IllegalStateException("Locator dosyası bulunamadı: " + resourcePath);
        }

        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            List<Map<String, Object>> definitions =
                    new Json().toType(reader, Json.LIST_OF_MAPS_TYPE);
            Map<String, By> locators = new LinkedHashMap<>();

            for (Map<String, Object> definition : definitions) {
                String key = requiredText(definition, "key", resourcePath);
                String type = requiredText(definition, "type", resourcePath);
                String value = requiredText(definition, "value", resourcePath);

                if (locators.putIfAbsent(key, createBy(type, value)) != null) {
                    throw new IllegalStateException(
                            "Locator anahtarı birden fazla tanımlanmış: "
                                    + resourcePath + " -> " + key);
                }
            }

            return Map.copyOf(locators);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Locator dosyası okunamadı: " + resourcePath, exception);
        }
    }

    private static String requiredText(
            Map<String, Object> definition, String field, String resourcePath) {
        Object rawValue = definition.get(field);
        String value = rawValue == null ? "" : rawValue.toString().trim();

        if (value.isBlank()) {
            throw new IllegalStateException(
                    "Locator alanı boş olamaz: " + resourcePath + " -> " + field);
        }

        return value;
    }

    private static By createBy(String type, String value) {
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "id" -> By.id(value);
            case "css", "cssselector" -> By.cssSelector(value);
            case "xpath" -> By.xpath(value);
            default -> throw new IllegalArgumentException(
                    "Desteklenmeyen locator tipi: " + type);
        };
    }
}
