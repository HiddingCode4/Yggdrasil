package studio.fourthrune.yggdrasil;

import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.*;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

@Mod(value=Yggdrasil.ID,dist=Dist.CLIENT)
public final class ClientFeatures {
 static final ModelLayerLocation BOSS_LAYER=new ModelLayerLocation(Identifier.parse("yggdrasil:nordic_guardian"),"main");
 static final ModelLayerLocation DRAKE_LAYER=new ModelLayerLocation(Identifier.parse("yggdrasil:drake"),"main");
 static SoundInstance playing;static float fog=100;static boolean clear;static int lastLightTick=-100;
 public ClientFeatures(IEventBus bus){bus.addListener(ClientFeatures::renderers);bus.addListener(ClientFeatures::layers);NeoForge.EVENT_BUS.addListener(ClientFeatures::fog);NeoForge.EVENT_BUS.addListener(ClientFeatures::fogColor);NeoForge.EVENT_BUS.addListener(ClientFeatures::tick);NeoForge.EVENT_BUS.addListener(ClientFeatures::skyApparition);}
 static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(Actors.BOSS.get(),BossRenderer::new);for(int i=0;i<5;i++){final int variant=i;e.registerEntityRenderer(Actors.GUARDIANS.get(i).get(),c->new BossRenderer(c,variant));}e.registerEntityRenderer(Actors.FROST_WATCHER.get(),FrostRenderer::new);e.registerEntityRenderer(Actors.SULFUR_WANDERER.get(),SulfurRenderer::new);e.registerEntityRenderer(Actors.DRAKE.get(),DrakeRenderer::new);e.registerEntityRenderer(Actors.GHOST_SKELETON.get(),GhostRenderer::new);e.registerEntityRenderer(Actors.SPECTER.get(),SpecterRenderer::new);}
 static ModelLayerLocation guardianLayer(int i){return new ModelLayerLocation(Yggdrasil.id("guardian_"+i),"main");}
 static void layers(EntityRenderersEvent.RegisterLayerDefinitions e){for(int i=0;i<5;i++){final int v=i;e.registerLayerDefinition(guardianLayer(i),()->guardianMesh(v));}e.registerLayerDefinition(DRAKE_LAYER,DrakeModel::layer);e.registerLayerDefinition(BOSS_LAYER,()->{var mesh=net.minecraft.client.model.HumanoidModel.createMesh(CubeDeformation.NONE,0);var head=mesh.getRoot().getChild("head");head.addOrReplaceChild("horns",CubeListBuilder.create().texOffs(40,0).addBox(-6,-11,-1,2,7,2).texOffs(40,0).addBox(4,-11,-1,2,7,2).texOffs(32,0).addBox(-5,-13,-1,1,3,2).texOffs(32,0).addBox(4,-13,-1,1,3,2),PartPose.ZERO);return LayerDefinition.create(mesh,64,64);});}
 static LayerDefinition guardianMesh(int v){var mesh=net.minecraft.client.model.HumanoidModel.createMesh(CubeDeformation.NONE,0);var root=mesh.getRoot();var head=root.getChild("head");var body=root.getChild("body");
  if(v==0){body.addOrReplaceChild("ice_shoulders",CubeListBuilder.create().texOffs(0,16).addBox(-11,-4,-4,7,8,8).addBox(4,-4,-4,7,8,8),PartPose.ZERO);head.addOrReplaceChild("ice_crown",CubeListBuilder.create().texOffs(0,0).addBox(-5,-15,-4,2,9,2).addBox(3,-15,-4,2,9,2).addBox(-1,-18,0,2,11,2),PartPose.ZERO);}
  if(v==1){body.addOrReplaceChild("furnace",CubeListBuilder.create().texOffs(16,16).addBox(-6,1,-5,12,10,3).addBox(-9,-2,-3,5,7,7).addBox(4,-2,-3,5,7,7),PartPose.ZERO);head.addOrReplaceChild("chimneys",CubeListBuilder.create().texOffs(0,0).addBox(-6,-17,0,3,11,3).addBox(3,-17,0,3,11,3),PartPose.ZERO);}
  if(v==2){body.addOrReplaceChild("mantle",CubeListBuilder.create().texOffs(16,16).addBox(-7,0,2,14,20,2),PartPose.ZERO);head.addOrReplaceChild("antlers",CubeListBuilder.create().texOffs(0,0).addBox(-8,-14,0,2,10,2).addBox(6,-14,0,2,10,2).addBox(-11,-13,0,5,2,2).addBox(6,-13,0,5,2,2),PartPose.ZERO);}
  if(v==3){body.addOrReplaceChild("funeral_robes",CubeListBuilder.create().texOffs(16,16).addBox(-6,7,-3,12,14,6),PartPose.ZERO);head.addOrReplaceChild("hood",CubeListBuilder.create().texOffs(0,0).addBox(-5,-11,0,10,12,5).addBox(-5,-11,-5,10,2,5),PartPose.ZERO);}
  if(v==4){body.addOrReplaceChild("wings",CubeListBuilder.create().texOffs(0,16).addBox(-24,-5,3,20,18,2).addBox(4,-5,3,20,18,2),PartPose.ZERO);head.addOrReplaceChild("crown",CubeListBuilder.create().texOffs(0,0).addBox(-6,-14,-4,2,9,2).addBox(4,-14,-4,2,9,2).addBox(-1,-17,-4,2,10,2),PartPose.ZERO);}
  return LayerDefinition.create(mesh,64,64);
 }
 static void fog(ViewportEvent.RenderFog e){var mc=Minecraft.getInstance();if(mc.player==null||(e.getType()!=FogType.NONE&&e.getType()!=FogType.ATMOSPHERIC))return;int stage=Campaign.stage(mc.level);if(stage<0)return;if(mc.player.tickCount-lastLightTick>10||mc.player.tickCount<lastLightTick){clear=Campaign.clearFog(mc.player);lastLightTick=mc.player.tickCount;}float target=switch(stage){case 0->10;case 1->55;case 2->clear?38:16;case 3->clear?38:18;default->220;};fog+=(target-fog)*.12f;e.setNearPlaneDistance(.5f);e.setFarPlaneDistance(fog);applyFog(e.getFogData(),fog);}
 static void applyFog(net.minecraft.client.renderer.fog.FogData data,float distance){data.environmentalStart=.5f;data.environmentalEnd=distance;data.renderDistanceStart=.5f;data.renderDistanceEnd=distance;data.skyEnd=distance;data.cloudEnd=distance;}
 static void fogColor(ViewportEvent.ComputeFogColor e){var mc=Minecraft.getInstance();if(mc.level==null)return;int stage=Campaign.stage(mc.level);if(stage<0)return;float[] rgb=switch(stage){case 0->new float[]{.64f,.77f,.83f};case 1->new float[]{.72f,.61f,.24f};case 2->new float[]{.25f,.32f,.38f};case 3->new float[]{.055f,.25f,.32f};default->new float[]{.004f,.006f,.016f};};e.setRed(rgb[0]);e.setGreen(rgb[1]);e.setBlue(rgb[2]);}
 static void tick(ClientTickEvent.Post e){var mc=Minecraft.getInstance();var p=mc.player;if(p==null||mc.level==null)return;int stage=Campaign.stage(mc.level);
  if(p.getVehicle() instanceof Actors.Drake&&p.tickCount%2==0)ClientPacketDistributor.sendToServer(new Network.DrakeControl((mc.options.keyUp.isDown()?1:0)-(mc.options.keyDown.isDown()?1:0),(mc.options.keyRight.isDown()?1:0)-(mc.options.keyLeft.isDown()?1:0),mc.options.keyJump.isDown()));
  if(stage==0){for(int i=0;i<32;i++){double x=p.getX()+p.getRandom().nextGaussian()*7,z=p.getZ()+p.getRandom().nextGaussian()*7,y=p.getY()+p.getRandom().nextDouble()*8;if(mc.level.canSeeSky(new net.minecraft.core.BlockPos((int)Math.floor(x),(int)Math.floor(y),(int)Math.floor(z))))mc.level.addAlwaysVisibleParticle(ParticleTypes.SNOWFLAKE,true,x,y,z,-.55,-.12,.08);}}
  if(stage==3&&p.tickCount%3==0){for(int i=0;i<4;i++){double x=p.getX()+p.getRandom().nextGaussian()*8,z=p.getZ()+p.getRandom().nextGaussian()*8;int y=mc.level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,(int)Math.floor(x),(int)Math.floor(z));if(!mc.level.getFluidState(new net.minecraft.core.BlockPos((int)Math.floor(x),y-1,(int)Math.floor(z))).isEmpty())mc.level.addParticle(ParticleTypes.SOUL,x,y,z,0,.04,0);}}
  if(stage==1&&p.tickCount%3==0){for(int i=0;i<3;i++){double x=p.getX()+p.getRandom().nextGaussian()*12,z=p.getZ()+p.getRandom().nextGaussian()*12;int y=mc.level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,(int)x,(int)z);if(mc.level.getBlockState(new net.minecraft.core.BlockPos((int)x,y-1,(int)z)).is(net.minecraft.world.level.block.Blocks.WATER))mc.level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,x,y,z,0,.05,0);}}

 }
 // Sky-only decorative projection: a fixed world anchor, independent of terrain fog and particle culling.
 static void skyApparition(RenderGuiEvent.Post e){var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||Campaign.stage(mc.level)!=3||mc.gui.screen()!=null)return;var camera=mc.gameRenderer.mainCamera();var origin=camera.position();if(!mc.level.canSeeSky(mc.player.blockPosition().above()))return;var g=e.getGuiGraphics();int w=g.guiWidth(),h=g.guiHeight();float scale=(float)(h/(2*Math.tan(Math.toRadians(mc.options.fov().get())/2)));var inverse=new org.joml.Quaternionf(camera.rotation()).conjugate();double t=mc.player.tickCount*.045;
  for(int i=0;i<420;i++){double y=100+(i%70)*2.2,r=5+(y-100)*.23,a=t+y*.13+(i/70)*1.047;double x=260+Math.cos(a)*r,z=65+Math.sin(a)*r;var point=new org.joml.Vector3f((float)(x-origin.x),(float)(y-origin.y),(float)(z-origin.z)).rotate(inverse);if(point.z>=-1)continue;int sx=(int)(w/2-point.x/point.z*scale),sy=(int)(h/2+point.y/point.z*scale);if(sx<0||sx>=w||sy<0||sy>=h)continue;int size=i%21==0?3:2;g.fill(sx,sy,sx+size,sy+size,i%21==0?0xa0b2fff1:0x5044a99e);}
 }
 static void scene(int mode){var mc=Minecraft.getInstance();if(mode==4||mode==0){if(playing!=null)mc.getSoundManager().stop(playing);playing=null;if(mode==0&&mc.gui.screen() instanceof Credits)mc.gui.setScreen(null);return;}mc.getMusicManager().stopPlaying();if(playing!=null)mc.getSoundManager().stop(playing);String sound=mode==1?"campaign.finale":mode==2?"campaign.boss":"campaign.return";playing=SimpleSoundInstance.forMusic(Content.MUSIC.get(sound).get());mc.getSoundManager().play(playing);if(mode==1)mc.gui.setScreen(new Credits());}
 static class Credits extends Screen {
  int age;boolean buttons;
  java.util.List<String> musicCredits=new java.util.ArrayList<>();
  Credits(){super(Component.literal("Les Royaumes de l’Arbre-Monde — Fin"));try{var res=Minecraft.getInstance().getResourceManager().getResource(Yggdrasil.id("music_credits.json"));if(res.isPresent())try(var reader=new java.io.InputStreamReader(res.get().open(),java.nio.charset.StandardCharsets.UTF_8)){var json=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();for(var track:json.getAsJsonArray("tracks")){var o=track.getAsJsonObject();musicCredits.add(o.get("title").getAsString());musicCredits.add(o.get("creator").getAsString());musicCredits.add("");}}}catch(Exception ignored){musicCredits.add("Crédits musicaux à renseigner");}}
  @Override public void tick(){age++;if(age>=1300&&!buttons){buttons=true;addRenderableWidget(Button.builder(Component.literal("Une suite à cette aventure"),b->reply(0)).bounds(width/2-130,height-62,260,20).build());addRenderableWidget(Button.builder(Component.literal("Découvrir un autre projet"),b->reply(1)).bounds(width/2-130,height-38,260,20).build());}}
  void reply(int n){ClientPacketDistributor.sendToServer(new Network.Feedback(n));}
  @Override public boolean shouldCloseOnEsc(){return false;}
  @Override public boolean isPauseScreen(){return false;}
  @Override public void extractBackground(GuiGraphicsExtractor g,int x,int y,float t){}
  @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
   g.fill(0,0,width,32,0xff02050a);g.fill(0,height-32,width,height,0xff02050a);
   if(age<250)g.centeredText(font,"LES ROYAUMES DE L’ARBRE-MONDE",width/2,height/2,0xffebd7a4);
   else if(age<500)g.centeredText(font,"La Dernière Nuit s’est tue.",width/2,height/2,0xff9fe8e1);
   else if(age<720)g.centeredText(font,"Les racines se souviendront de ton voyage.",width/2,height/2,0xffe7dbc3);
   else if(age<1300){g.fill(0,32,width,height-32,0xa0020711);java.util.List<String> lines=new java.util.ArrayList<>(java.util.List.of("FIN DE L’AVENTURE","","Une création de","FourthRuneStudio","","Musiques et créateurs"));lines.addAll(musicCredits);lines.add("Merci d’avoir parcouru les royaumes.");lines.add("Et maintenant… ?");int start=height-(age-720);for(int i=0;i<lines.size();i++){int y=start+i*23;if(y>40&&y<height-40)g.centeredText(font,lines.get(i),width/2,y,i==3?0xff55e5d3:0xffe8dabc);}}
   else{g.fill(0,32,width,height-32,0xc0020711);g.centeredText(font,"Merci d’avoir joué !",width/2,height/2-35,0xffe8d7b0);g.centeredText(font,"Ton avis : continuer ce projet ou explorer un autre univers ?",width/2,height/2-10,0xffb8ddd7);g.centeredText(font,"Ton choix reste dans cette sauvegarde.",width/2,height/2+10,0xff90aaa9);}
   super.extractRenderState(g,mx,my,dt);
  }
 }
 static class BossState extends ZombieRenderState {int variant;}
 static class BossRenderer extends ZombieRenderer {
  BossRenderer(EntityRendererProvider.Context c){this(c,-1);}
  BossRenderer(EntityRendererProvider.Context c,int v){super(c,v<0?BOSS_LAYER:guardianLayer(v),ModelLayers.ZOMBIE_BABY,ModelLayers.ZOMBIE_ARMOR,ModelLayers.ZOMBIE_BABY_ARMOR);}
  @Override public BossState createRenderState(){return new BossState();}
  @Override public void extractRenderState(Zombie e,ZombieRenderState s,float t){super.extractRenderState(e,s,t);((BossState)s).variant=e instanceof Actors.RealmBoss b?b.stage():0;}
  @Override public Identifier getTextureLocation(ZombieRenderState s){return Yggdrasil.id("textures/entity/boss_"+((BossState)s).variant+".png");}
 }
 static class FrostRenderer extends SkeletonRenderer {FrostRenderer(EntityRendererProvider.Context c){super(c);}@Override public Identifier getTextureLocation(SkeletonRenderState s){return Yggdrasil.id("textures/entity/frost_watcher.png");}}
 static class SulfurRenderer extends ZombieRenderer {SulfurRenderer(EntityRendererProvider.Context c){super(c);}@Override public Identifier getTextureLocation(ZombieRenderState s){return Yggdrasil.id("textures/entity/sulfur_wanderer.png");}}
 static class GhostRenderer extends SkeletonRenderer {GhostRenderer(EntityRendererProvider.Context c){super(c);}@Override public Identifier getTextureLocation(SkeletonRenderState s){return Yggdrasil.id("textures/entity/spectral_skeleton.png");}}
 static class SpecterRenderer extends VexRenderer {SpecterRenderer(EntityRendererProvider.Context c){super(c);}@Override public Identifier getTextureLocation(VexRenderState s){return Yggdrasil.id("textures/entity/lost_specter.png");}}
 static class DrakeRenderer extends MobRenderer<Actors.Drake,PhantomRenderState,DrakeModel>{DrakeRenderer(EntityRendererProvider.Context c){super(c,new DrakeModel(c.bakeLayer(DRAKE_LAYER)),1.2f);}@Override public PhantomRenderState createRenderState(){return new PhantomRenderState();}@Override public Identifier getTextureLocation(PhantomRenderState s){return Yggdrasil.id("textures/entity/runic_drake.png");}}
 static class DrakeModel extends EntityModel<PhantomRenderState>{
  final ModelPart left,right,tail,head;
  DrakeModel(ModelPart root){super(root);left=root.getChild("left_wing");right=root.getChild("right_wing");tail=root.getChild("tail");head=root.getChild("head");}
  static LayerDefinition layer(){var mesh=new MeshDefinition();var r=mesh.getRoot();r.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0).addBox(-5,-4,-9,10,8,18),PartPose.offset(0,12,0));
   var head=r.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,28).addBox(-3,-3,-7,6,6,8).texOffs(28,28).addBox(-2,-1,-11,4,3,5).texOffs(45,0).addBox(-3,-7,-2,1,5,2).texOffs(45,0).addBox(2,-7,-2,1,5,2),PartPose.offset(0,9,-9));
   r.addOrReplaceChild("left_wing",CubeListBuilder.create().texOffs(0,44).addBox(0,-1,-5,22,1,13).texOffs(44,8).addBox(0,-2,-6,22,2,2),PartPose.offset(4,10,-1));r.addOrReplaceChild("right_wing",CubeListBuilder.create().texOffs(0,44).mirror().addBox(-22,-1,-5,22,1,13).texOffs(44,8).addBox(-22,-2,-6,22,2,2),PartPose.offset(-4,10,-1));
   r.addOrReplaceChild("tail",CubeListBuilder.create().texOffs(65,0).addBox(-2,-2,0,4,4,20).texOffs(65,26).addBox(-1,-4,15,2,4,5),PartPose.offset(0,12,8));
   for(int x:new int[]{-4,4})for(int z:new int[]{-5,5})r.addOrReplaceChild("leg_"+x+"_"+z,CubeListBuilder.create().texOffs(96,0).addBox(-1,0,-1,2,8,3).texOffs(96,14).addBox(-2,7,-3,4,2,5),PartPose.offset(x,14,z));return LayerDefinition.create(mesh,128,64);}
  @Override public void setupAnim(PhantomRenderState s){super.setupAnim(s);left.zRot=(float)(.2+Math.sin(s.ageInTicks*.3)*.4);right.zRot=-left.zRot;tail.yRot=(float)Math.sin(s.ageInTicks*.12)*.15f;head.xRot=s.xRot*(float)Math.PI/180;}
 }
}
