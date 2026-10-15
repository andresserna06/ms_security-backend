package com.uc.ms_security.repository;

import com.uc.ms_security.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SessionRepository
        extends JpaRepository<Session, Long> {

    List<Session> findAllByUserId(Long userId); // Esto es la consulta para traerse las sessiones de un solo usuario

    Optional<Session> findByIdAndUserId( // Encontrar una session dado un id de session y un id de user
            Long sessionId,
            Long userId
    );

    boolean existsByToken(String token);

    boolean existsByTokenAndIdNot( // verifica que exista un token para evitar posibles ataques
            String token,
            Long id
    );
}
