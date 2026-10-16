import { writeFileSync } from 'node:fs';
import type { Reporter, TestCase, TestResult, FullResult } from '@playwright/test/reporter';

export default class SafeReporter implements Reporter {
  private cases: { name: string; status: string; errors: number }[] = [];
  private diagnostics: { name: string; errors: TestResult['errors'] }[] = [];
  onTestEnd(test: TestCase, result: TestResult) {
    // Never emit error objects, snippets, attachments, request/response or values.
    const entry = { name: test.title, status: result.status, errors: result.errors.length };
    this.cases.push(entry);
    if (result.errors.length) this.diagnostics.push({ name: test.title, errors: result.errors });
    console.log(`${entry.status}: ${entry.name}`);
  }
  onEnd(result: FullResult) {
    const report = { status: result.status, passed: this.cases.filter(c => c.status === 'passed').length,
      failed: this.cases.filter(c => c.status !== 'passed').length, cases: this.cases };
    if (process.env.C04_SAFE_REPORT) writeFileSync(process.env.C04_SAFE_REPORT, JSON.stringify(report, null, 2) + '\n');
    if (process.env.C04_SAFE_REPORT && this.diagnostics.length) writeFileSync(process.env.C04_SAFE_REPORT.replace('safe-browser-results.json', 'browser-private-diagnostics.json'), JSON.stringify(this.diagnostics, null, 2) + '\n');
    console.log(`Browser suite ${report.status}: ${report.passed} passed, ${report.failed} non-pass`);
  }
}
