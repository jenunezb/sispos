ALTER TABLE administrador
    ADD COLUMN IF NOT EXISTS es_super_admin BOOLEAN NOT NULL DEFAULT FALSE;

-- Hibernate genero originalmente este CHECK con los perfiles existentes en ese momento.
-- Se recrea de forma idempotente para permitir cuentas exclusivas de cocina sin alterar
-- los valores actuales de vendedor o produccion.
ALTER TABLE vendedor
    DROP CONSTRAINT IF EXISTS vendedor_tipo_perfil_check;

ALTER TABLE vendedor
    ADD CONSTRAINT vendedor_tipo_perfil_check
    CHECK (tipo_perfil IN ('VENDEDOR', 'PRODUCCION', 'COCINA'));

-- Directorio general: se ejecuta antes de la validacion de Hibernate.
-- Aditivo e idempotente para despliegues y reinicios sin perder clientes existentes.
ALTER TABLE cliente ADD COLUMN IF NOT EXISTS correo VARCHAR(254);
ALTER TABLE cliente ADD COLUMN IF NOT EXISTS direccion VARCHAR(255);

ALTER TABLE administrador
    ALTER COLUMN empresa_nit DROP NOT NULL;

ALTER TABLE administrador
    ADD COLUMN IF NOT EXISTS es_administrador_empresa BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE empresa
    ADD COLUMN IF NOT EXISTS impresion_cocina_habilitada BOOLEAN NOT NULL DEFAULT TRUE;

-- Inicializar solo los valores pendientes; nunca sobrescribir preferencias por sede.
ALTER TABLE sede
    ADD COLUMN IF NOT EXISTS impresion_cocina_habilitada BOOLEAN;

UPDATE sede s
SET impresion_cocina_habilitada = COALESCE(e.impresion_cocina_habilitada, TRUE)
FROM empresa e
WHERE s.empresa_id = e.nit AND s.impresion_cocina_habilitada IS NULL;

UPDATE sede SET impresion_cocina_habilitada = TRUE
WHERE impresion_cocina_habilitada IS NULL;

ALTER TABLE sede ALTER COLUMN impresion_cocina_habilitada SET DEFAULT TRUE;
ALTER TABLE sede ALTER COLUMN impresion_cocina_habilitada SET NOT NULL;

ALTER TABLE sede
    ADD COLUMN IF NOT EXISTS admin_pin_hash VARCHAR(120);

ALTER TABLE sede
    ADD COLUMN IF NOT EXISTS admin_pin_intentos_fallidos INTEGER NOT NULL DEFAULT 0;

ALTER TABLE sede
    ADD COLUMN IF NOT EXISTS admin_pin_bloqueado_hasta TIMESTAMP NULL;

CREATE TABLE IF NOT EXISTS administrador_sede (
    administrador_id INTEGER NOT NULL,
    sede_id BIGINT NOT NULL,
    PRIMARY KEY (administrador_id, sede_id),
    CONSTRAINT fk_administrador_sede_administrador FOREIGN KEY (administrador_id) REFERENCES administrador(codigo),
    CONSTRAINT fk_administrador_sede_sede FOREIGN KEY (sede_id) REFERENCES sede(id)
);

CREATE TABLE IF NOT EXISTS suscripcion_sede (
    id BIGSERIAL PRIMARY KEY,
    sede_id BIGINT NOT NULL UNIQUE,
    plan VARCHAR(20) NOT NULL DEFAULT 'BASICO',
    tipo_cobro VARCHAR(20) NOT NULL,
    precio_mensual DOUBLE PRECISION NOT NULL DEFAULT 0,
    precio_anual DOUBLE PRECISION NOT NULL DEFAULT 0,
    fecha_inicio_servicio DATE NULL,
    fecha_ultimo_pago DATE NULL,
    fecha_proximo_vencimiento DATE NULL,
    estado_servicio VARCHAR(20) NOT NULL DEFAULT 'VENCIDO',
    observacion VARCHAR(500) NULL,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_suscripcion_sede_sede FOREIGN KEY (sede_id) REFERENCES sede(id)
);

