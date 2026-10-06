-- DIAN foundation. Additive and idempotent so existing Railway data is preserved.
CREATE TABLE IF NOT EXISTS dian_configuration (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresa(nit),
    environment VARCHAR(20) NOT NULL,
    operation_mode VARCHAR(30) NOT NULL DEFAULT 'SOFTWARE_PROPIO',
    software_id VARCHAR(100),
    software_pin_encrypted VARCHAR(1000),
    test_set_id VARCHAR(100),
    technical_key_encrypted VARCHAR(1000),
    test_prefix VARCHAR(20),
    test_range_from BIGINT,
    test_range_to BIGINT,
    test_resolution_number VARCHAR(100),
    test_valid_from DATE,
    test_valid_until DATE,
    certificate_reference VARCHAR(500),
    certificate_password_encrypted VARCHAR(1000),
    certificate_expiration TIMESTAMP,
    dian_service_url VARCHAR(500),
    status VARCHAR(30) NOT NULL DEFAULT 'NOT_CONFIGURED',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_dian_configuration_empresa_ambiente UNIQUE (empresa_id, environment),
    CONSTRAINT ck_dian_configuration_environment CHECK (environment IN ('HABILITACION', 'PRODUCCION')),
    CONSTRAINT ck_dian_configuration_operation CHECK (operation_mode IN ('SOFTWARE_PROPIO', 'PROVEEDOR_TECNOLOGICO')),
    CONSTRAINT ck_dian_configuration_status CHECK (status IN ('NOT_CONFIGURED', 'CONFIGURED', 'TESTING', 'ENABLED', 'ERROR')),
    CONSTRAINT ck_dian_configuration_test_range CHECK (
        test_range_from IS NULL OR test_range_to IS NULL OR test_range_from <= test_range_to
    ),
    CONSTRAINT ck_dian_configuration_test_dates CHECK (
        test_valid_from IS NULL OR test_valid_until IS NULL OR test_valid_from <= test_valid_until
    )
);

CREATE TABLE IF NOT EXISTS dian_numbering_range (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresa(nit),
    dian_configuration_id BIGINT NOT NULL REFERENCES dian_configuration(id),
    document_type VARCHAR(30) NOT NULL,
    prefix VARCHAR(20) NOT NULL,
    resolution_number VARCHAR(100) NOT NULL,
    range_from BIGINT NOT NULL,
    range_to BIGINT NOT NULL,
    current_number BIGINT NOT NULL,
    valid_from DATE NOT NULL,
    valid_until DATE NOT NULL,
    technical_key_encrypted VARCHAR(1000),
    environment VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_dian_range_resolution UNIQUE (empresa_id, environment, document_type, prefix, resolution_number),
    CONSTRAINT ck_dian_range_values CHECK (range_from <= current_number AND current_number <= range_to),
    CONSTRAINT ck_dian_range_dates CHECK (valid_from <= valid_until),
    CONSTRAINT ck_dian_range_document_type CHECK (document_type IN ('INVOICE', 'CREDIT_NOTE', 'DEBIT_NOTE')),
    CONSTRAINT ck_dian_range_environment CHECK (environment IN ('HABILITACION', 'PRODUCCION'))
);

