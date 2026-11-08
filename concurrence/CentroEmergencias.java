package concurrence;

import events.EmergenciaListener;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import models.Ambulancia;
import models.Emergencia;
import models.Equipamiento;
import models.Medico;

public class CentroEmergencias {
    private final PriorityBlockingQueue<Emergencia> colaEmergencias;
    private final ConcurrentHashMap<String, Ambulancia> ambulancias;
    private final ConcurrentHashMap<String, Medico> medicos;
    private final ConcurrentHashMap<String, Equipamiento> equipos;
    private final List<EmergenciaListener> listeners;
    
    private final ExecutorService operadoresPool;
    private final ExecutorService despachadoresPool;
    
    private final AtomicInteger emergenciasAtendidas;
    private final AtomicBoolean sistemaActivo;
    
    // Semáforos independientes para prevenir esperas activas
    private final Semaphore semaforoAmbulancias; 
    private final Semaphore semaforoMedicos;
    private final Semaphore semaforoEquipos;

    public CentroEmergencias(int numDespachadores) {
        this.colaEmergencias = new PriorityBlockingQueue<>();
        this.ambulancias = new ConcurrentHashMap<>();
        this.medicos = new ConcurrentHashMap<>();
        this.equipos = new ConcurrentHashMap<>();
        this.listeners = new CopyOnWriteArrayList<>(); 
        
        this.operadoresPool = Executors.newCachedThreadPool();
        this.despachadoresPool = Executors.newFixedThreadPool(numDespachadores);
        
        this.emergenciasAtendidas = new AtomicInteger(0);
        this.sistemaActivo = new AtomicBoolean(true);
        
        this.semaforoAmbulancias = new Semaphore(0); 
        this.semaforoMedicos = new Semaphore(0);
        this.semaforoEquipos = new Semaphore(0);
    }

    public void registrarAmbulancia(Ambulancia a) { 
        ambulancias.put(a.getId(), a); semaforoAmbulancias.release(); 
    }
    public void registrarMedico(Medico m) { 
        medicos.put(m.getId(), m); semaforoMedicos.release(); 
    }
    public void registrarEquipo(Equipamiento e) { 
        equipos.put(e.getId(), e); semaforoEquipos.release(); 
    }

    public void agregarListener(EmergenciaListener listener) { 
        listeners.add(listener); 
    }

    public void recibirLlamada(Emergencia emergencia) {
        if (!sistemaActivo.get()) return;
        operadoresPool.submit(() -> {
            colaEmergencias.put(emergencia);
            notificarRegistro(emergencia);
        });
    }

    public void iniciarDespacho() {
        int numDespachadores = ((ThreadPoolExecutor) despachadoresPool).getCorePoolSize();
        for (int i = 0; i < numDespachadores; i++) {
            despachadoresPool.submit(() -> {
                while (sistemaActivo.get() && !Thread.currentThread().isInterrupted()) {
                    try {
                        
                        procesarEmergencia();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt(); 
                    }
                }
            });
        }
    }

    private void procesarEmergencia() throws InterruptedException {
        // LOCK ORDERING: Siempre adquirir en el mismo orden previene Deadlocks
        semaforoAmbulancias.acquire();
        semaforoMedicos.acquire(); //solicita y consume un permiso del semaforo
        semaforoEquipos.acquire();

        //variables para guardar los recursos asignados
        Ambulancia ambulanciaAsignada = null;
        Medico medicoAsignado = null;
        Equipamiento equipoAsignado = null;
        
        try {
            Emergencia emergencia = colaEmergencias.take(); //optine emergencia de la cola

            for (Ambulancia amb : ambulancias.values()) { 
                if (amb.intentarAsignar()) { 
                    ambulanciaAsignada = amb; 
                    break; 
                } 
            }
            for (Medico med : medicos.values()) { 
                if (med.intentarAsignar()) { 
                    medicoAsignado = med; 
                    break; 
                } 
            }
            for (Equipamiento eq : equipos.values()) { 
                if (eq.intentarAsignar()) { 
                    equipoAsignado = eq; 
                    break;
                } 
            }

            //aqui se comprueba que se hallan asignado los tres recursos necesarios
            if (ambulanciaAsignada != null && medicoAsignado != null && equipoAsignado != null) {
                notificarAsignacion(emergencia, ambulanciaAsignada, medicoAsignado, equipoAsignado);
                
                // Simulación de atención agrupada
                System.out.println(" Unidad desplegada -> " + ambulanciaAsignada.getId() + " | " + medicoAsignado.getId() + " | " + equipoAsignado.getId() + " hacia " + emergencia.getId());
                long tiempoAtencion = (long) (Math.random() * 3000 + 2000); //simula el tiempo que se tarda la atencion de una emergencia
                Thread.sleep(tiempoAtencion);//hace que el hilo actual se detenga durante el tiempo calculado
                
                ambulanciaAsignada.registrarAtencion(emergencia);
                emergenciasAtendidas.incrementAndGet();
                notificarAtencion(emergencia, ambulanciaAsignada, medicoAsignado, equipoAsignado);
            }
        } finally { //El bloque finally contiene instrucciones que deben ejecutarse al finalizar el bloque try, incluso si ocurre una excepción.
            // Liberación estricta de todos los recursos y semáforos
            if (ambulanciaAsignada != null) ambulanciaAsignada.liberar();
            if (medicoAsignado != null) medicoAsignado.liberar();
            if (equipoAsignado != null) equipoAsignado.liberar();
            
            semaforoEquipos.release();
            semaforoMedicos.release();
            semaforoAmbulancias.release();
        }
    }

    public void detenerSistema() {
        sistemaActivo.set(false);
        operadoresPool.shutdownNow(); 
        despachadoresPool.shutdownNow();
    }

    public int getEmergenciasAtendidas() { 
        return emergenciasAtendidas.get(); 
    }
    
    private void notificarRegistro(Emergencia e) { 
        for (EmergenciaListener listener : listeners) listener.onEmergenciaRegistrada(e); 
    }
    
    private void notificarAsignacion(Emergencia e, Ambulancia a, Medico m, Equipamiento eq) { 
        for (EmergenciaListener listener : listeners) listener.onRecursosAsignados(e, a, m, eq); 
    }
    
    private void notificarAtencion(Emergencia e, Ambulancia a, Medico m, Equipamiento eq) { 
        for (EmergenciaListener listener : listeners) listener.onEmergenciaAtendida(e, a, m, eq); 
    }
}