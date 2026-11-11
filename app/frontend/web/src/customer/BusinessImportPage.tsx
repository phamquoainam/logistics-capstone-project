import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { customerClient, CustomerApiError } from './client';
import type { BusinessImportJob, BusinessPreparationOperation } from './client';
import type { BusinessContext } from './business-data';
import { applyGridPaste, blankImportRow, exportCsv, flatCsvTemplate, hasImportData, importColumns, localCellIssues, rowFromPreparation, serverCsv } from './business-import-grid';
import type { ImportColumn, ImportGridRow } from './business-import-grid';
import { exportImportExcel, readImportFile } from './business-import-files';
import { fieldName, ImportIcon, importErrorNames, ParcelDialog, WorksheetDialog, WorksheetGrid } from './ImportWorksheet';
import type { RowOutcome } from './ImportWorksheet';
import './business-preparations.css';
import './business-import.css';

const initialRows = () => Array.from({ length: 10 }, blankImportRow);
function unavailableRow() { const row = blankImportRow(); for (const column of importColumns) row.cells[column[0]] = ''; return row; }
type Grid = { rows: ImportGridRow[]; revision: number; past: ImportGridRow[][]; future: ImportGridRow[][] };
type Check = { revision: number; job: BusinessImportJob; rowIds: string[] };
type Pending = { operation: BusinessPreparationOperation; key: string; target?: string; rowIds: string[]; revision: number };
const defaultColumns = importColumns.filter(c => !['senderCountry', 'recipientCountry'].includes(c[0])).map(c => c[0]);
function message(error: unknown) {
  const raw = error instanceof CustomerApiError ? error.code : error instanceof Error ? error.message : '';
  const code = ({ XLSX_ARCHIVE: 'XLSX_ZIP', XLSX_ACTIVE_CONTENT: 'XLSX_EXTERNAL', XLSX_EXPANSION: 'XLSX_BOUNDS', XLSX_RANGE: 'XLSX_LAYOUT', XLSX_PARCEL_LINK: 'XLSX_PARCELS' } as Record<string, string>)[raw] ?? raw;
  return ({ VALIDATION_FAILED: 'File không đúng cấu trúc hoặc vượt giới hạn. Kiểm tra dữ liệu và thử lại.', RESOURCE_NOT_FOUND: 'Không có quyền truy cập đợt nhập này.', BUSINESS_CONTEXT_UNAVAILABLE: 'Quyền doanh nghiệp đã thay đổi. Hãy tải lại Workspace.', PRECONDITION_STALE: 'Đợt nhập đã thay đổi. Mở lại từ lịch sử trước khi tiếp tục.', IMPORT_ROW_NOT_VALID: 'Dòng đã thay đổi trạng thái. Tải lại kết quả trước khi lưu.', OUTCOME_UNKNOWN: 'Chưa xác định kết quả ghi. Kiểm tra kết quả trước khi gửi thao tác mới.', FILE_HEADER: 'Header không khớp mẫu ArgoPost. Tải file mẫu và giữ nguyên tên cột.', FILE_COLUMNS: 'Số cột không khớp header.', FILE_QUOTES: 'Dấu nháy chưa đúng chuẩn CSV.', FILE_ROWS: 'Mỗi đợt nhận từ 1 đến 100 đơn.', FILE_SIZE: 'File vượt giới hạn: CSV 256 KiB, Excel 1 MiB.', FILE_EXTENSION: 'Chỉ hỗ trợ .csv và .xlsx; không nhận .xls hoặc macro.', PASTE_RANGE: 'Vùng dán vượt số cột hoặc giới hạn 100 dòng.', ROW_LOCKED: 'Vùng dán chứa dòng đã lưu nháp.', XLSX_RAW_PARCELS: 'Có dữ liệu kiện chưa đọc được. Sửa kiện hoặc xuất CSV để bảo toàn nội dung.', XLSX_TIMEOUT: 'Excel xử lý quá thời gian cho phép.', XLSX_FORMULA: 'Không nhận công thức Excel, kể cả công thức có kết quả lưu sẵn.', XLSX_EXTERNAL: 'Không nhận liên kết ngoài hoặc nội dung hoạt động trong Excel.', XLSX_SHEETS: 'Giữ các sheet DonHang, KienHang và HuongDan theo mẫu.', XLSX_CELL_TYPE: 'Kiểu ô chưa được hỗ trợ. Mã và điện thoại phải là văn bản.', XLSX_LAYOUT: 'Không nhận ô gộp, dòng/cột ẩn hoặc cấu trúc ngoài mẫu.', XLSX_PARCELS: 'Kiểm tra Liên kết kiện và số thứ tự kiện 2–5, không trùng hoặc bỏ số.', XLSX_INVALID: 'Không đọc được Excel. Hãy dùng .xlsx theo mẫu.', XLSX_BOUNDS: 'Excel vượt giới hạn giải nén hoặc số ô.', XLSX_ZIP: 'Cấu trúc Excel không hợp lệ hoặc không được hỗ trợ.' } as Record<string, string>)[code] ?? 'Chưa thực hiện được yêu cầu. Kiểm tra file hoặc kết nối và thử lại.';
}
function download(blob: Blob, name: string) { const url = URL.createObjectURL(blob), a = document.createElement('a'); a.href = url; a.download = name; a.click(); setTimeout(() => URL.revokeObjectURL(url), 1000); }
async function loadAll(context: BusinessContext, id: string): Promise<BusinessImportJob> {
  const first = await customerClient.readBusinessImport(context, id), rows = [...first.rows];
  for (let page = 1; page < Math.ceil(first.totalRows / 20); page++) {
    const next = await customerClient.readBusinessImport(context, id, page);
    if (next.version !== first.version || next.totalRows !== first.totalRows || next.updatedAt !== first.updatedAt) throw new Error('PRECONDITION_STALE');
    rows.push(...next.rows);
  }
  if (rows.length !== first.totalRows || new Set(rows.map(r => r.rowNumber)).size !== rows.length) throw new Error('FILE_ROWS');
  return { ...first, rows };
}

