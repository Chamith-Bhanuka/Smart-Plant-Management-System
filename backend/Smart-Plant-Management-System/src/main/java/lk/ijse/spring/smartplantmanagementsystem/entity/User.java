package lk.ijse.spring.smartplantmanagementsystem.entity;

import jakarta.persistence.*;

public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String password;
}
