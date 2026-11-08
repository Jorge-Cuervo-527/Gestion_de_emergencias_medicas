# Sistema de Gestión de Emergencias Médicas 

Sistema concurrente desarrollado en Java para coordinar la asignación de recursos médicos en tiempo real, aplicando priorización matemática, colecciones thread-safe y control de accesos basado en roles (RBAC).

## Requisitos Previos
* Java Development Kit (JDK) 8 o superior.
* IDE recomendado: Visual Studio Code, Eclipse, o IntelliJ IDEA.

## Instrucciones de Ejecución
1. Clonar el repositorio: `git clone [URL_DEL_REPO]`
2. Abrir la carpeta del proyecto en el IDE.
3. Asegurar que la estructura de paquetes sea: `concurrence`, `events`, `main`, `models`, y `security`.
4. Compilar y ejecutar la clase principal ubicada en `main.Simulador.java`.

## Credenciales de Acceso (RBAC)
Para probar el sistema, utilice las siguientes credenciales en el inicio de la simulación:
* **Administrador (Acceso Total):** Usuario: `jorge_admin` | Password: `admin123`
* **Operador (Acceso Restringido):** Usuario: `operador_1` | Password: `op123`