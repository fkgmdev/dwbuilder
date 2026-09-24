# Site rule engine — exact extracts from www/_nuxt bundles

These are direct (de-minified annotations of) functions from the live builder bundles.
Treat them as the authoritative spec. File names refer to `www/_nuxt/`.

## Points / Power — `KwKd8cfB.js`

```js
// sum of values in object
function Vo(e){ let t=0; for(const n of Object.values(e)) t += (n??0); return t; }
// max of values in object
function Wo(e){ let t=0; for(const n of Object.values(e)) (n??0)>t && (t=n??0); return t; }
const Rb = 330;                       // total attribute budget
function Pn(e){                       // total points spent
  let t = Vo(e.base) + Vo(e.weapon) + Vo(e.attunement);
  let n = false;                      // "already have an attunement ≥1"
  for(const r of Object.values(e.attunement)){
    if(n && (r??0) > 0) t--;          // −1 per extra attunement past the first
    if((r??0) >= 1) n = true;
  }
  return t;
}
function Jn(e){ const t=Pn(e); const n=Math.floor((t-30+15)/15); return Math.max(0,Math.min(20,n)); } // Power
function Ab(e){ return Rb - Pn(e); }  // points left
function Ob(e){                       // points to next power (site-exact quirk included)
  const t=Pn(e), n=Jn(e);
  return (n===1 && 30-t>0) ? 30-t
       : (n<20 && 15-(t-15)%15 !== 0) ? 15-(t-15)%15
       : 0;
}
```

- `attributes` shape: `{ base:{Strength,Ftitude,Agility,Intelligence,Willpower,Charisma},
  weapon:{"Heavy Wep.","Medium Wep.","Light Wep."},
  attunement:{Flamecharm,Frostdraw,Thundercall,Galebreathe,Shadowcast,Ironsing,Bloodrend} }`
- Stat accessor map `Pb` (for requirement checks): base six → `e.base.X`; Heavy/Medium/Light Weapon (and "Heavy Wep." etc.) → `e.weapon[...]`; attunements → `e.attunement.X`; `Power` → `Jn(e)`; `Body` → max(STR,AGL,FTD); `Mind` → max(INT,WLL,CHA); `Weapon`/`Weapons` → `Wo(e.weapon)`; `Attunement` → `Wo(e.attunement)`.

## Requirement checker — `KwKd8cfB.js` `nr(requirements, ctx, recursionSet, cache, shrineCache)`

```js
function nr(e,t,n=new Set,r,s){           // e=req obj, t=ctx (build+attributes+takenTalents+grantedTalents+getTalentByName)
  if(!e) return Cb;                        // Cb = {hardMet:true, warnings:[]}
  const o=[];
  if(e.stats){
    for(const [i,a] of Object.entries(e.stats)){
      const l = Math.max(0, a - Ib(t.build, i, "talent"));   // Ib with "talent" => 0 (see below)
      const u = Mi(t.attributes, i);
      if(u < l) o.push(`${i}: requires ${l}, currently ${u} (need ${l-u} more)`);
    }
    if(o.length) return {hardMet:false, warnings:o};
  }
  if(e.or && e.or.length>0){
    let i=null; const a=[];
    const u = Mi(t.attributes,"Attunement")>0 && e.or.some(c=>(c.stats?.Attunement??0)>0)
      ? e.or.filter(c=>(c.stats?.Attunement??0)>0) : e.or;
    for(const c of u){
      const f=nr(c,t,n,r,s);
      if(f.hardMet || a.push(f.warnings.join("; ")||"Requirements not met"),
         f.hardMet && ((!i || f.warnings.length<i.warnings.length) && (i=f), f.warnings.length===0)) break;
    }
    if(!i) return {hardMet:false, warnings:[`Requires one alternative: ${a.join(" OR ")}`]};
    o.push(...i.warnings);
  }
  if(e.talents) for(const i of e.talents){
    const a=t.takenTalents.has(i); if(t.grantedTalents?.has(i)) continue;
    if(n.has(i)){ if(!a) o.push(`Requires talent: ${i}`); continue; }
    const l=t.getTalentByName(i);
    if(l){
      let u=r?.get(l.name);
      if(u || (n.add(l.name), u=nr(l.requirements,t,n,r,s), n.delete(l.name), r?.set(l.name,u)),
         !u.hardMet && s){ /* shrine-recheck w/ s.ctx + s.cache */ }
      else if(!u.hardMet) return {hardMet:false, warnings:[`${i}: ${u.warnings.join("; ")||"Requirements not met"}`]};
    }
    if(!a) o.push(`Requires talent: ${i}`);
  }
  if(e.origin && t.build.stats.meta.Origin!==e.origin) o.push(`Requires Origin: ${e.origin}`);
  if(e.murmur && t.build.stats.meta.Murmur!==e.murmur) o.push(`Requires Murmur: ${e.murmur}`);
  if(e.aspect && t.build.stats.meta.Race!==e.aspect) o.push(`Requires Race: ${e.aspect}`);
  if(e.weaponType){
    const i=t.build.weapons?t.getWeaponByName(t.build.weapons):void 0; const a=new Set();
    for(const l of Object.keys(i?.scaling??{})) if(["Heavy Weapon","Medium Weapon","Light Weapon"].includes(l)) a.add(l);
    if(i?.type){ for(const l of i.type.split(/\s*\/\s*/)){ a.add(l); if(l==="Rifle") a.add("Rifles"); if(l==="Fists") a.add("Fist"); } if(i.type==="Fist") a.add("Fists"); }
    if(!a.has(e.weaponType)) o.push(`Requires Weapon Type: ${e.weaponType}`);
  }
  return {hardMet:true, warnings:o};
}
```

