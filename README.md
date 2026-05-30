# AmplProtections

Sistema avanzado de protecciones para servidores Minecraft con bloques personalizados, flags de permisos, alquileres, rollback y mas.


---

## Tabla de Contenidos

- [Requisitos](#requisitos)
- [Instalacion](#instalacion)
- [Configuracion](#configuracion)
  - [Base de Datos](#base-de-datos)
  - [Mundos](#mundos)
  - [Limites de Protecciones](#limites-de-protecciones)
  - [Bloques de Proteccion](#bloques-de-proteccion)
  - [Flags](#flags)
  - [Economia](#economia)
  - [Hologramas](#hologramas)
  - [Particulas](#particulas)
  - [Merge (Fusion)](#merge-fusion)
  - [Alquiler (Rental)](#alquiler-rental)
  - [Rollback](#rollback)
  - [Presets](#presets)
  - [Teletransporte](#teletransporte)
  - [Idioma](#idioma)
- [Comandos](#comandos)
  - [Comandos de Jugador](#comandos-de-jugador)
  - [Comandos de Admin](#comandos-de-admin)
- [Permisos](#permisos)
- [Sistema de Rangos](#sistema-de-rangos)
- [Sistema de Flags](#sistema-de-flags)
- [PlaceholderAPI](#placeholderapi)
- [Dependencias](#dependencias)

---

## Requisitos

- Java 21+
- Paper 1.21+
- MySQL

---

## Instalacion

1. Descarga el archivo `AmplProtections-1.0.5.jar`
2. Colocalo en la carpeta `plugins/` de tu servidor
3. Inicia el servidor para generar los archivos de configuracion
4. Configura la conexion a MySQL en `plugins/AmplProtections/config.yml`
5. Reinicia el servidor

---

## Configuracion

### Base de Datos

```yaml
database:
  host: 127.0.0.1
  port: 3306
  name: "database_name"
  username: "username"
  password: "password"
  properties: ?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
```

### Mundos

Controla en que mundos se pueden colocar protecciones y el limite por mundo.

```yaml
worlds:
  disabled-worlds:
    - world_nether
    - world_the_end
  max-protections-per-world: 5
```

### Limites de Protecciones

Configura cuantas protecciones puede tener cada jugador segun su rango. Los limites pueden ser globales o por tipo de proteccion.

```yaml
limits:
  default: 3      # Sin permiso especial
  vip: 5           # amplprotections.limit.vip
  mvp: 10          # amplprotections.limit.mvp
  admin: 999       # amplprotections.limit.admin
  # Limites por tipo (opcional, sobreescribe el global)
  by-type:
    carbon_protector:
      default: 3
      vip: 5
    iron_protector:
      default: 5
      vip: 7
```

### Bloques de Proteccion

Los bloques de proteccion se configuran en `blocks.yml`. Cada tipo soporta:

| Propiedad | Descripcion |
|-----------|-------------|
| `material` | Material de Minecraft (usa `PLAYER_HEAD` para cabezas) |
| `custom-model-data` | Valor para resource packs (`-1` para desactivar) |
| `skull-value` | Textura Base64 para cabezas custom |
| `display-name` | Nombre del item en formato MiniMessage |
| `radius` | Radio de proteccion (area = radius * 2 + 1) |
| `hide-block` | `true` para ocultar el bloque despues de colocarlo |
| `glow-on-view` | `true` para mostrar particulas al usar `/p view` |
| `enable-merge` | `true` para permitir fusionar con otros del mismo tipo |
| `max-merge-radius` | Radio maximo al fusionar (`0` = no aplica) |

**Ejemplo:**

```yaml
protection-blocks:
  carbon_protector:
    material: "PLAYER_HEAD"
    custom-model-data: -1
    skull-value: "eyJ0ZXh0dXJlcyI6eyJTS0lO..."
    display-name: "<gray>Protection Block <dark_gray>[15x15]"
    radius: 15
    hide-block: false
    glow-on-view: true
    enable-merge: false
    max-merge-radius: 30
```

**Tipos incluidos por defecto:**

| Tipo | Radio | Tamano |
|------|-------|--------|
| `carbon_protector` | 15 | 31x31 |
| `iron_protector` | 50 | 101x101 |
| `diamond_protector` | 100 | 201x201 |
| `emerald_protector` | 200 | 401x401 |

### Flags

Las flags controlan que acciones pueden realizar los jugadores dentro de una proteccion. Cada flag tiene 5 niveles de permiso: `NONE`, `OWNER`, `MEMBERS`, `ADMINS`, `EVERYONE`.

```yaml
flags:
  enabled:
    - pvp
    - mob-damage
    - mob-spawn
    - tnt
    - explosions
    - entity-damage
    - potion-splash
    - block-break
    - block-place
    - leaf-decay
    - fire-spread
    - fire-damage
    - fire-ignite
    - crop-trample
    - ice-melt
    - snow-melt
    - lava-flow
    - water-flow
    - soil-dry
    - use-doors
    - use-switches
    - use-chests
    - use-crafting
    - use-animals
    - use-portals
    - use-beds
    - use-villager
    - frame-rotate
    - frame-break
    - armor-stand-edit
    - painting-break
    - vehicle-place
    - vehicle-break
    - item-drop
    - item-pickup
```

**Flags silenciosas** (no muestran mensaje de acceso denegado):

```yaml
  silent-flags:
    - item-pickup
    - item-drop
    - crop-trample
    - mob-spawn
    # ... mas flags
  access-denied-cooldown-ms: 3000
```

### Economia

Integracion con Vault para cobrar al comprar o fusionar protecciones.

```yaml
economy:
  enabled: true
  protection-costs:
    carbon_protector: 100.0
    iron_protector: 500.0
    diamond_protector: 1500.0
    emerald_protector: 3000.0
  bypass-permission: amplprotections.economy.bypass
```

**Comandos al comprar** (ejecutados al adquirir una proteccion):

```yaml
buy-commands:
  carbon_protector:
    - "console: say %player% bought a Carbon Protector"
    - "console: money take %player% 100"
```

Placeholders disponibles: `%player%`, `%type%`, `%price%`, `%size%`, `%radius%`

Usa `player: <comando>` para ejecutar como jugador o `console: <comando>` para ejecutar desde consola.

### Hologramas

Muestra texto flotante sobre las protecciones con informacion del dueño.

```yaml
holograms:
  enabled: true
  update-interval-ticks: 20
  content: "<white>%owner%</white> | <gray>%size%</gray> | <green>%members_online%/%members% online</green>"
  height-offset: 2.5
  visible-distance: 32
  toggle-permission: amplprotections.hologram.toggle
```

Placeholders disponibles en `content`: `%owner%`, `%size%`, `%members%`, `%members_online%`, `%land_id%`, `%type%`, `%x%`, `%y%`, `%z%`

### Particulas

Configura las particulas que se muestran al usar `/p view`.

```yaml
particles:
  duration-ticks: 200
  density: 1
  types:
    border: DUST
    border-color: 0,200,255
    border-size: 1.2
    corner: DUST
    corner-color: 255,165,0
    corner-size: 1.5
    pillar: FLAME
    sparkle: HAPPY_VILLAGER
    wall: END_ROD
    fire: SOUL_FIRE_FLAME
    witch: WITCH
  pulse:
    enabled: true
    interval-ticks: 5
    size-min: 0.7
    size-max: 1.3
```

### Merge (Fusion)

Permite fusionar protecciones adyacentes del mismo tipo para crear una zona mas grande.

```yaml
merge:
  enabled: true
  cost: 50.0
  bypass-permission: amplprotections.economy.bypass
  max-distance: 5
```

### Alquiler (Rental)

Sistema de alquiler de protecciones entre jugadores.

```yaml
rental:
  enabled: true
  check-interval-ticks: 6000
  default-auto-renew: false
  max-rental-days: 30
  tax-percent: 5.0
```

### Rollback

Permite revertir cambios de bloques dentro de una proteccion.

```yaml
rollback:
  enabled: true
  flush-interval-ticks: 600
  max-age-days: 7
```

### Presets

Configuraciones predefinidas de flags que se pueden aplicar rapidamente.

```yaml
presets:
  built-in:
    public:
      description: "Everyone can interact"
      flags:
        block-break: EVERYONE
        block-place: EVERYONE
        # ...
    private:
      description: "Only the owner can interact"
      flags:
        block-break: OWNER
        block-place: OWNER
        # ...
    friends:
      description: "Members and higher ranks can interact"
      flags:
        block-break: MEMBERS
        block-place: MEMBERS
        # ...
```

### Teletransporte

```yaml
teleport:
  cooldown-seconds: 30
  bypass-permission: amplprotections.cooldown.bypass
```

### Idioma

El plugin soporta multiples idiomas. Los archivos de idioma estan en `plugins/AmplProtections/lang/`.

```yaml
language: es  # Idiomas disponibles: en, es
```

---

## Comandos

### Comandos de Jugador

| Comando | Descripcion |
|---------|-------------|
| `/p menu` | Abre el menu de configuracion de la proteccion |
| `/p info` | Muestra informacion de la proteccion actual |
| `/p add <jugador>` | Añade un miembro a la proteccion |
| `/p remove <jugador>` | Remueve un miembro de la proteccion |
| `/p promote <jugador>` | Asciende un miembro a Admin o Secondary Owner |
| `/p demote <jugador>` | Degrada un miembro |
| `/p members` | Lista los miembros de la proteccion |
| `/p flag <flag> <valor>` | Cambia el nivel de una flag |
| `/p view` | Muestra los limites de la proteccion con particulas |
| `/p list` | Lista todas tus protecciones |
| `/p buy` | Abre la tienda de protecciones |
| `/p merge` | Fusiona protecciones adyacentes |
| `/p lore <texto>` | Establece una descripcion personalizada |
| `/p hologram` | Activa/desactiva el holograma |
| `/p rent` | Gestiona el alquiler de una proteccion |
| `/p rent set <precio> <dias>` | Pone una proteccion en alquiler |
| `/p rent cancel` | Cancela el alquiler |
| `/p rent info` | Muestra informacion del alquiler |
| `/p rollback` | Abre el menu de rollback |
| `/p rollback player <jugador> <minutos>` | Revierte cambios de un jugador |
| `/p rollback all <minutos>` | Revierte todos los cambios |
| `/p rollback preview <minutos>` | Previsualiza cambios a revertir |
| `/p preset apply <nombre>` | Aplica un preset de flags |
| `/p preset create <nombre>` | Crea un preset personalizado |
| `/p preset delete <nombre>` | Elimina un preset personalizado |

**Aliases:** `/proteccion`, `/prote`, `/protection`, `/p`

### Comandos de Admin

| Comando | Descripcion |
|---------|-------------|
| `/aprot give <jugador> <tipo>` | Entrega un bloque de proteccion |
| `/aprot list` | Abre el panel administrador |
| `/aprot info` | Muestra informacion tecnica de la proteccion |
| `/aprot delete` | Elimina la proteccion actual |
| `/aprot reload` | Recarga la configuracion |

**Aliases:** `/adminprotecciones`, `/adminprot`

---

## Permisos

| Permiso | Descripcion | Default |
|---------|-------------|---------|
| `amplprotections.use` | Usar `/proteccion` | `true` |
| `amplprotections.admin.use` | Usar `/aprot` | `op` |
| `amplprotections.admin.bypass` | Bypass a todas las protecciones | `op` |
| `amplprotections.limit.default` | Limite de 3 protecciones | `true` |
| `amplprotections.limit.vip` | Limite de 5 protecciones | `false` |
| `amplprotections.limit.mvp` | Limite de 10 protecciones | `false` |
| `amplprotections.limit.admin` | Limite de 999 protecciones | `false` |
| `amplprotections.economy.bypass` | Comprar sin costo | `op` |
| `amplprotections.cooldown.bypass` | Bypass cooldown de teletransporte | `op` |
| `amplprotections.merge.use` | Fusionar protecciones | `false` |
| `amplprotections.rent.use` | Alquilar protecciones | `true` |
| `amplprotections.rent.create` | Poner protecciones en alquiler | `true` |
| `amplprotections.rollback.use` | Rollback en protecciones propias | `false` |
| `amplprotections.rollback.admin` | Rollback en cualquier proteccion | `op` |
| `amplprotections.hologram.toggle` | Activar/desactivar hologramas | `true` |
| `amplprotections.preset.use` | Usar presets | `true` |
| `amplprotections.preset.create` | Crear presets personalizados | `true` |

---

## Sistema de Rangos

Las protecciones tienen un sistema de rangos jerarquico:

| Rango | Peso | Permisos |
|-------|------|----------|
| **Owner** | 4 | Control total de la proteccion |
| **Secondary Owner** | 3 | Gestionar miembros, cambiar flags |
| **Admin** | 2 | Gestionar miembros, cambiar flags |
| **Member** | 1 | Acciones basicas segun flags |

---

## Sistema de Flags

Cada flag tiene 5 niveles de permiso:

| Nivel | Descripcion |
|-------|-------------|
| `NONE` | Nadie puede realizar la accion |
| `OWNER` | Solo el dueño |
| `MEMBERS` | Miembros y rangos superiores |
| `ADMINS` | Admins, secondary owners y dueño |
| `EVERYONE` | Todos los jugadores |

**Ejemplo de uso:**
```
/p flag block-break MEMBERS
/p flag pvp NONE
/p flag use-chests EVERYONE
```

---

## PlaceholderAPI

El plugin registra los siguientes placeholders bajo el identificador `%AmplProtections%`:

| Placeholder | Descripcion |
|-------------|-------------|
| `%AmplProtections_total%` | Total de protecciones del jugador |
| `%AmplProtections_limit%` | Limite maximo de protecciones |
| `%AmplProtections_limit_<tipo>%` | Limite para un tipo especifico |
| `%AmplProtections_type_count_<tipo>%` | Cantidad de un tipo especifico |
| `%AmplProtections_land_ids%` | Lista de IDs de protecciones |
| `%AmplProtections_land_count%` | Cantidad de protecciones |
| `%AmplProtections_current_land%` | ID de la proteccion actual |
| `%AmplProtections_current_owner%` | Dueño de la proteccion actual |
| `%AmplProtections_current_type%` | Tipo de la proteccion actual |
| `%AmplProtections_current_size%` | Tamano de la proteccion actual |
| `%AmplProtections_current_world%` | Mundo de la proteccion actual |
| `%AmplProtections_current_x%` | Coordenada X del centro |
| `%AmplProtections_current_y%` | Coordenada Y del centro |
| `%AmplProtections_current_z%` | Coordenada Z del centro |
| `%AmplProtections_current_members%` | Miembros de la proteccion actual |
| `%AmplProtections_current_rank%` | Rango del jugador en la proteccion actual |

---

## Dependencias

| Dependencia | Tipo | Descripcion |
|-------------|------|-------------|
| [Paper](https://papermc.io/) | Requerido | Servidor Minecraft 1.21+ |
| [MySQL](https://www.mysql.com/) | Requerido | Base de datos |
| [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) | Opcional | Placeholders para otros plugins |
| [Vault](https://www.spigotmc.org/resources/vault.34315/) | Opcional | Integracion con economia |

---

## Construccion

```bash
git clone <repositorio>
cd AmplProtections
mvn clean package
```

El archivo JAR se genera en `target/AmplProtections-1.0.5.jar`

---

## Soporte

Para reportar bugs o solicitar funcionalidades, abre un issue en el repositorio del proyecto.