CREATE TABLE IF NOT EXISTS pago_suscripcion_sede (
    id BIGSERIAL PRIMARY KEY,
    suscripcion_id BIGINT NOT NULL,
    sede_id BIGINT NOT NULL,
    tipo_pago VARCHAR(20) NOT NULL,
    valor DOUBLE PRECISION NOT NULL,
    fecha_pago DATE NOT NULL,
    periodo_desde DATE NOT NULL,
    periodo_hasta DATE NOT NULL,
    medio_pago VARCHAR(100) NULL,
    observacion VARCHAR(500) NULL,
    registrado_por VARCHAR(150) NULL,
    CONSTRAINT fk_pago_suscripcion_sede_suscripcion FOREIGN KEY (suscripcion_id) REFERENCES suscripcion_sede(id),
    CONSTRAINT fk_pago_suscripcion_sede_sede FOREIGN KEY (sede_id) REFERENCES sede(id)
);

CREATE TABLE IF NOT EXISTS comanda_cocina (
    id BIGSERIAL PRIMARY KEY,
    fecha_creacion TIMESTAMP NOT NULL,
    fecha_actualizacion TIMESTAMP NOT NULL,
    nombre_mesa VARCHAR(120) NOT NULL,
    observaciones VARCHAR(1000) NULL,
    estado VARCHAR(30) NOT NULL,
    total_items INTEGER NOT NULL DEFAULT 0,
    sede_id BIGINT NOT NULL,
    vendedor_id INTEGER NULL,
    administrador_id INTEGER NULL,
    CONSTRAINT fk_comanda_cocina_sede FOREIGN KEY (sede_id) REFERENCES sede(id),
    CONSTRAINT fk_comanda_cocina_vendedor FOREIGN KEY (vendedor_id) REFERENCES vendedor(codigo),
    CONSTRAINT fk_comanda_cocina_administrador FOREIGN KEY (administrador_id) REFERENCES administrador(codigo)
);

CREATE TABLE IF NOT EXISTS comanda_cocina_detalle (
    id BIGSERIAL PRIMARY KEY,
    comanda_id BIGINT NOT NULL,
    producto_nombre VARCHAR(255) NOT NULL,
    cantidad INTEGER NOT NULL,
    CONSTRAINT fk_comanda_cocina_detalle_comanda FOREIGN KEY (comanda_id) REFERENCES comanda_cocina(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS mesa_estado (
    id BIGSERIAL PRIMARY KEY,
    sede_id BIGINT NOT NULL,
    mesa_referencia_id BIGINT NOT NULL,
    numero INTEGER NOT NULL DEFAULT 0,
    nombre VARCHAR(120) NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'LIBRE',
    fecha_actualizacion TIMESTAMP NOT NULL,
    CONSTRAINT fk_mesa_estado_sede FOREIGN KEY (sede_id) REFERENCES sede(id),
    CONSTRAINT uk_mesa_estado_sede_mesa UNIQUE (sede_id, mesa_referencia_id)
);

CREATE TABLE IF NOT EXISTS mesa_estado_item (
    id BIGSERIAL PRIMARY KEY,
    mesa_estado_id BIGINT NOT NULL,
    producto_id BIGINT NULL,
    producto_nombre VARCHAR(255) NULL,
    stock_actual INTEGER NULL,
    entradas INTEGER NULL,
    salidas INTEGER NULL,
    perdidas INTEGER NULL,
    stock_minimo INTEGER NULL,
    precio_venta DOUBLE PRECISION NULL,
    nombre_libre VARCHAR(255) NULL,
    precio_unitario DOUBLE PRECISION NOT NULL,
    cantidad INTEGER NOT NULL,
    total DOUBLE PRECISION NOT NULL,
    CONSTRAINT fk_mesa_estado_item_mesa_estado FOREIGN KEY (mesa_estado_id) REFERENCES mesa_estado(id) ON DELETE CASCADE
);

ALTER TABLE venta
    ADD COLUMN IF NOT EXISTS numero_consecutivo BIGINT;

ALTER TABLE venta
    ADD COLUMN IF NOT EXISTS monto_efectivo DOUBLE PRECISION;

ALTER TABLE venta
    ADD COLUMN IF NOT EXISTS monto_transferencia DOUBLE PRECISION;

ALTER TABLE venta
    DROP CONSTRAINT IF EXISTS venta_modo_pago_check;

ALTER TABLE venta
    ADD CONSTRAINT venta_modo_pago_check
        CHECK (modo_pago IN ('EFECTIVO', 'TRANSFERENCIA', 'MIXTO'));

UPDATE venta
SET monto_efectivo = total,
    monto_transferencia = COALESCE(monto_transferencia, 0)
WHERE modo_pago = 'EFECTIVO'
  AND monto_efectivo IS NULL;

UPDATE venta
SET monto_efectivo = COALESCE(monto_efectivo, 0),
    monto_transferencia = total
WHERE modo_pago = 'TRANSFERENCIA'
  AND monto_transferencia IS NULL;

WITH ventas_ordenadas AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY sede_id
               ORDER BY fecha ASC, id ASC
           ) AS consecutivo
    FROM venta
)
UPDATE venta v
SET numero_consecutivo = vo.consecutivo
FROM ventas_ordenadas vo
WHERE v.id = vo.id
  AND v.numero_consecutivo IS NULL;

