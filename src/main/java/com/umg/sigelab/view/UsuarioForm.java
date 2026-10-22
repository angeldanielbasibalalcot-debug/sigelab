package com.umg.sigelab.view;

import com.umg.sigelab.controller.UsuarioController;
import com.umg.sigelab.exception.BusinessException;
import com.umg.sigelab.exception.DataAccessException;
import com.umg.sigelab.model.Rol;
import com.umg.sigelab.model.Usuario;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Formulario de gestion de usuarios (solo ADMIN), disenado con el GUI Designer: UsuarioForm.form.
 * Los campos de abajo deben llamarse EXACTAMENTE igual que el "field name" de cada componente
 * en el diseñador. Aqui solo hay logica de interfaz: las reglas viven en UsuarioService.
 */
public class UsuarioForm {

    private static final int COL_ESTADO = 6;

    // ---- Componentes del .form (field name en el diseñador) ----
    private JPanel panelPrincipal;
    private JButton btnSalir;
    private JTextField txtId;
    private JTextField txtUsuario;
    private JTextField txtNombre;
    private JTextField txtApellido;
    private JTextField txtCorreo;
    private JLabel lblPassword;
    private JPasswordField txtPassword;
    private JComboBox<Rol> cmbRol;
    private JCheckBox chkActivo;
    private JLabel lblEstadoRostro;
    private JButton btnNuevo;
    private JButton btnGuardar;
    private JButton btnActualizar;
    private JButton btnDesactivar;
    private JButton btnRegistrarRostro;
    private JButton btnEliminarRostro;
    private JTextField txtBuscar;
    private JButton btnBuscar;
    private JTable tblUsuarios;

    private final UsuarioController controller = new UsuarioController();
    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"ID", "Usuario", "Nombre", "Apellido", "Correo", "Rol", "Estado"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(modelo);

    private List<Usuario> usuarios = new ArrayList<>();
    private Usuario seleccionado; // null = se esta creando un usuario nuevo

