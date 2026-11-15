package studio.fourthrune.yggdrasil;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.*;
import net.minecraft.core.particles.*;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

final class Campaign {
 static final String[] ZONES={"hrimdalr","solfara","skaldheim","draugavik","sanctum"};
 static final String[] SOULS={"mana_soul_solfara","mana_soul_skaldheim","mana_soul_draugavik","mana_soul_sanctum"};
 static final String[][] ENCHANTS={{"hearthbound","rime_edge"},{"sulfur_ward","ember_edge"},{"oathlight","veilbreaker"},{"soul_anchor","soul_harvest"}};
 private static final Map<UUID,Finale> FINALES=new HashMap<>();
 private static final Map<UUID,Long> WEAPON_CD=new HashMap<>();
 record Finale(long start,GameType previous){}
 static void register(){NeoForge.EVENT_BUS.addListener(Campaign::login);NeoForge.EVENT_BUS.addListener(Campaign::damage);NeoForge.EVENT_BUS.addListener(Campaign::weather);NeoForge.EVENT_BUS.addListener(Campaign::specterTick);NeoForge.EVENT_BUS.addListener(Campaign::killed);NeoForge.EVENT_BUS.addListener(Campaign::entityLeave);NeoForge.EVENT_BUS.addListener(Campaign::stopping);}
 static int stage(Level l){if(!l.dimension().identifier().getNamespace().equals(Yggdrasil.ID))return -1;return Arrays.asList(ZONES).indexOf(l.dimension().identifier().getPath());}
 static int mask(ServerPlayer p){int m=1|1<<4;if(Travel.netherOpen(p))m|=1<<1;if(Travel.endOpen(p))m|=1<<2;if(p.entityTags().contains("ygg_unlock_solfara"))m|=1<<3;if(p.entityTags().contains("ygg_unlock_skaldheim"))m|=1<<6;if(p.entityTags().contains("ygg_unlock_draugavik"))m|=1<<5;return m;}
 static boolean unlocked(ServerPlayer p,int index){return index>=0&&index<7&&(mask(p)&1<<index)!=0;}
 static void give(ServerPlayer p,ItemStack item){if(!p.getInventory().add(item))p.spawnAtLocation((ServerLevel)p.level(),item);}
 static void say(ServerPlayer p,String text){p.sendSystemMessage(Component.literal(text));}
 static boolean waystone(ServerPlayer p){
  ItemStack held=p.getMainHandItem();
  for(int i=0;i<4;i++)if(held.is(Content.get(SOULS[i]))){
   String destination=ZONES[i+1],tag="ygg_unlock_"+destination;
   if(p.entityTags().contains(tag)){say(p,"Cette âme a déjà éveillé sa branche.");return true;}
   if(i>0&&!p.entityTags().contains("ygg_unlock_"+ZONES[i])){say(p,"La branche précédente doit être éveillée d’abord.");return true;}
   p.addTag(tag);if(!p.isCreative())held.shrink(1);
   var l=(ServerLevel)p.level();for(int k=0;k<32;k++){double a=k*Math.PI/16;l.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,Travel.STONE.getX()+.5+Math.cos(a)*2,Travel.STONE.getY()+1+k*.1,Travel.STONE.getZ()+.5+Math.sin(a)*2,1,0,0,0,.01);}
   say(p,"L’âme rejoint l’Arbre-Monde. La branche de "+destination+" est éveillée.");Network.open(p);return true;
  }
  if(held.is(Content.get("world_relic"))){
   if(!p.entityTags().contains("ygg_final_victory")){say(p,"La relique attend que tu affrontes le souverain du sanctuaire.");return true;}
   if(p.entityTags().contains("ygg_story_finished")){say(p,"Cette aventure est déjà inscrite dans les racines.");return true;}
   if(!p.isCreative())held.shrink(1);startFinale(p);return true;
  }
  return false;
 }
 static void repair(ServerPlayer p,BlockPos pos){
  var l=(ServerLevel)p.level();var st=Travel.state(l.getServer());if(st.repairs==null)st.repairs=new HashMap<>();String key=l.dimension().identifier()+":"+pos.asLong();int repaired=st.repairs.getOrDefault(key,0);ItemStack held=p.getMainHandItem();
  if(repaired<4&&!p.isCreative()){
   if(!held.is(Content.get("engraved_stone"))){say(p,"Arche brisée : apporte quatre pierres gravées, puis un Cœur des Racines.");return;}
   held.shrink(1);l.playSound(null,pos,net.minecraft.sounds.SoundEvents.RESPAWN_ANCHOR_CHARGE,net.minecraft.sounds.SoundSource.BLOCKS,.7f,.75f+repaired*.1f);st.repairs.put(key,++repaired);Travel.save(l.getServer());l.sendParticles(ParticleTypes.ENCHANT,pos.getX()+.5,pos.getY()+3,pos.getZ()+.5,25,2,2,.3,.3);say(p,"Pierre gravée restaurée : "+repaired+" / 4");return;
  }
  if(!p.isCreative()&&!held.is(Content.get("root_heart"))){say(p,"Place un Cœur des Racines pour éveiller le portail.");return;}
  if(!p.isCreative())held.shrink(1);l.setBlock(pos,Yggdrasil.PORTAL.get().defaultBlockState(),3);l.playSound(null,pos,net.minecraft.sounds.SoundEvents.PORTAL_TRIGGER,net.minecraft.sounds.SoundSource.BLOCKS,.5f,.8f);Travel.bindEntrance(p,pos);say(p,"Les racines répondent. Le passage vers Yggdrasil est ouvert.");
 }
 static void interact(ServerPlayer p,BlockPos pos,String name){
  var l=(ServerLevel)p.level();ItemStack held=p.getMainHandItem();int tier=-1;for(int i=0;i<4;i++)if(name.equals(Content.TIERS[i]+"_table"))tier=i;
  if(tier>=0){enchant(p,tier);return;}
  switch(name){
   case "distiller"->{
    if(!held.is(Items.WATER_BUCKET)){say(p,"Distillateur : seau d’eau acide + four allumé juste à côté.");return;}
    boolean lit=false;for(Direction d:Direction.values()){var b=l.getBlockState(pos.relative(d));if((b.is(Blocks.FURNACE)||b.is(Blocks.SMOKER)||b.is(Blocks.BLAST_FURNACE))&&b.hasProperty(BlockStateProperties.LIT)&&b.getValue(BlockStateProperties.LIT))lit=true;}
    if(!lit){say(p,"Le distillateur doit toucher un four en fonctionnement.");return;}
    if(!p.isCreative())held.shrink(1);give(p,new ItemStack(Items.BUCKET));give(p,new ItemStack(Content.get("heated_flask")));say(p,"Vapeur capturée : utilise un condenseur refroidi pour obtenir une eau sûre.");
   }
   case "condenser"->{
    if(!held.is(Content.get("heated_flask"))){say(p,"Condenseur : flacon chauffé + glace ou eau à côté.");return;}
    boolean cold=false;for(Direction d:Direction.values()){var b=l.getBlockState(pos.relative(d));if(b.is(Blocks.ICE)||b.is(Blocks.PACKED_ICE)||b.is(Blocks.BLUE_ICE)||b.is(Blocks.WATER))cold=true;}
    if(!cold){say(p,"Place de la glace ou de l’eau au contact du condenseur.");return;}
    if(!p.isCreative())held.shrink(1);give(p,new ItemStack(Content.get("purified_flask")));say(p,"Eau distillée et refroidie. Bois-la pour te rafraîchir.");
   }
   case "boss_altar"->{int stage=stage(l);if(stage<0)return;if(!held.is(Content.get("runic_offering"))&&!p.isCreative()){say(p,"Dépose une offrande runique pour appeler le gardien de ce royaume.");return;}if(Actors.summonBoss(p,pos,stage)&&!p.isCreative())held.shrink(1);}
   case "secret_gate"->{if(!p.entityTags().contains("ygg_unlock_sanctum")){say(p,"Le portail du sanctuaire réclame l’âme de Draugavik, déposée à Yggdrasil.");return;}CampaignWorld.secret(p);}
   case "commission_board"->commission(p);
   case "spectral_lantern"->say(p,"La lanterne des passeurs écarte les brumes de Draugavik. Porte-la en main ou place-la près de toi.");
  }
 }
 static int enchantLevel(LivingEntity p,String name){var registry=p.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);var holder=registry.get(ResourceKey.create(Registries.ENCHANTMENT,Yggdrasil.id(name)));if(holder.isEmpty())return 0;int level=0;for(EquipmentSlot slot:new EquipmentSlot[]{EquipmentSlot.MAINHAND,EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET})level=Math.max(level,EnchantmentHelper.getItemEnchantmentLevel(holder.get(),p.getItemBySlot(slot)));return level;}
 static void enchant(ServerPlayer p,int tier){
  if(stage(p.level())!=tier){say(p,"Cette table ne répond que dans son royaume d’origine.");return;}
  var item=p.getMainHandItem();var payment=p.getOffhandItem();
  if(item.isEmpty()||!item.isDamageableItem()){say(p,"Tiens une arme, un outil ou une armure en main principale, et trois lingots du royaume en seconde main.");return;}
  if(!p.isCreative()&&(p.experienceLevel<20||!payment.is(Content.get(Content.TIERS[tier]+"_ingot"))||payment.getCount()<3)){say(p,"Il faut 20 niveaux et trois lingots du royaume en seconde main.");return;}
  String choice=ENCHANTS[tier][item.getItem().toString().contains("sword")||item.getItem().toString().contains("axe")?1:0];
  var holder=p.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,Yggdrasil.id(choice)));int current=EnchantmentHelper.getItemEnchantmentLevel(holder,item);
  if(current>=3){say(p,"Cette rune a déjà atteint son rang maximal.");return;}
  item.enchant(holder,current+1);if(!p.isCreative()){p.giveExperienceLevels(-20);payment.shrink(3);}p.sendSystemMessage(Component.literal("Rune gravée : ").append(Component.translatable("enchantment.yggdrasil."+choice)).append(" "+(current+1)));
 }
 static boolean held(Player p,Item item){return p.getMainHandItem().is(item)||p.getOffhandItem().is(item);}
 static double light(Level l,BlockPos p,boolean specialOnly){
  if(!specialOnly)return l.getBrightness(LightLayer.BLOCK,p);
  for(int x=-8;x<=8;x+=1)for(int y=-4;y<=4;y++)for(int z=-8;z<=8;z++)if(x*x+z*z+y*y<90&&l.getBlockState(p.offset(x,y,z)).is(Content.block("spectral_lantern")))return 15;return 0;
 }
 static boolean clearFog(Player p){int stage=stage(p.level());if(stage==2)return light(p.level(),p.blockPosition(),false)>=8||enchantLevel(p,"oathlight")>0;if(stage==3)return held(p,Content.block("spectral_lantern").asItem())||light(p.level(),p.blockPosition(),true)>=8;return true;}
 static boolean storm(ServerLevel l){return stage(l)==0;}
 static void tick(ServerPlayer p){
  if(FINALES.containsKey(p.getUUID())){tickFinale(p);return;}
  var l=(ServerLevel)p.level();int stage=stage(l);int tick=l.getServer().getTickCount();
  if(stage==4&&p.getY()<45){Travel.teleport(p,l,new BlockPos(0,83,32));return;}
  if(tick%20!=0)return;
  if(stage<0){p.getPersistentData().putFloat("ygg_temperature",37);return;}
  if(stage>3||p.isSpectator())return;
  if(tick%160==0&&l.getEntitiesOfClass(Monster.class,p.getBoundingBox().inflate(48)).size()<12)Actors.spawnSpecter(p,stage);
  if(p.isCreative())return;
  boolean outside=l.canSeeSky(p.blockPosition().above());
  float temperature=p.getPersistentData().getFloatOr("ygg_temperature",37);
  if(stage==0){
   float warmth=0;for(int x=-5;x<=5;x++)for(int y=-2;y<=2;y++)for(int z=-5;z<=5;z++){var b=l.getBlockState(p.blockPosition().offset(x,y,z));if(b.is(Blocks.FIRE)||b.is(Blocks.LAVA)||b.is(Blocks.CAMPFIRE)&&b.getValue(BlockStateProperties.LIT)||b.hasProperty(BlockStateProperties.LIT)&&b.getValue(BlockStateProperties.LIT)&&b.getBlock() instanceof AbstractFurnaceBlock)warmth=1;}
   boolean insulated=armorPieces(p,0)>=3||enchantLevel(p,"hearthbound")>0;
   temperature+=warmth>0?Math.min(.55f,37-temperature):(outside?(storm(l)?-.06f:-.025f):-.012f)*(insulated?.3f:1);
   if(temperature<32){p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,60,0));p.setTicksFrozen(Math.min(p.getTicksRequiredToFreeze()+20,p.getTicksFrozen()+30));}
   if(temperature<28)p.hurtServer(l,p.damageSources().freeze(),2);
   if(storm(l)&&tick%100==0){int dx=p.getRandom().nextInt(17)-8,dz=p.getRandom().nextInt(17)-8;int x=p.blockPosition().getX()+dx,z=p.blockPosition().getZ()+dz;int y=l.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,x,z);BlockPos b=new BlockPos(x,y-1,z);if(l.getBlockState(b).is(Blocks.WATER)&&l.canSeeSky(b))l.setBlock(b,Blocks.ICE.defaultBlockState(),3);}
  }else if(stage==1){
   float protection=armorPieces(p,1)>=3||enchantLevel(p,"sulfur_ward")>0?.4f:1;
   temperature+=(outside?.045f:.018f)*protection;
   if(p.isInWater()||outside&&l.isRaining()){p.hurtServer(l,p.damageSources().magic(),2*protection);p.addEffect(new MobEffectInstance(MobEffects.POISON,60,0));}
   if(temperature>41)p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,60,0));if(temperature>44)p.hurtServer(l,p.damageSources().onFire(),2);
  }else{temperature+=(37-temperature)*.2f;}
  temperature=Math.max(20,Math.min(48,temperature));p.getPersistentData().putFloat("ygg_temperature",temperature);
  if(stage<2)p.sendSystemMessage(Component.literal((stage==0?"❄ Hrimdalr":"☀ Sólfara")+" · "+String.format(java.util.Locale.ROOT,"%.1f",temperature)+" °C"+(stage==0&&storm(l)?" · BLIZZARD":"")),true);

 }
 static void weather(net.neoforged.neoforge.event.tick.LevelTickEvent.Post e){if(e.getLevel() instanceof ServerLevel l){int stage=stage(l);if(stage==0||stage==1){boolean rain=stage==0?storm(l):Math.floorMod(l.getServer().getTickCount(),6000)<1800;l.setRainLevel(rain?1:0);l.setThunderLevel(0);if(l.getServer().getTickCount()%100==0)for(var p:l.players()){p.connection.send(new net.minecraft.network.protocol.game.ClientboundGameEventPacket(net.minecraft.network.protocol.game.ClientboundGameEventPacket.RAIN_LEVEL_CHANGE,rain?1:0));}}}}
 static void specterTick(net.neoforged.neoforge.event.tick.EntityTickEvent.Post e){var entity=e.getEntity();if(!(entity.level() instanceof ServerLevel l)||entity.tickCount%5!=0||entity.getType()!=Actors.SPECTER.get()&&entity.getType()!=Actors.GHOST_SKELETON.get())return;entity.setRemainingFireTicks(0);int stage=stage(l);boolean protectedArea=stage==2&&light(l,entity.blockPosition(),false)>=8||stage==3&&light(l,entity.blockPosition(),true)>0;if((stage==2||stage==3)&&!protectedArea)for(var p:l.players())if(p.distanceToSqr(entity)<100&&(stage==3?held(p,Content.block("spectral_lantern").asItem()):enchantLevel(p,"oathlight")>0)){protectedArea=true;break;}if(protectedArea){l.sendParticles(ParticleTypes.SOUL,entity.getX(),entity.getY()+1,entity.getZ(),15,.5,1,.5,.02);Actors.retreat((net.minecraft.world.entity.Mob)entity,l,stage);}}
 static void killed(net.neoforged.neoforge.event.entity.living.LivingDeathEvent e){if(e.getSource().getEntity() instanceof ServerPlayer hunter&&e.getEntity().entityTags().contains("ygg_realm_creature"))give(hunter,new ItemStack(Content.get("engraved_stone")));if(e.getSource().getEntity() instanceof ServerPlayer p&&e.getEntity() instanceof Monster&&enchantLevel(p,"soul_harvest")>0)p.heal(enchantLevel(p,"soul_harvest"));}
 static int armorPieces(Player p,int tier){int n=0;for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET})if(BuiltInRegistries.ITEM.getKey(p.getItemBySlot(slot).getItem()).getPath().startsWith(Content.TIERS[tier]+"_"))n++;return n;}
 static void testkit(ServerPlayer p){for(String tier:Content.TIERS){give(p,new ItemStack(Content.get(tier+"_ingot"),64));give(p,new ItemStack(Content.get(tier+"_sword")));give(p,new ItemStack(Content.get(tier+"_pickaxe")));}for(String kind:new String[]{"helmet","chestplate","leggings","boots"})give(p,new ItemStack(Content.get("hrimsteel_"+kind)));for(String soul:SOULS)give(p,new ItemStack(Content.get(soul)));give(p,new ItemStack(Content.get("root_heart")));give(p,new ItemStack(Content.get("engraved_stone"),4));give(p,new ItemStack(Content.get("runic_offering"),16));give(p,new ItemStack(Content.get("purified_flask"),8));for(String block:new String[]{"spectral_lantern","distiller","condenser"})give(p,new ItemStack(Content.block(block)));say(p,"Kit de test reçu. Les âmes doivent être déposées dans l’ordre sur la waystone. Pour tester le combat de chaque royaume : /yggdrasil shrine, puis offrande sur l’autel.");}
 static void drink(ServerPlayer p,ItemStack item){if(p.getCooldowns().isOnCooldown(item))return;float t=p.getPersistentData().getFloatOr("ygg_temperature",37);p.getPersistentData().putFloat("ygg_temperature",Math.max(36,t-4));p.removeEffect(MobEffects.POISON);p.getFoodData().eat(0,1);p.getCooldowns().addCooldown(item,40);if(!p.isCreative())item.shrink(1);give(p,new ItemStack(Items.GLASS_BOTTLE));}
 static void damage(LivingIncomingDamageEvent e){if(e.getSource().getEntity() instanceof ServerPlayer attacker){var victim=e.getEntity();if(enchantLevel(attacker,"rime_edge")>0)victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,80,1));if(enchantLevel(attacker,"ember_edge")>0)victim.igniteForSeconds(4);if(enchantLevel(attacker,"veilbreaker")>0&&(victim.getType()==Actors.SPECTER.get()||victim.getType()==Actors.GHOST_SKELETON.get()))e.setAmount(e.getAmount()*1.4f);}if(e.getEntity() instanceof ServerPlayer p&&enchantLevel(p,"soul_anchor")>0&&e.getSource().getEntity()!=null&&e.getSource().getEntity().getType()==Actors.SPECTER.get())e.setAmount(e.getAmount()*.65f);}
 static void weapon(ServerPlayer p,String name){var l=(ServerLevel)p.level();long now=l.getServer().getTickCount();if(now<WEAPON_CD.getOrDefault(p.getUUID(),0L))return;WEAPON_CD.put(p.getUUID(),now+200);for(var mob:l.getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(9),m->m instanceof Monster)){mob.hurtServer(l,p.damageSources().playerAttack(p),name.equals("storm_hammer")?20:15);mob.addEffect(new MobEffectInstance(MobEffects.GLOWING,100,0));if(name.equals("abyssal_blade"))p.heal(2);}l.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,p.getX(),p.getY()+1,p.getZ(),90,5,1,5,.03);}
 static boolean coveredPlot(Level l,int z){for(int dx:new int[]{-3,0,3})for(int dz:new int[]{-3,0,3}){boolean covered=false;for(int y=88;y<=103;y++){var b=l.getBlockState(new BlockPos(84+dx,y,z+dz));if(b.isSolidRender()&&!(b.getBlock() instanceof LeavesBlock)){covered=true;break;}}if(!covered)return false;}return true;}
 static void entityLeave(net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent e){if(e.getEntity() instanceof Actors.Drake d&&e.getLevel() instanceof ServerLevel l&&d.getHealth()>0&&!d.owner.isEmpty()&&d.getRemovalReason()!=null&&d.getRemovalReason().name().startsWith("UNLOADED")){Travel.state(l.getServer()).routes.put("drake:"+d.owner,new Travel.Route(l.dimension().identifier().toString(),d.blockPosition()));Travel.save(l.getServer());}}
 static void stopping(net.neoforged.neoforge.event.server.ServerStoppingEvent e){for(var l:e.getServer().getAllLevels())for(var entity:l.getAllEntities())if(entity instanceof Actors.Drake d&&d.getHealth()>0&&!d.owner.isEmpty())Travel.state(e.getServer()).routes.put("drake:"+d.owner,new Travel.Route(l.dimension().identifier().toString(),d.blockPosition()));Travel.save(e.getServer());}
 static void commission(ServerPlayer p){
  if(!p.level().dimension().equals(Yggdrasil.HUB)){say(p,"Les commandes de construction se valident à Yggdrasil.");return;}
  if(p.entityTags().contains("ygg_builder_reward")){if(p.getInventory().countItem(Content.get("drake_bond"))==0)give(p,new ItemStack(Content.get("drake_bond")));say(p,"Ton lien draconique est déjà accordé. Utilise-le pour rappeler ta monture.");return;}
  // Only count additions beyond the supplied hall, in three visibly marked plots.
  int[] logs=new int[3],walls=new int[3],light=new int[3],beds=new int[3];var l=(ServerLevel)p.level();
  for(int k=0;k<3;k++)for(int x=76;x<=92;x++)for(int z=112+k*27;z<=128+k*27;z++)for(int y=83;y<=96;y++){var b=l.getBlockState(new BlockPos(x,y,z));if(b.is(net.minecraft.tags.BlockTags.LOGS))logs[k]++;if(b.is(net.minecraft.tags.BlockTags.PLANKS)||b.is(Blocks.STONE_BRICKS))walls[k]++;if(b.getLightEmission()>0)light[k]++;if(b.getBlock() instanceof BedBlock)beds[k]++;}
  boolean complete=true;for(int k=0;k<3;k++)if(logs[k]<12||walls[k]<60||light[k]<3||beds[k]<2||!coveredPlot(l,120+k*27))complete=false;
  if(!complete){for(int k=0;k<3;k++){String progress="Pavillon "+(k+1)+" : "+logs[k]+"/12 bûches, "+walls[k]+"/60 planches/pierres, "+light[k]+"/3 lumières, "+beds[k]+"/2 parties de lit, toit="+coveredPlot(l,120+k*27);say(p,progress);if(Boolean.getBoolean("yggdrasil.selftest"))System.out.println(progress);}
   say(p,"Commande facultative : bâtis trois pavillons sur les parcelles à l’est (x 76–92 ; z 112–128, 139–155 et 166–182), avec chacun 12 bûches, 60 blocs de planches/pierre, 3 lumières et un lit complet. Décore-les librement.");return;}
  p.addTag("ygg_builder_reward");give(p,new ItemStack(Content.get("drake_bond")));say(p,"Les trois pavillons sont reconnus. Tu reçois un lien draconique unique.");
 }
 static void startFinale(ServerPlayer p){if(FINALES.containsKey(p.getUUID()))return;var mode=p.gameMode.getGameModeForPlayer();p.addTag("ygg_cinematic_from_"+mode.getName());FINALES.put(p.getUUID(),new Finale(p.level().getServer().getTickCount(),mode));p.setGameMode(GameType.SPECTATOR);Network.scene(p,1);}
 static void tickFinale(ServerPlayer p){var f=FINALES.get(p.getUUID());long elapsed=p.level().getServer().getTickCount()-f.start;if(elapsed>=1600){finish(p);return;}if(elapsed%2!=0)return;double t=Math.min(1,elapsed/1200.0),a=-Math.PI/2+t*Math.PI*1.6;double x=Math.cos(a)*62,z=190+Math.sin(a)*70,y=99+Math.sin(t*Math.PI)*80;float yaw=(float)Math.toDegrees(Math.atan2(x,219-z));float pitch=(float)Math.toDegrees(Math.atan2(y-135,Math.hypot(x,219-z)));p.teleportTo(p.level().getServer().getLevel(Yggdrasil.HUB),x,y,z,Set.of(),yaw,pitch,true);}
 static void finish(ServerPlayer p){var f=FINALES.remove(p.getUUID());GameType mode=f==null?GameType.SURVIVAL:f.previous;for(String tag:new ArrayList<>(p.entityTags()))if(tag.startsWith("ygg_cinematic_from_")){try{mode=GameType.byName(tag.substring("ygg_cinematic_from_".length()));}catch(Exception ignored){}p.removeTag(tag);}p.setGameMode(mode);p.addTag("ygg_story_finished");Travel.homeOverworld(p);Network.scene(p,0);}
 static void feedback(ServerPlayer p,int choice){if(!FINALES.containsKey(p.getUUID()))return;var st=Travel.state(p.level().getServer());if(st.feedback==null)st.feedback=new HashMap<>();st.feedback.put(p.getUUID().toString(),choice);Travel.save(p.level().getServer());p.addTag(choice==0?"ygg_feedback_continue":"ygg_feedback_other");finish(p);}
 static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p&&p.entityTags().stream().anyMatch(t->t.startsWith("ygg_cinematic_from_")))finish(p);}
}
