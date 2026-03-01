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

JDK 21 is required. Kotlin 2.3.0 with `-Xcontext-parameters` enabled.

## Architecture

This is an IntelliJ IDEA plugin (`org.jetbrains.intellij.platform` Gradle plugin) providing inspections, code generation, line markers, and framework detection for the **Surf ecosystem**: `surf-api`, `surf-redis`, and `surf-database-r2dbc`.

### Module layout (`src/main/kotlin/dev/slne/surf/idea/surfideaplugin/`)

- **`redis/`** — Largest module. Inspections, line markers, code generation, postfix templates, inlay hints, and a tool window for `surf-redis`. Annotation-driven handler pattern (`@OnRedisEvent`, `@HandleRedisRequest`).
- **`surfapi/`** — Platform detection (Paper, Velocity, Core) and API usage inspections for `surf-api`.
- **`databse/`** — Transaction context inspections for `surf-database-r2dbc`. (Note: the package is intentionally spelled `databse`.)
- **`common/`** — Shared utilities: `SurfLibraryDetector` (classpath detection), PSI helpers, K2 Analysis API extensions.
- **`projectgen/`** — Module builder for new Surf projects.

### Extension points

All inspections, line markers, facets, postfix templates, and actions are registered in `src/main/resources/META-INF/plugin.xml`. Every new extension must be declared there.

## Key Conventions

### Inspection pattern

All inspections extend `KotlinApplicableInspectionBase<ElementType, ContextType>` and follow this structure:

1. **`buildVisitor()`** — Use `classVisitor { }` or `namedFunctionVisitor { }` DSL to dispatch to `visitTargetElement()`.
2. **`isApplicableByPsi()`** — Fast PSI-level filtering. Always call `SurfLibraryDetector.hasSurfRedis(element)` (or the relevant library check) first to bail out early if the library isn't on the classpath.
3. **`getApplicableRanges()`** — Use `ApplicabilityRange.single(element) { ... }` to highlight the relevant token (e.g., `nameIdentifier`, a modifier keyword).
4. **`KaSession.prepareContext()`** — K2 Analysis API phase for accurate type/symbol resolution. Return `null` to skip the element.
5. **`InspectionManager.createProblemDescriptor()`** — Build the problem with a highlight type and quick fix (use `.asQuickFix()` to wrap Kotlin quick fixes).

### Library detection gate

Every feature (inspection, line marker, postfix template) must check for the relevant Surf library on the module classpath before activating. Use `SurfLibraryDetector`:

```kotlin
if (!SurfLibraryDetector.hasSurfRedis(element)) return false
```

### Kotlin context parameters

Utility extensions in `common/util/KotlinKaUtils.kt` use Kotlin context parameters (`context(_: KaSession)`) to provide implicit `KaSession` scope. These extensions are only callable inside a `KaSession` block (e.g., inside `prepareContext()`).

### K2 Analysis API only

The plugin targets **K2 mode only** (`<supportsKotlinPluginMode supportsK2="true"/>` without K1 support). All analysis uses `KaSession`, `KaClassSymbol`, `KaClassType`, etc.

### Message bundle

All user-facing strings (inspection display names, group names, hint descriptions) go in `src/main/resources/messages/SurfBundle.properties`, accessed via `SurfBundle.message("key")`. Each inspection also needs an HTML description file in `src/main/resources/inspectionDescriptions/<ShortName>.html`.

### Class name constants

FQNs for Surf framework annotations and classes are centralized in constant objects (e.g., `SurfRedisClassNames`). Always reference these constants rather than using string literals.

### Facet detection

Each Surf library has a `FacetBasedFrameworkDetector` + `Facet` + `FacetType` + `FacetConfiguration` set. Framework detection auto-activates when the library appears on the classpath.
