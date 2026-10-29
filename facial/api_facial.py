"""
API local de reconocimiento facial para SIGELAB (seccion 6.1).
Solo escucha en 127.0.0.1. Java la consume con HttpClient.

Variables de entorno opcionales:
  SIGELAB_CAMARA   indice de la camara (por defecto 0)
  SIGELAB_PUERTO   puerto de la API (por defecto 5001)
  SIGELAB_UMBRAL   distancia maxima aceptada, menor = mas estricto (por defecto 50)
"""
import os
import shutil
import threading
import time

import cv2
import numpy as np
from flask import Flask, jsonify

CAMARA = int(os.environ.get("SIGELAB_CAMARA", "0"))
PUERTO = int(os.environ.get("SIGELAB_PUERTO", "5001"))
UMBRAL = float(os.environ.get("SIGELAB_UMBRAL", "50"))

MUESTRAS_REGISTRO = 20
MINIMO_REGISTRO = 10
MUESTRAS_VERIFICAR = 5
MINIMO_VERIFICAR = 3
SEGUNDOS_MAXIMOS = 15
TAMANO = (200, 200)

CARPETA = os.path.join(os.path.dirname(os.path.abspath(__file__)), "rostros")
detector = cv2.CascadeClassifier(cv2.data.haarcascades + "haarcascade_frontalface_default.xml")

app = Flask(__name__)
candado = threading.Lock()  # la camara solo admite una operacion a la vez


def capturar_rostros(cantidad):
    """Captura hasta 'cantidad' rostros recortados en gris. Devuelve la lista."""
    camara = cv2.VideoCapture(CAMARA, cv2.CAP_DSHOW)
    rostros = []
    if not camara.isOpened():
        camara.release()
        return rostros
    for _ in range(10):  # calentamiento de la camara
        camara.read()
    inicio = time.time()
    while len(rostros) < cantidad and time.time() - inicio < SEGUNDOS_MAXIMOS:
        ok, imagen = camara.read()
        if not ok or imagen is None:
            continue
        gris = cv2.equalizeHist(cv2.cvtColor(imagen, cv2.COLOR_BGR2GRAY))
        encontrados = detector.detectMultiScale(gris, scaleFactor=1.1, minNeighbors=4, minSize=(80, 80))
        if len(encontrados) == 0:
            continue
        x, y, w, h = max(encontrados, key=lambda r: r[2] * r[3])
        rostros.append(cv2.resize(gris[y:y + h, x:x + w], TAMANO))
        time.sleep(0.15)
    camara.release()
    return rostros


def carpeta_usuario(usuario_id):
    return os.path.join(CARPETA, str(usuario_id))


def respuesta(ok, mensaje, **extra):
    datos = {"ok": ok, "mensaje": mensaje}
    datos.update(extra)
    return jsonify(datos)


@app.get("/salud")
def salud():
    return respuesta(True, "API facial activa", camara=CAMARA, umbral=UMBRAL)


@app.post("/registrar/<int:usuario_id>")
def registrar(usuario_id):
    with candado:
        rostros = capturar_rostros(MUESTRAS_REGISTRO)
        if len(rostros) < MINIMO_REGISTRO:
            return respuesta(False, "No se detecto su rostro con claridad. Mire de frente a la camara con buena luz.")
        destino = carpeta_usuario(usuario_id)
        shutil.rmtree(destino, ignore_errors=True)
        os.makedirs(destino, exist_ok=True)
        for i, rostro in enumerate(rostros, start=1):
            cv2.imwrite(os.path.join(destino, "%d.png" % i), rostro)
        return respuesta(True, "Rostro registrado correctamente.", muestras=len(rostros))


@app.post("/verificar/<int:usuario_id>")
def verificar(usuario_id):
    with candado:
        origen = carpeta_usuario(usuario_id)
        if not os.path.isdir(origen) or not os.listdir(origen):
            return respuesta(False, "El usuario no tiene un rostro registrado.", registrado=False)
        entrenamiento = []
        for nombre in os.listdir(origen):
            imagen = cv2.imread(os.path.join(origen, nombre), cv2.IMREAD_GRAYSCALE)
            if imagen is not None:
                entrenamiento.append(imagen)
        if not entrenamiento:
            return respuesta(False, "El rostro registrado no se pudo leer.", registrado=False)

        reconocedor = cv2.face.LBPHFaceRecognizer_create()
        reconocedor.train(entrenamiento, np.zeros(len(entrenamiento), dtype=np.int32))

        capturados = capturar_rostros(MUESTRAS_VERIFICAR)
        if len(capturados) < MINIMO_VERIFICAR:
            return respuesta(False, "No se detecto su rostro con claridad.", registrado=True)

        distancias = [float(reconocedor.predict(r)[1]) for r in capturados]
        mediana = float(np.median(distancias))
        coincide = mediana <= UMBRAL
        mensaje = "Rostro reconocido." if coincide else "El rostro no corresponde al usuario."
        return respuesta(coincide, mensaje, registrado=True, distancia=round(mediana, 1))


@app.delete("/rostro/<int:usuario_id>")
def eliminar(usuario_id):
    with candado:
        destino = carpeta_usuario(usuario_id)
        if not os.path.isdir(destino):
            return respuesta(False, "El usuario no tiene un rostro registrado.")
        shutil.rmtree(destino, ignore_errors=True)
        return respuesta(True, "Rostro eliminado.")


if __name__ == "__main__":
    os.makedirs(CARPETA, exist_ok=True)
    print("API facial en http://127.0.0.1:%d (camara %d, umbral %.0f)" % (PUERTO, CAMARA, UMBRAL))
    app.run(host="127.0.0.1", port=PUERTO, threaded=True)
