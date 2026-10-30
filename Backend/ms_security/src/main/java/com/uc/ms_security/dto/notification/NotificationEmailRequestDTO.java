package com.uc.ms_security.dto.notification;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NotificationEmailRequestDTO(
        String to,
        String subject,
        @JsonProperty("html_content")
        String htmlContent
) {
}
