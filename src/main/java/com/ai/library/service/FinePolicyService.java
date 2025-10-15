package com.ai.library.service;

import com.ai.library.model.FinePolicy;
import com.ai.library.repository.FinePolicyRepository;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;

@Service
public class FinePolicyService {
    private final FinePolicyRepository repo;

    public FinePolicyService(FinePolicyRepository repo) {
        this.repo = repo;
    }

    @PostConstruct
    public void ensurePolicy() {
        List<FinePolicy> all = repo.findAll();
        if (all.isEmpty()) {
            FinePolicy p = new FinePolicy();
            p.setAmountPerDay(1.0);
            repo.save(p);
        }
    }

    public FinePolicy getPolicy() {
        return repo.findAll().stream().findFirst().orElseGet(() -> {
            FinePolicy p = new FinePolicy(); p.setAmountPerDay(1.0); return repo.save(p);
        });
    }

    public FinePolicy updatePolicy(double amountPerDay) {
        FinePolicy p = getPolicy();
        p.setAmountPerDay(amountPerDay);
        return repo.save(p);
    }
}
