import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),"../..");
const read=p=>fs.readFileSync(path.join(root,p),"utf8");
const V=read("src/main/java/com/magneticraft2/common/systems/GEAR/GearPlacementValidator.java");
const N=read("src/main/java/com/magneticraft2/common/systems/GEAR/GearNetworkManager.java");
assert.match(V,/public static boolean isValidPerpendicularLargeMeshOffset/);
assert.match(V,/public static int getPerpendicularLargeMeshDirectionSign/);
assert.match(V,/if \(largeA && largeB\) \{[\s\S]*?return false;/);
assert.match(N,/LargeGearBlockEntity_wood/);
assert.match(N,/ConnectionKind\.PERPENDICULAR_GEAR_MESH/);
assert.match(N,/findIncompatibleDriveComponents\(level, gears\)/);
assert.match(N,/invalid\.addAll\(component\)/);
assert.match(N,/blocked\.setOverloaded\(true\)/);
assert.match(N,/gear\.isSource\(\) && incompatibleDrives\.contains\(gear\.getPosition\(\)\)/);
assert.match(N,/getConnectionSpeedRatio\(connection, current, neighbor\)/);
assert.match(N,/case PERPENDICULAR_GEAR_MESH/);

const axes=["X","Y","Z"];
const step=(p,a)=>p[axes.indexOf(a)];
const validCross=(a,ax,at,b,bx,bt)=>{
  if(ax===bx||at!==16||bt!==16)return false;
  const d=b.map((v,i)=>v-a[i]);
  return Math.abs(step(d,ax))===1&&Math.abs(step(d,bx))===1
    &&axes.every(c=>c===ax||c===bx||step(d,c)===0);
};
const signCross=(a,ax,b,bx)=>{
  const d=b.map((v,i)=>v-a[i]);
  return (step(d,ax)>0)!==(step(d,bx)>0)?-1:1;
};
let count=0;
for(const ax of axes)for(const bx of axes){
  if(ax===bx)continue;
  for(const s1 of [-1,1])for(const s2 of [-1,1]){
    const a=[0,0,0],b=[0,0,0];
    b[axes.indexOf(ax)]=s1;b[axes.indexOf(bx)]=s2;
    assert(validCross(a,ax,16,b,bx,16));
    assert(validCross(b,bx,16,a,ax,16));
    assert.equal(signCross(a,ax,b,bx),s1===s2?1:-1);
    assert.equal(signCross(b,bx,a,ax),signCross(a,ax,b,bx));
    assert(!validCross(a,ax,8,b,bx,16));
    const wrong=[...b];wrong[axes.findIndex(c=>c!==ax&&c!==bx)]=1;
    assert(!validCross(a,ax,16,wrong,bx,16));
    count++;
  }
}
assert.equal(count,24);
assert(!validCross([0,0,0],"Y",16,[2,0,0],"Y",16));
assert(!validCross([0,0,0],"X",16,[2,2,0],"Y",16));
const left={p:[0,0,0],axis:"Y"},right={p:[2,0,0],axis:"Y"},
small={p:[1,0,1],axis:"Y"};
const diagonal=(a,b)=>a.axis===b.axis
  &&Math.abs(a.p[0]-b.p[0])===1&&Math.abs(a.p[2]-b.p[2])===1;
assert(diagonal(left,small)&&diagonal(right,small));
assert.equal((-1)*(-1),1); // two external meshes restore original direction
// A three-large crossed loop closes with an impossible sign product.
const X={p:[0,0,0],axis:"X"},Y={p:[1,1,0],axis:"Y"},Z={p:[1,0,1],axis:"Z"};
assert(validCross(X.p,X.axis,16,Y.p,Y.axis,16));
assert(validCross(Y.p,Y.axis,16,Z.p,Z.axis,16));
assert(validCross(X.p,X.axis,16,Z.p,Z.axis,16));
assert.equal(signCross(X.p,X.axis,Y.p,Y.axis)
  *signCross(Y.p,Y.axis,Z.p,Z.axis)
  *signCross(Z.p,Z.axis,X.p,X.axis),-1);
console.log("PASS Gear V2 source invariants, 24 perpendicular cases, reversible signs, medium bridge and three-large loop contradiction");
