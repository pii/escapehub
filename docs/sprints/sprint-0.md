# Sprint 0

| | |
|--|--|
| **Εβδομάδες** | 1–2 |
| **Ημερομηνίες** | 12/11 - 19/11|
| **Scrum Master** | ΠΑΝΑΓΙΩΤΗΣ ΙΩΑΝΝΙΔΗΣ |
| **Sprint Review** | 19/11 |
| **Έκδοση** | `v0.1.0` |

## Sprint goal

> Κοινή κατανόηση των απαιτήσεων και έτοιμη υποδομή: αποθετήριο με κανόνες, CI, πρώτη έκδοση `v0.1.0`, εγκεκριμένο θέμα αποστολής και ιεραρχημένο backlog για το MVP.

## Παραδοτέα του Sprint Review (Ενότητα 6.1)

| Παραδοτέο | Πού | Κατάσταση |
|-----------|-----|-----------|
| Σύνθεση ομάδας και πρόγραμμα εναλλαγής Scrum Master | [team.md](../team.md) | Στο `main` (#1). Λείπουν οι Scrum Masters για τα Sprints 1–4. |
| Branch protection | GitHub settings, οδηγίες στο [CONTRIBUTING](../../CONTRIBUTING.md#προστασία-του-main) | Ολοκληρώθηκε (#2) |
| Pull request template | [.github/pull_request_template.md](../../.github/pull_request_template.md) | Στο `main` (#6) |
| Issue template | [.github/ISSUE_TEMPLATE/user-story.md](../../.github/ISSUE_TEMPLATE/user-story.md) | Στο `main` (#6) |
| Definition of Done | [definition-of-done.md](../definition-of-done.md) | Στο `main` (#6) |
| Αρχικό AI-usage log | [ai-usage-log.md](../ai-usage-log.md) | Στο `main` (#4), ενημερώνεται σε κάθε sprint |
| Ιεραρχημένο backlog με εκτιμήσεις | [product-backlog.md](../product-backlog.md) | Στο `main` (#15). Δεν έχουν καταγραφεί αποτελέσματα planning poker ούτε issues για το Sprint 1. |
| Θέμα και περίγραμμα αποστολής | [mission-outline.md](../mission-outline.md) | Στο `main` (#17) μόνο ως κενό πρότυπο. Εκκρεμούν το θέμα και η έγκριση του PO. |
| Αναφορά διερεύνησης APIs | [api-research.md](../api-research.md) | Στο `main` (#19) |
| Αρχικό αρχιτεκτονικό σχέδιο | [architecture.md](../architecture.md), [ADRs](../adr/README.md) | Στο `main` (#21) |
| Project skeleton | `pom.xml`, `src/` | Στο `main` (#3) |
| Επιτυχές CI | `.github/workflows/ci.yml` | Στο `main` (#7). Το `build` είναι required check του `main`. |
| Επισημασμένη έκδοση `v0.1.0` | tag και GitHub Release | Ολοκληρώθηκε (S0-12). Ο release workflow είναι έτοιμος (#13). |

## Sprint backlog

| ID | Εργασία | Owner | Εκτίμηση | Κατάσταση |
|----|---------|-------|----------|-----------|
| S0-01 | Σύνθεση ομάδας, ρόλοι, πρόγραμμα Scrum Master | ΠΑΝΑΓΙΩΤΗΣ ΙΩΑΝΝΙΔΗΣ | – | Ολοκληρώθηκε (#1). Λείπουν οι Scrum Masters για τα Sprints 1–4. |
| S0-02 | Ρύθμιση GitHub: branch protection, labels, Project board, milestones | ΠΑΝΑΓΙΩΤΗΣ ΙΩΑΝΝΙΔΗΣ | – | Ολοκληρώθηκε (#2). Λείπουν οι ημερομηνίες των milestones Sprint 1–4. |
| S0-03 | PR template, issue template, DoD, CONTRIBUTING (TS-02) | ΚΩΝΣΤΑΝΤΙΝΟΣ ΠΑΠΑΔΟΠΟΥΛΟΣ | 1 SP | Ολοκληρώθηκε (#6) |
| S0-04 | AI-usage log | ΙΩΑΝΝΗΣ ΠΑΠΑΣΤΕΡΓΙΟΥ | – | Ολοκληρώθηκε (#4) |
| S0-05 | Ανάλυση απαιτήσεων και product backlog, planning poker | ΙΩΑΝΝΗΣ ΠΑΠΑΣΤΕΡΓΙΟΥ | – | Ολοκληρώθηκε (#15). Λείπουν τα αποτελέσματα του planning poker και τα issues του Sprint 1. |
| S0-06 | Θέμα και περίγραμμα αποστολής, έγκριση από τον PO | ΓΕΩΡΓΙΟΣ ΕΥΑΓΓΕΛΟΠΟΥΛΟΣ | – | Ολοκληρώθηκε (#17), αλλά το περίγραμμα είναι ακόμη κενό πρότυπο. |
| S0-07 | Διερεύνηση Wikidata και Open Library | ΓΕΩΡΓΙΟΣ ΕΥΑΓΓΕΛΟΠΟΥΛΟΣ | – | Ολοκληρώθηκε (#19) |
| S0-08 | Αρχιτεκτονικό σχέδιο, ADR-0001, ADR-0002 | ΓΡΗΓΟΡΗΣ ΓΟΥΓΑΛΑΚΗΣ | – | Ολοκληρώθηκε (#21) |
| S0-09 | Project skeleton (Spring Boot) | ΠΑΝΑΓΙΩΤΗΣ ΙΩΑΝΝΙΔΗΣ | – | Ολοκληρώθηκε (#3) |
| S0-10 | CI με GitHub Actions και στατική ανάλυση (TS-01) | ΚΩΝΣΤΑΝΤΙΝΟΣ ΠΑΠΑΔΟΠΟΥΛΟΣ | 3 SP | Ολοκληρώθηκε (#7) |
| S0-11 | Έκδοση στο `/actuator/info`, Dockerfile, image στο GHCR, Compose (TS-03) | ΑΘΑΝΑΣΙΟΣ ΜΥΛΩΝΑΣ | 5 SP | Ολοκληρώθηκε (#13). Μένει η πρώτη εκτέλεση με το tag `v0.1.0`. |
| S0-12 | Release `v0.1.0` και προετοιμασία της παρουσίασης | ΓΡΗΓΟΡΗΣ ΓΟΥΓΑΛΑΚΗΣ | – | Ολοκληρώθηκε |

Ο owner είναι ο υπεύθυνος της εργασίας. Στο planning poker και στα code reviews συμμετέχουν όλα τα μέλη.

## Ερωτήματα προς τον Product Owner

1. Εγκρίνεται το Thymeleaf για το frontend ([αρχιτεκτονική, §5](../architecture.md#5-frontend-ανοιχτή-απόφαση));
2. Αρκούν δύο πηγές (Wikidata, Open Library) για το MVP;

## Αποφάσεις του Product Owner

| Θέμα | Απόφαση | Επίπτωση |
|------|---------|----------|
| Login | Ο μηχανισμός login δεν αξιολογείται. Ο παίκτης δίνει μόνο ένα μοναδικό όνομα. | Το US-01 έγινε «Είσοδος με μοναδικό όνομα παίκτη» και το US-02 καταργήθηκε (−3 SP). Ενημερώθηκε η [αρχιτεκτονική](../architecture.md). |
