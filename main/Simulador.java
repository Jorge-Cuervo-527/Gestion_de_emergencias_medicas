package main;

import concurrence.CentroEmergencias;
import events.EmergenciaListener;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import models.Ambulancia;
import models.Emergencia;
import models.Equipamiento;
import models.Medico;
import security.ControlAccesos;

public class Simulador {
    private static final Logger LOGGER = Logger.getLogger(Simulador.class.getName());

    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("  SISTEMA DE GESTIÓN DE EMERGENCIAS");
        System.out.println("==========================================");
        System.out.println("--- INICIANDO PROTOCOLO DE SEGURIDAD (2FA) ---");
        
        ControlAccesos seguridad = new ControlAccesos();
        String tokenActivo = null;

        // Implementación de try-with-resources para el manejo seguro de entradas del sistema
        try (Scanner scanner = new Scanner(System.in)) {
            while (tokenActivo == null) {
                System.out.print("\n Ingrese su usuario: ");
                String usuario = scanner.nextLine();
                
                System.out.print(" Ingrese su contraseña: ");
                String password = scanner.nextLine();

                System.out.print(" Ingrese su código TOTP (6 dígitos): ");
                String totp = scanner.nextLine();

                // 1. Autenticación Multifactor, envia las credenciales al objeto seguridad para verificar la identidad del usuario
                tokenActivo = seguridad.autenticar(usuario, password, totp);

                if (tokenActivo != null) { //significa que la autenticación fue exitosa 
                    // 2. Autorización por Rol
                    if (seguridad.autorizarPorRol(tokenActivo, "ADMINISTRADOR")) {
                        System.out.println("\n [ACCESO CONCEDIDO] Levantando servicios concurrentes...\n");
                        ejecutarSistema(seguridad, tokenActivo);
                    } else {
                        seguridad.cerrarSesion(tokenActivo);
                        tokenActivo = null; 
                    }
                } else {
                    System.out.println(" Por favor, intente nuevamente.");
                }
            }
        } // El scanner se cierra automáticamente al salir de este bloque, eliminando la necesidad de scanner.close()
        
        seguridad.cerrarSesion(tokenActivo);
    }

    private static void ejecutarSistema(ControlAccesos seguridad, String token) {
        CentroEmergencias centro = new CentroEmergencias(3);

        centro.agregarListener(new EmergenciaListener() {
            @Override
            public void onEmergenciaRegistrada(Emergencia emergencia) {
                String prioridad = traducirGravedad(emergencia.getNivelGravedad());
                System.out.println(" [LLAMADA] " + emergencia.getId() + " registrada. [Prioridad: " + prioridad + "]");
            }

            @Override
            public void onRecursosAsignados(Emergencia emergencia, Ambulancia ambulancia, Medico medico, Equipamiento equipo) {
                if(seguridad.autorizarPorObjeto(token, emergencia)){
                     String prioridad = traducirGravedad(emergencia.getNivelGravedad());
                     System.out.println(" [DESPACHO] Asignando [" + ambulancia.getId() + ", " + medico.getId() + ", " + equipo.getId() + "] a " + emergencia.getId() + " [Atendiendo caso " + prioridad + "]");
                }
            }

            @Override
            public void onEmergenciaAtendida(Emergencia emergencia, Ambulancia ambulancia, Medico medico, Equipamiento equipo) {
                System.out.println(" [REPORTE] " + emergencia.getId() + " resuelta exitosamente por el equipo " + medico.getId() + ".");
            }
        });

        //se crean y se registran los tres recursos
        for (int i = 1; i <= 3; i++) {
            centro.registrarAmbulancia(new Ambulancia("AMB-" + i));
            centro.registrarMedico(new Medico("MED-" + i));
            centro.registrarEquipo(new Equipamiento("EQP-" + i));
        }

        centro.iniciarDespacho(); //se activan los despachadores para comenzar a atender emergencias

        String[] ids = {"E-001", "E-002", "E-003", "E-004", "E-005", "E-006", "E-007"};
        int[] gravedades = {1, 2, 2, 1, 3, 4, 3,}; 
        double[] distancias = {5.2, 1.5, 3.0, 8.4, 2.1, 4.0, 7.5};

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1); //crea un progamador de tareas con un solo hilo

        for (int i = 0; i < ids.length; i++) {
            final int index = i;
            scheduler.schedule(() -> {
                Emergencia em = new Emergencia(ids[index], gravedades[index], distancias[index]);
                centro.recibirLlamada(em);
            }, 300L * i, TimeUnit.MILLISECONDS);
        }
        scheduler.shutdown(); 

        try {
            Thread.sleep(12000); //pausa el hilo principal por 12 segundos
            centro.detenerSistema();//solicita detener el sistema 
            System.out.println("\n Sistema detenido de forma segura.");
            System.out.println(" Total de emergencias atendidas: " + centro.getEmergenciasAtendidas());
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "La simulación principal fue interrumpida", e);
            Thread.currentThread().interrupt();
        }
    }
    
   // Función para traducir los números a texto usando Switch Expressions (Java 14+)
   private static String traducirGravedad(int nivel) {
    return switch (nivel) {
        case 4 -> "CRÍTICO";
        case 3 -> "GRAVE";
        case 2 -> "MODERADO";
        case 1 -> "LEVE";
        default -> "DESCONOCIDO";
    };
}
}