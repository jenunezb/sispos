ALTER TABLE administrador
    ADD COLUMN IF NOT EXISTS es_super_admin BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE administrador
    ALTER COLUMN empresa_nit DROP NOT NULL;

ALTER TABLE administrador
    ADD COLUMN IF NOT EXISTS es_administrador_empresa BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE empresa
    ADD COLUMN IF NOT EXISTS impresion_cocina_habilitada BOOLEAN NOT NULL DEFAULT TRUE;

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
