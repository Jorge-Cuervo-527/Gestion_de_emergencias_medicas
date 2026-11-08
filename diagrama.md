```mermaid
classDiagram
    class Simulador {
        -Logger LOGGER
        +main(String[] args)
        -ejecutarSistema(ControlAccesos, String)
        -traducirGravedad(int)
    }

    class ControlAccesos {
        -ConcurrentHashMap credenciales
        -ConcurrentHashMap roles
        -ConcurrentHashMap secretosTotp
        -ConcurrentHashMap sectores
        -ConcurrentHashMap sesionesActivas
        +autenticar(String, String, String) String
        +autorizarPorRol(String, String) boolean
        +autorizarPorObjeto(String, Emergencia) boolean
        -validarSesion(String) String
        +cerrarSesion(String)
    }

    class Sesion {
        <<Inner Class>>
        +String usuario
        +long ultimoUso
    }

    class CentroEmergencias {
        -PriorityBlockingQueue colaEmergencias
        -ConcurrentHashMap ambulancias
        -ConcurrentHashMap medicos
        -ConcurrentHashMap equipos
        -List listeners
        -ExecutorService operadoresPool
        -ExecutorService despachadoresPool
        -Semaphore semaforoAmbulancias
        -Semaphore semaforoMedicos
        -Semaphore semaforoEquipos
        +registrarAmbulancia(Ambulancia)
        +registrarMedico(Medico)
        +registrarEquipo(Equipamiento)
        +agregarListener(EmergenciaListener)
        +recibirLlamada(Emergencia)
        +iniciarDespacho()
        -procesarEmergencia()
        +detenerSistema()
    }

    class Emergencia {
        -String id
        -int nivelGravedad
        -double distancia
        -long tiempoIngreso
        +getId() String
        +getNivelGravedad() int
        +compareTo(Emergencia) int
    }

    class Ambulancia {
        -String id
        -AtomicBoolean disponible
        -CopyOnWriteArrayList historialAtenciones
        +intentarAsignar() boolean
        +liberar()
        +registrarAtencion(Emergencia)
    }

    class Medico {
        -String id
        -AtomicBoolean disponible
        +intentarAsignar() boolean
        +liberar()
    }

    class Equipamiento {
        -String id
        -AtomicBoolean disponible
        +intentarAsignar() boolean
        +liberar()
    }

    class EmergenciaListener {
        <<Interface>>
        +onEmergenciaRegistrada(Emergencia)
        +onRecursosAsignados(Emergencia, Ambulancia, Medico, Equipamiento)
        +onEmergenciaAtendida(Emergencia, Ambulancia, Medico, Equipamiento)
    }

    %% Relaciones Arquitectónicas
    Simulador ..> ControlAccesos : Orquesta seguridad
    Simulador ..> CentroEmergencias : Inicializa motor
    Simulador ..> EmergenciaListener : Implementa anonimamente
    
    ControlAccesos *-- Sesion : Contiene
    
    CentroEmergencias o-- Ambulancia : Agrupa (Lock Ordering)
    CentroEmergencias o-- Medico : Agrupa (Lock Ordering)
    CentroEmergencias o-- Equipamiento : Agrupa (Lock Ordering)
    CentroEmergencias o-- Emergencia : Encola (PriorityBlockingQueue)
    CentroEmergencias --> EmergenciaListener : Notifica eventos
    
    EmergenciaListener ..> Emergencia : Depende