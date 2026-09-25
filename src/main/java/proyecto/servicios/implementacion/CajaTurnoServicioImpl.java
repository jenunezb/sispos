package proyecto.servicios.implementacion;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import proyecto.dto.CajaAperturaDTO;
import proyecto.dto.CajaCierreDTO;
import proyecto.dto.CajaResumenDTO;
import proyecto.dto.CajaTurnoResponseDTO;
import proyecto.entidades.Administrador;
import proyecto.entidades.CajaTurno;
import proyecto.entidades.EstadoCaja;
import proyecto.entidades.ModoPago;
import proyecto.entidades.Sede;
import proyecto.entidades.Vendedor;
import proyecto.repositorios.CajaTurnoRepository;
import proyecto.repositorios.GastoDiarioRepository;
import proyecto.repositorios.SedeRepository;
import proyecto.repositorios.VentaRepository;
import proyecto.servicios.interfaces.CajaTurnoServicio;

import java.time.LocalDateTime;
import java.util.List;
import proyecto.utils.FechaColombiaUtils;

@Service
@RequiredArgsConstructor
public class CajaTurnoServicioImpl implements CajaTurnoServicio {

    private final CajaTurnoRepository cajaTurnoRepository;
    private final SedeRepository sedeRepository;
    private final VentaRepository ventaRepository;
    private final GastoDiarioRepository gastoDiarioRepository;

    @Override
    public CajaTurnoResponseDTO abrirCaja(Administrador administrador, CajaAperturaDTO dto) {
        if (cajaTurnoRepository.existsBySedeIdAndEstado(dto.sedeId(), EstadoCaja.ABIERTA)) {
            throw new RuntimeException("Ya existe una caja abierta para la sede seleccionada");
        }

        Sede sede = sedeRepository.findById(dto.sedeId())
                .orElseThrow(() -> new RuntimeException("Sede no encontrada"));

        CajaTurno caja = new CajaTurno();
        caja.setSede(sede);
        caja.setAdministradorApertura(administrador);
        caja.setFechaApertura(FechaColombiaUtils.ahora());
        caja.setEstado(EstadoCaja.ABIERTA);
        caja.setBaseInicial(dto.baseInicial());
        caja.setObservacion(normalizarTexto(dto.observacion()));
        caja.setVentasEfectivo(0.0);
        caja.setGastosEfectivo(0.0);
        caja.setEfectivoEsperado(dto.baseInicial());
        caja.setDiferencia(0.0);

        return mapToResponse(cajaTurnoRepository.save(caja));
    }

    @Override
    public CajaTurnoResponseDTO abrirCaja(Vendedor vendedor, CajaAperturaDTO dto) {
        if (cajaTurnoRepository.existsBySedeIdAndEstado(dto.sedeId(), EstadoCaja.ABIERTA)) {
            throw new RuntimeException("Ya existe una caja abierta para la sede seleccionada");
        }

        Sede sede = sedeRepository.findById(dto.sedeId())
                .orElseThrow(() -> new RuntimeException("Sede no encontrada"));

        CajaTurno caja = new CajaTurno();
        caja.setSede(sede);
        caja.setVendedorApertura(vendedor);
        caja.setFechaApertura(FechaColombiaUtils.ahora());
        caja.setEstado(EstadoCaja.ABIERTA);
        caja.setBaseInicial(dto.baseInicial());
        caja.setObservacion(normalizarTexto(dto.observacion()));
        caja.setVentasEfectivo(0.0);
        caja.setGastosEfectivo(0.0);
        caja.setEfectivoEsperado(dto.baseInicial());
        caja.setDiferencia(0.0);

        return mapToResponse(cajaTurnoRepository.save(caja));
    }

    @Override
    public CajaTurnoResponseDTO cerrarCaja(Administrador administrador, Long cajaId, CajaCierreDTO dto) {
        CajaTurno caja = cajaTurnoRepository.findById(cajaId)
                .orElseThrow(() -> new RuntimeException("Caja no encontrada"));

        if (caja.getEstado() != EstadoCaja.ABIERTA) {
            throw new RuntimeException("La caja seleccionada ya fue cerrada");
        }

        LocalDateTime fechaCierre = FechaColombiaUtils.ahora();
        CajaResumenDTO resumen = construirResumen(caja, fechaCierre);
        double efectivoContado = dto.efectivoContado() != null ? dto.efectivoContado() : 0.0;

        caja.setAdministradorCierre(administrador);
        caja.setFechaCierre(fechaCierre);
        caja.setEstado(EstadoCaja.CERRADA);
        caja.setVentasEfectivo(resumen.ventasEfectivo());
        caja.setGastosEfectivo(resumen.gastosEfectivo());
        caja.setEfectivoEsperado(resumen.efectivoEsperado());
        caja.setEfectivoContado(efectivoContado);
        caja.setDiferencia(efectivoContado - resumen.efectivoEsperado());
        caja.setObservacionCierre(normalizarTexto(dto.observacionCierre()));

        return mapToResponse(cajaTurnoRepository.save(caja));
    }

