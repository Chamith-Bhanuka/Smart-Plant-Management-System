package lk.ijse.spring.smartplantmanagementsystem.dto;

import lombok.Data;

import java.util.Set;

@Data
public class RegisterDTO {
    private String email;
    private String role;
    private String password;
    private String about;
    Set<String> specializations;
    private String education;
    private String experience;
}
