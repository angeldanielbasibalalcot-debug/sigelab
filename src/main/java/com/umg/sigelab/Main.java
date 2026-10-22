package com.umg.sigelab;

import com.umg.sigelab.view.Inicio_de_sesion;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Punto de entrada de SIGELAB. */
public class Main {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignorada) {
            // Si falla, se usa el aspecto por defecto de Swing.
        }
        SwingUtilities.invokeLater(Inicio_de_sesion::abrir);
    }
}