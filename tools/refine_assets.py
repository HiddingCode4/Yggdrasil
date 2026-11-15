"""Deterministic hand-shaped 32px realm metal assets; run after generate_campaign.py."""
from pathlib import Path
from PIL import Image,ImageDraw
import random,math
A=Path(__file__).resolve().parents[1]/'src/main/resources/assets/yggdrasil'
TIERS=['hrimsteel','sunbronze','oathsilver','soulsteel'];COLORS=[(115,205,228),(228,155,59),(160,146,209),(69,180,162)]
def shade(c,k):return tuple(max(0,min(255,int(x*k))) for x in c)
def save(folder,name,im):im.save(A/f'textures/{folder}/{name}.png')
def rune(d,x,y,c):d.line([(x,y-3),(x,y+3)],fill=c);d.line([(x,y-2),(x+2,y),(x,y+1)],fill=c)
for tier,c in zip(TIERS,COLORS):
 rng=random.Random(tier)
 im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
 for y in range(32):
  for x in range(32):
   v=rng.randint(-8,8)+int(4*math.sin(x*.5+y*.7));d.point((x,y),fill=(max(0,52+v),max(0,57+v),max(0,63+v),255))
 for i in range(12):
  x,y=rng.randrange(32),rng.randrange(32);d.line([(x,y),(x+4,y-2),(x+8,y-1)],fill=(37,41,47),width=1)
 for x,y in [(4,7),(17,5),(12,19),(25,23)]:
  pts=[(x-3,y),(x,y-3),(x+4,y-1),(x+3,y+3),(x-1,y+4)]
  d.polygon([(a+1,b+1) for a,b in pts],fill=(20,27,33));d.polygon(pts,fill=shade(c,.55));d.polygon([(x-2,y),(x,y-2),(x+3,y-1),(x+1,y+1)],fill=c);d.point((x,y-2),fill=shade(c,1.35))
 save('block',tier+'_ore',im)
 im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
 d.polygon([(3,15),(10,8),(25,8),(29,14),(25,23),(5,23)],fill=(22,27,32));d.polygon([(4,15),(11,9),(24,9),(27,14),(20,18)],fill=shade(c,1.1));d.polygon([(4,16),(20,19),(20,23),(5,22)],fill=shade(c,.6));d.polygon([(21,19),(28,15),(24,22),(21,23)],fill=shade(c,.8));d.line([(6,14),(12,10),(23,10)],fill=shade(c,1.35));d.line([(5,16),(19,19)],fill=c);rune(d,17,13,shade(c,.38))
 for x,y in [(9,18),(13,20),(24,16)]:d.point((x,y),fill=shade(c,.85))
 save('item',tier+'_ingot',im)
 im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
 for x,y,sz in [(10,15,8),(20,20,7),(20,9,5)]:
  d.polygon([(x-sz,y),(x-3,y-sz),(x+sz,y-3),(x+sz-1,y+4),(x,y+sz)],fill=(40,45,51));d.polygon([(x-4,y),(x,y-5),(x+5,y-3),(x+3,y+3),(x-2,y+4)],fill=shade(c,.8));d.line([(x-3,y),(x,y-4),(x+4,y-2)],fill=shade(c,1.3))
 save('item',tier+'_raw',im)
 # Forged metal blocks and carved table stone.
 for kind in ['block','table']:
  im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
  for y in range(32):
   for x in range(32):d.point((x,y),fill=shade(c,(.48 if kind=='block' else .19)+rng.random()*.09))
  d.rectangle((1,1,30,30),outline=shade(c,.85),width=1);d.rectangle((3,3,28,28),outline=shade(c,.3));d.line([(4,27),(4,4),(27,4)],fill=shade(c,.7));rune(d,15,16,shade(c,1.2));d.line([(8,21),(8,10),(23,10),(23,21)],fill=shade(c,.65));save('block',tier+'_'+kind,im)
 for kind in ['sword','axe','pickaxe','shovel','hoe']:
  im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im);d.line((6,27,22,11),fill=(28,25,29),width=5);d.line((6,26,22,10),fill=(103,67,43),width=3);d.line((7,25,22,10),fill=(164,112,65),width=1)
  if kind=='sword':pts=[(11,20),(12,16),(24,3),(28,2),(28,6),(16,19)]
  elif kind=='pickaxe':pts=[(9,8),(16,4),(25,6),(29,11),(29,17),(25,11),(17,9)]
  elif kind=='axe':pts=[(15,9),(22,3),(28,5),(29,14),(25,20),(18,17),(21,11)]
  elif kind=='shovel':pts=[(18,12),(19,5),(26,2),(30,6),(27,13),(21,15)]
  else:pts=[(11,7),(18,4),(27,8),(28,15),(24,12),(17,10)]
  d.polygon(pts,fill=(25,30,35));d.polygon([(x,y+1) for x,y in pts],fill=shade(c,.6));d.line(pts[:3],fill=shade(c,1.25),width=2);d.line((18,12,23,7),fill=c,width=2)
  if kind=='sword':d.line((9,16,18,25),fill=shade(c,.6),width=3);d.line((10,16,18,24),fill=c)
  save('item',tier+'_'+kind,im)
 # Armor inventory icons, matching forged metal and luminous central knot.
 for kind in ['helmet','chestplate','leggings','boots']:
  im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
  pts={'helmet':[(6,23),(5,12),(9,5),(23,5),(27,12),(26,23),(22,23),(22,16),(10,16),(10,23)],'chestplate':[(3,8),(10,4),(13,8),(19,8),(22,4),(29,8),(26,17),(23,14),(23,28),(9,28),(9,14),(6,17)],'leggings':[(7,4),(25,4),(25,28),(18,28),(17,15),(15,15),(14,28),(7,28)],'boots':[(6,8),(13,8),(13,27),(2,27),(2,22),(6,20),(6,8),(19,8),(26,8),(26,20),(30,22),(30,27),(19,27)]}[kind]
  d.polygon(pts,fill=shade(c,.48));d.line(pts+[pts[0]],fill=(28,32,36),width=1);d.line([(x+1,y+1) for x,y in pts[:5]],fill=c,width=1);rune(d,16,11,shade(c,1.3));save('item',tier+'_'+kind,im)
