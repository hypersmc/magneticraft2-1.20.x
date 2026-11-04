/** Rebuild and verify Magneticraft2 original pixel texture kit. Node 18+, zero deps. */
import {readFileSync,writeFileSync,mkdirSync,existsSync} from 'node:fs';
import {join,dirname} from 'node:path';
import {fileURLToPath} from 'node:url';
import {deflateSync} from 'node:zlib';
const base=dirname(fileURLToPath(import.meta.url));
const src=JSON.parse(readFileSync(join(base,'texture-kit-v1.json'),'utf8'));
const root=process.argv.find((a,i)=>i>=2&&a!=='--check')||'src/main/resources/assets/magneticraft2/textures/block';
const check=process.argv.includes('--check');
const u32=n=>Buffer.from([(n>>>24)&255,(n>>>16)&255,(n>>>8)&255,n&255]);
function crc32(bytes){let c=0xffffffff;for(const b of bytes){c^=b;for(let i=0;i<8;i++)c=(c>>>1)^((c&1)?0xedb88320:0);}return (c^0xffffffff)>>>0;}
function chunk(type,data){const name=Buffer.from(type,'ascii');return Buffer.concat([u32(data.length),name,data,u32(crc32(Buffer.concat([name,data])))]);}
function png(pixels,n){
  const rows=Buffer.alloc(n*(n*4+1));
  for(let y=0;y<n;y++){rows[y*(n*4+1)]=0;pixels.copy(rows,y*(n*4+1)+1,y*n*4,(y+1)*n*4);}
  const header=Buffer.concat([u32(n),u32(n),Buffer.from([8,6,0,0,0])]);
  return Buffer.concat([Buffer.from([137,80,78,71,13,10,26,10]),chunk('IHDR',header),chunk('IDAT',deflateSync(rows,{level:9})),chunk('IEND',Buffer.alloc(0))]);
}
if(src.schema!==1||src.resolution!==16)throw Error('Unexpected texture kit version/size');
let done=0;
for(const [name,t] of Object.entries(src.textures)){
  if(!/^[a-z_]+$/.test(name)||!/^(material|component)\/[a-z_]+\.png$/.test(t.path)||!t.path.endsWith('/'+name+'.png'))throw Error('Unsafe texture name/path: '+name);
  if(t.pixels.length!==16||t.pixels.some(row=>row.length!==16))throw Error('Expected 16 rows of 16 symbols: '+name);
  const rgba=Buffer.alloc(16*16*4);
  for(let y=0;y<16;y++)for(let x=0;x<16;x++){
    const sym=t.pixels[y][x],color=sym==='.'?'#00000000':t.palette[sym];
    if(!/^#[0-9a-fA-F]{8}$/.test(color||''))throw Error('Bad palette symbol '+sym+' in '+name);
    for(let i=0;i<4;i++)rgba[(y*16+x)*4+i]=parseInt(color.slice(1+i*2,3+i*2),16);
  }
  const file=join(root,t.path),expected=png(rgba,16);
  if(check){if(!existsSync(file)||!readFileSync(file).equals(expected))throw Error('Missing or changed: '+file);}
  else{mkdirSync(dirname(file),{recursive:true});writeFileSync(file,expected);}
  done++;
}
console.log((check?'Verified':'Built')+' '+done+' original 16x16 textures');
