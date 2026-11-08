package models;

public class Emergencia implements Comparable<Emergencia> {
    private final String id;
    private final int nivelGravedad; // 4 = Crítico, 3 = Grave, 2 = Moderado, 1 = Leve
    private final double distancia;  // Distancia en kilómetros o unidades relativas
    private final long tiempoIngreso;

    public Emergencia(String id, int nivelGravedad, double distancia) {
        this.id = id;
        this.nivelGravedad = nivelGravedad;
        this.distancia = distancia;
        this.tiempoIngreso = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public int getNivelGravedad() {
        return nivelGravedad;
    }

    public double getDistancia() {
        return distancia;
    }

    public long getTiempoIngreso() {
        return tiempoIngreso;
    }

    @Override
    public int compareTo(Emergencia otra) {
        // 1. Priorizar por gravedad (Mayor nivel de gravedad va primero)
        int cmpGravedad = Integer.compare(this.nivelGravedad * -1, otra.nivelGravedad * -1);
        if (cmpGravedad != 0) {
            return cmpGravedad;
        }
        
        // 2. si hay igual gravedad, priorizar tiempo de espera (El más antiguo va primero)
        int cmpTiempo = Long.compare(this.tiempoIngreso, otra.tiempoIngreso);
        if (cmpTiempo != 0) {
            return cmpTiempo;
        }
        
        // 3. A igual gravedad y tiempo, priorizar ubicación (El más cercano va primero)
        return Double.compare(this.distancia, otra.distancia);
    }

    @Override
    public String toString() {
        return String.format("[ID: %s | Gravedad: %d | Distancia: %.1f]", id, nivelGravedad, distancia);
    }
}