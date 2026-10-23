#!/usr/bin/env node
/**
 * Magneticraft2 Settling Tank: six original hand-drawn 16x16 textures.
 * Each palette character is one painted pixel. No random noise, filters,
 * borrowed textures or fake baked directional shading.
 *
 * From repository root:
 *   node tools/water-settling-tank/build-textures.mjs
 *   node tools/water-settling-tank/build-textures.mjs --check
 * Uses only Node core modules.
 */
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { deflateSync, inflateSync } from 'node:zlib';

const tiles = {
  "refractory": {
    "palette": {
      "0": "#656A66",
      "1": "#505852",
      "2": "#5C625E",
      "3": "#747A72",
      "4": "#6D716A"
    },
    "pixels": [
      "3333333331000000",
      "0000000021000000",
      "0000000021000000",
      "0044400021000000",
      "0000000021000000",
      "0000000021000000",
      "2222222221000000",
      "1111111111111111",
      "0001333333333333",
      "0021000000000000",
      "0021000000004440",
      "0021000000000000",
      "0021000000000000",
      "0021000000000000",
      "0001222222222222",
      "1111111111111111"
    ]
  },
  "lining": {
    "palette": {
      "0": "#8D9C97",
      "1": "#74847E",
      "2": "#96A59E",
      "3": "#9DABA5",
      "4": "#86948F",
      "5": "#96A7A0",
      "6": "#A3B0A9"
    },
    "pixels": [
      "0333333333333331",
      "2000000000000001",
      "2000000000000001",
      "2006666000000001",
      "2000000000000001",
      "2000000000000501",
      "2000000000000001",
      "2000000000000001",
      "2000000000000001",
      "2000000000000001",
      "2000000005555001",
      "2000000000000001",
      "2000000000000001",
      "2000000000000001",
      "2044444444444001",
      "1111111111111111"
    ]
  },
  "bronze": {
    "palette": {
      "0": "#A47753",
      "1": "#78553E",
      "2": "#BD8D62",
      "3": "#8A6144",
      "4": "#B3845C",
      "5": "#916B4C",
      "6": "#B0845B",
      "7": "#67503D",
      "8": "#876248",
      "9": "#BB8E62",
      "A": "#DFB17D"
    },
    "pixels": [
      "2222222222222222",
      "4000000000000003",
      "4006666666660003",
      "4000000000000003",
      "400A900000000003",
      "4008700000000003",
      "4000000000000003",
      "4000000000000003",
      "4000000000000003",
      "4000000000000003",
      "4000000000000003",
      "400000000000A903",
      "4000000000008703",
      "4000555555555003",
      "4000000000000003",
      "1111111111111111"
    ]
  },
  "riveted_iron": {
    "palette": {
      "0": "#6D7779",
      "1": "#465258",
      "2": "#8D9696",
      "3": "#536065",
      "4": "#838F91",
      "5": "#646F73",
      "6": "#778082",
      "7": "#465055",
      "8": "#626D70",
      "9": "#919B9A",
      "A": "#B8B5A6"
    },
    "pixels": [
      "2222222222222222",
      "4000000000000003",
      "4000000000000003",
      "400A900000000003",
      "4008700000000003",
      "4000000000000003",
      "4000000000000003",
      "4055555555555503",
      "4006666666666003",
      "4000000000000003",
      "4000000000000003",
      "4000000000000003",
      "400000000000A903",
      "4000000000008703",
      "4000000000000003",
      "1111111111111111"
    ]
  },
  "grate": {
    "palette": {
      "0": "#293337",
      "1": "#8F9A9B",
      "2": "#697678",
      "3": "#546366",
      "4": "#283238",
      "5": "#313D41"
    },
    "pixels": [
      "1111111111111111",
      "3333333333333333",
      "1200000120000012",
      "1204440120444012",
      "1204440120444012",
      "1205550120555012",
      "1200000120000012",
      "1200000120000012",
      "1111111111111111",
      "3333333333333333",
      "1200000120000012",
      "1204440120444012",
      "1204440120444012",
      "1205550120555012",
      "1200000120000012",
      "1200000120000012"
    ]
  },
  "sludge": {
    "palette": {
      "0": "#716859",
      "1": "#625A4E",
      "2": "#5D554A",
      "3": "#817660",
      "4": "#8B806A",
      "5": "#857965"
    },
    "pixels": [
      "0001111000000000",
      "0000000000000000",
      "0000000001111110",
      "3333330001111110",
      "3333333330011111",
      "0000033330000000",
      "0055000000000000",
      "0055000000000550",
      "0000000000000550",
      "0222222000000000",
      "0222222222200000",
      "0000000222200000",
      "0000000004444440",
      "0000000000444444",
      "0000000000000000",
      "1111000000000000"
    ]
  }
};

