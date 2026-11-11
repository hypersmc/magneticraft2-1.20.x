import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

// This validates a design manifest, NOT Minecraft Java, meshes or gameplay.
const layout = JSON.parse(readFileSync(new URL('../../docs/steam0a-firebox-v2-layout.json', import.meta.url), 'utf8'));
const key = p => p.join(',');
const count = cells => cells.reduce((a,c) => { const id=c.id.split(':').at(-1); a[id]=(a[id]??0)+1; return a; }, {});
const rotate = ([x,y,z], f) => ({south:[x,y,z],west:[-z,y,x],north:[-x,y,-z],east:[z,y,-x]})[f];
const adjacent = (a,b) => a.reduce((n,v,i)=>n+Math.abs(v-b[i]),0) === 1;
const [x0,y0,z0] = layout.firebox.origin;
const firebox = [];
for (const [y, rows] of layout.firebox.layers.entries()) {
  assert.equal(rows.length, layout.firebox.dimensions_xyz[2]);
  for (const [z, row] of rows.entries()) {
    assert.equal(row.length, layout.firebox.dimensions_xyz[0]);
    for (const [x, char] of [...row].entries()) {
      assert(layout.firebox.symbols[char], 'Unknown firebox symbol');
      firebox.push({p:[x0+x,y0+y,z0+z],id:layout.firebox.symbols[char]});
    }
  }
}
assert.deepEqual(count(firebox), layout.firebox.expected_ledger);
assert.equal(firebox.length, 18);
const actualRoot = firebox.filter(c => c.id.endsWith(':boiler_firebox_front'));
assert.equal(actualRoot.length,1);
assert.deepEqual(actualRoot[0].p,layout.firebox.controller_relative_to_vessel);
assert.equal(firebox.find(c=>key(c.p)===key(layout.firebox.heat_interface)).id,'magneticraft2:boiler_firebox_mount');
assert.equal(firebox.find(c=>key(c.p)===key(layout.firebox.chimney_base)).id,'magneticraft2:boiler_firebox_lining');
const fullVessel=[];
for(let x=-1;x<=1;x++)for(let y=0;y<=3;y++)for(let z=-4;z<=0;z++)fullVessel.push([x,y,z]);
const air=new Set(layout.vessel_required_air.map(key));
const vessel = fullVessel.filter(p=>!air.has(key(p)));
assert.equal(vessel.length, 53);
assert.equal(air.size,7);
assert.equal(Object.values(layout.vessel_ledger).reduce((a,b)=>a+b,0),53);
const chimney = layout.chimney.cells.map(p=>({p,id:layout.chimney.block}));
const fitting = layout.retained_fittings.map(c=>({p:c.cell,id:c.block}));
const addenda = [...firebox,...chimney,...fitting];
assert.equal(addenda.length,24);
for(const proposal of layout.new_recipe_proposals){
  assert(layout.firebox.new_ids.includes(proposal.result));
  assert.equal(proposal.inputs.length,4);
  assert(proposal.inputs.every(([id,qty])=>typeof id==='string'&&Number.isInteger(qty)&&qty>0));
  assert.equal(proposal.count,1);
}
const results=[];
for(const facing of ['south','west','north','east']) {
  const bodySet=new Set(fullVessel.map(p=>key(rotate(p,facing))));
  const physicalBody=new Set(vessel.map(p=>key(rotate(p,facing))));
  const attached=addenda.map(c=>({...c,p:rotate(c.p,facing)}));
  const attachedSet=new Set(attached.map(c=>key(c.p)));
  assert.equal(attachedSet.size,24, 'Duplicate external owner');
  for(const c of attached)assert(!bodySet.has(key(c.p)), 'External part overlaps old vessel, including reserved air');
  const total=[...vessel.map(p=>rotate(p,facing)),...attached.map(c=>c.p)];
  assert.equal(new Set(total.map(key)).size,77);
  const stack=chimney.map(c=>rotate(c.p,facing));
  assert(adjacent(rotate(layout.firebox.chimney_base,facing),stack[0]));
  assert(stack.every((p,i)=>i===0 || (adjacent(stack[i-1],p)&&p[1]===stack[i-1][1]+1)));
  for(const c of layout.retained_fittings){
    const p=rotate(c.cell,facing), support=rotate(c.support,facing);
    assert(physicalBody.has(key(support)), 'No real vessel support at fitting boundary');
    assert(adjacent(p,support));
  }
  const back=rotate(layout.firebox.heat_interface,facing);
  const contact=rotate([0,1,0],facing);
  assert(physicalBody.has(key(contact)) && adjacent(back,contact));
  for(const p of layout.front_access_clear_cells){
    const v=key(rotate(p,facing));
    assert(!bodySet.has(v)&&!attachedSet.has(v), 'Blocked stoking access');
  }
  // Every extension/chimney cell is connected to the real ground-level base.
  const structural=[...firebox,...chimney].map(c=>rotate(c.p,facing));
  const visited=new Set([key(structural[0])]);
  for(let changed=true;changed;){
    changed=false;
    for(const p of structural)if(!visited.has(key(p)) && structural.some(q=>visited.has(key(q))&&adjacent(p,q))){visited.add(key(p));changed=true;}
  }
  assert.equal(visited.size,21,'Disconnected structural group');
  const dims=[0,1,2].map(i=>Math.max(...total.map(p=>p[i]))-Math.min(...total.map(p=>p[i]))+1);
  assert.deepEqual(dims,(facing==='east'||facing==='west')?[7,6,4]:layout.overall_envelope_xyz);
  results.push({facing,external_parts:attached.length,total_blocks:total.length,envelope:dims});
}
const legacy=key(layout.legacy_chimney_cell);
assert(firebox.some(c=>key(c.p)===legacy&&c.id.endsWith(':boiler_firebox_lining')),'Migration collision must be documented');
assert.equal(layout.totals.vessel+layout.totals.firebox+layout.totals.chimney+layout.totals.retained_fittings,layout.totals.complete_installation);
console.log(JSON.stringify({result:'PASS',scope:'design coordinates and part ledger only',facings:results,preserved_vessel:53,required_air:7,new_external_parts:24,legacy_chimney_requires_manual_removal:true},null,2));
console.log('NOT VERIFIED: Forge compilation, model/collision contact, installation, drops, save/reload, multiplayer.');
