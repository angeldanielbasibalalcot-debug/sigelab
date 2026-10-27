package com.umg.sigelab.service;

import com.umg.sigelab.model.Equipo;
import com.umg.sigelab.repository.PrestamoRepository;

import java.util.List;

public class PrestamoService {

    private PrestamoRepository repository =
            new PrestamoRepository();

    public List<Equipo> listarEquiposDisponibles() {

        return repository.listarEquiposDisponibles();

    }
}