# New mob skins follow matching existing entity layouts.
for src,dest,c in [('spectral_skeleton','frost_watcher',(130,190,220)),('boss_1','sulfur_wanderer',(167,116,61))]:
 im=Image.open(A/f'textures/entity/{src}.png').convert('RGBA');pixels=im.load()
 for y in range(im.height):
  for x in range(im.width):
   rr,gg,bb,a=pixels[x,y];v=(rr+gg+bb)/765;pixels[x,y]=(*shade(c,.35+v),a)
 d=ImageDraw.Draw(im);d.line((8,10,14,10),fill=(169,245,255) if 'frost' in dest else (255,206,91));save('entity',dest,im)
# Preview for inspecting all four materials at nearest-neighbor scale.
preview=Image.new('RGB',(4*224,144),(24,29,36));d=ImageDraw.Draw(preview)
for i,t in enumerate(TIERS):
 d.text((i*224+8,8),t,fill=(225,226,230))
 for k,(folder,suffix) in enumerate([('block','ore'),('item','raw'),('item','ingot')]):
  im=Image.open(A/f'textures/{folder}/{t}_{suffix}.png').convert('RGBA').resize((64,64),Image.Resampling.NEAREST);preview.paste(im,(i*224+k*72+4,40),im)
preview.save(A.parents[3]/'texture-preview.png')
print('REFINED_ASSETS_PASS 4 ores/raw/ingots, 8 building/table textures, 36 equipment icons, 2 entity skins')

for name in ['engraved_stone','runic_offering']:
 im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im);d.polygon([(8,3),(23,4),(28,12),(25,27),(7,28),(3,19)],fill=(33,41,46));d.polygon([(9,5),(22,6),(25,13),(22,24),(8,25),(5,18)],fill=(93,108,111));d.line([(8,24),(6,17),(10,7),(21,7)],fill=(143,158,154),width=1);rune(d,15,15,(107,225,218) if name=='engraved_stone' else (244,202,112));
 if name=='runic_offering':d.ellipse((4,4,27,27),outline=(177,125,56),width=2);d.line((7,8,22,24),fill=(175,131,65),width=1)
 save('item',name,im)
