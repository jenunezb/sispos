package proyecto.servicios.implementacion;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import proyecto.dto.SuperAdminConfigurarSuscripcionDTO;
import proyecto.dto.SuperAdminPagoSuscripcionDTO;
import proyecto.dto.SuperAdminRegistrarPagoSuscripcionDTO;
import proyecto.dto.SuperAdminSuscripcionSedeDTO;
import proyecto.entidades.EstadoSuscripcionSede;
import proyecto.entidades.PagoSuscripcionSede;
import proyecto.entidades.Sede;
import proyecto.entidades.SuscripcionSede;
import proyecto.entidades.TipoCobroSuscripcion;
import proyecto.repositorios.PagoSuscripcionSedeRepository;
import proyecto.repositorios.SedeRepository;
import proyecto.repositorios.SuscripcionSedeRepository;
import proyecto.servicios.interfaces.SuperAdminSuscripcionServicio;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SuperAdminSuscripcionServicioImpl implements SuperAdminSuscripcionServicio {

    private static final long DIAS_POR_VENCER = 5;

    private final SuscripcionSedeRepository suscripcionSedeRepository;
    private final PagoSuscripcionSedeRepository pagoSuscripcionSedeRepository;
    private final SedeRepository sedeRepository;

    @Override
    @Transactional
    public SuperAdminSuscripcionSedeDTO configurarSuscripcion(SuperAdminConfigurarSuscripcionDTO dto) {
        if (dto.sedeId() == null) {
            throw new RuntimeException("La sede es obligatoria");
        }

        Sede sede = sedeRepository.findById(dto.sedeId())
                .orElseThrow(() -> new RuntimeException("Sede no encontrada"));

        SuscripcionSede suscripcion = suscripcionSedeRepository.findBySedeId(dto.sedeId())
                .orElseGet(SuscripcionSede::new);

        if (suscripcion.getId() == null) {
            suscripcion.setSede(sede);
        }

        TipoCobroSuscripcion tipoCobro = parseTipoCobro(dto.tipoCobro(), "tipo de cobro");
        suscripcion.setTipoCobro(tipoCobro);
        suscripcion.setPrecioMensual(normalizarValor(dto.precioMensual(), "precio mensual"));
        suscripcion.setPrecioAnual(normalizarValor(dto.precioAnual(), "precio anual"));
        suscripcion.setFechaInicioServicio(dto.fechaInicioServicio());
        suscripcion.setObservacion(dto.observacion());
        suscripcion.setActiva(dto.activa() == null ? Boolean.TRUE : dto.activa());

        if (!Boolean.TRUE.equals(suscripcion.getActiva())) {
            suscripcion.setEstadoServicio(EstadoSuscripcionSede.SUSPENDIDO);
        } else if (suscripcion.getFechaProximoVencimiento() == null) {
            suscripcion.setEstadoServicio(EstadoSuscripcionSede.VENCIDO);
        } else {
            actualizarEstado(suscripcion);
        }

        suscripcion = suscripcionSedeRepository.save(suscripcion);
        return mapSuscripcion(suscripcion, true);
    }

    @Override
    @Transactional
    public SuperAdminPagoSuscripcionDTO registrarPago(SuperAdminRegistrarPagoSuscripcionDTO dto, String correoRegistrador) {
        if (dto.sedeId() == null) {
            throw new RuntimeException("La sede es obligatoria");
        }

        SuscripcionSede suscripcion = suscripcionSedeRepository.findBySedeId(dto.sedeId())
                .orElseThrow(() -> new RuntimeException("La sede no tiene suscripcion configurada"));

        if (!Boolean.TRUE.equals(suscripcion.getActiva())) {
            throw new RuntimeException("La suscripcion de la sede esta suspendida");
        }

        TipoCobroSuscripcion tipoPago = parseTipoCobro(dto.tipoPago(), "tipo de pago");
        LocalDate fechaPago = dto.fechaPago() != null ? dto.fechaPago() : LocalDate.now();

        LocalDate base = suscripcion.getFechaProximoVencimiento();
        if (base == null || base.isBefore(fechaPago)) {
            base = fechaPago;
        }

        LocalDate periodoDesde = base;
        LocalDate periodoHasta = tipoPago == TipoCobroSuscripcion.ANUAL
                ? base.plusYears(1).minusDays(1)
                : base.plusMonths(1).minusDays(1);

        Double valor = dto.valor();
        if (valor == null || valor <= 0) {
            valor = tipoPago == TipoCobroSuscripcion.ANUAL
                    ? suscripcion.getPrecioAnual()
                    : suscripcion.getPrecioMensual();
        }

        if (valor == null || valor <= 0) {
            throw new RuntimeException("Debe configurar el valor del pago o el precio de la suscripcion");
        }

        PagoSuscripcionSede pago = new PagoSuscripcionSede();
        pago.setSuscripcion(suscripcion);
        pago.setSede(suscripcion.getSede());
        pago.setTipoPago(tipoPago);
        pago.setValor(valor);
        pago.setFechaPago(fechaPago);
        pago.setPeriodoDesde(periodoDesde);
        pago.setPeriodoHasta(periodoHasta);
        pago.setMedioPago(dto.medioPago());
        pago.setObservacion(dto.observacion());
        pago.setRegistradoPor(correoRegistrador);

        pago = pagoSuscripcionSedeRepository.save(pago);

        suscripcion.setFechaUltimoPago(fechaPago);
        suscripcion.setFechaProximoVencimiento(periodoHasta.plusDays(1));
        suscripcion.setTipoCobro(tipoPago);
        actualizarEstado(suscripcion);
        suscripcionSedeRepository.save(suscripcion);

        return mapPago(pago);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SuperAdminSuscripcionSedeDTO> listarSuscripciones() {
        List<Sede> sedes = sedeRepository.findAll();
        List<SuscripcionSede> suscripciones = suscripcionSedeRepository.findAll();

        return sedes.stream()
                .map(sede -> {
                    Optional<SuscripcionSede> encontrada = suscripciones.stream()
                            .filter(s -> s.getSede() != null && sede.getId().equals(s.getSede().getId()))
                            .findFirst();
                    return encontrada
                            .map(suscripcion -> mapSuscripcion(suscripcion, false))
                            .orElseGet(() -> mapSedeSinConfigurar(sede));
                })
                .sorted(Comparator.comparing(SuperAdminSuscripcionSedeDTO::empresaNombre, Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(SuperAdminSuscripcionSedeDTO::sedeUbicacion, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SuperAdminSuscripcionSedeDTO obtenerSuscripcionPorSede(Long sedeId) {
        Sede sede = sedeRepository.findById(sedeId)
                .orElseThrow(() -> new RuntimeException("Sede no encontrada"));

        return suscripcionSedeRepository.findBySedeId(sedeId)
                .map(suscripcion -> mapSuscripcion(suscripcion, true))
                .orElseGet(() -> mapSedeSinConfigurar(sede));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SuperAdminPagoSuscripcionDTO> listarPagos(Long sedeId) {
        List<PagoSuscripcionSede> pagos = sedeId != null
                ? pagoSuscripcionSedeRepository.findBySedeIdOrderByFechaPagoDescIdDesc(sedeId)
                : pagoSuscripcionSedeRepository.findAllByOrderByFechaPagoDescIdDesc();

        return pagos.stream()
                .map(this::mapPago)
                .toList();
    }

    private TipoCobroSuscripcion parseTipoCobro(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new RuntimeException("El " + campo + " es obligatorio");
        }

        try {
            return TipoCobroSuscripcion.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("El " + campo + " no es valido");
        }
    }

    private Double normalizarValor(Double valor, String campo) {
        if (valor == null) {
            return 0D;
        }
        if (valor < 0) {
            throw new RuntimeException("El " + campo + " no puede ser negativo");
        }
        return valor;
    }

    private void actualizarEstado(SuscripcionSede suscripcion) {
        if (!Boolean.TRUE.equals(suscripcion.getActiva())) {
            suscripcion.setEstadoServicio(EstadoSuscripcionSede.SUSPENDIDO);
            return;
        }

        LocalDate vencimiento = suscripcion.getFechaProximoVencimiento();
        if (vencimiento == null) {
            suscripcion.setEstadoServicio(EstadoSuscripcionSede.VENCIDO);
            return;
        }

        LocalDate hoy = LocalDate.now();
        if (vencimiento.isAfter(hoy.plusDays(DIAS_POR_VENCER))) {
            suscripcion.setEstadoServicio(EstadoSuscripcionSede.ACTIVO);
        } else if (!vencimiento.isBefore(hoy)) {
            suscripcion.setEstadoServicio(EstadoSuscripcionSede.POR_VENCER);
        } else {
            suscripcion.setEstadoServicio(EstadoSuscripcionSede.VENCIDO);
        }
    }

    private SuperAdminSuscripcionSedeDTO mapSuscripcion(SuscripcionSede suscripcion, boolean incluirPagos) {
        EstadoSuscripcionSede estado = calcularEstado(suscripcion);
        Long diasRestantes = calcularDiasRestantes(suscripcion.getFechaProximoVencimiento());

        return new SuperAdminSuscripcionSedeDTO(
                suscripcion.getId(),
                true,
                suscripcion.getSede() != null && suscripcion.getSede().getEmpresa() != null ? suscripcion.getSede().getEmpresa().getNit() : null,
                suscripcion.getSede() != null && suscripcion.getSede().getEmpresa() != null ? suscripcion.getSede().getEmpresa().getNombre() : null,
                suscripcion.getSede() != null ? suscripcion.getSede().getId() : null,
                suscripcion.getSede() != null ? suscripcion.getSede().getUbicacion() : null,
                suscripcion.getTipoCobro() != null ? suscripcion.getTipoCobro().name() : null,
                suscripcion.getPrecioMensual(),
                suscripcion.getPrecioAnual(),
                suscripcion.getFechaInicioServicio(),
                suscripcion.getFechaUltimoPago(),
                suscripcion.getFechaProximoVencimiento(),
                estado.name(),
                estado == EstadoSuscripcionSede.ACTIVO || estado == EstadoSuscripcionSede.POR_VENCER,
                diasRestantes,
                suscripcion.getActiva(),
                suscripcion.getObservacion(),
                incluirPagos
                        ? suscripcion.getPagos().stream()
                        .sorted(Comparator.comparing(PagoSuscripcionSede::getFechaPago).reversed()
                                .thenComparing(PagoSuscripcionSede::getId, Comparator.reverseOrder()))
                        .map(this::mapPago)
                        .toList()
                        : List.of()
        );
    }

    private SuperAdminSuscripcionSedeDTO mapSedeSinConfigurar(Sede sede) {
        return new SuperAdminSuscripcionSedeDTO(
                null,
                false,
                sede.getEmpresa() != null ? sede.getEmpresa().getNit() : null,
                sede.getEmpresa() != null ? sede.getEmpresa().getNombre() : null,
                sede.getId(),
                sede.getUbicacion(),
                null,
                null,
                null,
                null,
                null,
                null,
                "SIN_CONFIGURAR",
                false,
                null,
                false,
                null,
                List.of()
        );
    }

    private EstadoSuscripcionSede calcularEstado(SuscripcionSede suscripcion) {
        SuscripcionSede copia = new SuscripcionSede();
        copia.setActiva(suscripcion.getActiva());
        copia.setFechaProximoVencimiento(suscripcion.getFechaProximoVencimiento());
        actualizarEstado(copia);
        return copia.getEstadoServicio();
    }

    private Long calcularDiasRestantes(LocalDate fechaProximoVencimiento) {
        if (fechaProximoVencimiento == null) {
            return null;
        }
        return ChronoUnit.DAYS.between(LocalDate.now(), fechaProximoVencimiento);
    }

    private SuperAdminPagoSuscripcionDTO mapPago(PagoSuscripcionSede pago) {
        return new SuperAdminPagoSuscripcionDTO(
                pago.getId(),
                pago.getSuscripcion() != null ? pago.getSuscripcion().getId() : null,
                pago.getSede() != null && pago.getSede().getEmpresa() != null ? pago.getSede().getEmpresa().getNit() : null,
                pago.getSede() != null && pago.getSede().getEmpresa() != null ? pago.getSede().getEmpresa().getNombre() : null,
                pago.getSede() != null ? pago.getSede().getId() : null,
                pago.getSede() != null ? pago.getSede().getUbicacion() : null,
                pago.getTipoPago() != null ? pago.getTipoPago().name() : null,
                pago.getValor(),
                pago.getFechaPago(),
                pago.getPeriodoDesde(),
                pago.getPeriodoHasta(),
                pago.getMedioPago(),
                pago.getObservacion(),
                pago.getRegistradoPor()
        );
    }
}