export function BusinessImportWorkspace({ context }: { context: BusinessContext }) {
  const [params] = useSearchParams(), id = params.get('job'), navigate = useNavigate(), base = '/merchant/' + context.organizationId;
  const [grid, setGrid] = useState<Grid>(() => ({ rows: initialRows(), revision: 0, past: [], future: [] })), [outcomes, setOutcomes] = useState<Record<string, RowOutcome>>({}), [check, setCheck] = useState<Check>();
  const [selected, setSelected] = useState<Set<string>>(new Set()), [search, setSearch] = useState(''), [visible, setVisible] = useState<ImportColumn[]>(defaultColumns), [active, setActive] = useState<{ id: string; column: ImportColumn }>();
  const [dirty, setDirty] = useState(false);
  const [busy, setBusy] = useState(false), [uncertain, setUncertain] = useState(false), [error, setError] = useState(''), [notice, setNotice] = useState(''), [fileName, setFileName] = useState('');
  const [dialog, setDialog] = useState<'templates' | 'export' | 'columns' | 'history'>(), [parcelRow, setParcelRow] = useState<string>(), [errorRow, setErrorRow] = useState<string>(), [history, setHistory] = useState<{ items: BusinessImportJob[]; page: number; hasNext: boolean }>(), [historyPage, setHistoryPage] = useState(0), [historyError, setHistoryError] = useState('');
  const root = useRef<HTMLElement>(null), file = useRef<HTMLInputElement>(null), running = useRef(false), live = useRef(false), pending = useRef<Pending | undefined>(undefined), activeJob = useRef<string | undefined>(undefined), abort = useRef<AbortController | undefined>(undefined);
  const snapshot = useRef(customerClient.getSnapshot());
  const current = () => live.current && snapshot.current === customerClient.getSnapshot();
  const checked = !!check && check.revision === grid.revision, locked = new Set(Object.keys(outcomes).filter(key => outcomes[key].state === 'CREATED'));
  const dataRows = grid.rows.filter(r => hasImportData(r) || outcomes[r.id]), toCheck = dataRows.filter(r => !locked.has(r.id)), validSelected = checked ? grid.rows.filter(r => selected.has(r.id) && outcomes[r.id]?.state === 'VALID').map(r => r.id) : [];
  useEffect(() => { live.current = true; abort.current = new AbortController(); return () => { live.current = false; abort.current?.abort(); }; }, []);
  useEffect(() => { if (!uncertain && (checked || !dirty)) return; const warn = (e: BeforeUnloadEvent) => e.preventDefault(); window.addEventListener('beforeunload', warn); return () => window.removeEventListener('beforeunload', warn); }, [uncertain, checked, dirty]);
  function install(data: BusinessImportJob, rowIds?: string[], revision?: number) {
    const ids = rowIds ?? data.rows.map(() => crypto.randomUUID());
    const mapped = Object.fromEntries(data.rows.map((r, i) => [ids[i], { ...r, jobId: data.id }]));
    if (rowIds) { setOutcomes(v => ({ ...v, ...mapped })); setCheck({ job: data, rowIds: ids, revision: revision! }); }
    else {
      const rows = data.rows.map((r, i) => ({ ...(r.input ? rowFromPreparation(r.input) : unavailableRow()), id: ids[i] })), nextRevision = grid.revision + 1;
      setGrid({ rows, revision: nextRevision, past: [], future: [] }); setCheck({ job: data, rowIds: ids, revision: nextRevision }); setOutcomes(mapped);
      if (data.invalidRows) setNotice('Dòng lỗi trong lịch sử không lưu nội dung gốc. Mở lại file nguồn để sửa đầy đủ.');
    }
    setDirty(false); activeJob.current = data.id; pending.current = undefined; setUncertain(false); setSelected(new Set()); setError('');
    if (id !== data.id) navigate(base + '/imports?job=' + data.id, { replace: true });
  }
  useEffect(() => {
    if (!id || activeJob.current === id) return; let valid = true; setBusy(true); setError('');
    void loadAll(context, id).then(data => { if (valid && current()) install(data); }).catch(e => { if (valid && current()) setError(message(e)); }).finally(() => { if (valid && current()) setBusy(false); });
    return () => { valid = false; };
    // Scope remounts on Organization/authority changes. URL only loads a different job.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id, context]);
  useEffect(() => {
    if (dialog !== 'history') return; let valid = true; setHistory(undefined); setHistoryError('');
    void customerClient.listBusinessImports(context, historyPage).then(data => { if (valid && current()) setHistory(data); }).catch(e => { if (valid && current()) setHistoryError(message(e)); }); return () => { valid = false; };
  }, [dialog, historyPage, context]);
  function mutate(rows: ImportGridRow[]) {
    if (busy || uncertain) return;
    setDirty(true); setGrid(v => ({ rows, revision: v.revision + 1, past: [...v.past, v.rows].slice(-20), future: [] })); setCheck(undefined); setError(''); setNotice('Dữ liệu đã thay đổi. Kiểm tra lại trước khi lưu nháp.'); activeJob.current = undefined;
    if (id) navigate(base + '/imports', { replace: true });
  }
  function undo(redo = false) {
    if (busy || uncertain) return;
    setDirty(true); setGrid(v => { const stack = redo ? v.future : v.past; if (!stack.length) return v; return { rows: stack.at(-1)!, revision: v.revision + 1, past: redo ? [...v.past, v.rows].slice(-20) : v.past.slice(0, -1), future: redo ? v.future.slice(0, -1) : [...v.future, v.rows].slice(-20) }; });
    setCheck(undefined); activeJob.current = undefined; if (id) navigate(base + '/imports', { replace: true });
  }
  async function openFile(chosen: File) {
    if (running.current || uncertain || dirty && !window.confirm('Thay bảng hiện tại bằng nội dung file? Các nháp đã lưu không bị xóa.')) return;
    running.current = true; setBusy(true); setError('');
    try { const rows = await readImportFile(chosen, abort.current?.signal); if (current()) { setDirty(true); setGrid(v => ({ rows: rows.length ? rows : initialRows(), revision: v.revision + 1, past: [], future: [] })); setOutcomes({}); setCheck(undefined); setSelected(new Set()); setSearch(''); setFileName(chosen.name); setNotice('File đã mở. Kiểm tra dữ liệu trước khi lưu thành nháp.'); activeJob.current = undefined; if (id) navigate(base + '/imports', { replace: true }); } }
    catch (e) { if (current()) setError(message(e)); } finally { running.current = false; if (current()) setBusy(false); }
  }
  async function preview() {
    if (running.current || uncertain || !toCheck.length) return;
    let csv: string; try { csv = serverCsv(toCheck); } catch (e) { setError(message(e)); return; }
    running.current = true; setBusy(true); setError(''); const request: Pending = { operation: 'IMPORT_PREVIEW', key: crypto.randomUUID(), rowIds: toCheck.map(r => r.id), revision: grid.revision }; pending.current = request; let written = false;
    try { const first = await customerClient.previewBusinessImport(context, csv, request.key); written = true; request.target = first.id; const data = first.hasNext ? await loadAll(context, first.id) : first; if (current()) { install(data, request.rowIds, request.revision); setNotice('Đã kiểm tra trên Backend. Chọn dòng hợp lệ để lưu nháp.'); } }
    catch (e) { if (current()) { const unknown = written || e instanceof CustomerApiError && e.code === 'OUTCOME_UNKNOWN'; setUncertain(unknown); setError(unknown ? message(new Error('OUTCOME_UNKNOWN')) : message(e)); if (!unknown) pending.current = undefined; } }
    finally { running.current = false; if (current()) setBusy(false); }
  }
  async function commit() {
    if (!check || !checked || !validSelected.length || running.current || uncertain || !window.confirm('Lưu ' + validSelected.length + ' dòng hợp lệ thành nháp doanh nghiệp? Chưa Booking hoặc bàn giao hàng.')) return;
    running.current = true; setBusy(true); setError(''); let job = check.job;
    for (let start = 0; start < validSelected.length; start += 20) {
      const chunk = validSelected.slice(start, start + 20).map(key => check.rowIds.indexOf(key) + 1);
      const request: Pending = { operation: 'IMPORT_COMMIT', key: crypto.randomUUID(), target: job.id, rowIds: check.rowIds, revision: grid.revision }; pending.current = request; let written = false;
      try {
        await customerClient.commitBusinessImport(context, job, chunk, request.key); written = true; job = await loadAll(context, job.id);
        if (!current()) break; install(job, request.rowIds, request.revision); setGrid(v => ({ ...v, past: [], future: [] })); setNotice('Đã lưu ' + job.createdRows + ' bản nháp trong đợt nhập. Chưa tạo vận đơn.');
      } catch (e) {
        if (current()) { const unknown = written || e instanceof CustomerApiError && e.code === 'OUTCOME_UNKNOWN'; setUncertain(unknown); setError(unknown ? message(new Error('OUTCOME_UNKNOWN')) : message(e)); if (!unknown) { pending.current = undefined; setCheck(undefined); } } break;
      }
    }
    running.current = false; if (current()) setBusy(false);
  }
  async function resolve() {
    const request = pending.current; if (!request || running.current) return; running.current = true; setBusy(true);
    try { const receipt = await customerClient.businessPreparationReceipt(context, request.operation, request.key); if (request.target && receipt.targetId !== request.target) throw new Error(); const data = await loadAll(context, receipt.targetId); if (current()) { install(data, request.rowIds, request.revision); setGrid(v => ({ ...v, past: [], future: [] })); setNotice('Đã đối chiếu kết quả Backend. Không gửi lặp thao tác.'); } }
    catch { if (current()) setError('Chưa xác nhận được kết quả. Giữ trang này và kiểm tra lại; không gửi thao tác mới.'); }
    finally { running.current = false; if (current()) setBusy(false); }
  }
  async function saveFile(format: 'csv' | 'xlsx', template: boolean) {
    if (running.current) return; running.current = true; setBusy(true); setError('');
    try { const blob = format === 'xlsx' ? await exportImportExcel(dataRows, template, abort.current?.signal) : new Blob([template ? flatCsvTemplate() : exportCsv(dataRows)], { type: 'text/csv;charset=utf-8' }); if (current()) { download(blob, 'argopost-business-' + (template ? 'mau' : 'du-lieu') + '.' + format); setDialog(undefined); } }
    catch (e) { if (current()) { setDialog(undefined); setError(message(e)); } } finally { running.current = false; if (current()) setBusy(false); }
  }
  function openHistory(jobId: string) {
    if (dirty && !window.confirm('Thay bảng bằng đợt nhập đã lưu? Dữ liệu chưa gửi sẽ được thay thế.')) return;
    setDialog(undefined); activeJob.current = undefined;
    if (jobId !== id) { navigate(base + '/imports?job=' + jobId); return; }
    setBusy(true); void loadAll(context, jobId).then(data => { if (current()) install(data); }).catch(e => { if (current()) setError(message(e)); }).finally(() => { if (current()) setBusy(false); });
  }
  const focusRow = grid.rows.find(r => r.id === active?.id), detail = grid.rows.find(r => r.id === errorRow), parcel = grid.rows.find(r => r.id === parcelRow);
  const summary = [dataRows.length, checked ? check.job.validRows : '—', checked ? check.job.invalidRows : '—', dataRows.filter(r => locked.has(r.id)).length, checked ? check.job.conflictRows : '—'];
  return <section className="ig-workspace bp-workspace" ref={root} aria-busy={busy}>
    <div className="ig-heading"><div><h1>Nhập đơn hàng từ file</h1><p>{context.displayName} <span>· CSV & Excel</span></p></div><div className="ig-heading-actions"><button className="pw-primary" disabled={busy || uncertain} onClick={() => file.current?.click()}><ImportIcon name="file" />Mở file</button><button className="pw-secondary" disabled={busy || uncertain} onClick={() => setDialog('templates')}><ImportIcon name="down" />Tải file mẫu</button><button className="pw-secondary" disabled={busy || uncertain} onClick={() => { setHistoryPage(0); setDialog('history'); }}><ImportIcon name="history" />Lịch sử nhập file</button></div></div>
    <input ref={file} className="ig-file-input" tabIndex={-1} type="file" aria-label="Chọn file CSV hoặc Excel" accept=".csv,.xlsx,text/csv,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" disabled={busy || uncertain} onChange={e => { const chosen = e.target.files?.[0]; e.target.value = ''; if (chosen) void openFile(chosen); }} />
    {error && <p role="alert" className="cp-notice cp-error">{error}</p>}{uncertain && <button className="pw-secondary" disabled={busy} onClick={() => { void resolve(); }}>Kiểm tra kết quả nhập</button>}
    <div className="ig-sheet"><div className="ig-toolbar"><label className="ig-search"><ImportIcon name="search" /><input aria-label="Tìm trong bảng nhập" placeholder="Tìm mã đơn, người nhận, số điện thoại…" value={search} onChange={e => setSearch(e.target.value)} /></label><div className="ig-tools">
      <button title="Hoàn tác" aria-label="Hoàn tác" disabled={busy || uncertain || !grid.past.length} onClick={() => undo()}><ImportIcon name="undo" /></button><button title="Làm lại" aria-label="Làm lại" disabled={busy || uncertain || !grid.future.length} onClick={() => undo(true)}><ImportIcon name="redo" /></button>
      <button title="Thêm dòng" aria-label="Thêm dòng" disabled={busy || uncertain || grid.rows.length >= 100} onClick={() => mutate([...grid.rows, blankImportRow()])}><ImportIcon name="plus" /></button><button title="Xóa dòng được chọn khỏi bảng" aria-label="Xóa dòng được chọn khỏi bảng" disabled={busy || uncertain || !selected.size} onClick={() => { if (window.confirm('Bỏ dòng được chọn khỏi bảng? Không xóa bản nháp đã lưu.')) { mutate(grid.rows.filter(r => !selected.has(r.id))); setSelected(new Set()); } }}><ImportIcon name="trash" /></button>
      <button title="Chọn cột hiển thị" aria-label="Chọn cột hiển thị" disabled={busy} onClick={() => setDialog('columns')}><ImportIcon name="columns" /></button><button title="Xuất bảng hiện tại" aria-label="Xuất bảng hiện tại" disabled={busy || uncertain || !dataRows.length} onClick={() => setDialog('export')}><ImportIcon name="down" /></button><button title="Toàn màn hình" aria-label="Toàn màn hình" onClick={() => { const action = document.fullscreenElement ? document.exitFullscreen() : root.current?.requestFullscreen(); void action?.catch(() => setError('Trình duyệt chưa cho phép toàn màn hình.')); }}><ImportIcon name="expand" /></button>
    </div></div><div className="ig-formula"><span>{active ? (visible.indexOf(active.column) + 1) + ':' + (grid.rows.findIndex(r => r.id === active.id) + 1) : '—'}</span><i>fx</i><output>{focusRow && active ? focusRow.cells[active.column] : 'Chọn một ô để xem nội dung đầy đủ'}</output></div>
      <WorksheetGrid rows={grid.rows} outcomes={outcomes} checked={checked} selected={selected} visible={visible} search={search} busy={busy || uncertain} base={base} select={(key, yes) => setSelected(v => { const next = new Set(v); if (yes) next.add(key); else next.delete(key); return next; })} selectVisible={(keys, yes) => setSelected(v => { const next = new Set(v); keys.forEach(key => { if (yes) next.add(key); else next.delete(key); }); return next; })} edit={(key, field, value) => { if (locked.has(key) || value.length > 4000) return; mutate(grid.rows.map(r => r.id === key ? { ...r, cells: { ...r.cells, [field]: value }, ...(importColumns.find(c => c[0] === field)?.[3] === 'parcel' ? { rawParcels: undefined } : {}) } : r)); }} paste={(key, field, value) => { if (search.trim()) { setError('Xóa tìm kiếm trước khi dán nhiều ô để tránh sửa dòng đang ẩn.'); return; } try { mutate(applyGridPaste(grid.rows, key, field, value, visible, locked)); } catch (e) { setError(message(e)); } }} active={(key, column) => setActive({ id: key, column })} parcels={setParcelRow} errors={setErrorRow} />
    </div>
    <div className="ig-footer"><div className="ig-footer-note"><strong>{fileName || 'Bảng nhập mới'}</strong><span>{notice || 'Tối đa 100 đơn · 5 kiện/đơn. Kiểm tra rồi chọn dòng hợp lệ để lưu nháp.'}</span></div><dl className="bp-import-totals ig-totals">{['Tổng dòng', 'Hợp lệ', 'Cần sửa', 'Đã lưu nháp', 'Trùng nguồn'].map((label, i) => <div key={label}><dt>{label}</dt><dd>{summary[i]}</dd></div>)}</dl><div className="ig-submit"><button className="pw-secondary" disabled={busy || uncertain || !toCheck.length} onClick={() => { void preview(); }}><ImportIcon name="check" />Kiểm tra dữ liệu</button><button className="pw-primary" disabled={busy || uncertain || !validSelected.length || !context.permissions.includes('shipment.draft.write')} onClick={() => { void commit(); }}>Lưu {validSelected.length} dòng thành nháp</button></div></div>
    <div className="ig-bottom-links"><button className="pw-text-link" disabled={!checked || busy || uncertain || !check.job.validRows} onClick={() => setSelected(new Set(check?.rowIds.filter(key => outcomes[key]?.state === 'VALID')))}>Chọn tất cả dòng hợp lệ</button><Link to={base + '/preparations'} onClick={e => { if ((uncertain || dirty && !checked) && !window.confirm('Rời bảng nhập? Dữ liệu chưa gửi chỉ được giữ trong trang hiện tại.')) e.preventDefault(); }}>Xem đơn nháp doanh nghiệp</Link><span>Cước chỉ xuất hiện khi có báo giá hợp lệ. Lưu nháp chưa tạo vận đơn.</span></div>
    {(dialog === 'templates' || dialog === 'export') && <WorksheetDialog title={dialog === 'templates' ? 'Tải file mẫu ArgoPost' : 'Xuất bảng hiện tại'} close={() => { if (!busy) setDialog(undefined); }}><div className="ig-download-options"><button disabled={busy} onClick={() => { void saveFile('xlsx', dialog === 'templates'); }}><ImportIcon name="file" /><strong>Excel .xlsx</strong><span>Sheet đơn hàng và kiện bổ sung. Giữ số 0 đầu điện thoại.</span></button><button disabled={busy} onClick={() => { void saveFile('csv', dialog === 'templates'); }}><ImportIcon name="file" /><strong>CSV UTF-8</strong><span>{dialog === 'templates' ? 'Các cột cho một kiện; thêm kiện trong bảng.' : 'CSV V1 bảo toàn toàn bộ các kiện.'}</span></button></div><p className="ig-dialog-hint">Không dùng công thức. Mã nguồn và điện thoại là văn bản. Không đưa thông tin thanh toán hoặc quyền truy cập vào file.</p></WorksheetDialog>}
    {dialog === 'columns' && <WorksheetDialog title="Cột hiển thị" close={() => setDialog(undefined)}><div className="ig-column-options">{importColumns.map(c => <label key={c[0]}><input type="checkbox" checked={visible.includes(c[0])} disabled={visible.length === 1 && visible.includes(c[0])} onChange={e => setVisible(v => e.target.checked ? importColumns.filter(col => col[0] === c[0] || v.includes(col[0])).map(col => col[0]) : v.filter(key => key !== c[0]))} />{c[2]}</label>)}</div><p className="ig-dialog-hint">Ẩn cột không xóa dữ liệu.</p></WorksheetDialog>}
    {dialog === 'history' && <WorksheetDialog title="Lịch sử nhập file" close={() => setDialog(undefined)} wide><div className="ig-history">{historyError ? <p role="alert">{historyError}</p> : !history ? <p role="status">Đang tải lịch sử doanh nghiệp…</p> : !history.items.length ? <p>Chưa có đợt nhập trong phạm vi quyền hiện tại.</p> : <table><thead><tr><th>Đợt nhập / thời gian</th><th>Tổng dòng</th><th>Đã lưu</th><th>Cần sửa</th><th /></tr></thead><tbody>{history.items.map(job => <tr key={job.id}><td>{job.id.slice(0, 8)}<small>{new Date(job.createdAt).toLocaleString('vi-VN')}</small></td><td>{job.totalRows}</td><td>{job.createdRows}</td><td>{job.invalidRows}</td><td><button className="pw-text-link" onClick={() => openHistory(job.id)}>Mở đợt nhập</button></td></tr>)}</tbody></table>}</div><footer><button className="pw-secondary" disabled={!history || historyPage === 0} onClick={() => setHistoryPage(v => v - 1)}>Trang trước</button><span>Trang {historyPage + 1}</span><button className="pw-secondary" disabled={!history?.hasNext} onClick={() => setHistoryPage(v => v + 1)}>Trang sau</button></footer></WorksheetDialog>}
    {parcel && <ParcelDialog key={parcel.id} row={parcel} readOnly={locked.has(parcel.id)} close={() => setParcelRow(undefined)} save={next => { mutate(grid.rows.map(r => r.id === next.id ? next : r)); setParcelRow(undefined); }} />}
    {detail && <WorksheetDialog title={'Kiểm tra dòng ' + (grid.rows.indexOf(detail) + 1)} close={() => setErrorRow(undefined)}><div className="ig-row-detail">{outcomes[detail.id]?.state === 'CREATED' ? <p>Đã lưu bản nháp. Mở nháp để tiếp tục chỉnh sửa theo phiên bản hiện hành.</p> : <><p>{checked ? 'Kết quả Backend và gợi ý định dạng:' : 'Chưa có kết quả Backend cho phiên bản bảng này.'}</p><ul>{(checked ? outcomes[detail.id]?.errors ?? [] : []).map((e, i) => <li key={i}>{fieldName(e.field)}: {importErrorNames[e.code] ?? 'Dữ liệu chưa hợp lệ.'}</li>)}{Object.entries(localCellIssues(detail)).map(([key, value]) => <li key={key}>{importColumns.find(c => c[0] === key)?.[2]}: {value}</li>)}</ul>{checked && outcomes[detail.id]?.state === 'VALID' && <p>Hợp lệ để lưu nháp. Chưa xác nhận vùng phục vụ hoặc báo giá.</p>}</>}</div></WorksheetDialog>}
  </section>;
}
