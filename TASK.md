# DEZSYS_GK71_FLOODMONITOR_REST


**GIT-Repository:** 

## Einführung

Entlang eines Flusses befinden sich mehrere Messstationen eines Hochwasser-Frühwarnsystems. Die Stationen erfassen in regelmäßigen Zeitabständen den Wasserstand, die Fließgeschwindigkeit, die Niederschlagsmenge und weitere Umgebungsdaten.

Die Messdaten sollen über eine standardisierte REST-Schnittstelle für unterschiedliche Zielsysteme zur Verfügung gestellt werden. Abhängig vom Client müssen die Daten sowohl im Format **JSON** als auch im Format **XML** abrufbar sein.

Neben den aktuellen Messwerten sollen historische Daten, Warnmeldungen und einfache statistische Auswertungen angeboten werden. Eine Webanwendung visualisiert die Daten und ermöglicht das Filtern nach Messstation, Zeitraum und Warnstufe.

## Voraussetzungen

- Java und/oder Python Programmierkenntnisse
- Verwendung von Maven oder Gradle
- Verwendung von Git
- Grundlagen dezentraler Systeme, HTTP und REST
- Grundlagen XML und JSON
- Grundlagen HTML, JavaScript und Spring Boot

## Lernziele

Nach Abschluss der Aufgabe können Sie:

- eine REST-Schnittstelle entwerfen und implementieren,
- Ressourcen und URLs sinnvoll strukturieren,
- JSON- und XML-Dokumente erzeugen und verarbeiten,
- HTTP-Methoden, Statuscodes und Content Negotiation korrekt einsetzen,
- Messdaten realistisch simulieren,
- Query-Parameter validieren und Fehler einheitlich zurückgeben,
- eine REST-Schnittstelle mit einem Webclient konsumieren,
- grundlegende Sicherheitsmaßnahmen für APIs umsetzen.

## Szenario und Datenmodell

Das Hochwasser-Frühwarnsystem besteht aus mehreren Messstationen. Jede Station befindet sich an einem bestimmten Flussabschnitt und erzeugt regelmäßig Messungen.

Eine Messung enthält mindestens folgende Daten:

| Eigenschaft | Beschreibung | Einheit |
|---|---|---:|
| `timestamp` | Zeitpunkt der Messung | ISO-8601 |
| `waterLevel` | aktueller Wasserstand | cm |
| `flowRate` | Durchflussmenge | m³/s |
| `rainfall` | Niederschlag der letzten Stunde | mm/h |
| `temperature` | Lufttemperatur | °C |
| `batteryLevel` | Akkustand der Messstation | % |
| `status` | technischer Zustand der Station | Text |
| `warningLevel` | berechnete Hochwasserwarnstufe | Text |

Eine Messstation enthält mindestens `id`, `name`, `river`, `location`, `normalWaterLevel`, `warningWaterLevel`, `criticalWaterLevel` und `isActive`. Die geografische Position besteht aus Breitengrad und Längengrad.

Implementieren Sie mindestens die Klassen `Station`, `Location`, `Measurement`, `WarningLevel` und `StationStatus`.

Verwenden Sie folgende Enums:

- `WarningLevel`: `NORMAL`, `WARNING`, `CRITICAL`, `UNKNOWN`
- `StationStatus`: `ONLINE`, `MAINTENANCE`, `OFFLINE`

Zeitpunkte sollen mit einem geeigneten Java-Zeitdatentyp aus `java.time` gespeichert werden.

## Aufgabenstellung

### 1. Projekt einrichten

Erstellen Sie ein Spring-Boot-Projekt. Empfohlene Abhängigkeiten:

- Spring Web
- Validation
- Jackson XML

Das Projekt muss mit Maven oder Gradle startbar sein:

```text
mvn spring-boot:run
```

oder:

```text
gradle bootRun
```

### 2. Messwerte simulieren

Implementieren Sie einen Simulator, der für jede aktive Station regelmäßig eine neue Messung erzeugt. Die Werte müssen realistisch sein und logisch zusammenpassen:

- Wasserstände dürfen nicht negativ sein.
- Der Akkustand liegt zwischen 0 und 100 Prozent.
- Starker Niederschlag soll den Wasserstand tendenziell erhöhen.
- Ein höherer Wasserstand soll zu einer höheren Durchflussmenge führen.
- Der Wasserstand soll sich zwischen zwei Messungen nicht völlig zufällig verändern.
- Bei sehr niedrigem Akkustand kann der Status auf `MAINTENANCE` wechseln.
- Für deaktivierte oder ausgefallene Stationen werden keine neuen Messungen erzeugt.

Berücksichtigen Sie bei der Erzeugung eines neuen Messwerts den vorherigen Messwert. Das Simulationsintervall soll konfigurierbar sein, zum Beispiel:

```properties
simulation.interval=10000
```

### 3. Warnstufe berechnen

Die Warnstufe wird serverseitig berechnet und darf nicht vom Client vorgegeben werden.

| Bedingung | Warnstufe |
|---|---|
| Wasserstand kleiner als Warnwert | `NORMAL` |
| Wasserstand ab Warnwert | `WARNING` |
| Wasserstand ab kritischem Wert | `CRITICAL` |
| Keine aktuelle oder gültige Messung | `UNKNOWN` |

Optional dürfen Niederschlag und die Geschwindigkeit des Wasseranstiegs berücksichtigt werden. Dokumentieren Sie Ihre verwendeten Regeln.

### 4. REST-Schnittstelle implementieren

Die Basis-URL lautet:

```text
/api/v1
```

Implementieren Sie mindestens folgende Endpunkte:

| Methode | Endpunkt | Beschreibung |
|---|---|---|
| `GET` | `/api/v1/stations` | Alle Messstationen abrufen |
| `GET` | `/api/v1/stations/{stationId}` | Einzelne Station abrufen |
| `GET` | `/api/v1/stations/{stationId}/measurements/latest` | Aktuellsten Messwert abrufen |
| `GET` | `/api/v1/stations/{stationId}/measurements` | Historische Messwerte abrufen |
| `GET` | `/api/v1/alerts` | Aktuelle Warnungen abrufen |

Mögliche Filter für Stationen:

```text
GET /api/v1/stations?river=Donau
GET /api/v1/stations?isActive=true
GET /api/v1/stations?warningLevel=CRITICAL
```

Unterstützte Parameter für historische Messwerte: `from`, `to`, `limit`, `warningLevel`.

```text
GET /api/v1/stations/ST-001/measurements?from=2026-05-01T08:00:00Z&to=2026-05-01T12:00:00Z&limit=100
```

Für Warnungen sollen mindestens `warningLevel` und `river` als optionale Filter unterstützt werden.

Die Statistik enthält mindestens niedrigsten und höchsten Wasserstand, durchschnittlichen Wasserstand, durchschnittliche Durchflussmenge, Summe des Niederschlags, Anzahl der Messungen, Anzahl der Warnungen und Anzahl der kritischen Warnungen.

### 5. JSON und XML: Content Negotiation

Die Schnittstelle liefert standardmäßig JSON und unterstützt XML. Der Client wählt das Format über den `Accept`-Header:

```http
Accept: application/json
```

```http
Accept: application/xml
```

Ohne `Accept`-Header wird JSON geliefert. Bei einem nicht unterstützten Format antwortet die Schnittstelle mit `406 Not Acceptable`.

JSON-Beispiel:

```json
{
  "stationId": "ST-001",
  "timestamp": "2026-05-01T10:15:00Z",
  "waterLevel": 318.4,
  "flowRate": 742.6,
  "rainfall": 12.7,
  "temperature": 14.3,
  "batteryLevel": 87,
  "status": "ONLINE",
  "warningLevel": "WARNING"
}
```