    public UsuarioForm() {
        configurarTabla();

        txtId.setEditable(false);
        chkActivo.setEnabled(false);
        chkActivo.setToolTipText("El estado se cambia con el botón Desactivar / Reactivar.");
        // El reconocimiento facial se integra mas adelante (modulo facial).
        btnRegistrarRostro.setEnabled(false);
        btnEliminarRostro.setEnabled(false);
        btnRegistrarRostro.setToolTipText("Disponible cuando se integre el reconocimiento facial.");
        btnEliminarRostro.setToolTipText("Disponible cuando se integre el reconocimiento facial.");

        btnNuevo.addActionListener(e -> nuevo());
        btnGuardar.addActionListener(e -> guardar());
        btnActualizar.addActionListener(e -> actualizar());
        btnDesactivar.addActionListener(e -> alternarEstado());
        btnBuscar.addActionListener(e -> filtrar());
        txtBuscar.addActionListener(e -> filtrar());
        btnSalir.addActionListener(e -> salir());
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filtrar();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filtrar();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filtrar();
            }
        });

        ejecutar(() -> controller.listarRoles().forEach(cmbRol::addItem));
        cargarUsuarios();
        limpiarFormulario();
    }

    /** Panel que MainFrame muestra en su area de contenido. */
    public JPanel getPanelPrincipal() {
        return panelPrincipal;
    }

    // ------------------------------------------------------------------
    // Tabla
    // ------------------------------------------------------------------
    private void configurarTabla() {
        tblUsuarios.setModel(modelo);
        tblUsuarios.setRowSorter(sorter);
        tblUsuarios.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblUsuarios.getTableHeader().setReorderingAllowed(false);
        tblUsuarios.setRowHeight(24);
        tblUsuarios.getColumnModel().getColumn(0).setPreferredWidth(40);
        tblUsuarios.getColumnModel().getColumn(0).setMaxWidth(60);
        tblUsuarios.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object valor, boolean seleccionada,
                                                           boolean foco, int fila, int columna) {
                Component c = super.getTableCellRendererComponent(t, valor, seleccionada, foco, fila, columna);
                int filaModelo = t.convertRowIndexToModel(fila);
                boolean inactivo = "INACTIVO".equals(String.valueOf(modelo.getValueAt(filaModelo, COL_ESTADO)));
                if (!seleccionada) {
                    c.setForeground(inactivo ? Color.GRAY : t.getForeground());
                }
                return c;
            }
        });
        tblUsuarios.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                alSeleccionarFila();
            }
        });
    }

    private void cargarUsuarios() {
        ejecutar(() -> {
            usuarios = controller.listar();
            modelo.setRowCount(0);
            for (Usuario u : usuarios) {
                modelo.addRow(new Object[]{u.getId(), u.getUsername(), u.getNombre(), u.getApellido(),
                        u.getCorreo(), u.getRol().getNombre(), u.getEstado()});
            }
        });
    }

    private void filtrar() {
        String texto = txtBuscar.getText().trim();
        if (texto.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(texto)));
        }
    }

    private void alSeleccionarFila() {
        int fila = tblUsuarios.getSelectedRow();
        if (fila < 0) {
            return;
        }
        mostrarEnFormulario(usuarios.get(tblUsuarios.convertRowIndexToModel(fila)));
    }

    // ------------------------------------------------------------------
    // Formulario
    // ------------------------------------------------------------------
    private void mostrarEnFormulario(Usuario u) {
        seleccionado = u;
        txtId.setText(String.valueOf(u.getId()));
        txtUsuario.setText(u.getUsername());
        txtUsuario.setEditable(false);
        txtNombre.setText(u.getNombre());
        txtApellido.setText(u.getApellido());
        txtCorreo.setText(u.getCorreo());
        txtPassword.setText("");
        lblPassword.setText("Nueva contraseña (opcional)");
        for (int i = 0; i < cmbRol.getItemCount(); i++) {
            if (cmbRol.getItemAt(i).getId() == u.getRol().getId()) {
                cmbRol.setSelectedIndex(i);
                break;
            }
        }
        chkActivo.setSelected(u.estaActivo());
        btnDesactivar.setText(u.estaActivo() ? "Desactivar" : "Reactivar");
        btnDesactivar.setEnabled(true);
        lblEstadoRostro.setText("Reconocimiento facial: pendiente de integrar.");
    }

    private void nuevo() {
        tblUsuarios.clearSelection();
        limpiarFormulario();
    }

    private void limpiarFormulario() {
        seleccionado = null;
        txtId.setText("");
        txtUsuario.setText("");
        txtUsuario.setEditable(true);
        txtNombre.setText("");
        txtApellido.setText("");
        txtCorreo.setText("");
        txtPassword.setText("");
        lblPassword.setText("Contraseña");
        cmbRol.setSelectedIndex(-1);
        chkActivo.setSelected(false);
        btnDesactivar.setText("Desactivar");
        btnDesactivar.setEnabled(false);
        lblEstadoRostro.setText("Seleccione un usuario.");
        txtUsuario.requestFocusInWindow();
    }

    private Usuario leerFormulario() {
        Usuario datos = new Usuario();
        datos.setUsername(txtUsuario.getText());
        datos.setNombre(txtNombre.getText());
        datos.setApellido(txtApellido.getText());
        datos.setCorreo(txtCorreo.getText());
        datos.setRol((Rol) cmbRol.getSelectedItem());
        return datos;
    }

    private void recargar() {
        tblUsuarios.clearSelection();
        cargarUsuarios();
        limpiarFormulario();
    }

    // ------------------------------------------------------------------
    // Acciones de los botones
    // ------------------------------------------------------------------
    /** Guardar = crear un usuario nuevo. */
    private void guardar() {
        if (seleccionado != null) {
            advertir("Hay un usuario seleccionado. Use «Actualizar» para modificarlo "
                    + "o «Nuevo» para crear otro.");
            return;
        }
        final Usuario datos = leerFormulario();
        final String password = new String(txtPassword.getPassword());
        ejecutar(() -> {
            controller.crear(datos, password);
            informar("Usuario creado correctamente.");
            recargar();
        });
    }

    /** Actualizar = modificar el usuario seleccionado en la tabla. */
    private void actualizar() {
        if (seleccionado == null) {
            advertir("Seleccione un usuario de la tabla para actualizarlo.");
            return;
        }
        final Usuario datos = leerFormulario();
        datos.setId(seleccionado.getId());
        final String password = new String(txtPassword.getPassword());
        ejecutar(() -> {
            controller.actualizar(datos, password);
            informar("Usuario actualizado correctamente.");
            recargar();
        });
    }

    private void alternarEstado() {
        if (seleccionado == null) {
            advertir("Seleccione un usuario de la tabla.");
            return;
        }
        final Usuario u = seleccionado;
        ejecutar(() -> {
            if (u.estaActivo()) {
                int respuesta = JOptionPane.showConfirmDialog(panelPrincipal,
                        "¿Desactivar al usuario " + u.getUsername() + "?\n"
                                + "No podrá iniciar sesión, pero su historial se conserva.",
                        "Confirmar desactivación", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (respuesta != JOptionPane.YES_OPTION) {
                    return;
                }
                controller.desactivar(u.getId());
                informar("Usuario desactivado correctamente.");
            } else {
                controller.reactivar(u.getId());
                informar("Usuario reactivado correctamente.");
            }
            recargar();
        });
    }

    /** Cierra el modulo y deja el area de contenido lista para elegir otra opcion del menu. */
    private void salir() {
        Container padre = panelPrincipal.getParent();
        if (padre == null) {
            return;
        }
        padre.removeAll();
        padre.add(new JLabel("Seleccione una opción del menú.", SwingConstants.CENTER), BorderLayout.CENTER);
        padre.revalidate();
        padre.repaint();
    }

    // ------------------------------------------------------------------
    // Mensajes y manejo de errores
    // ------------------------------------------------------------------
    private void informar(String mensaje) {
        JOptionPane.showMessageDialog(panelPrincipal, mensaje, "Usuarios", JOptionPane.INFORMATION_MESSAGE);
    }

    private void advertir(String mensaje) {
        JOptionPane.showMessageDialog(panelPrincipal, mensaje, "Atención", JOptionPane.WARNING_MESSAGE);
    }

    /** Ejecuta una accion y convierte las excepciones en mensajes comprensibles. */
    private void ejecutar(Runnable accion) {
        try {
            accion.run();
        } catch (BusinessException e) {
            JOptionPane.showMessageDialog(panelPrincipal, e.getMessage(),
                    "Revise los datos", JOptionPane.WARNING_MESSAGE);
        } catch (DataAccessException e) {
            JOptionPane.showMessageDialog(panelPrincipal, e.getMessage(),
                    "Error de datos", JOptionPane.ERROR_MESSAGE);
        } catch (RuntimeException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(panelPrincipal, "Ocurrió un error inesperado. Intente nuevamente.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        panelPrincipal = new JPanel();
        panelPrincipal.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(12, 7, new Insets(8, 8, 8, 8), -1, -1));

        final JLabel label1 = new JLabel();
        label1.setText("SIGELAB");
        panelPrincipal.add(label1, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 6, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));

        btnSalir = new JButton();
        btnSalir.setText("SALIR");
        panelPrincipal.add(btnSalir, new com.intellij.uiDesigner.core.GridConstraints(0, 6, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));

        txtId = new JTextField();
        panelPrincipal.add(txtId, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 1, 5, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));

        txtNombre = new JTextField();
        panelPrincipal.add(txtNombre, new com.intellij.uiDesigner.core.GridConstraints(3, 2, 1, 5, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));

        cmbRol = new JComboBox<>();
        panelPrincipal.add(cmbRol, new com.intellij.uiDesigner.core.GridConstraints(7, 2, 1, 5, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));

        final JLabel label2 = new JLabel();
        label2.setText("ID");
        panelPrincipal.add(label2, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));

        final JLabel label3 = new JLabel();
        label3.setText("Correo");
        panelPrincipal.add(label3, new com.intellij.uiDesigner.core.GridConstraints(5, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));

        lblPassword = new JLabel();
        lblPassword.setText("Contraseña");
        panelPrincipal.add(lblPassword, new com.intellij.uiDesigner.core.GridConstraints(6, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));

        final JLabel label4 = new JLabel();
        label4.setText("Rol");
        panelPrincipal.add(label4, new com.intellij.uiDesigner.core.GridConstraints(7, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));

        final JLabel label5 = new JLabel();
        label5.setText("Activo");
        panelPrincipal.add(label5, new com.intellij.uiDesigner.core.GridConstraints(8, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));

        chkActivo = new JCheckBox();
        panelPrincipal.add(chkActivo, new com.intellij.uiDesigner.core.GridConstraints(8, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));

        lblEstadoRostro = new JLabel();
        lblEstadoRostro.setText("Seleccione un usuario.");
        panelPrincipal.add(lblEstadoRostro, new com.intellij.uiDesigner.core.GridConstraints(8, 3, 1, 4, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));

        btnNuevo = new JButton();
        btnNuevo.setText("Nuevo");
        panelPrincipal.add(btnNuevo, new com.intellij.uiDesigner.core.GridConstraints(9, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));

        btnGuardar = new JButton();
        btnGuardar.setText("Guardar");
        panelPrincipal.add(btnGuardar, new com.intellij.uiDesigner.core.GridConstraints(9, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));

        btnActualizar = new JButton();
        btnActualizar.setText("Actualizar");
        panelPrincipal.add(btnActualizar, new com.intellij.uiDesigner.core.GridConstraints(9, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));

        btnDesactivar = new JButton();
        btnDesactivar.setText("Desactivar");
        panelPrincipal.add(btnDesactivar, new com.intellij.uiDesigner.core.GridConstraints(9, 4, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));

        btnRegistrarRostro = new JButton();
        btnRegistrarRostro.setText("Registrar rostro");
        panelPrincipal.add(btnRegistrarRostro, new com.intellij.uiDesigner.core.GridConstraints(9, 5, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));

        btnEliminarRostro = new JButton();
        btnEliminarRostro.setText("Eliminar rostro");
        panelPrincipal.add(btnEliminarRostro, new com.intellij.uiDesigner.core.GridConstraints(9, 6, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));

        final JLabel label6 = new JLabel();
        label6.setText("Buscar usuario");
        panelPrincipal.add(label6, new com.intellij.uiDesigner.core.GridConstraints(10, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));

        txtBuscar = new JTextField();
        panelPrincipal.add(txtBuscar, new com.intellij.uiDesigner.core.GridConstraints(10, 2, 1, 4, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));

        btnBuscar = new JButton();
        btnBuscar.setText("Buscar");
        panelPrincipal.add(btnBuscar, new com.intellij.uiDesigner.core.GridConstraints(10, 6, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));

        final JScrollPane scrollPane1 = new JScrollPane();
        panelPrincipal.add(scrollPane1, new com.intellij.uiDesigner.core.GridConstraints(11, 0, 1, 7, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tblUsuarios = new JTable();
        scrollPane1.setViewportView(tblUsuarios);

        final JLabel label7 = new JLabel();
        label7.setText("Usuario");
        panelPrincipal.add(label7, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));

        final JLabel label8 = new JLabel();
        label8.setText("Nombre");
        panelPrincipal.add(label8, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));

        final JLabel label9 = new JLabel();
        label9.setText("Apellido");
        panelPrincipal.add(label9, new com.intellij.uiDesigner.core.GridConstraints(4, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));

        txtUsuario = new JTextField();
        panelPrincipal.add(txtUsuario, new com.intellij.uiDesigner.core.GridConstraints(2, 2, 1, 5, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));

        txtApellido = new JTextField();
        panelPrincipal.add(txtApellido, new com.intellij.uiDesigner.core.GridConstraints(4, 2, 1, 5, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));

        txtPassword = new JPasswordField();
        panelPrincipal.add(txtPassword, new com.intellij.uiDesigner.core.GridConstraints(6, 2, 1, 5, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));

        txtCorreo = new JTextField();
        panelPrincipal.add(txtCorreo, new com.intellij.uiDesigner.core.GridConstraints(5, 2, 1, 5, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }
}
