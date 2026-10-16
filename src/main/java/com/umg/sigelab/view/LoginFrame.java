package com.umg.sigelab.view;

import com.umg.sigelab.controller.AuthController;
import com.umg.sigelab.exception.BusinessException;
import com.umg.sigelab.exception.DataAccessException;
import com.umg.sigelab.model.Usuario;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.concurrent.ExecutionException;

/** Pantalla de inicio de sesion. Solo captura datos y muestra mensajes; no valida reglas. */
public class LoginFrame extends JFrame {

    private final AuthController controller = new AuthController();

    private final JTextField txtUsuario = new JTextField(18);
    private final JPasswordField txtPassword = new JPasswordField(18);
    private final JButton btnIngresar = new JButton("Ingresar");
    private final JLabel lblEstado = new JLabel(" ", SwingConstants.CENTER);

    public LoginFrame() {
        super("SIGELAB - Inicio de sesión");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setContentPane(construirPanel());
        getRootPane().setDefaultButton(btnIngresar);
        btnIngresar.addActionListener(e -> ingresar());
        setResizable(false);
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel construirPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel titulo = new JLabel("SIGELAB", SwingConstants.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 26f));
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        panel.add(titulo, c);

        JLabel subtitulo = new JLabel("Gestión de Laboratorios Universitarios", SwingConstants.CENTER);
        c.gridy = 1;
        panel.add(subtitulo, c);

        c.gridwidth = 1;
        c.gridy = 2;
        c.gridx = 0;
        panel.add(new JLabel("Usuario:"), c);
        c.gridx = 1;
        panel.add(txtUsuario, c);

        c.gridy = 3;
        c.gridx = 0;
        panel.add(new JLabel("Contraseña:"), c);
        c.gridx = 1;
        panel.add(txtPassword, c);

        c.gridy = 4;
        c.gridx = 0;
        c.gridwidth = 2;
        panel.add(btnIngresar, c);

        c.gridy = 5;
        panel.add(lblEstado, c);
        return panel;
    }

    private void ingresar() {
        final String usuario = txtUsuario.getText();
        final String password = new String(txtPassword.getPassword());
        setProcesando(true);

        // SwingWorker: el paso facial puede tardar y no debe congelar la ventana.
        new SwingWorker<Usuario, Void>() {
            @Override
            protected Usuario doInBackground() {
                Usuario u = controller.validarCredenciales(usuario, password);
                controller.validarRostro(u);
                controller.confirmarAcceso(u);
                return u;
            }

            @Override
            protected void done() {
                try {
                    get();
                    abrirPrincipal();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    setProcesando(false);
                } catch (ExecutionException e) {
                    setProcesando(false);
                    mostrarError(e.getCause());
                }
            }
        }.execute();
    }

    private void abrirPrincipal() {
        new MainFrame().setVisible(true);
        dispose();
    }

    private void setProcesando(boolean procesando) {
        btnIngresar.setEnabled(!procesando);
        txtUsuario.setEnabled(!procesando);
        txtPassword.setEnabled(!procesando);
        lblEstado.setText(procesando ? "Validando credenciales y rostro..." : " ");
    }

    private void mostrarError(Throwable causa) {
        txtPassword.setText("");
        txtPassword.requestFocusInWindow();
        if (causa instanceof BusinessException) {
            JOptionPane.showMessageDialog(this, causa.getMessage(),
                    "No se pudo iniciar sesión", JOptionPane.WARNING_MESSAGE);
        } else if (causa instanceof DataAccessException) {
            JOptionPane.showMessageDialog(this, causa.getMessage(),
                    "Error de conexión", JOptionPane.ERROR_MESSAGE);
        } else {
            if (causa != null) {
                causa.printStackTrace();
            }
            JOptionPane.showMessageDialog(this,
                    "Ocurrió un error inesperado. Intente nuevamente.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