ALTER TABLE venta
    ALTER COLUMN numero_consecutivo SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_venta_sede_numero_consecutivo
    ON venta (sede_id, numero_consecutivo);

ALTER TABLE empresa
    ADD COLUMN IF NOT EXISTS dv VARCHAR(5);

ALTER TABLE suscripcion_sede
    ADD COLUMN IF NOT EXISTS plan VARCHAR(20) NOT NULL DEFAULT 'BASICO';

CREATE TABLE IF NOT EXISTS gasto_diario (
    id BIGSERIAL PRIMARY KEY,
    sede_id BIGINT NOT NULL,
    administrador_id INTEGER NOT NULL,
    descripcion VARCHAR(255) NOT NULL,
    valor DOUBLE PRECISION NOT NULL,
    fecha TIMESTAMP NOT NULL,
    modo_pago VARCHAR(30) NOT NULL,
    CONSTRAINT fk_gasto_diario_sede FOREIGN KEY (sede_id) REFERENCES sede(id),
    CONSTRAINT fk_gasto_diario_administrador FOREIGN KEY (administrador_id) REFERENCES administrador(codigo)
);

-- Estado compartido del punto de venta. Las columnas son aditivas para conservar
-- todas las mesas y carritos que ya existan en instalaciones desplegadas.
ALTER TABLE mesa_estado ADD COLUMN IF NOT EXISTS tipo VARCHAR(20);
ALTER TABLE mesa_estado ADD COLUMN IF NOT EXISTS visible BOOLEAN;
ALTER TABLE mesa_estado ADD COLUMN IF NOT EXISTS orden_visual INTEGER;
ALTER TABLE mesa_estado ADD COLUMN IF NOT EXISTS domicilio_direccion VARCHAR(255);
ALTER TABLE mesa_estado ADD COLUMN IF NOT EXISTS domicilio_costo DOUBLE PRECISION;
ALTER TABLE mesa_estado ADD COLUMN IF NOT EXISTS domicilio_nombre_recibe VARCHAR(150);
ALTER TABLE mesa_estado ADD COLUMN IF NOT EXISTS domicilio_celular_recibe VARCHAR(30);
ALTER TABLE mesa_estado ADD COLUMN IF NOT EXISTS version BIGINT;

UPDATE mesa_estado
SET tipo = CASE
        WHEN mesa_referencia_id = 0 THEN 'MOSTRADOR'
        WHEN mesa_referencia_id = 1 THEN 'BARRA'
        WHEN mesa_referencia_id IN (9991, 9992, 9993, 9994, 9999) THEN 'DOMICILIO'
        ELSE 'MESA'
    END
WHERE tipo IS NULL;

