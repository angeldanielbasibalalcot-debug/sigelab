
    package com.umg.sigelab.view;

import java.util.Scanner;

    public class PrestamoView {

        private Scanner sc = new Scanner(System.in);

        public void menuPrestamos() {

            int opcion;

            do {

                System.out.println("\n===== PRESTAMOS =====");
                System.out.println("1. Registrar Prestamo");
                System.out.println("2. Registrar Devolucion");
                System.out.println("3. Salir");

                opcion = sc.nextInt();

                switch (opcion) {

                    case 1:
                        System.out.println("Registrar Prestamo");
                        break;

                    case 2:
                        System.out.println("Registrar Devolucion");
                        break;

                    case 3:
                        System.out.println("Saliendo...");
                        break;

                    default:
                        System.out.println("Opcion invalida");
                }

            } while(opcion != 3);
        }
    }



