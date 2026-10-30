# unfinal-compiler-plugin

A Kotlin compiler plugin that drops `ACC_FINAL` from properties and methods you
select by annotation.

Two rules:

- **property rule** — the backing field of a `val` property loses `ACC_FINAL`;
- **method rule** — an annotated method becomes `open`.

## Why

Some frameworks write fields and call methods after the constructor has run.
Since class-file version 53 (Java 9) a `putfield` outside `<init>` is illegal on a
`final` field, and a `final` method cannot be intercepted at all.

Kotlin has no syntax that says "final in source, not final in the class file", so
every `val` backing field is final by construction. This plugin is the missing
switch: it ships with no annotation names and applies whatever lists you give it.

## Compared with `all-open`

The Kotlin `all-open` plugin changes **modality**: a marked class and its members stop
being `final`, so they can be subclassed and overridden.

It does not touch backing fields. A `val` still compiles to a `final` field under
`all-open` — that is
[KT-90583](https://youtrack.jetbrains.com/issue/KT-90583) — so every `putfield`
from a generated method stays illegal on those classes.

|                               | `all-open`                       | this plugin                             |
|-------------------------------|----------------------------------|-----------------------------------------|
| class `ACC_FINAL`             | removed                          | untouched                               |
| accessor / method `ACC_FINAL` | removed for every member         | removed only for methods you select     |
| backing field `ACC_FINAL`     | untouched                        | removed for `val` properties you select |
| effect on dispatch            | every member becomes overridable | nothing, unless the method rule matched |
| effect on subclassing         | classes become extensible        | nothing                                 |

So they are complementary, not alternatives: use `all-open` when a framework needs
an open class or open accessors, and this plugin when it needs a writable field.

One overlap to keep in mind: both plugins have a method rule. `all-open` opens
every member of a marked class; this plugin opens only methods whose annotation
you list.

## Versioning

The plugin is compiled against the Kotlin compiler's IR API, which is stable only
within a Kotlin minor version. Releases track that line:

| plugin  | Kotlin |
|---------|--------|
| `2.4.x` | 2.4.x  |

One artifact line per Kotlin line. A Kotlin 2.5 build needs a 2.5.x release.

## Requirements

Kotlin 2.4.x, JDK 8 or newer at build time. The plugin jars are Java 8 bytecode
and are compiled against the Java 8 API.

## Options

All three are repeatable and independent. An empty list switches its rule off.

| option                           | effect                                                                                                                       |
|----------------------------------|------------------------------------------------------------------------------------------------------------------------------|
| `allPropertiesInClassAnnotation` | every `val` property of a class carrying one of these annotations loses `ACC_FINAL`; subclasses of such a class are included |
| `propertyAnnotation`             | a `val` property carrying one of these annotations loses `ACC_FINAL`, in any class                                           |
| `methodAnnotation`               | a method carrying one of these annotations becomes `open`, in any class                                                      |

`propertyAnnotation` needs no class marker, so it also reaches plain classes and
DTOs. `methodAnnotation` too, which is what makes listener classes work without a
class marker.

Only members of classes are visited. A top-level (file-scope) property or function
is never touched, and a subclass inherits the class rule through any number of
levels.

Skipped on purpose: `var` properties are already writable, `const val` has no
backing field to touch, delegated properties keep their `$delegate` field, and
`private` methods are left alone (they cannot be overridden anyway).

## Selector syntax

```
fqn
fqn#member
fqn#member=value
```

| form               | matches                                                                       |
|--------------------|-------------------------------------------------------------------------------|
| `fqn`              | the annotation, whatever its arguments                                        |
| `fqn#member`       | the annotation when that argument is present                                  |
| `fqn#member=value` | the annotation when that argument, or its declaration default, equals `value` |

A value is compared as text with any qualifier stripped, so `=LAZY` matches both
`LAZY` and `SomeEnum.LAZY`.

When the source omits an argument, the annotation declaration's default is used.
That works for annotation classes compiled from Kotlin and from Java class files,
so an annotation declared `fetch() default LAZY` matches `#fetch=LAZY` without an
explicit attribute, while one defaulting to `EAGER` does not.

## Install

Put the jar on the Kotlin compiler plugin classpath and pass the selectors.

Gradle:

```kotlin
// build.gradle.kts
dependencies {
    kotlinCompilerPluginClasspath("dev.kergos:unfinal-compiler-plugin:2.4.0")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
    compilerOptions.freeCompilerArgs.addAll(
        listOf("-P", "plugin:dev.kergos.unfinal:propertyAnnotation=com.example.Persisted"),
    )
}
```

For JPA, the `dev.kergos.jpa` preset does this for you; see
[`kergosdev/jpa-preset-gradle-plugin`](https://github.com/kergosdev/jpa-preset-gradle-plugin).

Maven cannot register a third-party compiler plugin in `<compilerPlugins>`: that
list is resolved as Plexus components. Every dependency of `kotlin-maven-plugin`
is passed to the compiler as `-Xplugin`, so put the artifact there and pass the
options in `<args>`.

```xml

<plugin>
    <groupId>org.jetbrains.kotlin</groupId>
    <artifactId>kotlin-maven-plugin</artifactId>
    <version>${kotlin.version}</version>
    <configuration>
        <args>
            <arg>-P</arg>
            <arg>plugin:dev.kergos.unfinal:propertyAnnotation=com.example.Persisted</arg>
        </args>
    </configuration>
    <dependencies>
        <dependency>
            <groupId>dev.kergos</groupId>
            <artifactId>unfinal-compiler-plugin</artifactId>
            <version>2.4.0</version>
        </dependency>
    </dependencies>
</plugin>
```

`<pluginOptions>` does not work here: it resolves names through Plexus and fails
with `PluginNotFoundException` before reaching the compiler.

## Build

```
./gradlew build          # plugin + tests
./gradlew publishToMavenLocal
```

Tests compile classes with `kotlin-compile-testing` and assert the emitted flags
with reflection: declaration defaults, all selector forms, Kotlin and Java
annotation classes, classes without a marker, and untouched `var`/delegated
properties.

## Publishing

Maven Central via the [Central Portal](https://central.sonatype.com) publisher API,
through the `com.gradleup.nmcp` and `com.gradleup.nmcp.aggregation` plugins. They are
applied per project on purpose: nmcp's settings plugin breaks the Kotlin build with
the configuration cache on (`__buildFusService__` provider error).

Credentials live in `~/.gradle/gradle.properties`, never in the repo:

```properties
centralPortalUsername=<token user from central.sonatype.com>
centralPortalPassword=<token password>
signingInMemoryKeyId=<key id; only the last 8 characters are used>
signingInMemoryKeyPassword=<passphrase>
signingInMemoryKey=<armored secret key on one line, newlines as \n>
```

```bash
./gradlew clean nmcpPublishAggregationToCentralPortal
```

The passphrase never has to be stored. With `-PsigningUseGpg` the build signs
through the local `gpg` binary instead, and gpg-agent asks for the passphrase once
per cache lifetime:

```bash
./gradlew clean nmcpPublishAggregationToCentralPortal -PsigningUseGpg
```

That needs `gpg` on `PATH` and the secret key in the user keyring; if the prompt
does not appear from the Gradle daemon, prime the agent first
(`gpg -bs </dev/null`) or run the build with `--no-daemon`.

`publishingType` is `USER_MANAGED`: the upload is validated but stays private until
you release it in the portal. Signing is skipped when `signingInMemoryKey` is absent,
so `publishToMavenLocal` works without a key.

## Licence

Apache 2.0, see `LICENSE`.
