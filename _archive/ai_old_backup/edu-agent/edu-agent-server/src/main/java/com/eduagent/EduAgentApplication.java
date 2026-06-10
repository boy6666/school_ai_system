package com.eduagent;

import com.eduagent.mapper.UserMapper;
import com.eduagent.entity.User;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class EduAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(EduAgentApplication.class, args);
    }

    @Bean
    CommandLineRunner initAdmin(UserMapper userMapper) {
        return args -> {
            User admin = userMapper.selectByUsername("admin");
            if (admin != null) {
                String newHash = new BCryptPasswordEncoder().encode("123456");
                admin.setPassword(newHash);
                userMapper.updateById(admin);
                System.out.println("=== Admin password reset to: 123456 ===");
            }
        };
    }
}
