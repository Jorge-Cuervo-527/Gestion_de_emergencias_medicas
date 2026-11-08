package security;

import java.security.SecureRandom; //genera numeros aleatorios
import java.time.Instant;
import java.util.Base64; //convierte datos binario en texto
import java.util.concurrent.ConcurrentHashMap; //para almacenar informacion de forma segura cuando varios hilos acceden
import models.Emergencia;

public class ControlAccesos {
    private final ConcurrentHashMap<String, String> credenciales; //almacenar credenciales
    private final ConcurrentHashMap<String, String> roles; //los permisos generales de cada usuario
    private final ConcurrentHashMap<String, String> secretosTotp; // codigos del segundo factor
    private final ConcurrentHashMap<String, String> sectores; // Para la autorización por objeto
    
    // Gestión de sesiones segura 
    private final ConcurrentHashMap<String, Sesion> sesionesActivas;
    private static final long TIEMPO_INACTIVIDAD_MAX = 15 * 60; // 15 minutos en segundos

    public ControlAccesos() {
        this.credenciales = new ConcurrentHashMap<>();
        this.roles = new ConcurrentHashMap<>();
        this.secretosTotp = new ConcurrentHashMap<>();
        this.sectores = new ConcurrentHashMap<>();
        this.sesionesActivas = new ConcurrentHashMap<>();

        // registro de dos usuarios de prueba
        credenciales.put("jorge_admin", "admin123");
        roles.put("jorge_admin", "ADMINISTRADOR");
        secretosTotp.put("jorge_admin", "123456"); // Simulación del código TOTP
        sectores.put("jorge_admin", "TODOS");

        credenciales.put("operador_1", "op123");
        roles.put("operador_1", "OPERADOR");
        secretosTotp.put("operador_1", "654321");
        sectores.put("operador_1", "NORTE");
    }

    /**
     * 1. Autenticación Múltiple (Estilo JAAS): Valida Clave + TOTP y genera Token de Sesión
     */
    public String autenticar(String usuario, String password, String codigoTotp) {
        
        if (!credenciales.containsKey(usuario) || !credenciales.get(usuario).equals(password)) {
            System.err.println(" [SEGURIDAD] Error de autenticación: Credenciales inválidas.");
            return null;
        }

        // Módulo TOTP (Segundo factor)
        if (!secretosTotp.get(usuario).equals(codigoTotp)) {
            System.err.println(" [SEGURIDAD] Error de autenticación: Código TOTP inválido.");
            return null;
        }

        // Regenerar ID de sesión (token)seguro de 256 bits
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32]; // 256 bits
        random.nextBytes(bytes);
        String tokenSesion = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        // Guardamos la sesión con timestamp para caducidad
        sesionesActivas.put(tokenSesion, new Sesion(usuario, Instant.now().getEpochSecond()));
        
        System.out.println(" [SEGURIDAD] Autenticación 2FA exitosa. Bienvenido, " + usuario + ".");
        return tokenSesion;
    }

    /**
     * 2. Autorización por Rol (Capa 1)
     */
    public boolean autorizarPorRol(String tokenSesion, String rolRequerido) {
        String usuario = validarSesion(tokenSesion);
        if (usuario == null) return false;
        
        String rolUsuario = roles.get(usuario);
        if (rolUsuario != null && rolUsuario.equals(rolRequerido)) {
            return true; //Autoriza el acceso
        }
        
        System.err.println(" [SEGURIDAD] Acceso denegado: El usuario no cuenta con el rol " + rolRequerido + ".");
        return false; 
    }

    /**
     * 3. Autorización por Objeto (Capa 2): Valida si el usuario puede operar sobre ESA emergencia
     */
    public boolean autorizarPorObjeto(String tokenSesion, Emergencia emergencia) {
         String usuario = validarSesion(tokenSesion);
         if (usuario == null) return false;

         String sectorUsuario = sectores.get(usuario); //busca en sectores a ver cual es el sector qeu tiene asociado ese usuario
         
         // El admin puede todo, el operador solo si la emergencia (ID hipotético) contiene su sector
         if (sectorUsuario.equals("TODOS") || emergencia.getId().contains(sectorUsuario)) {
             return true;
         }

         System.err.println(" [SEGURIDAD] Acceso denegado (IDOR): No es titular del sector de la emergencia " + emergencia.getId() + ".");
         return false;
    }

    private String validarSesion(String tokenSesion) {
        if (tokenSesion == null || !sesionesActivas.containsKey(tokenSesion)) {
            System.err.println(" [SEGURIDAD] Acceso denegado: Token inválido o inexistente.");
            return null;
        }

        Sesion sesion = sesionesActivas.get(tokenSesion);
    long ahora = Instant.now().getEpochSecond();

        // Verificamos inactividad
        if (ahora - sesion.ultimoUso > TIEMPO_INACTIVIDAD_MAX) {
            sesionesActivas.remove(tokenSesion);
            System.err.println(" [SEGURIDAD] Sesión caducada por inactividad.");
            return null;
        }

        sesion.ultimoUso = ahora; // Renovamos uso
        return sesion.usuario;
    }
    
    public void cerrarSesion(String tokenSesion) {
        if (tokenSesion != null && sesionesActivas.remove(tokenSesion) != null) {
            System.out.println(" [SEGURIDAD] Sesión destruida de forma segura del servidor.");
        }
    }

    // Clase interna para gestionar el estado de la sesión
    private class Sesion {
        String usuario;
        long ultimoUso;

        Sesion(String usuario, long ultimoUso) {
            this.usuario = usuario;
            this.ultimoUso = ultimoUso;
        }
    }
}