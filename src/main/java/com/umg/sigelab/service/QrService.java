package com.umg.sigelab.service;

import com.google.zxing.ReaderException;
import com.google.zxing.WriterException;
import com.umg.sigelab.exception.BusinessException;
import com.umg.sigelab.model.Equipo;
import com.umg.sigelab.model.NombreRol;
import com.umg.sigelab.util.QrUtil;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

public class QrService {

    private final EquipoService equipoService = new EquipoService();

    /**
     * Comprueba que el equipo exista y genera el QR
     * con su código de inventario guardado en SQL Server.
     */
    public BufferedImage generar(String codigoInventario) {
        exigirPermiso();

        Equipo equipo = equipoService.buscarPorCodigo(codigoInventario);

        try {
            return QrUtil.generar(equipo.getCodigoInventario());
        } catch (WriterException e) {
            throw new BusinessException(
                    "No se pudo generar el QR del equipo."
            );
        }
    }

    /**
     * Lee el QR y consulta los datos actuales del equipo.
     * Leer un QR no modifica su estado.
     */
    public Equipo leer(Path archivo) {
        exigirPermiso();

        String codigo;

        try {
            codigo = QrUtil.leer(archivo);
        } catch (IOException e) {
            throw new BusinessException(
                    "No se pudo abrir la imagen. Seleccione un archivo PNG o JPG válido."
            );
        } catch (ReaderException e) {
            throw new BusinessException(
                    "No se pudo leer un QR en la imagen. Use una imagen clara y completa."
            );
        }

        if (codigo == null || codigo.isBlank() || codigo.length() > 30) {
            throw new BusinessException(
                    "El QR no contiene un código de inventario válido."
            );
        }

        return equipoService.buscarPorCodigo(codigo);
    }

    /**
     * Guarda la imagen en el destino elegido desde el formulario.
     */
    public void guardarPng(BufferedImage imagen, Path destino) {
        exigirPermiso();

        if (imagen == null || destino == null) {
            throw new BusinessException(
                    "Primero genere un QR y seleccione dónde guardarlo."
            );
        }

        try {
            QrUtil.guardarPng(imagen, destino);
        } catch (IOException e) {
            throw new BusinessException(
                    "No se pudo guardar el QR. Revise los permisos y el espacio disponible."
            );
        }
    }

    private void exigirPermiso() {
        AutorizacionService.exigirRol(
                NombreRol.ADMIN,
                NombreRol.ENCARGADO
        );
    }
}
