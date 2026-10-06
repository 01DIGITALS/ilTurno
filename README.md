# ilTurno

App Android per gestire i turni di un ristorante, destinata all'uso personale. Sviluppatore: **LouisBigDev**.

## Versione 1.2

- Apertura sul giorno odierno, anche senza persone.
- Nominativi in ordine alfabetico.
- Reparti configurabili: aggiunta, rinomina, rimozione reversibile e storico conservato.
- Giorno e Settimana con tutti i reparti; vista semplice Pranzo/Cena quando la suddivisione è disattivata o non ci sono reparti attivi.
- PDF A4 orizzontale: sette giorni sulle righe, colonne Pranzo/Cena per reparto, carattere adattivo. Verificato un singolo foglio con quattro reparti e dieci persone per servizio nel weekend. Se il contenuto supera lo spazio leggibile, continua su altre pagine senza omettere nomi.
- Excel con date numeriche, stampa A4 orizzontale e adattamento a una pagina.
- Promemoria locali da giovedì a domenica alle 12 e alle 19 finché i servizi sono coperti; controllo finale domenica alle 23 anche a settimana completa.
- Info con versione installata e ringraziamento: **Un ringraziamento particolare a GioGio!**

## Installazione e aggiornamenti

Gli APK sono negli [allegati dell'ultima release](https://github.com/01DIGITALS/ilTurno/releases/latest). Il repository è privato: accedere a GitHub con un account autorizzato.

Compatibilità minima: Android 8.0 / API 26. Target Android 16 / API 36.

Per aggiornare conservando i dati, installare il nuovo APK sopra quello precedente: stesso identificativo `it.sanges.ilturno` e stessa firma. Disinstallare cancella i dati locali.

La firma delle build personali viene conservata localmente; le chiavi non appartengono al repository. Le release usano tag coerenti con `versionName` in `app/build.gradle.kts`, e `versionCode` aumenta a ogni release.

## Sviluppo

Android nativo: Kotlin, Compose, Room. Dati sul dispositivo; nessun account applicativo, pubblicità o sincronizzazione cloud.

Aprire in Android Studio con Android SDK 37 e JDK compatibile con il wrapper Gradle.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:assembleDebugAndroidTest :app:connectedDebugAndroidTest
```

Gli script in `tool/` supportano il collaudo locale e l'ispezione indipendente degli export. Usare emulatori dedicati: i test UI cancellano i dati del dispositivo di collaudo.

Gli schemi Room V1 e V2 sono in `app/schemas`. La migrazione conserva le assegnazioni precedenti in “Senza reparto”.

