package com.soma.server.controller;

import com.soma.server.entity.User;
import com.soma.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/soma/api/users")
@RequiredArgsConstructor
public class UsersController {

    private final UserRepository userRepository;

    @GetMapping("/exists")
    public ResponseEntity<Boolean> exists(@RequestParam(required = false) String email,
                                          @RequestParam(required = false) String username) {
        if (email != null) {
            return ResponseEntity.ok(userRepository.existsByEmail(email));
        }
        if (username != null) {
            return ResponseEntity.ok(userRepository.existsByUsername(username));
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/by-email/{email}")
    public ResponseEntity<User> byEmail(@PathVariable String email) {
        Optional<User> user = userRepository.findByEmail(email);
        return user.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/by-username/{username}")
    public ResponseEntity<User> byUsername(@PathVariable String username) {
        Optional<User> user = userRepository.findByUsername(username);
        return user.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/by-id/{id}")
    public ResponseEntity<User> byId(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
