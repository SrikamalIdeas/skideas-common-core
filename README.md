# skideas-common-core

Shared infrastructure library for all `SrikamalIdeas` projects.

**Artifact**: `com.skideas:common-core`  
**Registry**: [GitHub Packages](https://github.com/SrikamalIdeas/skideas-common-core/packages)

---

## What's included

| Package | Contents |
|---------|----------|
| `com.skideas.common.exception` | `SkideasException`, `ResourceNotFoundException`, `ValidationException`, `ExternalServiceException` |
| `com.skideas.common.converter` | `EncryptedStringConverter` (AES-256-GCM) |
| `com.skideas.common.util` | `IdGenerator`, `DateTimeUtils`, `StringSanitizer`, `TraceIdProvider` |
| `com.skideas.common.entity` | `AuditableEntity` (`@MappedSuperclass`) |

---

## Add as a dependency

### 1. Configure GitHub Packages in `~/.m2/settings.xml`

```xml
<servers>
  <server>
    <id>github</id>
    <username>YOUR_GITHUB_USERNAME</username>
    <password>${env.GITHUB_PACKAGES_TOKEN}</password>
  </server>
</servers>
```

Set `GITHUB_PACKAGES_TOKEN` in your shell to a GitHub classic PAT with `read:packages` scope.

### 2. Add repository to your `pom.xml`

```xml
<repositories>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/SrikamalIdeas/skideas-common-core</url>
  </repository>
</repositories>
```

### 3. Add dependency

```xml
<dependency>
  <groupId>com.skideas</groupId>
  <artifactId>common-core</artifactId>
  <version>0.1.0</version>
</dependency>
```

---

## Required configuration

### Encryption key
Add to your `application.properties`:
```properties
skideas.encryption.key=${SKIDEAS_ENCRYPTION_KEY}
```

Set `SKIDEAS_ENCRYPTION_KEY` to a Base64-encoded 32-byte AES key:
```bash
openssl rand -base64 32
```

### JPA Auditing
Add `@EnableJpaAuditing` to your Spring Boot application class to activate `AuditableEntity` timestamp population.

---

## Build & test

```bash
mvn verify
```

Requires Java 21. JaCoCo enforces ≥95% line coverage on `verify`.
