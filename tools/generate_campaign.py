"""Generate exact, code-owned pixel assets and the Minecraft 26.3 campaign data."""
from pathlib import Path
import json,zipfile,io,random,math
import numpy as np
from PIL import Image,ImageDraw
R=Path(__file__).resolve().parents[1]/'src/main/resources';A=R/'assets/yggdrasil';D=R/'data/yggdrasil'
z=zipfile.ZipFile('/home/agent/.gradle/caches/neoformruntime/artifacts/minecraft_26.3_client.jar')
colors=[(101,221,245),(232,164,57),(155,156,236),(64,230,204)];tiers=['hrimsteel','sunbronze','oathsilver','soulsteel'];zones=['hrimdalr','solfara','skaldheim','draugavik']
fr={};en={}
def put(base,p,data):
 p=base/p;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
def png(p,img):p=A/p;p.parent.mkdir(parents=True,exist_ok=True);img.save(p)
def translated(kind,name,label):fr[f'{kind}.yggdrasil.{name}']=label;en[f'{kind}.yggdrasil.{name}']=name.replace('_',' ').title()
def recolor(path,color,alpha=None):
 im=Image.open(io.BytesIO(z.read('assets/minecraft/'+path))).convert('RGBA');a=np.asarray(im).copy();gray=a[:,:,:3].mean(2)/255;col=np.array(color);rgb=np.clip(gray[:,:,None]*col*1.55+np.array(color)*.1,0,255);a[:,:,:3]=rgb.astype('uint8');
 if alpha is not None:a[:,:,3]=(a[:,:,3].astype(float)*alpha).astype('uint8')
 return Image.fromarray(a)
def model_item(name):put(A,Path(f'models/item/{name}.json'),{'parent':'minecraft:item/generated','textures':{'layer0':f'yggdrasil:item/{name}'}});put(A,Path(f'items/{name}.json'),{'model':{'type':'minecraft:model','model':f'yggdrasil:item/{name}'}})
def item(name,label,color,shape='gem'):
 im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im);c=color
 if shape=='orb':
  for radius in range(13,3,-1):d.ellipse((16-radius,16-radius,16+radius,16+radius),fill=(*c,30+int((13-radius)*13)))
  d.ellipse((10,8,23,23),fill=(*c,255));d.ellipse((12,9,17,14),fill=(230,255,255,255));d.line((14,24,10,30),fill=(*c,180),width=3)
 elif shape=='ingot':d.polygon([(5,13),(12,6),(26,10),(28,20),(20,26),(4,22)],fill=tuple(int(x*.6) for x in c));d.polygon([(5,13),(12,6),(26,10),(20,17)],fill=c);d.line((6,14,19,18,26,11),fill=(235,245,230),width=2)
 elif shape=='flask':d.rectangle((12,2,19,10),fill=(166,203,209,255));d.ellipse((6,9,25,29),fill=(131,188,201,210));d.ellipse((9,14,23,27),fill=(*c,255));d.line((10,14,10,20),fill='white',width=2)
 else:d.polygon([(16,2),(27,11),(24,25),(14,30),(4,19),(6,8)],fill=tuple(int(x*.45) for x in c));d.polygon([(16,4),(24,11),(16,24),(8,18),(8,10)],fill=c);d.line((16,5,18,13,12,20),fill=(235,250,241),width=2)
 png(Path(f'textures/item/{name}.png'),im);model_item(name);translated('item',name,label)
