package com.umg.sigelab.service;

import com.umg.sigelab.exception.BusinessException;
import com.umg.sigelab.model.Usuario;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cliente de la API facial local (Python + OpenCV) en http://127.0.0.1:5001.
 *  - verificar: segundo factor del login (seccion 6.1, RN-02). Devuelve true solo si la API responde ok:true.
 *  - tieneRostro / registrarRostro / eliminarRostro: gestion del rostro de un usuario.
 * Los permisos y la auditoria de la gestion NO van aqui: los aplica UsuarioService.
 * Si la API no esta disponible se lanza BusinessException con un mensaje comprensible.
 */
public class FacialService {

    private static final String URL_BASE = "http://127.0.0.1:5001";
    private static final Pattern OK_TRUE = Pattern.compile("\"ok\"\\s*:\\s*true");
    private static final Pattern REGISTRADO_TRUE = Pattern.compile("\"registrado\"\\s*:\\s*true");
    private static final Pattern REGISTRADO_FALSE = Pattern.compile("\"registrado\"\\s*:\\s*false");
    private static final Pattern MENSAJE = Pattern.compile("\"mensaje\"\\s*:\\s*\"([^\"]*)\"");

    private final HttpClient cliente = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    /** Segundo factor del login (RN-02). */
    public boolean verificar(Usuario usuario) {
        String cuerpo = enviar("POST", "/verificar/" + usuario.getId(), 60, " Acceso denegado.");
        if (REGISTRADO_FALSE.matcher(cuerpo).find()) {
            throw new BusinessException("El usuario no tiene un rostro registrado. "
                    + "Pida a un administrador que lo registre.");
        }
        return OK_TRUE.matcher(cuerpo).find();
    }

    /** Consulta liviana: no enciende la camara. */
    public boolean tieneRostro(int idUsuario) {
        String cuerpo = enviar("GET", "/estado/" + idUsuario, 5, "");
        return REGISTRADO_TRUE.matcher(cuerpo).find();
    }

    /** Captura y guarda el rostro del usuario (dura unos 20 segundos). */
    public void registrarRostro(int idUsuario) {
        String cuerpo = enviar("POST", "/registrar/" + idUsuario, 60, "");
        if (!OK_TRUE.matcher(cuerpo).find()) {
            throw new BusinessException(mensajeDe(cuerpo, "No se pudo registrar el rostro."));
        }
    }

    /** Elimina el rostro registrado del usuario. */
    public void eliminarRostro(int idUsuario) {
        String cuerpo = enviar("DELETE", "/rostro/" + idUsuario, 10, "");
        if (!OK_TRUE.matcher(cuerpo).find()) {
            throw new BusinessException(mensajeDe(cuerpo, "No se pudo eliminar el rostro."));
        }
    }

    // ---------------- privados ----------------

    private String enviar(String metodo, String ruta, int segundos, String consecuencia) {
        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(URL_BASE + ruta))
                .timeout(Duration.ofSeconds(segundos))
                .method(metodo, HttpRequest.BodyPublishers.noBody())
                .build();
        try {
            HttpResponse<String> respuesta = cliente.send(peticion, HttpResponse.BodyHandlers.ofString());
            return respuesta.body() == null ? "" : respuesta.body();
        } catch (IOException e) {
            throw new BusinessException("No se pudo comunicar con el reconocimiento facial. "
                    + "Verifique que el componente facial est\u00e9 iniciado." + consecuencia);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("La operaci\u00f3n facial fue interrumpida." + consecuencia);
        }
    }

    private String mensajeDe(String cuerpo, String porDefecto) {
        Matcher m = MENSAJE.matcher(cuerpo);
        return m.find() ? m.group(1) : porDefecto;
    }
}
