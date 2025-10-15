package com.ai.library.repository;

import com.ai.library.model.Fine;
import com.ai.library.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FineRepository extends JpaRepository<Fine, Long> {
	List<Fine> findAllByUser(User user);
}
