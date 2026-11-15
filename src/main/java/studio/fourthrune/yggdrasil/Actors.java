package studio.fourthrune.yggdrasil;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

final class Actors {
 static final DeferredRegister<EntityType<?>> TYPES=DeferredRegister.create(Registries.ENTITY_TYPE,Yggdrasil.ID);
 static final DeferredHolder<EntityType<?>,EntityType<RealmBoss>> BOSS=TYPES.register("realm_guardian",()->EntityType.Builder.of(RealmBoss::new,MobCategory.MONSTER).notInPeaceful().sized(.9f,2.6f).fireImmune().clientTrackingRange(12).build(ResourceKey( "realm_guardian")));
 static final String[] BOSS_IDS={"hrymr","eldsvorn","skald","nastrand","varnott"};
 static final java.util.List<DeferredHolder<EntityType<?>,EntityType<RealmBoss>>> GUARDIANS=java.util.Arrays.stream(BOSS_IDS).map(name->TYPES.register(name,()->EntityType.Builder.of(RealmBoss::new,MobCategory.MONSTER).notInPeaceful().sized(1.2f,3f).fireImmune().clientTrackingRange(16).build(ResourceKey(name)))).toList();
 static final DeferredHolder<EntityType<?>,EntityType<RealmSkeleton>> FROST_WATCHER=TYPES.register("frost_watcher",()->EntityType.Builder.of(RealmSkeleton::new,MobCategory.MONSTER).notInPeaceful().sized(.6f,1.99f).clientTrackingRange(8).build(ResourceKey("frost_watcher")));
 static final DeferredHolder<EntityType<?>,EntityType<SulfurWanderer>> SULFUR_WANDERER=TYPES.register("sulfur_wanderer",()->EntityType.Builder.of(SulfurWanderer::new,MobCategory.MONSTER).notInPeaceful().sized(.6f,1.99f).fireImmune().clientTrackingRange(8).build(ResourceKey("sulfur_wanderer")));
 static class RealmSkeleton extends Skeleton {RealmSkeleton(EntityType<? extends Skeleton> t,Level l){super(t,l);}@Override public void aiStep(){super.aiStep();setRemainingFireTicks(0);}}
 static class SulfurWanderer extends Zombie {SulfurWanderer(EntityType<? extends Zombie> t,Level l){super(t,l);}@Override protected boolean isSunSensitive(){return false;}}
 static final DeferredHolder<EntityType<?>,EntityType<Drake>> DRAKE=TYPES.register("runic_drake",()->EntityType.Builder.of(Drake::new,MobCategory.CREATURE).sized(1.8f,1.4f).fireImmune().clientTrackingRange(12).build(ResourceKey("runic_drake")));
 static final DeferredHolder<EntityType<?>,EntityType<Skeleton>> GHOST_SKELETON=TYPES.register("spectral_skeleton",()->EntityType.Builder.of(Skeleton::new,MobCategory.MONSTER).notInPeaceful().sized(.6f,1.99f).fireImmune().clientTrackingRange(8).build(ResourceKey("spectral_skeleton")));
 static final DeferredHolder<EntityType<?>,EntityType<Vex>> SPECTER=TYPES.register("lost_specter",()->EntityType.Builder.of(Vex::new,MobCategory.MONSTER).notInPeaceful().sized(.7f,1.2f).fireImmune().clientTrackingRange(8).build(ResourceKey("lost_specter")));
 static net.minecraft.resources.ResourceKey<EntityType<?>> ResourceKey(String n){return net.minecraft.resources.ResourceKey.create(Registries.ENTITY_TYPE,Yggdrasil.id(n));}
 static void register(IEventBus bus){TYPES.register(bus);bus.addListener(Actors::attributes);}
 static void attributes(EntityAttributeCreationEvent e){e.put(FROST_WATCHER.get(),Skeleton.createAttributes().add(Attributes.MAX_HEALTH,30).build());e.put(SULFUR_WANDERER.get(),Zombie.createAttributes().add(Attributes.MAX_HEALTH,38).build());for(var type:GUARDIANS)e.put(type.get(),Zombie.createAttributes().add(Attributes.MAX_HEALTH,300).add(Attributes.ATTACK_DAMAGE,9).add(Attributes.FOLLOW_RANGE,100).add(Attributes.MOVEMENT_SPEED,.28).add(Attributes.KNOCKBACK_RESISTANCE,.9).build());e.put(BOSS.get(),Zombie.createAttributes().add(Attributes.MAX_HEALTH,300).add(Attributes.ATTACK_DAMAGE,9).add(Attributes.FOLLOW_RANGE,100).add(Attributes.MOVEMENT_SPEED,.28).add(Attributes.KNOCKBACK_RESISTANCE,.9).build());e.put(DRAKE.get(),Mob.createMobAttributes().add(Attributes.ATTACK_DAMAGE,4).add(Attributes.FLYING_SPEED,.6).add(Attributes.FOLLOW_RANGE,64).add(Attributes.MAX_HEALTH,100).add(Attributes.MOVEMENT_SPEED,.35).build());e.put(GHOST_SKELETON.get(),Skeleton.createAttributes().add(Attributes.MAX_HEALTH,40).build());e.put(SPECTER.get(),Vex.createAttributes().add(Attributes.MAX_HEALTH,32).build());}
 static boolean summonBoss(ServerPlayer p,BlockPos pos,int stage){var l=(ServerLevel)p.level();if(l.getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL){Campaign.say(p,"Choisis une difficulté non paisible pour appeler un gardien.");return false;}if(!l.getEntitiesOfClass(RealmBoss.class,new AABB(pos).inflate(180)).isEmpty()){Campaign.say(p,"Un gardien est déjà éveillé dans cette arène.");return false;}RealmBoss boss=GUARDIANS.get(stage).get().create(l,EntitySpawnReason.TRIGGERED);if(boss==null)return false;boss.configure(stage);boss.setPos(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5);l.addFreshEntity(boss);for(var player:l.players())if(player.distanceToSqr(boss)<200*200)Network.scene(player,2);return true;}
 static Mob spawnSpecter(ServerPlayer p,int stage){var l=(ServerLevel)p.level();if(stage<0||stage>3||l.getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL)return null;
  for(int attempt=0;attempt<12;attempt++){double a=p.getRandom().nextDouble()*Math.PI*2;int x=(int)Math.floor(p.getX()+Math.cos(a)*24),z=(int)Math.floor(p.getZ()+Math.sin(a)*24);l.getChunk(x>>4,z>>4);int y=l.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);BlockPos pos=new BlockPos(x,y,z);
   if(stage>=2&&protectedAt(l,pos,stage))continue;
   Mob mob=stage==0?FROST_WATCHER.get().create(l,EntitySpawnReason.EVENT):stage==1?SULFUR_WANDERER.get().create(l,EntitySpawnReason.EVENT):p.getRandom().nextBoolean()?GHOST_SKELETON.get().create(l,EntitySpawnReason.EVENT):SPECTER.get().create(l,EntitySpawnReason.EVENT);if(mob==null)return null;mob.setPos(x+.5,y,z+.5);if(!l.noCollision(mob)||l.getBlockState(pos.below()).isAir()||(!(mob instanceof Vex)&&!l.getFluidState(pos.below()).isEmpty()))continue;
   if(!p.isCreative())mob.setTarget(p);if(mob instanceof Skeleton)mob.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(net.minecraft.world.item.Items.BOW));mob.addTag("ygg_realm_creature");mob.setCustomName(Component.literal(stage==0?"Veilleur du Givre":stage==1?"Errant des Soufres":mob instanceof Vex?"Spectre des marées oubliées":"Squelette des serments brisés"));return l.addFreshEntity(mob)?mob:null;
  }
  return null;
 }
 static boolean protectedAt(ServerLevel l,BlockPos pos,int stage){if(stage==2&&Campaign.light(l,pos,false)>=8||stage==3&&Campaign.light(l,pos,true)>0)return true;for(var p:l.players())if(p.distanceToSqr(Vec3.atCenterOf(pos))<100&&(stage==2?Campaign.enchantLevel(p,"oathlight")>0:Campaign.held(p,Content.block("spectral_lantern").asItem())))return true;return false;}
 static void retreat(Mob mob,ServerLevel l,int stage){mob.setTarget(null);Vec3 best=null;double score=-1;for(int i=0;i<16;i++){double a=i*Math.PI/8;BlockPos b=mob.blockPosition().offset((int)(Math.cos(a)*12),0,(int)(Math.sin(a)*12));b=new BlockPos(b.getX(),l.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,b.getX(),b.getZ()),b.getZ());if(!protectedAt(l,b,stage)){double value=b.distSqr(mob.blockPosition());if(value>score){score=value;best=Vec3.atBottomCenterOf(b);}}}if(best!=null){mob.getNavigation().moveTo(best.x,best.y,best.z,1.5);Vec3 direction=best.subtract(mob.position()).normalize();mob.setDeltaMovement(direction.scale(.35));}}
 static void summonDrake(ServerPlayer p){
  if(!p.entityTags().contains("ygg_builder_reward")){Campaign.say(p,"Ce lien draconique attend la reconnaissance des trois pavillons.");return;}
  var server=p.level().getServer();String key="drake:"+p.getUUID();var st=Travel.state(server);Drake found=null;
  var last=st.routes.get(key);if(last!=null){var level=server.getLevel(net.minecraft.resources.ResourceKey.create(Registries.DIMENSION,net.minecraft.resources.Identifier.parse(last.dimension)));if(level!=null)level.getChunk(last.x>>4,last.z>>4);}
  for(var level:server.getAllLevels())for(Entity e:level.getAllEntities())if(e instanceof Drake d&&p.getUUID().toString().equals(d.owner)){found=d;break;}
  if(found==null&&last!=null){Campaign.say(p,"Ton drake se réveille dans son dernier refuge. Réutilise le lien dans quelques secondes.");return;}
  if(found==null){found=DRAKE.get().create((ServerLevel)p.level(),EntitySpawnReason.TRIGGERED);if(found==null)return;found.owner=p.getUUID().toString();found.setCustomName(Component.literal("Drake de "+p.getName().getString()));found.setPersistenceRequired();found.setPos(p.getX()+3,p.getY()+1,p.getZ());((ServerLevel)p.level()).addFreshEntity(found);}else found.teleportTo((ServerLevel)p.level(),p.getX()+3,p.getY()+1,p.getZ(),Set.of(),p.getYRot(),0,true);
  found.waiting=false;st.routes.put(key,new Travel.Route(p.level().dimension().identifier().toString(),found.blockPosition()));Travel.save(server);Campaign.say(p,"Ton drake t’a rejoint. Clic droit : monter. Accroupi + clic droit : attendre / suivre. En vol : avancer et regarder pour diriger, Espace pour monter, accroupi pour descendre de selle.");
 }
 static class RealmBoss extends Zombie {
  static final EntityDataAccessor<Integer> VARIANT=SynchedEntityData.defineId(RealmBoss.class,EntityDataSerializers.INT);
  final ServerBossEvent bar=new ServerBossEvent(java.util.UUID.randomUUID(),Component.literal("Gardien des Royaumes"),BossEvent.BossBarColor.BLUE,BossEvent.BossBarOverlay.PROGRESS);
  static final String[] NAMES={"Hrymr, Jarl du Givre","Eldsvorn, Gardien des Soufres","Skald le Parjure","Nástrand, Passeur des Morts","Várnótt, Souverain de la Dernière Nuit"};
  Vec3 warning;int impactAt;
  RealmBoss(EntityType<? extends Zombie> type,Level l){super(type,l);setPersistenceRequired();}
  @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(VARIANT,0);}
  int stage(){return entityData.get(VARIANT);}
  void configure(int stage){setNoAi(true);entityData.set(VARIANT,stage);getAttribute(Attributes.MAX_HEALTH).setBaseValue(stage==4?1600:300+stage*200);setHealth(getMaxHealth());getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(9+stage*4);getAttribute(Attributes.SCALE).setBaseValue(stage==4?3.2:2.2);setCustomName(Component.literal(NAMES[stage]));setCustomNameVisible(true);bar.setName(getCustomName());bar.setColor(stage==0?BossEvent.BossBarColor.BLUE:stage==1?BossEvent.BossBarColor.YELLOW:BossEvent.BossBarColor.PURPLE);}
  @Override protected boolean isSunSensitive(){return false;}
  @Override protected void addAdditionalSaveData(ValueOutput o){super.addAdditionalSaveData(o);o.putInt("ygg_stage",stage());}
  @Override protected void readAdditionalSaveData(ValueInput i){super.readAdditionalSaveData(i);int stage=i.getIntOr("ygg_stage",0);entityData.set(VARIANT,Math.max(0,Math.min(4,stage)));bar.setName(Component.literal(NAMES[stage()]));}
  @Override public void tick(){super.tick();if(!(level() instanceof ServerLevel l)||!isAlive())return;bar.setProgress(Math.max(0,Math.min(1,getHealth()/getMaxHealth())));
   if(tickCount%20==0){for(var p:l.players()){if(p.distanceToSqr(this)<180*180)bar.addPlayer(p);else bar.removePlayer(p);}}
   if(tickCount<60){setTarget(null);l.sendParticles(ParticleTypes.REVERSE_PORTAL,getX(),getY()+2,getZ(),12,2,2,2,.05);return;}
   ServerPlayer target=l.players().stream().filter(p->!p.isSpectator()&&!p.isCreative()&&p.distanceToSqr(this)<150*150).min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);if(target==null)return;
   setNoAi(false);setTarget(target);int interval=getHealth()<getMaxHealth()/2?80:130;
   if(tickCount%interval==0){warning=target.position();impactAt=tickCount+30;for(int k=0;k<30;k++){double a=k*Math.PI/15;l.sendParticles(stage()==0?ParticleTypes.SNOWFLAKE:stage()==1?ParticleTypes.FLAME:ParticleTypes.SOUL_FIRE_FLAME,warning.x+Math.cos(a)*5,warning.y+.2,warning.z+Math.sin(a)*5,1,0,0,0,0);}for(var p:l.players())if(p.distanceToSqr(this)<150*150)p.sendSystemMessage(Component.literal(stage()==0?"La glace se fissure sous tes pieds…":stage()==1?"Une vapeur brûlante s’accumule…":"Les runes annoncent une onde — écarte-toi !"),true);}
   if(warning!=null&&tickCount==impactAt){l.sendParticles(stage()==0?ParticleTypes.SNOWFLAKE:ParticleTypes.SOUL_FIRE_FLAME,warning.x,warning.y+1,warning.z,100,5,2,5,.1);for(var p:l.players())if(p.position().distanceToSqr(warning)<30&&!p.isCreative()&&!p.isSpectator()){p.hurtServer(l,damageSources().mobAttack(this),10+stage()*4);p.addEffect(new MobEffectInstance(stage()==0?MobEffects.SLOWNESS:stage()==1?MobEffects.POISON:MobEffects.DARKNESS,100,stage()==0?2:0));}warning=null;}
   if(stage()==4&&tickCount%180==0){for(int k=0;k<48;k++){double a=k*Math.PI/24;l.sendParticles(ParticleTypes.END_ROD,getX()+Math.cos(a)*16,getY()+.4,getZ()+Math.sin(a)*16,2,0,.5,0,.02);}for(var p:l.players())if(p.distanceToSqr(this)<36&&!p.isCreative()){p.hurtServer(l,damageSources().mobAttack(this),12);p.push(p.getX()-getX(),.5,p.getZ()-getZ());}}
   if(getHealth()<getMaxHealth()*.5&&tickCount%240==0){for(int j=0;j<2;j++){Vex vex=SPECTER.get().create(l,EntitySpawnReason.EVENT);if(vex!=null){vex.setPos(getX()+j*3,getY()+2,getZ());vex.setLimitedLife(300);vex.setTarget(target);l.addFreshEntity(vex);}}}
  }
  @Override public void die(DamageSource source){if(level() instanceof ServerLevel l){bar.removeAllPlayers();int stage=stage();if(stage<4){boolean rewarded=false;for(var p:l.players())if(!p.isSpectator()&&p.distanceToSqr(this)<200*200){Campaign.give(p,new ItemStack(Content.get(SOULS(stage))));Network.scene(p,4);rewarded=true;}if(!rewarded)spawnAtLocation(l,new ItemStack(Content.get(SOULS(stage))));}else for(var p:new ArrayList<>(l.players()))if(p.distanceToSqr(this)<200*200&&!p.isSpectator()){if(p.addTag("ygg_final_victory")){Campaign.give(p,new ItemStack(Content.get("world_relic")));String[] rewards={"valkyrie_blade","abyssal_blade","storm_hammer"};Campaign.give(p,new ItemStack(Content.get(rewards[p.getRandom().nextInt(3)])));}CampaignWorld.returnDungeon(p);p.addTag("ygg_return_music");Network.scene(p,3);Campaign.say(p,"La Dernière Nuit s’achève. Rapporte la relique à la waystone d’Yggdrasil.");}}super.die(source);}
  static String SOULS(int i){return Campaign.SOULS[i];}
 }
 static class Drake extends Phantom {
  String owner="";boolean waiting;float forward,strafe;boolean up;int controlAge;
  Drake(EntityType<? extends Phantom> t,Level l){super(t,l);this.moveControl=new net.minecraft.world.entity.ai.control.MoveControl(this);setPersistenceRequired();}
  @Override protected void registerGoals(){}
  @Override public boolean removeWhenFarAway(double distance){return false;}
  @Override public boolean isClientAuthoritative(){return false;}
  // Flight is server-authoritative; the client sends bounded input, never vehicle positions.
  @Override public LivingEntity getControllingPassenger(){if(level().isClientSide())return null;return getFirstPassenger() instanceof Player p&&p.getUUID().toString().equals(owner)?p:null;}
  @Override public boolean causeFallDamage(double d,float m,DamageSource s){return false;}
  @Override protected InteractionResult mobInteract(Player p,InteractionHand hand){if(!p.getUUID().toString().equals(owner))return InteractionResult.PASS;if(!level().isClientSide()){if(p.isShiftKeyDown()){waiting=!waiting;if(p instanceof ServerPlayer sp)Campaign.say(sp,waiting?"Le drake attendra ici.":"Le drake te suit.");}else p.startRiding(this,true,true);}return InteractionResult.SUCCESS;}
  void control(ServerPlayer p,float forward,float strafe,boolean up){if(getControllingPassenger()!=p)return;this.forward=Math.max(-1,Math.min(1,forward));this.strafe=Math.max(-1,Math.min(1,strafe));this.up=up;controlAge=0;}
  @Override public void tick(){setDeltaMovement(Vec3.ZERO);super.tick();setNoGravity(true);if(!(level() instanceof ServerLevel l))return;
   Vec3 movement=Vec3.ZERO;var rider=getControllingPassenger();if(rider instanceof ServerPlayer p){setYRot(p.getYRot());yBodyRot=p.getYRot();setYHeadRot(p.getYRot());setXRot(p.getXRot()*.4f);if(controlAge++<10){Vec3 look=p.getLookAngle();double a=Math.toRadians(p.getYRot());movement=look.scale(forward*.85).add(Math.cos(a)*strafe*.5,up?.4:0,Math.sin(a)*strafe*.5);}}
   else if(!waiting&&!owner.isEmpty()){Player p=l.getServer().getPlayerList().getPlayer(UUID.fromString(owner));if(p!=null&&p.level()!=l){teleportTo((ServerLevel)p.level(),p.getX()+3,p.getY()+2,p.getZ(),Set.of(),p.getYRot(),0,true);return;}if(p!=null){Vec3 delta=p.position().add(3,2,3).subtract(position());if(delta.length()>5)movement=delta.normalize().scale(Math.min(.65,delta.length()*.025));}}
   if(movement.lengthSqr()>0)move(MoverType.SELF,movement);setDeltaMovement(Vec3.ZERO);setRemainingFireTicks(0);
   if(tickCount%100==0&&!owner.isEmpty()){var st=Travel.state(l.getServer());st.routes.put("drake:"+owner,new Travel.Route(l.dimension().identifier().toString(),blockPosition()));if(tickCount%600==0)Travel.save(l.getServer());}
  }
  @Override protected void addAdditionalSaveData(ValueOutput o){super.addAdditionalSaveData(o);o.putString("owner",owner);o.putBoolean("waiting",waiting);}
  @Override protected void readAdditionalSaveData(ValueInput i){super.readAdditionalSaveData(i);owner=i.getStringOr("owner","");waiting=i.getBooleanOr("waiting",false);}
  @Override public void die(DamageSource s){if(level() instanceof ServerLevel l&&!owner.isEmpty()){Travel.state(l.getServer()).routes.remove("drake:"+owner);Travel.save(l.getServer());}super.die(s);}
 }
}
