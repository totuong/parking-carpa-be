package main.twinbackend.controller;

import lombok.RequiredArgsConstructor;
import main.twinbackend.dto.LoginRequest;
import main.twinbackend.dto.RegisterRequest;
import main.twinbackend.entity.UserAccount;
import main.twinbackend.repository.UserAccountRepository;
import main.twinbackend.service.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtService jwtService;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Vui lòng nhập đầy đủ tài khoản và mật khẩu");
        }

        if (userAccountRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Tên tài khoản đã tồn tại");
        }

        UserAccount newAccount = UserAccount.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole() != null && !request.getRole().isBlank() ? request.getRole() : "USER")
                .build();

        userAccountRepository.save(newAccount);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Đăng ký tài khoản thành công",
                        "username", newAccount.getUsername()
                ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request.getUsername() == null || request.getPassword() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Vui lòng nhập đầy đủ tài khoản và mật khẩu");
        }

        UserAccount account = userAccountRepository.findByUsername(request.getUsername())
                .orElse(null);

        if (account == null || !passwordEncoder.matches(request.getPassword(), account.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Sai tài khoản hoặc mật khẩu");
        }

        String token = jwtService.generateToken(account.getUsername());

        return ResponseEntity.ok(Map.of("token", token));
    }
}