CREATE TABLE IF NOT EXISTS dian_electronic_document (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresa(nit),
    venta_id BIGINT REFERENCES venta(id),
    related_document_id BIGINT REFERENCES dian_electronic_document(id),
    document_type VARCHAR(30) NOT NULL,
    environment VARCHAR(20) NOT NULL,
    prefix VARCHAR(20),
    consecutive BIGINT,
    full_number VARCHAR(100),
    cufe_or_cude VARCHAR(128),
    software_security_code VARCHAR(128),
    test_set_id VARCHAR(100),
    zip_key VARCHAR(200),
    track_id VARCHAR(200),
    request_xml_storage_reference VARCHAR(500),
    signed_xml_storage_reference VARCHAR(500),
    response_xml_storage_reference VARCHAR(500),
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    dian_status_code VARCHAR(100),
    dian_status_message VARCHAR(2000),
    dian_validation_errors TEXT,
    attempts INTEGER NOT NULL DEFAULT 0,
    sent_at TIMESTAMP,
    validated_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT ck_dian_document_type CHECK (document_type IN ('INVOICE', 'CREDIT_NOTE', 'DEBIT_NOTE')),
    CONSTRAINT ck_dian_document_environment CHECK (environment IN ('HABILITACION', 'PRODUCCION')),
    CONSTRAINT ck_dian_document_status CHECK (status IN ('DRAFT', 'GENERATED', 'SIGNED', 'SENT', 'PROCESSING', 'ACCEPTED', 'REJECTED', 'ERROR'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_dian_document_number
    ON dian_electronic_document (empresa_id, environment, document_type, prefix, consecutive)
    WHERE consecutive IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_dian_document_cufe
    ON dian_electronic_document (cufe_or_cude)
    WHERE cufe_or_cude IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_dian_document_sale_idempotency
    ON dian_electronic_document (empresa_id, environment, venta_id, document_type)
    WHERE venta_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS dian_transmission_attempt (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresa(nit),
    electronic_document_id BIGINT REFERENCES dian_electronic_document(id),
    operation VARCHAR(100) NOT NULL,
    correlation_id VARCHAR(150),
    status VARCHAR(50) NOT NULL,
    sanitized_response TEXT,
    error_category VARCHAR(40),
    error_message_sanitized VARCHAR(2000),
    attempted_at TIMESTAMP NOT NULL,
    CONSTRAINT ck_dian_attempt_error_category CHECK (
        error_category IS NULL OR error_category IN (
            'VALIDATION_ERROR', 'CONFIGURATION_ERROR', 'CERTIFICATE_ERROR',
            'DIAN_REJECTION', 'DIAN_TEMPORARY_ERROR', 'NETWORK_ERROR', 'INTERNAL_ERROR'
        )
    )
);

CREATE INDEX IF NOT EXISTS idx_dian_configuration_company
    ON dian_configuration (empresa_id, environment);
CREATE INDEX IF NOT EXISTS idx_dian_range_company_active
    ON dian_numbering_range (empresa_id, environment, active);
CREATE INDEX IF NOT EXISTS idx_dian_document_company_status
    ON dian_electronic_document (empresa_id, environment, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_dian_attempt_document
    ON dian_transmission_attempt (empresa_id, electronic_document_id, attempted_at DESC);

-- Fiscal master data is nullable to preserve every existing company, sale and product.
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS tipo_documento_fiscal VARCHAR(10);
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS tipo_persona_fiscal VARCHAR(10);
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS razon_social VARCHAR(255);
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS nombre_comercial VARCHAR(255);
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS responsabilidad_fiscal VARCHAR(100);
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS regimen_fiscal VARCHAR(100);
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS direccion_fiscal VARCHAR(255);
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS municipio_codigo VARCHAR(10);
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS departamento_codigo VARCHAR(10);
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS pais_codigo VARCHAR(3);
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS codigo_postal VARCHAR(20);
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS correo_facturacion VARCHAR(254);
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS telefono_facturacion VARCHAR(30);

ALTER TABLE sede ADD COLUMN IF NOT EXISTS direccion_fiscal VARCHAR(255);
ALTER TABLE sede ADD COLUMN IF NOT EXISTS municipio_codigo VARCHAR(10);
ALTER TABLE sede ADD COLUMN IF NOT EXISTS departamento_codigo VARCHAR(10);
ALTER TABLE sede ADD COLUMN IF NOT EXISTS pais_codigo VARCHAR(3);
ALTER TABLE sede ADD COLUMN IF NOT EXISTS codigo_postal VARCHAR(20);

ALTER TABLE cliente ADD COLUMN IF NOT EXISTS tipo_documento_fiscal VARCHAR(10);
ALTER TABLE cliente ADD COLUMN IF NOT EXISTS dv VARCHAR(5);
ALTER TABLE cliente ADD COLUMN IF NOT EXISTS tipo_persona_fiscal VARCHAR(10);
ALTER TABLE cliente ADD COLUMN IF NOT EXISTS responsabilidad_fiscal VARCHAR(100);
ALTER TABLE cliente ADD COLUMN IF NOT EXISTS regimen_fiscal VARCHAR(100);
ALTER TABLE cliente ADD COLUMN IF NOT EXISTS municipio_codigo VARCHAR(10);
ALTER TABLE cliente ADD COLUMN IF NOT EXISTS departamento_codigo VARCHAR(10);
ALTER TABLE cliente ADD COLUMN IF NOT EXISTS pais_codigo VARCHAR(3);
ALTER TABLE cliente ADD COLUMN IF NOT EXISTS codigo_postal VARCHAR(20);

ALTER TABLE producto ADD COLUMN IF NOT EXISTS codigo_estandar_fiscal VARCHAR(50);
ALTER TABLE producto ADD COLUMN IF NOT EXISTS unidad_medida_dian VARCHAR(10);
ALTER TABLE producto ADD COLUMN IF NOT EXISTS tributo_codigo VARCHAR(10);
ALTER TABLE producto ADD COLUMN IF NOT EXISTS tarifa_iva NUMERIC(9,6);
ALTER TABLE producto ADD COLUMN IF NOT EXISTS tarifa_inc NUMERIC(9,6);
ALTER TABLE producto ADD COLUMN IF NOT EXISTS tarifa_ica NUMERIC(9,6);

ALTER TABLE venta ADD COLUMN IF NOT EXISTS moneda_codigo VARCHAR(3);
ALTER TABLE venta ADD COLUMN IF NOT EXISTS forma_pago_dian VARCHAR(10);
ALTER TABLE venta ADD COLUMN IF NOT EXISTS medio_pago_dian VARCHAR(10);
ALTER TABLE venta ADD COLUMN IF NOT EXISTS fecha_vencimiento_pago TIMESTAMP;
ALTER TABLE venta ADD COLUMN IF NOT EXISTS subtotal_fiscal NUMERIC(19,6);
ALTER TABLE venta ADD COLUMN IF NOT EXISTS impuestos_fiscal NUMERIC(19,6);
ALTER TABLE venta ADD COLUMN IF NOT EXISTS descuentos_fiscal NUMERIC(19,6);
ALTER TABLE venta ADD COLUMN IF NOT EXISTS total_fiscal NUMERIC(19,6);

ALTER TABLE detalle_venta ADD COLUMN IF NOT EXISTS precio_unitario_fiscal NUMERIC(19,6);
ALTER TABLE detalle_venta ADD COLUMN IF NOT EXISTS subtotal_fiscal NUMERIC(19,6);
ALTER TABLE detalle_venta ADD COLUMN IF NOT EXISTS descuento_fiscal NUMERIC(19,6);
ALTER TABLE detalle_venta ADD COLUMN IF NOT EXISTS base_impuesto_fiscal NUMERIC(19,6);
ALTER TABLE detalle_venta ADD COLUMN IF NOT EXISTS tarifa_impuesto_fiscal NUMERIC(9,6);
ALTER TABLE detalle_venta ADD COLUMN IF NOT EXISTS valor_impuesto_fiscal NUMERIC(19,6);
