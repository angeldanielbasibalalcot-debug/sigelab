package com.umg.sigelab.service;

import com.umg.sigelab.model.Equipo;
import com.umg.sigelab.repository.PrestamoRepository;

import java.util.List;

import com.umg.sigelab.model.Prestamo;
import com.umg.sigelab.model.DetallePrestamo;

public class PrestamoService {

    private PrestamoRepository repository =
            new PrestamoRepository();

    public List<Equipo> listarEquiposDisponibles() {

        return repository.listarEquiposDisponibles();


}
public int guardarPrestamo(Prestamo prestamo) {

    return repository.guardarPrestamo(prestamo);

    }
    public void guardarDetallePrestamo(
            DetallePrestamo detalle) {

        repository.guardarDetallePrestamo(
                detalle);

    }

    public void actualizarEstadoEquipo(
            int equipoId) {

        repository.actualizarEstadoEquipo(
                equipoId);

    }

}