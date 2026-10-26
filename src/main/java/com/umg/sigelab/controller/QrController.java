package com.umg.sigelab.controller;

import com.umg.sigelab.model.Equipo;
import com.umg.sigelab.service.QrService;

import java.awt.image.BufferedImage;
import java.nio.file.Path;

public class QrController {

    private final QrService service = new QrService();

    public BufferedImage generar(String codigoInventario) {
        return service.generar(codigoInventario);
    }

    public Equipo leer(Path archivo) {
        return service.leer(archivo);
    }

    public void guardarPng(BufferedImage imagen, Path destino) {
        service.guardarPng(imagen, destino);
    }
}
