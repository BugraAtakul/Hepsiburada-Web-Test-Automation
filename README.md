# Hepsiburada Web Test Automation

Hepsiburada üzerindeki giriş, ürün arama ve sepet işlemlerini uçtan uca doğrulayan web otomasyon projesidir. Test senaryoları okunabilir Gauge spesifikasyonlarıyla tanımlanmış; tarayıcı etkileşimleri Java, Selenium ve Page Object Model kullanılarak geliştirilmiştir.

> Bu proje canlı Hepsiburada ortamında çalışır. Ağ hızı, kampanyalar, A/B testleri ve sayfanın dinamik yapısı test sürelerini veya kararlılığını etkileyebilir.

## Test Kapsamı

Ana senaryo [`specs/hepsiburada_search.spec`](specs/hepsiburada_search.spec) dosyasında bulunur ve aşağıdaki akışı doğrular:

1. Hepsiburada ana sayfası açılır ve varsa çerez bildirimi kapatılır.
2. Tanımlı kullanıcı bilgileriyle giriş yapılır.
3. `bilgisayar` kelimesi aranır ve sonuç başlığı kontrol edilir.
4. Sonuçların ikinci satırındaki ilk ürünün detay sayfası açılır ve doğru ürüne gidildiği doğrulanır.
5. Ürün, sonuç kartından değil ürün detay sayfasından sepete eklenir.
6. Sepet sayacının arttığı doğrulanır.
7. Eklenen ürünün sepet sayfasında bulunduğu kontrol edilir.
8. Ürün sepetten kaldırılır ve kaldırma işlemi doğrulanır.

Senaryo etiketleri: `login`, `arama`, `sepet`, `smoke`

## Kullanılan Teknolojiler

| Teknoloji | Sürüm / Amaç |
|---|---|
| Java | 21 |
| Gauge | Okunabilir test spesifikasyonları ve test çalıştırma |
| Selenium | 4.46.0 — web tarayıcı otomasyonu |
| AssertJ | 3.25.3 — anlaşılır doğrulamalar |
| WebDriverManager | 5.7.0 — ChromeDriver yönetimi |
| Maven | Bağımlılık ve test yaşam döngüsü yönetimi |
| Gauge HTML Report | Çalıştırma sonucu ve hata ekran görüntüleri |

## Gereksinimler

Testleri çalıştırmadan önce aşağıdaki araçların kurulu olduğundan emin olun:

- JDK 21
- Maven 3.9 veya IntelliJ IDEA ile gelen Maven
- Gauge CLI ve Java eklentisi
- Güncel Google Chrome

Kurulumları kontrol etmek için:

```powershell
java -version
mvn -version
gauge version
```

Eksik Gauge eklentileri varsa:

```powershell
gauge install java
gauge install html-report
```

## Kurulum

Projeyi klonlayın ve proje dizinine geçin:

```powershell
git clone <repository-url>
cd "Hepsiburada Web Test Automation"
```

Maven bağımlılıklarını hazırlayın:

```powershell
mvn test-compile
```

## Ortam Değişkenleri

Giriş bilgileri kaynak kodda veya repoda tutulmaz. Test başlamadan önce aşağıdaki ortam değişkenleri tanımlanmalıdır:

| Değişken | Açıklama |
|---|---|
| `HEPSIBURADA_EMAIL` | Test hesabının e-posta adresi |
| `HEPSIBURADA_PASSWORD` | Test hesabının parolası |

PowerShell oturumu için geçici olarak tanımlama:

```powershell
$env:HEPSIBURADA_EMAIL="test-hesabi@example.com"
$env:HEPSIBURADA_PASSWORD="guvenli-parola"
```

Bu değerler yalnızca açık PowerShell oturumunda geçerli olur.

> Güvenlik: Gerçek kullanıcı bilgilerini `README.md`, Gauge spec dosyaları, kaynak kod veya Git geçmişine eklemeyin.

## Testleri Çalıştırma

### Maven ile tüm testler

```powershell
mvn test
```

### Gauge ile tüm spesifikasyonlar

```powershell
gauge run specs
```

### Yalnızca ana senaryo dosyası

```powershell
gauge run specs/hepsiburada_search.spec
```

### Etikete göre çalıştırma

```powershell
gauge run --tags "smoke" specs
```

