package lk.ijse.spring.smartplantmanagementsystem.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lk.ijse.spring.smartplantmanagementsystem.dto.APIResponse;
import lk.ijse.spring.smartplantmanagementsystem.dto.AuthDTO;
import lk.ijse.spring.smartplantmanagementsystem.dto.RegisterDTO;
import lk.ijse.spring.smartplantmanagementsystem.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<APIResponse> registerUser (@RequestBody RegisterDTO registerDTO) {
        return ResponseEntity.ok(
                new APIResponse(
                        200,
                        "User registered successfully",
                        authService.register(registerDTO)
                )
        );
    }

}
