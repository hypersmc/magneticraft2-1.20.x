#!/usr/bin/env node
// Editable Foundry geometry: designs/*.json -> non-vanilla canonical sources/*.json.
// Static visible faces only: internal faces and same-plane material overlays
// are removed before V2 clips the model into chunk-baked, vanilla-sized parts.
import fs from "node:fs";
import path from "node:path";
const SIDE={"north":[2,-1,0,1],"south":[2,1,0,1],"west":[0,-1,2,1],"east":[0,1,2,1],"up":[1,1,0,2],"down":[1,-1,0,2]}, FACES=Object.keys(SIDE), EPS=1e-6;
function sortUnique(values){return [...new Set(values.map(v=>+v.toFixed(5)))].sort((a,b)=>a-b);}

function compile(design){
 if(design.schema!==1||!Array.isArray(design.boxes)||!design.boxes.length)throw Error("Unsupported/empty multiblock design");
 for(const box of design.boxes){
  if(!Object.hasOwn(design.textures,box.material)||box.from.length!==3||box.to.length!==3||
     !box.from.every((v,i)=>Number.isFinite(v)&&Number.isFinite(box.to[i])&&box.to[i]>v))
      throw Error("Invalid material or cuboid "+box.name);
 }
 const boxes=design.boxes,patches=[];
 for(let i=0;i<boxes.length;i++){
  const {from:f,to:t}=boxes[i];
  for(const face of FACES){
   const [axis,sign,u,v]=SIDE[face], plane=sign>0?t[axis]:f[axis], related=[];
   for(let j=0;j<boxes.length;j++){
    if(i===j)continue;
    const b=boxes[j],o=b.from,p=b.to;
    if(Math.max(f[u],o[u])+EPS>=Math.min(t[u],p[u])||
       Math.max(f[v],o[v])+EPS>=Math.min(t[v],p[v]))continue;
    const outside=plane+sign*1e-4;
    if((o[axis]<outside&&p[axis]>outside)
       ||(j>i&&Math.abs((sign>0?p[axis]:o[axis])-plane)<EPS))related.push({j,b});
   }
   const us=sortUnique([f[u],t[u],...related.flatMap(x=>[Math.max(f[u],x.b.from[u]),Math.min(t[u],x.b.to[u])])]);
   const vs=sortUnique([f[v],t[v],...related.flatMap(x=>[Math.max(f[v],x.b.from[v]),Math.min(t[v],x.b.to[v])])]);
   for(let a=0;a<us.length-1;a++)for(let b=0;b<vs.length-1;b++){
    const u0=us[a],u1=us[a+1],v0=vs[b],v1=vs[b+1];
    if(u1-u0<EPS||v1-v0<EPS)continue;
    const mx=(u0+u1)/2,my=(v0+v1)/2,q=plane+sign*1e-4;
    const covered=related.some(({j,b})=>
      mx>b.from[u]+EPS&&mx<b.to[u]-EPS&&my>b.from[v]+EPS&&my<b.to[v]-EPS&&
      ((b.from[axis]<q&&b.to[axis]>q)||(j>i&&Math.abs((sign>0?b.to[axis]:b.from[axis])-plane)<EPS)));
    if(!covered)patches.push({face,plane,material:boxes[i].material,u0,u1,v0,v1});
   }
  }
 }
 const groups=new Map();
 for(const p of patches){
  const k=`${p.face}:${p.plane.toFixed(5)}:${p.material}`;
  if(!groups.has(k))groups.set(k,[]);
  groups.get(k).push({...p});
 }
 const result=[];
 for(const g of groups.values()){
  let changed=true;
  while(changed){
   changed=false;
   loop:for(let i=0;i<g.length;i++)for(let j=i+1;j<g.length;j++){
    const a=g[i],b=g[j];let merged=null;
    if(Math.abs(a.v0-b.v0)<EPS&&Math.abs(a.v1-b.v1)<EPS &&
       (Math.abs(a.u1-b.u0)<EPS||Math.abs(b.u1-a.u0)<EPS))
       merged={...a,u0:Math.min(a.u0,b.u0),u1:Math.max(a.u1,b.u1)};
    if(Math.abs(a.u0-b.u0)<EPS&&Math.abs(a.u1-b.u1)<EPS &&
       (Math.abs(a.v1-b.v0)<EPS||Math.abs(b.v1-a.v0)<EPS))
       merged={...a,v0:Math.min(a.v0,b.v0),v1:Math.max(a.v1,b.v1)};
    if(merged){g[i]=merged;g.splice(j,1);changed=true;break loop;}
   }
  }
  result.push(...g);
 }
 const elements=result.map(p=>{
  const [axis,sign,u,v]=SIDE[p.face],from=[0,0,0],to=[0,0,0];
  from[u]=p.u0;to[u]=p.u1;from[v]=p.v0;to[v]=p.v1;
  if(sign>0){from[axis]=p.plane-.0625;to[axis]=p.plane;}
  else {from[axis]=p.plane;to[axis]=p.plane+.0625;}
  return {from,to,faces:{[p.face]:{texture:"#"+p.material,uv:[p.u0,p.v0,p.u1,p.v1]}}};
 });
 return {parent:"minecraft:block/block",ambientocclusion:false,uvMode:"world",textures:design.textures,elements};
}