- `Ib(e, t, n)`: only non-zero for weapon requirements: Khan race → +3 to all stats except `Power`; Silentheart oath → +25 to Heavy/Medium/Light Weapon (incl. abbreviations).
- `Cb = {hardMet:true, warnings:[]}`.

## Talent eligibility — `KwKd8cfB.js` `Kw(talent, ctx)` (independent of points)

```js
function Kw(e,t){
  const n=t.stats.meta.Oath, r=t.stats.meta.Origin, s=t.stats.meta.Race;
  if(e.rarity==="Oath" && e.category && e.category!==n) return false;   // oath talent requires matching oath
  for(const o of e.attributes??[])
    if((o.startsWith("Oath: ")&&o.slice(6)!==n)||(o.startsWith("Origin: ")&&o.slice(8)!==r)) return false;
  return !( e.requirements?.origin && e.requirements.origin!==r
    || (e.rarity==="Origin" && !e.requirements?.origin && !e.requirements?.or?.length && e.category && e.category!==r)
    || (e.rarity==="Innate" && e.requirements?.aspect && e.requirements.aspect!==s) );
}
```

## Mantra slots — `KwKd8cfB.js`

```js
const Lb={Combat:3,Mobility:1,Support:1,Wisp:0,Wildcard:1};          // base slots
function zw(e,t){                                                     // e = taken talents array, ctx
  const n={...Lb}, r=t(e.stats.meta.Oath);
  if(r?.slots){ n.Combat+=r.slots.Combat??0; n.Mobility+=r.slots.Mobility??0;
                n.Support+=r.slots.Support??0; n.Wildcard+=r.slots.Wildcard??0; }
  if(e.talents.includes("Neuroplasticity")) n.Wildcard++;
  if(e.talents.some(s=>s.includes("Will o' Wisp"))) n.Wisp++;
  if(e.talents.some(s=>s.includes("Chorus of Souls"))) n.Wisp++;
  return n;
}
// assign each taken mantra to a slot bucket with overflow
function Qw(e,t,n){ ... counts/buckets/overflow; category must be Combat/Mobility/Support/Wisp;
  if bucket for its category has room -> use it; else Wisp->Support if room; else Wildcard if room; else overflow++ }
```

## Shrine constants — `KwKd8cfB.js`

```js
const qo = 25;   // max points drained per stat per shrine pass
const Db = 100;  // stat cap
```

- `$b` = shrine distribution: average the stats, then iteratively cap stats above `base + 25`, redistribute, floor, cap 100.

## Weapon enchants (8) — `CkKCXiBo.js` `Z`

None, Iron (+10% PEN), Gold (−10% dmg, slow), Umbrite (+10% dmg, wither, 25% slower),
Erisore (−20% dmg, 50% anti-heal), Irithine (−20% dmg, shaky block), Gale (−5% ground/+25% air), Frost (−5% dmg, freeze after 7 hits).

## Hardcoded pickers — `C74yZVe_.js`

- Murmurs: `["Ardour","Rhythm","Tacet"]`
- Origins: `["Castaway","Authority Ensign","Deepbound","Ignition Delver","Lone Warrior","Voidwalker","Justicar"]`
- Bells: `["None","Blood Scourge","Crazy Slots","Chorus Divide","Dimensional Travel","Gravity Field","Jar Of Souls","Paralytic Dust","Payback","Portals","Preservation","Resurrection","Run It Back","Sacred Field","Shard Bow","Skeleton Key","Smite","Smokescreen","Teleportation","Wind Up"]`
- Oath requirements (map): Oathless "No Stat Requirement", Arcwarder "20 FIR + 20 LTN + 20 FTD", Bladeharper "(75 MED or comb. 90 WEP) + (25 STR or AGL)", Blightsurger "(comb. 80 STR, FTD, AGL) + (40 WND or LTN)", Blindseer "40 WLL", Chainwarden "comb. 40 STR, FTD, WLL", Contractor "No Stat Requirement", Dawnwalker "LVL 15", Fadetrimmer "LVL 12", Jetstriker "50 AGL", Linkstrider "No Stat Requirement", Saintsworn "15 FIR + 15 ICE + 15 LTN + 15 WND + 15 SDW", Saltchemist "75 INT", Silentheart "(comb. 75 WEP) + (25 STR) + (25 AGL or CHA)", Soulbreaker "comb. 50 WLL, CHA", Starkindred "40 STR", Visionshaper "50 CHA"

## PvE calc — `C74yZVe_.js` (`xr`)

- `hit = weaponHit * powerScaling(power) * (1 + DVM/100) * resistanceMultiplier`
- resistance defaults 25% (per-enemy `resistance` rarely present), UI override 0–100
- enemy health parsed from `health` + `healthAdditives` (e.g. `"1,000 (Knight)"` additive)
- power scaling reaches 7.56× at Power 20

## Misc

- Defense base values (C74yZVe_.js SSR text): base HP 210 + Power, Posture 20, Ether 120, Tempo 120, Sanity +80, Carry Load +130.
- Talent caps: 76 total (50 rollable / 40 guaranteed rare attempts / 24 spare), faction max 4 (5 w/ Authority Ensign's Command Division), Oath/Quest exempt; each non-exempt mantra costs 2 talent slots.
- Draft persistence: `localStorage._dwb.draft.v1.{buildId}` = `{version:1, build, phase, timestamp}`.
- Build model: `{name, level, race, origin, oath, attributes{base[6]+weapon[3]+attunement[7]}, mantras[], talents[], equipment, boons/flaws, bell, murmur, shrineModes}`.