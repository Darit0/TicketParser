import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
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
            driver.get(config.getProperty("aeroflot.url"));

            // Ожидание загрузки основной формы
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector(".main-module__search-form__inner")));

            // Заполнение полей
            fillCityField("#ticket-city-departure-0-booking", departure);
            fillCityField("#ticket-city-arrival-0-booking", destination);
            setDate("#ticket-date-from-booking", date);

            // Клик по кнопке поиска
            WebElement searchButton = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("button.main-module__button--lg")));
            searchButton.click();

            // Ожидание загрузки результатов
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector(".price-chart")));

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
        field.sendKeys(Keys.CONTROL + "a");
        field.sendKeys(Keys.DELETE);
        field.sendKeys(value);

        // Ожидание появления подсказки с точным совпадением города
        By suggestionLocator = By.xpath(String.format(
                "//div[contains(@class, 'suggestion-item') and .//*[contains(text(), '%s')]]",
                value
        ));
        WebElement suggestion = wait.until(ExpectedConditions.elementToBeClickable(suggestionLocator));
        suggestion.click();
    }

    /**
     * Установка даты в поле ввода
     * @param selector - CSS-селектор поля даты
     * @param date - дата в формате ДД.ММ.ГГГГ
     */
    private void setDate(String selector, String date) {
        WebElement dateField = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(selector)));
        dateField.click(); // Активируем календарь

        // Ожидание загрузки календаря
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector(".pika-single")
        ));

        dateField.sendKeys(Keys.CONTROL + "a");
        dateField.sendKeys(date);
        dateField.sendKeys(Keys.ENTER);
    }

    /**
     * Извлечение цены для активной (выбранной) даты
     * @return числовое значение цены
     */
    private double extractActivePrice() {
        // Обновленный XPath с учетом новой структуры
        WebElement priceElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//div[contains(@class, 'price-chart__item--active')]//div[contains(@class, 'price-chart__item-price')]")));
        return parsePrice(priceElement.getText());
    }

    /**
     * Преобразование текстового представления цены в число
     * @param priceText - строка с ценой (например: "25 901 a")
     * @return числовое значение цены
     */
    private double parsePrice(String priceText) {
        return Double.parseDouble(priceText
                .replaceAll("[^\\d]", "") // Удаление всех нецифровых символов
                .replaceAll("\\s+", "")   // Удаление неразрывных пробелов (nbsp)
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