XML-Beispiel:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<measurement>
  <stationId>ST-001</stationId>
  <timestamp>2026-05-01T10:15:00Z</timestamp>
  <waterLevel>318.4</waterLevel>
  <flowRate>742.6</flowRate>
  <rainfall>12.7</rainfall>
  <temperature>14.3</temperature>
  <batteryLevel>87</batteryLevel>
  <status>ONLINE</status>
  <warningLevel>WARNING</warningLevel>
</measurement>
```

JSON und XML müssen dieselben fachlichen Informationen enthalten.

### 6. HTTP-Statuscodes und Fehlerbehandlung

| Situation | Statuscode |
|---|---:|
| Anfrage erfolgreich | `200 OK` |
| Ressource erstellt | `201 Created` |
| Erfolgreich, keine Antwortdaten | `204 No Content` |
| Ungültige Eingabe | `400 Bad Request` |
| Anmeldung erforderlich | `401 Unauthorized` |
| Zugriff nicht erlaubt | `403 Forbidden` |
| Station nicht gefunden | `404 Not Found` |
| Format nicht unterstützt | `406 Not Acceptable` |
| Konflikt | `409 Conflict` |
| Medientyp nicht unterstützt | `415 Unsupported Media Type` |
| Interner Fehler | `500 Internal Server Error` |

Implementieren Sie eine zentrale Fehlerbehandlung, beispielsweise mit `@ControllerAdvice`. Fehler müssen ebenfalls in JSON oder XML ausgegeben werden.

```json
{
  "timestamp": "2026-05-01T10:20:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Station ST-999 wurde nicht gefunden",
  "path": "/api/v1/stations/ST-999"
}
```

Behandeln Sie mindestens unbekannte Stations-IDs, ungültige Zeiträume, `from` nach `to`, ungültige Warnstufen, negative oder zu große `limit`-Werte, ungültige Datumsformate und nicht unterstützte Formate. Geben Sie keine Stacktraces aus.

### 7. Webclient

Erstellen Sie einen einfachen Client mit HTML, CSS und jQuery. Er muss:

- Stationen laden,
- den aktuellen Messwert einer Station anzeigen,
- historische Messwerte tabellarisch darstellen,
- nach Zeitraum und Warnstufe filtern,
- zwischen JSON und XML umschalten,
- Daten periodisch aktualisieren,
- Ladezustände und verständliche Fehlermeldungen anzeigen.

Die Tabelle enthält Station, Zeitpunkt, Wasserstand, Durchflussmenge, Niederschlag, Akkustand, Stationsstatus und Warnstufe. Kennzeichnen Sie `NORMAL` grün, `WARNING` orange, `CRITICAL` rot und `UNKNOWN` grau. Bei XML-Antworten muss der Client das XML auswerten und dieselben Informationen darstellen wie bei JSON.

### 8. Architektur

Teilen Sie die Anwendung mindestens in diese Bereiche:

- **Model:** Datenklassen und Enums
- **Repository:** Zugriff auf Stationen und Messungen
- **Service:** Geschäftslogik, Simulation und Warnstufenberechnung
- **Controller:** HTTP-Anfragen
- **Exception Handling:** einheitliche Fehlerbehandlung
- **Client:** Konsumieren und Darstellen der REST-Daten

Controller dürfen keine umfangreiche Geschäftslogik enthalten.

### 9. Automatisierte Tests

Implementieren Sie mindestens Tests für:

1. Abruf aller Stationen als JSON
2. Abruf aller Stationen als XML
3. Abruf einer existierenden Station
4. Abruf einer unbekannten Station
5. Abruf des aktuellsten Messwerts
6. Filterung nach Zeitraum
7. Ungültigen Zeitraum
8. Warnstufe `NORMAL`
9. Warnstufe `WARNING`
10. Warnstufe `CRITICAL`
11. Nicht unterstütztes Format
12. Maximales Limit

Die Simulation muss für Tests kontrollierbar oder deaktivierbar sein. Tests dürfen nicht zufällig fehlschlagen.

## Erweiterungen

### Verwaltung von Stationen

Ergänzen Sie administrative Endpunkte:

| Methode | Endpunkt |
|---|---|
| `POST` | `/api/v1/stations` |
| `PATCH` | `/api/v1/stations/{stationId}` |
| `DELETE` | `/api/v1/stations/{stationId}` |

Validieren Sie eingehende Daten. Insbesondere muss der kritische Wasserstand größer als der Warnwasserstand sein. Über `PATCH` soll auch `isActive` gesetzt werden können.

### API-Dokumentation

Dokumentieren Sie die API mit OpenAPI. Die Dokumentation muss Endpunkte, Methoden, Parameter, Request- und Response-Bodies, Formate, Statuscodes, Fehlerantworten und Authentifizierung enthalten.

## Technische Mindestanforderungen

- Java und Spring Boot
- Maven oder Gradle
- REST, JSON und XML
- HTML, CSS, JavaScript und jQuery
- Git
- automatisierte Tests

## Abnahmekriterien

Die Aufgabe ist vollständig umgesetzt, wenn:

1. mindestens drei Messstationen vorhanden sind;
2. regelmäßig zusammenhängende Messwerte erzeugt werden;
3. die Warnstufe serverseitig berechnet wird;
4. alle verpflichtenden Endpunkte funktionieren;
5. JSON und XML über `Accept` abrufbar sind;
6. ungültige Anfragen korrekt behandelt werden;
7. passende HTTP-Statuscodes verwendet werden;
8. der Webclient JSON und XML verarbeitet;
9. Daten in einer Tabelle dargestellt werden;
10. Filterung und automatische Aktualisierung funktionieren;
11. automatisierte Tests vorhanden sind;
12. das Projekt mit Maven oder Gradle gestartet werden kann;
13. eine Projektdokumentation vorhanden ist;
14. nachvollziehbare Git-Commits vorhanden sind.

## Bewertung  

*   Gruppengrösse: 1 Person
*   Abgabemodus: per Cheatsheet und kurzes Abgabegespräch
*   Anforderungen **"überwiegend erfüllt"**
    *   Implementierung der REST Schnittstelle für mindestens 3 Messstationen
    *   Darstellung der Rückgabewerte in JSON und XML
    *   Simulation der Messwerte basierend auf Regeln
    *   Umsetzung aller Endpunkte  
    *   Beantwortung der Fragestellungen   
*   Anforderungen **"zur Gänze erfüllt"**
    *   Korrekte Fehlerverarbeitung bei nicht korrekten Abfragen
    *   Filterung der Endpunkte
*  Erweiterte Anforderungen **"überwiegend erfüllt"**
    *   Aufteilung des zentralen Prozesses in mindestens 3 Prozesse (1 Prozess pro Messstation)
    *   Implementierung einer Zentrale, die in regelmässigen Abständen die Werte der Messstationen abfragt
    *   Zusammensetzung der Werte der einzelen Messtationen in eine Datenstruktur
*  Erweiterte Anforderungen **"zur Gänze erfüllt"**
    *   Implementierung eines Sicherheitskonzepts zur Unterscheidung von einfachen Abfragen "Viewer" und der Administration von Parametern als "Admin

## Abgabe

Die Abgabe erfolgt über ein Git-Repository und enthält:

- vollständigen Quellcode,
- `../untitled/README.md`,
- Build-Konfiguration,
- Konfigurationsbeispiel ohne geheime Zugangsdaten,
- automatisierte Tests,
- API-Dokumentation,
- Architektur- und Datenmodellbeschreibung,
- Dokumentation der Simulationsregeln,
- Startanleitung,
- Beispiele für JSON- und XML-Anfragen,
- Screenshots des Webclients,
- kurze Reflexion über aufgetretene Probleme.

## Dokumente und Links

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Building an Application with Spring Boot](https://spring.io/guides/gs/spring-boot/)
- [Spring Initializr](https://start.spring.io/)
- [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
- [Consuming a RESTful Web Service](https://spring.io/guides/gs/consuming-rest/)
- [OpenAPI Introduction](https://www.youtube.com/watch?v=pRS9LRBgjYg)
