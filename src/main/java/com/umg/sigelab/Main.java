package com.umg.sigelab;

import com.umg.sigelab.view.LoginFrame;

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
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
