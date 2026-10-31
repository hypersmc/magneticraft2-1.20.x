/**
 * Original Refractory Kiln pixel art. All tiles are 16x16 RGBA, generated
 * deterministically. No Retort, Settling Tank or vanilla texture pixels copied.
 * Run build-textures.mjs --write to update the checked-in PNG resources.
 */
export const kilnTileNames=['brick','plinth','roof','iron','bronze','soot','grate','door','input','output'];
function rgb(hex){
 const h=hex.replace('#','');
 return [parseInt(h.slice(0,2),16),parseInt(h.slice(2,4),16),parseInt(h.slice(4,6),16),255];
}
function hash(x,y,s=0){let n=(x*83+y*199+s*347+23)|0;n=Math.imul(n^(n>>>12),0x45d9f3b);return (n^(n>>>16))>>>0;}
function color(tile,x,y){
 const h=hash(x,y,kilnTileNames.indexOf(tile));
 const grain=h%17;
 if(tile==='brick'){
  const course=Math.floor(y/5), seam=(x+(course%2?5:0))%9===0;
  if(y%5===0||seam)return rgb(grain<3?'#453933':'#55453b');
  if(y%5===1)return rgb(grain<3?'#bf8f72':'#a7795f');
  if(y%5===4)return rgb(grain<3?'#6a4c3e':'#855d4b');
  return rgb(grain<3?'#c09072':grain>14?'#805847':'#9c6d55');
 }
 if(tile==='plinth'){
  const course=Math.floor(y/8),seam=(x+(course%2?6:0))%11===0;
  if(y%8===0||seam)return rgb('#30383a');
  if(y%8===1)return rgb('#687277');
  if(y%8===7)return rgb('#3d494e');
  return rgb(grain<3?'#737a7b':grain>14?'#394448':'#535f63');
 }
 if(tile==='roof'){
  if(y%6===0)return rgb('#24292d');
  if(y%6===1)return rgb('#72706a');
  if((x+(Math.floor(y/6)%2?6:0))%10===0)return rgb('#37373a');
  return rgb(grain<3?'#696661':'#555553');
 }
 if(tile==='iron'){
  if(x===0||y===0||x===15||y===15)return rgb('#252d30');
  if((x===2||x===13)&&(y===2||y===13))return rgb('#a0aaa4');
  if(y===2||y===13)return rgb('#5b6668');
  return rgb(grain<2?'#647073':grain>14?'#303b3f':'#424d51');
 }
 if(tile==='bronze'){
  if(x===0||x===15)return rgb('#5f4a32');
  if(y===0||y===15)return rgb('#aa8858');
  if((x===3||x===12)&&(y===3||y===12))return rgb('#e0bb79');
  return rgb(grain<3?'#c29a62':grain>13?'#786040':'#967346');
 }
 if(tile==='soot')return rgb(grain<3?'#41413d':grain>14?'#14191a':'#262a2a');
 if(tile==='grate'){
  if(y===0||y===15)return rgb('#8c9290');
  if(x===0||x===15)return rgb('#505b5e');
  if(x%4===1)return rgb('#8c9795');
  if(x%4===2)return rgb('#454f52');
  return rgb(grain===0?'#3a3732':'#171d20');
 }
 if(tile==='door'){
  const bolt=(x===2||x===13)&&(y===2||y===13);
  if(bolt)return rgb('#d1ad71');
  if(x===0||x===15||y===0||y===15)return rgb('#232b2e');
  if(x===1||x===14||y===1||y===14)return rgb('#727b78');
  if(y===7||y===8)return rgb('#5d696c');
  if(x>=10&&x<=13&&y>=5&&y<=10)return rgb(y===7||y===8?'#c79c60':'#8e6c42');
  if(x===3||x===12)return rgb('#3f4849');
  return rgb(grain<2?'#5b6060':'#343e42');
 }
 if(tile==='input'||tile==='output'){
  if(x===0||x===15||y===0||y===15)return rgb('#242d30');
  if(x===1||x===14||y===1||y===14)return rgb('#626d70');
  const cx=8,cy=8;
  // A distinct bronze arrow is embossed onto each port's own iron panel.
  const right=tile==='output';
  const dx=x-cx,dy=y-cy;
  if((Math.abs(dy)<=1&&Math.abs(dx)<=4)||(right?dx>=1&&dx<=4:dx<=-1&&dx>=-4)&&Math.abs(dy)===(right?4-dx:4+dx))
   return rgb('#c7a16a');
  return rgb(grain<3?'#485459':'#303b3f');
 }
 throw Error('Unknown kiln tile '+tile);
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
export function makeKilnPngs(){
 const result={};
 for(const tile of kilnTileNames){
  const rgba=new Uint8Array(16*16*4);
  for(let y=0;y<16;y++)for(let x=0;x<16;x++)rgba.set(color(tile,x,y),(y*16+x)*4);
  result[tile]=encodePng(rgba);
 }
 return result;
}
