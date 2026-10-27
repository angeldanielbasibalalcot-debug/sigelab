package com.umg.sigelab.service;

import com.umg.sigelab.model.Equipo;
import com.umg.sigelab.repository.PrestamoRepository;

import java.util.List;

import com.umg.sigelab.model.Prestamo;

public class PrestamoService {

    private PrestamoRepository repository =
            new PrestamoRepository();

    public List<Equipo> listarEquiposDisponibles() {

        return repository.listarEquiposDisponibles();


}
public int guardarPrestamo(Prestamo prestamo) {

    return repository.guardarPrestamo(prestamo);

    }
}