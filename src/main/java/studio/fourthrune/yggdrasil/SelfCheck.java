package studio.fourthrune.yggdrasil;

import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.item.DyeColor;

/** Opt-in integration check in a disposable development world only. */
final class SelfCheck {
 static void started(ServerStartedEvent e){if(!Boolean.getBoolean("yggdrasil.selftest"))return;var s=e.getServer();s.execute(()->{try{
  for(int i=0;i<7;i++)if(s.getLevel(Travel.key(i))==null)throw new AssertionError("Missing realm "+i);
  if(Boolean.getBoolean("yggdrasil.renovationSelftest"))Travel.renovate(s);
  Travel.ensureHub(s);var hub=s.getLevel(Yggdrasil.HUB);
  require(hub.getBlockState(Travel.STONE).is(Yggdrasil.WAYSTONE.get()),"waystone");
  require(hub.getBlockState(new BlockPos(0,81,0)).is(Yggdrasil.PORTAL.get()),"entrance");
  require(hub.getBlockState(new BlockPos(-8,85,203)).is(Blocks.ANVIL),"forge");
  require(hub.getBlockState(new BlockPos(6,85,204)).is(Blocks.SMOKER),"kitchen");
  require(hub.getBlockState(new BlockPos(-10,91,177)).is(Blocks.BED.pick(DyeColor.CYAN)),"bedrooms");
  require(hub.getBlockState(new BlockPos(0,84,168)).isAir(),"front door clearance");
  // All generated leaves use persistent states; sample every canopy layer.
  int sampled=0;
  for(int x=-60;x<=60;x+=3)for(int z=160;z<=278;z+=3)for(int y=130;y<=198;y+=3){var st=hub.getBlockState(new BlockPos(x,y,z));if(st.getBlock() instanceof net.minecraft.world.level.block.LeavesBlock){require(st.getValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT),"persistent leaves");sampled++;}}
  require(sampled>500,"continuous canopy samples");
  require(!hub.getBlockState(new BlockPos(4,102,168)).isAir(),"closed front gable");
  require(hub.getBlockState(new BlockPos(0,119,162)).is(net.minecraft.world.level.block.Blocks.STRIPPED_DARK_OAK_LOG),"carved ridge prow");
  // Real collision checks along the walkable route, including the two elevation changes.
  for(int z=10;z<140;z++){int x=(int)(Math.sin(z*.037)*7),y=80+(z>60?1:0)+(z>115?1:0);require(hub.getBlockState(new BlockPos(x,y,z)).isSolidRender(),"path floor "+z);require(hub.getBlockState(new BlockPos(x,y+1,z)).isAir(),"path headroom "+z);}
  for(int i=3;i<7;i++){var l=s.getLevel(Travel.key(i));WorldBuilder.realmArrival(l,i,new BlockPos(0,82,0));WorldBuilder.gate(l,new BlockPos(0,82,0));require(l.getBlockState(new BlockPos(0,82,0)).is(Yggdrasil.PORTAL.get()),"return gate "+i);require(l.getBlockState(new BlockPos(0,82,3)).isAir(),"arrival headroom "+i);}
  var fake=new net.neoforged.neoforge.common.util.FakePlayer(hub,new com.mojang.authlib.GameProfile(java.util.UUID.fromString("00000000-0000-0000-0000-000000000044"),"YggdrasilTest")){@Override public void sendSystemMessage(net.minecraft.network.chat.Component c,boolean overlay){System.out.println("YGGDRASIL_TEST_MESSAGE: "+c.getString());}};
  fake.setPos(0.5,84,150.5);
  int before=Travel.state(s).routes.size();Travel.select(fake,-1);Travel.select(fake,99);Travel.select(fake,2);Travel.select(fake,1);require(Travel.state(s).routes.size()==before,"invalid/locked destinations rejected");
  fake.setPos(100.5,84,150.5);Travel.select(fake,3);require(Travel.state(s).routes.size()==before,"remote click rejected");
  fake.addTag("ygg_unlock_solfara");fake.addTag("ygg_unlock_skaldheim");fake.addTag("ygg_unlock_draugavik");fake.setPos(.5,84,150.5);Travel.select(fake,3);require(Travel.state(s).routes.containsKey("solfara"),"destination route saved");require(fake.level().dimension().equals(Yggdrasil.realm("solfara")),"cross dimension teleport");Travel.enter(fake);require(fake.level().dimension().equals(Yggdrasil.HUB),"return teleport");
  fake.addTag("yggdrasil_nether");fake.addTag("yggdrasil_end");
  for(int i=0;i<7;i++){fake.setPos(.5,84,150.5);Travel.select(fake,i);require(fake.level().dimension().equals(Travel.key(i)),"destination teleport "+i);Travel.enter(fake);require(fake.level().dimension().equals(Yggdrasil.HUB),"return from "+i);}
  CampaignChecks.run(s);Travel.save(s);System.out.println("YGGDRASIL_SELFTEST_PASS: seven map destinations, six custom dimensions, hub, path, rooms, forge, kitchen, gates.");
 }catch(Throwable t){t.printStackTrace();System.out.println("YGGDRASIL_SELFTEST_FAIL");}finally{s.halt(false);}});}
 static void require(boolean test,String message){if(!test)throw new AssertionError(message);}
}
