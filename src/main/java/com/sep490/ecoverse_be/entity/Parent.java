package com.sep490.ecoverse_be.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "parents", indexes = {
        @Index(name = "idx_parents_user_id", columnList = "user_id"),
        @Index(name = "idx_parents_phone_number", columnList = "phone_number")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Parent extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String fullName;

    @Column(length = 20)
    private String phoneNumber;

    @Column(name = "is_first_login")
    private Boolean isFirstLogin = true;

    // Danh dau da gui email thong tin dang nhap lan dau cho phu huynh hay chua
    @Column(name = "credential_email_sent")
    private Boolean credentialEmailSent = false;
}
