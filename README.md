# ArgoPost — Local/Test development baseline

From a clean clone with Docker Engine/Desktop (Linux containers) and Compose:

```sh
docker compose up --build -d
docker compose ps
```

Web: **https://localhost:5443** · synthetic mail: **http://localhost:8025**.
Trust the installation's generated public CA before logging in.

Read the [portable Compose runbook](docs/runbooks/portable-compose.md) for CA
trust, initialization, environment placeholders, health, persistent volumes and
safe stop/restart. No host Java/Node, personal AWS profile, real S3/Gmail or private
demo artifacts are needed for startup. Keys/passwords are generated per install.

The fresh stack creates no Admin/Staff/Customer accounts or application grants.
Create synthetic Customer accounts through real email verification; Business and
internal onboarding retain their canonical authorization gates. Do not fabricate
roles, custody or financial data with SQL. SYSTEM remains INACTIVE by default.

Current accepted milestone: Personal + Business Booking → Station Intake →
Custody → Tracking, Local/Test pilot with UNPAID; see [S1 evidence](docs/implementation/v1-dual-audience-source-test-execution.md).
Sorting/Manifest (Spec012), Pickup, COD/payment and Production are separate scope.

Release status and tested revisions: [baseline execution](docs/implementation/develop-baseline-release.md).
Team ownership: [V1 team documents](docs/implementation/v1-team/).
The legacy Windows demo launcher uses a separate private environment.
