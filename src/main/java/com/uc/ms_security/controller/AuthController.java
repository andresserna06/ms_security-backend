package com.uc.ms_security.controller;

import com.uc.ms_security.dto.auth.AuthResponseDTO;
import com.uc.ms_security.dto.auth.GoogleLoginRequestDTO;
import com.uc.ms_security.dto.auth.LoginInitResponseDTO;
import com.uc.ms_security.dto.auth.LoginRequestDTO;
import com.uc.ms_security.dto.auth.Verify2FaRequestDTO;
import com.uc.ms_security.dto.user.CreateUserDTO;
import com.uc.ms_security.dto.user.UserResponseDTO;
import com.uc.ms_security.entity.AuthProvider;
import com.uc.ms_security.service.AuthService;
import com.uc.ms_security.service.OAuthService;
import com.uc.ms_security.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OAuthService oAuthService;
    private final UserService userService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseDTO register(@Valid @RequestBody CreateUserDTO dto) {
        return userService.create(dto);
    }

    @PostMapping("/login")
    public LoginInitResponseDTO login(@Valid @RequestBody LoginRequestDTO dto) {
        return authService.login(dto);
    }

    @PostMapping("/verify-2fa")
    public AuthResponseDTO verify2Fa(@Valid @RequestBody Verify2FaRequestDTO dto) {
        return authService.verify2Fa(dto);
    }

    @PostMapping("/google")
    public AuthResponseDTO loginWithGoogle(@Valid @RequestBody GoogleLoginRequestDTO dto) {
        return oAuthService.loginWithProvider(AuthProvider.GOOGLE, dto.getIdToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        authService.logout(authHeader);
    }
}
