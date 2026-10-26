"""Read-only V3 documentation gates. No network, runtime, migration or source edits."""
from __future__ import annotations
import csv
import hashlib
import gzip
import json
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "docs/reconciliation/v3"
BASELINE = "ff3b2963ee1b165c7ade412e8b37389da65ca28e"
DEFER = {"BC-006", "BC-040", "BC-045", "BC-046", "BC-085"}
DOMAINS = {"identity", "merchant", "orders", "pricing", "warehouse", "dispatch", "finance", "routing", "notification", "platform"}
ID_PATTERN = re.compile(r"\b(?:REQ-\d{3}-\d{2}|AC-\d{3}-\d{2}|T-\d{3}-\d{2}(?:-[A-Z0-9]+)*|DEMO-(?:T\d+|\d+)|D1-\d{2}(?:-[A-Z0-9]+)*|ADR-\d{4}|FR-[A-Z]+\d{2}|BR-\d{2}|NFR-[A-Z]\d{2}|AC-\d{2}|DEC-\d{2})\b")


def git(*args: str, cwd: Path = ROOT) -> str:
    return subprocess.check_output(["git", "-C", str(cwd), *args]).decode("utf-8")


def sha(p: Path) -> str:
    return hashlib.sha256(p.read_bytes()).hexdigest()


def read_csv(name: str) -> list[dict[str, str]]:
    with (OUT / name).open(encoding="utf-8", newline="") as f:
        return list(csv.DictReader(f))


def encoding_errors(text: str) -> bool:
    return "\ufffd" in text or bool(re.search(r"[\w]\?[\w]|\?\?", text))


def source_rows() -> list[dict[str, str]]:
    text = (ROOT / "docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md").read_text(encoding="utf-8")
    result = []
    for line in text.splitlines():
        if re.match(r"^\| BC-\d{3} \|", line):
            a = [v.strip() for v in line.strip("|").split("|")]
            result.append(dict(capability_id=a[0], role_id=a[1][:3], module_id=a[2], classification=a[3], business_operation=a[4], source_guard=a[5]))
    return result


