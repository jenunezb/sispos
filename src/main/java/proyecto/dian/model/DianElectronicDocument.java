package proyecto.dian.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import proyecto.entidades.Empresa;
import proyecto.entidades.Venta;

import java.time.LocalDateTime;

@Entity
@Table(name = "dian_electronic_document")
@Getter @Setter @NoArgsConstructor
public class DianElectronicDocument {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "empresa_id", nullable = false) private Empresa empresa;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "venta_id") private Venta venta;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "related_document_id") private DianElectronicDocument relatedDocument;
    @Enumerated(EnumType.STRING) @Column(name = "document_type", nullable = false, length = 30) private DianDocumentType documentType;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private DianEnvironment environment;
    @Column(length = 20) private String prefix;
    private Long consecutive;
    @Column(name = "full_number", length = 100) private String fullNumber;
    @Column(name = "cufe_or_cude", length = 128) private String cufeOrCude;
    @Column(name = "software_security_code", length = 128) private String softwareSecurityCode;
    @Column(name = "test_set_id", length = 100) private String testSetId;
    @Column(name = "zip_key", length = 200) private String zipKey;
    @Column(name = "track_id", length = 200) private String trackId;
    @Column(name = "request_xml_storage_reference", length = 500) private String requestXmlStorageReference;
    @Column(name = "signed_xml_storage_reference", length = 500) private String signedXmlStorageReference;
    @Column(name = "response_xml_storage_reference", length = 500) private String responseXmlStorageReference;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private DianDocumentStatus status = DianDocumentStatus.DRAFT;
    @Column(name = "dian_status_code", length = 100) private String dianStatusCode;
    @Column(name = "dian_status_message", length = 2000) private String dianStatusMessage;
    @Column(name = "dian_validation_errors", columnDefinition = "TEXT") private String dianValidationErrors;
    @Column(nullable = false) private Integer attempts = 0;
    @Column(name = "sent_at") private LocalDateTime sentAt;
    @Column(name = "validated_at") private LocalDateTime validatedAt;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    @PrePersist void createTimestamps() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate void updateTimestamp() { updatedAt = LocalDateTime.now(); }
}
