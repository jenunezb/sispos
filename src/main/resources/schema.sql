ALTER TABLE administrador
    ADD COLUMN IF NOT EXISTS es_super_admin BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE administrador
    ALTER COLUMN empresa_nit DROP NOT NULL;

CREATE TABLE IF NOT EXISTS suscripcion_sede (
    id BIGSERIAL PRIMARY KEY,
    sede_id BIGINT NOT NULL UNIQUE,
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
