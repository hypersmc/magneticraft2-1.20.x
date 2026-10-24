/**
 * Charcoal Retort V3 visual shell, preserving every real 3x4x3 block position.
 * Port models are temporary neighbors while the V2 face partitioner runs.
 * finalizeRetortV3() moves them back to models selected by the REAL port BEs.
 * No gameplay or per-frame rendering work is done in this generator.
 */
export function applyRetortV3(files) {
 const a="src/main/resources/assets/magneticraft2/",d="src/main/resources/data/magneticraft2/";
 const dirs=["south","west","north","east"];
 const rows=[["SSS","SSS","SCS"],["BYB","BAB","UBO"],["BBB","BAB","BBB"],["PPP","PAP","PPP"]];
 const textures={
  stone:"magneticraft2:block/settling_tank/foundation",metal:"magneticraft2:block/settling_tank/riveted_iron",
  join:"magneticraft2:block/settling_tank/bronze_joint",bronze:"magneticraft2:block/settling_tank/bronze",
  brick:"magneticraft2:block/charcoal_retort/refractory",roof:"magneticraft2:block/charcoal_retort/roof",
  soot:"magneticraft2:block/charcoal_retort/soot",vent:"magneticraft2:block/charcoal_retort/grille",
  off:"magneticraft2:block/charcoal_retort/hatch_off",on:"magneticraft2:block/charcoal_retort/hatch_on",
  wood:"minecraft:block/stripped_spruce_log",charcoal:"minecraft:block/coal_block",
  particle:"magneticraft2:block/charcoal_retort/refractory"
 };
 const shapes=new Map(),portShapes=new Map();
 const cells=[];for(let y=0;y<4;y++)for(let z=0;z<3;z++)for(let x=0;x<3;x++)cells.push({x:x-1,y,z:z-2,k:rows[y][z][x]});
 const cellKey=c=>[c.x,c.y,c.z].join(",");
 function add(map,c,mat,from,to){
  if(from.some((v,i)=>v<0||to[i]>16||to[i]<=v))throw Error("V3 model exceeded block bounds");
  if(!map.has(cellKey(c)))map.set(cellKey(c),[]);
  map.get(cellKey(c)).push({mat,from,to});
 }
 const solid=(c,mat,from,to)=>add(shapes,c,mat,from,to);
 const port=(c,mat,from,to)=>add(portShapes,c,mat,from,to);
 for(const c of cells){
  const {x,y,z,k}=c;
  if(k==="S"){
   solid(c,"stone",[0,0,0],[16,16,16]);
   if(z===0){
    solid(c,"metal",[0,1,14],[16,3,16]);
    solid(c,"metal",[0,12,14],[16,14,16]);
    if(x!==0)solid(c,"join",[2,14,14],[14,16,16]);
   } else if(x!==0&&z===-1){
    const xa=x===-1?0:14;
    solid(c,"metal",[xa,0,0],[xa+2,12,16]);
   }
  } else if(k==="C"){
   // Functional firebox at the controller: inset and visibly lit only while burning.
   solid(c,"stone",[0,0,0],[16,16,11]);
   solid(c,"metal",[2,2,11],[14,14,13]);
   solid(c,"soot",[3,3,13],[13,13,14]);
   solid(c,"off",[4,4,14],[12,12,15]);
   solid(c,"metal",[0,0,13],[3,16,16]);solid(c,"metal",[13,0,13],[16,16,16]);
   solid(c,"bronze",[3,13,13],[13,16,16]);solid(c,"bronze",[3,0,13],[13,3,16]);
   solid(c,"join",[6,1,15],[10,4,16]);
  } else if(k==="B"){
   if(y===2){
    // V3.8: reference-led BRICK CHAMBER ONLY. The copper roof and the
    // real Y=1 hatches/ports are locked. All new detail is contained
    // inside the eight Y=2 brick positions, never in neighboring cells.
    const front=z===0,outer=x!==0,rear=z===-2;
    // ONE BLOCK AT A TIME: the front-left Y=2 brick cell (SOUTH x=-1).
    // Geometry belongs only to this cell; other front pieces and the
    // approved roof remain unchanged until this left panel is accepted.
    const leftFacade=front&&x===-1;
    if(leftFacade){
      // V3.17: preserve copper L-shape and 3D bricks; fix ONLY the two
      // left corner fittings to match the user's arrowed reference.
      // Restore brick-course relief only: the mortar stays deep and the
      // raised brick faces now reach Z=15.45..15.70 below the trim at 16.
      // Everything remains in the same one 16x16x16 front-left cell.
      // The user's copper vent requires a shallow dark recess in this
      // LEFT-facing side, but the front-facing brick core is preserved.
      solid(c,"brick",[0,0,0],[16,13.35,14]);
      solid(c,"brick",[1.6,13.35,0],[16,15.975,14]);
      solid(c,"brick",[0,15.975,0],[16,16,14]);
      solid(c,"brick",[0,13.35,13],[1.6,15.975,14]);

      // Mortar is a thin solid backing against the core at Z=14.2.
      // The physical brick faces rise 1.25..1.50px beyond it,
      // creating a clear side facet and shadow for each real brick.
      // The wall remains solid: no missing full-block pocket.
      solid(c,"mortar",[3,2.5,14],[16,15,14.2]);

      // Three coursed rows. Joints stagger from course to course;
      // tiny gaps leave the light mortar behind visibly exposed.
      // Widths and placements are deliberately asymmetric like real
      // laid masonry rather than a tiled full-cube brick texture.
      const courses=[
        {y0:3.0,y1:6.55,spans:[[3.25,7.9],[8.35,15.75]]},
        {y0:7.05,y1:10.6,spans:[[3.25,5.45],[5.9,10.15],[10.6,15.75]]},
        {y0:11.1,y1:14.55,spans:[[3.25,8.2],[8.65,15.75]]}
      ];
      for(const row of courses){
        for(const [x0,x1] of row.spans){
          // Visible individual brick faces rise almost to the copper trim,
          // while neighboring faces differ by 0.25px to break flatness.
          // The side faces are genuine textured geometry, not decals.
          const relief=x0===3.25?15.70:15.45;
          solid(c,"brick",[x0,row.y0,14.2],[x1,row.y1,relief]);
        }
      }

      // Copper header: the reference has an L-shaped outline, with
      // ONE strong left post and a horizontal copper lintel. It stops
      // at this cell boundary where the existing center vent begins.
      // The header is shallowly raised above the brick faces and
      // is not the rejected dark band beneath the copper roof.
      solid(c,"bronze",[3,12.75,14.5],[16,16,16]);
      // A tiny step at the lower edge gives the header a real underside
      // without hiding the staggered central masonry courses.
      solid(c,"bronze",[3.75,12.25,15],[16,12.75,16]);

      // Preserve concealed edge contacts to the adjacent course while
      // integrating the copper post and dark iron mount into the wall.
      // The top and bottom retain their invisible masonry contacts.
      // Crucially the exposed fronts are not solid iron cuboids: that
      // covered the dark inset and copper shoulder in V3.16.
      // The iron contact must terminate behind the existing front mount;
      // its old rearward extension covered the side-vent openings.
      solid(c,"metal",[0,13,13],[3,16,15.25]);
      solid(c,"metal",[0,15.5,15.25],[3,16,16]);
      solid(c,"metal",[0,0,0],[3,3,15]);

      // Broad structural outer copper post; the bottom shoulder is
      // stepped only on this side, following the concept reference.
      solid(c,"bronze",[0.5,3,14],[3,13,16]);
      // Reference: the LOWER SHOULDER is copper, with a much thinner
      // dark iron anchoring foot BELOW it, not an all-iron 3px box.
      solid(c,"bronze",[0,1.25,14],[4,4.5,16]);
      solid(c,"bronze",[0.5,5,14.5],[3.5,6,16]);
      solid(c,"metal",[0,0,14],[4,1.25,16]);

      // Reference top-left bracket: a deep cast-iron fixing with a
      // DISTINCT dark square inset. A complete front-facing metal cube
      // would obscure the recess, so the edge is made from four rails.
      solid(c,"metal",[0,12,14],[4,16,15.25]);
      solid(c,"metal",[0,12,15.25],[4,12.5,16]);
      solid(c,"metal",[0,15.5,15.25],[4,16,16]);
      solid(c,"metal",[0,12.5,15.25],[0.5,15.5,16]);
      solid(c,"metal",[3.5,12.5,15.25],[4,15.5,16]);
      // Inner riveted ring and recessed dark center (Z=15.65),
      // versus the surrounding bracket frame at Z=16.
      solid(c,"join",[0.5,12.5,15.25],[1,15.5,16]);
      solid(c,"join",[3,12.5,15.25],[3.5,15.5,16]);
      solid(c,"join",[1,12.5,15.25],[3,13,16]);
      solid(c,"join",[1,15,15.25],[3,15.5,16]);
      solid(c,"stone",[1,13,15.25],[3,15,15.65]);

      // No right-hand pillar, cap, iron foot or bottom-wide sill:
      // those formed the unintended U-shape in V3.14. The masonry
      // continues through X=16 to meet the unchanged center block.
    }else if(front&&x===0){
      // V3.21: center Y2 ONLY. Retain V3.20 cross-block copper header,
      // five recessed grille slots and V3.19 stepped copper arch.
      // Restore the LEFT facade's actual raised-brick + mortar depth,
      // not a flat brick texture on a 16px full front wall.
      solid(c,"brick",[0,0,0],[16,16,9]);
      // Solid structural wall immediately behind new shallow masonry.
      solid(c,"brick",[0,0,9],[16,10.5,14]);
      // Side returns meet the iron/copper framing so this is a SOLID
      // wall, not the rejected deep alcove. The middle brick field
      // remains recessed ~0.3..0.55px below the copper/iron trim.
      solid(c,"brick",[0,0,14],[3.15,10.5,15]);
      solid(c,"brick",[12.85,0,14],[16,10.5,15]);
      solid(c,"brick",[0,15,9],[16,16,15]);
      solid(c,"brick",[0,10.5,9],[3.1,15,15]);
      solid(c,"brick",[12.9,10.5,9],[16,15,15]);

      // Real 3D masonry: stone mortar exposed between nine staggered
      // raised brick solids. All geometry stays inside 1 block and
      // uses native-proportional per-face UVs.
      solid(c,"mortar",[3.15,3.9,14],[12.85,10.45,14.2]);
      const centerBrickCourses=[
        {y0:4.05,y1:5.85,spans:[[3.25,6.1],[6.5,9.25],[9.65,12.75]]},
        {y0:6.2,y1:8.1,spans:[[3.25,4.55],[4.95,8.25],[8.65,12.75]]},
        {y0:8.45,y1:10.25,spans:[[3.25,6.9],[7.3,10.05],[10.45,12.75]]}
      ];
      for(const row of centerBrickCourses)for(const [x0,x1] of row.spans){
        const front=x0===3.25?15.7:15.45;
        solid(c,"brick",[x0,row.y0,14.2],[x1,row.y1,front]);
      }

      // Recessed soot visible through five copper-fronted vent slots,
      // not free-floating metal bars. The copper facade ends at Z=16.
      solid(c,"soot",[3.1,10.5,9],[12.9,15,13.75]);
      // V3.20 — copper header shared with the left and right Y2 cells.
      // The existing center grille header covered only X=2.5..13.5,
      // leaving two visible 2.5px gaps at the inter-block seams.
      // Add real end returns flush with the outer face at Z=16.
      // Keep the dark iron end mountings on top, not buried in copper.
      solid(c,"bronze",[0,14.1,14.5],[0.25,16,16]);
      solid(c,"bronze",[15.75,14.1,14.5],[16,16,16]);
      // The central grille header is the uninterrupted copper span
      // between the two iron mountings; five vent slots sit below.
      solid(c,"bronze",[2.75,14.1,14.5],[13.25,16,16]);
      solid(c,"bronze",[2.75,10.5,14.5],[13.25,11.3,16]);
      solid(c,"bronze",[2.75,11.3,14.5],[4,14.1,16]);
      solid(c,"bronze",[12.45,11.3,14.5],[13.25,14.1,16]);
      // Four integrated copper stanchions -> five true dark slots.
      // Slots: 4..5.25; 5.8..7.05; 7.6..8.85;
      //        9.4..10.65; 11.2..12.45.
      for(const xx of [5.25,7.05,8.85,10.65])
        solid(c,"bronze",[xx,11.3,14.5],[xx+0.55,14.1,16]);
      // Iron mounting brackets on each end, raised 1px over brick.
      // Both plates use a solid rear and four iron border rails,
      // exposing a physically recessed dark boss instead of burying
      // it behind a full face metal cube.
      // Widen iron mountings 1.90 -> 2.50 px, while keeping their
      // inset dark center physically behind the raised border.
      // They stop short of all five real vent slots.
      for(const bx of [0.25,13.25]){
        solid(c,"metal",[bx,11.5,14.5],[bx+2.5,16,15.2]);
        solid(c,"metal",[bx,11.5,15.2],[bx+2.5,12.2,16]);
        solid(c,"metal",[bx,15.3,15.2],[bx+2.5,16,16]);
        solid(c,"metal",[bx,12.2,15.2],[bx+0.5,15.3,16]);
        solid(c,"metal",[bx+2,12.2,15.2],[bx+2.5,15.3,16]);
        solid(c,"stone",[bx+0.5,12.2,15.2],[bx+2,15.3,15.65]);
      }

      // Brick front directly beneath the vent: solid masonry, slightly
      // behind the framing so the side supports cast shadows.
      // Core face Z=15 is only 1px back from copper, NOT a hollow alcove.
      // The vertical copper ribs CONNECT DIRECTLY to the underside
      // of the two iron grille mountings at Y=11.5, without the V3.18
      // 0.3px air gap. They meet the bracket bottom rails at a real
      // geometric seam; the copper does not protrude in front of iron.
      // Widen copper uprights 1.1 -> 1.9px and iron feet 1.5 ->
      // 2.25px. They still meet the bottom rails of both iron plates,
      // without touching any of the five grille openings.
      solid(c,"bronze",[1.05,2.3,14.5],[2.95,11.5,16]);
      solid(c,"bronze",[13.05,2.3,14.5],[14.95,11.5,16]);
      solid(c,"metal",[0.95,0,14.5],[3.2,2.4,16]);
      solid(c,"metal",[12.8,0,14.5],[15.05,2.4,16]);

      // Seven symmetric voussoir-like segments form a shallow RISING
      // COPPER ARCH over the Y1 hatch. Both the upper and lower
      // contours rise toward the middle, forming a genuine curved
      // opening rather than a straight rectangular sill or a U-tray.
      // A Minecraft-scale stair-step approximation avoids UV stretch.
      // The end pieces physically meet the adjacent iron feet.
      // V3.30: widen the visual rise while staying entirely within the
      // ONE upper center Y2 cell. Seven symmetrical copper segments.
      const archSegments=[
        [2.8,4.0,1.25,3.0],
        [4.0,5.2,1.85,3.55],
        [5.2,6.4,2.55,4.1],
        [6.4,9.6,3.25,4.55],
        [9.6,10.8,2.55,4.1],
        [10.8,12.0,1.85,3.55],
        [12.0,13.2,1.25,3.0]
      ];
      for(const [x0,x1,bottom,top] of archSegments)
        solid(c,"bronze",[x0,bottom,14.75],[x1,top,16]);

      // V3.29: add the reference's center copper drip-apron and iron
      // underside immediately above the lower center hatch. This does
      // NOT replace the accepted shallow seven-segment copper arch.
      // The narrow 0.52px dark strip is a local apron shadow, not a
      // full-width Retort side-wall iron band.
      solid(c,"metal",[3.2,0.1,15.45],[12.8,0.62,16]);
      solid(c,"bronze",[3.35,0.62,15.55],[12.65,1.02,16]);

      // Taller, wider inset iron pockets. The front face is 0.22px
      // behind the copper trim and the raised arch, not flush with it.
      for(const [xa,xb] of [[4.6,6.4],[9.6,11.4]])
        solid(c,"metal",[xa,1.02,15.35],[xb,2.55,15.78]);

      // Separate copper cheeks border both recesses without covering
      // them, while a central copper pier rises to meet the new arch.
      for(const [xa,xb] of [[3.4,4.35],[4.35,4.6],[6.4,6.7],
                            [9.3,9.6],[11.4,11.65],[11.65,12.6]])
        solid(c,"bronze",[xa,1.02,15.6],[xb,2.55,16]);
      solid(c,"bronze",[6.7,1.02,15.6],[9.3,3.25,16]);

    }else if(front&&x===1){
      // V3.22: match the right-hand concept image. This is a SOLID
      // upper front brick wall with physical staggered masonry courses,
      // one substantial outer RIGHT copper support, and a recessed
      // square iron fixing. Do not rebuild the center-facing inner edge
      // into another U-shaped frame.
      solid(c,"brick",[0,0,0],[14,16,14]);
      // Mortar backing, 0.2px proud of the structural brick core.
      solid(c,"mortar",[0,2.5,14],[13,14.1,14.2]);
      const rightCourses=[
        {y0:3.0,y1:6.55,spans:[[0.25,7.65],[8.1,12.75]]},
        {y0:7.05,y1:10.6,spans:[[0.25,5.4],[5.85,10.1],[10.55,12.75]]},
        {y0:11.1,y1:13.8,spans:[[0.25,7.35],[7.8,12.75]]}
      ];
      for(const row of rightCourses)for(const [xa,xb] of row.spans){
        // Genuine cuboids, not a stretched brick overlay. Front faces
        // alternate Z=15.45 / 15.70, leaving recessed mortar joints.
        const faceDepth=xa===0.25?15.7:15.45;
        solid(c,"brick",[xa,row.y0,14.2],[xb,row.y1,faceDepth]);
      }

      // V3.24: continue V3.23's raised side bricks across the front-right
      // upper block, stopping at its accepted corner copper upright.
      solid(c,"mortar",[14,0,0],[14.2,16,13]);
      const cornerSideCourses=[
        {y0:0.25,y1:2.8,spans:[[0.15,3.9],[4.35,9.5],[9.95,12.85]]},
        {y0:3.25,y1:5.85,spans:[[0.15,5.25],[5.7,10.65],[11.1,12.85]]},
        {y0:6.3,y1:8.9,spans:[[0.15,3.4],[3.85,9.3],[9.75,12.85]]},
        {y0:9.35,y1:11.95,spans:[[0.15,5.85],[6.3,11.3],[11.75,12.85]]},
        {y0:12.4,y1:13.35,spans:[[0.15,4.2],[4.65,9.6],[10.05,12.85]]}
      ];
      for(const [courseIndex,row] of cornerSideCourses.entries())
        for(const [brickIndex,[za,zb]] of row.spans.entries()){
          const relief=(courseIndex+brickIndex)%2===0?15.7:15.45;
          solid(c,"sideBrick",[14.2,row.y0,za],[relief,row.y1,zb]);
        }
      // V3.28: reflect the approved left upper copper header exactly.
      // Left main X3..16 Y12.75..16 mirrors to right X0..13;
      // the shallow Y12.25..12.75 stepped lower lip is mirrored too.
      // The iron mounting is authored afterward and remains on top.
      solid(c,"bronze",[0,12.75,14.5],[13,16,16]);
      solid(c,"bronze",[0,12.25,15],[12.25,12.75,16]);

      // Outer RIGHT copper post. The thick copper shoulder at its base
      // is distinct from the thin dark iron foot BELOW the shoulder.
      solid(c,"bronze",[13,3,13],[16,13,16]);
      solid(c,"bronze",[12,1.25,14],[16,4.5,16]);
      solid(c,"bronze",[12.5,5,14.5],[15.5,6,16]);
      solid(c,"metal",[12,0,14],[16,1.25,16]);

      // Structural contacts continue into the adjacent roof/bottom and
      // side, but stop behind the VISIBLE front trim at Z=15.25.
      solid(c,"metal",[13,13,13],[16,16,15.25]);
      solid(c,"metal",[13,0,13],[16,3,15]);
      // Outer top-right dark iron mounting, with a distinct framed,
      // recessed square center — do not hide it behind a full iron cube.
      solid(c,"metal",[12,12,14],[16,16,15.25]);
      solid(c,"metal",[12,12,15.25],[16,12.5,16]);
      solid(c,"metal",[12,15.5,15.25],[16,16,16]);
      solid(c,"metal",[12,12.5,15.25],[12.5,15.5,16]);
      solid(c,"metal",[15.5,12.5,15.25],[16,15.5,16]);
      solid(c,"join",[12.5,12.5,15.25],[13,15.5,16]);
      solid(c,"join",[15,12.5,15.25],[15.5,15.5,16]);
      solid(c,"join",[13,12.5,15.25],[15,13,16]);
      solid(c,"join",[13,15,15.25],[15,15.5,16]);
      solid(c,"stone",[13,13,15.25],[15,15,15.65]);
    }else if(x===1 && !front){
      // V3.23 RIGHT SIDE WALL (upper Y2 layer, two blocks long).
      // The reference is continuous raised brick masonry, NOT the
      // old broad iron rails at top and bottom. The structural core
      // remains solid and the visible outer surface faces X=16.
      solid(c,"brick",[0,0,0],[14,16,16]);
      solid(c,"mortar",[14,0,0],[14.2,16,16]);
      // Five physical horizontal masonry courses, intentionally
      // staggered differently in the middle and rear side blocks.
      // Their Z endpoints almost meet at both cell boundaries so
      // no hollow space or disconnected masonry appears at seams.
      const sideCourses=z===-1 ? [
        {y0:0.25,y1:2.8,spans:[[0.15,4.9],[5.35,10.75],[11.2,15.85]]},
        {y0:3.25,y1:5.85,spans:[[0.15,2.65],[3.1,8.6],[9.05,14.35],[14.8,15.85]]},
        {y0:6.3,y1:8.9,spans:[[0.15,5.75],[6.2,11.85],[12.3,15.85]]},
        {y0:9.35,y1:11.95,spans:[[0.15,3.8],[4.25,9.75],[10.2,15.85]]},
        {y0:12.4,y1:13.35,spans:[[0.15,6.1],[6.55,12.05],[12.5,15.85]]}
      ] : [
        {y0:0.25,y1:2.8,spans:[[0.15,3.1],[3.55,9.05],[9.5,15.85]]},
        {y0:3.25,y1:5.85,spans:[[0.15,5.1],[5.55,10.8],[11.25,15.85]]},
        {y0:6.3,y1:8.9,spans:[[0.15,3.65],[4.1,9.55],[10,15.85]]},
        {y0:9.35,y1:11.95,spans:[[0.15,5.9],[6.35,12.15],[12.6,15.85]]},
        {y0:12.4,y1:13.35,spans:[[0.15,3.2],[3.65,8.9],[9.35,15.85]]}
      ];
      for(const [courseIndex,row] of sideCourses.entries()){
        for(const [brickIndex,span] of row.spans.entries()){
          const depth=(courseIndex+brickIndex)%2===0?15.7:15.45;
          solid(c,"sideBrick",[14.2,row.y0,span[0]],[depth,row.y1,span[1]]);
        }
      }
      // Retain ONLY the rear corner's original upright copper fixing.
      // It ties this wall into the rear surface without recreating
      // the rejected continuous dark Y=0..3 / Y=13..16 bands.
      if(rear){
        solid(c,"bronze",[13,3,0],[16,14,2]);
        // Rear-corner iron caps remain, but only as a short 0.5px
        // return at the rear edge, never a stripe across the side.
        solid(c,"metal",[13,0,0],[16,3,0.5]);
        solid(c,"metal",[13,14,0],[16,16,0.5]);
      }
    }else if(outer){
      // Other chamber walls remain on the previous accepted path.
      solid(c,"brick",[x===-1?1.5:0,0,0],
                       [x===1?14.5:16,16,16]);
    }else{
      solid(c,"brick",[0,0,0],[16,16,16]);
    }
    if(front&&outer&&!leftFacade&&x!==1){
      const corner=x===-1?0:13;
      const inside=x===-1?13:1;
      // Outside edge: copper column with a substantial square iron
      // bracket beneath the roof, not a strip of exposed wood.
      solid(c,"bronze",[corner,3,13],[corner+3,15,16]);
      solid(c,"metal",[corner,0,0],[corner+3,3,16]);
      solid(c,"metal",[corner,11,14],[corner+3,16,16]);
      solid(c,"join",[corner+1,12,15],[corner+3,14,16]);
      // Inside edge: slim copper reinforcing pilaster on the front,
      // stopping just below the roof and finishing in a dark metal foot.
      solid(c,"bronze",[inside,3,13],[inside+2,13,16]);
      solid(c,"metal",[inside,0,13],[inside+2,3,16]);
      solid(c,"bronze",[inside,13,13],[inside+2,15,16]);
      // Low brick course becomes a stronger masonry sill on each side.
      solid(c,"bronze",[3,0,14],[13,1.5,16]);
    }
    if(outer&&!leftFacade&&!(x===1)){
      // Preserve exactly the prior side contacts at the top and bottom
      // to lock the Y=1 and Y=3 neighboring part meshes.
      const xx=x===-1?0:13;
      // V3.27: lift the obsolete left upper iron stripe off the vent.
      // Keep the original visible rear metal end cap and its material.
      if(rear)solid(c,"metal",[xx,13,0],[xx+3,16,0.5]);
      solid(c,"metal",[xx,0,0],[xx+3,3,16]);
      if(rear)solid(c,"bronze",[xx,3,0],[xx+3,14,2]);
    }

    // V3.25 — copper side cap/lip above the recessed masonry detail.
    // Mirror on both upper Y2 walls; the front iron mounts stop the run.
    if(outer){
      const endZ=front?(x===1?13:13.75):16;
      const minX=x===-1?0:15.15,maxX=x===-1?0.85:16;
      solid(c,"bronze",[minX,12.45,0],[maxX,13.35,endZ]);
      // Raised upper edge, not another full-height horizontal iron band.
      solid(c,"bronze",[x===-1?0:15.6,13.35,0],
                       [x===-1?0.4:16,13.65,endZ]);
    }

    // V3.27: ONE copper-framed vent with FOUR openings over the whole
    // THREE-block side, mirrored on both walls. SOUTH-local U=0..48
    // increases from the rear end towards the front mounting bracket.
    if(outer){
      const offset=(z+2)*16;
      const clip=(a,b)=>{
        const lo=Math.max(0,a-offset),hi=Math.min(16,b-offset);
        return hi-lo>1e-6?[lo,hi]:null;
      };
      // Leave the existing rear copper fitting (U0..2) and the front
      // square iron mounting (U45..48) undisturbed.
      const start=2.2,end=45;
      const posts=[[2.2,4.2],[12.4,14.4],[22.6,24.6],
                   [32.8,34.8],[43,45]];
      const sideMin=x===-1?0:14.85,sideMax=x===-1?1.15:16;
      const panel=clip(start,end);
      if(panel){
        // A recessed *visual* soot panel: no through-hole, no changes
        // to the multiblock's structural cells or functionality.
        if(x===-1)solid(c,"soot",[1.45,13.35,panel[0]],
                                   [1.6,15.075,panel[1]]);
        else solid(c,"soot",[14.2,13.35,panel[0]],
                             [14.3,15.075,panel[1]]);
        // Continuous upper copper frame, clipped at block boundaries.
        solid(c,"bronze",[sideMin,15.075,panel[0]],
                         [sideMax,16,panel[1]]);
      }
      for(const [a,b] of posts){
        const section=clip(a,b);
        if(section)solid(c,"bronze",[sideMin,13.35,section[0]],
                                     [sideMax,15.075,section[1]]);
      }
    }
    // NO continuous metal course below the roof. That extra horizontal
    // strip was not in the approved front reference (V3.10). The roof
    // contacts and the two actual corner fasteners remain unchanged.
   }else{
   const front=z===0,side=x!==0,rear=z===-2;
   // The important front openings are hollow/recessed masonry, not painted
   // faces on a full cube. The shoulder/corner cells also step backward.
   if(front&&x===0&&(y===1||y===2)){
    const opening=y===1?{lo:2,hi:14,bottom:2,top:15}:{lo:3,hi:13,bottom:6,top:12};
    solid(c,"brick",[0,0,0],[16,16,8]);
    solid(c,"brick",[0,0,8],[opening.lo,16,16]);
    solid(c,"brick",[opening.hi,0,8],[16,16,16]);
    solid(c,"brick",[opening.lo,0,8],[opening.hi,opening.bottom,16]);
    solid(c,"brick",[opening.lo,opening.top,8],[opening.hi,16,16]);
   }else if(front){
    solid(c,"brick",[0,0,0],[16,16,13]);
   }else if(side){
    // The two side walls are actually recessed behind their outer ribs.
    solid(c,"brick",[x===-1?1.5:0,0,0],[x===1?14.5:16,16,16]);
   }else{
    solid(c,"brick",[0,0,0],[16,16,16]);
   }
   if(side){
    // Actual corner buttresses instead of two layers of free-floating copper bands.
    const xa=x===-1?0:13;
    solid(c,"metal",[xa,0,0],[xa+3,3,16]);
    solid(c,"join",[xa,13,0],[xa+3,16,16]);
    if(front){
     solid(c,"bronze",[xa,3,13],[xa+3,15,16]);
     solid(c,"metal",[xa,0,14],[xa+3,3,16]);
    }else if(rear){
     solid(c,"metal",[xa,3,0],[xa+3,14,2]);
    }
   }
   if(front&&x===0&&y===1){
    // Dominant sealed door with copper jambs, side hinges and dark central grate.
    // A deep, wide opening. Dark recess sits behind discrete iron grating;
    // it is not another solid texture covering the whole opening.
    solid(c,"soot",[2.5,2.5,8],[13.5,14.5,10]);
    solid(c,"metal",[3,3,10],[13,13.5,11]);
    solid(c,"soot",[3.5,3.5,11],[12.5,12.5,12]);
    for(const xx of [4,7,10])solid(c,"metal",[xx,4,12],[xx+1,12,14]);
    solid(c,"metal",[3,3,12],[13,4,14]);
    solid(c,"metal",[3,12,12],[13,13,14]);
    solid(c,"bronze",[1,1,12],[3.5,15,16]);
    solid(c,"bronze",[12.5,1,12],[15,15,16]);
    solid(c,"bronze",[3.5,13.5,12],[12.5,16,16]);
    solid(c,"metal",[3.5,1,12],[12.5,3,16]);
    solid(c,"join",[11.5,7,15],[14,9,16]);
   } else if(front&&x===0&&y===2){
    // Small upper vent deliberately subordinate to the one-block door below.
    solid(c,"soot",[3,6,9],[13,12,11]);
    solid(c,"metal",[4,7,11],[12,11,12]);
    for(const xx of [4,7,10])solid(c,"metal",[xx,7,12],[xx+1,11,14]);
    solid(c,"bronze",[3,12,12],[13,14,16]);
    solid(c,"metal",[3,5,13],[13,6,16]);
    solid(c,"join",[2,5,13],[4,13,16]);
    solid(c,"join",[12,5,13],[14,13,16]);
   }
   if(side&&z===-1){
    if(x===-1)solid(c,"metal",[0,3,0],[2,5,16]);
    else solid(c,"metal",[14,3,0],[16,5,16]);
   }
   }
  } else if(k==="P"){
   // ROOF-ONLY PASS: the two dark lines are GROOVES CUT INTO copper,
   // not raised iron rails. The same local-X trench spans all 3 Z cells
   // in each outer roof column, for every rotated machine orientation.
   // The 3x3 JSON still contains eight roof blocks plus chimney AIR.
   solid(c,"bronze",[0,0,0],[16,3,16]);
   if(z===0&&x!==0){
     // Front-facing continuation of the two inset longitudinal grooves.
     // Do NOT overlay iron onto a full copper cube: carve the front
     // fascia into left copper, recessed iron channel, right copper.
     const lo=x===-1?12:2,hi=x===-1?14:4;
     solid(c,"roof",[0,3,0],[16,8,15.5]);
     solid(c,"roof",[0,3,15.5],[lo,8,16]);
     solid(c,"roof",[hi,3,15.5],[16,8,16]);
     solid(c,"metal",[lo,3,15.5],[hi,8.75,15.75]);
   }else{
     solid(c,"roof",[0,3,0],[16,8,16]);
   }
   if(x===-1){
    // Left seam, cut 1.25px below the 10px copper walking surface.
    solid(c,"roof",[0,8,0],[12,10,16]);
    solid(c,"roof",[14,8,0],[16,10,16]);
    solid(c,"metal",[12,8,0],[14,8.75,16]);
   }else if(x===1){
    // Mirrored right seam. Copper vertical walls remain visible.
    solid(c,"roof",[0,8,0],[2,10,16]);
    solid(c,"roof",[4,8,0],[16,10,16]);
    solid(c,"metal",[2,8,0],[4,8.75,16]);
   }else{
    solid(c,"roof",[0,8,0],[16,10,16]);
   }
  } else if(k==="A"&&y===3){
   // V3.6: a CONTINUOUS COPPER ROOF around the center air throat.
   // All four model segments sit flush with adjacent Y=3 copper roof at
   // local Y=10. This is not a raised shelf/collar: the center A cell
   // simply has its own roof surface around the 6x6 open flue.
   // These vertical copper inner-wall faces also cover the otherwise
   // untextured-looking gap from the roof's underside to the chimney.
   for(const [f,t] of [
     [[0,0,0],[5,10,16]],  [[11,0,0],[16,10,16]],
     [[5,0,0],[11,10,5]], [[5,0,11],[11,10,16]]
   ])solid(c,"roof",f,t);
   // V3.5 chimney-only cleanup: block-local, INTEGER-pixel copper.
   // The copper walls begin directly at roof Y=10, without the extra
   // floating lower collar. All exposed undersides get normal face UVs.
   // Two real horizontal vent rows cut all four walls (Y=11..12,
   // Y=13..14); the middle 6x6 shaft stays open.
   const courses=[[10,11],[12,13],[14,15]];
   const gaps=[[11,12],[13,14]];
   // Continuous structural corners: 3px-long solid posts at front/back.
   for(const [xa,xb] of [[3,6],[10,13]]){
     for(const [za,zb] of [[3,5],[11,13]]){
       solid(c,"roof",[xa,10,za],[xb,15,zb]);
     }
   }
   // The two long front/back walls. The center of each window is AIR,
   // with short copper end posts retained on either side.
   for(const [za,zb] of [[3,5],[11,13]]){
     for(const [ya,yb] of courses)
       solid(c,"roof",[6,ya,za],[10,yb,zb]);
   }
   // Side walls match the front/back construction and have matching
   // two-row slots. No solid wall is hidden behind the apertures.
   for(const [xa,xb] of [[3,5],[11,13]]){
     for(const [ya,yb] of courses)
       solid(c,"roof",[xa,ya,5],[xb,yb,11]);
   }
   // Very small remaining wall returns at the ends of each vent row.
   // These are part of the chimney body, not decorative floating strips.
   for(const [ya,yb] of gaps){
     for(const [za,zb] of [[3,5],[11,13]]){
       solid(c,"roof",[5,ya,za],[6,yb,zb]);
       solid(c,"roof",[10,ya,za],[11,yb,zb]);
     }
     for(const [xa,xb] of [[3,5],[11,13]]){
       solid(c,"roof",[xa,ya,5],[xb,yb,6]);
       solid(c,"roof",[xa,ya,10],[xb,yb,11]);
     }
   }
   // A single 1px-thick copper top lip (Y=15..16), with a proper
   // downward face exposed on its overhang. No dark band beneath it.
   for(const [f,t] of [
     [[2,15,2],[5,16,14]], [[11,15,2],[14,16,14]],
     [[5,15,2],[11,16,5]], [[5,15,11],[11,16,14]]
   ])solid(c,"roof",f,t);
  } else if(["U","O","Y"].includes(k)){
   // These are not fillers. The real item port BE selects this baked model.
   // Left log hopper vs right charcoal drawer vs rear tar port remain distinct.
   const rear=k==="Y",input=k==="U";
   // Not the generic wooden bins: inset feed/collection chambers in
   // real port blocks, framed by the same kiln-facing metal/bronze.
   port(c,"metal",[0,0,0],[16,4,16]);
   port(c,"stone",[0,4,0],[16,6,12]);
   port(c,"soot",[2.5,6,6],[13.5,12,12]);
   port(c,input?"wood":k==="O"?"charcoal":"soot",[4,7,9],[12,9.5,12]);
   port(c,"bronze",[1,5,11],[3.5,14,16]);
   port(c,"bronze",[12.5,5,11],[15,14,16]);
   port(c,"bronze",[3.5,12.5,11],[12.5,15,16]);
   port(c,"metal",[3.5,5,12],[12.5,6.5,16]);
   if(rear){
    // Rear tar drain is a recessed metal fitting, not a third front hopper.
    port(c,"metal",[4,7,0],[12,12,3]);
    port(c,"soot",[5,8,0],[11,11,1]);
    port(c,"join",[6,9,0],[10,10,2]);
   }else{
    port(c,"join",[4,5,14],[12,6.5,16]);
   }
  }
 }
 const rotate=(x,z,dir)=>dir==="south"?[x,z]:dir==="west"?[-z,x]:dir==="north"?[-x,-z]:[z,-x];
 const rotatePixel=(x,z,dir)=>dir==="south"?[x,z]:dir==="west"?[16-z,x]:dir==="north"?[16-x,16-z]:[z,16-x];
 const uv=(f,t)=>({
  north:[16-t[0],16-t[1],16-f[0],16-f[1]],
  south:[f[0],16-t[1],t[0],16-f[1]],
  east:[16-t[2],16-t[1],16-f[2],16-f[1]],
  west:[f[2],16-t[1],t[2],16-f[1]],
  up:[f[0],f[2],t[0],t[2]],
  down:[f[0],16-t[2],t[0],16-f[2]]
 });
 function makeModel(parts,dir){
  return {parent:"minecraft:block/block",ambientocclusion:false,textures,
   elements:parts.map(box=>{
    const corners=[[box.from[0],box.from[2]],[box.from[0],box.to[2]],
      [box.to[0],box.from[2]],[box.to[0],box.to[2]]].map(([x,z])=>rotatePixel(x,z,dir));
    const f=[Math.min(...corners.map(p=>p[0])),box.from[1],Math.min(...corners.map(p=>p[1]))],
          t=[Math.max(...corners.map(p=>p[0])),box.to[1],Math.max(...corners.map(p=>p[1]))];
    return {from:f,to:t,faces:Object.fromEntries(Object.entries(uv(f,t)).map(([s,u])=>[s,{texture:"#"+box.mat,uv:u}]))};
   })
  };
 }
 const json=o=>JSON.stringify(o)+"\n";
 const ports=[];
 for(const dir of dirs)for(const c of cells){
  const [x,z]=rotate(c.x,c.z,dir);
  let parts=shapes.get(cellKey(c));
  const byPort=portShapes.get(cellKey(c));
  if(byPort)parts=byPort;
  if(!parts)continue;
  const path=a+"models/multiblock/charcoal_retort_"+dir+"/part_"+x+"_"+c.y+"_"+z+".json";
  const m=makeModel(parts,dir);
  // Keep only this layer's texture change. The other three layers use
  // their old shared material map and generated JSON byte-for-byte.
  if(c.y===3)m.textures={...m.textures,roof:"minecraft:block/cut_copper"};
  if(c.y===2 && c.k==="B")m.textures={
    ...m.textures,
    // Keep structural side/rear backing on the original Retort brick.
    // Only physically raised RIGHT SIDE faces use vanilla sideBrick;
    // this avoids recoloring the rear exterior/corner wall as a
    // side effect of the visual front/side masonry cleanup.
    brick:c.z===0?"minecraft:block/bricks":"magneticraft2:block/charcoal_retort/refractory",
    bronze:"minecraft:block/cut_copper",
    join:"magneticraft2:block/settling_tank/riveted_iron",
    ...(c.z===0 ? {mortar:"minecraft:block/stone"} : {}),
    ...(c.x===1 ?
      {mortar:"minecraft:block/stone",sideBrick:"minecraft:block/bricks"} : {})
  };
  files.set(path,json(m));
  if(byPort){
   ports.push({dir,role:c.k==="U"?"input":c.k==="O"?"primary":"byproduct",path});
  }
  if(c.k==="C"){
   const lit=JSON.parse(JSON.stringify(m));
   for(const part of lit.elements)for(const face of Object.values(part.faces)){
    if(face.texture==="#off")face.texture="#on";
   }
   files.set(path.replace(".json","_lit.json"),json(lit));
   if(dir==="south"){
    files.set(a+"models/block/charcoal_retort_controller.json",json(m));
    files.set(a+"models/block/charcoal_retort_controller_lit.json",json(lit));
   }
  }
 }
 const physical=[];
 for(const c of cells){
  if(["S","B","P","C"].includes(c.k)){
   physical.push([c.x*16,c.y*16,c.z*16,(c.x+1)*16,c.y*16+(c.k==="P"?10:16),(c.z+1)*16]);
  }else if(c.k==="A"&&c.y===3){
   // Match the contiguous 10px roof seat, keep the 6x6 shaft empty.
   // Above the copper slab, four coarse chimney walls occupy y=10..16.
   for(const [f,t]of [
    [[0,0,0],[5,10,16]], [[11,0,0],[16,10,16]],
    [[5,0,0],[11,10,5]], [[5,0,11],[11,10,16]],
    [[3,10,3],[5,16,13]], [[11,10,3],[13,16,13]],
    [[5,10,3],[11,16,5]], [[5,10,11],[11,16,13]]
   ])physical.push([...f.map((v,i)=>v+[c.x,c.y,c.z][i]*16),...t.map((v,i)=>v+[c.x,c.y,c.z][i]*16)]);
  }
 }
 files.set(d+"multiblock_voxels/charcoal_retort.json",json({
  format:1,unit:"model_pixels",collision:physical,selection:physical,zones:{}
 }));
 return {ports,modelCells:shapes.size,physicalBoxes:physical.length};
}
export function finalizeRetortV3(files,ports){
 for(const {dir,role,path}of ports){
  const target="src/main/resources/assets/magneticraft2/models/multiblock/charcoal_retort_"+
   dir+"/port_"+role+".json";
  const model=files.get(path);
  if(!model)throw Error("Missing baked formed-state real port "+path);
  files.set(target,model);
  files.delete(path); // critical: the real block, not a V2 filler, renders this model
 }
}
/**
 * Shade exposed INNER wall faces without inserting coplanar texture plates.
 * This runs AFTER the V2 geometry partitioner so its mixed-material faces
 * cannot be mistaken for overlapping solid cuboids during generation.
 * The 6x6 chimney aperture is symmetric under all horizontal rotations.
 */
