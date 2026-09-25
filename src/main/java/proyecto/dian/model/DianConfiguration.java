package proyecto.dian.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import proyecto.entidades.Empresa;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "dian_configuration", uniqueConstraints =
        @UniqueConstraint(name = "uk_dian_configuration_empresa_ambiente", columnNames = {"empresa_id", "environment"}))
@Getter
@Setter
@NoArgsConstructor
public class DianConfiguration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DianEnvironment environment;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_mode", nullable = false, length = 30)
    private DianOperationMode operationMode = DianOperationMode.SOFTWARE_PROPIO;

    @Column(name = "software_id", length = 100)
    private String softwareId;

    @Column(name = "software_pin_encrypted", length = 1000)
    private String softwarePinEncrypted;

    @Column(name = "test_set_id", length = 100)
    private String testSetId;

    @Column(name = "technical_key_encrypted", length = 1000)
    private String technicalKeyEncrypted;

    @Column(name = "test_prefix", length = 20)
    private String testPrefix;

    @Column(name = "test_range_from")
    private Long testRangeFrom;

    @Column(name = "test_range_to")
    private Long testRangeTo;

    @Column(name = "test_resolution_number", length = 100)
    private String testResolutionNumber;

    @Column(name = "test_valid_from")
    private LocalDate testValidFrom;

    @Column(name = "test_valid_until")
    private LocalDate testValidUntil;

    @Column(name = "certificate_reference", length = 500)
    private String certificateReference;

    @Column(name = "certificate_password_encrypted", length = 1000)
    private String certificatePasswordEncrypted;

    @Column(name = "certificate_expiration")
    private LocalDateTime certificateExpiration;

    @Column(name = "dian_service_url", length = 500)
    private String dianServiceUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DianConfigurationStatus status = DianConfigurationStatus.NOT_CONFIGURED;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void createTimestamps() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void updateTimestamp() {
        updatedAt = LocalDateTime.now();
    }
}
