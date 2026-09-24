const f=["2","3","4"];function i(e){if(!e||e.length===0)return!1;const t=new Set(f);for(const n of e){const r=typeof n=="string"?n:n.id;if(r&&t.has(r))return!0}return!1}export{i};
