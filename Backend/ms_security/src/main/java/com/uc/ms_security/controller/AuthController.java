package com.uc.ms_security.controller;

import com.uc.ms_security.dto.auth.AuthResponseDTO;
import com.uc.ms_security.dto.auth.LoginRequestDTO;
import com.uc.ms_security.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth") // Cualquier petición web que empiece con la dirección /api/auth debe ser enviada a este controlador
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public AuthResponseDTO login(
            @Valid
            @RequestBody LoginRequestDTO dto) { // "Toma el texto JSON que el usuario envió oculto en el cuerpo de su petición HTTP (el correo y la contraseña), y conviértelo automáticamente en el objeto Java LoginRequestDTO

        return authService.login(dto);
    }
}