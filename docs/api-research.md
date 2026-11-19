# Αναφορά διερεύνησης δημόσιων APIs

> Τα παραδείγματα αποκρίσεων προέρχονται από πραγματικές κλήσεις στις **19/11/2026**. Τα όρια χρήσης είναι από την επίσημη τεκμηρίωση της ίδιας ημέρας. Τα όρια μπορεί να αλλάξουν, οπότε τα ξαναελέγχουμε πριν από κάθε release.

## Σύνοψη

| | Wikidata | Open Library |
|--|----------|--------------|
| Περιεχόμενο | Δομημένα δεδομένα για πρόσωπα, τόπους, έργα και τις σχέσεις τους | Βιβλία, εκδόσεις, συγγραφείς |
| Χρήση στην αποστολή | Πρόσωπα, τόποι και σχέσεις μεταξύ τους (π.χ. γονείς, σύζυγος, τοποθεσία) | Αναγνώριση βιβλίων και συγγραφέων |
| Μορφή | JSON (Action API, REST), SPARQL JSON | JSON |
| Αυθεντικοποίηση | Δεν απαιτείται για ανάγνωση | Δεν απαιτείται |
| Υποχρεωτικό User-Agent | Ναι (User-Agent policy της Wikimedia) | Συνιστάται, με email. Τριπλασιάζει το όριο. |
| Κύριο όριο | SPARQL: 60 s ανά ερώτημα, 60 s επεξεργασίας ανά λεπτό ανά client | 1 αίτημα/s χωρίς ταυτοποίηση, 3 αιτήματα/s με ταυτοποίηση |
| Σύνδεση των δύο πηγών | Ιδιότητα **P648** (Open Library ID) | Πεδίο `remote_ids.wikidata` στους συγγραφείς |

## 1. Wikidata

### Δυνατότητες

Κάθε οντότητα έχει σταθερό αναγνωριστικό (Q-id), π.χ. `Q47152` για τη Mary Shelley. Κάθε ιδιότητα έχει P-id. Οι ιδιότητες που μας ενδιαφέρουν:

| Ιδιότητα | Σημασία |
|----------|---------|
| P22 | πατέρας |
| P25 | μητέρα |
| P26 | σύζυγος |
| P50 | συγγραφέας |
| P577 | ημερομηνία δημοσίευσης |
| P800 | αξιόλογο έργο |
| P648 | Open Library ID |

### Endpoints

| Σκοπός | Αίτημα |
|--------|--------|
| Αναζήτηση με όνομα | `GET https://www.wikidata.org/w/api.php?action=wbsearchentities&search={όρος}&language=en&format=json&limit=5` |
| Λήψη οντοτήτων (έως 50 ανά αίτημα) | `GET https://www.wikidata.org/w/api.php?action=wbgetentities&ids=Q47152&props=labels\|descriptions\|claims&languages=en\|el&format=json` |
| Λήψη οντότητας (REST) | `GET https://www.wikidata.org/w/rest.php/wikibase/v1/entities/items/Q47152` |
| Ερώτημα σχέσεων | `GET https://query.wikidata.org/sparql?query={SPARQL}` με `Accept: application/sparql-results+json` |

### Παράδειγμα: αναζήτηση

`wbsearchentities` με `search=Mary Shelley` (περικομμένη απόκριση):

```json
{
  "searchinfo": { "search": "Mary Shelley" },
  "search": [
    {
      "id": "Q47152",
      "concepturi": "http://www.wikidata.org/entity/Q47152",
      "label": "Mary Shelley",
      "description": "English writer (1797–1851)",
      "match": { "type": "label", "language": "en", "text": "Mary Shelley" }
    }
  ]
}
```

Η αναζήτηση για «Villa Diodati» επέστρεψε `Q670519`, με περιγραφή «mansion in Cologny in the canton of Geneva, Switzerland».

### Παράδειγμα: σχέσεις με SPARQL

```sparql
SELECT DISTINCT ?relLabel ?o ?oLabel WHERE {
  VALUES ?rel { wd:P22 wd:P25 wd:P26 wd:P800 }
  ?rel wikibase:directClaim ?prop .
  wd:Q47152 ?prop ?o .
  SERVICE wikibase:label { bd:serviceParam wikibase:language "en". }
}
LIMIT 50
```

Αποτέλεσμα:

| Σχέση | Τιμή | Q-id |
|-------|------|------|
| father | William Godwin | Q188569 |
| mother | Mary Wollstonecraft | Q101638 |
| spouse | Percy Bysshe Shelley | Q93343 |
| notable work | Frankenstein; or, The Modern Prometheus | Q150827 |
| notable work | The Last Man | Q2323961 |

Η μορφή της απόκρισης SPARQL είναι `{"head": {"vars": [...]}, "results": {"bindings": [{"var": {"type": "uri|literal", "value": "..."}}]}}`. Ο adapter τη μετατρέπει σε δικά μας DTOs.

