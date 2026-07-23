package model;

import java.net.URI;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Bir ürünü URL/ürün koduyla tanımlar ve görünen adını doğrulama için saklar. */
public record ProductIdentity(String name, String url, String productCode) {

    private static final double MIN_NAME_OVERLAP_RATIO = 0.80;
    private static final Pattern PRODUCT_CODE_PATTERN =
            Pattern.compile("(?i)-(?:p|pm)-([a-z0-9]+)(?:/|$)");

    /** Ürün kartındaki ad ve bağlantıdan doğrulamada kullanılacak kimliği üretir. */
    public static ProductIdentity from(String name, String url) {
        String validatedName = requireNotBlank(name, "Ürün adı okunamadı.");
        String validatedUrl = requireNotBlank(url, "Ürün bağlantısı okunamadı.");
        return new ProductIdentity(
                validatedName,
                validatedUrl,
                extractProductCode(validatedUrl));
    }

    /** Aday bağlantının aynı ürüne ait olup olmadığını kod veya tam URL yolu ile kontrol eder. */
    public boolean matchesUrl(String candidateUrl) {
        if (candidateUrl == null || candidateUrl.isBlank()) {
            return false;
        }

        try {
            String candidateProductCode = extractProductCode(candidateUrl);
            if (productCode != null && candidateProductCode != null) {
                return productCode.equalsIgnoreCase(candidateProductCode);
            }

            return canonicalPath(url).equals(canonicalPath(candidateUrl));
        } catch (IllegalArgumentException ignored) {
            // Yeni sekme ilk açılırken görülebilen about:blank gibi geçici adresler ürün değildir.
            return false;
        }
    }

    /**
     * Sepette görünen ürün adını, büyük-küçük harf, Türkçe karakter,
     * noktalama ve fazla boşluk farklarından bağımsız olarak karşılaştırır.
     */
    public boolean matchesName(String candidateName) {
        if (candidateName == null || candidateName.isBlank()) {
            return false;
        }

        String expectedName = normalizeName(name);
        String actualName = normalizeName(candidateName);
        if (expectedName.isBlank() || actualName.isBlank()) {
            return false;
        }

        // Site bazen arama kartına yinelenen marka ön eki, sepete ise ek bilgi koyabilir.
        // Kısa bir marka/model parçasının tek başına eşleşmesini önlemek için kısa olan
        // başlığın uzun olanın en az %80'ini oluşturması gerekir.
        String shorterName = expectedName.length() <= actualName.length()
                ? expectedName
                : actualName;
        String longerName = expectedName.length() > actualName.length()
                ? expectedName
                : actualName;

        return longerName.contains(shorterName)
                && (double) shorterName.length() / longerName.length() >= MIN_NAME_OVERLAP_RATIO;
    }

    /** Ürün adını karşılaştırmaya uygun sade ve kararlı bir biçime dönüştürür. */
    private static String normalizeName(String value) {
        String withoutDiacritics = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");

        return withoutDiacritics
                .toLowerCase(Locale.ROOT)
                .replace('ı', 'i')
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    /** URL'nin sorgu ve takip parametrelerini dışarıda bırakan ürün yolunu döndürür. */
    private static String canonicalPath(String productUrl) {
        try {
            String path = URI.create(productUrl.trim()).getPath();
            if (path == null || path.isBlank()) {
                throw new IllegalArgumentException("Ürün URL'sinde geçerli bir yol bulunamadı: " + productUrl);
            }

            String normalizedPath = path.replaceAll("/+$", "");
            return normalizedPath.toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Geçersiz ürün URL'si: " + productUrl, exception);
        }
    }

    /** Hepsiburada ürün URL'sindeki -p- veya -pm- bölümünden kararlı ürün kodunu çıkarır. */
    private static String extractProductCode(String productUrl) {
        String path = canonicalPath(productUrl);
        Matcher matcher = PRODUCT_CODE_PATTERN.matcher(path);
        return matcher.find() ? matcher.group(1).toUpperCase(Locale.ROOT) : null;
    }

    private static String requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
        return value.trim();
    }
}
