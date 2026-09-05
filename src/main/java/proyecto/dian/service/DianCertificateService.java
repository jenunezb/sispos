package proyecto.dian.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import proyecto.dian.dto.DianCertificateResponse;
import proyecto.dian.model.DianConfiguration;
import proyecto.dian.model.DianEnvironment;
import proyecto.dian.model.DianTransmissionAttempt;
import proyecto.dian.repository.DianConfigurationRepository;
import proyecto.dian.repository.DianTransmissionAttemptRepository;
import proyecto.entidades.Empresa;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Enumeration;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DianCertificateService {
    private static final long MAX_CERTIFICATE_BYTES = 5L * 1024 * 1024;
    private final DianConfigurationRepository configurations;
    private final DianTenantContextService tenantContext;
    private final DianCryptoService crypto;
    private final DianTransmissionAttemptRepository audit;

    @Value("${dian.storage.path:./data/dian-private}")
    private String storagePath;

    public DianCertificateResponse upload(String authorization, DianEnvironment environment,
                                          MultipartFile file, String password) {
        Empresa company = tenantContext.requireCompanyAdministrator(authorization);
        validateUpload(file, password);
        DianConfiguration configuration = configurations.findByEmpresaNitAndEnvironment(company.getNit(), environment)
                .orElseThrow(() -> new IllegalArgumentException("Primero debe guardar la configuracion DIAN del ambiente"));
        byte[] source;
        try {
            source = file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("No fue posible leer el certificado");
        }
        CertificateInfo info = inspect(source, password);
        String context = context(company.getNit(), environment);
        byte[] protectedCertificate = crypto.encryptBytes(source, context + ":certificate");
        String newReference = company.getNit() + "-" + environment.name().toLowerCase(Locale.ROOT)
                + "-" + UUID.randomUUID() + ".p12.enc";
        writePrivate(newReference, protectedCertificate);

        String oldReference = configuration.getCertificateReference();
        configuration.setCertificateReference(newReference);
        configuration.setCertificatePasswordEncrypted(crypto.encrypt(password, context + ":certificate-password"));
        configuration.setCertificateExpiration(info.expiration());
        configurations.save(configuration);
        if (oldReference != null && !oldReference.equals(newReference)) deletePrivate(oldReference);
        recordAudit(company, "CERTIFICATE_UPLOAD");
        return new DianCertificateResponse(environment, true, info.expiration(), info.subject());
    }

    public DianCertificateResponse delete(String authorization, DianEnvironment environment) {
        Empresa company = tenantContext.requireCompanyAdministrator(authorization);
        DianConfiguration configuration = configurations.findByEmpresaNitAndEnvironment(company.getNit(), environment)
                .orElseThrow(() -> new IllegalArgumentException("Configuracion DIAN no encontrada"));
        String reference = configuration.getCertificateReference();
        configuration.setCertificateReference(null);
        configuration.setCertificatePasswordEncrypted(null);
        configuration.setCertificateExpiration(null);
        configurations.save(configuration);
        if (reference != null) deletePrivate(reference);
        recordAudit(company, "CERTIFICATE_DELETE");
        return new DianCertificateResponse(environment, false, null, null);
    }

    SigningMaterial load(DianConfiguration configuration) {
        if (configuration == null || configuration.getCertificateReference() == null
                || configuration.getCertificatePasswordEncrypted() == null) {
            throw new IllegalStateException("Falta cargar el certificado digital P12/PFX");
        }
        Long companyNit = configuration.getEmpresa().getNit();
        String secretContext = context(companyNit, configuration.getEnvironment());
        try {
            byte[] encrypted = Files.readAllBytes(resolve(configuration.getCertificateReference()));
            return new SigningMaterial(
                    crypto.decryptBytes(encrypted, secretContext + ":certificate"),
                    crypto.decrypt(configuration.getCertificatePasswordEncrypted(),
                            secretContext + ":certificate-password"));
        } catch (java.nio.file.NoSuchFileException exception) {
            throw new IllegalStateException("No se encontró el certificado digital almacenado");
        } catch (IOException exception) {
            throw new IllegalStateException("No fue posible leer el certificado digital", exception);
        }
    }

    private void validateUpload(MultipartFile file, String password) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Debe cargar un certificado P12 o PFX");
        if (file.getSize() > MAX_CERTIFICATE_BYTES) throw new IllegalArgumentException("El certificado supera el limite de 5 MB");
        if (password == null || password.isBlank()) throw new IllegalArgumentException("La contraseña del certificado es obligatoria");
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".p12") && !name.endsWith(".pfx")) {
            throw new IllegalArgumentException("El archivo debe tener extension .p12 o .pfx");
        }
    }

    private CertificateInfo inspect(byte[] bytes, String password) {
        try {
            KeyStore store = KeyStore.getInstance("PKCS12");
            store.load(new ByteArrayInputStream(bytes), password.toCharArray());
            Enumeration<String> aliases = store.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (store.isKeyEntry(alias) && store.getCertificate(alias) instanceof X509Certificate certificate) {
                    certificate.checkValidity();
                    LocalDateTime expiration = certificate.getNotAfter().toInstant()
                            .atZone(ZoneId.of("America/Bogota")).toLocalDateTime();
                    return new CertificateInfo(expiration, certificate.getSubjectX500Principal().getName());
                }
            }
            throw new IllegalArgumentException("El archivo no contiene una llave privada con certificado X.509");
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("El certificado o su contraseña no son validos");
        }
    }

    private void writePrivate(String reference, byte[] bytes) {
        try {
            Path root = root();
            Files.createDirectories(root);
            Files.write(resolve(reference), bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new IllegalStateException("No fue posible almacenar el certificado de forma privada");
        }
    }

    private void deletePrivate(String reference) {
        try {
            Files.deleteIfExists(resolve(reference));
        } catch (IOException e) {
            throw new IllegalStateException("No fue posible eliminar el certificado privado");
        }
    }

    private Path resolve(String reference) {
        if (reference == null || reference.contains("/") || reference.contains("\\")) {
            throw new IllegalArgumentException("Referencia de certificado invalida");
        }
        Path root = root();
        Path target = root.resolve(reference).normalize();
        if (!target.startsWith(root)) throw new IllegalArgumentException("Referencia de certificado invalida");
        return target;
    }

    private Path root() {
        return Path.of(storagePath).toAbsolutePath().normalize();
    }

    private String context(Long companyNit, DianEnvironment environment) {
        return "dian:" + companyNit + ":" + environment.name();
    }

    private void recordAudit(Empresa company, String operation) {
        DianTransmissionAttempt attempt = new DianTransmissionAttempt();
        attempt.setEmpresa(company);
        attempt.setOperation(operation);
        attempt.setCorrelationId(UUID.randomUUID().toString());
        attempt.setStatus("SUCCESS");
        audit.save(attempt);
    }

    private record CertificateInfo(LocalDateTime expiration, String subject) {}
    record SigningMaterial(byte[] pkcs12, String password) {}
}
