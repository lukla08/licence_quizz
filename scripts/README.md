# scripts — buildy komponentów Javy

Wrappery budujące rdzeń Spring i klienta JavaFX właściwym łańcuchem
**JDK 21 + Maven 3.9.x**. Domyślne `mvn`/`JAVA_HOME` w tej powłoce to wciąż
Maven 3.5.0 + JDK 11, dlatego skrypty wstrzykują:

- `JAVA_HOME21` → JDK 21
- `MAVEN_HOME9` → Maven 3.9.x (`$MAVEN_HOME9/bin/mvn`)

Obie zmienne muszą być ustawione w środowisku (skrypt przerwie z błędem, gdy ich brak).
Skrypty działają z dowolnego katalogu — ścieżkę do `pom.xml` rozwiązują względem własnej lokalizacji.

## Użycie

| Cel | Git Bash | cmd.exe / PowerShell |
| --- | --- | --- |
| Build rdzenia (clean package) | `scripts/build-server.sh` | `scripts\build-server.cmd` |
| Uruchom rdzeń (spring-boot:run) | `scripts/run-server.sh` | `scripts\run-server.cmd` |
| Build klienta JavaFX (clean compile) | `scripts/build-desktop.sh` | `scripts\build-desktop.cmd` |
| Uruchom GUI JavaFX (javafx:run) | `scripts/run-desktop.sh` | `scripts\run-desktop.cmd` |

Każdy skrypt przyjmuje własne cele Mavena zamiast domyślnych, np.:

```bash
scripts/build-server.sh clean test
scripts/build-desktop.sh clean compile -o
```

## Pozostałe klienty

`clients/web` (Vite+React) i `clients/flutter` używają standardowych narzędzi i nie
wymagają wrapperów:

- web: `cd clients/web && npm install && npm run build`
- flutter: `cd clients/flutter && flutter build web` (lub `flutter run -d windows`)
