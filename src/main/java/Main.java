import java.util.*;
import java.util.concurrent.*;

public class Main {
    private static final long CHECK_INTERVAL = 1; // Интервал проверки в минутах
    private static double initialPrice = -1; // Начальная цена

    /**
     * Главный метод программы
     * @param args - аргументы командной строки
     */
    public static void main(String[] args) {
        Properties config = loadConfig();
        AeroFlotDataParsing parser = new AeroFlotDataParsing(config);
        EmailNotifier notifier = new EmailNotifier(config);

        try (Scanner scanner = new Scanner(System.in)) {
            // Ввод параметров поиска
            System.out.print("Город вылета: ");
            String departure = scanner.nextLine();

            System.out.print("Город назначения: ");
            String destination = scanner.nextLine();

            System.out.print("Дата вылета (ДД.ММ.ГГГГ): ");
            String date = scanner.nextLine();

            // Настройка периодической проверки
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(() ->
                            checkPrice(parser, notifier, departure, destination, date),
                    0, CHECK_INTERVAL, TimeUnit.MINUTES
            );
        } finally {
            parser.close();
        }
    }

    /**
     * Проверка изменения цены и отправка уведомления
     */
    private static void checkPrice(AeroFlotDataParsing parser, EmailNotifier notifier,
                                   String departure, String destination, String date) {
        try {
            double currentPrice = parser.getPrice(departure, destination, date);

            // Инициализация начальной цены
            if (initialPrice < 0) {
                initialPrice = currentPrice;
                System.out.println("Начальная цена: " + initialPrice);
                return;
            }

            // Проверка порога изменения
            double threshold = Double.parseDouble(config.getProperty("price.threshold"));
            double changePercentage = ((currentPrice - initialPrice) / initialPrice) * 100;

            if (Math.abs(changePercentage) >= threshold) {
                notifier.sendPriceAlert(initialPrice, currentPrice);
            }
        } catch (Exception e) {
            System.err.println("Ошибка проверки: " + e.getMessage());
        }
    }

    /**
     * Загрузка конфигурации из файла
     * @return объект с настройками
     */
    private static Properties loadConfig() {
        try {
            Properties config = new Properties();
            config.load(Main.class.getResourceAsStream("config.properties"));
            return config;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка загрузки конфигурации");
        }
    }
}