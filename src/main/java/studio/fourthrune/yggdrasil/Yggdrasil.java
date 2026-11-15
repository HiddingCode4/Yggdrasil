package studio.fourthrune.yggdrasil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

@Mod(Yggdrasil.ID)
public final class Yggdrasil {
 public static final String ID="yggdrasil";
 public static Identifier id(String path){return Identifier.fromNamespaceAndPath(ID,path);}
 public static ResourceKey<Level> realm(String path){return ResourceKey.create(Registries.DIMENSION,id(path));}
 public static final ResourceKey<Level> HUB=realm("yggdrasil");
 public static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(ID);
 public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(ID);
 public static final DeferredBlock<Block> WAYSTONE=BLOCKS.registerBlock("waystone",p->new RuneBlock(p,true),p->p.strength(-1,3600000).noOcclusion().lightLevel(s->12));
 public static final DeferredBlock<Block> WAYSTONE_PART=BLOCKS.registerBlock("waystone_part",p->new RuneBlock(p,true),p->p.strength(-1,3600000).noOcclusion());
 public static final DeferredBlock<Block> PORTAL=BLOCKS.registerBlock("portal",p->new RuneBlock(p,false),p->p.strength(-1,3600000).noOcclusion().noCollision().lightLevel(s->14));
 public static final DeferredBlock<Block> INACTIVE=BLOCKS.registerBlock("portal_inactive",p->new RuneBlock(p,false),p->p.strength(4).noOcclusion().noCollision());
 public static final DeferredBlock<Block> GLOW_WOOD=BLOCKS.registerSimpleBlock("rune_wood",p->p.strength(3).lightLevel(s->10));
 public static final DeferredItem<BlockItem> PORTAL_ITEM=ITEMS.registerSimpleBlockItem("portal_inactive",INACTIVE);
 public Yggdrasil(IEventBus bus){
  Content.init();Content.SOUNDS.register(bus);Content.TABS.register(bus);Actors.register(bus);
  BLOCKS.register(bus);ITEMS.register(bus);bus.addListener(Network::register);
  NeoForge.EVENT_BUS.addListener(this::commands);NeoForge.EVENT_BUS.addListener(this::tick);NeoForge.EVENT_BUS.addListener(RealmEvents::interact);NeoForge.EVENT_BUS.addListener(RealmEvents::chunk);NeoForge.EVENT_BUS.addListener(RealmEvents::levelTick);NeoForge.EVENT_BUS.addListener(SelfCheck::started);NeoForge.EVENT_BUS.addListener(RealmEvents::clonePlayer);Campaign.register();
 }
 private void commands(RegisterCommandsEvent e){
  e.getDispatcher().register(Commands.literal("yggdrasil").requires(s->s.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
   .then(Commands.literal("testkit").executes(c->{Campaign.testkit(c.getSource().getPlayerOrException());return 1;}))
   .then(Commands.literal("drake").executes(c->{var p=c.getSource().getPlayerOrException();p.addTag("ygg_builder_reward");Campaign.give(p,new net.minecraft.world.item.ItemStack(Content.get("drake_bond")));Actors.summonDrake(p);return 1;}))
   .then(Commands.literal("ruin").executes(c->{var p=c.getSource().getPlayerOrException();var pos=p.blockPosition().offset(0,0,5);if(WorldBuilder.clearGateSpace((ServerLevel)p.level(),pos))p.level().setBlock(pos,INACTIVE.get().defaultBlockState(),3);return 1;}))
   .then(Commands.literal("shrine").executes(c->{var p=c.getSource().getPlayerOrException();int stage=Campaign.stage(p.level());if(stage>=0&&stage<4){CampaignWorld.zone(p,0);var r=Travel.state(p.level().getServer()).routes.get("built:zone_"+stage);if(r!=null)Travel.teleport(p,(ServerLevel)p.level(),r.pos().offset(0,0,10));}return 1;}))
   .then(Commands.literal("ending_test").executes(c->{var p=c.getSource().getPlayerOrException();p.addTag("ygg_final_victory");Travel.enter(p);Campaign.startFinale(p);return 1;}))
   .then(Commands.literal("feedback").executes(c->{var st=Travel.state(c.getSource().getServer());long a=st.feedback==null?0:st.feedback.values().stream().filter(v->v==0).count(),b=st.feedback==null?0:st.feedback.values().stream().filter(v->v==1).count();c.getSource().sendSuccess(()->Component.literal("Avis : "+a+" pour une suite ; "+b+" pour un autre projet."),false);return 1;}))
   .then(Commands.literal("renovate").executes(c->{Travel.renovate(c.getSource().getServer());c.getSource().sendSuccess(()->Component.literal("Yggdrasil rénové : arbre, pont et halle."),true);return 1;}))
   .then(Commands.literal("build").executes(c->{Travel.ensureHub(c.getSource().getServer());c.getSource().sendSuccess(()->Component.literal("Yggdrasil construit."),true);return 1;}))
   .then(Commands.literal("enter").executes(c->{Travel.enter(c.getSource().getPlayerOrException());return 1;}))
   .then(Commands.literal("portal").executes(c->{ServerPlayer p=c.getSource().getPlayerOrException();Travel.createEntrance(p);return 1;}))
   .then(Commands.literal("unlock_end").executes(c->{Travel.unlockEnd(c.getSource().getPlayerOrException());return 1;})));
 }
 private void tick(PlayerTickEvent.Post e){if(e.getEntity() instanceof ServerPlayer p){Travel.tick(p);Campaign.tick(p);}}
 static final class RuneBlock extends Block {
  private final boolean waystone;
  RuneBlock(BlockBehaviour.Properties p,boolean w){super(p);waystone=w;}
  @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
   if(player instanceof ServerPlayer p){
    if(waystone && level.dimension().equals(HUB)){if(!Campaign.waystone(p))Network.open(p);}
    else if(!waystone){
     if(state.is(INACTIVE.get()))Campaign.repair(p,pos);
     else if(level.dimension().equals(HUB))Travel.homeOverworld(p);else Travel.enter(p);
    }
   }
   return InteractionResult.SUCCESS;
  }
 }
}
