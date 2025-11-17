package com.soma.server.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "yandex_user_details")
public class YandexUserDetails extends UserDetailsInterface {
    
    @Column(name = "yandex_user_id", unique = true, length = 100)
    private String yandexUserId;
    
    @Column(name = "display_name", length = 100)
    private String displayName;
    
    @Column(name = "login", length = 100)
    private String login;
    
    @Column(name = "first_name", length = 50)
    private String firstName;
    
    @Column(name = "last_name", length = 50)
    private String lastName;
}