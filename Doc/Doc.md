# Hochwasser-Frühwarnsystem (FloodMonitor)

REST-API mit Spring Boot, die Pegelmessstationen simuliert, Warnstufen berechnet und die Daten wahlweise als **JSON** oder **XML** ausliefert.

## Inhaltsverzeichnis

1. [Arbeitsschritte](#arbeitsschritte)
2. [Startanleitung](#startanleitung)
3. [Simulationsregeln](#simulationsregeln)
4. [Warnstufen](#warnstufen)
5. [REST-Schnittstellen](#rest-schnittstellen)
6. [Zwischen JSON und XML wechseln](#zwischen-json-und-xml-wechseln)
7. [Beispiele für alle Endpoints](#beispiele-für-alle-endpoints)
8. [Fehlercodes](#fehlercodes)

---
## Arbeitsschritte

Dieser Abschnitt protokolliert, in welcher Reihenfolge das Projekt umgesetzt wurde.

| Nr. | Arbeitsschritt | Beschreibung | Status |
|---|---|---|---|
| 1 | Projektsetup | Spring-Boot-Projekt mit Gradle (Java 17) angelegt, Abhängigkeiten für Web, XML (Jackson) und OpenAPI eingebunden | ✅ |
| 2 | Datenmodell | Klassen `Station` und `Measurement` sowie Enums für Status (`ACTIVE`, `MAINTENANCE`, `DISABLED`, `FAILED`) und Warnstufe (`NORMAL`, `WARNING`, `CRITICAL`, `UNKNOWN`) erstellt | ✅ |
| 3 | Endpoint-Design | REST-Pfade, Filterparameter und Statuscodes festgelegt (`/api/v1/stations`, `/measurements`, `/statistics`, `/alerts`, `/count`) | ✅ |
| 4 | Messwert-Simulator | Simulation aus der vorherigen Messung (Niederschlag, Wasserstand, Durchfluss, Temperatur, Akku, Wartungslogik), konfigurierbar über `simulation.interval` und `simulation.enabled` | ✅ |
| 5 | Warnstufen-Berechnung | Serverseitige Berechnung anhand von Warn- und kritischem Schwellwert, Warnstufe wird pro Messung und an der Station gespeichert | ✅ |
| 6 | REST-Controller | GET-, POST- und PATCH-Endpoints implementiert, inklusive Filtern, Validierung (`from`/`to`, `limit`) und Fehlerbehandlung (400, 404, 409) | ✅ |
| 7 | Content Negotiation | Ausgabe wahlweise als JSON oder XML über den `Accept`-Header, `406` bei nicht unterstützten Formaten | ✅ |
| 8 | Statistik und Warnungen | Auswertung (Min/Max/Durchschnitt, Niederschlagssumme, Warnungsanzahl) und Alert-Endpoint umgesetzt | ✅ |
| 9 | Restrukturierung | Aufteilung in Schichten (Controller, Service, Repository), Historie auf 1000 Messungen pro Station begrenzt | ✅ |
| 10 | Web-Client | Einfache Oberfläche zur Darstellung der REST-Daten mit Format-Umschaltung (JSON/XML) | ✅ |
| 11 | Tests | Tests mit Profil `test` (Simulation aus, feste Testdaten) und manuelle Prüfung mit `curl` | ✅ |
| 12 | Dokumentation | OpenAPI/Swagger-UI eingebunden, diese README mit Beispielen für alle Endpoints geschrieben | ✅ |

## Startanleitung

### Voraussetzungen

- Java 17
- Gradle (Wrapper `../gradlew` liegt im Projekt)

### Anwendung starten

**Terminal:**

```bash
./gradlew bootRun
```

**IntelliJ:** `FloodMonitorApplication` öffnen und auf den grünen Pfeil neben `main` klicken.

**Als JAR:**

```bash
./gradlew clean build
java -jar build/libs/<projektname>-<version>.jar
```

Danach läuft die Anwendung auf `http://localhost:8080`.

| URL | Inhalt |
|---|---|
| `http://localhost:8080/` | Web-Client (Darstellung der REST-Daten) |
| `http://localhost:8080/api/v1/stations` | REST-API |
| `http://localhost:8080/swagger-ui.html` | Swagger UI (OpenAPI-Dokumentation) |
| `http://localhost:8080/v3/api-docs` | OpenAPI-Beschreibung als JSON |

### Konfiguration (`../src/main/resources/application.properties`)

| Property | Standard | Bedeutung |
|---|---|---|
| `simulation.interval` | `10000` | Simulationsintervall in Millisekunden |
| `simulation.enabled` | `true` | Simulation ein- oder ausschalten |

### Tests ausführen

```bash
./gradlew cleanTest test
```

Der Bericht liegt unter `../build/reports/tests/test/index.html`. Die Tests laufen mit dem Profil `test` (`@ActiveProfiles("test")`). Dabei ist die Simulation abgeschaltet und die Testdaten sind fest vorgegeben, sodass die Tests nicht zufällig fehlschlagen. In IntelliJ lassen sie sich per Rechtsklick auf `src/test/java` → *Run 'Tests in ...'* starten.

---

## Simulationsregeln

Alle `simulation.interval` Millisekunden erzeugt der Simulator für jede Station eine neue Messung. Sie wird aus der **vorherigen Messung** berechnet, die Werte springen also nicht zufällig.

| Größe | Regel |
|---|---|
| **Niederschlag** | Klingt pro Messung um 10 % ab. Mit 5 % Wahrscheinlichkeit kommt ein Regenereignis (+5 bis +20 mm/h dazu), maximal 30 mm/h. |
| **Wasserstand** | `neu = alt + Niederschlag × 0,02 − (alt − 1,5) × 0,05 + Rauschen`. Regen erhöht den Pegel, der Rückfluss zieht ihn zum Normalwert 1,5 m, das Rauschen (σ = 0,03) sorgt für kleine Schwankungen. Der Wert ist mindestens 0. |
| **Durchfluss** | `12 × Wasserstand^1,5` in m³/s. Ein höherer Pegel bedeutet also immer mehr Durchfluss. |
| **Wassertemperatur** | Driftet pro Messung leicht (σ = 0,1 °C), begrenzt auf 0 bis 30 °C. |
| **Akkustand** | Sinkt pro Messung um 0,2 bis 0,8 %, bleibt zwischen 0 und 100 %. |
| **Wartung** | Unter 10 % Akku wechselt die Station auf `MAINTENANCE`. Pro Intervall wird der Akku um 20 % geladen, ab 80 % ist die Station wieder `ACTIVE`. Während der Wartung bleiben die Wasserwerte unverändert, die Messung hat die Warnstufe `UNKNOWN`. |
| **Deaktiviert / ausgefallen** | Stationen mit Status `DISABLED` oder `FAILED` erhalten **keine** neuen Messungen. |
| **Historie** | Pro Station werden die letzten 1000 Messungen im Speicher gehalten (In-Memory). |
| **Startwerte** | Wasserstand 0,5 bis 2,5 m, Temperatur 4 bis 20 °C, Akku 60 bis 100 %, kein Niederschlag. |

Stationen, die per `POST` angelegt werden, haben keine Startmessung. Der Simulator erzeugt für sie keine Werte, ihre Warnstufe bleibt `UNKNOWN`.

**Zeitstempel:** UTC im ISO-8601-Format (z. B. `2026-10-12T09:23:56.123Z`).

---

## Warnstufen

Die Warnstufe wird **serverseitig** berechnet und kann vom Client nicht vorgegeben werden (auch nicht per `POST`). Jede Station hat einen Warnwert (Standard `3,0 m`) und einen kritischen Wert (Standard `4,5 m`).

| Bedingung | Warnstufe |
|---|---|
| Status nicht `ACTIVE`, keine Messung, Messung älter als 3 × Intervall oder Wasserstand < 0 | `UNKNOWN` |
| Wasserstand ≥ kritischer Wert | `CRITICAL` |
| Wasserstand ≥ Warnwert | `WARNING` |
| Optional: Niederschlag ≥ 15 mm/h **und** Wasserstand ≥ 80 % des Warnwerts | `WARNING` |
| sonst | `NORMAL` |

Die Warnstufe wird für jede Messung gespeichert (`measurements[].warningLevel`) und zusätzlich als aktuelle Stufe an der Station (`stations[].warningLevel`).

---

## REST-Schnittstellen

Basis-URL: `http://localhost:8080/api/v1`

| Methode | Pfad | Beschreibung |
|---|---|---|
| GET | `/stations` | Alle Stationen. Filter: `river`, `isActive`, `warningLevel`, `id` |
| GET | `/stations/{stationId}` | Einzelne Station |
| POST | `/stations` | Neue Station anlegen |
| PATCH | `/stations/{id}/status?status=...` | Status ändern (`ACTIVE`, `MAINTENANCE`, `DISABLED`, `FAILED`) |
| PATCH | `/stations/{id}/thresholds?warning=...&critical=...` | Schwellwerte ändern (zum Testen von Warnungen) |
| GET | `/stations/{stationId}/measurements/latest` | Aktuellster Messwert |
| GET | `/stations/{stationId}/measurements` | Historische Messwerte. Filter: `from`, `to`, `warningLevel`, `limit` |
| GET | `/stations/{stationId}/statistics` | Statistik. Filter: `from`, `to` |
| GET | `/alerts` | Aktuelle Warnungen. Filter: `warningLevel`, `river` |
| GET | `/count` | Anzahl der Stationen |

**Parameter im Detail**

| Parameter | Format | Hinweis |
|---|---|---|
| `from`, `to` | ISO-8601 in UTC, z. B. `2026-10-12T08:00:00Z` | Grenzen sind inklusive. `from` darf nicht nach `to` liegen (sonst 400). |
| `limit` | Ganzzahl 1 bis 1000 | Liefert die **neuesten** N Messungen. Werte außerhalb des Bereichs ergeben 400. |
| `warningLevel` | `NORMAL`, `WARNING`, `CRITICAL`, `UNKNOWN` | Bei `/alerts` ohne Angabe: alle Stationen mit `WARNING` oder `CRITICAL`. |
| `isActive` | `true` / `false` | Entspricht `status == ACTIVE`. |
| `river` | z. B. `Donau` | Groß-/Kleinschreibung egal. |

---

## Zwischen JSON und XML wechseln

Das Format wird über den HTTP-Header **`Accept`** gewählt (Content Negotiation).

| Header | Ergebnis |
|---|---|
| `Accept: application/json` | JSON (auch Standard, wenn kein Header gesendet wird) |
| `Accept: application/xml` | XML |
| anderes Format (z. B. `application/pdf`) | `406 Not Acceptable` |

```bash
# JSON
curl -H "Accept: application/json" http://localhost:8080/api/v1/stations

# XML
curl -H "Accept: application/xml" http://localhost:8080/api/v1/stations
```

Mit `-i` werden zusätzlich Statuscode und Header angezeigt:

```bash
curl -i -H "Accept: application/xml" http://localhost:8080/api/v1/stations/1
```

Beim `POST` wird das Format des Request-Bodys über `Content-Type` angegeben (`application/json` oder `application/xml`), das Format der Antwort wie gewohnt über `Accept`.

Im Web-Client (`http://localhost:8080/`) wird das Format im Dropdown **Format** umgestellt.

---

## Beispiele für alle Endpoints

### Alle Stationen

```bash
curl http://localhost:8080/api/v1/stations
```

```json
[
  {
    "id": "1",
    "stationName": "Station Vienna",
    "river": "Donau",
    "unit": "m",
    "status": "ACTIVE",
    "warningThreshold": 3.0,
    "criticalThreshold": 4.5,
    "warningLevel": "NORMAL"
  }
]
```

Als XML:

```bash
curl -H "Accept: application/xml" http://localhost:8080/api/v1/stations
```

```xml
<List>
  <item>
    <id>1</id>
    <stationName>Station Vienna</stationName>
    <river>Donau</river>
    <unit>m</unit>
    <status>ACTIVE</status>
    <warningThreshold>3.0</warningThreshold>
    <criticalThreshold>4.5</criticalThreshold>
    <warningLevel>NORMAL</warningLevel>
  </item>
</List>
```

(Zur besseren Lesbarkeit formatiert, der Server liefert das XML in einer Zeile.)

### Stationen filtern

```bash
# nach Fluss
curl "http://localhost:8080/api/v1/stations?river=Donau"

# nur aktive Stationen
curl "http://localhost:8080/api/v1/stations?isActive=true"

# nach Warnstufe
curl "http://localhost:8080/api/v1/stations?warningLevel=CRITICAL"

# kombiniert
curl "http://localhost:8080/api/v1/stations?river=Donau&isActive=true&warningLevel=NORMAL"
```

### Einzelne Station

```bash
curl http://localhost:8080/api/v1/stations/1
curl -H "Accept: application/xml" http://localhost:8080/api/v1/stations/1
```

Unbekannte ID → `404 Not Found`:

```bash
curl -i http://localhost:8080/api/v1/stations/999
```

### Neue Station anlegen (POST)

Mit JSON:

```bash
curl -X POST http://localhost:8080/api/v1/stations \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "id": "6",
    "stationName": "Station Bregenz",
    "river": "Bregenzer Ach",
    "warningThreshold": 2.5,
    "criticalThreshold": 4.0
  }'
```

Mit XML:

```bash
curl -X POST http://localhost:8080/api/v1/stations \
  -H "Content-Type: application/xml" \
  -H "Accept: application/xml" \
  -d '<Station>
        <id>7</id>
        <stationName>Station Villach</stationName>
        <river>Drau</river>
      </Station>'
```

Antworten: `201 Created` mit der angelegten Station, `409 Conflict` wenn die ID schon existiert. Eine vom Client mitgeschickte `warningLevel` wird ignoriert und serverseitig neu berechnet.

### Status einer Station ändern

```bash
# deaktivieren: es werden keine neuen Messungen mehr erzeugt
curl -X PATCH "http://localhost:8080/api/v1/stations/2/status?status=DISABLED"

# wieder aktivieren
curl -X PATCH "http://localhost:8080/api/v1/stations/2/status?status=ACTIVE"
```

### Schwellwerte ändern (Warnungen auslösen)

```bash
# WARNING auslösen
curl -X PATCH "http://localhost:8080/api/v1/stations/1/thresholds?warning=0.1&critical=10"

# CRITICAL auslösen
curl -X PATCH "http://localhost:8080/api/v1/stations/1/thresholds?warning=0.1&critical=0.2"

# zurücksetzen
curl -X PATCH "http://localhost:8080/api/v1/stations/1/thresholds?warning=3.0&critical=4.5"
```

### Aktuellster Messwert

```bash
curl http://localhost:8080/api/v1/stations/1/measurements/latest
```

```json
{
  "timestamp": "2026-10-12T09:23:56.123456Z",
  "waterLevel": 1.45,
  "waterTemperature": 13.3,
  "waterFlow": 21.0,
  "precipitation": 0.0,
  "batteryLevel": 69.1,
  "warningLevel": "NORMAL"
}
```

Als XML:

```bash
curl -H "Accept: application/xml" http://localhost:8080/api/v1/stations/1/measurements/latest
```

```xml
<Measurement>
  <timestamp>2026-10-12T09:23:56.123456Z</timestamp>
  <waterLevel>1.45</waterLevel>
  <waterTemperature>13.3</waterTemperature>
  <waterFlow>21.0</waterFlow>
  <precipitation>0.0</precipitation>
  <batteryLevel>69.1</batteryLevel>
  <warningLevel>NORMAL</warningLevel>
</Measurement>
```

### Historische Messwerte

```bash
# alle gespeicherten Messwerte
curl http://localhost:8080/api/v1/stations/1/measurements

# die neuesten 10
curl "http://localhost:8080/api/v1/stations/1/measurements?limit=10"

# Zeitraum
curl "http://localhost:8080/api/v1/stations/1/measurements?from=2026-10-12T08:00:00Z&to=2026-10-12T12:00:00Z"

# nur kritische Messungen
curl "http://localhost:8080/api/v1/stations/1/measurements?warningLevel=CRITICAL"

# alle Filter kombiniert, als XML
curl -H "Accept: application/xml" \
  "http://localhost:8080/api/v1/stations/1/measurements?from=2026-10-12T08:00:00Z&to=2026-10-12T12:00:00Z&warningLevel=WARNING&limit=100"
```

Fehlerfälle:

```bash
# from nach to -> 400
curl -i "http://localhost:8080/api/v1/stations/1/measurements?from=2026-10-12T12:00:00Z&to=2026-10-12T08:00:00Z"

# limit über dem Maximum (1000) -> 400
curl -i "http://localhost:8080/api/v1/stations/1/measurements?limit=1001"

# ungültiges Datum -> 400
curl -i "http://localhost:8080/api/v1/stations/1/measurements?from=gestern"
```

### Statistik

```bash
curl http://localhost:8080/api/v1/stations/1/statistics
curl "http://localhost:8080/api/v1/stations/1/statistics?from=2026-10-12T08:00:00Z&to=2026-10-12T12:00:00Z"
```

```json
{
  "stationId": "1",
  "measurementCount": 120,
  "minWaterLevel": 1.12,
  "maxWaterLevel": 3.4,
  "avgWaterLevel": 1.68,
  "avgWaterFlow": 26.3,
  "totalPrecipitation": 42.5,
  "warningCount": 5,
  "criticalCount": 0
}
```

`warningCount` zählt Messungen mit `WARNING` oder `CRITICAL`, `criticalCount` nur `CRITICAL`. Der Niederschlag ist die Summe der mm/h-Werte aller Messungen im Zeitraum. Ohne Messungen sind alle Werte 0.

### Warnungen

```bash
# alle aktuellen Warnungen (WARNING und CRITICAL)
curl http://localhost:8080/api/v1/alerts

# nur kritische
curl "http://localhost:8080/api/v1/alerts?warningLevel=CRITICAL"

# nur an der Donau
curl "http://localhost:8080/api/v1/alerts?river=Donau"

# als XML
curl -H "Accept: application/xml" http://localhost:8080/api/v1/alerts
```

### Anzahl der Stationen

```bash
curl http://localhost:8080/api/v1/count
```

---

## Fehlercodes

| Status | Bedeutung | Beispiel |
|---|---|---|
| `200 OK` | Erfolgreich | normale Abfragen |
| `201 Created` | Station angelegt | `POST /stations` |
| `400 Bad Request` | Ungültige Parameter | `from` nach `to`, `limit` > 1000, ungültiges Datum |
| `404 Not Found` | Station oder Messung nicht vorhanden | `GET /stations/999` |
| `406 Not Acceptable` | Nicht unterstütztes Format | `Accept: application/pdf` |
| `409 Conflict` | ID existiert bereits | `POST` mit vorhandener ID |