package com.umg.sigelab.service;

import com.umg.sigelab.exception.BusinessException;
import com.umg.sigelab.model.*;
import com.umg.sigelab.repository.AuditoriaRepository;
import com.umg.sigelab.repository.EquipoRepository;
import com.umg.sigelab.repository.LaboratorioRepository;
import com.umg.sigelab.repository.TipoEquipoRepository;
import com.umg.sigelab.util.SesionActual;
import com.umg.sigelab.util.Transaccion;

import java.sql.SQLException;
import java.util.List;

public class EquipoService {

    private final EquipoRepository equipoRepository =
            new EquipoRepository();

    private final TipoEquipoRepository tipoRepository =
            new TipoEquipoRepository();

    private final LaboratorioRepository laboratorioRepository =
            new LaboratorioRepository();

    private final AuditoriaRepository auditoriaRepository =
            new AuditoriaRepository();

    public List<Equipo> listar() {
        exigirPermiso();
        return equipoRepository.listarTodos();
    }

    public List<TipoEquipo> listarTipos() {
        exigirPermiso();
        return tipoRepository.listarTodos();
    }

    public List<Laboratorio> listarLaboratorios() {
        exigirPermiso();
        return laboratorioRepository.listarTodos();
    }

    public Equipo buscarPorCodigo(String codigo) {
        exigirPermiso();

        String limpio = obligatorio(
                codigo, "el código de inventario", 30);

        return equipoRepository.buscarPorCodigo(limpio)
                .orElseThrow(() -> new BusinessException(
                        "No existe un equipo con el código " + limpio + "."));
    }

    public void crear(Equipo datos) {
        exigirPermiso();

        Equipo nuevo = validarYPreparar(datos);
        nuevo.setId(0);

        // El estado inicial lo decide el Service.
        nuevo.setEstado(EstadoEquipo.DISPONIBLE);

        verificarCodigo(nuevo.getCodigoInventario(), 0);

        int actor = SesionActual.getUsuario().getId();

        Transaccion.ejecutarSinResultado(con -> {
            int id;

            try {
                id = equipoRepository.insertar(con, nuevo);
            } catch (SQLException ex) {
                throw traducirErrorEquipo(ex);
            }

            auditoriaRepository.registrar(
                    con,
                    actor,
                    AccionAuditoria.EQUIPO_CREADO,
                    "Equipo",
                    id,
                    "Equipo creado: " + nuevo.getCodigoInventario()
            );
        });
    }

    public void actualizar(Equipo datos) {
        exigirPermiso();

        if (datos == null || datos.getId() <= 0) {
            throw new BusinessException(
                    "Seleccione un equipo en la tabla.");
        }

        Equipo actual = equipoRepository.buscarPorId(datos.getId())
                .orElseThrow(() -> new BusinessException(
                        "El equipo seleccionado ya no existe. Recargue la lista."));

        Equipo cambios = validarYPreparar(datos);
        cambios.setId(actual.getId());
        cambios.setEstado(actual.getEstado());

        verificarCodigo(
                cambios.getCodigoInventario(), cambios.getId());

        int actor = SesionActual.getUsuario().getId();

        Transaccion.ejecutarSinResultado(con -> {
            try {
                // Este UPDATE no modifica la columna estado.
                equipoRepository.actualizar(con, cambios);
            } catch (SQLException ex) {
                throw traducirErrorEquipo(ex);
            }

            auditoriaRepository.registrar(
                    con,
                    actor,
                    AccionAuditoria.EQUIPO_ACTUALIZADO,
                    "Equipo",
                    cambios.getId(),
                    "Equipo actualizado: " + cambios.getCodigoInventario()
            );
        });
    }

    private Equipo validarYPreparar(Equipo datos) {
        if (datos == null) {
            throw new BusinessException(
                    "Ingrese los datos del equipo.");
        }

        String codigo = obligatorio(
                datos.getCodigoInventario(),
                "el código de inventario", 30);

        String nombre = obligatorio(
                datos.getNombre(), "el nombre del equipo", 100);

        String marca = opcional(datos.getMarca(), "La marca", 50);
        String modelo = opcional(datos.getModelo(), "El modelo", 50);

        String serie = opcional(
                datos.getNumeroSerie(), "El número de serie", 80);

        String observaciones = opcional(
                datos.getObservaciones(), "Las observaciones", 300);

        if (datos.getTipoEquipo() == null
                || datos.getTipoEquipo().getId() <= 0) {
            throw new BusinessException(
                    "Seleccione un tipo de equipo.");
        }

        if (datos.getLaboratorio() == null
                || datos.getLaboratorio().getId() <= 0) {
            throw new BusinessException(
                    "Seleccione un laboratorio.");
        }

        TipoEquipo tipo = tipoRepository
                .buscarPorId(datos.getTipoEquipo().getId())
                .orElseThrow(() -> new BusinessException(
                        "El tipo de equipo seleccionado ya no existe."));

        Laboratorio laboratorio = laboratorioRepository
                .buscarPorId(datos.getLaboratorio().getId())
                .orElseThrow(() -> new BusinessException(
                        "El laboratorio seleccionado ya no existe."));

        return new Equipo(
                datos.getId(),
                codigo,
                nombre,
                tipo,
                marca,
                modelo,
                serie,
                laboratorio,
                datos.getEstado(),
                observaciones
        );
    }

    private void verificarCodigo(String codigo, int idExcluir) {
        if (equipoRepository.existeCodigo(codigo, idExcluir)) {
            throw new BusinessException(
                    "Ya existe un equipo con el código " + codigo + ".");
        }
    }

    private String obligatorio(
            String valor, String campo, int maximo) {

        String limpio = valor == null ? "" : valor.trim();

        if (limpio.isEmpty()) {
            throw new BusinessException("Ingrese " + campo + ".");
        }

        if (limpio.length() > maximo) {
            throw new BusinessException(
                    "El campo " + campo
                            + " no puede superar los "
                            + maximo + " caracteres.");
        }

        return limpio;
    }

    private String opcional(
            String valor, String campo, int maximo) {

        String limpio = valor == null ? "" : valor.trim();

        if (limpio.isEmpty()) {
            return null;
        }

        if (limpio.length() > maximo) {
            throw new BusinessException(
                    campo + " no puede superar los "
                            + maximo + " caracteres.");
        }

        return limpio;
    }

    private void exigirPermiso() {
        AutorizacionService.exigirRol(
                NombreRol.ADMIN, NombreRol.ENCARGADO);
    }

    private SQLException traducirErrorEquipo(SQLException error) {
        // Protege también si dos usuarios intentan registrar
        // simultáneamente el mismo código.
        for (SQLException actual = error;
             actual != null;
             actual = actual.getNextException()) {

            if (actual.getErrorCode() == 2601
                    || actual.getErrorCode() == 2627) {
                throw new BusinessException(
                        "Ya existe un equipo con ese código de inventario.");
            }

            if (actual.getErrorCode() == 547) {
                throw new BusinessException(
                        "No se pudo guardar por una restricción de datos. "
                                + "Recargue los tipos y laboratorios "
                                + "y revise la selección.");
            }
        }

        return error;
    }
}
