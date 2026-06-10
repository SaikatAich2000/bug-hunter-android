"""Generate the Android-side API documentation.

The Android app is a CLIENT, not a server, so there's no FastAPI route
table to dump. Instead we:

  1. Parse every Retrofit @GET / @POST / @PUT / @PATCH / @DELETE
     annotation in app/src/main/java/com/bughunter/core/network/api/
     to learn which (method, path) tuples Android actually calls.
  2. Load the enterprise backend's OpenAPI spec (the schemas + examples
     are authoritative there).
  3. Filter that spec to only the operations Android exercises.
  4. Emit `openapi.json`, `openapi.yaml`, and `postman_collection.json`
     for that subset, plus a human-readable `android-api-usage.md`.

The output is what someone needs in order to: (a) understand the Android
app's network surface, (b) test those endpoints in Postman, or (c) write
an Android-compatible mock backend.

Re-run after touching anything in core/network/api/ or after the
enterprise backend gains endpoints Android starts consuming:

    python scripts/gen-android-api-docs.py
"""
from __future__ import annotations

import json
import os
import re
import sys
from collections import defaultdict
from pathlib import Path

import yaml

REPO_ROOT = Path(__file__).resolve().parent.parent
OUT_DIR = REPO_ROOT / "docs" / "api"
RETROFIT_DIR = REPO_ROOT / "app" / "src" / "main" / "java" / "com" / "bughunter" / "core" / "network" / "api"

JSON_MIME = "application/json"

# Enterprise spec is the upstream source of truth. If the path doesn't
# exist yet, fall back to telling the user how to generate it.
ENTERPRISE_OPENAPI = Path(
    os.environ.get(
        "ENTERPRISE_OPENAPI",
        r"c:\Bug Hunter\Web_Hosting\bug-hunter-enterprise\docs\api\openapi.json",
    )
)

APP_NAME = os.environ.get("DOC_APP_NAME", "Bug Hunter — Android API surface")
SERVER_URL = os.environ.get("DOC_SERVER_URL", "https://www.bughunter.co.in")


# ────────────────────────────────────────────────────────────────────────
# Retrofit annotation parser
# ────────────────────────────────────────────────────────────────────────
RETROFIT_ANNOTATION = re.compile(
    r'@(GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS)\("([^"]+)"\)'
)


def parse_retrofit_endpoints() -> list[tuple[str, str, str]]:
    """Return a sorted list of (method, openapi_path, retrofit_file)."""
    out: list[tuple[str, str, str]] = []
    for kt_file in sorted(RETROFIT_DIR.glob("*.kt")):
        text = kt_file.read_text(encoding="utf-8")
        for method, path in RETROFIT_ANNOTATION.findall(text):
            # Retrofit declares paths without leading "/", OpenAPI uses
            # leading "/". Reconcile so we can match.
            openapi_path = "/" + path.lstrip("/")
            out.append((method.upper(), openapi_path, kt_file.stem))
    return out


