import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

public class EmailNotifier {
    private final Properties config;
    private final Session session;

    /**
     * Конструктор класса. Инициализирует SMTP-сессию.
     * @param config - объект с настройками из config.properties
     */
    public EmailNotifier(Properties config) {
        this.config = config;
        this.session = createSession();
    }

    /**
     * Создание SMTP-сессии с авторизацией
     * @return настроенный объект Session
     */
    private Session createSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host", config.getProperty("smtp.host"));
        props.put("mail.smtp.port", config.getProperty("smtp.port"));
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        return Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(
                        config.getProperty("email.user"),
                        config.getProperty("email.password")
                );
            }
        });
    }

    /**
     * Отправка уведомления об изменении цены
     * @param initialPrice - начальная цена
     * @param currentPrice - текущая цена
     */
    public void sendPriceAlert(double initialPrice, double currentPrice) {
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(config.getProperty("email.from")));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(config.getProperty("email.to")));

            // Расчет изменения цены в процентах
            double difference = currentPrice - initialPrice;
            double percentage = (difference / initialPrice) * 100;

            // Формирование содержимого письма
            String subject = String.format("Цена изменилась на %.2f%%", percentage);
            String body = String.format(
                    "Исходная цена: %.2f ₽\nТекущая цена: %.2f ₽\nИзменение: %+.2f ₽ (%+.2f%%)",
                    initialPrice, currentPrice, difference, percentage
            );

            message.setSubject(subject);
            message.setText(body);
            Transport.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка отправки письма: " + e.getMessage());
        }
    }
}