def mapping_errors(rows: list[dict[str, str]], cards: list[dict], source: list[dict[str, str]]) -> list[str]:
    errors: list[str] = []
    def check(condition: bool, message: str) -> None:
        if not condition:
            errors.append(message)
    expected = {f"BC-{i:03}" for i in range(1, 372)}
    check(len(rows) == 371 and {x["capability_id"] for x in rows} == expected, "BC count/unique/continuity")
    check({x["role_id"] for x in rows} == {f"R{i:02}" for i in range(1, 34)}, "role coverage")
    check({x["module_id"] for x in rows} == {f"M{i:02}" for i in range(1, 19)}, "module coverage")
    check(sum(x["classification"] == "CORE" for x in rows) == 366, "CORE count")
    check({x["capability_id"] for x in rows if x["classification"] == "DEFER"} == DEFER, "exact DEFER IDs")
    index = {s["id"]: s for s in cards}
    check(len(cards) == 30 and set(index) == {f"L{i:03}" for i in range(1, 31)}, "logical IDs")
    src = {x["capability_id"]: x for x in source}
    for row in rows:
        key = row["capability_id"]
        check(row["primary_spec"] in index and row["primary_domain_owner"] in DOMAINS, key + " primary owner")
        check(row["mapping_status"] in {"PROPOSED", "REVIEWED", "APPROVED"}, key + " mapping enum")
        check(row["mapping_status"] == "PROPOSED", key + " unauthorized mapping promotion")
        check(row["implementation_status"] in {"NOT_STARTED", "PARTIAL", "IMPLEMENTED", "VERIFIED", "BLOCKED", "NOT_VERIFIED"}, key + " implementation enum")
        check(row["implementation_status"] != "VERIFIED", key + " unproved target VERIFIED")
        check(all(value.strip() for field, value in row.items() if field not in {"secondary_consumers", "dependency"}), key + " empty required field")
        check(all(x in index for x in row["secondary_consumers"].split(";") if x), key + " orphan consumer")
        check(row["primary_spec"] not in row["secondary_consumers"].split(";"), key + " duplicate primary/consumer")
        if key in src:
            check(all(row[f] == src[key][f] for f in ["role_id", "module_id", "classification", "business_operation"]), key + " source mutation")
            check(src[key]["source_guard"] in row["required_permission_guard"], key + " dropped guard")
        if key in DEFER or key in {"BC-243", "BC-261", "BC-280", "BC-354", "BC-355", "BC-362"}:
            check(row["acceptance_kind"] == "NEGATIVE/DENY", key + " missing denial")
        check(row["target_requirement"] == "REQ-V3-" + key, key + " requirement ID")
        if row["primary_spec"] in index:
            check(key in index[row["primary_spec"]]["capability_ids"], key + " register mismatch")
    required = {"id", "name", "domain_owner", "purpose", "excluded_scope", "role_ids", "capability_ids", "invariants", "hard_dependencies", "soft_dependencies", "backend_scope", "frontend_scope", "database_scope", "contract_scope", "authorization", "workflow", "idempotency_concurrency", "audit_privacy", "acceptance", "test_evidence", "definition_of_ready", "definition_of_done", "implementation_order", "blockers", "decision_owner", "folder", "readiness", "wave"}
    visited: set[str] = set()
    active: set[str] = set()
    def visit(key: str) -> None:
        if key in active:
            errors.append("hard dependency cycle " + key)
            return
        if key in visited or key not in index:
            return
        active.add(key)
        for dep in index[key]["hard_dependencies"]:
            check(dep in index, key + " missing dependency " + dep)
            if dep in index:
                check(index[dep]["implementation_order"] < index[key]["implementation_order"], key + " invalid execution order")
                visit(dep)
        active.remove(key)
        visited.add(key)
    folders = []
    for card in cards:
        check(required <= card.keys(), card["id"] + " missing card fields")
        folders.append(card["folder"])
        check(set(card["capability_ids"]) == {x["capability_id"] for x in rows if x["primary_spec"] == card["id"]}, card["id"] + " owner register mismatch")
        visit(card["id"])
    check(len(set(folders)) == 30, "physical folder collision")
    check(all(s["folder"].startswith("specs/planning/v3/L") for s in cards[2:]), "legacy numeric ID reuse")
    return errors