IntelliJ IDEA kullanılıyorsa spec dosyasındaki senaryo başlığının veya adımların yanındaki çalıştırma simgesi de kullanılabilir.

## Raporlar ve Loglar

Her çalıştırmanın ardından aşağıdaki çıktılar oluşturulur:

- HTML rapor: `reports/html-report/index.html`
- Gauge çalışma logu: `logs/gauge.log`
- Başarısız adım ekran görüntüleri: `.gauge/screenshots/`
- Derleme çıktıları: `target/`

Mevcut ayarda raporlar her yeni çalıştırmada yenilenir. Başarısız bir testi incelerken önce HTML raporundaki hata mesajına, ardından `logs/gauge.log` içindeki `Failed Step` kaydına bakın.

## Proje Yapısı

```text
.
├── env/default/                       # Gauge çalışma ayarları
├── specs/
│   └── hepsiburada_search.spec        # İş senaryosu ve test adımları
├── src/test/java/
│   ├── base/BaseTest.java              # Page nesnelerinin ortak yönetimi
│   ├── driver/Driver.java             # Chrome WebDriver yapılandırması
│   ├── hooks/Hooks.java               # Senaryo öncesi/sonrası yaşam döngüsü
│   ├── pages/                          # Page Object sınıfları
│   ├── steps/HepsiburadaSteps.java    # Gauge adım implementasyonları
│   └── utils/                          # Ortak element, locator ve ortam yardımcıları
├── src/test/resources/
│   └── locators/                       # Key, type ve value biçimli locator JSON dosyaları
├── manifest.json                      # Gauge proje ve eklenti tanımı
└── pom.xml                            # Maven bağımlılıkları ve eklentileri
```

## Mimari Yaklaşım

- **Page Object Model:** Sayfa locator'ları JSON dosyalarında, kullanıcı etkileşimleri `pages` paketinde tutulur.
- **Açık beklemeler:** Dinamik öğeler sabit ve uzun beklemeler yerine Selenium koşullarıyla beklenir.
- **Bağımsız test verisi:** Seçilen ürünün adı, ürün bağlantısı/kodu ve sepet sayısı senaryo sırasında kaydedilir.
- **Kararlı ürün doğrulaması:** Sepetteki ürün, benzer adlardan etkilenmemesi için ürün kodu veya takip parametrelerinden arındırılmış URL yolu ile eşleştirilir.
- **Temizleme:** Test sepete eklediği ürünü senaryo sonunda kaldırır.
- **Güvenli kimlik bilgileri:** E-posta ve parola yalnızca ortam değişkenlerinden okunur.

## Sorun Giderme

### `Gerekli ortam değişkeni tanımlanmamış`

`HEPSIBURADA_EMAIL` ve `HEPSIBURADA_PASSWORD` değişkenlerinin testi çalıştırdığınız terminal veya IDE sürecinde tanımlı olduğunu kontrol edin.

### Giriş menüsü aralıklı olarak açılmıyor

Hepsiburada üst menüsü dinamik olarak yüklenir. Header geç yüklenirse veya hover olayı site tarafından yakalanmazsa `Giriş ekranını aç` adımı zaman aşımına uğrayabilir. Hata ekran görüntüsü ile `logs/gauge.log` kaydını birlikte inceleyin ve testi temiz bir tarayıcı oturumunda yeniden çalıştırın.

### ChromeDriver veya tarayıcı sürümü hatası

Chrome'u güncelleyin. WebDriverManager uygun sürücüyü otomatik hazırladığı için sürücüyü elle projeye eklemeyin.

### Türkçe karakterler bozuk görünüyor

Proje UTF-8 kullanır. IDE dosya kodlamasını UTF-8 olarak ayarlayın ve Gauge JVM parametrelerinin `env/default/java.properties` içindeki UTF-8 ayarlarını kullandığını doğrulayın.

## Notlar

- Otomasyon gerçek kullanıcı hesabı ve canlı sepet üzerinde işlem yapar; yalnızca test amacıyla ayrılmış bir hesap kullanılması önerilir.
- Site tasarımı veya locator'lar değiştiğinde ilgili Page Object sınıfı güncellenmelidir.
- `target`, `reports`, `logs` ve `.gauge` dizinleri çalışma çıktısıdır ve Git'e eklenmez.
