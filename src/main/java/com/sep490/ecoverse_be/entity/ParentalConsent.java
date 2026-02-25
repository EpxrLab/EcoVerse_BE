package com.sep490.ecoverse_be.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "parental_consents", indexes = {
        @Index(name = "idx_consent_student_id", columnList = "student_id"),
        @Index(name = "idx_consent_parent_id", columnList = "parent_id"),
        @Index(name = "idx_consent_type", columnList = "consent_type"),
        @Index(name = "idx_consent_given", columnList = "consent_given"),
        @Index(name = "idx_consent_given_at", columnList = "given_at")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ParentalConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    private Parent parent;

    @Column(name = "consent_type", length = 100, nullable = false)
    private String consentType;

    @Column(name = "consent_given", nullable = false)
    private boolean consentGiven;

    @Column(name = "consent_text", columnDefinition = "text")
    private String consentText;

    @Column(name = "consent_version", length = 20)
    private String consentVersion;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;

    @CreationTimestamp
    @Column(name = "given_at", updatable = false)
    private LocalDateTime givenAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
}
