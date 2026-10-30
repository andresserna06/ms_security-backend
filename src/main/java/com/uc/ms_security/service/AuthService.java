package com.uc.ms_security.service;

import com.uc.ms_security.dto.auth.AuthResponseDTO;
import com.uc.ms_security.dto.auth.LoginInitResponseDTO;
import com.uc.ms_security.dto.auth.LoginRequestDTO;
import com.uc.ms_security.dto.auth.Verify2FaRequestDTO;
import com.uc.ms_security.entity.Session;
import com.uc.ms_security.entity.User;
import com.uc.ms_security.exception.ApplicationException;
import com.uc.ms_security.exception.ErrorCase;
import com.uc.ms_security.repository.SessionRepository;
import com.uc.ms_security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final NotificationService notificationService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public LoginInitResponseDTO login(LoginRequestDTO dto) {
        User user = userRepository
                .findByEmail(dto.getEmail())
                .orElseThrow(this::unauthorized);

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new ApplicationException(
                    ErrorCase.UNAUTHORIZED,
                    "El usuario se encuentra inhabilitado"
            );
        }

        boolean validPassword = passwordEncoder.matches(
                dto.getPassword(),
                user.getPassword()
        );

        if (!validPassword) {
            throw unauthorized();
        }

        // Generar código numérico de 6 dígitos (000000 - 999999)
        String code2FA = String.format("%06d", secureRandom.nextInt(1_000_000));

        // Registrar la sesión con el código 2FA y expiración en 5 minutos
        Session session = new Session();
        session.setUser(user);
        session.setCode2FA(code2FA);
        session.setExpiration(LocalDateTime.now().plusMinutes(5));
        session.setCodeUsed(false);
        sessionRepository.save(session);

        // Enviar el código por correo a través de ms_notificaciones
        notificationService.send2FaCode(user.getEmail(), code2FA);

        return new LoginInitResponseDTO(
                "Código de autenticación enviado al correo electrónico",
                user.getEmail()
        );
    }

    @Transactional
    public AuthResponseDTO verify2Fa(Verify2FaRequestDTO dto) {
        User user = userRepository
                .findWithRolesAndPermissionsByEmail(dto.getEmail())
                .orElseGet(() -> userRepository.findByEmail(dto.getEmail())
                        .orElseThrow(this::unauthorized));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new ApplicationException(
                    ErrorCase.UNAUTHORIZED,
                    "El usuario se encuentra inhabilitado"
            );
        }

        Session session = sessionRepository
                .findTopByUser_EmailOrderByIdDesc(dto.getEmail())
                .orElseThrow(this::unauthorized);

        if (session.isCodeUsed()) {
            throw new ApplicationException(
                    ErrorCase.UNAUTHORIZED,
                    "El código de verificación ya ha sido utilizado"
            );
        }

        if (session.getExpiration().isBefore(LocalDateTime.now())) {
            throw new ApplicationException(
                    ErrorCase.UNAUTHORIZED,
                    "El código de verificación ha expirado"
            );
        }

        if (!session.getCode2FA().equals(dto.getCode())) {
            throw unauthorized();
        }

        // Marcar el código como usado para impedir reutilización
        session.setCodeUsed(true);

        // Generar el token JWT con roles y permisos
        AuthResponseDTO authResponse = jwtService.generateToken(user);

        // Almacenar el token emitido en la sesión
        session.setToken(authResponse.accessToken());
        sessionRepository.save(session);

        return authResponse;
    }

    @Transactional
    public void logout(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }
        String token = authHeader.substring(7).trim();
        sessionRepository.findByToken(token).ifPresent(session -> {
            session.setExpiration(LocalDateTime.now());
            sessionRepository.save(session);
        });
    }

    private ApplicationException unauthorized() {
        return new ApplicationException(
                ErrorCase.UNAUTHORIZED,
                "Credenciales o código incorrectos"
        );
    }
}