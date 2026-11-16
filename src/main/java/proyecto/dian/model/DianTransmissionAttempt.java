package proyecto.dian.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import proyecto.entidades.Empresa;

import java.time.LocalDateTime;

@Entity
@Table(name = "dian_transmission_attempt")
@Getter @Setter @NoArgsConstructor
public class DianTransmissionAttempt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "empresa_id", nullable = false) private Empresa empresa;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "electronic_document_id") private DianElectronicDocument electronicDocument;
    @Column(nullable = false, length = 100) private String operation;
    @Column(name = "correlation_id", length = 150) private String correlationId;
    @Column(nullable = false, length = 50) private String status;
    @Column(name = "sanitized_response", columnDefinition = "TEXT") private String sanitizedResponse;
    @Enumerated(EnumType.STRING) @Column(name = "error_category", length = 40) private DianErrorCategory errorCategory;
    @Column(name = "error_message_sanitized", length = 2000) private String errorMessageSanitized;
    @Column(name = "attempted_at", nullable = false) private LocalDateTime attemptedAt;
    @PrePersist void setAttemptedAt() { if (attemptedAt == null) attemptedAt = LocalDateTime.now(); }
}
