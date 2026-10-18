#!/usr/bin/env node
// Stamp Mill-specific outer frame: derived from the accepted 63-face input
// model without double-rendering the real Mechanical Input Module's wood
// rotor, bearing housing or their swept clearance.
import fs from "node:fs";
import path from "node:path";

const buildDetail=function buildDetail(legacy,housing,wood){
  const e=1e-6,round=n=>+n.toFixed(5);
  if(legacy.elements.length!==63)throw Error("Original Stamp Mill frame changed; re-evaluate central/outer panel ownership");
  const axes={north:[2,0,1],south:[2,0,1],east:[0,2,1],west:[0,2,1],up:[1,0,2],down:[1,0,2]};
  const faceBlock=b=>({from:[b.from[0],16-b.to[2],b.from[1]],to:[b.to[0],16-b.from[2],b.to[1]]});
  // The reusable Mechanical Input Module renders these real solids after
  // mapping canonical Y to the machine's local Z. None belong to this mesh.
  const occupied=[...housing.elements.map(faceBlock),...wood.elements.map(faceBlock)];
  const output=[];
  // Original 0..42 are outside timber/copper frame surfaces; 43..62
  // are the old central iron sleeve and copper bearing around the rotor.
  for(const panel of legacy.elements.slice(0,43)){
    const faces=Object.keys(panel.faces);
    if(faces.length!==1)throw Error("Expected one visible surface per original frame element");
    const face=faces[0],originalFace=panel.faces[face],[normal,u,v]=axes[face];
    if(!axes[face])throw Error("Invalid legacy frame face "+face);
    const cuts=occupied.filter(b=>
      Math.min(panel.to[normal],b.to[normal])-Math.max(panel.from[normal],b.from[normal])>e
      &&[u,v].every(a=>Math.min(panel.to[a],b.to[a])-Math.max(panel.from[a],b.from[a])>e));
    const splits=ax=>[...new Set([panel.from[ax],panel.to[ax],
      ...cuts.flatMap(b=>[Math.max(panel.from[ax],b.from[ax]),Math.min(panel.to[ax],b.to[ax])])].map(round))].sort((a,b)=>a-b);
    const us=splits(u),vs=splits(v),patches=[];
    for(let i=0;i<us.length-1;i++)for(let j=0;j<vs.length-1;j++){
      const [u0,u1,v0,v1]=[us[i],us[i+1],vs[j],vs[j+1]];
      if(u1-u0<e||v1-v0<e)continue;
      const uc=(u0+u1)/2,vc=(v0+v1)/2;
      if(cuts.some(b=>uc>b.from[u]+e&&uc<b.to[u]-e&&vc>b.from[v]+e&&vc<b.to[v]-e))continue;
      patches.push({u0,u1,v0,v1});
    }
    // Merge adjacent retained rectangles into large quads: avoids a face
    // explosion when the housing clips one small corner of a long rail.
    let changed=true;
    while(changed){
      changed=false;
      outer:for(let i=0;i<patches.length;i++)for(let j=i+1;j<patches.length;j++){
        const a=patches[i],b=patches[j];let joined=null;
        if(Math.abs(a.v0-b.v0)<e&&Math.abs(a.v1-b.v1)<e
          &&(Math.abs(a.u1-b.u0)<e||Math.abs(b.u1-a.u0)<e))
          joined={u0:Math.min(a.u0,b.u0),u1:Math.max(a.u1,b.u1),v0:a.v0,v1:a.v1};
        else if(Math.abs(a.u0-b.u0)<e&&Math.abs(a.u1-b.u1)<e
          &&(Math.abs(a.v1-b.v0)<e||Math.abs(b.v1-a.v0)<e))
          joined={u0:a.u0,u1:a.u1,v0:Math.min(a.v0,b.v0),v1:Math.max(a.v1,b.v1)};
        if(joined){patches[i]=joined;patches.splice(j,1);changed=true;break outer;}
      }
    }
    for(const p of patches){
      const from=[...panel.from],to=[...panel.to];
      from[u]=p.u0;to[u]=p.u1;from[v]=p.v0;to[v]=p.v1;
      const uv=(n,pos,axis)=>round(originalFace.uv[n]+
        (pos-panel.from[axis])/(panel.to[axis]-panel.from[axis])*
          (originalFace.uv[n+2]-originalFace.uv[n]));
      output.push({from,to,faces:{[face]:{texture:originalFace.texture,
        uv:[uv(0,p.u0,u),uv(1,p.v0,v),uv(0,p.u1,u),uv(1,p.v1,v)]}}});
    }
  }
  return {parent:"minecraft:block/block",ambientocclusion:false,
    textures:{bronze:legacy.textures.bronze,dark:legacy.textures.dark,particle:legacy.textures.particle},
    elements:output};
};
const folder=path.join("src","main","resources","assets","magneticraft2","models");
const read=name=>JSON.parse(fs.readFileSync(path.join(folder,name),"utf8"));
const legacy=read("multiblock/bronze_stamp_mill_input_frame.json");
const housing=read("block/mechanical_input_module_housing.json");
const shaft=read("block/shaft_wood.json");
const output=JSON.stringify(buildDetail(legacy,housing,shaft),null,2)+"\n";
const target=path.join(folder,"multiblock/bronze_stamp_mill_input_frame_detail.json");

if(process.argv.includes("--check")){
  if(!fs.existsSync(target)||fs.readFileSync(target,"utf8")!==output){
    console.error("Stale Stamp Mill input-frame detail: regenerate using node tools/bronze-stamp-mill/build-input-frame-detail.mjs");
    process.exitCode=1;
  }
}else{
  fs.writeFileSync(target,output,"utf8");
}