UPDATE mesa_estado SET visible = TRUE WHERE visible IS NULL;
UPDATE mesa_estado SET orden_visual = mesa_referencia_id::INTEGER WHERE orden_visual IS NULL;
UPDATE mesa_estado SET version = 0 WHERE version IS NULL;

ALTER TABLE mesa_estado ALTER COLUMN tipo SET DEFAULT 'MESA';
ALTER TABLE mesa_estado ALTER COLUMN tipo SET NOT NULL;
ALTER TABLE mesa_estado ALTER COLUMN visible SET DEFAULT TRUE;
ALTER TABLE mesa_estado ALTER COLUMN visible SET NOT NULL;
ALTER TABLE mesa_estado ALTER COLUMN orden_visual SET DEFAULT 0;
ALTER TABLE mesa_estado ALTER COLUMN orden_visual SET NOT NULL;
ALTER TABLE mesa_estado ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE mesa_estado ALTER COLUMN version SET NOT NULL;

ALTER TABLE gasto_diario
    ALTER COLUMN administrador_id DROP NOT NULL;

ALTER TABLE gasto_diario
    ADD COLUMN IF NOT EXISTS vendedor_id INTEGER NULL;

CREATE TABLE IF NOT EXISTS caja_turno (
    id BIGSERIAL PRIMARY KEY,
    sede_id BIGINT NOT NULL,
    administrador_apertura_id INTEGER NOT NULL,
    administrador_cierre_id INTEGER NULL,
    fecha_apertura TIMESTAMP NOT NULL,
    fecha_cierre TIMESTAMP NULL,
    estado VARCHAR(20) NOT NULL,
    base_inicial DOUBLE PRECISION NOT NULL,
    ventas_efectivo DOUBLE PRECISION NULL,
    gastos_efectivo DOUBLE PRECISION NULL,
    efectivo_esperado DOUBLE PRECISION NULL,
    efectivo_contado DOUBLE PRECISION NULL,
    diferencia DOUBLE PRECISION NULL,
    observacion VARCHAR(500) NULL,
    observacion_cierre VARCHAR(500) NULL,
    CONSTRAINT fk_caja_turno_sede FOREIGN KEY (sede_id) REFERENCES sede(id),
    CONSTRAINT fk_caja_turno_admin_apertura FOREIGN KEY (administrador_apertura_id) REFERENCES administrador(codigo),
    CONSTRAINT fk_caja_turno_admin_cierre FOREIGN KEY (administrador_cierre_id) REFERENCES administrador(codigo)
);

ALTER TABLE caja_turno
    ALTER COLUMN administrador_apertura_id DROP NOT NULL;

ALTER TABLE caja_turno
    ADD COLUMN IF NOT EXISTS vendedor_apertura_id INTEGER NULL;

ALTER TABLE caja_turno
    ADD COLUMN IF NOT EXISTS vendedor_cierre_id INTEGER NULL;

CREATE INDEX IF NOT EXISTS idx_caja_turno_sede_estado
    ON caja_turno (sede_id, estado);

CREATE INDEX IF NOT EXISTS idx_caja_turno_fecha_apertura
    ON caja_turno (fecha_apertura);

ALTER TABLE IF EXISTS movimiento_produccion
    ADD COLUMN IF NOT EXISTS stock_anterior INTEGER NULL;

ALTER TABLE IF EXISTS movimiento_produccion
    ADD COLUMN IF NOT EXISTS stock_nuevo INTEGER NULL;

ALTER TABLE IF EXISTS movimiento_produccion
    DROP CONSTRAINT IF EXISTS movimiento_produccion_tipo_check;

ALTER TABLE IF EXISTS movimiento_produccion
    ADD CONSTRAINT movimiento_produccion_tipo_check
    CHECK (tipo IN ('PRODUCCION', 'DESPACHO', 'AJUSTE'));

CREATE INDEX IF NOT EXISTS idx_movimiento_produccion_sede_tipo_fecha
    ON movimiento_produccion (sede_id, tipo, fecha DESC);

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
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS tributo_codigo VARCHAR(10);
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS tributo_nombre VARCHAR(100);
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS municipio_nombre VARCHAR(100);
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS departamento_nombre VARCHAR(100);
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS pais_nombre VARCHAR(100);
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS departamento_codigo VARCHAR(10);
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS pais_codigo VARCHAR(3);
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS codigo_postal VARCHAR(20);
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS correo_facturacion VARCHAR(254);
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS telefono_facturacion VARCHAR(30);

