package lk.ijse.spring.smartplantmanagementsystem.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lk.ijse.spring.smartplantmanagementsystem.dto.*;
import lk.ijse.spring.smartplantmanagementsystem.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthDTO authDTO, HttpServletResponse response){
        AuthResponseDTO authResponseDTO = authService.authenticate(authDTO);

        Cookie refreshCookie = new Cookie("refreshToken", authResponseDTO.getRefreshToken());
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(7*24*60*60);
        response.addCookie(refreshCookie);

        return ResponseEntity.ok(Map.of("accessToken", authResponseDTO.getAccessToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAccessToken(@CookieValue(value = "refreshToken", required = false) String refreshToken) {
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No refresh token found");
        }

        AuthResponseDTO response = authService.refreshAccessToken(refreshToken);
        return ResponseEntity.ok(Map.of("accessToken", response.getAccessToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestHeader(value = "Authorization", required = false) String accessToken,
            @CookieValue(value = "refreshToken", required = false) String refreshToken, HttpServletResponse response) {

        if (refreshToken != null) {
            authService.logout(accessToken, refreshToken);
        }

        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        cookie.setSecure(true);
        cookie.setAttribute("SameSite", "Strict");

        return ResponseEntity.ok(Map.of("message", "Logout successfully..!"));
    }

    @GetMapping("/me")
    public ResponseEntity<MeDTO> me(@AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails user) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String email = user.getUsername();
        String role = user.getAuthorities().stream().findFirst().map(Object::toString).orElse("USER");
        return ResponseEntity.ok(new MeDTO(email, role));
    }

}
