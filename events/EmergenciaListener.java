package events;

import models.Ambulancia;
import models.Emergencia;
import models.Equipamiento;
import models.Medico;

/**
 * Interfaz para el patrón Observer.
 * Permite que los subsistemas (como el monitor de consola) escuchen los 
 * eventos que ocurren en el Centro de Emergencias sin acoplar el código directamente
 */
public interface EmergenciaListener {
    
    void onEmergenciaRegistrada(Emergencia emergencia);
    
    void onRecursosAsignados(Emergencia emergencia, Ambulancia ambulancia, Medico medico, Equipamiento equipo);
    
    void onEmergenciaAtendida(Emergencia emergencia, Ambulancia ambulancia, Medico medico, Equipamiento equipo);
    
}
