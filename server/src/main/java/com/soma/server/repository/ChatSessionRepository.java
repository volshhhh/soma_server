package com.soma.server.repository;

import com.soma.server.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {
    List<ChatSession> findByUserIdOrderByLastActivityAtDesc(Long userId);
    Optional<ChatSession> findByIdAndUserId(String id, Long userId);
}

