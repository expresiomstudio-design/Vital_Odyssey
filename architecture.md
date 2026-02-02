# Vital Odyssey - Documento de Arquitectura y Contexto

## 1. Visión General del Proyecto
**Vital Odyssey** es una aplicación nativa para Android que gamifica la creación de hábitos y el bienestar digital mediante un sistema RPG (Juego de Rol). Las estadísticas del usuario en el juego (Salud, Ataque, Estamina) están directamente ligadas a datos biométricos del mundo real (sueño, pasos), la finalización de tareas diarias y la gestión del tiempo de pantalla. El objetivo es derrotar una campaña finita de Jefes a lo largo de varios meses.

## 2. Fases de Desarrollo (Roadmap Modular)

### Fase 1: El Núcleo del Guerrero (MVP)
**Objetivo:** Motor base de hábitos, persistencia local y bucle de combate manual.
* **Mecánica Base:** La Vida (Defensa) y la Estamina son fijas o dependen exclusivamente del ingreso manual de hábitos y rachas.
* **Implementación:** * Configuración de Jetpack DataStore para preferencias del usuario (Nivel, XP, HP, Hora de corte).
    * Base de datos local con Room (Entidad `Habit`).
    * Sistema de Combate (Cálculo de Daño y Desmayo).
    * Interfaz UI con Jetpack Compose (Dashboard y Gestión de Hábitos).

### Fase 2: El Escudo Físico (Módulo de Salud)
**Objetivo:** Integración de Google Health Connect.
* **Mecánica Base:** Los pasos diarios y las horas de sueño nutren la **Defensa** del jugador.
* **Implementación:**
    * Permisos y conexión con Health Connect.
    * Lectura en background/foreground de pasos y sueño.
    * Sustitución de la defensa fija por defensa variable (+ multiplicadores automáticos).

### Fase 3: La Estamina Mental (Módulo de Bienestar Digital)
**Objetivo:** Integración de `UsageStatsManager`.
* **Mecánica Base:** Respetar los límites de uso de aplicaciones dopamínicas (ej. Instagram, TikTok) nutre la **Estamina**, permitiendo golpes críticos o habilidades especiales.
* **Implementación:**
    * Monitoreo de tiempo de uso de apps en segundo plano.
    * Sistema de recompensas o penalizaciones de estamina según el cumplimiento del límite.

## 3. Stack Tecnológico y Librerías Principales
- **Plataforma**: Android Nativo (Min SDK 28 / Target SDK 34+) 
- **Lenguaje**: Kotlin (Versión 2.2.10+)
- **UI Toolkit**: Jetpack Compose (Material Design 3)
- **Arquitectura**: MVVM (Model-View-ViewModel) + Principios de Clean Architecture
- **Programación Asíncrona**: Kotlin Coroutines & Flow (StateFlow/SharedFlow)
- **Inyección de Dependencias**: Koin (Priorizado por su ligereza y rapidez de configuración)
- **Persistencia Local**: Room Database y Jetpack DataStore
- **Persistencia Remota y Auth**: Firebase (Firestore, Authentication)
- **APIs del Dispositivo**:
    - `Health Connect API` (Pasos, Sueño)
    - `UsageStatsManager` (Rastreo de tiempo de uso de apps)
- **APIs Externas**: API de OpenAI/Gemini (Generación de texto narrativo)

## 4. Capas Arquitectónicas (Clean Architecture + MVVM)
El proyecto está estrictamente organizado en tres capas principales para separar las responsabilidades:

### `presentation/` (Capa de UI)
- Contiene todas las pantallas de Jetpack Compose, definiciones de temas y componentes de UI.
- **ViewModels**: Gestionan el estado de la UI y manejan las intenciones del usuario. Se comunican estrictamente con la capa de Dominio (UseCases).
- Aquí no ocurre ninguna lógica de negocio ni obtención de datos.

### `domain/` (Capa de Lógica de Negocio)
- El núcleo de la aplicación. Independiente del framework de Android (Kotlin puro).
- **Models**: Entidades de negocio principales (`UserStats`, `Boss`, `Habit`, `DailyPerformance`).
- **UseCases (Interactors)**: Encapsulan reglas de negocio únicas y específicas (ej., `CalculateDailyDamageUseCase`, `CheckAppUsageLimitUseCase`).
- **Interfaces de Repositorio**: Definen los contratos para el acceso a datos.

