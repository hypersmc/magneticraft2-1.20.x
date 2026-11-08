#!/usr/bin/env python3
"""Authoring helper for the shear's native-density baked JSON components.
No runtime renderer/model loader is introduced. Run from the repository root.
"""
import json, math
from pathlib import Path
ROOT = Path(__file__).resolve().parents[2]
MODELS = ROOT / 'src/main/resources/assets/magneticraft2/models'
TEX = {'iron':'magneticraft2:block/material/dark_work_iron',
       'bronze':'magneticraft2:block/material/bronze_machined',
       'stone':'minecraft:block/stone_bricks',
       'belt':'magneticraft2:block/bronze_stamp_mill_drive_belt',
       'particle':'magneticraft2:block/material/dark_work_iron'}
FACES = ('north','south','east','west','up','down')
def cube(a,b,material='iron',rotation=None):
    w,h,d = [round(b[i]-a[i],6) for i in range(3)]
    assert min(w,h,d)>0
    e={'from':a,'to':b,'faces':{}}
    for f in FACES:
        u,v=(w,d) if f in ('up','down') else ((d,h) if f in ('east','west') else (w,h))
        assert max(u,v)<=16, (a,b,f,u,v)
        e['faces'][f]={'texture':'#'+material,'uv':[0,0,u,v]}
    if rotation: e['rotation']=rotation
    return e
def normalize(value):
    if isinstance(value,float) and value.is_integer(): return int(value)
    if isinstance(value,list): return [normalize(v) for v in value]
    if isinstance(value,dict): return {k:normalize(v) for k,v in value.items()}
    return value
def save(name,elements):
    p=MODELS/(name+'.json');p.parent.mkdir(parents=True,exist_ok=True)
    p.write_text(json.dumps(normalize({'parent':'minecraft:block/block','ambientocclusion':False,
                            'textures':TEX,'elements':elements}),indent=2)+'\n')
# Local origin for each moving model is (8,8,8), except the rod and blade at (8,0,8).
# Shaft is translated from (8,8,0); two fixed lengths avoid UV-stretching a unit cube.
save('multiblock/iron_stock_shear_shaft',[cube([7,7,0],[9,9,6.5])])
save('multiblock/iron_stock_shear_coupling',[cube([5.25,5.25,0],[10.75,10.75,.5]),cube([7,7,.5],[9,9,1])])
save('multiblock/iron_stock_shear_crank',[
    cube([6.5,6.5,6.5],[9.5,9.5,8]),
    cube([7,4,7],[9,6.5,8]),
    cube([7.5,4.5,8],[8.5,5.5,10],'bronze')])
save('multiblock/iron_stock_shear_link',[
    cube([7.25,1,7.5],[8.75,9,8.5]),
    cube([6.5,-1,7.5],[9.5,1,8.5],'bronze'),
    cube([6.5,9,7.5],[9.5,11,8.5],'bronze')])
save('multiblock/iron_stock_shear_blade',[
    cube([1,-1,5],[15,1,7]),
    # 1/8-pixel manufacturing clearance from the fixed edge at global z=-10.
    cube([2,-3,5.625],[14,-1,6.625]),
    cube([7.5,-.5,7],[8.5,.5,9],'bronze')])
# An eight-segment sheave inside a 3.35 pixel corner envelope. An eight-sided
# belt wrap has a 3.51-pixel centre-line apothem and 0.18-pixel thickness.
# This leaves clearance throughout rotation instead of intersecting the belt.
pulley=[]
scale=3.35/(3.5**2+1.5**2)**.5
for angle in (0,-45,45):
    for a,b in [([4.5,6.5,7.5],[5,9.5,8.5]),([11,6.5,7.5],[11.5,9.5,8.5])]:
        a=[round(8+(a[0]-8)*scale,6),round(8+(a[1]-8)*scale,6),a[2]]
        b=[round(8+(b[0]-8)*scale,6),round(8+(b[1]-8)*scale,6),b[2]]
        pulley.append(cube(a,b,rotation={'origin':[8,8,8],'axis':'z','angle':angle,'rescale':False} if angle else None))
for a,b in [([6.5,4.5,7.5],[9.5,5,8.5]),([6.5,11,7.5],[9.5,11.5,8.5])]:
    a=[round(8+(a[0]-8)*scale,6),round(8+(a[1]-8)*scale,6),a[2]]
    b=[round(8+(b[0]-8)*scale,6),round(8+(b[1]-8)*scale,6),b[2]]
    pulley.append(cube(a,b))
