package com.ai.library.controller;

import com.ai.library.model.Fine;
import com.ai.library.repository.FineRepository;
import com.ai.library.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/fines")
public class FineController {
    private final FineRepository fineRepository;
    private final UserRepository userRepository;
    private final com.ai.library.service.FinePolicyService finePolicyService;

    public FineController(FineRepository fineRepository, UserRepository userRepository, com.ai.library.service.FinePolicyService finePolicyService) {
        this.fineRepository = fineRepository;
        this.userRepository = userRepository;
        this.finePolicyService = finePolicyService;
    }

    @GetMapping
    public ResponseEntity<?> myFines() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return ResponseEntity.status(401).body("Unauthorized");
        String username = null;
        Object p = auth.getPrincipal();
        if (p instanceof UserDetails) username = ((UserDetails) p).getUsername();
        else if (p instanceof String) username = (String) p;
        if (username == null) return ResponseEntity.status(401).body("Unauthorized");
        var uOpt = userRepository.findByUsername(username);
        if (uOpt.isEmpty()) return ResponseEntity.status(401).body("Unauthorized");
        List<Fine> fines = fineRepository.findAllByUser(uOpt.get());
        return ResponseEntity.ok(fines);
    }

    @GetMapping("/policy")
    @PreAuthorize("hasAnyRole('LIBRARIAN','ADMIN')")
    public ResponseEntity<?> getPolicy() {
        return ResponseEntity.ok(finePolicyService.getPolicy());
    }

    @PostMapping("/policy")
    @PreAuthorize("hasAnyRole('LIBRARIAN','ADMIN')")
    public ResponseEntity<?> updatePolicy(@RequestParam double amountPerDay) {
        var p = finePolicyService.updatePolicy(amountPerDay);
        return ResponseEntity.ok(p);
    }
}
