package com.uc.ms_security.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "sessions")
@Getter
@Setter
@NoArgsConstructor
public class Session {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String token;

    @Column(
            nullable = false
    )
    private LocalDateTime expiration;

    @Column(
            name = "code_2fa",
            length = 10
    )
    private String code2FA;

    @Column(
        name = "is_code_used" 
    )
    private boolean isCodeUsed = false;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;
}
