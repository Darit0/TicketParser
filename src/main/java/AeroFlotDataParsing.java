import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import java.util.Properties;

public class AeroFlotDataParsing {
    private final WebDriver driver;
    private final WebDriverWait wait;
    private final Properties config;

    /**
     * Конструктор класса. Инициализирует WebDriver и настраивает параметры браузера.
     * @param config - объект с настройками из config.properties
     */
    public AeroFlotDataParsing(Properties config) {
        this.config = config;
        System.setProperty("webdriver.chrome.driver", config.getProperty("webdriver.path"));
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless", "--disable-gpu", "--window-size=1920,1200");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    /**
     * Основной метод для получения текущей цены билета
     * @param departure - город вылета
     * @param destination - город назначения
     * @param date - дата вылета в формате ДД.ММ.ГГГГ
     * @return текущая цена билета
     */
    public double getPrice(String departure, String destination, String date) {
        try {
            // Загрузка страницы поиска
            driver.get(config.getProperty("aeroflot.url"));

            // Заполнение полей и выполнение поиска
            fillCityField(".departure-field input", departure);
            fillCityField(".arrival-field input", destination);
            setDate(".departure-date input", date);
            driver.findElement(By.cssSelector(".search-button")).click();

            // Извлечение цены из активной даты
            return extractActivePrice();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при получении цены: " + e.getMessage());
        }
    }

    /**
     * Заполнение поля города с выбором из выпадающего списка
     * @param selector - CSS-селектор поля ввода
     * @param value - название города
     */
    private void fillCityField(String selector, String value) {
        WebElement field = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(selector)));
        field.clear();
        field.sendKeys(value);
        // Выбор первого предложения из выпадающего списка
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".suggestion-item"))).click();
    }

    /**
     * Установка даты в поле ввода
     * @param selector - CSS-селектор поля даты
     * @param date - дата в формате ДД.ММ.ГГГГ
     */
    private void setDate(String selector, String date) {
        WebElement dateField = driver.findElement(By.cssSelector(selector));
        dateField.sendKeys(date);
    }

    /**
     * Извлечение цены для активной (выбранной) даты
     * @return числовое значение цены
     */
    private double extractActivePrice() {
        // Ожидание появления элемента с ценой
        WebElement priceElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//div[contains(@class, 'price-chart__item--active')]//div[contains(@class, 'price-chart__item-price')]")
        ));
        return parsePrice(priceElement.getText());
    }

    /**
     * Преобразование текстового представления цены в число
     * @param priceText - строка с ценой (например: "16 723 ₽")
     * @return числовое значение цены
     */
    private double parsePrice(String priceText) {
        return Double.parseDouble(priceText
                .replaceAll("[^\\d]", "") // Удаление всех нецифровых символов
                .replace("₽", "")
                .trim()
        );
    }

    /**
     * Закрытие браузера и освобождение ресурсов
     */
    public void close() {
        driver.quit();
    }
}