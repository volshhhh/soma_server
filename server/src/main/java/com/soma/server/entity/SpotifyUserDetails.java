package com.soma.server.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "spotify_user_details")
public class SpotifyUserDetails extends UserDetailsInterface {
    
    @Column(name = "spotify_user_id", unique = true, length = 100)
    private String spotifyUserId;
    
    @Column(name = "display_name", length = 100)
    private String displayName;
    
    @Column(name = "email", length = 120)
    private String email;
    
    @Column(name = "product_type", length = 50)
    private String productType; // premium, free etc.
    
    @Column(name = "country", length = 10)
    private String country;
}