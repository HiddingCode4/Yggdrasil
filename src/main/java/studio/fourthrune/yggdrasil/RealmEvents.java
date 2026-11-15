package studio.fourthrune.yggdrasil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import java.util.*;

final class RealmEvents {
 private static final Map<ServerLevel,Deque<BlockPos>> QUEUE=new WeakHashMap<>();
 static void chunk(ChunkEvent.Load e){CampaignWorld.overworldChunk(e);if(!e.isNewChunk()||!(e.getLevel() instanceof ServerLevel l))return;String realm=l.dimension().identifier().getPath();if(!l.dimension().identifier().getNamespace().equals(Yggdrasil.ID)||realm.equals("yggdrasil"))return;var c=e.getChunk().getPos();if(Math.floorMod(c.x(),12)==3&&Math.floorMod(c.z(),12)==5)l.getServer().execute(()->QUEUE.computeIfAbsent(l,k->new ArrayDeque<>()).add(new BlockPos(c.getMinBlockX()+8,0,c.getMinBlockZ()+8)));}
 static void levelTick(LevelTickEvent.Post e){if(!(e.getLevel() instanceof ServerLevel l)||l.getServer().getTickCount()%20!=0)return;Deque<BlockPos> q=QUEUE.get(l);if(q==null||q.isEmpty())return;BlockPos p=q.removeFirst();if(!l.hasChunkAt(p))return;int y=l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,p.getX(),p.getZ());landmark(l,p.atY(Math.max(y,64)));}
 static void landmark(ServerLevel l,BlockPos p){int x=p.getX(),y=p.getY(),z=p.getZ();String realm=l.dimension().identifier().getPath();
  WorldBuilder.box(l,x-5,y-1,z-6,x+5,y-1,z+6,realm.equals("hrimdalr")?Blocks.PACKED_ICE:Blocks.MOSSY_STONE_BRICKS);
  if(realm.equals("hrimdalr")){for(int dx:new int[]{-4,4}){WorldBuilder.box(l,x+dx,y,z-2,x+dx,y+12,z+2,Blocks.BLUE_ICE);WorldBuilder.put(l,x+dx,y+1,z-3,Blocks.AMETHYST_BLOCK);}return;}
  if(realm.equals("draugavik")){WorldBuilder.box(l,x-2,y,z-6,x+2,y,z+6,Blocks.DARK_OAK_PLANKS);WorldBuilder.box(l,x-3,y+1,z-4,x-3,y+1,z+4,Blocks.DARK_OAK_STAIRS);WorldBuilder.box(l,x+3,y+1,z-4,x+3,y+1,z+4,Blocks.DARK_OAK_STAIRS);WorldBuilder.box(l,x,y+1,z,x,y+8,z,Blocks.DARK_OAK_FENCE);WorldBuilder.box(l,x+1,y+5,z,x+4,y+8,z,Blocks.WOOL.pick(net.minecraft.world.item.DyeColor.CYAN));WorldBuilder.put(l,x,y+1,z-5,Blocks.SOUL_LANTERN);return;}
  for(int dx:new int[]{-5,5})for(int dz:new int[]{-5,5}){WorldBuilder.box(l,x+dx,y,z+dz,x+dx,y+5,z+dz,Blocks.CHISELED_STONE_BRICKS);WorldBuilder.put(l,x+dx,y+6,z+dz,Yggdrasil.GLOW_WOOD.get());}
  if(realm.equals("skaldheim")){for(int dx:new int[]{-3,0,3})l.setBlock(new BlockPos(x+dx,y,z-4),Blocks.NOTE_BLOCK.defaultBlockState().setValue(NoteBlock.NOTE,dx<0?5:dx==0?9:12),2);}else{WorldBuilder.put(l,x,y,z,Blocks.CHISELED_RED_SANDSTONE);WorldBuilder.put(l,x,y+1,z,Blocks.LANTERN);}
 }
 static void interact(PlayerInteractEvent.RightClickBlock e){if(e.getHand()!=net.minecraft.world.InteractionHand.MAIN_HAND)return;if(!(e.getEntity() instanceof ServerPlayer p)||!(e.getLevel() instanceof ServerLevel l)||!l.dimension().identifier().getNamespace().equals(Yggdrasil.ID))return;String realm=l.dimension().identifier().getPath();BlockPos pos=e.getPos();var block=l.getBlockState(pos);
  if(realm.equals("hrimdalr")&&block.is(Blocks.AMETHYST_BLOCK)){
   e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);String tag="ygg_memory_"+pos.asLong();
   if(p.addTag(tag)){long n=p.entityTags().stream().filter(t->t.startsWith("ygg_memory_")).count();String[] memories={"La glace chante : « L’Arbre ne sépare pas les mondes. Il les retient ensemble. »","Un souvenir apparaît : une voyageuse franchit une porte aux racines d’argent.","Une voix murmure : « Écoute les morts, puis donne ta parole aux vivants. »"};p.sendSystemMessage(Component.literal(memories[(int)Math.min(n-1,2)]));if(n>=3)p.addTag("ygg_ice");}else p.sendSystemMessage(Component.literal("Tu connais déjà le souvenir de ce cristal."));l.sendParticles(ParticleTypes.END_ROD,pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,24,1,1,1,.02);check(p);
  }
  if(realm.equals("draugavik")&&block.is(Blocks.SOUL_LANTERN)){
   e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);p.addTag("ygg_ancestors");p.sendSystemMessage(Component.literal("L’ancêtre répond : « Nous veillons dans les marées. Porte nos souvenirs jusqu’à la halle des chants. »"));l.sendParticles(ParticleTypes.SOUL,pos.getX()+.5,pos.getY()+2,pos.getZ()+.5,16,1,1,1,.01);check(p);
  }
  if(realm.equals("skaldheim")&&block.is(Blocks.NOTE_BLOCK)){
   e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);int note=block.getValue(NoteBlock.NOTE),stage=p.entityTags().contains("ygg_song_2")?2:p.entityTags().contains("ygg_song_1")?1:0;int[] notes={5,9,12};
   l.playSound(null,pos,SoundEvents.NOTE_BLOCK_CHIME.value(),SoundSource.BLOCKS,1,(float)Math.pow(2,(note-12)/12.0));
   if(note==notes[stage]){if(stage<2)p.addTag("ygg_song_"+(stage+1));else{p.removeTag("ygg_song_1");p.removeTag("ygg_song_2");p.addTag("ygg_song");WorldBuilder.box(l,-1,82,8,1,85,8,Blocks.AIR);l.sendParticles(ParticleTypes.END_ROD,pos.getX(),pos.getY()+2,pos.getZ(),45,4,2,4,.03);p.sendSystemMessage(Component.literal("Ton serment résonne. Les pierres de la halle s’écartent."));check(p);}}else{p.removeTag("ygg_song_1");p.removeTag("ygg_song_2");p.sendSystemMessage(Component.literal("Le chant retombe. Reprends les trois notes de la plus grave à la plus aiguë."));}
  }
 }
 static void clonePlayer(net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone e){for(String t:e.getOriginal().entityTags())if(t.startsWith("ygg"))e.getEntity().addTag(t);}
 static void check(ServerPlayer p){}
}
