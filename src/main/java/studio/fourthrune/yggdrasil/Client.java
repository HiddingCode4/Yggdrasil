package studio.fourthrune.yggdrasil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class Client {
 private Client(){}
 static boolean isPrimaryClick(MouseButtonEvent event){return event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT;}
 public static void open(Network.OpenMap p){Minecraft.getInstance().setScreenAndShow(new RealmMap(p));}
 static final class RealmMap extends Screen {
  private final Network.OpenMap access;
  private int left,top,w,h;
  private static final String[] TITLES={"Overworld","Nether","End","Sólfara","Hrimdalr","Draugavik","Skaldheim"};
  private static final String[] DESC={"Terres vivantes et racines vertes.","Restaure cette liaison en visitant le Nether.","La branche fracturée attend son éveil.","Forêts cuivrées et longues aurores.","Glace chantante et souvenirs oubliés.","Mer noire et lumières des ancêtres.","Halles anciennes et pierres des serments."};
  // Coordinates in the original 1536 × 1024 illustration, including each landscape and banner.
  private static final int[][] REGIONS={{95,160,545,385},{90,640,540,870},{575,100,990,285},{1020,160,1450,385},{90,400,530,615},{1015,625,1460,875},{1010,405,1450,615}};
  RealmMap(Network.OpenMap access){super(Component.literal("Les Royaumes de l’Arbre-Monde"));this.access=access;}
  boolean available(int i){return (access.mask() & (1<<i))!=0;}
  @Override protected void init(){
   w=Math.min(width-24,(height-58)*3/2);h=w*2/3;left=(width-w)/2;top=(height-36-h)/2;
   addRenderableWidget(Button.builder(Component.literal("Fermer"),b->onClose()).bounds(width/2-45,height-24,90,20).build());
  }
  int hover(double mx,double my){double u=(mx-left)*1536/w,v=(my-top)*1024/h;for(int i=0;i<7;i++){int[] r=REGIONS[i];if(u>=r[0]&&u<=r[2]&&v>=r[1]&&v<=r[3])return i;}return -1;}
  @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
   g.fill(0,0,width,height,0xe609111b);
   g.blit(Yggdrasil.id("textures/gui/realms.png"),left,top,left+w,top+h,0f,1f,0f,1f);
   int hover=hover(mx,my);
   for(int i=0;i<7;i++){int[] r=REGIONS[i];int x=left+r[0]*w/1536,y=top+r[1]*h/1024,rw=(r[2]-r[0])*w/1536,rh=(r[3]-r[1])*h/1024;
    if(!available(i)){g.fill(x,y,x+rw,y+rh,0x99060d18);g.centeredText(font,"VERROUILLÉ",x+rw/2,y+rh/2,0xffc7c2b8);}
    if(hover==i){g.fill(x,y,x+rw,y+rh,available(i)?0x2223e3d5:0x22352222);g.outline(x,y,rw,rh,available(i)?0xff52f4dd:0xff95675b);}
   }
   String text=hover<0?"Choisis un royaume sur la carte":TITLES[hover]+" — "+DESC[hover];
   g.centeredText(font,text,width/2,Math.min(top+h+5,height-36),0xffe8d9b2);
   super.extractRenderState(g,mx,my,dt);
  }
  @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice){int i=hover(e.x(),e.y());if(isPrimaryClick(e)&&i>=0){if(available(i)){ClientPacketDistributor.sendToServer(new Network.SelectRealm(i));onClose();}return true;}return super.mouseClicked(e,twice);}
  @Override public boolean isPauseScreen(){return false;}
 }
}
