
package com.uc.ms_security.repository;

import com.uc.ms_security.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PermissionRepository
        extends JpaRepository<Permission, Long> {

    boolean existsByUrlAndMethod(String url, String method);

    boolean existsByUrlAndMethodAndIdNot(
            String url,
            String method,
            Long id
    );

    @Query("""
            SELECT DISTINCT rp.permission
            FROM UserRole ur
            JOIN ur.role r
            JOIN r.rolePermissions rp
            WHERE ur.user.id = :userId
            """)
    List<Permission> findAllByUserId(@Param("userId") Long userId);
}
