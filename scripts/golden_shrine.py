#!/usr/bin/env python3
"""Golden-fixture generator for the Shrine engine.

Transcribes the shrine of order redistribution (`$b`), point totals (`Pn`),
effective-attribute subtraction (`qb`) and derived power/points (`P`) exactly
as they appear in `www/_nuxt/KwKd8cfB.js` (and the mastery dialog of
`C74yZVe_.js`). The outputs are baked into
`android/app/src/test/resources/golden/shrine.json` so the Kotlin port can be
pinned against an independent implementation of the same source.

   const qo = 25, Db = 100;
   function $b(e, t = {}) { ... }          // shrine of order redistribution
   function Pn(e) { ... }                  // total spent w/ 2nd-attunement discount
   function qb(e, t) { ... }               // attributes minus mastery withdrawals
   Jn/Ab/Ob                               // power / points left / next power

Regenerate with:  python3 scripts/golden_shrine.py
"""
import json
import math
import os

QO = 25     # drain cap: a non-attunement stat may lose at most 25 points
DB = 100    # final per-stat cap after surplus distribution
BUDGET = 330


def clone(e):
    return {"base": dict(e["base"]), "weapon": dict(e["weapon"]), "attunement": dict(e["attunement"])}


def attrs(base=None, weapon=None, attunement=None):
    """Canonical attribute structure (missing keys default to 0 for lookups)."""
    return {"base": dict(base or {}), "weapon": dict(weapon or {}), "attunement": dict(attunement or {})}


def redistribute(e, t=None):  # `$b(e, t)`
    t = t or {}
    n = clone(e)
    r = []
    for p in ["base", "weapon", "attunement"]:
        for y in n[p]:
            r.append((p, y))

    def s(p):  # current value in working copy
        return n[p[0]][p[1]]

    def o(p, y):  # set value in working copy
        n[p[0]][p[1]] = y

    def i(p):  # racial floor (base stats only)
        return t.get(p[1], 0) if p[0] == "base" else 0

    a = clone(n)  # snapshot taken BEFORE any modification

    def l(p):  # original value
        return a[p[0]][p[1]]

    u = [p for p in r if s(p) - i(p) > 0]
    if len(u) == 0:
        return n
    c = 0
    for p in u:
        c += s(p)
    f = c / len(u)
    for p in u:
        o(p, f)  # set every disposable stat to the mean
    h = set()
    d = clone(n)
    m = True
    g = 32
    while m and g > 0:
        g -= 1
        m = False
        p = 0
        for stat in u:
            if stat[0] == "attunement" or stat in h:
                continue
            R = d[stat[0]][stat[1]]
            A = l(stat)
            x = s(stat)
            if A - x > QO:
                O = A - QO
                o(stat, O)
                p += O - R
                h.add(stat)
        y = len(u) - len(h)
        if y > 0 and p != 0:
            share = p / y
            for stat in u:
                if stat in h:
                    continue
                o(stat, s(stat) - share)
                if stat[0] != "attunement" and l(stat) - s(stat) > QO:
                    m = True
        d = clone(n)
    for p in u:
        o(p, math.floor(s(p)))
    T = 0
    for p in u:
        T += s(p)
    S = c - T
    b = [p for p in u if p not in h]
    if len(b) > 0:
        p = S
        while p >= len(b) and not any(s(yy) + 1 > DB for yy in b):
            for yy in b:
                o(yy, s(yy) + 1)
            p -= len(b)
    return n


def total_points(e):  # `Pn`
    t = sum(e["base"].values()) + sum(e["weapon"].values()) + sum(e["attunement"].values())
    had = False
    for r in e["attunement"].values():
        if had and (r or 0) > 0:
            t -= 1
        if (r or 0) >= 1:
            had = True
    return t


def minus_mastery(e, mastery):  # `qb`
    def sub(m):
        return {k: max(0, (v or 0) - (mastery.get(k, 0) or 0)) for k, v in m.items()}
    return {"base": sub(e["base"]), "weapon": sub(e["weapon"]), "attunement": sub(e["attunement"])}


def js_mod(a, b):
    """JS `%` = truncated remainder; Python's `%` floored-mod differs for negatives."""
    q = math.trunc(a / b)
    return a - q * b


