package com.umg.sigelab.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.umg.sigelab.exception.BusinessException;

import java.awt.image.BufferedImage;
import java.util.Map;

/**
 * Herramienta para generar codigos QR con ZXing (6.6 de la guia).
 * El QR contiene unicamente el codigo de inventario del equipo y sirve solo para identificarlo:
 * no guarda estado ni modifica nada (RN-19).
 */
public final class QrUtil {

    private QrUtil() {
    }

    /** Genera una imagen cuadrada de 'tamano' x 'tamano' pixeles con el texto indicado. */
    public static BufferedImage generarImagen(String contenido, int tamano) {
        if (contenido == null || contenido.isBlank()) {
            throw new BusinessException("No hay un código para generar el QR.");
        }
        try {
            Map<EncodeHintType, Object> pistas = Map.of(
                    EncodeHintType.MARGIN, 2,
                    EncodeHintType.CHARACTER_SET, "UTF-8");
            BitMatrix matriz = new QRCodeWriter()
                    .encode(contenido.trim(), BarcodeFormat.QR_CODE, tamano, tamano, pistas);
            return MatrixToImageWriter.toBufferedImage(matriz);
        } catch (WriterException e) {
            throw new BusinessException("No se pudo generar el código QR.");
        }
    }
}