def run() -> tuple[list[str], dict]:
    rows = read_csv("capability-crosswalk.csv")
    cards = json.loads((OUT / "spec-register.json").read_text(encoding="utf-8"))
    errors = mapping_errors(rows, cards, source_rows())
    protection = json.loads((OUT / "protection-baseline.json").read_text(encoding="utf-8"))
    for path, digest in protection["protected_files"].items():
        if path == "docs/implementation/dependency-order.md":
            continue  # explicitly authorized planning navigation, never a runtime receipt
        if not (ROOT / path).is_file() or sha(ROOT / path) != digest:
            errors.append("protected file changed " + path)
    original = Path("D:/TLCN/e-logistic")
    for path, digest in protection["original_foundation"].items():
        if sha(original / path) != digest:
            errors.append("original Foundation changed " + path)
    for name, digest in protection["source_inputs"].items():
        copy = ROOT / "docs/business-references" / name
        raw = gzip.decompress(copy.with_suffix(".md.gz").read_bytes()) if copy.with_suffix(".md.gz").exists() else copy.read_bytes()
        display = raw.decode("utf-8").splitlines(keepends=True)
        for i, line in enumerate(display):
            ending = "\r\n" if line.endswith("\r\n") else "\n" if line.endswith("\n") else ""
            body = line[:-len(ending)] if ending else line
            if body.endswith("  "):
                display[i] = body[:-2] + "\\" + ending
        expected = "".join(display).encode("utf-8") if copy.with_suffix(".md.gz").exists() else raw
        if hashlib.sha256(raw).hexdigest() != digest or copy.read_bytes() != expected or sha(original / "docs/business-references" / name) != digest:
            errors.append("source input changed " + name)
    for worktree in protection["existing_worktrees"]:
        path = Path(worktree["path"])
        if git("rev-parse", "HEAD", cwd=path).strip() != worktree["head"] or git("status", "--porcelain", cwd=path) != worktree["status"]:
            errors.append("existing worktree changed " + str(path))
    legacy = read_csv("legacy-crosswalk.csv")
    legacy_keys = {(x["path"], x["legacy_id"]) for x in legacy}
    tree = git("ls-tree", "-r", "--name-only", BASELINE).splitlines()
    for path in tree:
        if path.startswith("specs/") and path.endswith(".md"):
            old = git("show", BASELINE + ":" + path)
            for ident in set(ID_PATTERN.findall(old)):
                if (path, ident) not in legacy_keys:
                    errors.append("legacy ID dropped " + path + " " + ident)
            if old.rstrip() not in (ROOT / path).read_text(encoding="utf-8"):
                errors.append("legacy content rewritten " + path)
    changed = set(git("diff", "--name-only", BASELINE).splitlines()) | set(git("ls-files", "--others", "--exclude-standard").splitlines())
    for path in changed:
        if Path(path).suffix in {".md", ".json", ".csv", ".py"}:
            content = (ROOT / path).read_text(encoding="utf-8")
            if re.search(r"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----|gh[pousr]_[A-Za-z0-9]{30,}|sk-proj-[A-Za-z0-9_-]{30,}", content):
                errors.append("possible secret material " + path)
        if not path.startswith(("docs/", "specs/")):
            errors.append("non-doc change " + path)
    # Only new/changed links: report old broken links and missing source assets separately.
    broken_old: list[str] = []
    checked_links = 0
    for name in sorted(changed):
        path = ROOT / name
        if path.suffix != ".md" or "docs/business-references/" in name:
            continue
        current = path.read_text(encoding="utf-8")
        try:
            old = git("show", BASELINE + ":" + name) if name in tree else ""
        except subprocess.CalledProcessError:
            old = ""
        added = current[len(old.rstrip()):] if old and current.startswith(old.rstrip()) else current
        if encoding_errors(added):
            errors.append("corrupt UTF-8 authored Markdown " + name)
        if current.count("```") % 2:
            errors.append("unbalanced Markdown fence " + name)
        for target in re.findall(r"(?<!!)\[[^\]]+\]\(([^)]+)\)", current):
            if target.startswith(("https:", "http:", "mailto:", "#")):
                continue
            local, _, anchor = target.partition("#")
            dest = (path.parent / local).resolve()
            checked_links += 1
            problem = not dest.exists()
            if anchor and dest.is_file() and dest.suffix == ".md":
                body = dest.read_text(encoding="utf-8")
                headings = set()
                for title in re.findall(r"^#{1,6} (.+)$", body, re.M):
                    headings.add(re.sub(r"[^\w\- ]", "", title.lower()).replace(" ", "-"))
                problem = problem or anchor not in headings and f'id="{anchor}"' not in body
            if problem:
                item = name + " -> " + target
                (broken_old if "](" + target + ")" in old else errors).append(item)
    # Exact one requirement definition per BC in its owner planning spec (L002 BC denial has supplement).
    for row in rows:
        owner = next(s for s in cards if s["id"] == row["primary_spec"])
        if row["primary_spec"] == "L002":
            dest = OUT / "identity-target-delta.md"
        else:
            dest = ROOT / owner["folder"] / "spec.md"
        if row["target_requirement"] not in dest.read_text(encoding="utf-8"):
            errors.append("orphan target requirement " + row["capability_id"])
    summary = dict(capabilities=len(rows), roles=len({x["role_id"] for x in rows}), modules=len({x["module_id"] for x in rows}), core=366, defer=sorted(DEFER), logical_specs=len(cards), legacy_records=len(legacy), inventory_files=len(read_csv("repository-inventory.csv")), protected_files=len(protection["protected_files"]), protected_worktrees=len(protection["existing_worktrees"]), checked_links=checked_links, preexisting_broken_links=sorted(set(broken_old)), errors=errors)
    return errors, summary


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    failures, summary = run()
    print(json.dumps(summary, ensure_ascii=False, indent=2))
    raise SystemExit(bool(failures))