ALTER TABLE sede ADD COLUMN IF NOT EXISTS direccion_fiscal VARCHAR(255);
ALTER TABLE sede ADD COLUMN IF NOT EXISTS municipio_codigo VARCHAR(10);
ALTER TABLE sede ADD COLUMN IF NOT EXISTS municipio_nombre VARCHAR(100);
ALTER TABLE sede ADD COLUMN IF NOT EXISTS departamento_nombre VARCHAR(100);
ALTER TABLE sede ADD COLUMN IF NOT EXISTS pais_nombre VARCHAR(100);
ALTER TABLE sede ADD COLUMN IF NOT EXISTS departamento_codigo VARCHAR(10);
ALTER TABLE sede ADD COLUMN IF NOT EXISTS pais_codigo VARCHAR(3);
ALTER TABLE sede ADD COLUMN IF NOT EXISTS codigo_postal VARCHAR(20);

ALTER TABLE cliente ADD COLUMN IF NOT EXISTS tipo_documento_fiscal VARCHAR(10);
ALTER TABLE cliente ADD COLUMN IF NOT EXISTS dv VARCHAR(5);
ALTER TABLE cliente ADD COLUMN IF NOT EXISTS tipo_persona_fiscal VARCHAR(10);
ALTER TABLE cliente ADD COLUMN IF NOT EXISTS responsabilidad_fiscal VARCHAR(100);
ALTER TABLE cliente ADD COLUMN IF NOT EXISTS regimen_fiscal VARCHAR(100);
ALTER TABLE cliente ADD COLUMN IF NOT EXISTS municipio_codigo VARCHAR(10);
ALTER TABLE cliente ADD COLUMN IF NOT EXISTS tributo_codigo VARCHAR(10);
ALTER TABLE cliente ADD COLUMN IF NOT EXISTS tributo_nombre VARCHAR(100);
ALTER TABLE cliente ADD COLUMN IF NOT EXISTS municipio_nombre VARCHAR(100);
ALTER TABLE cliente ADD COLUMN IF NOT EXISTS departamento_nombre VARCHAR(100);
ALTER TABLE cliente ADD COLUMN IF NOT EXISTS pais_nombre VARCHAR(100);
ALTER TABLE producto ADD COLUMN IF NOT EXISTS precio_incluye_impuestos BOOLEAN;
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
ALTER TABLE venta ADD COLUMN IF NOT EXISTS es_domicilio BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE venta ADD COLUMN IF NOT EXISTS direccion_domicilio VARCHAR(500);
ALTER TABLE venta ADD COLUMN IF NOT EXISTS costo_domicilio DOUBLE PRECISION NOT NULL DEFAULT 0;
ALTER TABLE venta ADD COLUMN IF NOT EXISTS nombre_recibe_domicilio VARCHAR(255);
ALTER TABLE venta ADD COLUMN IF NOT EXISTS celular_recibe_domicilio VARCHAR(50);

ALTER TABLE detalle_venta ADD COLUMN IF NOT EXISTS precio_unitario_fiscal NUMERIC(19,6);
ALTER TABLE detalle_venta ADD COLUMN IF NOT EXISTS subtotal_fiscal NUMERIC(19,6);
ALTER TABLE detalle_venta ADD COLUMN IF NOT EXISTS descuento_fiscal NUMERIC(19,6);
ALTER TABLE detalle_venta ADD COLUMN IF NOT EXISTS base_impuesto_fiscal NUMERIC(19,6);
ALTER TABLE detalle_venta ADD COLUMN IF NOT EXISTS tarifa_impuesto_fiscal NUMERIC(9,6);
ALTER TABLE detalle_venta ADD COLUMN IF NOT EXISTS valor_impuesto_fiscal NUMERIC(19,6);