# Hub and spokes join the rim; no stretched resource-pack geometry.
inner=3*scale
pulley += [cube([6.5,6.5,7],[9.5,9.5,9],'bronze'),
           cube([8-inner,7.5,7.5],[6.5,8.5,8.5]),cube([9.5,7.5,7.5],[8+inner,8.5,8.5]),
           cube([7.5,8-inner,7.5],[8.5,6.5,8.5]),cube([7.5,9.5,7.5],[8.5,8+inner,8.5])]
save('multiblock/iron_stock_shear_pulley',pulley)
# Open 1:1 belt with equal sheaves. Native UVs on each straight and each wrap.
R=3.8; thickness=.18
belt=[cube([8-R-thickness/2,8,7.5],[8-R+thickness/2,24,8.5],'belt'),
      cube([8+R-thickness/2,8,7.5],[8+R+thickness/2,24,8.5],'belt')]
for cy,sign in [(8,-1),(24,1)]:
    vertices=[(8+R*math.cos(i*math.pi/4),cy+sign*R*math.sin(i*math.pi/4)) for i in range(5)]
    for (ax,ay),(bx,by) in zip(vertices,vertices[1:]):
        mx,my=(ax+bx)/2,(ay+by)/2
        length=math.hypot(bx-ax,by-ay)
        deg=math.degrees(math.atan2(by-ay,bx-ax))
        # Pick an X- or Y-long box so the JSON element rotation is +/-22.5.
        while deg>90: deg-=180
        while deg<-90: deg+=180
        if abs(deg)>45:
            deg=deg-90 if deg>0 else deg+90
            a=[mx-thickness/2,my-length/2,7.5]; b=[mx+thickness/2,my+length/2,8.5]
        else:
            a=[mx-length/2,my-thickness/2,7.5]; b=[mx+length/2,my+thickness/2,8.5]
        a=[round(v,6) for v in a];b=[round(v,6) for v in b]
        belt.append(cube(a,b,'belt',{'origin':[round(mx,6),round(my,6),8],'axis':'z','angle':round(deg,1),'rescale':False}))
save('multiblock/iron_stock_shear_belt',belt)
# The real workpiece has a flat support and an open blade slot. These are the
# only changed lower canonical SOUTH cells; their rotations remain V2 parts.
low=[cube([0,0,0],[16,4,16],'stone'),
     # Rear drive pocket: the sheave clears the base instead of cutting into it.
     cube([0,4,0],[4,6,2]),cube([12,4,0],[16,6,2]),cube([0,4,2],[16,6,16]),
     cube([2,6,2],[14,9,5]),cube([2,6,5],[14,8,6]),
     cube([2,8,5],[14,9,6]),cube([2,6,7.5],[14,8,14]),
     cube([2,8,7.5],[3,9,14]),cube([13,8,7.5],[14,9,14]),
     cube([3,8,13],[13,9,14]),
     cube([5.5,9,2],[7,11,5]),cube([9,9,2],[10.5,11,5]),
     cube([0,6,1],[1,16,9]),cube([15,6,1],[16,16,9])]
# Upper guide continuation, plus an open bearing bridge attached to the
# base-supported guide columns. A three-pixel aperture clears the rotating
# two-pixel square shaft at every angle (corner radius sqrt(2)).
up=[cube([0,0,1],[1,10,9]),cube([15,0,1],[16,10,9])]
for z in [2]:
    up += [cube([1,7,z],[5.5,9,z+3]),cube([10.5,7,z],[15,9,z+3]),
           cube([5.5,5.5,z],[6.5,10.5,z+3],'bronze'),cube([9.5,5.5,z],[10.5,10.5,z+3],'bronze'),
           cube([6.5,5.5,z],[9.5,6.5,z+3],'bronze'),cube([6.5,9.5,z],[9.5,10.5,z+3],'bronze')]
def rotpoint(x,z,d):
    return (x,z) if d=='south' else ((-x,-z) if d=='north' else ((z,-x) if d=='east' else (-z,x)))
def rotate(e,d):
    x,y,z=e['from']; a,b,c=e['to']
    if d=='south': lo,hi=[x,y,z],[a,b,c]
    elif d=='north':lo,hi=[16-a,y,16-c],[16-x,b,16-z]
    elif d=='east':lo,hi=[z,y,16-a],[c,b,16-x]
    else:lo,hi=[16-c,y,x],[16-z,b,a]
    return cube(lo,hi,next(iter(e['faces'].values()))['texture'][1:])
for y,els in [(0,low),(1,up)]:
    for d in ['south','west','north','east']:
        x,z=rotpoint(0,-1,d)
        save(f'multiblock/iron_stock_shear_{d}/part_{x}_{y}_{z}',[rotate(e,d) for e in els])
save('item/iron_rivet_blank',[cube([7,6.25,7],[9,9.75,9])])
print('Wrote 16 baked geometry/item JSON files; no textures or construction blueprints changed.')
