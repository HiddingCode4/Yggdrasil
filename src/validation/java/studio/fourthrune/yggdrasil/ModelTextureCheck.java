package studio.fourthrune.yggdrasil;

import java.nio.file.*;
import com.google.gson.JsonParser;
import net.minecraft.client.resources.model.sprite.TextureSlots;

/** Runs the actual Minecraft 26.3 texture-slot resolver without starting a renderer. */
public final class ModelTextureCheck {
 public static void main(String[] args)throws Exception{
  int left=com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT;
  var click=new net.minecraft.client.input.MouseButtonEvent(625,155,new net.minecraft.client.input.MouseButtonInfo(left,0));
  if(!Client.isPrimaryClick(click))throw new AssertionError("Real 26.3 left click rejected");
  for(int other:new int[]{0,2,3,4})if(other!=left&&Client.isPrimaryClick(new net.minecraft.client.input.MouseButtonEvent(625,155,new net.minecraft.client.input.MouseButtonInfo(other,0))))throw new AssertionError("Other button accepted");
  System.out.println("PRIMARY_CLICK_PASS Minecraft 26.3 left button="+left);
  var dragon=ClientFeatures.DrakeModel.layer().bakeRoot();
  for(String part:new String[]{"head","body","left_wing","right_wing","tail"})if(dragon.getChild(part)==null)throw new AssertionError("Missing drake model part "+part);
  System.out.println("DRAKE_MODEL_PASS head/body/wings/tail geometry baked");
  for(int v=0;v<5;v++)ClientFeatures.guardianMesh(v).bakeRoot();
  var fog=new net.minecraft.client.renderer.fog.FogData();ClientFeatures.applyFog(fog,10);
  if(fog.environmentalEnd!=10||fog.renderDistanceEnd!=10||fog.skyEnd!=10||fog.cloudEnd!=10)throw new AssertionError("Fog buffer does not hide distant terrain/sky/clouds");
  System.out.println("FOG_BUFFER_PASS all four fog endpoints updated; GUARDIAN_MODELS_PASS five silhouettes baked");
  Path root=Path.of("src/main/resources/assets/yggdrasil");
  for(String name:new String[]{"waystone","portal_active","portal_inactive"}){
   var json=JsonParser.parseString(Files.readString(root.resolve("models/block/"+name+".json"))).getAsJsonObject();
   var data=TextureSlots.parseTextureMap(json.getAsJsonObject("textures"));
   var slots=new TextureSlots.Resolver().addLast(data).resolve(()->"yggdrasil:"+name);
   String mtl=Files.readString(root.resolve("models/block/"+name+".mtl"));
   String ref=mtl.lines().filter(line->line.startsWith("map_Kd ")).findFirst().orElseThrow().substring(7).trim();
   var material=slots.getMaterial(ref);if(material==null)throw new AssertionError("Unresolved OBJ slot: "+name+" "+ref);
   if(!material.sprite().toString().equals("yggdrasil:block/"+name))throw new AssertionError("Wrong sprite: "+name);
   if(!Files.isRegularFile(root.resolve("textures/block/"+name+".png")))throw new AssertionError("Missing PNG "+name);
   if(slots.getMaterial("#particle")==null)throw new AssertionError("Missing particle texture");
   System.out.println("TEXTURE_SLOT_PASS "+name+" -> "+material.sprite());
  }
 }
}