# ────────────────────────────────────────────────────────────────────────
# OpenAPI filtering: keep only the Android-touched operations
# ────────────────────────────────────────────────────────────────────────
def filter_openapi(spec: dict, wanted: set[tuple[str, str]]) -> dict:
    """Return a deep-copy of `spec` with only the operations whose
    (method, path) appear in `wanted`. Schemas referenced from those
    operations are preserved; unreferenced schemas are dropped to keep
    the file small."""
    out = json.loads(json.dumps(spec))  # cheap deep copy via JSON round-trip
    out["info"] = {
        **(out.get("info") or {}),
        "title": APP_NAME,
        "description": (
            "Subset of the Bug Hunter enterprise API that the Android app "
            "actually calls (parsed from Retrofit interfaces under "
            "app/src/main/java/com/bughunter/core/network/api/). For the "
            "full backend surface, see the enterprise repo's docs/api/."
        ),
    }
    out["servers"] = [
        {"url": SERVER_URL, "description": "Production"},
        {"url": "http://localhost:8000", "description": "Local dev"},
    ]

    paths = out.get("paths", {})
    kept_paths: dict = {}
    for path, ops in paths.items():
        kept_ops: dict = {}
        for method, op in ops.items():
            if method.upper() not in {"GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"}:
                # Keep non-operation entries like "parameters" verbatim.
                kept_ops[method] = op
                continue
            if (method.upper(), path) in wanted:
                kept_ops[method] = op
        if kept_ops:
            kept_paths[path] = kept_ops
    out["paths"] = kept_paths

    # Prune unreferenced schemas to keep the file lean. Walk what's left
    # for $ref values, transitively collect them, then drop the rest.
    components = out.get("components", {}) or {}
    schemas = components.get("schemas", {}) or {}
    if schemas:
        referenced: set[str] = set()
        _collect_refs(kept_paths, referenced)
        # Resolve transitive refs.
        frontier = set(referenced)
        while frontier:
            nxt: set[str] = set()
            for ref in frontier:
                if ref in schemas:
                    _collect_refs(schemas[ref], nxt)
            new_refs = nxt - referenced
            referenced |= new_refs
            frontier = new_refs
        components["schemas"] = {k: v for k, v in schemas.items() if k in referenced}
        out["components"] = components

    return out


def _collect_refs(node: object, into: set[str]) -> None:
    """Walk a JSON-shaped tree and add every `#/components/schemas/X`
    target into `into` (as just `X`)."""
    if isinstance(node, dict):
        for key, value in node.items():
            if key == "$ref" and isinstance(value, str) and value.startswith("#/components/schemas/"):
                into.add(value.rsplit("/", 1)[-1])
            else:
                _collect_refs(value, into)
    elif isinstance(node, list):
        for item in node:
            _collect_refs(item, into)


# ────────────────────────────────────────────────────────────────────────
# Postman collection builder (re-implemented here to keep this script
# self-contained — same algorithm as the backend generators).
# ────────────────────────────────────────────────────────────────────────
def openapi_to_postman(spec: dict) -> dict:
    paths = spec.get("paths", {})
    info_block = spec.get("info", {})
    collection: dict = {
        "info": {
            "name": APP_NAME,
            "_postman_id": _stable_id(APP_NAME),
            "description": info_block.get("description", ""),
            "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
            "version": info_block.get("version", "1.0"),
        },
        "variable": [
            {"key": "baseUrl", "value": SERVER_URL, "type": "string"},
        ],
        "item": [],
    }

    grouped: dict[str, list[dict]] = defaultdict(list)
    for path, ops in sorted(paths.items()):
        for method, op in ops.items():
            if method.upper() not in {"GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"}:
                continue
            tag = (op.get("tags") or ["untagged"])[0]
            grouped[tag].append(_build_postman_item(path, method, op, spec))

    for tag in sorted(grouped.keys()):
        collection["item"].append({
            "name": tag.title(),
            "item": grouped[tag],
            "description": f"Endpoints tagged `{tag}`.",
        })
    return collection


def _stable_id(name: str) -> str:
    import hashlib
    h = hashlib.md5(name.encode("utf-8")).hexdigest()
    return f"{h[:8]}-{h[8:12]}-{h[12:16]}-{h[16:20]}-{h[20:32]}"


