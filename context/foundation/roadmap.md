# Roadmap — sekwencja bootstrapu

Architektura: wspólny rdzeń (lokalny serwer sync + lokalna baza + model
domenowy) w **Spring/Java**, z wymiennymi klientami (natywny web, Flutter,
natywny desktop w Javie). Stos rdzenia ustalony w `tech-stack.md`.

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
