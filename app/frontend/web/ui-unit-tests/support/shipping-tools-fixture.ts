import type { PilotCatalog } from '../../src/customer/client';
export const toolsCatalog: PilotCatalog = {
  simulation: true, serviceCode: 'ROAD_STANDARD', quoteEnabled: true, bookingEnabled: true,
  coverage: { datasetVersion: 'ARGO-SIM-COVERAGE-1', namespace: 'ARGO_SIMULATED_ADMIN_1', provenance: 'SIMULATION', effectiveFrom: '2026-11-01T00:00:00Z',
    areas: ['A1', 'A2', 'B1'].map(k => ({ code: 'SIM-' + k, region: 'SIM-REGION-' + k[0], label: 'Synthetic ' + k, dropOffCode: 'SIM-DROP-' + k })),
    routes: [['A1', 'A2'], ['A2', 'A1'], ['A1', 'B1'], ['B1', 'A1']].map(([a, b]) => ({ datasetVersion: 'ARGO-SIM-COVERAGE-1', originCode: 'SIM-' + a, destinationCode: 'SIM-' + b, classification: a[0] === b[0] ? 'INTRA' : 'INTER', dropOffCode: 'SIM-DROP-' + a })) },
  tariff: { version: 'ARGO-ROAD-TARIFF-1', intraFirstVnd: 25000, intraStepVnd: 3000, interFirstVnd: 38000, interStepVnd: 5000, stepGrams: 500, volumetricDivisor: 5000, maxParcels: 5, maxActualGrams: 20000, maxDimensionMm: 1000, maxGirthMm: 2000, ttlSeconds: 900, taxTreatment: 'SIMULATION_NOT_TAX_INVOICE' },
  paymentMethod: 'CASH_ON_HANDOVER', payer: 'SENDER', paymentCollectionEnabled: false,
};
