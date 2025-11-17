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

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 120)
    private String email;

    @JsonIgnore
    @Column(nullable = false, length = 100)
    private String password;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<UserDetailsInterface> allUserDetails = new ArrayList<>();

    private User(Builder builder) {
        this.username = builder.username;
        this.password = builder.password;
        this.email = builder.email;
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String username;
        private String password;
        private String email;

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

        public User build() {
            validate();
            return new User(this);
        }

        private void validate() {
            if (username == null || username.trim().isEmpty()) {
                throw new IllegalArgumentException("Username is required");
            }
            if (password == null || password.isEmpty()) {
                throw new IllegalArgumentException("Password is required");
            }
            if (email == null || email.trim().isEmpty()) {
                throw new IllegalArgumentException("Email is required");
            }
        }
    }
}