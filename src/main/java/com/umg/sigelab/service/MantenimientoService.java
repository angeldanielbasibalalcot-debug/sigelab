package com.umg.sigelab.service;

import com.umg.sigelab.exception.BusinessException;
import com.umg.sigelab.model.*;
import com.umg.sigelab.repository.AuditoriaRepository;
import com.umg.sigelab.repository.EquipoRepository;
import com.umg.sigelab.repository.MantenimientoRepository;
import com.umg.sigelab.util.SesionActual;
import com.umg.sigelab.util.Transaccion;

import java.util.List;

public class MantenimientoService {

    private final MantenimientoRepository mantenimientoRepository =
            new MantenimientoRepository();

    private final EquipoRepository equipoRepository =
            new EquipoRepository();

    private final AuditoriaRepository auditoriaRepository =
            new AuditoriaRepository();

    public List<Mantenimiento> listar() {
        exigirPermiso();
        return mantenimientoRepository.listarTodos();
    }

    public List<Mantenimiento> listarEnProceso() {
        exigirPermiso();
        return mantenimientoRepository.listarEnProceso();
    }

    public List<Mantenimiento> listarPorEquipo(int equipoId) {
        exigirPermiso();
        return mantenimientoRepository.listarPorEquipo(equipoId);
    }

    // Equipos que SI pueden entrar a mantenimiento: DANADO o DISPONIBLE.
    public List<Equipo> listarEquiposElegibles() {
        exigirPermiso();

        return equipoRepository.listarTodos().stream()
                .filter(e -> e.getEstado() == EstadoEquipo.DANADO
                        || e.getEstado() == EstadoEquipo.DISPONIBLE)
                .toList();
    }

    // CP-15: DANADO -> MANTENIMIENTO. Tambien permite el preventivo
    // (DISPONIBLE -> MANTENIMIENTO).
    public void enviar(int equipoId, TipoMantenimiento tipo,
                       String descripcion, String responsable,
                       String observaciones) {
        exigirPermiso();

        if (equipoId <= 0) {
            throw new BusinessException("Seleccione un equipo.");
        }

        if (tipo == null) {
            throw new BusinessException(
                    "Seleccione el tipo de mantenimiento.");
        }

        String desc = obligatorio(descripcion, "la descripcion", 300);
        String resp = obligatorio(responsable, "el responsable", 100);
        String obs = opcional(observaciones, "Las observaciones", 300);

        Equipo equipo = equipoRepository.buscarPorId(equipoId)
                .orElseThrow(() -> new BusinessException(
                        "El equipo seleccionado ya no existe. Recargue la lista."));

        validarEstadoParaEnviar(equipo);

        if (mantenimientoRepository.existeEnProceso(equipo.getId())) {
            throw new BusinessException(
                    "El equipo " + equipo.getCodigoInventario()
                            + " ya tiene un mantenimiento en proceso.");
        }

        Mantenimiento nuevo = new Mantenimiento();
        nuevo.setEquipo(equipo);
        nuevo.setTipo(tipo);
        nuevo.setDescripcion(desc);
        nuevo.setResponsable(resp);
        nuevo.setObservaciones(obs);

        int actor = SesionActual.getUsuario().getId();
        nuevo.setUsuarioId(actor);

        // Una sola transaccion: registro + cambio de estado + auditoria.
        Transaccion.ejecutarSinResultado(con -> {
            int id = mantenimientoRepository.insertar(con, nuevo);

            mantenimientoRepository.actualizarEstadoEquipo(
                    con, equipo.getId(), EstadoEquipo.MANTENIMIENTO);

            auditoriaRepository.registrar(
                    con,
                    actor,
                    AccionAuditoria.EQUIPO_ENVIADO_MANTENIMIENTO,
                    "Mantenimiento",
                    id,
                    "Equipo " + equipo.getCodigoInventario()
                            + " enviado a mantenimiento " + tipo.name()
                            + " (estado anterior: "
                            + equipo.getEstado().name() + ")"
            );
        });
    }

    // CP-16: REPARADO -> DISPONIBLE. CP-17: NO_REPARABLE -> BAJA.
    public void finalizar(int mantenimientoId,
                          ResultadoMantenimiento resultado,
                          String observaciones) {
        exigirPermiso();

        if (mantenimientoId <= 0) {
            throw new BusinessException(
                    "Seleccione un mantenimiento en la tabla.");
        }

        if (resultado == null) {
            throw new BusinessException(
                    "Seleccione el resultado del mantenimiento.");
        }

        String obs = opcional(observaciones, "Las observaciones", 300);

        Mantenimiento mantenimiento =
                mantenimientoRepository.buscarPorId(mantenimientoId)
                        .orElseThrow(() -> new BusinessException(
                                "El mantenimiento seleccionado ya no existe. Recargue la lista."));

        if (mantenimiento.getEstado() != EstadoMantenimiento.EN_PROCESO) {
            throw new BusinessException(
                    "Ese mantenimiento ya fue finalizado.");
        }

        Equipo equipo = mantenimiento.getEquipo();

        EstadoEquipo nuevoEstado =
                resultado == ResultadoMantenimiento.REPARADO
                        ? EstadoEquipo.DISPONIBLE
                        : EstadoEquipo.BAJA;

        int actor = SesionActual.getUsuario().getId();

        // Una sola transaccion: cierre + estado del equipo + auditoria.
        Transaccion.ejecutarSinResultado(con -> {
            mantenimientoRepository.finalizar(
                    con, mantenimiento.getId(), resultado, obs);

            mantenimientoRepository.actualizarEstadoEquipo(
                    con, equipo.getId(), nuevoEstado);

            auditoriaRepository.registrar(
                    con,
                    actor,
                    AccionAuditoria.MANTENIMIENTO_FINALIZADO,
                    "Mantenimiento",
                    mantenimiento.getId(),
                    "Mantenimiento de " + equipo.getCodigoInventario()
                            + " finalizado como " + resultado.name()
                            + "; el equipo queda " + nuevoEstado.name()
            );
        });
    }

    private void validarEstadoParaEnviar(Equipo equipo) {
        String codigo = equipo.getCodigoInventario();

        switch (equipo.getEstado()) {
            case DANADO, DISPONIBLE -> {
                // Estados permitidos.
            }
            case PRESTADO -> throw new BusinessException(
                    "El equipo " + codigo + " esta PRESTADO. "
                            + "Debe devolverse antes de enviarlo a mantenimiento.");
            case MANTENIMIENTO -> throw new BusinessException(
                    "El equipo " + codigo + " ya esta en MANTENIMIENTO.");
            case BAJA -> throw new BusinessException(
                    "El equipo " + codigo + " esta dado de BAJA "
                            + "y no puede enviarse a mantenimiento.");
        }
    }

    private String obligatorio(String valor, String campo, int maximo) {
        String limpio = valor == null ? "" : valor.trim();

        if (limpio.isEmpty()) {
            throw new BusinessException("Ingrese " + campo + ".");
        }

        if (limpio.length() > maximo) {
            throw new BusinessException(
                    "El campo " + campo + " no puede superar los "
                            + maximo + " caracteres.");
        }

        return limpio;
    }

    private String opcional(String valor, String campo, int maximo) {
        String limpio = valor == null ? "" : valor.trim();

        if (limpio.isEmpty()) {
            return null;
        }

        if (limpio.length() > maximo) {
            throw new BusinessException(
                    campo + " no puede superar los " + maximo + " caracteres.");
        }

        return limpio;
    }

    private void exigirPermiso() {
        AutorizacionService.exigirRol(
                NombreRol.ADMIN, NombreRol.ENCARGADO);
    }
}