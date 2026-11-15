package studio.fourthrune.yggdrasil;

import net.minecraft.core.*;
import net.minecraft.world.level.block.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.level.ChunkEvent;

final class CampaignWorld {
 static void overworldChunk(ChunkEvent.Load e){if(e.isNewChunk()&&e.getLevel() instanceof ServerLevel l&&l.dimension().equals(Level.OVERWORLD)){var c=e.getChunk().getPos();if(Math.floorMod(c.x(),32)==9&&Math.floorMod(c.z(),32)==13)l.getServer().execute(()->ruin(l,c.getMinBlockX()+8,c.getMinBlockZ()+8));}}
 static void ruin(ServerLevel l,int x,int z){if(!l.hasChunkAt(new BlockPos(x,80,z)))return;int y=l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);if(!l.getFluidState(new BlockPos(x,y-1,z)).isEmpty())return;WorldBuilder.platform(l,new BlockPos(x,y-1,z),5,Blocks.MOSSY_STONE_BRICKS);for(int dx:new int[]{-5,5})for(int dz:new int[]{-4,4}){WorldBuilder.box(l,x+dx,y,z+dz,x+dx,y+2+Math.floorMod(x+dz,3),z+dz,Blocks.MOSSY_COBBLESTONE);}WorldBuilder.put(l,x,y,z,Yggdrasil.INACTIVE.get());}
 static boolean built(ServerLevel l,String name){return Travel.state(l.getServer()).routes.containsKey("built:"+name);}
 static void mark(ServerLevel l,String name,BlockPos pos){Travel.state(l.getServer()).routes.put("built:"+name,new Travel.Route(l.dimension().identifier().toString(),pos));Travel.save(l.getServer());}
 static void zone(ServerPlayer p,int index){var l=(ServerLevel)p.level();int stage=Campaign.stage(l);if(stage<0||stage>3||built(l,"zone_"+stage))return;int x=160,z=0;l.getChunk(x>>4,z>>4);int y=Math.max(70,l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z)+2);
  // A reserved arrival landmark above the terrain, with a recognizable arena for each guardian.
  WorldBuilder.disk(l,x,y-1,z,29,stage==0?Blocks.BLUE_ICE:stage==1?Blocks.CUT_COPPER.weathering().unaffected():Blocks.POLISHED_DEEPSLATE);
  for(int a=0;a<12;a++){double angle=a*Math.PI/6;int px=x+(int)(Math.cos(angle)*25),pz=z+(int)(Math.sin(angle)*25);WorldBuilder.box(l,px-1,y,pz-1,px+1,y+(stage==0?18:10),pz+1,stage==0?Blocks.PACKED_ICE:Blocks.CHISELED_STONE_BRICKS);WorldBuilder.put(l,px,y+11,pz,stage==0?Blocks.SEA_LANTERN:Blocks.SOUL_LANTERN);}
  if(stage==3){
   for(int dx:new int[]{-23,23})WorldBuilder.box(l,x+dx,y,-25,x+dx,y+18,25,Blocks.DEEPSLATE_BRICKS);
   WorldBuilder.box(l,x-23,y,25,x+23,y+18,25,Blocks.DEEPSLATE_BRICKS);WorldBuilder.box(l,x-24,y+19,-26,x+24,y+20,26,Blocks.DEEPSLATE_TILES);
   for(int dz=-18;dz<=18;dz+=9)for(int side:new int[]{-1,1}){WorldBuilder.box(l,x+side*18,y,dz,x+side*18,y+18,dz,Blocks.POLISHED_BLACKSTONE);WorldBuilder.lamp(l,x+side*17,y+1,dz);}
   for(int k=0;k<6;k++){WorldBuilder.put(l,x-15+k*6,y,20,Blocks.CHEST);}
   WorldBuilder.put(l,x,y,18,Content.block("secret_gate"));
  }
  if(stage==0)for(int dx:new int[]{-14,14})for(int dz:new int[]{-14,14})WorldBuilder.put(l,x+dx,y,dz,Blocks.AMETHYST_BLOCK);
  if(stage==1){for(int dx:new int[]{-12,12})for(int dz:new int[]{-12,12}){WorldBuilder.put(l,x+dx,y-1,dz,Blocks.MAGMA_BLOCK);WorldBuilder.put(l,x+dx,y,dz,Blocks.CAMPFIRE);}}
  if(stage==2){for(int dx:new int[]{-10,10})WorldBuilder.put(l,x+dx,y,12,Content.block("spectral_lantern"));}
  WorldBuilder.put(l,x,y,z,Content.block("boss_altar"));mark(l,"zone_"+stage,new BlockPos(x,y,z));Campaign.say(p,"Le sanctuaire du gardien se trouve à x="+x+", y="+y+", z=0. Apporte une offrande runique à l’autel.");
 }
 static void hub(ServerLevel l){if(built(l,"commissions"))return;
  for(int x=14;x<=84;x++){for(int dz=-1;dz<=1;dz++)WorldBuilder.put(l,x,82,147+dz,Blocks.DARK_OAK_PLANKS);if(x%8==0){WorldBuilder.lamp(l,x,83,144);WorldBuilder.lamp(l,x,83,150);}}
  for(int z=112;z<=182;z++){for(int dx=-1;dx<=1;dx++)WorldBuilder.put(l,84+dx,82,z,Blocks.DARK_OAK_PLANKS);}
  for(int k=0;k<3;k++){int z=120+k*27;WorldBuilder.platform(l,new BlockPos(84,82,z),10,Blocks.DARK_OAK_PLANKS);for(int x:new int[]{74,94})for(int dz:new int[]{-10,10})WorldBuilder.lamp(l,x,83,z+dz);}
  WorldBuilder.put(l,18,83,147,Content.block("commission_board"));mark(l,"commissions",new BlockPos(84,82,147));
 }
 static void secret(ServerPlayer p){var s=p.level().getServer();var l=s.getLevel(Yggdrasil.realm("sanctum"));if(l==null){Campaign.say(p,"Le sanctuaire n’est pas chargé.");return;}
  Travel.state(s).routes.put("checkpoint:"+p.getUUID(),new Travel.Route(p.level().dimension().identifier().toString(),p.blockPosition().offset(0,0,-4)));Travel.save(s);
  if(!built(l,"arena")){
   int r=86;for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++){double d=Math.hypot(x,z);if(d<=r){Block floor=(Math.abs(d-23)<.8||Math.abs(d-46)<.8||Math.abs(d-69)<.8)?Yggdrasil.GLOW_WOOD.get():Blocks.POLISHED_BLACKSTONE;WorldBuilder.put(l,x,82,z,floor);WorldBuilder.put(l,x,81,z,Blocks.DEEPSLATE_BRICKS);if(d>r-2)WorldBuilder.box(l,x,83,z,x,87,z,Blocks.CHISELED_POLISHED_BLACKSTONE);}}
   for(int a=0;a<16;a++){double angle=a*Math.PI/8;int x=(int)(Math.cos(angle)*78),z=(int)(Math.sin(angle)*78);WorldBuilder.box(l,x-1,83,z-1,x+1,108,z+1,Blocks.POLISHED_BLACKSTONE);WorldBuilder.put(l,x,109,z,Blocks.SOUL_LANTERN);
    int rx=(int)(Math.cos(angle)*58),rz=(int)(Math.sin(angle)*58);for(int t=-6;t<=6;t++){WorldBuilder.put(l,rx,82,rz+t,Yggdrasil.GLOW_WOOD.get());if(t>=0&&t<=4){WorldBuilder.put(l,rx+t,82,rz+4-t,Yggdrasil.GLOW_WOOD.get());WorldBuilder.put(l,rx-t,82,rz+4-t,Yggdrasil.GLOW_WOOD.get());}}
   }
   WorldBuilder.put(l,0,83,0,Content.block("boss_altar"));mark(l,"arena",new BlockPos(0,83,0));
  }
  Travel.teleport(p,l,new BlockPos(0,83,62));Campaign.say(p,"Le Sanctuaire de la Dernière Nuit. Une offrande au centre appellera Várnótt.");
 }
 static void returnDungeon(ServerPlayer p){var s=p.level().getServer();var r=Travel.state(s).routes.get("checkpoint:"+p.getUUID());if(r!=null){var key=ResourceKey.create(Registries.DIMENSION,net.minecraft.resources.Identifier.parse(r.dimension));var level=s.getLevel(key);if(level!=null){Travel.teleport(p,level,r.pos());return;}}Travel.enter(p);}
}
