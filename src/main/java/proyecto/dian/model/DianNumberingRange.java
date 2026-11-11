package proyecto.dian.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import proyecto.entidades.Empresa;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "dian_numbering_range")
@Getter @Setter @NoArgsConstructor
public class DianNumberingRange {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "dian_configuration_id", nullable = false)
    private DianConfiguration configuration;
    @Enumerated(EnumType.STRING) @Column(name = "document_type", nullable = false, length = 30)
    private DianDocumentType documentType;
    @Column(nullable = false, length = 20) private String prefix;
    @Column(name = "resolution_number", nullable = false, length = 100) private String resolutionNumber;
    @Column(name = "range_from", nullable = false) private Long rangeFrom;
    @Column(name = "range_to", nullable = false) private Long rangeTo;
    @Column(name = "current_number", nullable = false) private Long currentNumber;
    @Column(name = "valid_from", nullable = false) private LocalDate validFrom;
    @Column(name = "valid_until", nullable = false) private LocalDate validUntil;
    @Column(name = "technical_key_encrypted", length = 1000) private String technicalKeyEncrypted;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private DianEnvironment environment;
    @Column(nullable = false) private Boolean active = true;
    @Version @Column(nullable = false) private Long version = 0L;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    @PrePersist void createTimestamps() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate void updateTimestamp() { updatedAt = LocalDateTime.now(); }
}
