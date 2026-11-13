package com.uc.ms_security.service;

import com.uc.ms_security.dto.auth.AuthResponseDTO;
import com.uc.ms_security.dto.auth.ForgotPasswordRequestDTO;
import com.uc.ms_security.dto.auth.LoginInitResponseDTO;
import com.uc.ms_security.dto.auth.LoginRequestDTO;
import com.uc.ms_security.dto.auth.ResetPasswordRequestDTO;
import com.uc.ms_security.dto.auth.Verify2FaRequestDTO;
import com.uc.ms_security.entity.AuthProvider;
import com.uc.ms_security.entity.PasswordResetToken;
import com.uc.ms_security.entity.Session;
import com.uc.ms_security.entity.User;
import com.uc.ms_security.exception.ApplicationException;
import com.uc.ms_security.exception.ErrorCase;
import com.uc.ms_security.repository.PasswordResetTokenRepository;
import com.uc.ms_security.repository.SessionRepository;
import com.uc.ms_security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final NotificationService notificationService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

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

        // Las cuentas creadas con un proveedor externo (OAuth) no tienen contraseña de la plataforma
        if (user.getAuthProvider() != AuthProvider.LOCAL || user.getPassword() == null) {
            throw new ApplicationException(
                    ErrorCase.UNAUTHORIZED,
                    "Esta cuenta fue registrada con " + formatProviderName(user.getAuthProvider()) + ". Inicie sesión con ese proveedor."
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

        // Almacenar el token emitido en la sesión y actualizar su expiración a la del JWT (15 min)
        session.setToken(authResponse.accessToken());
        session.setExpiration(LocalDateTime.ofInstant(
                authResponse.expiresAt(), ZoneId.systemDefault()));
        sessionRepository.save(session);

        return authResponse;
    }

    @Transactional
    public Map<String, String> forgotPassword(ForgotPasswordRequestDTO dto) {
        User user = userRepository.findByEmail(dto.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new ApplicationException(
                        ErrorCase.NOT_FOUND,
                        "No existe una cuenta registrada con el correo indicado."
                ));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new ApplicationException(
                    ErrorCase.UNAUTHORIZED,
                    "El usuario se encuentra inhabilitado."
            );
        }

        // Criterio de aceptación: disponible únicamente para cuentas de correo y contraseña (LOCAL)
        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            throw new ApplicationException(
                    ErrorCase.INVALID_OPERATION,
                    "Esta cuenta fue registrada mediante " + formatProviderName(user.getAuthProvider()) +
                    ". La recuperación de contraseña no se realiza desde la plataforma; debes gestionar tus credenciales directamente con tu proveedor."
            );
        }

        // Invalidar tokens previos del usuario para garantizar un único flujo activo
        passwordResetTokenRepository.deleteByUser(user);

        // Generar token seguro temporal válido por 15 minutos
        String token = UUID.randomUUID().toString();
        LocalDateTime expiration = LocalDateTime.now().plusMinutes(15);

        PasswordResetToken resetToken = new PasswordResetToken(token, user, expiration);
        passwordResetTokenRepository.save(resetToken);

        // Construir enlace y enviar correo vía ms_notificaciones
        String resetLink = frontendUrl + "/#/reset-password?token=" + token;
        notificationService.sendPasswordResetLink(user.getEmail(), resetLink, user.getName());

        return Map.of("message", "Se ha enviado un enlace de recuperación a tu correo electrónico. Es válido durante 15 minutos.");
    }

    @Transactional
    public Map<String, String> resetPassword(ResetPasswordRequestDTO dto) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(dto.getToken().trim())
                .orElseThrow(() -> new ApplicationException(
                        ErrorCase.NOT_FOUND,
                        "El enlace o token de recuperación no es válido o no existe."
                ));

        if (resetToken.isUsed()) {
            throw new ApplicationException(
                    ErrorCase.INVALID_OPERATION,
                    "Este enlace de recuperación ya ha sido utilizado."
            );
        }

        if (resetToken.isExpired()) {
            throw new ApplicationException(
                    ErrorCase.INVALID_OPERATION,
                    "El enlace de recuperación ha expirado. Por favor solicita uno nuevo."
            );
        }

        User user = resetToken.getUser();
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new ApplicationException(
                    ErrorCase.UNAUTHORIZED,
                    "El usuario se encuentra inhabilitado."
            );
        }

        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            throw new ApplicationException(
                    ErrorCase.INVALID_OPERATION,
                    "Las cuentas registradas mediante proveedores externos deben gestionar su acceso directamente con el proveedor."
            );
        }

        // Actualizar contraseña encriptada
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);

        // Marcar token como consumido
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        // Criterio de aceptación: invalidar las sesiones anteriores del usuario
        sessionRepository.deleteByUserId(user.getId());

        return Map.of("message", "Contraseña restablecida exitosamente. Ya puedes iniciar sesión con tu nueva contraseña.");
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

    private String formatProviderName(AuthProvider provider) {
        if (provider == null) {
            return "un proveedor externo";
        }
        return switch (provider) {
            case GOOGLE -> "Google";
            case AZURE, MICROSOFT -> "Microsoft Azure";
            case GITHUB -> "Github";
            case LOCAL -> "correo electrónico";
        };
    }
}
