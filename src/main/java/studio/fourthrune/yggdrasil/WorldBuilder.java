package studio.fourthrune.yggdrasil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import java.util.Random;

final class WorldBuilder {
 static void put(ServerLevel l,int x,int y,int z,Block b){l.setBlock(new BlockPos(x,y,z),b.defaultBlockState(),2);}
 static void box(ServerLevel l,int x1,int y1,int z1,int x2,int y2,int z2,Block b){for(int x=x1;x<=x2;x++)for(int z=z1;z<=z2;z++)for(int y=y1;y<=y2;y++)put(l,x,y,z,b);}
 static void platform(ServerLevel l,BlockPos b,int radius,Block material){for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++){put(l,b.getX()+x,b.getY(),b.getZ()+z,material);put(l,b.getX()+x,b.getY()-1,b.getZ()+z,Blocks.DEEPSLATE_BRICKS);}}
 static boolean clearGateSpace(ServerLevel l,BlockPos b){for(int x=-3;x<=3;x++)for(int y=0;y<=6;y++)for(int z=-2;z<=2;z++)if(!l.getBlockState(b.offset(x,y,z)).canBeReplaced()||!l.getFluidState(b.offset(x,y,z)).isEmpty())return false;return true;}
 static void gate(ServerLevel l,BlockPos b){put(l,b.getX(),b.getY(),b.getZ(),Yggdrasil.PORTAL.get());}
 static void lamp(ServerLevel l,int x,int y,int z){put(l,x,y,z,Blocks.CHISELED_STONE_BRICKS);put(l,x,y+1,z,Blocks.DARK_OAK_FENCE);put(l,x,y+2,z,Blocks.SOUL_LANTERN);}
 static int leafCount=0;
 static void ellipsoid(ServerLevel l,int cx,int cy,int cz,int rx,int ry,int rz,Block material,boolean shell){
  for(int x=-rx;x<=rx;x++)for(int y=-ry;y<=ry;y++)for(int z=-rz;z<=rz;z++){
   double d=x*x/(double)(rx*rx)+y*y/(double)(ry*ry)+z*z/(double)(rz*rz);
   if(d<=1 && (!shell||d>.48)){
    BlockPos p=new BlockPos(cx+x,cy+y,cz+z);
    if(material instanceof LeavesBlock){if(!l.getBlockState(p).isAir()&&!(l.getBlockState(p).getBlock() instanceof LeavesBlock))continue;
     BlockState st=material.defaultBlockState().setValue(LeavesBlock.PERSISTENT,true);l.setBlock(p,st,2);leafCount++;
    }else put(l,p.getX(),p.getY(),p.getZ(),material);
   }
  }
 }
 static void limb(ServerLevel l,double ax,double ay,double az,double bx,double by,double bz,double radius){
  double dx=bx-ax,dy=by-ay,dz=bz-az;int steps=(int)(Math.sqrt(dx*dx+dy*dy+dz*dz)*1.5)+1;
  for(int t=0;t<=steps;t++){double f=t/(double)steps;int r=Math.max(1,(int)Math.round(radius*(1-.65*f)));ellipsoid(l,(int)(ax+dx*f),(int)(ay+dy*f),(int)(az+dz*f),r,r,r,Blocks.DARK_OAK_LOG,false);}
 }
 static void hangingGarden(ServerLevel l,int x,int y,int z,int length){
  for(int d=0;d<length;d++){
   var p=new BlockPos(x,y-d,z);if(l.getBlockState(p).isAir()){
    BlockState st=Blocks.CAVE_VINES_PLANT.defaultBlockState().setValue(CaveVinesPlantBlock.BERRIES,d%3==0);l.setBlock(p,st,2);
   }
  }
 }
 static void disk(ServerLevel l,int cx,int y,int cz,int radius,Block floor){
  for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++)if(x*x+z*z<radius*radius){put(l,cx+x,y,cz+z,(x*x+z*z>(radius-2)*(radius-2))?Blocks.MOSSY_STONE_BRICKS:floor);int depth=Math.max(1,(radius-Math.max(Math.abs(x),Math.abs(z)))/3);for(int d=1;d<=depth;d++)put(l,cx+x,y-d,cz+z,Blocks.DARK_OAK_LOG);}
 }
 static void hub(ServerLevel l){
  leafCount=0;Random random=new Random(444);
  // A living root supporting a carved bridge, with ribbons, ribs and side gardens.
  for(int z=-10;z<=166;z++){
   int cx=(int)(Math.sin(z*.037)*7),y=80+(z>60?1:0)+(z>115?1:0);
   for(int x=-7;x<=7;x++)for(int d=0;d<5;d++)if(Math.abs(x)+d<9){BlockPos p=new BlockPos(cx+x,y-d,z);l.setBlock(p,Blocks.DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.Z),2);}
   for(int x=-4;x<=4;x++)put(l,cx+x,y,z,Math.abs(x)==4?Blocks.POLISHED_DEEPSLATE:((z%8==0)?Blocks.STRIPPED_SPRUCE_LOG:Blocks.DARK_OAK_PLANKS));
   put(l,cx,y,z,Yggdrasil.GLOW_WOOD.get());
   for(int side:new int[]{-1,1}){
    // Explicit connections prevent isolated fence posts with world-generation update flags.
    BlockState fence=Blocks.DARK_OAK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH,true).setValue(FenceBlock.SOUTH,true);
    l.setBlock(new BlockPos(cx+side*5,y+1,z),fence,2);
    if(z%8==0){lamp(l,cx+side*5,y+1,z);put(l,cx+side*6,y,z,Blocks.CHISELED_POLISHED_BLACKSTONE);}
    if(z%12==0){for(int d=1;d<=4;d++)put(l,cx+side*(5+d/2),y-d,z,Blocks.DARK_OAK_LOG);hangingGarden(l,cx+side*7,y-2,z,5+random.nextInt(7));}
   }
  }
  // Carved archways frame the view instead of a featureless straight deck.
  for(int z:new int[]{22,58,100,136}){
   int cx=(int)(Math.sin(z*.037)*7),y=80+(z>60?1:0)+(z>115?1:0);
   for(int side:new int[]{-1,1}){limb(l,cx+side*6,y,z,cx+side*6,y+7,z,2);limb(l,cx+side*6,y+7,z,cx,y+12,z+1,2);ellipsoid(l,cx+side*7,y+9,z,4,3,4,Blocks.FLOWERING_AZALEA_LEAVES,true);hangingGarden(l,cx+side*7,y+8,z-1,5);}
   put(l,cx,y+10,z,Blocks.SOUL_LANTERN);put(l,cx,y+12,z,Yggdrasil.GLOW_WOOD.get());
  }
  for(int z:new int[]{40,82,120}){
   int cx=(int)(Math.sin(z*.037)*7),y=80+(z>60?1:0)+(z>115?1:0);
   for(int side:new int[]{-1,1}){int x=cx+side*10;disk(l,x,y,z,6,Blocks.MOSS_BLOCK);box(l,cx+side*5,y,z-2,cx+side*5,y+1,z+2,Blocks.AIR);for(int k=4;k<=10;k++)for(int dz=-1;dz<=1;dz++)put(l,cx+side*k,y,z+dz,Blocks.DARK_OAK_PLANKS);limb(l,x,y+1,z,x+side,y+7,z+1,1.8);ellipsoid(l,x,y+8,z,5,3,5,Blocks.AZALEA_LEAVES,true);put(l,x,y+7,z,Blocks.SHROOMLIGHT);for(int dz:new int[]{-3,3}){hangingGarden(l,x+side*4,y,z+dz,8);put(l,x-2,y+1,z+dz,Blocks.SMALL_AMETHYST_BUD);}lamp(l,x-side*2,y+1,z+3);}
  }
  disk(l,0,80,0,11,Blocks.STONE_BRICKS);gate(l,new BlockPos(0,81,0));
  disk(l,0,82,154,17,Blocks.DEEPSLATE_TILES);
  for(int x=-12;x<=12;x++)for(int z=142;z<=166;z++){int r=x*x+(z-154)*(z-154);if(r>100&&r<130)put(l,x,82,z,Yggdrasil.GLOW_WOOD.get());}
  for(int x=-1;x<=1;x++)for(int y=0;y<5;y++)for(int z=0;z<=1;z++)put(l,x,83+y,154+z,Yggdrasil.WAYSTONE_PART.get());put(l,0,83,154,Yggdrasil.WAYSTONE.get());
  for(int side:new int[]{-1,1}){lamp(l,side*7,83,149);limb(l,side*14,79,156,side*11,91,158,3);ellipsoid(l,side*13,94,160,7,4,7,Blocks.FLOWERING_AZALEA_LEAVES,true);hangingGarden(l,side*17,91,155,10);}
  // Irregular taper, buttress roots, crooked primary limbs and secondary boughs.
  for(int y=34;y<=180;y++){double t=(y-34)/146.0;int cx=(int)(Math.sin(t*5)*3),cz=219+(int)(Math.sin(t*3)*3);int r=(int)(14-9*t);for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++)if(x*x+z*z<r*r && (x*x+z*z>(r-3)*(r-3)||y>145))put(l,cx+x,y,cz+z,((x+z+y)%13==0)?Blocks.STRIPPED_DARK_OAK_LOG:Blocks.DARK_OAK_LOG);}
  for(int a=0;a<10;a++){double angle=a*Math.PI/5;int ex=(int)(Math.cos(angle)*36),ez=219+(int)(Math.sin(angle)*36);limb(l,0,92,219,ex,64,ez,5);limb(l,ex,64,ez,ex*1.25,47,219+(ez-219)*1.25,3);}
  for(int a=0;a<11;a++){
   double angle=a*Math.PI*2/11+.2;int sx=(int)(Math.cos(angle)*6),sz=219+(int)(Math.sin(angle)*6),sy=118+(a%3)*12;
   int mx=(int)(Math.cos(angle)*24),mz=219+(int)(Math.sin(angle)*24),my=sy+15;
   int ex=(int)(Math.cos(angle+.16)*43),ez=219+(int)(Math.sin(angle+.16)*43),ey=sy+24+(a%2)*6;
   limb(l,sx,sy,sz,mx,my,mz,5);limb(l,mx,my,mz,ex,ey,ez,3.5);
   for(int j=0;j<3;j++){double da=angle+(j-1)*.5;int tx=ex+(int)(Math.cos(da)*10),tz=ez+(int)(Math.sin(da)*10),ty=ey+6+j*2;limb(l,mx,my,mz,tx,ty,tz,2);ellipsoid(l,tx,ty+3,tz,11,6,10,(a+j)%3==0?Blocks.FLOWERING_AZALEA_LEAVES:Blocks.AZALEA_LEAVES,true);put(l,tx,ty+2,tz,Blocks.SHROOMLIGHT);for(int k=0;k<3;k++)hangingGarden(l,tx-6+k*6,ty,tz-6,7+random.nextInt(12));}
   ellipsoid(l,mx,my+7,mz,12,7,11,Blocks.AZALEA_LEAVES,true);
  }
  for(int j=0;j<5;j++)ellipsoid(l,(j-2)*8,180+((j%2)*7),219+(j%2)*12,14,8,13,Blocks.FLOWERING_AZALEA_LEAVES,true);
  // Floating gardens give depth on both sides while leaving the sky visible.
  for(int i=0;i<9;i++){int side=i%2==0?-1:1,x=side*(24+(i%3)*10),z=24+i*15,y=67+(i%3)*5;disk(l,x,y,z,5+i%3,Blocks.MOSS_BLOCK);limb(l,x,y,z,x+side*2,y+7,z,2);ellipsoid(l,x+side*2,y+9,z,6,4,6,Blocks.FLOWERING_AZALEA_LEAVES,true);put(l,x,y+6,z,Blocks.SHROOMLIGHT);for(int k=-3;k<=3;k+=3)hangingGarden(l,x+k,y,z-4,10+random.nextInt(8));}
  disk(l,0,82,188,28,Blocks.DARK_OAK_PLANKS);
  for(int side:new int[]{-1,1}){limb(l,side*22,49,209,side*16,82,180,5);for(int z=174;z<=206;z+=8){lamp(l,side*21,83,z);ellipsoid(l,side*23,83,z+2,3,2,3,Blocks.AZALEA_LEAVES,true);}}
  hall(l);
 }
 static void stair(ServerLevel l,int x,int y,int z,Block stair,Direction face){l.setBlock(new BlockPos(x,y,z),stair.defaultBlockState().setValue(StairBlock.FACING,face),2);}
 static void hall(ServerLevel l){
  int front=168,back=207;
  box(l,-15,83,front,15,83,back,Blocks.DARK_OAK_PLANKS);
  for(int y=84;y<=95;y++){
   for(int side:new int[]{-1,1})box(l,side*15,y,front,side*15,y,back,y<87?Blocks.STONE_BRICKS:Blocks.SPRUCE_PLANKS);
   box(l,-15,y,front,15,y,front,y<87?Blocks.MOSSY_STONE_BRICKS:Blocks.SPRUCE_PLANKS);box(l,-15,y,back,15,y,back,y<87?Blocks.STONE_BRICKS:Blocks.SPRUCE_PLANKS);
  }
  for(int z=168;z<=207;z+=7)for(int side:new int[]{-1,1}){box(l,side*15,84,z,side*15,96,z,Blocks.STRIPPED_DARK_OAK_LOG);box(l,side*16,84,z,side*16,87,z,Blocks.CHISELED_STONE_BRICKS);put(l,side*16,90,z,Blocks.SOUL_LANTERN);}
  for(int x:new int[]{-15,-8,0,8,15})for(int z:new int[]{front,back})box(l,x,84,z,x,96,z,Blocks.STRIPPED_DARK_OAK_LOG);
  // Closed triangular timber gables, repeated sloping beams and deep, steep eaves.
  for(int x=-18;x<=18;x++){
   int roof=96+(18-Math.abs(x));
   for(int z=165;z<=210;z++){stair(l,x,roof,z,Blocks.DEEPSLATE_TILE_STAIRS,x<0?Direction.EAST:Direction.WEST);if(z%7==4)put(l,x,roof+1,z,Blocks.DARK_OAK_LOG);}
   if(Math.abs(x)<=15)for(int z:new int[]{front,back})box(l,x,96,z,x,roof-1,z,Math.abs(x)%7==0?Blocks.DARK_OAK_LOG:Blocks.SPRUCE_PLANKS);
  }
  box(l,0,114,164,0,115,211,Blocks.DARK_OAK_LOG);
  for(int z=165;z<=210;z+=5){put(l,0,116,z,Yggdrasil.GLOW_WOOD.get());for(int side:new int[]{-1,1})put(l,side*18,96,z,Blocks.DARK_OAK_LOG);}
  // Upturned carved prow heads at both ridge ends.
  for(int z:new int[]{163,212}){int side=z<180?-1:1;box(l,0,114,z,0,118,z,Blocks.DARK_OAK_LOG);put(l,0,119,z+side,Blocks.STRIPPED_DARK_OAK_LOG);box(l,-1,119,z+side*2,1,120,z+side*2,Blocks.DARK_OAK_LOG);put(l,0,120,z+side*3,Blocks.DARK_OAK_STAIRS);put(l,-1,120,z+side*2,Blocks.SOUL_LANTERN);put(l,1,120,z+side*2,Blocks.SOUL_LANTERN);put(l,-1,121,z+side,Blocks.DARK_OAK_FENCE);put(l,1,121,z+side,Blocks.DARK_OAK_FENCE);}
  // Framed round-ish stained-glass gable windows and carved cyan crest.
  for(int z:new int[]{167,208})for(int x=-3;x<=3;x++)for(int y=100;y<=106;y++)if(x*x+(y-103)*(y-103)<12)put(l,x,y,z,Blocks.STAINED_GLASS.pick(net.minecraft.world.item.DyeColor.CYAN));
  for(int y=98;y<=110;y++)put(l,0,y,166,(y%3==0)?Yggdrasil.GLOW_WOOD.get():Blocks.STRIPPED_DARK_OAK_LOG);
  // Porch with a smaller pitched roof, stone steps and two carved gateposts.
  box(l,-8,83,160,8,83,167,Blocks.SPRUCE_PLANKS);
  for(int z=157;z<=159;z++)for(int x=-4;x<=4;x++)stair(l,x,80+z-157,z,Blocks.STONE_BRICK_STAIRS,Direction.SOUTH);
  for(int x:new int[]{-7,7}){box(l,x,84,161,x,93,161,Blocks.STRIPPED_DARK_OAK_LOG);box(l,x-1,84,161,x+1,85,161,Blocks.CHISELED_STONE_BRICKS);put(l,x,91,160,Blocks.SOUL_LANTERN);}
  for(int x=-9;x<=9;x++)for(int z=159;z<=169;z++)stair(l,x,94+(9-Math.abs(x))/2,z,Blocks.DARK_OAK_STAIRS,x<0?Direction.EAST:Direction.WEST);
  box(l,-2,84,167,2,88,169,Blocks.AIR);
  // Warm windows, side shields and decorated wall bands.
  for(int z=173;z<205;z+=7)for(int side:new int[]{-1,1}){for(int y=87;y<=89;y++)for(int dz=-1;dz<=1;dz++)put(l,side*15,y,z+dz,Blocks.STAINED_GLASS.pick(net.minecraft.world.item.DyeColor.ORANGE));put(l,side*14,87,z,Blocks.LANTERN);put(l,side*16,91,z+3,Blocks.WOOL.pick(net.minecraft.world.item.DyeColor.CYAN));put(l,side*16,90,z+3,Blocks.IRON_BARS);}
  box(l,-14,90,169,14,90,206,Blocks.DARK_OAK_PLANKS);
  box(l,-13,90,170,-10,90,178,Blocks.AIR);
  for(int t=0;t<7;t++)for(int x=-13;x<=-11;x++)stair(l,x,84+t,171+t,Blocks.SPRUCE_STAIRS,Direction.SOUTH);
  for(int side:new int[]{-1,1})for(int start=173;start<=195;start+=11){int xa=side<0?-14:4,xb=side<0?-4:14;box(l,xa,91,start,xb,95,start,Blocks.SPRUCE_PLANKS);box(l,side*4,91,start,side*4,95,start+10,Blocks.SPRUCE_PLANKS);box(l,side*4,91,start+4,side*4,92,start+5,Blocks.AIR);bed(l,side*10,91,start+4);put(l,side*12,91,start+7,Blocks.CHEST);lamp(l,side*7,91,start+8);}
  box(l,-2,84,185,2,84,191,Blocks.COBBLESTONE);put(l,0,85,188,Blocks.CAMPFIRE);box(l,-1,88,187,1,117,189,Blocks.STONE_BRICKS);box(l,-2,118,186,2,118,190,Blocks.COBBLESTONE_SLAB);
  for(int x:new int[]{-7,7}){box(l,x,84,181,x,84,190,Blocks.DARK_OAK_FENCE);box(l,x-1,85,181,x+1,85,190,Blocks.SPRUCE_SLAB);for(int z=181;z<=190;z+=3){stair(l,x-3,84,z,Blocks.SPRUCE_STAIRS,Direction.EAST);stair(l,x+3,84,z,Blocks.SPRUCE_STAIRS,Direction.WEST);}}
  for(int x=5;x<=13;x++)put(l,x,84,204,Blocks.SPRUCE_PLANKS);put(l,6,85,204,Blocks.SMOKER);put(l,7,85,204,Blocks.SMOKER);put(l,10,85,204,Blocks.WATER_CAULDRON);put(l,12,85,204,Blocks.CRAFTING_TABLE);for(int x=5;x<=13;x+=2)put(l,x,86,206,Blocks.BARREL);
  box(l,-13,84,198,-5,84,206,Blocks.STONE_BRICKS);put(l,-12,85,204,Blocks.BLAST_FURNACE);put(l,-11,85,204,Blocks.BLAST_FURNACE);put(l,-8,85,203,Blocks.ANVIL);put(l,-6,85,204,Blocks.SMITHING_TABLE);put(l,-12,85,199,Blocks.GRINDSTONE);put(l,-6,85,199,Blocks.CRAFTING_TABLE);
  for(int z=172;z<=202;z+=6){lamp(l,-13,84,z);lamp(l,13,84,z);put(l,0,89,z,Blocks.LANTERN);}
 }
 static void bed(ServerLevel l,int x,int y,int z){l.setBlock(new BlockPos(x,y,z),Blocks.BED.pick(net.minecraft.world.item.DyeColor.CYAN).defaultBlockState().setValue(BedBlock.FACING,Direction.SOUTH).setValue(BedBlock.PART,BedPart.FOOT),2);l.setBlock(new BlockPos(x,y,z+1),Blocks.BED.pick(net.minecraft.world.item.DyeColor.CYAN).defaultBlockState().setValue(BedBlock.FACING,Direction.SOUTH).setValue(BedBlock.PART,BedPart.HEAD),2);}
 static void realmArrival(ServerLevel l,int index,BlockPos b){
  // Only build in newly introduced dimensions, in a reserved arrival sanctuary.
  int y=b.getY();
  Block floor=switch(index){case 3->Blocks.CUT_COPPER.weathering().unaffected();case 4->Blocks.PACKED_ICE;case 5->Blocks.DEEPSLATE_BRICKS;default->Blocks.STONE_BRICKS;};
  box(l,-16,(y-4),-16,16,(y+8),16,Blocks.AIR);platform(l,b.below(),12,floor);
  for(int x:new int[]{-10,10})for(int z:new int[]{-10,10}){box(l,x-1,y,z-1,x+1,index==4?(y+15):(y+7),z+1,index==4?Blocks.BLUE_ICE:Blocks.POLISHED_DEEPSLATE);put(l,x,index==4?(y+16):(y+8),z,Blocks.SEA_LANTERN);}
  for(int z=4;z<=12;z++)put(l,0,(y-1),z,Yggdrasil.GLOW_WOOD.get());
  if(index==3){for(int x:new int[]{-7,7}){box(l,x,y,7,x,(y+6),7,Blocks.CHERRY_LOG);box(l,x-3,(y+7),4,x+3,(y+8),10,Blocks.CHERRY_LEAVES);}box(l,-5,y,-12,5,(y+1),-10,Blocks.CHISELED_RED_SANDSTONE);}
  if(index==4){for(int x:new int[]{-6,6}){box(l,x,y,7,x,(y+5),7,Blocks.BLUE_ICE);put(l,x,(y+6),7,Blocks.AMETHYST_BLOCK);}}
  if(index==5){for(int x=-11;x<=11;x++){put(l,x,y,12,Blocks.DARK_OAK_FENCE);if(x%4==0)lamp(l,x,y,11);}box(l,6,y,4,9,y,9,Blocks.DARK_OAK_PLANKS);box(l,7,(y+1),5,7,(y+6),5,Blocks.DARK_OAK_FENCE);box(l,8,(y+4),5,10,(y+6),5,Blocks.WOOL.pick(net.minecraft.world.item.DyeColor.CYAN));}
  if(index==6){box(l,-5,y,8,5,(y+5),8,Blocks.CHISELED_STONE_BRICKS);for(int x=-4;x<=4;x+=4){l.setBlock(new BlockPos(x,(y+1),7),Blocks.NOTE_BLOCK.defaultBlockState().setValue(NoteBlock.NOTE,x<0?5:x==0?9:12),2);put(l,x,(y+3),7,Yggdrasil.GLOW_WOOD.get());}}
 }
}
