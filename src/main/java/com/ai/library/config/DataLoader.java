package com.ai.library.config;

import com.ai.library.model.Book;
import com.ai.library.model.Role;
import com.ai.library.model.User;
import com.ai.library.repository.BookRepository;
import com.ai.library.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
public class DataLoader {
    @Bean
    CommandLineRunner init(UserRepository userRepository, BookRepository bookRepository, PasswordEncoder encoder) {
        return args -> {
            if (userRepository.count() == 0) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(encoder.encode("adminpass"));
                admin.setEmail("admin@example.com");
                admin.setRoles(Set.of(Role.ROLE_ADMIN));
                userRepository.save(admin);

                User student = new User();
                student.setUsername("student");
                student.setPassword(encoder.encode("studentpass"));
                student.setEmail("student@example.com");
                student.setRoles(Set.of(Role.ROLE_STUDENT));
                userRepository.save(student);
            }

            if (bookRepository.count() == 0) {
                Book b = new Book();
                b.setTitle("Introduction to AI");
                b.setAuthor("A. Author");
                b.setIsbn("ISBN-001");
                b.setGenre("Technology");
                b.setSummary("A beginner's guide to AI concepts.");
                b.setTotalCopies(3);
                b.setAvailableCopies(3);
                bookRepository.save(b);
            }
        };
    }
}
