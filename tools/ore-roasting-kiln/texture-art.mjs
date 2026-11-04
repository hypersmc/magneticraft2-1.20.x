/**
 * Original 16x16 Ore Roasting Kiln pixels. Seeded per-pixel grain.
 * Own machine texture family; not copied from the Refractory Kiln/Retort.
 * These are opaque baked-material tiles, not animated effects.
 */
export const roasterTiles=['brick','foundation','refractory','roof','iron',
                           'bronze','soot','ember','grate','input','output'];
function hash(x,y,s){let n=(x*83+y*199+s*347+23)|0;n=Math.imul(n^(n>>>12),0x45d9f3b);return (n^(n>>>16))>>>0;}
function rgb(hex){const h=hex.slice(1);return [parseInt(h.slice(0,2),16),parseInt(h.slice(2,4),16),parseInt(h.slice(4,6),16),255];}
function pixel(tile,x,y){
 const n=hash(x,y,roasterTiles.indexOf(tile)),g=n%19;
 if(tile==='brick'){
  const row=Math.floor(y/5),joint=(x+(row%2?6:0))%11===0;
  if(y%5===0||joint)return rgb(g<4?'#59413a':'#704d3d');
  if(y%5===1)return rgb(g<4?'#d99872':'#c47e59');
  if(y%5===4)return rgb(g<4?'#765043':'#955d47');
  return rgb(g<4?'#c98665':g>16?'#a9644e':'#b97655');
 }
 if(tile==='foundation'){
  if(y%7===0||(x+Math.floor(y/7)*5)%12===0)return rgb('#424b50');
  if(y%7===1)return rgb('#90938b');
  return rgb(g<3?'#9a978e':g>16?'#5f6669':'#777c7a');
 }
 if(tile==='refractory'){
  if(y%6===0||(x+Math.floor(y/6)*6)%11===0)return rgb('#877b67');
  return rgb(g<4?'#e0ceaa':g>16?'#aa9877':'#c7b493');
 }
 if(tile==='roof'){
  if(y%5===0)return rgb('#2c3338');
  if((x+Math.floor(y/5)*6)%10===0)return rgb('#495058');
  return rgb(g<3?'#99958c':g>15?'#4d555b':'#6d7276');
 }
 if(tile==='iron'){
  if(x===0||y===0||x===15||y===15)return rgb('#283039');
  if(x===2||y===2)return rgb('#7b8583');
  return rgb(g<4?'#828c8a':g>16?'#363e42':'#515b61');
 }
 if(tile==='bronze'){
  if(x===0||y===0||x===15||y===15)return rgb('#4d4035');
  if(x===2||y===2)return rgb('#d0a86f');
  return rgb(g<4?'#c09a60':'#967146');
 }
 if(tile==='soot')return rgb(g<3?'#3e3430':g>16?'#0f1418':'#212227');
 if(tile==='ember'){
  // Slag bed remains dark, with warm fissures. No emission/light claimed.
  if(y<3||x===0||x===15)return rgb('#242023');
  const crack=(y===8&&x>2&&x<13)||(x%5===0&&y>5&&y<12);
  if(crack)return rgb(g<7?'#f4b552':'#b9502f');
  if(g<3)return rgb('#71352b');
  return rgb(g>15?'#674334':'#262326');
 }
 if(tile==='grate'){
  if(y===0||y===15)return rgb('#9ba09a');
  if(x%4===0)return rgb('#81898b');
  if(x%4===1)return rgb('#313a3d');
  return rgb(g<5?'#4b4c4b':'#292b2c');
 }
 if(tile==='input'||tile==='output'){
  if(x<2||x>13||y<2||y>13)return rgb('#343c43');
  const dx=x-8,dy=y-8,side=tile==='input'?-1:1;
  if(Math.abs(dy)<=1&&Math.abs(dx)<=4)return rgb('#d5a15e');
  if(side*dx>=1&&side*dx<=4&&Math.abs(dy)===4-side*dx)
   return rgb('#d5a15e');
  return rgb(g<4?'#888a86':'#596164');
 }
 throw Error('Unknown roaster tile '+tile);
}
function encodePng(rgba){
 const raw=[];
 for(let y=0;y<16;y++){
  raw.push(0);
  for(let x=0;x<16;x++){const j=(y*16+x)*4;raw.push(rgba[j],rgba[j+1],rgba[j+2],rgba[j+3]);}
 }
 const be32=n=>[(n>>>24)&255,(n>>>16)&255,(n>>>8)&255,n&255];
 const ascii=s=>[...s].map(c=>c.charCodeAt(0));
 let a=1,b=0;
 for(const byte of raw){a=(a+byte)%65521;b=(b+a)%65521;}
 const len=raw.length;
 const compressed=[0x78,0x01,0x01,len&255,(len>>>8)&255,(~len)&255,((~len)>>>8)&255,...raw,...be32(((b<<16)|a)>>>0)];
 function crc32(bytes){let c=0xffffffff;for(const byte of bytes){c^=byte;for(let i=0;i<8;i++)c=(c>>>1)^((c&1)?0xedb88320:0);}return (c^0xffffffff)>>>0;}
 const chunk=(name,bytes)=>[...be32(bytes.length),...ascii(name),...bytes,...be32(crc32([...ascii(name),...bytes]))];
 return Uint8Array.from([137,80,78,71,13,10,26,10,
  ...chunk('IHDR',[...be32(16),...be32(16),8,6,0,0,0]),
  ...chunk('IDAT',compressed),...chunk('IEND',[])]);
}

export function makeRoasterPngs(){
 const result={};
 for(const name of roasterTiles){
  const bytes=new Uint8Array(16*16*4);
  for(let y=0;y<16;y++)for(let x=0;x<16;x++)
    bytes.set(pixel(name,x,y),(y*16+x)*4);
  result[name]=encodePng(bytes);
 }
 return result;
}
