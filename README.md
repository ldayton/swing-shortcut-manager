# Swing Shortcut Manager

A Java Swing application for managing keyboard shortcuts

## Project Structure

This is a Java 17+ project using Gradle 9.0.0 for build management. The application provides a GUI for editing keyboard shortcuts defined in XML configuration files.

## Build System

### Requirements
- Java 17 or higher
- Gradle 9.0.0 (included via wrapper)

### Building
```bash
./gradlew build
```

### Running
```bash
./gradlew run
```

### Quality Tools
The project uses a streamlined quality toolchain:

- **Error Prone**: Static analysis integrated into compilation
- **Spotless**: Code formatting with Google Java Format
- **Dependency Updates**: Check for outdated dependencies

Run quality checks:
```bash
./gradlew check
```

Auto-format code:
```bash
./gradlew spotlessApply
```

Check for dependency updates:
```bash
./gradlew dependencyUpdates
```

## Dependencies

### Runtime
- Jackson XML 2.18.2 - Modern XML parsing (replaces legacy JDOM/Jaxen)

### Build Tools
- Error Prone 2.33.0 - Static analysis
- Spotless - Code formatting
- Gradle Versions Plugin - Dependency management

## Architecture

The application follows a standard Swing architecture:

- `Main.java` - Application entry point
- `ShortcutManager.java` - Main GUI window and table management
- `XActionParser.java` - XML configuration parsing using Jackson
- `Shortcut.java` - Keyboard shortcut representation and platform handling
- `UserDB.java` - Persistent storage using Java Preferences API
- `XAction.java` - Action definition record class

## Configuration

Keyboard shortcuts are defined in `src/main/resources/actions.xml` with the following structure:

```xml
<actions>
  <action class="com.example.Action" name="Action Name" tooltip="Description">
    <shortcut>
      <mask keyname="menu" />
      <key keyname="P" />
    </shortcut>
  </action>
</actions>
```

## Development

### Code Style
Code is automatically formatted using Google Java Format via Spotless. Run `./gradlew spotlessApply` to format all code.

### Testing
Currently no automated tests. The application can be tested by running `./gradlew run` and interacting with the GUI.

### Adding Dependencies
Add new dependencies to `build.gradle` in the appropriate scope:
- `implementation` - Runtime dependencies  
- `errorprone` - Static analysis tools

## Platform Support

The application supports cross-platform keyboard shortcuts with platform-specific display:
- **macOS**: Uses Command (⌘), Option (⌥), Shift (⇧), Control (^) symbols
- **Other platforms**: Uses Ctrl, Alt, Shift text with + separators

