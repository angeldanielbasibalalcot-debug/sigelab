package com.umg.sigelab.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.ReaderException;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class QrUtil {

    private static final int TAMANO = 350;

    private QrUtil() {
    }

    /**
     * Genera una imagen QR cuyo contenido es solo el código de inventario.
     */
    public static BufferedImage generar(String codigoInventario)
            throws WriterException {

        if (codigoInventario == null || codigoInventario.isBlank()) {
            throw new IllegalArgumentException(
                    "El código de inventario no puede estar vacío."
            );
        }

        Map<EncodeHintType, Object> opciones = Map.of(
                EncodeHintType.CHARACTER_SET, "UTF-8",
                EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN, 4
        );

        BitMatrix matriz = new QRCodeWriter().encode(
                codigoInventario,
                BarcodeFormat.QR_CODE,
                TAMANO,
                TAMANO,
                opciones
        );

        return MatrixToImageWriter.toBufferedImage(matriz);
    }

    /**
     * Guarda una imagen como PNG en la ruta indicada.
     * La pantalla solicitará confirmación si el archivo ya existe.
     */
    public static void guardarPng(BufferedImage imagen, Path destino)
            throws IOException {

        if (imagen == null || destino == null) {
            throw new IllegalArgumentException(
                    "Debe indicar una imagen y una ruta de destino."
            );
        }

        Path carpeta = destino.toAbsolutePath().getParent();
        Files.createDirectories(carpeta);

        // Usamos un stream para evitar la caché temporal de ImageIO en disco.
        try (var salida = Files.newOutputStream(destino)) {
            if (!ImageIO.write(imagen, "PNG", salida)) {
                throw new IOException(
                        "No se pudo guardar la imagen en formato PNG."
                );
            }
        }
    }

    /**
     * Lee el contenido de un QR desde una imagen.
     * No consulta ni modifica la base de datos.
     */
    public static String leer(Path archivo)
            throws IOException, ReaderException {

        if (archivo == null || !Files.isRegularFile(archivo)) {
            throw new IOException("Seleccione un archivo de imagen válido.");
        }

        BufferedImage imagen = ImageIO.read(archivo.toFile());

        if (imagen == null) {
            throw new IOException(
                    "El archivo seleccionado no es una imagen compatible."
            );
        }

        BufferedImageLuminanceSource fuente =
                new BufferedImageLuminanceSource(imagen);

        BinaryBitmap bitmap =
                new BinaryBitmap(new HybridBinarizer(fuente));

        Map<DecodeHintType, Object> opciones = Map.of(
                DecodeHintType.CHARACTER_SET, "UTF-8",
                DecodeHintType.TRY_HARDER, Boolean.TRUE
        );

        return new QRCodeReader().decode(bitmap, opciones).getText();
    }
}
