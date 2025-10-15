package com.ai.library.repository;

import com.ai.library.model.FinePolicy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinePolicyRepository extends JpaRepository<FinePolicy, Long> {
}