### Όρια και όροι χρήσης

- **SPARQL (Query Service):**
  - Κάθε ερώτημα τερματίζεται στα **60 δευτερόλεπτα**.
  - Κάθε client (User-Agent και IP) έχει **60 δευτερόλεπτα επεξεργασίας ανά 60 δευτερόλεπτα**.
  - Επιτρέπονται έως **30 ερωτήματα με σφάλμα ανά λεπτό**.
  - Όποιος ξεπερνά τα όρια δέχεται throttling, και αν συνεχίσει μπορεί να αποκλειστεί προσωρινά.
- **User-Agent:** Όποιος δεν τηρεί την [User-Agent policy](https://foundation.wikimedia.org/wiki/Policy:User-Agent_policy) μπορεί να αποκλειστεί εντελώς. Στέλνουμε π.χ. `EscapeHub/0.1.0 (https://github.com/pii/escapehub; <email ομάδας>)`.
- **Action API:** Κάνουμε τα αιτήματα σειριακά, όχι παράλληλα, και ομαδοποιούμε έως 50 ids ανά `wbgetentities`.
- **Άδεια:** Τα δομημένα δεδομένα του Wikidata διατίθενται με CC0.

### Παρατηρήσεις από τις δοκιμές

- **Διπλότυπα:** Ερώτημα για το *Frankenstein* χωρίς `DISTINCT` επέστρεψε **την ίδια γραμμή 41 φορές**, επειδή το αντικείμενο έχει πολλές τιμές τίτλου (P1476). Γι' αυτό όλα τα ερωτήματα θα έχουν `DISTINCT` και `LIMIT`.
- **Σιωπηλή αποτυχία:** Ένα λάθος ερώτημα επέστρεψε κενό αποτέλεσμα με HTTP 200, όχι σφάλμα. Για κάθε ερώτημα που χρησιμοποιούμε θα υπάρχει έλεγχος με γνωστό αναμενόμενο αποτέλεσμα (fixture).
- **Πολλαπλές αντιστοιχίσεις:** Το *Frankenstein* (Q150827) έχει τρία Open Library IDs (P648): `OL450063W`, `OL450065W`, `OL25595002W`. Ο κώδικας δεν πρέπει να υποθέτει μία τιμή ανά ιδιότητα.
- **Μεταβλητότητα:** Τα Q-ids είναι σταθερά, αλλά οι τιμές των ιδιοτήτων μπορούν να αλλάξουν από οποιονδήποτε συντάκτη. Γι' αυτό χρειάζονται snapshots.

## 2. Open Library

### Endpoints

| Σκοπός | Αίτημα |
|--------|--------|
| Αναζήτηση βιβλίων | `GET https://openlibrary.org/search.json?q={όρος}&fields=key,title,author_name,author_key,first_publish_year&limit=5` |
| Έργο (work) | `GET https://openlibrary.org/works/{OLID}.json` |
| Συγγραφέας | `GET https://openlibrary.org/authors/{OLID}.json` |

Το `search.json` δέχεται τις παραμέτρους `fields` (επιλογή πεδίων) και `limit`. Η απόκριση περιέχει και `documentation_url` προς την τεκμηρίωση.

### Παράδειγμα: αναζήτηση

`search.json?q=the modern prometheus&fields=...&limit=2`:

```json
{
  "numFound": 472,
  "start": 0,
  "documentation_url": "https://openlibrary.org/dev/docs/api/search",
  "q": "the modern prometheus",
  "docs": [
    {
      "key": "/works/OL450063W",
      "title": "Frankenstein; or, The Modern Prometheus",
      "author_name": ["Mary Shelley"],
      "author_key": ["OL25342A"],
      "first_publish_year": 1818
    },
    {
      "key": "/works/OL25595002W",
      "title": "Mary Shelley's Frankenstein; or, the Modern Prometheus (1818 text)",
      "author_name": ["Mary Shelley"],
      "author_key": ["OL25342A"],
      "first_publish_year": 1818
    }
  ]
}
```

### Παράδειγμα: συγγραφέας

`authors/OL25342A.json` (επιλεγμένα πεδία):

```json
{
  "name": "Mary Shelley",
  "birth_date": "30 August 1797",
  "death_date": "1 February 1851",
  "remote_ids": { "wikidata": "Q47152", "viaf": "6293", "project_gutenberg": "61" }
}
```

### Όρια και όροι χρήσης

- **Ρυθμός:** **1 αίτημα/s** χωρίς ταυτοποίηση. **3 αιτήματα/s** με User-Agent που περιέχει όνομα εφαρμογής και email ή τηλέφωνο, π.χ. `MyLibraryApp (contact@example.org)`.
- **Οδηγίες:**
  - Κάνουμε αιτήματα για λογαριασμό πραγματικών χρηστών και κρατάμε cache.
  - Δεν κάνουμε scraping σελίδων HTML.
  - Δεν μοιράζουμε αιτήματα σε πολλές IP.
  - Δεν κάνουμε εκατοντάδες αιτήματα για μεμονωμένα βιβλία· χρησιμοποιούμε το `search.json`.
  - Για μαζική λήψη υπάρχουν τα μηνιαία data dumps.
- **Συνέπειες παραβίασης:** «aggressive rate limiting or blocking».

### Παρατηρήσεις από τις δοκιμές

- **Πολλά αποτελέσματα:** Η αναζήτηση του υποτίτλου επέστρεψε **472 αποτελέσματα**. Ο adapter ταξινομεί και φιλτράρει, π.χ. με `author_key` και `first_publish_year`.
- **Ασυνεπή πεδία:** Η ίδια εγγραφή δίνει `first_publish_year: 1818` στο `search.json`, ενώ το `works/OL450063W.json` δίνει `first_publish_date: "2006"`. Η σωστή απάντηση ενός γρίφου **δεν** μπορεί να υπολογίζεται από τη ζωντανή απόκριση.
- **Ανύπαρκτο αναγνωριστικό:** Επιστρέφει **HTTP 404** και ο adapter το αντιστοιχίζει σε «δεν βρέθηκε».

## 3. Σύνδεση των δύο πηγών

Οι δύο πηγές συνδέονται άμεσα, κάτι που επιτρέπει τη «μεταφορά ευρήματος» που ζητά το MVP-6:

```mermaid
flowchart LR
    OL1["Open Library search.json<br/>«The Modern Prometheus»"] -->|author_key OL25342A| OL2["Open Library author<br/>remote_ids.wikidata"]
    OL2 -->|Q47152| WD1["Wikidata Q47152<br/>Mary Shelley"]
    WD1 -->|P800| WD2["Q150827 Frankenstein<br/>P577 = 1818"]
    WD2 -->|P648| OL3["OL450063W ..."]
```

Επαληθευμένες αντιστοιχίσεις μέσω P648:

| Wikidata | Open Library |
|----------|--------------|
| Q47152 Mary Shelley | OL25342A |
| Q93343 Percy Bysshe Shelley | OL20743A |
| Q5679 Lord Byron | OL113143A |
| Q150827 Frankenstein | OL450063W, OL450065W, OL25595002W |

## 4. Χειρισμός σφαλμάτων και timeouts (πρόταση)

| Θέμα | Πρόταση |
|------|---------|
| Connect timeout | 2 s |
| Read timeout | 5 s (REST/Action API), 10 s (SPARQL). Πολύ κάτω από το όριο των 60 s του WDQS, ώστε ο παίκτης να μην περιμένει. |
| Επαναλήψεις | Έως 2, με εκθετική αναμονή, μόνο για GET που απέτυχαν με timeout ή 5xx |
| HTTP 429 | Τήρηση του `Retry-After` αν υπάρχει. Αλλιώς fallback σε snapshot. |
| Ρυθμός | Rate limiter ανά πηγή: Open Library ≤ 1 αίτημα/s, Wikidata σειριακά αιτήματα |
| Cache | Cache στη μνήμη με TTL (π.χ. 1 ώρα) ανά URL αιτήματος |
| Απρόσμενη απόκριση | Έλεγχος υποχρεωτικών πεδίων κατά τη μετατροπή. Αν λείπουν, ελεγχόμενο σφάλμα και όχι 500. |
| Αποτυχία πηγής | Fallback στο snapshot και ένδειξη στο UI: «εμφανίζονται αποθηκευμένα δεδομένα» |
| Μήνυμα προς τον χρήστη | ProblemDetail (RFC 9457), με φιλικό μήνυμα στα ελληνικά |

Υλοποίηση: `RestClient` του Spring με ρυθμισμένο `ClientHttpRequestFactory` για τα timeouts, και Resilience4j (Retry, RateLimiter, προαιρετικά CircuitBreaker). Το User-Agent ορίζεται στη ρύθμιση `escapehub.external.user-agent`.

Για τη στρατηγική snapshots βλ. [ADR-0002](adr/0002-external-data-snapshots.md).

## 5. Υποψήφια τρίτη πηγή (εκτός MVP)

Το **Wikipedia REST API** (`/api/rest_v1/page/summary/{τίτλος}`) δίνει σύντομη περιγραφή και εικόνα για κάθε πρόσωπο ή τόπο. Θα μπορούσε να εμπλουτίσει την παρουσίαση των στοιχείων. Δεν έχει διερευνηθεί αναλυτικά.

## 6. Ανοιχτά θέματα

- Το email επικοινωνίας στο User-Agent: χρειαζόμαστε κοινό email ομάδας, όχι προσωπικό.
- Επιβεβαίωση ότι η χρήση μέσα από ένα εκπαιδευτικό παιχνίδι συμφωνεί με τις οδηγίες του Open Library. Η χρήση μας είναι αιτήματα για λογαριασμό πραγματικών χρηστών, με χαμηλό όγκο και cache.
