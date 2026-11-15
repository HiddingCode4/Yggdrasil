package studio.fourthrune.yggdrasil;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class Network {
 public record OpenMap(int mask) implements CustomPacketPayload {
  public boolean endUnlocked(){return (mask&4)!=0;}public boolean netherUnlocked(){return (mask&2)!=0;}
  public static final Type<OpenMap> TYPE=new Type<>(Yggdrasil.id("open_map"));
  public static final StreamCodec<RegistryFriendlyByteBuf,OpenMap> CODEC=StreamCodec.of((b,p)->b.writeVarInt(p.mask),b->new OpenMap(b.readVarInt()));
  public Type<OpenMap> type(){return TYPE;}
 }
 public record SelectRealm(int index) implements CustomPacketPayload {
  public static final Type<SelectRealm> TYPE=new Type<>(Yggdrasil.id("select_realm"));
  public static final StreamCodec<RegistryFriendlyByteBuf,SelectRealm> CODEC=StreamCodec.of((b,p)->b.writeVarInt(p.index),b->new SelectRealm(b.readVarInt()));
  public Type<SelectRealm> type(){return TYPE;}
 }
 public record Scene(int mode) implements CustomPacketPayload {
  public static final Type<Scene> TYPE=new Type<>(Yggdrasil.id("scene"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Scene> CODEC=StreamCodec.of((b,p)->b.writeVarInt(p.mode),b->new Scene(b.readVarInt()));
  public Type<Scene> type(){return TYPE;}
 }
 public record Feedback(int choice) implements CustomPacketPayload {
  public static final Type<Feedback> TYPE=new Type<>(Yggdrasil.id("feedback"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Feedback> CODEC=StreamCodec.of((b,p)->b.writeVarInt(p.choice),b->new Feedback(b.readVarInt()));
  public Type<Feedback> type(){return TYPE;}
 }
 public record DrakeControl(float forward,float strafe,boolean up) implements CustomPacketPayload {
  public static final Type<DrakeControl> TYPE=new Type<>(Yggdrasil.id("drake_control"));
  public static final StreamCodec<RegistryFriendlyByteBuf,DrakeControl> CODEC=StreamCodec.of((b,p)->{b.writeFloat(p.forward);b.writeFloat(p.strafe);b.writeBoolean(p.up);},b->new DrakeControl(b.readFloat(),b.readFloat(),b.readBoolean()));
  public Type<DrakeControl> type(){return TYPE;}
 }
 static void register(RegisterPayloadHandlersEvent e){var r=e.registrar("2");
  r.playToClient(OpenMap.TYPE,OpenMap.CODEC,(p,c)->c.enqueueWork(()->Client.open(p)));
  r.playToClient(Scene.TYPE,Scene.CODEC,(p,c)->c.enqueueWork(()->ClientFeatures.scene(p.mode)));
  r.playToServer(SelectRealm.TYPE,SelectRealm.CODEC,(p,c)->c.enqueueWork(()->{if(c.player() instanceof ServerPlayer player)Travel.select(player,p.index);}));
  r.playToServer(Feedback.TYPE,Feedback.CODEC,(p,c)->c.enqueueWork(()->{if(p.choice>=0&&p.choice<=1&&c.player() instanceof ServerPlayer player)Campaign.feedback(player,p.choice);}));
  r.playToServer(DrakeControl.TYPE,DrakeControl.CODEC,(p,c)->c.enqueueWork(()->{if(Float.isFinite(p.forward)&&Float.isFinite(p.strafe)&&c.player() instanceof ServerPlayer player&&player.getVehicle() instanceof Actors.Drake d)d.control(player,p.forward,p.strafe,p.up);}));
 }
 public static void open(ServerPlayer p){PacketDistributor.sendToPlayer(p,new OpenMap(Campaign.mask(p)));}
 static void scene(ServerPlayer p,int mode){PacketDistributor.sendToPlayer(p,new Scene(mode));}
}
