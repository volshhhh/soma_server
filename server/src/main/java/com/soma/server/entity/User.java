package com.soma.server.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_username", columnNames = "username"),
        @UniqueConstraint(name = "uk_user_email", columnNames = "email")
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(nullable = true, length = 120)
    private String email;

    @JsonIgnore
    @Column(nullable = true, length = 100)
    private String password;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role = Role.USER;
    
    @Column(name = "email_verified")
    private boolean emailVerified = false;
    
    @Column(name = "verification_token", length = 64)
    private String verificationToken;
    
    @Column(name = "verification_token_expiry")
    private java.time.LocalDateTime verificationTokenExpiry;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<UserDetailsInterface> allUserDetails = new ArrayList<>();

    private User(Builder builder) {
        this.username = builder.username;
        this.password = builder.password;
        this.email = builder.email;
        if (builder.role != null) {
            this.role = builder.role;
        }
    }

    public void addSpotifyDetails(SpotifyUserDetails spotifyDetails) {
        spotifyDetails.setUser(this);
        this.allUserDetails.add(spotifyDetails);
    }

    public void addYandexDetails(YandexUserDetails yandexDetails) {
        yandexDetails.setUser(this);
        this.allUserDetails.add(yandexDetails);
    }

    public void removeSpotifyDetails(SpotifyUserDetails spotifyDetails) {
        this.allUserDetails.remove(spotifyDetails);
        spotifyDetails.setUser(null);
    }

    public void removeYandexDetails(YandexUserDetails yandexDetails) {
        this.allUserDetails.remove(yandexDetails);
        yandexDetails.setUser(null);
    }
    
    public enum Role {
        USER,
        ADMIN,
        PREMIUM
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String username;
        private String password;
        private String email;
        private Role role;

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }
        
        public Builder role(Role role) {
            this.role = role;
            return this;
        }

        public User build() {
            validate();
            return new User(this);
        }

        private void validate() {
            if (username == null || username.trim().isEmpty()) {
                throw new IllegalArgumentException("Username is required");
            }
            // Password and email are optional for OAuth-only users
        }
    }
}
