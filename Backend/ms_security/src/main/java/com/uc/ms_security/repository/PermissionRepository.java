package com.uc.ms_security.repository;

import com.uc.ms_security.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    boolean existsByUrlAndMethod(String url, String method);

    boolean existsByUrlAndMethodAndIdNot(String url, String method, Long id);

    Optional<Permission> findByUrlAndMethod(String url, String method);

    List<Permission> findByModel(String model);

    List<Permission> findByMethod(String method);
}