def _build_postman_item(path: str, method: str, op: dict, spec: dict) -> dict:
    pm_path = []
    for part in path.split("/"):
        if not part:
            continue
        if part.startswith("{") and part.endswith("}"):
            pm_path.append(":" + part[1:-1])
        else:
            pm_path.append(part)

    query_params: list[dict] = []
    path_vars: list[dict] = []
    for p in op.get("parameters", []):
        loc = p.get("in")
        if loc == "query":
            query_params.append({
                "key": p["name"],
                "value": _scalar_example(p.get("schema", {})),
                "description": p.get("description", ""),
                "disabled": not p.get("required", False),
            })
        elif loc == "path":
            path_vars.append({
                "key": p["name"],
                "value": _scalar_example(p.get("schema", {})),
                "description": p.get("description", ""),
            })

    headers = [{"key": "Accept", "value": JSON_MIME, "type": "text"}]

    raw_url = "{{baseUrl}}/" + "/".join(pm_path)
    active = [q for q in query_params if not q.get("disabled")]
    if active:
        raw_url += "?" + "&".join(f"{q['key']}={q['value']}" for q in active)

    request = {
        "method": method.upper(),
        "header": headers,
        "url": {
            "raw": raw_url,
            "host": ["{{baseUrl}}"],
            "path": pm_path,
            "query": query_params,
            "variable": path_vars,
        },
        "description": op.get("description") or op.get("summary") or "",
    }

    body_spec = op.get("requestBody", {}) or {}
    content = body_spec.get("content", {})
    if JSON_MIME in content:
        example = _example_from_schema(content[JSON_MIME].get("schema", {}), spec)
        request["body"] = {
            "mode": "raw",
            "raw": json.dumps(example, indent=2, ensure_ascii=False),
            "options": {"raw": {"language": "json"}},
        }
        headers.append({"key": "Content-Type", "value": JSON_MIME, "type": "text"})
    elif "multipart/form-data" in content:
        schema = content["multipart/form-data"].get("schema", {})
        formdata = []
        for prop_name, prop_schema in (schema.get("properties") or {}).items():
            is_file = prop_schema.get("format") == "binary"
            formdata.append({
                "key": prop_name,
                "type": "file" if is_file else "text",
                "value": "" if is_file else _scalar_example(prop_schema),
            })
        request["body"] = {"mode": "formdata", "formdata": formdata}

    return {
        "name": op.get("summary") or f"{method.upper()} {path}",
        "request": request,
        "response": [],
    }


def _scalar_example(schema: dict) -> str:
    if "$ref" in schema:
        return ""
    typ = schema.get("type", "")
    if typ == "integer":
        return "0"
    if typ == "number":
        return "0.0"
    if typ == "boolean":
        return "false"
    if schema.get("enum"):
        return str(schema["enum"][0])
    return ""


def _example_from_schema(schema: dict, spec: dict, seen: set | None = None) -> object:
    if seen is None:
        seen = set()
    if "$ref" in schema:
        ref = schema["$ref"]
        if ref in seen:
            return {}
        seen = seen | {ref}
        parts = ref.lstrip("#/").split("/")
        resolved: object = spec
        for part in parts:
            resolved = (resolved or {}).get(part, {}) if isinstance(resolved, dict) else {}
        return _example_from_schema(resolved if isinstance(resolved, dict) else {}, spec, seen)
    if schema.get("allOf"):
        merged: dict = {}
        for sub in schema["allOf"]:
            piece = _example_from_schema(sub, spec, seen)
            if isinstance(piece, dict):
                merged.update(piece)
        return merged
    if schema.get("oneOf") or schema.get("anyOf"):
        return _example_from_schema((schema.get("oneOf") or schema.get("anyOf"))[0], spec, seen)
    typ = schema.get("type", "")
    if typ == "object" or "properties" in schema:
        return {
            name: _example_from_schema(prop, spec, seen)
            for name, prop in (schema.get("properties") or {}).items()
        }
    if typ == "array":
        return [_example_from_schema(schema.get("items", {}), spec, seen)]
    if typ == "integer":
        return 0
    if typ == "number":
        return 0.0
    if typ == "boolean":
        return False
    if typ == "string":
        fmt = schema.get("format", "")
        if fmt == "date-time":
            return "2026-01-01T00:00:00Z"
        if fmt == "date":
            return "2026-01-01"
        if fmt == "email":
            return "user@example.com"
        if fmt == "uuid":
            return "00000000-0000-0000-0000-000000000000"
        if schema.get("enum"):
            return schema["enum"][0]
        return schema.get("default", "")
    return None


