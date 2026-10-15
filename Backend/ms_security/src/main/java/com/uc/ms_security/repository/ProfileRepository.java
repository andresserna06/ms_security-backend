package com.uc.ms_security.repository;

import com.uc.ms_security.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfileRepository extends JpaRepository<Profile, Long> {

    boolean existsByUserId(Long userId); // Verificación de si un usuario tiene un perfil

    boolean existsByPhone(String phone); // Verificación de existencia por telefono

    boolean existsByPhoneAndIdNot(String phone, Long id); // Verificación de existencia por telefono y id

    Optional<Profile> findByUserId(Long userId); // Buscando un perfil dado el identificador del usuario
}
