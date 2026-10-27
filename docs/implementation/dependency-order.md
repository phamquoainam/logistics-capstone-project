# Canonical V3 dependency navigation

[Roadmap](roadmap-v3.md) and [register](../reconciliation/v3/spec-register.json) are the single current dependency source. [Delivery principles](delivery-principles-v3.md) supersede slice-first development descriptions. Legacy numeric feature order retained only in [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/docs/implementation/dependency-order.md).

Do not map legacy005 to canonical005; canonical003–030 IDs have retirement aliases. Minimum contract order and full feature acceptance are different; workflow calls public APIs/local DB transaction, never finance/orders internals or queue/Redis replacement for invariants.
