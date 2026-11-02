package com.uc.ms_security.repository;

import com.uc.ms_security.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = {"profile"})
    Optional<User> findWithProfileById(Long id); // El "With" es un JOIN internamente - Busca el usuario con el perfil del ID

    // aca se hace un join
    @EntityGraph(attributePaths = {"sessions"}) // aca se le dice al user que tiene una lista llamada sessions
    Optional<User> findWithSessionsById(Long id); // toma esas sessions y trae las pertenecientes a x usuario

        // Aquí hace un JOIN de tres niveles (Tres Tablas), del usuario va a la intermedia (UserRole) y de la intermedia a los roles

        // SELECT * FROM user_roles ON user_roles.user_id JOIN roles on user_roles.role_id = roles.id WHERE user.id = ?
    @EntityGraph(attributePaths = {"userRoles", "userRoles.role"})
    Optional<User> findWithRolesById(Long id); // Dado un ID de usuario encuentre unos roles

    @EntityGraph(attributePaths = {
            "userRoles",
            "userRoles.role"
    })
    Optional<User> findWithRolesAndPermissionsByEmail(String email);
}
