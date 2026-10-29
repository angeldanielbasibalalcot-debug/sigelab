package com.umg.sigelab.service;

import com.umg.sigelab.exception.BusinessException;
import com.umg.sigelab.model.Usuario;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Pattern;

/**
 * Segundo factor del login (seccion 6.1, RN-02).
 * Consume la API facial local (Python + OpenCV) en http://127.0.0.1:5001.
 * Devuelve true solo si la API responde ok:true. Si la API no esta disponible,
 * o el usuario no tiene rostro registrado, se deniega el acceso con un mensaje comprensible.
 */
public class FacialService {

    private static final String URL_BASE = "http://127.0.0.1:5001";
    private static final Pattern OK_TRUE = Pattern.compile("\"ok\"\\s*:\\s*true");
    private static final Pattern REGISTRADO_FALSE = Pattern.compile("\"registrado\"\\s*:\\s*false");

    private final HttpClient cliente = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    public boolean verificar(Usuario usuario) {
        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(URL_BASE + "/verificar/" + usuario.getId()))
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        try {
            HttpResponse<String> respuesta = cliente.send(peticion, HttpResponse.BodyHandlers.ofString());
            String cuerpo = respuesta.body() == null ? "" : respuesta.body();
            if (REGISTRADO_FALSE.matcher(cuerpo).find()) {
                throw new BusinessException("El usuario no tiene un rostro registrado. "
                        + "Pida a un administrador que lo registre.");
            }
            return OK_TRUE.matcher(cuerpo).find();
        } catch (IOException e) {
            throw new BusinessException("No se pudo comunicar con el reconocimiento facial. "
                    + "Verifique que el componente facial est\u00e9 iniciado. Acceso denegado.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("La validaci\u00f3n facial fue interrumpida. Acceso denegado.");
        }
    }
}