def recipe(name,result,pattern,key,count=1):put(D,Path(f'recipe/{name}.json'),{'type':'minecraft:crafting_shaped','pattern':pattern,'key':key,'result':{'id':result,'count':count}})
def loot(name,drop):put(D,Path(f'loot_table/blocks/{name}.json'),{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':drop}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
def block(name,label,c,ore=False,table=False):
 im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im);rnd=random.Random(name)
 for x in range(16):
  for y in range(16):
   v=rnd.randint(-12,12);base=(45,52,58) if ore else tuple(int(t*.35) for t in c);d.point((x,y),fill=tuple(max(0,min(255,t+v)) for t in base)+(255,))
 if ore:
  for i in range(9):x=rnd.randint(1,13);y=rnd.randint(1,13);d.rectangle((x,y,x+1,y+1),fill=c)
 else:
  d.rectangle((1,1,14,14),outline=tuple(int(x*.65) for x in c));d.line((8,3,8,12,4,8,8,6,12,8),fill=c,width=1)
 png(Path(f'textures/block/{name}.png'),im)
 mod={'parent':'minecraft:block/cube_all','textures':{'all':f'yggdrasil:block/{name}'}}
 if table:mod={'textures':{'stone':f'yggdrasil:block/{name}','particle':f'yggdrasil:block/{name}','book':'minecraft:block/bookshelf'},'elements':[{'from':[0,0,0],'to':[16,9,16],'faces':{side:{'texture':'#stone'} for side in ['up','down','north','south','east','west']}},{'from':[2,9,2],'to':[14,12,14],'faces':{side:{'texture':'#stone'} for side in ['up','down','north','south','east','west']}},{'from':[4,12,5],'to':[12,14,11],'faces':{side:{'texture':'#book'} for side in ['up','down','north','south','east','west']}}]}
 put(A,Path(f'models/block/{name}.json'),mod);put(A,Path(f'blockstates/{name}.json'),{'variants':{'':{'model':f'yggdrasil:block/{name}'}}});put(A,Path(f'models/item/{name}.json'),{'parent':f'yggdrasil:block/{name}'});put(A,Path(f'items/{name}.json'),{'model':{'type':'minecraft:model','model':f'yggdrasil:item/{name}'}});translated('block',name,label);loot(name,'yggdrasil:'+name)
labels=['Acier du Givre','Bronze solaire','Argent des Serments','Acier des Âmes']
for i,(tier,c,zone) in enumerate(zip(tiers,colors,zones)):
 item(tier+'_ingot','Lingot — '+labels[i],c,'ingot');item(tier+'_raw','Minerai brut — '+labels[i],c)
 for kind,vanilla in [('sword','sword'),('pickaxe','pickaxe'),('axe','axe'),('shovel','shovel'),('hoe','hoe'),('helmet','helmet'),('chestplate','chestplate'),('leggings','leggings'),('boots','boots')]:
  png(Path(f'textures/item/{tier}_{kind}.png'),recolor('textures/item/netherite_'+vanilla+'.png',c));model_item(tier+'_'+kind);translated('item',tier+'_'+kind,{'sword':'Épée','pickaxe':'Pioche','axe':'Hache','shovel':'Pelle','hoe':'Houe','helmet':'Casque','chestplate':'Plastron','leggings':'Jambières','boots':'Bottes'}[kind]+' — '+labels[i])
 for layer in ['humanoid','humanoid_leggings']:
  png(Path(f'textures/entity/equipment/{layer}/{tier}.png'),recolor(f'textures/entity/equipment/{layer}/netherite.png',c))
 put(A,Path('equipment/'+tier+'.json'),{'layers':{'humanoid':[{'texture':'yggdrasil:'+tier}],'humanoid_leggings':[{'texture':'yggdrasil:'+tier}]}})
 for suffix,label in [('ore','Minerai'),('block','Bloc'),('table','Table runique')]:block(tier+'_'+suffix,label+' — '+labels[i],c,ore=suffix=='ore',table=suffix=='table')
 loot(tier+'_ore','yggdrasil:'+tier+'_raw');put(D,Path('tags/item/'+tier+'_repair.json'),{'values':['yggdrasil:'+tier+'_ingot']})
 recipe(tier+'_block','yggdrasil:'+tier+'_block',['III','III','III'],{'I':'yggdrasil:'+tier+'_ingot'})
 for typ in ['smelting','blasting']:put(D,Path(f'recipe/{tier}_{typ}.json'),{'type':'minecraft:'+typ,'ingredient':'yggdrasil:'+tier+'_raw','result':{'id':'yggdrasil:'+tier+'_ingot'},'experience':1,'cookingtime':200 if typ=='smelting' else 100})
 patterns={'sword':[' I ',' I ',' S '],'pickaxe':['III',' S ',' S '],'axe':['II ','IS ',' S '],'shovel':[' I ',' S ',' S '],'hoe':['II ',' S ',' S '],'helmet':['III','I I'],'chestplate':['I I','III','III'],'leggings':['III','I I','I I'],'boots':['I I','I I']}
 for kind,pattern in patterns.items():recipe(tier+'_'+kind,'yggdrasil:'+tier+'_'+kind,pattern,{'I':'yggdrasil:'+tier+'_ingot',**({'S':'minecraft:stick'} if kind in ['sword','pickaxe','axe','shovel','hoe'] else {})})
 recipe(tier+'_table','yggdrasil:'+tier+'_table',['IAI','IEI','IOI'],{'I':'yggdrasil:'+tier+'_ingot','A':'minecraft:amethyst_block','E':'minecraft:enchanting_table','O':'minecraft:obsidian'})
 put(D,Path(f'worldgen/feature/{tier}_ore.json'),{'type':'minecraft:ore','size':6,'discard_chance_on_air_exposure':0.1,'targets':[{'state':'yggdrasil:'+tier+'_ore','target':{'predicate_type':'minecraft:tag_match','tag':tag}} for tag in ['minecraft:stone_ore_replaceables','minecraft:deepslate_ore_replaceables']]})
 put(D,Path(f'worldgen/placed_feature/{tier}_ore.json'),{'feature':'yggdrasil:'+tier+'_ore','placement':[{'type':'minecraft:count','count':9 if i==3 else 10-i},{'type':'minecraft:in_square'},{'type':'minecraft:height_range','height':{'type':'minecraft:uniform','min_inclusive':{'absolute':-56},'max_inclusive':{'absolute':48 if i==3 else 80}}},{'type':'minecraft:biome'}]})
 p=D/f'worldgen/biome/{zone}.json';b=json.loads(p.read_text());b['features'][6]=[v for v in b['features'][6] if not v.startswith('yggdrasil:')]+['yggdrasil:'+tier+'_ore'];
 spawns=b.setdefault('attributes',{}).get('minecraft:gameplay/natural_mob_spawns',{}).get('argument',{}).get('spawns_by_category',{})
 if i==0:spawns.pop('creature',None)
 if i>=2:spawns.pop('monster',None)
 if i==1:b['has_precipitation']=True;b['temperature']=1.4
 p.write_text(json.dumps(b,indent=2))
item('root_heart','Cœur des Racines',(82,229,180),'orb');item('engraved_stone','Pierre gravée',(151,184,181));item('runic_offering','Offrande runique',(213,177,98));item('world_relic','Relique de la Dernière Nuit',(218,198,255),'orb');item('drake_bond','Lien draconique',(74,243,189),'orb');item('heated_flask','Flacon de vapeur brûlante',(249,150,49),'flask');item('purified_flask','Eau distillée refroidie',(96,218,248),'flask')
for i,dest in enumerate(['solfara','skaldheim','draugavik','sanctum']):item('mana_soul_'+dest,'Âme de mana — '+dest.title(),colors[i],'orb')
for i,n in enumerate(['valkyrie_blade','abyssal_blade','storm_hammer']):png(Path('textures/item/'+n+'.png'),recolor('textures/item/netherite_'+('axe' if i==2 else 'sword')+'.png',[(251,217,130),(154,104,242),(101,238,240)][i]));model_item(n);translated('item',n,['Lame des Valkyries','Lame des Abysses','Marteau des Tempêtes'][i])
for name,label,c in [('distiller','Distillateur',(223,168,66)),('condenser','Condenseur',(104,211,235)),('spectral_lantern','Lanterne des Passeurs',(54,246,218)),('boss_altar','Autel du Gardien',(174,153,219)),('secret_gate','Porte de la Dernière Nuit',(116,67,210)),('commission_board','Tableau des Bâtisseurs',(202,168,102))]:block(name,label,c,table=name in ['distiller','condenser','boss_altar'])
recipe('root_heart','yggdrasil:root_heart',['ACA','CSC','ACA'],{'A':'minecraft:amethyst_shard','C':'minecraft:copper_ingot','S':'#minecraft:saplings'})
recipe('engraved_stone','yggdrasil:engraved_stone',[' A ','LSL',' A '],{'A':'minecraft:amethyst_shard','L':'minecraft:lapis_lazuli','S':'minecraft:chiseled_stone_bricks'},4)
recipe('runic_offering','yggdrasil:runic_offering',[' A ','ASA',' A '],{'A':'minecraft:amethyst_shard','S':'yggdrasil:engraved_stone'})
recipe('distiller','yggdrasil:distiller',['ICI','I I','OFO'],{'I':'yggdrasil:sunbronze_ingot','C':'minecraft:cauldron','O':'minecraft:copper_ingot','F':'minecraft:furnace'})
recipe('condenser','yggdrasil:condenser',['III','GBG','III'],{'I':'minecraft:copper_ingot','G':'minecraft:glass','B':'minecraft:bucket'})
recipe('spectral_lantern','yggdrasil:spectral_lantern',[' I ','ALA',' I '],{'I':'yggdrasil:oathsilver_ingot','A':'minecraft:amethyst_shard','L':'minecraft:soul_lantern'})
# Tables apply these registered enchantments explicitly; they are absent from the vanilla enchanting-table pool.
names=[('hearthbound','Chaleur des Foyers'),('rime_edge','Morsure du Givre'),('sulfur_ward','Sceau des Soufres'),('ember_edge','Tranchant des Braises'),('oathlight','Lueur des Serments'),('veilbreaker','Briseur de Voile'),('soul_anchor','Ancrage de l’Âme'),('soul_harvest','Moisson des Âmes')]
for name,label in names:
 put(D,Path('enchantment/'+name+'.json'),{'anvil_cost':5,'description':{'translate':'enchantment.yggdrasil.'+name},'effects':{},'max_cost':{'base':40,'per_level_above_first':10},'max_level':3,'min_cost':{'base':20,'per_level_above_first':10},'slots':['any'],'supported_items':'#minecraft:enchantable/durability','weight':1});translated('enchantment',name,label)
# Registry item tags let vanilla enchants/recipes recognize the new equipment.
for tag,suffixes in [('swords',['sword']),('pickaxes',['pickaxe']),('axes',['axe']),('shovels',['shovel']),('hoes',['hoe']),('head_armor',['helmet']),('chest_armor',['chestplate']),('leg_armor',['leggings']),('foot_armor',['boots'])]:put(R,Path('data/minecraft/tags/item/'+tag+'.json'),{'replace':False,'values':['yggdrasil:'+tier+'_'+kind for tier in tiers for kind in suffixes]})
for tag in ['mineable/pickaxe','needs_diamond_tool']:put(R,Path('data/minecraft/tags/block/'+tag+'.json'),{'replace':False,'values':['yggdrasil:'+tier+'_'+kind for tier in tiers for kind in ['ore','block','table']]+['yggdrasil:'+name for name in ['distiller','condenser','spectral_lantern','boss_altar','secret_gate','commission_board']]})
# Separate, dark, starless final dimension; the game creates the arena only once.
b=json.loads((D/'dimension_type/yggdrasil.json').read_text());b['ambient_light']=.12;b['attributes'].update({'minecraft:visual/sky_color':'#000000','minecraft:visual/fog_color':'#01010a','minecraft:visual/star_brightness':0.0});put(D,Path('dimension_type/sanctum.json'),b);dim=json.loads((D/'dimension/yggdrasil.json').read_text());dim['type']='yggdrasil:sanctum';put(D,Path('dimension/sanctum.json'),dim)
# Custom Nordic boss skins and spectral mobs, based on the game's layout so no models are missing.
for i,c in enumerate(colors+[(133,77,207)]):
 im=recolor('textures/entity/zombie/zombie.png',c);d=ImageDraw.Draw(im);d.line((8,8,15,15),fill=(225,255,255),width=1);d.rectangle((10,10,11,10),fill=(255,250,208));d.rectangle((13,10,14,10),fill=(255,250,208));png(Path(f'textures/entity/boss_{i}.png'),im)
png(Path('textures/entity/spectral_skeleton.png'),recolor('textures/entity/skeleton/skeleton.png',(94,237,227)));png(Path('textures/entity/lost_specter.png'),recolor('textures/entity/illager/vex.png',(118,232,252)))
im=Image.new('RGBA',(128,64),(35,84,78,255));d=ImageDraw.Draw(im)
for x in range(128):
 for y in range(64):
  if (x+y*3)%9==0:d.point((x,y),fill=(65,177,157,255))
d.rectangle((0,44,44,57),fill=(44,145,135,255));d.line((0,44,43,57),fill=(152,246,215),width=2);d.rectangle((0,28,44,39),fill=(55,134,122,255));png(Path('textures/entity/runic_drake.png'),im)
put(A,Path('sounds.json'),{'campaign.boss':{'sounds':[{'name':'minecraft:music/game/end/boss','stream':True}]},'campaign.return':{'sounds':[{'name':'minecraft:music/game/end/the_end','stream':True}]},'campaign.finale':{'sounds':[{'name':'minecraft:music/game/end/alpha','stream':True}]}})
put(A,Path('music_credits.json'),{'studio':'FourthRuneStudio','tracks':[{'event':'campaign.boss','title':'Musique Minecraft provisoire','creator':'Crédits personnalisés à fournir'},{'event':'campaign.return','title':'Musique Minecraft provisoire','creator':'Crédits personnalisés à fournir'},{'event':'campaign.finale','title':'Musique Minecraft provisoire','creator':'Crédits personnalisés à fournir'}],'feedback_url':''})
for locale,extra in [('fr_fr',fr),('en_us',en)]:
 p=A/f'lang/{locale}.json';base=json.loads(p.read_text());base.update(extra)
 for name,label in [('realm_guardian','Gardien des Royaumes'),('runic_drake','Drake Runique'),('spectral_skeleton','Squelette Spectral'),('lost_specter','Spectre Égaré')]:base['entity.yggdrasil.'+name]=label if locale=='fr_fr' else name.replace('_',' ').title()
 p.write_text(json.dumps(base,ensure_ascii=False,indent=2))
print('Campaign data, registered-enchantment definitions, recipes, ore features and pixel assets generated.')

import runpy
runpy.run_path(str(Path(__file__).with_name("refine_assets.py")))
