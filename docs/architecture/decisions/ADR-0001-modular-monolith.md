# ADR-0001: Modular Monolith for P0

- **Status:** ACCEPTED_BY_REPORT (Chương 4 §4.1.2)
- **Context:** Nine Bounded Contexts, high consistency money-stock-shipment, group three, prototype Docker Compose.
- **Decision:** One Spring Boot backend artifact using Package by Domain, public/internal API; one PostgreSQL instance with ownership schemas; frontend web/mobile deploy separately.
- **Consequences:** Simpler local ACID transactions/deploy; boundary tests required; shared DB and optional cross-schema FK still create extraction coupling.
- **Not selected:** Microservices + Saga in P0. Not a blanket claim Monolith always superior.
- **Validate:** one artifact starts; architecture tests fail on internal imports; end-to-end atomic confirm.
