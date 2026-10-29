# Notia

Proyecto de la materia **Desarrollo de Aplicaciones Móviles** — Universidad del Valle, Sede Tuluá. Programa de Ingeniería de Sistemas.

## ¿Qué es Notia?

Notia es una aplicación móvil Android para la gestión de notas académicas. Conecta a estudiantes, docentes y coordinadores dentro de una misma organización (universidad, academia o curso), y el rol de cada usuario cambia según la organización en la que esté.

- **Estudiantes**: consultan sus materias y notas, ven su promedio y usan un simulador que les dice cuánto necesitan en las evaluaciones pendientes para alcanzar una meta.
- **Docentes**: registran notas por actividad, guardan borradores y las publican para que los estudiantes reciban una notificación.
- **Coordinación**: administra la organización, sus miembros, materias y periodos académicos, e invita usuarios mediante un código.

## Funcionalidades principales

| Módulo | Descripción |
|---|---|
| Cuenta de usuario | Registro, inicio y cierre de sesión, y recuperación de contraseña por correo |
| Organizaciones y roles | Crear organizaciones, unirse con código de invitación y gestión de roles |
| Estudiante | Consulta de materias y notas, simulador de nota final |
| Docente | Registro, borrador y publicación de notas |
| Coordinación | Panel de miembros, materias y periodos |

## Tecnologías

- **Lenguaje:** Kotlin
- **Interfaz:** Jetpack Compose + Material 3
- **Navegación:** Navigation Compose
- **Plataforma:** Android (minSdk 26, targetSdk 37)
- **Diseño:** Figma, con una guía de estilo propia (paleta índigo/caléndula y tipografía Inter)

## Estructura del proyecto

```text
app/src/main/java/co/edu/univalle/notia/
├── MainActivity.kt          Punto de entrada
├── core/
│   ├── navigation/          Navegación global (NotiaNavHost)
│   └── ui/                  Componentes reutilizables
├── features/
│   ├── auth/                Ingreso, registro y recuperación de contraseña
│   ├── organizations/       Organizaciones y creación de organización
│   ├── student/             Funcionalidades del estudiante
│   ├── teacher/             Funcionalidades del docente
│   └── coordinator/         Funcionalidades de coordinación
└── ui/theme/                Colores, tipografía y tema de Notia
```

## Cómo ejecutar

1. Clonar el repositorio:
   ```bash
   git clone <url-del-repositorio>
   ```
2. Abrir la carpeta del proyecto en **Android Studio**.
3. Esperar a que termine la sincronización de Gradle.
4. Seleccionar un emulador o un dispositivo físico y presionar **Run ▶**.

También se puede compilar desde la terminal:

```bash
./gradlew :app:assembleDebug
```

## Integrantes

| Nombre | Código |
|---|---|
| Santiago Velásquez Bedoya | 2380378 |
| Gustavo Adolfo Restrepo Muñoz | 2380618 |
| Santiago Hernández Aguado | 2380367 |
| Nicolás Chaparro Gonzales | 2380530 |