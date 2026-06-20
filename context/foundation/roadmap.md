# Roadmap — sekwencja bootstrapu

Architektura: wspólny rdzeń (lokalny serwer sync + lokalna baza + model
domenowy) w **Spring/Java**, z wymiennymi klientami (natywny web, Flutter,
natywny desktop w Javie). Stos rdzenia ustalony w `tech-stack.md`.

## Układ katalogów

```
licence_quizz/
  server/              ← rdzeń Spring (przeniesiony z root)
  clients/
    web/               ← vite-react (zescaffoldowany)
    flutter/           ← flutter (zescaffoldowany)
    desktop-java/      ← javafx (zescaffoldowany)
  context/             ← wspólne dla repo (PRD, hand-offy, logi)
```

## Łańcuch build dla Javy (rdzeń + klient JavaFX)

Oba komponenty Javy celują w **Java 21**. Domyślny `JAVA_HOME` na tej maszynie
wskazuje wciąż na JDK 11, a domyślny `mvn` to 3.5.0 (2017) — za stare dla
nowoczesnych pluginów. Buduj jawnie nowoczesnym łańcuchem dostępnym przez
zmienne środowiskowe:

- `JAVA_HOME21` → `c:\Program Files\Java\jdk-21.0.11+10`
- `MAVEN_HOME9` → `c:\Program Files\maven\3.9.15`

```bash
JAVA_HOME="$JAVA_HOME21" "$MAVEN_HOME9/bin/mvn" -B clean compile   # w server/ lub clients/desktop-java/
```

Zweryfikowane: oba komponenty kompilują się czysto na JDK 21 + Maven 3.9.15
(`release 21`, compiler plugin 3.13.0).

Hand-offy: rdzeń w `tech-stack.md`; klienci w
`tech-stack-client-web.md`, `tech-stack-client-flutter.md`,
`tech-stack-client-desktop-java.md`. Bootstrap klienta: wskaż ścieżkę hand-offu
i scaffolduj do osobnego podkatalogu pod `clients/`, nie do root.

## Kolejność

1. **Bootstrap rdzenia (Spring).** Hand-off w `tech-stack.md` dotyczy tylko
   rdzenia. Uruchom `/10x-bootstrapper`.

2. **Po bootstrapie rdzenia — ponowne uruchomienie `/10x-tech-stack-selector`
   dla pierwszego klienta.** Wybór stosu klienta to OSOBNA decyzja i osobny
   hand-off (np. `flutter` dla web+desktop z jednego kodu, albo starter JS dla
   weba). Każdy klient = osobny przebieg selektora + osobny bootstrap.
   > To jest celowo odłożone: ten skill zapisuje jeden starter na przebieg, a
   > rdzeń jest fundamentem, od którego zależą wszyscy klienci.

3. Powtórz krok 2 dla kolejnych klientów w miarę wzrostu zakresu. Logika
   domenowa zostaje w rdzeniu Spring, żeby frontendy pozostały wymienne.
