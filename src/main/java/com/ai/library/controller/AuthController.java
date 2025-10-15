package com.ai.library.controller;

import com.ai.library.dto.AuthRequest;
import com.ai.library.dto.AuthResponse;
import com.ai.library.model.Role;
import com.ai.library.model.User;
import com.ai.library.repository.UserRepository;
import com.ai.library.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest req) {
        if (userRepository.findByUsername(req.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Username exists");
        }

        User u = new User();
        u.setUsername(req.getUsername());
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        u.setRoles(new HashSet<>());
        u.getRoles().add(Role.ROLE_STUDENT);
        userRepository.save(u);
        return ResponseEntity.ok("registered");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest req) {
        var userOpt = userRepository.findByUsername(req.getUsername());
        if (userOpt.isEmpty()) return ResponseEntity.status(401).body("invalid credentials");
        User u = userOpt.get();
        if (!passwordEncoder.matches(req.getPassword(), u.getPassword())) return ResponseEntity.status(401).body("invalid credentials");
        var roles = u.getRoles().stream().map(Enum::name).collect(java.util.stream.Collectors.toSet());
        String token = jwtUtil.generateToken(u.getUsername(), roles);
        return ResponseEntity.ok(new AuthResponse(token));
    }
}
