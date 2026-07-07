# Copilot Instructions — surf-idea-plugin

## Build & Run

```shell
# Build the plugin
./gradlew build

# Run tests
./gradlew test

# Launch a sandboxed IDE with the plugin loaded
./gradlew runIde

# Verify plugin compatibility
./gradlew verifyPlugin
```

JDK 21 is required. Kotlin 2.3.x with `-Xcontext-parameters` enabled.

## Architecture

This is an IntelliJ IDEA plugin (`org.jetbrains.intellij.platform` Gradle plugin) providing inspections, code generation, line markers, and framework detection for the **Surf ecosystem**: `surf-api`, `surf-redis`, and `surf-rabbitmq`.

### Module layout (`src/main/kotlin/dev/slne/surf/idea/surfideaplugin/`)

- **`redis/`** — Inspections, line markers, and code generation for `surf-redis`. Annotation-driven handler pattern (`@OnRedisEvent`, `@HandleRedisRequest`).
- **`rabbitmq/`** — Inspections, line markers, completion, and new-file actions for `surf-rabbitmq`: packet-based messaging (`@RabbitHandler`, `RabbitRequestPacket`/`RabbitResponsePacket`) and the `@RpcService`-based RPC system (`inspection/rpc/`).
- **`surfapi/`** — Platform support (Paper, Velocity, Core), internal-API gating (`@InternalAPIMarker`), and listener generation for `surf-api`.
- **`common/`** — Shared infrastructure: `common/library/SurfLibraryDetector` + `SurfLibraryMarker` (cached classpath detection), inspection base classes (`common/inspection/`), quick fixes, PSI helpers, K2 Analysis API extensions.

### Extension points

All inspections, line markers, and actions are registered in `src/main/resources/META-INF/plugin.xml`. Every new extension must be declared there.

## Key Conventions

### Inspection pattern

Inspections extend one of two Surf base classes (both gate on library presence per file):

- **`SurfApplicableInspection<ElementType, ContextType>`** (preferred; wraps the K2 `KotlinApplicableInspectionBase`):
  1. **`buildVisitor()`** — Use `classVisitor { }` / `namedFunctionVisitor { }` / `parameterVisitor { }` DSL to dispatch to `visitTargetElement()`.
  2. **`isApplicableByPsi()`** — Fast PSI-level filtering only (no analysis; use `hasAnnotationPsi`/`KotlinPsiHeuristics`).
  3. **`getApplicableRanges()`** — Use `ApplicabilityRange.single(element) { ... }` or `ApplicabilityRanges.declarationName(element)`.
  4. **`KaSession.prepareContext()`** — K2 Analysis API phase for accurate type/symbol resolution. Return `null` to skip the element.
  5. **`InspectionManager.createProblemDescriptor()`** — Build the problem with a highlight type and quick fixes (use `.asQuickFix()` to wrap ModCommand actions).
- **`SurfKotlinInspection`** (classic `AbstractKotlinInspection`) for visitors that need to report on multiple element kinds; call `analyze(element) { ... }` inside the visitor for resolution.

Both take the required `SurfLibraryMarker`s as constructor varargs — no manual library check needed inside the inspection.

### Library detection gate

Features outside the inspection bases (line markers, actions, completion) must check the module classpath before activating:

```kotlin
if (!module.hasLibrary(SurfLibraryMarker.SURF_REDIS_API)) return
```

`SurfLibraryDetector` caches per module and invalidates on project-root changes — call it freely, do not add ad-hoc caches.

### Kotlin context parameters

Utility extensions in `common/util/KotlinKaUtils.kt` use Kotlin context parameters (`context(_: KaSession)`) to provide implicit `KaSession` scope. These extensions are only callable inside a `KaSession` block (e.g., inside `prepareContext()`).

### K2 Analysis API only

The plugin targets **K2 mode only** (`<supportsKotlinPluginMode supportsK2="true"/>` without K1 support). All analysis uses `KaSession`, `KaClassSymbol`, `KaClassType`, etc.

### Message bundle

Inspection display names and group names go in `src/main/resources/messages/SurfBundle.properties` (referenced from `plugin.xml` via `key`/`groupKey`). Each inspection also needs an HTML description file in `src/main/resources/inspectionDescriptions/<ShortName>.html` whose name matches the registered `shortName` exactly.

### Class name constants

FQNs for Surf framework annotations and classes are centralized in constant objects (e.g., `SurfRedisClassNames`, `SurfRabbitClassNames`). Always reference these constants rather than using string literals. Icons: use `AllIcons` or the plugin's own `icons/` resources — never IDE-edition-specific icon classes (e.g. `JavaUltimateIcons`).
