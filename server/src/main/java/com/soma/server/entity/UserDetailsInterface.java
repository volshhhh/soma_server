package com.soma.server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "user_details")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class UserDetailsInterface {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "access_token", length = 500)
    protected String accessToken;
    
    @Column(name = "refresh_token", length = 500)
    protected String refreshToken;
    
    @Column(name = "ref_id", length = 100)
    protected String refId;
    
    @Column(name = "avatar_url", length = 500)
    protected String avatarUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    protected User user;
}