def power(t):
    return max(0, min(20, math.floor((t - 30 + 15) / 15)))


def next_power(t):
    p = power(t)
    if p == 1 and 30 - t > 0:
        return 30 - t
    if p < 20 and 15 - js_mod(t - 15, 15) != 0:
        return 15 - js_mod(t - 15, 15)
    return 0


def apply_order(e, mastery, t):  # `Ee` (applyShrineOrder)
    eff = minus_mastery(e, mastery)
    c = total_points(eff)
    pre = clone(e)
    post = redistribute(eff, t)
    j = total_points(post)
    return {"pre": pre, "post": post, "sparePoints": max(0, c - j)}


def case(name, base, weapon=None, attunement=None, race=None, mastery=None):
    e = attrs(base, weapon, attunement)
    redistributed = redistribute(e, race)
    order = apply_order(e, mastery or {}, race)
    eff = minus_mastery(e, mastery or {})
    t_eff = total_points(eff)
    return {
        "name": name,
        "input": e,
        "raceBonuses": dict(race or {}),
        "mastery": dict(mastery or {}),
        "redistributed": redistributed,
        "order": {"sparePoints": order["sparePoints"], "post": order["post"]},
        "effective": eff,
        "derived": {
            "spent": total_points(eff),
            "pointsLeft": BUDGET - total_points(eff),
            "power": power(t_eff),
            "pointsUntilNextPower": next_power(t_eff),
        },
    }


CASES = [
    # 1. Empty build → unchanged, zero spare.
    case("empty", {}),
    # 2. Already-uniform stats → unchanged (mean redistribution is identity).
    case("uniform-10", {s: 10 for s in ["Strength", "Fortitude", "Agility", "Intelligence", "Willpower", "Charisma"]}),
    # 3. Single spike: 100 STR / 1 FTD → pinned at 75, the rest absorbs.
    case("spike-100-1", {"Strength": 100, "Fortitude": 1}),
    # 4. Two stats: 100 / 30 → pinned 75, other raised to 55.
    case("two-100-30", {"Strength": 100, "Agility": 30}),
    # 5. Fractional loss: 10 / 1 over 2 stats → 5 / 5 with 1 spare point.
    case("floor-loss", {"Strength": 10, "Fortitude": 1}),
    # 6. Three stats with drain + fractional floor → one spare point.
    case("three-100-30-10", {"Strength": 100, "Fortitude": 30, "Agility": 10}),
    # 7. Racial floor: STR can't drop below 20 → both settle at 20.
    case("race-floor", {"Strength": 30, "Agility": 10}, race={"Strength": 20}),
    # 8. Attunements participate in redistribution (absorb overflow, never pinned).
    case("attunement-absorb", {"Strength": 100, "Agility": 30},
         attunement={"Flamecharm": 10}),
    # 9. 6-stat spread: 100 + 5×20 → pinned at 75, rest at 25.
    case("six-100-20x5", {"Strength": 100, "Fortitude": 20, "Agility": 20,
                          "Intelligence": 20, "Willpower": 20, "Charisma": 20}),
    # 10. Pre-mastery withdrawals are subtracted before redistribution.
    case("pre-mastery", {"Strength": 100, "Fortitude": 30, "Agility": 10},
         mastery={"Strength": 25}),
    # 11. Effective stats + derived power with mastery (site's `P`).
    case("effective-power", {"Strength": 100, "Fortitude": 30, "Agility": 10},
         attunement={"Flamecharm": 20}, mastery={"Strength": 25}),
    # 12. Mastery budget cap: 50 pre + 60 post > 100 → rejected upstream (not represented here).
]

fixtures = {"qo": QO, "statCap": DB, "budget": BUDGET, "cases": CASES}
outfile = os.path.join(os.path.dirname(__file__), "..", "android", "app", "src", "test", "resources",
                       "golden", "shrine.json")
os.makedirs(os.path.dirname(outfile), exist_ok=True)
with open(outfile, "w") as f:
    json.dump(fixtures, f, indent=2, sort_keys=True)
print(f"wrote {outfile}: {len(CASES)} cases")