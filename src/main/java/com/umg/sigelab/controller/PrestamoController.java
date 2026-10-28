package com.umg.sigelab.controller;

import com.umg.sigelab.model.Equipo;
import com.umg.sigelab.service.PrestamoService;

import java.nio.file.Path;
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

    /** Lee el QR de una imagen y devuelve el equipo real. No cambia estados (RN-19). */
    public Equipo identificarPorQr(Path imagen) {
        return prestamoService.identificarPorQr(imagen);
    }

    public int crearPrestamo(String solicitante, LocalDate fechaEsperada,
                             String observaciones, List<Integer> equipoIds) {
        return prestamoService.crearPrestamo(solicitante, fechaEsperada, observaciones, equipoIds);
    }
}