### `data/` (Capa de Datos)
- Implementaciones de las interfaces de Repositorio definidas en la capa de Dominio.
- **DataSources**:
    - `local/`: DAOs de Room, Entidades y DataStore/SharedPreferences.
    - `remote/`: Interacciones con Firebase, llamadas a API (Retrofit).
    - `device/`: Wrappers para las APIs del sistema Android (`HealthConnectManager`, `UsageStatsMonitor`).
- Data Transfer Objects (DTOs) y Mappers para convertir modelos de Datos a modelos de Dominio.

## 5. Lógica de Negocio Principal (El Motor RPG)

### Los 3 Pilares
1. **Defensa (Salud/Vitalidad)**:
    - Alimentado por: Google Health Connect (Horas de sueño + Pasos).
    - Lógica: No cumplir los objetivos de sueño/pasos reduce la defensa del usuario, haciendo que el Jefe inflija más daño durante el cálculo nocturno.
2. **Ataque (Disciplina)**:
    - Alimentado por: Room DB (Finalización de hábitos/tareas).
    - Lógica: Completar hábitos genera Puntos de Ataque. Las rachas actúan como multiplicadores de daño.
3. **Estamina (Foco)**:
    - Alimentado por: `UsageStatsManager`.
    - Lógica: El usuario define una lista de bloqueo/límite de tiempo para apps (ej., Instagram, TikTok). Mantenerse por debajo del límite conserva la estamina. Excederlo drena la estamina y bloquea los ataques críticos.

### El Sistema de Batalla
- Las batallas son asíncronas.
- Hay una campaña estática de Jefes.
- Un worker en segundo plano (`WorkManager`) se ejecuta al final del día para:
    1. Obtener pasos/sueño.
    2. Obtener la finalización de hábitos.
    3. Obtener el tiempo de pantalla.
    4. Calcular el daño total infligido al Jefe y el daño recibido por el Usuario.
    5. Actualizar Firestore y Room.

## 6. El Motor Matemático del RPG

El juego está balanceado para una progresión de 6 meses (~180 días) hasta alcanzar el Nivel Máximo (100). Utiliza una curva de progresión de ratio constante (Exponencial Suave), donde el esfuerzo requerido y las recompensas crecen de forma cuadrática.

### 6.1. Fórmulas de Nivel y Experiencia (XP)
El costo de XP para subir al siguiente nivel ($L$) aumenta progresivamente:
* **Fórmula de XP:** $XP_{req}(L) = 50 + 4L$

### 6.2. Fórmulas de Estadísticas del Jugador
Las estadísticas no se guardan en la base de datos, se calculan en tiempo real según el nivel actual ($L$) usando ecuaciones de segundo grado:
* **Vida Máxima (HP):** $HP(L) = 1000 + 5L + 0.1L^2$
* **Ataque Base:** $Atk(L) = 100 + 0.5L + 0.01L^2$
* **Defensa Base:** $Def(L) = 10 + 0.2L + 0.003L^2$

### 6.3. Regla del Desmayo (Zona Crítica al 50%)
* El jugador entra en "Fatiga Extrema" si su HP interno cae al 50% de su HP Máximo actual.
* **Interfaz (UI):** El 50% interno se representa visualmente como el 50% en la barra de vida de la aplicación.
* **Penalización:** Pérdida de Rachas activas, curación del Jefe (+15% HP) y reinicio de la vida del jugador al 100% para el día siguiente.

## 7. Bucle de Combate y Multiplicadores

El combate se ejecuta contra los "Jefes de Mes" (Entidades con HP masivo). El ataque puede ser Manual o Automático.

### 7.1. El Corte Diario
El usuario define su "Hora de Corte" (ej. 23:59 o 02:00 AM). Los hábitos se agrupan en este bloque de 24 horas.

### 7.2. Tipos de Ataque
1.  **Ataque Manual (Recomendado):** El usuario abre la app y pulsa "Atacar".
    * *Recompensa:* Multiplicador de Presencia (x1.1) + 20 XP adicionales.
