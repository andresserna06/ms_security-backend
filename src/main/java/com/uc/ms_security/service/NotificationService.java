package com.uc.ms_security.service;

import com.uc.ms_security.exception.ApplicationException;
import com.uc.ms_security.exception.ErrorCase;
import java.net.http.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final RestClient restClient;

    public NotificationService(
            @Value("${services.notifications.url:http://localhost:8000}") String notificationsUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.restClient = RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .baseUrl(notificationsUrl)
                .build();
    }

    public void send2FaCode(String toEmail, String code) {
        String htmlContent = String.format(
                "<h3>Código de verificación 2FA</h3>" +
                "<p>Tu código de acceso es: <strong>%s</strong></p>" +
                "<p>Este código expira en 5 minutos y sólo puede ser utilizado una vez.</p>",
                code
        );

        String jsonPayload = String.format(
                "{\"to\":\"%s\",\"subject\":\"%s\",\"html_content\":\"%s\"}",
                escapeJson(toEmail),
                escapeJson("Tu código de verificación 2FA - JPA Sports"),
                escapeJson(htmlContent)
        );

        try {
            log.info("Despachando notificación 2FA hacia ms_notificaciones: {}", jsonPayload);

            restClient.post()
                    .uri("/send-email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonPayload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Código 2FA enviado exitosamente a {}", toEmail);
        } catch (Exception ex) {
            log.error("Error al comunicarse con ms_notificaciones al enviar 2FA a {}: {}", toEmail, ex.getMessage(), ex);
            throw new ApplicationException(
                    ErrorCase.INVALID_OPERATION,
                    "No fue posible enviar el código de verificación al correo. Intente más tarde."
            );
        }
    }

    private String escapeJson(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r");
    }
}

