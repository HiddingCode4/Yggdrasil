package studio.fourthrune.yggdrasil;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.equipment.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.registries.*;

final class Content {
 static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,Yggdrasil.ID);
 static final String[] TIERS={"hrimsteel","sunbronze","oathsilver","soulsteel"};
 static final DeferredRegister<net.minecraft.sounds.SoundEvent> SOUNDS=DeferredRegister.create(Registries.SOUND_EVENT,Yggdrasil.ID);
 static final Map<String,DeferredHolder<net.minecraft.sounds.SoundEvent,net.minecraft.sounds.SoundEvent>> MUSIC=new HashMap<>();
 static final Map<String,DeferredItem<Item>> ITEMS=new LinkedHashMap<>();
 static final Map<String,DeferredBlock<? extends Block>> BLOCKS=new LinkedHashMap<>();
 static DeferredItem<Item> item(String name){return ITEMS.get(name);}
 static Item get(String name){return item(name).get();}
 static Block block(String name){return BLOCKS.get(name).get();}
 static void init(){TABS.register("realms",()->CreativeModeTab.builder().title(net.minecraft.network.chat.Component.literal("SworldCraft — Royaumes")).icon(()->new ItemStack(get("root_heart"))).displayItems((params,out)->{ITEMS.values().forEach(i->out.accept(i.get()));BLOCKS.values().forEach(b->out.accept(b.get().asItem()));}).build());for(String n:new String[]{"campaign.boss","campaign.return","campaign.finale"})MUSIC.put(n,SOUNDS.register(n,()->net.minecraft.sounds.SoundEvent.createVariableRangeEvent(Yggdrasil.id(n))));
  for(String name:new String[]{"root_heart","engraved_stone","mana_soul_solfara","mana_soul_skaldheim","mana_soul_draugavik","mana_soul_sanctum","world_relic","drake_bond","runic_offering","heated_flask","purified_flask","valkyrie_blade","abyssal_blade","storm_hammer"}){
   ITEMS.put(name,Yggdrasil.ITEMS.registerItem(name,p->new Artifact(p,name),p->{p.stacksTo(name.contains("soul")||name.contains("relic")||name.contains("blade")||name.contains("hammer")||name.equals("drake_bond")?1:16);if(name.contains("blade")||name.contains("hammer"))p.sword(ToolMaterial.NETHERITE,17,-2.6f).durability(10000);return p;}));
  }
  for(String name:new String[]{"distiller","condenser","spectral_lantern","boss_altar","secret_gate","commission_board"})registerBlock(name,4,name.equals("spectral_lantern")?15:8);
  for(int i=0;i<4;i++){
   int stage=i;String tier=TIERS[i];
   ITEMS.put(tier+"_raw",Yggdrasil.ITEMS.registerSimpleItem(tier+"_raw"));
   ITEMS.put(tier+"_ingot",Yggdrasil.ITEMS.registerSimpleItem(tier+"_ingot"));
   registerBlock(tier+"_ore",7+i*2,3);registerBlock(tier+"_block",6,5);registerBlock(tier+"_table",5,10);
   var repair=TagKey.create(Registries.ITEM,Yggdrasil.id(tier+"_repair"));
   var tool=new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL,2800+i*1100,11+i*2,6+i*2,20+i*3,repair);
   var armor=new ArmorMaterial(45+i*12,Map.of(ArmorType.HELMET,4+i,ArmorType.CHESTPLATE,9+i*2,ArmorType.LEGGINGS,7+i*2,ArmorType.BOOTS,4+i),22+i*3,SoundEvents.ARMOR_EQUIP_NETHERITE,4+i*1.5f,.12f+i*.03f,repair,ResourceKey.create(EquipmentAssets.ROOT_ID,Yggdrasil.id(tier)));
   for(String kind:new String[]{"sword","pickaxe","axe","shovel","hoe"})ITEMS.put(tier+"_"+kind,Yggdrasil.ITEMS.registerSimpleItem(tier+"_"+kind,p->switch(kind){case "sword"->p.sword(tool,3,-2.4f);case "pickaxe"->p.pickaxe(tool,1,-2.8f);case "axe"->p.axe(tool,5,-3);case "shovel"->p.shovel(tool,1.5f,-3);default->p.hoe(tool,0,-1);}));
   for(ArmorType type:new ArmorType[]{ArmorType.HELMET,ArmorType.CHESTPLATE,ArmorType.LEGGINGS,ArmorType.BOOTS}){String n=tier+"_"+type.getName();ITEMS.put(n,Yggdrasil.ITEMS.registerSimpleItem(n,p->p.humanoidArmor(armor,type)));}
  }
 }
 static void registerBlock(String name,float hardness,int light){
  var b=Yggdrasil.BLOCKS.registerBlock(name,p->new CampaignBlock(p,name),p->p.strength(hardness,1200).lightLevel(s->light).requiresCorrectToolForDrops());BLOCKS.put(name,b);
  Yggdrasil.ITEMS.registerSimpleBlockItem(name,b);
 }
 static class CampaignBlock extends Block {
  final String name;CampaignBlock(BlockBehaviour.Properties p,String n){super(p);name=n;}
  @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player player,BlockHitResult hit){if(player instanceof ServerPlayer p)Campaign.interact(p,pos,name);return InteractionResult.SUCCESS;}
 }
 static class Artifact extends Item {
  final String name;Artifact(Item.Properties p,String n){super(p);name=n;}
  @Override public InteractionResult use(Level l,Player player,InteractionHand hand){if(player instanceof ServerPlayer p){if(name.equals("purified_flask")){Campaign.drink(p,p.getItemInHand(hand));return InteractionResult.SUCCESS;}if(name.equals("drake_bond")){Actors.summonDrake(p);return InteractionResult.SUCCESS;}if(name.equals("storm_hammer")||name.contains("blade")){Campaign.weapon(p,name);return InteractionResult.SUCCESS;}}return super.use(l,player,hand);}
 }
}
