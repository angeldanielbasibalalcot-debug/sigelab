
    package com.umg.sigelab.model;

import java.util.Date;

    public class Prestamo {

        private int idPrestamo;
        private int idUsuario;
        private Date fechaPrestamo;
        private EstadoPrestamo  estado;

        public Prestamo() {
        }

        public Prestamo(int idUsuario) {
            this.idUsuario = idUsuario;
        }

        public int getIdPrestamo() {
            return idPrestamo;
        }

        public void setIdPrestamo(int idPrestamo) {
            this.idPrestamo = idPrestamo;
        }

        public int getIdUsuario() {
            return idUsuario;
        }

        public void setIdUsuario(int idUsuario) {
            this.idUsuario = idUsuario;
        }

        public Date getFechaPrestamo() {
            return fechaPrestamo;
        }

        public void setFechaPrestamo(Date fechaPrestamo) {
            this.fechaPrestamo = fechaPrestamo;
        }

        public EstadoPrestamo  getEstado() {
            return estado;
        }

        public void setEstado(EstadoPrestamo  estado) {
            this.estado = estado;
        }
    }



