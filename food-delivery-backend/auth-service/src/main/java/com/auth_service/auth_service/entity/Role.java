package com.auth_service.auth_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity // Marks this class as a JPA entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // uses auto-increment (MySQL style)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;
}

/**
 * 
 * @Builder
 *          Enables builder pattern (clean object creation)
 *          Role role = Role.builder()
 *          .name("ADMIN")
 *          .build();
 */