    @Override
    public CajaTurnoResponseDTO cerrarCaja(Vendedor vendedor, Long cajaId, CajaCierreDTO dto) {
        CajaTurno caja = cajaTurnoRepository.findById(cajaId)
                .orElseThrow(() -> new RuntimeException("Caja no encontrada"));

        if (caja.getEstado() != EstadoCaja.ABIERTA) {
            throw new RuntimeException("La caja seleccionada ya fue cerrada");
        }

        LocalDateTime fechaCierre = FechaColombiaUtils.ahora();
        CajaResumenDTO resumen = construirResumen(caja, fechaCierre);
        double efectivoContado = dto.efectivoContado() != null ? dto.efectivoContado() : 0.0;

        caja.setVendedorCierre(vendedor);
        caja.setFechaCierre(fechaCierre);
        caja.setEstado(EstadoCaja.CERRADA);
        caja.setVentasEfectivo(resumen.ventasEfectivo());
        caja.setGastosEfectivo(resumen.gastosEfectivo());
        caja.setEfectivoEsperado(resumen.efectivoEsperado());
        caja.setEfectivoContado(efectivoContado);
        caja.setDiferencia(efectivoContado - resumen.efectivoEsperado());
        caja.setObservacionCierre(normalizarTexto(dto.observacionCierre()));

        return mapToResponse(cajaTurnoRepository.save(caja));
    }

    @Override
    public CajaTurnoResponseDTO obtenerCajaActual(Long sedeId) {
        CajaTurno caja = cajaTurnoRepository.findFirstBySedeIdAndEstadoOrderByFechaAperturaDesc(sedeId, EstadoCaja.ABIERTA)
                .orElseThrow(() -> new RuntimeException("No hay una caja abierta para la sede seleccionada"));

        return mapToResponse(caja);
    }

    @Override
    public CajaTurnoResponseDTO obtenerPorId(Long cajaId) {
        CajaTurno caja = cajaTurnoRepository.findById(cajaId)
                .orElseThrow(() -> new RuntimeException("Caja no encontrada"));
        return mapToResponse(caja);
    }

    @Override
    public List<CajaTurnoResponseDTO> listar(Long empresaNit, Long sedeId, LocalDateTime desde, LocalDateTime hasta) {
        return cajaTurnoRepository.listarPorEmpresaYSedeEntreFechas(empresaNit, sedeId, desde, hasta)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private CajaTurnoResponseDTO mapToResponse(CajaTurno caja) {
        CajaResumenDTO resumen = construirResumen(caja, caja.getFechaCierre() != null ? caja.getFechaCierre() : FechaColombiaUtils.ahora());

        return new CajaTurnoResponseDTO(
                caja.getId(),
                caja.getSede().getId(),
                caja.getSede().getUbicacion(),
                caja.getEstado(),
                caja.getFechaApertura(),
                caja.getFechaCierre(),
                caja.getBaseInicial(),
                resumen,
                caja.getObservacion(),
                caja.getObservacionCierre(),
                caja.getAdministradorApertura() != null ? caja.getAdministradorApertura().getCodigo() : null,
                nombreCompleto(caja.getAdministradorApertura()),
                caja.getAdministradorCierre() != null ? caja.getAdministradorCierre().getCodigo() : null,
                nombreCompleto(caja.getAdministradorCierre()),
                caja.getVendedorApertura() != null ? caja.getVendedorApertura().getCodigo() : null,
                nombreCompleto(caja.getVendedorApertura()),
                caja.getVendedorCierre() != null ? caja.getVendedorCierre().getCodigo() : null,
                nombreCompleto(caja.getVendedorCierre())
        );
    }

    private CajaResumenDTO construirResumen(CajaTurno caja, LocalDateTime fechaHasta) {
        double ventasEfectivo = defaultDouble(
                ventaRepository.totalVentasEfectivoPorSedeEntreFechas(caja.getSede().getId(), caja.getFechaApertura(), fechaHasta)
        );
        double gastosEfectivo = defaultDouble(
                gastoDiarioRepository.totalGastosPorSedeYModoPago(caja.getSede().getId(), ModoPago.EFECTIVO, caja.getFechaApertura(), fechaHasta)
        );
        double efectivoEsperado = defaultDouble(caja.getBaseInicial()) + ventasEfectivo - gastosEfectivo;
        double efectivoContado = caja.getEfectivoContado() != null ? caja.getEfectivoContado() : 0.0;
        double diferencia = caja.getEstado() == EstadoCaja.CERRADA
                ? efectivoContado - efectivoEsperado
                : 0.0;

        return new CajaResumenDTO(
                defaultDouble(caja.getBaseInicial()),
                ventasEfectivo,
                gastosEfectivo,
                efectivoEsperado,
                caja.getEfectivoContado(),
                diferencia
        );
    }

    private double defaultDouble(Double valor) {
        return valor != null ? valor : 0.0;
    }

    private String normalizarTexto(String valor) {
        if (valor == null) {
            return null;
        }
        String texto = valor.trim();
        return texto.isBlank() ? null : texto;
    }

    private String nombreCompleto(Administrador administrador) {
        if (administrador == null) {
            return null;
        }

        String nombre = administrador.getNombre() != null ? administrador.getNombre().trim() : "";
        String apellido = administrador.getApellido() != null ? administrador.getApellido().trim() : "";
        String nombreCompleto = (nombre + " " + apellido).trim();
        return nombreCompleto.isBlank() ? null : nombreCompleto;
    }

    private String nombreCompleto(Vendedor vendedor) {
        if (vendedor == null) {
            return null;
        }
        String nombre = vendedor.getNombre() != null ? vendedor.getNombre().trim() : "";
        return nombre.isBlank() ? null : nombre;
    }
}