2.  **Ataque Automático:** Si pasa el mediodía (12:00 PM) del día siguiente y el usuario no ha reportado, el sistema `WorkManager` ataca por él con los datos guardados.
    * *Penalización:* Pierde el Multiplicador de Presencia (x1.0) y no gana XP extra de reporte.

### 7.3. Tabla de Multiplicadores Acumulativos
* **Daño Base:** Se calcula por el % de hábitos completados (Ej: 4/5 hábitos = 80% del Ataque Base).
* **Rachas:** +0.05 al daño por cada día consecutivo cumpliendo objetivos (Máximo x1.5).
* **Módulo Salud (Manual):** x1.2 si el usuario reporta manualmente haber cumplido sus pasos/sueño.
* **Módulo Salud (Automático):** x1.5 si Health Connect verifica automáticamente el cumplimiento.

## 8. Reglas y Directrices para el Asistente de IA
*Al generar código para este proyecto, la IA debe adherirse estrictamente a las siguientes reglas:*

1. **Jetpack Compose Primero**: Nunca usar layouts XML. Toda la UI debe estar escrita en Jetpack Compose declarativo.
2. **Gestión de Estado**: Usar `StateFlow` en los ViewModels para exponer el estado a Compose. Usar data classes para el Estado de la UI (ej., `data class HomeUiState(...)`).
3. **Conciencia de Ciclo de Vida**: Usar `collectAsStateWithLifecycle()` al recolectar flows en Compose.
4. **Permisos**: Los permisos de Android para Health Connect y Usage Access son complejos. Proporcionar siempre una UI de respaldo elegante (graceful fallback) e instrucciones claras al solicitar permisos.
5. **Procesamiento en Segundo Plano**: Usar `WorkManager` para los cálculos diarios. Evitar los Services de larga duración a menos que sea estrictamente necesario para el bloqueo de apps en tiempo real.
6. **Separación de Responsabilidades**: Nunca inyectar un Context o DAO directamente en un ViewModel. Pasar siempre los datos a través de un Repositorio y UseCase.
7. **Código Limpio**: Mantener las funciones pequeñas. Usar las scope functions de Kotlin (`let`, `apply`, `run`) de forma idiomática.

## 9. Estructura del Proyecto (Árbol de Carpetas)
```text
com.moises.vitalodyssey
├── app/                  # Application class, DI setup (Koin)
├── core/                 # Extensions, Clases Base, Theme, Rutas de Navegación
├── data/
│   ├── local/            # Room Database, DAOs, DataStore (UserPreferences)
│   ├── remote/           # Firebase, Network clients
│   ├── device/           # Wrappers de HealthConnect, UsageStats
│   ├── repository/       # Implementaciones de Repositorios
│   └── mapper/           # Mappers de Datos a Dominio
├── domain/
│   ├── model/            # Objetos de negocio (Boss, Habit, UserStats)
│   ├── repository/       # Interfaces de Repositorios
│   └── usecase/          # CalculateDamage, GetDailyStats, etc.
└── presentation/
    ├── screens/          # Pantallas Compose (Home, Battle, Habits, Settings)
    ├── components/       # Widgets reutilizables (Botones, ProgressBars)
    └── viewmodels/       # ViewModels de las pantallas
```

## 10. Estructura de Datos (Room y DataStore)

### 10.1. UserPreferences (Jetpack DataStore)
Almacena el estado global del jugador:
* `level`: Int (Default 1)
* `currentXp`: Int (Default 0)
* `currentHp`: Int (Calculado al inicio)
* `cutoffTime`: String (Ej: "23:59")
* `healthGoalSteps`: Int (Ej: 8000)
* `healthGoalSleep`: Float (Ej: 7.5)

### 10.2. Entidad Habit (Room Database)
Almacena la configuración de cada hábito:
* `id`: Int (PrimaryKey, AutoGenerate)
* `name`: String
* `note`: String
* `type`: Enum (BOOLEAN, MEASURABLE)
* `unit`: String (Ej: Km, mins)
* `targetValue`: Float
* `targetType`: Enum (AT_LEAST, AT_MOST)
* `frequency`: Enum (DAILY, WEEKLY, MONTHLY)
* `isCompleted`: Boolean
* `currentCount`: Float
* `currentStreak`: Int
***