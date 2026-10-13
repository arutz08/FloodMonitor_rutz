## Fragestellungen

### Was sind die Hauptmerkmale von JSON?

- Textbasiertes, leichtgewichtiges Datenformat
- Aufbau aus Schlüssel-Wert-Paaren (`{}`) und Arrays (`[]`)
- Datentypen: String, Zahl, Boolean, `null`, Objekt, Array
- Kompakt und gut lesbar
- Nativ in JavaScript nutzbar, in fast allen Sprachen leicht zu parsen
- Keine Kommentare, kein Schema zwingend vorgeschrieben

### Was sind die Hauptmerkmale von XML?

- Auszeichnungssprache (Markup) mit öffnenden und schließenden Tags (`<station>...</station>`)
- Hierarchische Baumstruktur mit genau einem Wurzelelement
- Unterstützt Attribute, Namespaces und Kommentare
- Validierung über XSD/DTD möglich
- Ausführlicher (mehr Overhead) als JSON
- Selbstbeschreibend und plattformunabhängig, verbreitet in Enterprise-Umgebungen

### Wie unterscheidet die Applikation, ob JSON oder XML zurückgegeben wird?

- Über **Content Negotiation** mit dem HTTP-Header `Accept`
- `Accept: application/json` → JSON (auch Standard ohne Header)
- `Accept: application/xml` → XML
- Nicht unterstütztes Format (z. B. `application/pdf`) → `406 Not Acceptable`
- Beim `POST` bestimmt `Content-Type` das Format des Request-Bodys
- Spring wählt dazu passende `HttpMessageConverter` (Jackson JSON/XML)

### Was zeichnet ein REST Interface aus und warum ist es so verbreitet?

**Merkmale:**

- Ressourcenorientiert: Ressourcen werden über URIs identifiziert (`/stations/1`)
- Nutzung der Standard-HTTP-Methoden (GET, POST, PUT, PATCH, DELETE)
- Zustandslos (Stateless): jeder Request enthält alle nötigen Informationen
- Unterschiedliche Darstellungen derselben Ressource (JSON, XML)
- Aussagekräftige HTTP-Statuscodes (200, 201, 404, ...)
- Client-Server-Trennung, Cachebarkeit, einheitliche Schnittstelle

**Warum Standard:**

- Baut auf bestehendem HTTP auf, keine zusätzliche Infrastruktur nötig
- Einfach zu verstehen und zu testen (`curl`, Browser, Swagger)
- Plattform- und sprachunabhängig
- Gut skalierbar (zustandslos, cachebar)
- Lose Kopplung zwischen Client und Server
- Breite Unterstützung durch Frameworks und Tools

### Unterschied zwischen GET, POST, PUT und PATCH

| Methode | Zweck | Body | Idempotent | Sicher (safe) |
|---|---|---|---|---|
| **GET** | Ressource lesen | nein | ja | ja |
| **POST** | Neue Ressource anlegen | ja | nein | nein |
| **PUT** | Ressource vollständig ersetzen (oder anlegen) | ja | ja | nein |
| **PATCH** | Ressource teilweise ändern | ja | meist nein* | nein |

\* hängt von der Implementierung ab

### Neue Request-Methode / neuen Pfad hinzufügen: Was muss ich ändern?

1. **Controller:** neue Methode mit `@GetMapping` / `@PostMapping` usw., Pfad, Parameter und Statuscodes
2. **Service:** Geschäftslogik ergänzen
3. **Repository:** falls neue Datenzugriffe nötig sind
4. **Model/DTO:** falls neue Datenstrukturen nötig sind
5. **Fehlerbehandlung:** Validierung und Exceptions (400, 404, ...)
6. **OpenAPI-Annotationen:** `@Operation`, `@ApiResponse` für die Swagger-Doku
7. **Tests:** neue Testfälle ergänzen
8. **Dokumentation:** README und Web-Client anpassen, falls betroffen

### Kann man OpenAPI und Swagger vergleichen?

**Teilweise.** Es sind keine Konkurrenten, sondern gehören zusammen und liegen auf unterschiedlichen Ebenen.

| | OpenAPI | Swagger |
|---|---|---|
| **Was ist es?** | Spezifikation (Standard) | Toolset rund um OpenAPI |
| **Aufgabe** | Beschreibt REST-APIs maschinenlesbar (JSON/YAML) | Erstellt, visualisiert und testet APIs |
| **Beispiele** | `/v3/api-docs` | Swagger UI, Swagger Editor, Swagger Codegen |
| **Verwaltung** | OpenAPI Initiative (Linux Foundation) | SmartBear |

**Fazit:**

- OpenAPI = der Standard (das *Was*)
- Swagger = die Werkzeuge, die den Standard nutzen (das *Wie*)
- Früher hieß die Spezifikation selbst „Swagger Specification“, seit Version 3.0 heißt sie OpenAPI
- Ein direkter Vergleich auf gleicher Ebene ist daher nicht sinnvoll, ein Vergleich von Rolle und Zweck schon

### Wo liegt im Code-Snippet der Fehler?

```java
@GetMapping("/stations/add")
public String createStation(@RequestParam String name, @RequestParam double warning) {
    stationService.save(new Station(name, warning));
    return "Station successfully created!";
}
```

**Hauptfehler: Falsche HTTP-Methode (`GET` statt `POST`)**

- GET ist **safe** und **idempotent**, darf also keine Daten verändern
- Ein Anlegen verändert den Serverzustand, dafür ist `POST` vorgesehen
- Browser, Crawler oder Prefetching können GET-Requests unbeabsichtigt auslösen und so Stationen erzeugen
- GET-Requests werden gecacht, geloggt und stehen in der URL (Daten sichtbar in Verlauf und Logs)

**Weitere Fehler:**

- Pfad enthält ein Verb (`/add`), REST nutzt stattdessen Substantive: `POST /stations`
- Daten als `@RequestParam` statt als Request-Body (`@RequestBody`)
- Rückgabe nur als String, besser: das angelegte Objekt zurückgeben
- Kein korrekter Statuscode, richtig wäre `201 Created`
- Keine Validierung und kein `409 Conflict` bei doppelter ID

**Korrekte Variante:**

```java
@PostMapping("/stations")
public ResponseEntity<Station> createStation(@RequestBody Station station) {
    Station saved = stationService.save(station);
    return ResponseEntity.status(HttpStatus.CREATED).body(saved);
}
```