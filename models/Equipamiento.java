package models;

import java.util.concurrent.atomic.AtomicBoolean;

public class Equipamiento {
    private final String id;
    private final AtomicBoolean disponible;

    public Equipamiento(String id) {
        this.id = id;
        this.disponible = new AtomicBoolean(true);
    }

    public String getId() { 
        return id; 
    }
    public boolean isDisponible() { 
        return disponible.get(); 
    }
    public boolean intentarAsignar() { 
        return disponible.compareAndSet(true, false); 
    }
    public void liberar() {
         disponible.set(true);    
    }
}