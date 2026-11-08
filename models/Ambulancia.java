package models;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class Ambulancia {
    private final String id;
    private final AtomicBoolean disponible; 
    private final CopyOnWriteArrayList<Emergencia> historialAtenciones;

    public Ambulancia(String id) {
        this.id = id;
        this.disponible = new AtomicBoolean(true);
        this.historialAtenciones = new CopyOnWriteArrayList<>();
    }

    public String getId() {
        return id;
    }

    public boolean isDisponible() {
        return disponible.get();
    }

    public List<Emergencia> getHistorialAtenciones() {
        return historialAtenciones;
    }

    public boolean intentarAsignar() {
        return disponible.compareAndSet(true, false);
    }

    public void liberar() {
        disponible.set(true);
    }

    public void registrarAtencion(Emergencia emergencia) {
        historialAtenciones.add(emergencia);
    }
}