# ────────────────────────────────────────────────────────────────────────
# Usage report (markdown)
# ────────────────────────────────────────────────────────────────────────
def write_usage_report(endpoints: list[tuple[str, str, str]], matched: set[tuple[str, str]]) -> str:
    lines = [
        "# Android API consumption",
        "",
        "Auto-generated by `scripts/gen-android-api-docs.py` from the Retrofit",
        "interfaces under `app/src/main/java/com/bughunter/core/network/api/`.",
        "",
        f"**Total endpoints called: {len(endpoints)}**",
        "",
        "## By Retrofit file",
        "",
    ]
    by_file: dict[str, list[tuple[str, str]]] = defaultdict(list)
    for method, path, kt_file in endpoints:
        by_file[kt_file].append((method, path))
    for kt_file in sorted(by_file):
        lines.append(f"### `{kt_file}.kt`")
        lines.append("")
        for method, path in sorted(by_file[kt_file]):
            in_spec = "✅" if (method, path) in matched else "⚠️ NOT IN ENTERPRISE SPEC"
            lines.append(f"- `{method:6s}` `{path}` — {in_spec}")
        lines.append("")
    return "\n".join(lines)


# ────────────────────────────────────────────────────────────────────────
# main
# ────────────────────────────────────────────────────────────────────────
def main() -> None:
    endpoints = parse_retrofit_endpoints()
    if not endpoints:
        print(f"!! Found no Retrofit annotations under {RETROFIT_DIR}", file=sys.stderr)
        sys.exit(1)

    wanted: set[tuple[str, str]] = {(m, p) for m, p, _ in endpoints}
    print(f"Found {len(wanted)} (method, path) tuples across {len({f for _, _, f in endpoints})} Retrofit files")

    if not ENTERPRISE_OPENAPI.exists():
        print(f"!! Enterprise OpenAPI not found at {ENTERPRISE_OPENAPI}", file=sys.stderr)
        print("   Run scripts/gen-api-docs.py in the enterprise repo first, or", file=sys.stderr)
        print("   override with ENTERPRISE_OPENAPI=<path>", file=sys.stderr)
        sys.exit(1)

    enterprise_spec = json.loads(ENTERPRISE_OPENAPI.read_text(encoding="utf-8"))
    filtered = filter_openapi(enterprise_spec, wanted)
    matched: set[tuple[str, str]] = {
        (m.upper(), p) for p, ops in filtered.get("paths", {}).items()
        for m in ops if m.upper() in {"GET", "POST", "PUT", "PATCH", "DELETE"}
    }
    unmatched = wanted - matched
    if unmatched:
        print(f"!! {len(unmatched)} Android-called paths not found in enterprise OpenAPI:")
        for m, p in sorted(unmatched):
            print(f"     {m} {p}")

    OUT_DIR.mkdir(parents=True, exist_ok=True)

    (OUT_DIR / "openapi.json").write_text(
        json.dumps(filtered, indent=2, ensure_ascii=False), encoding="utf-8")
    (OUT_DIR / "openapi.yaml").write_text(
        yaml.safe_dump(filtered, sort_keys=False, allow_unicode=True), encoding="utf-8")

    postman = openapi_to_postman(filtered)
    # Named to match the convention used by the backend repos'
    # scripts/gen-api-docs.py — Postman shows the file name on import,
    # so a distinct name avoids collisions when all three collections
    # live in the same Postman workspace.
    (OUT_DIR / "Bug-Hunter-Android.postman_collection.json").write_text(
        json.dumps(postman, indent=2, ensure_ascii=False), encoding="utf-8")

    (OUT_DIR / "android-api-usage.md").write_text(
        write_usage_report(endpoints, matched), encoding="utf-8")

    print(f"Wrote OpenAPI + Postman + usage report to {OUT_DIR}")
    print(f"  Endpoints called:    {len(wanted)}")
    print(f"  Matched in spec:     {len(matched)}")
    print(f"  Unmatched (warning): {len(unmatched)}")


if __name__ == "__main__":
    main()
