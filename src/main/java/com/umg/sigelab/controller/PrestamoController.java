package com.umg.sigelab.controller;

import com.umg.sigelab.model.Equipo;
import com.umg.sigelab.service.PrestamoService;

import java.time.LocalDate;
import java.util.List;

/**
 * Coordina las acciones de la pantalla de prestamos.
 * No contiene reglas de negocio ni SQL: solo delega al Service.
 */
public class PrestamoController {

    private final PrestamoService prestamoService = new PrestamoService();

    public List<Equipo> listarEquiposDisponibles() {
        return prestamoService.listarEquiposDisponibles();
    }

    public int crearPrestamo(String solicitante, LocalDate fechaEsperada,
                             String observaciones, List<Integer> equipoIds) {
        return prestamoService.crearPrestamo(solicitante, fechaEsperada, observaciones, equipoIds);
    }
}