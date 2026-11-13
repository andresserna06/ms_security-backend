package com.uc.ms_security.controller;

import com.uc.ms_security.dto.profile.CreateProfileDTO;
import com.uc.ms_security.dto.profile.ProfileResponseDTO;
import com.uc.ms_security.dto.profile.UpdateProfileDTO;
import com.uc.ms_security.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/{userId}/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ProfileResponseDTO findByUserId(
            @PathVariable Long userId,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) {
        return profileService.findByUserId(
                userId,
                getRequesterId(jwt),
                isAdmin(authentication));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProfileResponseDTO create(
            @PathVariable Long userId,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication,
            @Valid @RequestBody CreateProfileDTO dto) {
        return profileService.create(
                userId,
                getRequesterId(jwt),
                isAdmin(authentication),
                dto);
    }

    @PutMapping
    public ProfileResponseDTO update(
            @PathVariable Long userId,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication,
            @Valid @RequestBody UpdateProfileDTO dto) {
        return profileService.update(
                userId,
                getRequesterId(jwt),
                isAdmin(authentication),
                dto);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long userId,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) {
        profileService.delete(
                userId,
                getRequesterId(jwt),
                isAdmin(authentication));
    }

    private Long getRequesterId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}