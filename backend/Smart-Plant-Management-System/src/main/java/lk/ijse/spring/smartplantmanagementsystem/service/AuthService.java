package lk.ijse.spring.smartplantmanagementsystem.service;

import lk.ijse.spring.smartplantmanagementsystem.dto.RegisterDTO;
import lk.ijse.spring.smartplantmanagementsystem.entity.Role;
import lk.ijse.spring.smartplantmanagementsystem.entity.User;
import lk.ijse.spring.smartplantmanagementsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public String register(RegisterDTO registerDTO) {

        if (userRepository.findByEmail(registerDTO.getEmail()).isPresent()) {
            throw new RuntimeException("Email Already Exists");
        }

        User user = User.builder()
                .email(registerDTO.getEmail())
                .password(passwordEncoder.encode(registerDTO.getPassword()))
                .role(Role.valueOf(registerDTO.getRole()))
                .build();

        userRepository.save(user);
        return "User Registered Successfully";
    }
}
