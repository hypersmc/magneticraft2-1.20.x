#!/usr/bin/env node
/**
 * Magneticraft2 Settling Tank: nine original hand-drawn 16x16 textures.
 * Each palette character is one painted pixel. V5 uses material roles,\n * including distinct foundation, waterproof floor and dark bronze joints.\n * No random noise, filters,
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
  "foundation": {
    "palette": {
      "0": "#454C48",
      "1": "#313B37",
      "2": "#59635D",
      "3": "#3D4540",
      "4": "#4C554E",
      "5": "#707A70",
      "6": "#363F3B"
    },
    "pixels": [
      "1111111111111111",
      "2222222214222222",
      "0444400014000000",
      "3333333360004000",
      "3333333360000000",
      "3333333360000000",
      "4444444413333333",
      "1111111111111111",
      "2222222222222222",
      "0001400000000000",
      "0001433333333330",
      "0001034433333330",
      "0001033333333330",
      "0001033333333330",
      "0001444444444444",
      "1111111111111111"
    ]
  },
  "refractory": {
    "palette": {
      "0": "#68726C",
      "1": "#4A5650",
      "2": "#829087",
      "3": "#5C6961",
      "4": "#727E75",
      "5": "#929D8F",
      "6": "#59655E"
    },
    "pixels": [
      "1111111111111111",
      "2222222222142222",
      "0000000000140000",
      "0044000000133333",
      "0000000000133333",
      "0000000000133333",
      "6666666666166666",
      "1111111111111111",
      "2222222222222222",
      "0000140000000000",
      "0000140000004000",
      "3333100000000000",
      "3333100000000000",
      "3333100000000000",
      "3333144444444444",
      "1111111111111111"
    ]
  },
  "lining": {
    "palette": {
      "0": "#9AABA3",
      "1": "#718981",
      "2": "#BAC8BD",
      "3": "#A7B9AE",
      "4": "#8F9F97",
      "5": "#CBD6CA",
      "6": "#81948C"
    },
    "pixels": [
      "1111111111111111",
      "2222222222222222",
      "1200000000000006",
      "1200333300000046",
      "1200000000000046",
      "1200000000000046",
      "1233333333333346",
      "1233333333333346",
      "6666666666666666",
      "1233333333333336",
      "1200000000000006",
      "1200000000000006",
      "1200000004444006",
      "1200000000000006",
      "1444444444444446",
      "1111111111111111"
    ]
  },
  "basin_floor": {
    "palette": {
      "0": "#7E9189",
      "1": "#536C64",
      "2": "#9AADA2",
      "3": "#71867C",
      "4": "#899D91",
      "5": "#667A70",
      "6": "#A4B4A9"
    },
    "pixels": [
      "1111111111111111",
      "2222222222254222",
      "0000000000054000",
      "0044000000053333",
      "0000000000053333",
      "0000000000053333",
      "3333333333353333",
      "1111111111111111",
      "2222222222222222",
      "0000540000000000",
      "3333500000440000",
      "3333500000000000",
      "3333500000000000",
      "3333500000000000",
      "3333544444444444",
      "1111111111111111"
    ]
  },
  "bronze": {
    "palette": {
      "0": "#B7825A",
      "1": "#7C503B",
      "2": "#D2A177",
      "3": "#9A6849",
      "4": "#C18D64",
      "5": "#E2B58A",
      "6": "#A87450",
      "7": "#87573E"
    },
    "pixels": [
      "2222222222222222",
      "4444444444444444",
      "2400000000000003",
      "2404444444444063",
      "2404444444444063",
      "2405000000000063",
      "2400063333600063",
      "2400066666600063",
      "2400000000000063",
      "2400444400005063",
      "2400000000000063",
      "2406666666666063",
      "2406666666666063",
      "2400000000000003",
      "7777777777777777",
      "1111111111111111"
    ]
  },
  "bronze_joint": {
    "palette": {
      "0": "#885941",
      "1": "#633D30",
      "2": "#AA7654",
      "3": "#764936",
      "4": "#956548",
      "5": "#CCA079",
      "6": "#543B32",
      "7": "#B6855E"
    },
    "pixels": [
      "1111111111111111",
      "2222222222222222",
      "1222222222222221",
      "1000000330000001",
      "1005560330556001",
      "1005560330556001",
      "1006670330667001",
      "1444444444444441",
      "1444444444444441",
      "1000000330000001",
      "1005560330556001",
      "1005560330556001",
      "1006670330667001",
      "1000000000000001",
      "3333333333333333",
      "1111111111111111"
    ]
  },
  "riveted_iron": {
    "palette": {
      "0": "#61747B",
      "1": "#3F5057",
      "2": "#90A1A4",
      "3": "#51636A",
      "4": "#768991",
      "5": "#B5BDB6",
      "6": "#37474F",
      "7": "#A1ACA9"
    },
    "pixels": [
      "2222222222222222",
      "4444444444444444",
      "1000000000000001",
      "1003730000373001",
      "1003530000353001",
      "1003330000333001",
      "1000000000000001",
      "1000000000000001",
      "1666666666666661",
      "1444444444444441",
      "1000000000000001",
      "1003730000373001",
      "1003530000353001",
      "1003330000333001",
      "3333333333333333",
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
