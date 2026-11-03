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

        sendEmail(toEmail, "Tu código de verificación 2FA - JPA Sports", htmlContent);
    }

    public void sendPasswordResetLink(String toEmail, String resetLink, String userName) {
        String displayName = (userName != null && !userName.isBlank()) ? userName : "Usuario";
        String htmlContent = String.format(
                "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;\">" +
                "<h2 style=\"color: #2c3e50;\">Recuperación de Contraseña</h2>" +
                "<p>Hola <strong>%s</strong>,</p>" +
                "<p>Hemos recibido una solicitud para restablecer la contraseña asociada a tu cuenta en <strong>JPA Sports</strong>.</p>" +
                "<p>Para definir tu nueva contraseña, haz clic en el siguiente botón:</p>" +
                "<div style=\"text-align: center; margin: 30px 0;\">" +
                "<a href=\"%s\" style=\"background-color: #70B038; color: #ffffff; padding: 12px 28px; text-decoration: none; border-radius: 5px; font-weight: bold; display: inline-block;\">Restablecer Contraseña</a>" +
                "</div>" +
                "<hr style=\"border: none; border-top: 1px solid #e0e0e0; margin: 25px 0;\" />" +
                "<p style=\"font-size: 12px; color: #7f8c8d;\">Este enlace es de uso único y tiene una validez de <strong>15 minutos</strong>. Si tú no realizaste esta solicitud, puedes ignorar este mensaje; tu cuenta permanecerá segura.</p>" +
                "</div>",
                escapeJson(displayName),
                resetLink
        );

        sendEmail(toEmail, "Recuperación de contraseña - JPA Sports", htmlContent);
    }

    private void sendEmail(String toEmail, String subject, String htmlContent) {
        String jsonPayload = String.format(
                "{\"to\":\"%s\",\"subject\":\"%s\",\"html_content\":\"%s\"}",
                escapeJson(toEmail),
                escapeJson(subject),
                escapeJson(htmlContent)
        );

        try {
            log.info("Despachando notificación hacia ms_notificaciones: {}", jsonPayload);

            restClient.post()
                    .uri("/send-email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonPayload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Correo enviado exitosamente a {}", toEmail);
        } catch (Exception ex) {
            log.error("Error al comunicarse con ms_notificaciones para {}: {}", toEmail, ex.getMessage(), ex);
            throw new ApplicationException(
                    ErrorCase.INVALID_OPERATION,
                    "No fue posible enviar la notificación por correo. Intente más tarde."
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
