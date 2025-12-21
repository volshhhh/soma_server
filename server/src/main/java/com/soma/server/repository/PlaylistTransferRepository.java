package com.soma.server.repository;

import com.soma.server.entity.PlaylistTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlaylistTransferRepository extends JpaRepository<PlaylistTransfer, Long> {
    List<PlaylistTransfer> findByUserIdOrderByCreatedAtDesc(Long userId);
}