function crc32(bytes) {
  let crc=0xffffffff;
  for(const byte of bytes){
    crc ^= byte;
    for(let bit=0;bit<8;bit++)crc=(crc>>>1)^(0xedb88320&-(crc&1));
  }
  return (crc^0xffffffff)>>>0;
}
function chunk(kind,data) {
  const name=Buffer.from(kind,'ascii'), output=Buffer.alloc(data.length+12);
  output.writeUInt32BE(data.length,0);
  name.copy(output,4);data.copy(output,8);
  output.writeUInt32BE(crc32(Buffer.concat([name,data])),data.length+8);
  return output;
}
function encodeTile(name,art) {
  if(art.pixels.length!==16 || art.pixels.some(row=>row.length!==16))
    throw Error('Invalid pixel grid '+name);
  const scanlines=Buffer.alloc(16*(1+16*3)); // row filter=0, 16 RGB pixels
  for(let y=0;y<16;y++)for(let x=0;x<16;x++){
    const color=art.palette[art.pixels[y][x]];
    if(!color || !/^#[0-9a-fA-F]{6}$/.test(color))
      throw Error('Undefined palette color '+name+' at '+x+','+y);
    const pos=y*49+1+x*3;
    scanlines[pos]=parseInt(color.slice(1,3),16);
    scanlines[pos+1]=parseInt(color.slice(3,5),16);
    scanlines[pos+2]=parseInt(color.slice(5,7),16);
  }
  const ihdr=Buffer.alloc(13);
  ihdr.writeUInt32BE(16,0);ihdr.writeUInt32BE(16,4);
  ihdr[8]=8;ihdr[9]=2; // 8-bit RGB PNG
  return Buffer.concat([
    Buffer.from('89504e470d0a1a0a','hex'),chunk('IHDR',ihdr),
    chunk('IDAT',deflateSync(scanlines,{level:9})),chunk('IEND',Buffer.alloc(0))
  ]);
}
/** Validate exact painted pixels, independent of zlib compression version. */
function decodePixels(bytes) {
  if(!bytes || bytes.length<57 ||
     !bytes.subarray(0,8).equals(Buffer.from('89504e470d0a1a0a','hex')) ||
     bytes.readUInt32BE(16)!==16 || bytes.readUInt32BE(20)!==16 ||
     bytes[24]!==8 || bytes[25]!==2 || bytes[28]!==0) return null;
  const chunks=[];
  for(let offset=8;offset+12<=bytes.length;) {
    const len=bytes.readUInt32BE(offset);
    if(offset+len+12>bytes.length) return null;
    const type=bytes.toString('ascii',offset+4,offset+8);
    if(type==='IDAT') chunks.push(bytes.subarray(offset+8,offset+8+len));
    offset+=len+12;
    if(type==='IEND') break;
  }
  if(!chunks.length) return null;
  try {
    const raw=inflateSync(Buffer.concat(chunks));
    if(raw.length!==16*49 || Array.from({length:16},(_,y)=>raw[y*49])
        .some(filter=>filter!==0)) return null;
    return raw;
  } catch { return null; }
}
const outputDir=process.env.MGC2_TEXTURE_OUT ??
  'src/main/resources/assets/magneticraft2/textures/block/settling_tank';
const check=process.argv.includes('--check');
if(!check)mkdirSync(outputDir,{recursive:true});
let failed=0;
for(const [name,art] of Object.entries(tiles)){
  const output=join(outputDir,name+'.png');
  const expected=encodeTile(name,art);
  if(check){
    let existing=null;
    try{existing=readFileSync(output);}catch{}
    if(!decodePixels(existing)?.equals(decodePixels(expected))){
      console.error('Texture differs from pixel-art source: '+output);
      failed++;
    }
  }else writeFileSync(output,expected);
}
console.log('Water Settling Tank textures: '+Object.keys(tiles).length+' tiles '+
  (check?'verified':'generated'));
if(failed)process.exitCode=1;
