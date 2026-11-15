package studio.fourthrune.yggdrasil;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.*;
import java.util.*;

public final class Travel {
 static final String[] NAMES={"overworld","nether","end","solfara","hrimdalr","draugavik","skaldheim"};
 static final BlockPos STONE=new BlockPos(0,83,154);
 static final BlockPos HUB_GATE=new BlockPos(0,81,0);
 private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
 private static final Map<MinecraftServer,State> STATES=new WeakHashMap<>();
 private static final Map<UUID,Long> COOLDOWN=new HashMap<>();
 static final class Route {String dimension;int x,y,z;Route(String d,BlockPos p){dimension=d;x=p.getX();y=p.getY();z=p.getZ();}BlockPos pos(){return new BlockPos(x,y,z);}}
 static final class State {boolean built,endOpen;Map<String,Integer> repairs=new HashMap<>();Map<String,Integer> feedback=new HashMap<>();Map<String,Route> routes=new HashMap<>();}
 static Path file(MinecraftServer s){return s.getWorldPath(LevelResource.ROOT).resolve("data/yggdrasil-routes.json");}
 static State state(MinecraftServer s){return STATES.computeIfAbsent(s,k->{try{if(Files.exists(file(s))){State st=GSON.fromJson(Files.readString(file(s)),State.class);if(st!=null&&st.routes!=null)return st;}}catch(Exception ex){throw new IllegalStateException("Cannot read Yggdrasil routes; refusing to overwrite",ex);}return new State();});}
 static void save(MinecraftServer s){try{Path p=file(s);Files.createDirectories(p.getParent());Path tmp=p.resolveSibling("yggdrasil-routes.json.tmp");Files.writeString(tmp,GSON.toJson(state(s)));try{Files.move(tmp,p,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException ex){Files.move(tmp,p,StandardCopyOption.REPLACE_EXISTING);}}catch(Exception ex){throw new IllegalStateException("Cannot save Yggdrasil routes",ex);}}
 static ResourceKey<Level> key(int i){return switch(i){case 0->Level.OVERWORLD;case 1->Level.NETHER;case 2->Level.END;default->Yggdrasil.realm(NAMES[i]);};}
 static boolean endOpen(ServerPlayer p){return state(p.level().getServer()).endOpen || p.entityTags().contains("yggdrasil_end");}
 static boolean netherOpen(ServerPlayer p){return p.entityTags().contains("yggdrasil_nether");}
 static void unlockEnd(ServerPlayer p){state(p.level().getServer()).endOpen=true;save(p.level().getServer());p.sendSystemMessage(Component.literal("La branche de l’End est éveillée."));}
 static void ensureHub(MinecraftServer s){State st=state(s);if(!st.built){ServerLevel hub=s.getLevel(Yggdrasil.HUB);if(hub==null)throw new IllegalStateException("Yggdrasil dimension missing");WorldBuilder.hub(hub);st.built=true;save(s);}}
 static void renovate(MinecraftServer s){
  ServerLevel l=s.getLevel(Yggdrasil.HUB);if(l==null)return;
  // Explicit administrator command only: no automatic replacement of an existing player's hub.
  for(int x=-53;x<=53;x++)for(int z=-15;z<=275;z++){
   int low=z<164?72:32,high=z<164?101:185;
   if(z<164&&Math.abs(x)>18)continue;
   for(int y=low;y<=high;y++){BlockPos b=new BlockPos(x,y,z);if(!l.getBlockState(b).isAir())l.setBlock(b,Blocks.AIR.defaultBlockState(),2);}
  }
  WorldBuilder.hub(l);state(s).routes.remove("built:commissions");CampaignWorld.hub(l);state(s).built=true;save(s);
 }
 static void bindEntrance(ServerPlayer p,BlockPos pos){var s=p.level().getServer();String dimension=p.level().dimension().identifier().toString();var route=new Route(dimension,pos);state(s).routes.put("gate:"+p.getUUID()+":"+dimension,route);state(s).routes.putIfAbsent("entrance:"+dimension,route);save(s);}
 static void enter(ServerPlayer p){if(p.removeTag("ygg_return_music"))Network.scene(p,4);MinecraftServer s=p.level().getServer();ensureHub(s);if(!p.level().dimension().equals(Yggdrasil.HUB)){String dimension=p.level().dimension().identifier().toString();state(s).routes.put("player:"+p.getUUID(),new Route(dimension,p.blockPosition()));BlockPos at=p.blockPosition();BlockPos anchor=null;for(int dy=-5;dy<=1;dy++)for(int dx=-3;dx<=3;dx++)for(int dz=-2;dz<=2;dz++){var test=at.offset(dx,dy,dz);if(p.level().getBlockState(test).is(Yggdrasil.PORTAL.get()))anchor=test;}if(anchor!=null)bindEntrance(p,anchor);String entry="entrance:"+dimension;state(s).routes.putIfAbsent(entry,new Route(dimension,anchor==null?p.blockPosition():anchor));save(s);}CampaignWorld.hub(s.getLevel(Yggdrasil.HUB));teleport(p,s.getLevel(Yggdrasil.HUB),new BlockPos(0,82,4));}
 static boolean safe(ServerLevel l,BlockPos p){return l.getBlockState(p.below()).isSolidRender()&&l.getBlockState(p).isAir()&&l.getBlockState(p.above()).isAir()&&l.getFluidState(p.below()).isEmpty()&&!l.getBlockState(p.below()).is(Blocks.MAGMA_BLOCK);}
 static BlockPos findSafe(ServerLevel l,int sx,int sz){
  for(int r=0;r<64;r+=4)for(int dx=-r;dx<=r;dx+=4)for(int dz=-r;dz<=r;dz+=4){int x=sx+dx,z=sz+dz;l.getChunk(x>>4,z>>4);int top=l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);if(l.dimension().equals(Level.NETHER)){for(int y=110;y>25;y--)if(safe(l,new BlockPos(x,y,z)))return new BlockPos(x,y,z);}else if(safe(l,new BlockPos(x,top,z)))return new BlockPos(x,top,z);}
  return null;
 }
 static BlockPos netherGateSite(ServerLevel l,int sx,int sz){
  for(int radius=0;radius<=96;radius+=8)for(int dx=-radius;dx<=radius;dx+=8)for(int dz=-radius;dz<=radius;dz+=8){
   if(radius>0&&Math.abs(dx)!=radius&&Math.abs(dz)!=radius)continue;int x=sx+dx,z=sz+dz;l.getChunk(x>>4,z>>4);
   for(int y=112;y>=34;y--){BlockPos b=new BlockPos(x,y,z);if(safe(l,b)&&WorldBuilder.clearGateSpace(l,b))return b;}
  }
  // Last resort: a small chamber carved only in natural dry Nether rock, never through inventories or bedrock.
  for(int offset=0;offset<=96;offset+=12){BlockPos b=new BlockPos(sx+offset,106,sz);boolean natural=true;
   for(int x=-4;x<=4&&natural;x++)for(int y=0;y<=6&&natural;y++)for(int z=-3;z<=3;z++){var at=b.offset(x,y,z);var state=l.getBlockState(at);if(!l.getFluidState(at).isEmpty()||!(state.isAir()||state.is(Blocks.NETHERRACK)||state.is(Blocks.BASALT)||state.is(Blocks.BLACKSTONE))){natural=false;break;}}
   if(natural){WorldBuilder.box(l,b.getX()-4,b.getY(),b.getZ()-3,b.getX()+4,b.getY()+6,b.getZ()+3,Blocks.AIR);WorldBuilder.platform(l,b.below(),4,Blocks.STONE_BRICKS);return b;}
  }
  return null;
 }
 static BlockPos surfaceGateSite(ServerLevel l,int x,int z){int y=65;for(int dx=-4;dx<=4;dx++)for(int dz=-3;dz<=3;dz++){l.getChunk((x+dx)>>4,(z+dz)>>4);y=Math.max(y,l.getHeight(Heightmap.Types.WORLD_SURFACE,x+dx,z+dz));}BlockPos pos=new BlockPos(x,y,z);return y+7<l.getMaxY()&&WorldBuilder.clearGateSpace(l,pos)?pos:null;}
 static void createEntrance(ServerPlayer p){ServerLevel l=(ServerLevel)p.level();BlockPos base=p.blockPosition().offset(0,0,5);if(!WorldBuilder.clearGateSpace(l,base)){p.sendSystemMessage(Component.literal("Il faut un espace libre de 7 × 7 × 5 blocs devant toi."));return;}WorldBuilder.gate(l,base);state(l.getServer()).routes.put("entrance:"+l.dimension().identifier(),new Route(l.dimension().identifier().toString(),base));save(l.getServer());p.sendSystemMessage(Component.literal("Portail d’Yggdrasil créé. Traverse-le pour rejoindre l’Arbre-Monde."));}
 static void select(ServerPlayer p,int index){
  if(index<0||index>=NAMES.length||!p.level().dimension().equals(Yggdrasil.HUB)||p.distanceToSqr(STONE.getX()+.5,STONE.getY()+2,STONE.getZ()+.5)>100)return;
  if(!Campaign.unlocked(p,index)){p.sendSystemMessage(Component.literal("Cette branche n’est pas encore éveillée."));return;}
  MinecraftServer s=p.level().getServer();ServerLevel target=s.getLevel(key(index));if(target==null){p.sendSystemMessage(Component.literal("Royaume indisponible."));return;}
  State st=state(s);String name=NAMES[index];Route route=st.routes.get(name);
  if(index==0){Route own=st.routes.get("gate:"+p.getUUID()+":"+target.dimension().identifier());if(own==null)own=st.routes.get("entrance:"+target.dimension().identifier());if(own!=null)route=own;}
  if(route==null){
   BlockPos arrival;
   if(index>=3){target.getChunk(0,0);arrival=new BlockPos(0,Math.max(66,target.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,0,0)),0);WorldBuilder.realmArrival(target,index,arrival);}else{
    Route origin=st.routes.get("entrance:"+target.dimension().identifier());int x=origin!=null?origin.x:100,z=origin!=null?origin.z:0;
    arrival=index==1?netherGateSite(target,x+12,z+12):findSafe(target,x+12,z+12);
    if(arrival==null&&index==0){target.getChunk((x+12)>>4,(z+12)>>4);int y=Math.max(target.getSeaLevel()+2,target.getHeight(Heightmap.Types.WORLD_SURFACE,x+12,z+12)+1);BlockPos candidate=new BlockPos(x+12,y,z+12);if(WorldBuilder.clearGateSpace(target,candidate))arrival=candidate;}
    if(arrival==null){if(index!=2){p.sendSystemMessage(Component.literal("Aucun point d’arrivée sûr trouvé. Ouvre d’abord un portail depuis ce royaume."));return;}arrival=new BlockPos(100,65,0);WorldBuilder.platform(target,arrival.below(),5,Blocks.OBSIDIAN);}
    // Search for a clear site rather than destroying existing buildings.
    BlockPos site=index==0||index==2?surfaceGateSite(target,arrival.getX(),arrival.getZ()):null;for(int a=0;a<64&&site==null;a++){BlockPos test=a==0?arrival:findSafe(target,arrival.getX()+a*3,arrival.getZ());if(test!=null&&WorldBuilder.clearGateSpace(target,test))site=test;}
    if(site==null){p.sendSystemMessage(Component.literal("Pas assez d’espace pour un portail de retour."));return;}arrival=site;WorldBuilder.platform(target,arrival.below(),4,Blocks.STONE_BRICKS);
   }
   WorldBuilder.gate(target,arrival);route=new Route(target.dimension().identifier().toString(),arrival);st.routes.put(name,route);save(s);
  }
  teleport(p,target,route.pos().offset(0,0,3));CampaignWorld.zone(p,index);
 }
 static void homeOverworld(ServerPlayer p){var s=p.level().getServer();var l=s.overworld();Route r=state(s).routes.get("gate:"+p.getUUID()+":"+Level.OVERWORLD.identifier());if(r==null)r=state(s).routes.get("entrance:"+Level.OVERWORLD.identifier());if(r==null)r=state(s).routes.get("overworld");BlockPos pos=r==null?findSafe(l,100,0):r.pos().offset(0,0,3);if(pos!=null)teleport(p,l,pos);}
 static void returnHome(ServerPlayer p){
  var s=p.level().getServer();Route r=state(s).routes.get("player:"+p.getUUID());
  if(r==null){select(p,0);return;}
  var k=ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,net.minecraft.resources.Identifier.parse(r.dimension));
  var level=s.getLevel(k);if(level!=null){var pos=findSafe(level,r.x,r.z);if(pos!=null)teleport(p,level,pos);}
 }
 static void teleport(ServerPlayer p,ServerLevel l,BlockPos b){if(l==null)return;l.getChunk(b);COOLDOWN.put(p.getUUID(),l.getServer().getTickCount()+100L);p.teleportTo(l,b.getX()+.5,b.getY(),b.getZ()+.5,Set.of(),180,0,true);p.fallDistance=0;}
 static void tick(ServerPlayer p){
  ServerLevel l=(ServerLevel)p.level();if(l.dimension().equals(Level.NETHER))p.addTag("yggdrasil_nether");if(l.dimension().equals(Level.END))p.addTag("yggdrasil_end");
  if(l.dimension().equals(Yggdrasil.HUB)&&p.getY()<30){teleport(p,l,new BlockPos(0,82,4));return;}
  if(l.getServer().getTickCount()%5!=0||COOLDOWN.getOrDefault(p.getUUID(),0L)>l.getServer().getTickCount())return;
  if(l.dimension().equals(Yggdrasil.HUB)){
   if(l.getServer().getTickCount()%10==0)l.sendParticles(net.minecraft.core.particles.ParticleTypes.GLOW,p.getX(),p.getY()+2,p.getZ(),4,6,1.5,6,.01);
   if(p.distanceToSqr(.5,82,.5)<2){homeOverworld(p);}return;
  }
  BlockPos at=p.blockPosition();for(int y=-4;y<=0;y++)for(int x=-2;x<=2;x++){BlockPos b=at.offset(x,y,0);if(l.getBlockState(b).is(Yggdrasil.PORTAL.get())&&Math.abs(p.getZ()-(b.getZ()+.5))<.7&&Math.abs(p.getX()-(b.getX()+.5))<1.2){enter(p);return;}}
 }
}
