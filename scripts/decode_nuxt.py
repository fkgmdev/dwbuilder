"""Decode Nuxt 4 __NUXT_DATA__ (data-nuxt-data) payloads."""
import json, re, sys, urllib.request

def decode(htmltext):
    m = re.search(r'<script type="application/json" data-nuxt-data="nuxt-app" data-ssr="true" id="__NUXT_DATA__">(.*?)</script>', htmltext, re.S)
    if not m:
        return None
    data = json.loads(m.group(1))

    # The graph: every string is stored once as a cell; other cells reference by integer index.
    # Wrappers: ["Reactive", i], ["ShallowReactive", i]
    resolved = {}

    def resolve(idx, depth=0):
        if idx in resolved:
            return resolved[idx]
        cell = data[idx]
        if isinstance(cell, list) and len(cell) == 2 and isinstance(cell[1], int) and cell[0] in ("Reactive", "ShallowReactive"):
            resolved[idx] = ("REACTIVE", resolve(cell[1], depth+1))
            return resolved[idx]
        if isinstance(cell, dict):
            out = {}
            for k, v in cell.items():
                out[k] = resolve(v, depth+1) if isinstance(v, int) else v
            resolved[idx] = out
            return out
        if isinstance(cell, list):
            out = []
            for v in cell:
                out.append(resolve(v, depth+1) if isinstance(v, int) else v)
            resolved[idx] = out
            return out
        # primitive (string/number/bool/None) -> use as-is
        resolved[idx] = cell
        return cell

    out = resolve(1)   # root object {"data":..., ...}
    # strip REACTIVE wrappers
    def strip(x):
        if isinstance(x, tuple) and x and x[0] == "REACTIVE":
            return strip(x[1])
        if isinstance(x, dict):
            return {k: strip(v) for k, v in x.items()}
        if isinstance(x, list):
            return [strip(v) for v in x]
        return x
    return strip(out)

def fetch(url, ua="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"):
    req = urllib.request.Request(url, headers={"User-Agent": ua, "Accept": "text/html,application/xhtml+xml"})
    with urllib.request.urlopen(req, timeout=40) as r:
        return r.read().decode("utf-8", "replace")

if __name__ == "__main__":
    url = sys.argv[1]
    htmltxt = fetch(url)
    d = decode(htmltxt)
    data = d.get("data", {})
    print("URL:", url)
    print("data keys:", list(data.keys()) if isinstance(data, dict) else type(data))
    for k, v in (data.items() if isinstance(data, dict) else []):
        if isinstance(v, list):
            print(f"  {k}: {len(v)} items; first keys: {list(v[0].keys()) if v and isinstance(v[0], dict) else v[:3]}")