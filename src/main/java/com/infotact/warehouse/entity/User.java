package com.infotact.warehouse.entity;


import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

@Data
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String name;

    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private String contactNumber;

    private String password;

    // 🔥 ADD THE ANNOTATION RIGHT HERE:
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Role role;

    private String status = "INACTIVE";
}