function clipSingleBlock(model){
 const results=[],wrap=x=>((x%16)+16)%16;
 for(const el of model.elements){
  const f=Object.keys(el.faces)[0],data=el.faces[f],[axis,sign,u,v]=SIDE[f],b=[...el.from,...el.to];
  for(let y=Math.floor(b[1]/16);y<Math.ceil((b[4]-EPS)/16);y++)
  for(let z=Math.floor(b[2]/16);z<Math.ceil((b[5]-EPS)/16);z++)
  for(let x=Math.floor(b[0]/16);x<Math.ceil((b[3]-EPS)/16);x++){
   const c=[Math.max(b[0],x*16),Math.max(b[1],y*16),Math.max(b[2],z*16),
    Math.min(b[3],(x+1)*16),Math.min(b[4],(y+1)*16),Math.min(b[5],(z+1)*16)];
   if(Math.min(c[3]-c[0],c[4]-c[1],c[5]-c[2])<=EPS)continue;
   if(Math.abs((sign<0?c[axis]:c[axis+3])-(sign<0?b[axis]:b[axis+3]))>EPS)continue;
   const U=wrap(c[u]),V=wrap(c[v]),UV=[U,V,U+c[u+3]-c[u],V+c[v+3]-c[v]];
   if([...c,...UV].some((p,i)=>i<6?(p<-16-EPS||p>32+EPS):(p<-EPS||p>16+EPS)))
     throw Error("Unformed controller cannot be baked inside vanilla bounds "+JSON.stringify({c,UV,f}));
   results.push({from:c.slice(0,3).map(p=>+p.toFixed(5)),
     to:c.slice(3).map(p=>+p.toFixed(5)),
     faces:{[f]:{texture:data.texture,uv:UV.map(p=>+p.toFixed(5))}}});
  }
 }
 return {parent:"minecraft:block/block",ambientocclusion:false,textures:model.textures,elements:results};
}

function verifyNoCoplanar(model){
 const byPlane=new Map();
 for(let i=0;i<model.elements.length;i++){
  const el=model.elements[i];
  for(const f of Object.keys(el.faces)){
   const [axis,sign,u,v]=SIDE[f],coord=sign>0?el.to[axis]:el.from[axis],key=f+":"+coord.toFixed(5);
   const group=byPlane.get(key)||[];
   for(const prev of group)if(Math.min(prev.to[u],el.to[u])-Math.max(prev.from[u],el.from[u])>EPS
          &&Math.min(prev.to[v],el.to[v])-Math.max(prev.from[v],el.from[v])>EPS)
     throw Error("Coplanar generated faces "+key+" "+i);
   group.push(el);byPlane.set(key,group);
  }
 }
}

const args=new Set(process.argv.slice(2)),root=process.cwd();
const dir=path.join(root,"tools/multiblock-models/designs");
const sources=path.join(root,"tools/multiblock-models/sources");
if(args.has("--self-test")){
 const tiny={schema:1,textures:{brick:"minecraft:block/bricks"},
  boxes:[{name:"a",material:"brick",from:[0,0,0],to:[16,16,16]},
         {name:"b",material:"brick",from:[0,0,0],to:[16,16,16]}]};
 const compiled=compile(tiny);verifyNoCoplanar(compiled);
 if(compiled.elements.length!==6)throw Error("Duplicated boxes should leave six outside faces");
 console.log("Occluded/coincident-solid faces eliminated");
 process.exit(0);
}
let errors=0;
for(const filename of fs.readdirSync(dir).filter(f=>f.endsWith(".json")).sort()){
 const id=filename.slice(0,-5),design=JSON.parse(fs.readFileSync(path.join(dir,filename),"utf8"));
 const canonical=compile(design);verifyNoCoplanar(canonical);
 const writes=new Map([[path.join(sources,id+".json"),JSON.stringify(canonical,null,2)+"\n"]]);
 if(id==="bronze_foundry"){
  const filtered={...design,boxes:design.boxes.filter(b=>/^(firebox_|grate_|bezel_)/.test(b.name))};
  const controller=clipSingleBlock(compile(filtered));verifyNoCoplanar(controller);
  writes.set(path.join(root,"src/main/resources/assets/magneticraft2/models/block/bronze_foundry_controller.json"),
   JSON.stringify(controller,null,2)+"\n");
 }
 for(const [filepath,contents] of writes){
  const fixed=contents.replace(/\\\\n$/, "\\n");
  if(args.has("--check")){
   if(!fs.existsSync(filepath)||fs.readFileSync(filepath,"utf8")!==fixed){
    console.error("Outdated:",path.relative(root,filepath));errors++;
   }
  }else{
   fs.mkdirSync(path.dirname(filepath),{recursive:true});
   fs.writeFileSync(filepath,fixed,"utf8");
  }
 }
 console.log(id+": "+design.boxes.length+" solids -> "+canonical.elements.length+" visible face panels");
}
if(errors)process.exit(1);