export function textureRetortChimneyInteriors(files) {
 const prefix="src/main/resources/assets/magneticraft2/models/multiblock/";
 let changed=0;
 for(const [dir,offset] of Object.entries({
   south:"0_3_-1",north:"0_3_1",west:"1_3_0",east:"-1_3_0"
 })){
   const path=prefix+"charcoal_retort_"+dir+"/part_"+offset+".json";
   const model=JSON.parse(files.get(path)||"null");
   if(!model||!Array.isArray(model.elements))throw Error("No chimney mesh for "+dir);
   let shaded=0;
   for(const element of model.elements){
     const f=element.from,t=element.to;
     // Assign soot exactly to *visible* inward faces on the shaft wall.
     // Existing face UVs remain in 1-model-pixel-per-texel scale.
     const inside=[
       ["east",t[0]===5,f[2]>=5&&t[2]<=11],
       ["west",f[0]===11,f[2]>=5&&t[2]<=11],
       ["south",t[2]===5,f[0]>=5&&t[0]<=11],
       ["north",f[2]===11,f[0]>=5&&t[0]<=11]
     ];
     for(const [side,onPlane,inSpan] of inside){
       if(onPlane&&inSpan&&f[1]>=0&&t[1]<=15&&element.faces[side]){
         element.faces[side].texture="#roof";
         shaded++;
       }
     }
   }
   if(shaded===0)throw Error("Chimney missing inner copper wall faces: "+dir);
   files.set(path,JSON.stringify(model)+"\n");
   changed++;
 }
 return changed;
}
