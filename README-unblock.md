# ArgoPost — manifest handoff for locally blocked CSKH-IMPLEMENT-01

This ZIP provides the candidate manifest with the exact Owner-approved SHA-256 and the implementation handoff. Download/unpack into a temporary folder before replacing files; do not automatically overwrite repository files.

- Original repository manifest SHA-256: 370f965872c4bb3997cc868ebb6d89c945a88035eea484e07f3501c1e9b798cb
- Approved replacement SHA-256: acf43f4ed06ac77bf3c405376adc2d7d9e0aa7db2598782b59eef35f4971ebb4

1. Verify `D:\TLCN\e-logistic\docs\implementation\cskh-staff-authority-technical-manifest.json` is still the original hash. If not, STOP and report the drift.
2. Preserve the original file as a separate backup outside the repository.
3. Copy `docs/implementation/cskh-staff-authority-technical-manifest.json` from this package to the same relative path in the existing repository.
4. Verify file hash equals approved hash using `Get-FileHash <path> -Algorithm SHA256`.
5. Read `docs/implementation/cskh-staff-authority-implementation-handoff.md`. This handoff was generated at a prior review checkpoint; its 'approval required' line refers to pre-approval state. The Owner has since approved exactly the replacement SHA.
6. Re-run non-destructive preflight and continue source/test only. No demo deployment, privileges, or runtime activation are authorized.

No database or repository modifications were made by preparation of this archive.
