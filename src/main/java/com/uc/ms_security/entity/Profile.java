package com.uc.ms_security.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "profiles")
@Getter
@Setter
@NoArgsConstructor
public class Profile {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @Column(
            nullable = false,
            unique = true,
            length = 20
    )
    private String phone;

    @Column(
            name = "birth_date",
            nullable = false
    )
    private LocalDate birthDate;

    // Relación 1 a 1 con User (la clave foránea user_id se aloja en esta tabla)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true
    )
    private User user; // Programación orientada a objetos - Se tendra acceso a un usuario
}
