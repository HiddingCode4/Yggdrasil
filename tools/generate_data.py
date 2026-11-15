from pathlib import Path
import json
R=Path(__file__).resolve().parents[1]/'src/main/resources'
def write(p,o):
 p=R/p;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(o,ensure_ascii=False,indent=2)+'\n')
A='assets/yggdrasil/';D='data/yggdrasil/'
for name,model in [('waystone','waystone'),('portal','portal_active'),('portal_inactive','portal_inactive')]:
 write(A+f'blockstates/{name}.json',{'variants':{'':{'model':f'yggdrasil:block/{model}'}}})
 write(A+f'items/{name}.json',{'model':{'type':'minecraft:model','model':f'yggdrasil:block/{model}'}})
write(A+'models/block/empty.json',{'textures':{'particle':'minecraft:block/stone'}})
write(A+'blockstates/waystone_part.json',{'variants':{'':{'model':'yggdrasil:block/empty'}}})
write(A+'models/block/rune_wood.json',{'parent':'minecraft:block/cube_all','textures':{'all':'minecraft:block/warped_stem'}})
write(A+'blockstates/rune_wood.json',{'variants':{'':{'model':'yggdrasil:block/rune_wood'}}})
write(A+'lang/fr_fr.json',{'block.yggdrasil.waystone':'Pierre des Royaumes','block.yggdrasil.portal':'Portail d’Yggdrasil','block.yggdrasil.portal_inactive':'Portail nordique ancien','block.yggdrasil.rune_wood':'Bois runique lumineux'})
write(A+'lang/en_us.json',{'block.yggdrasil.waystone':'Realm Waystone','block.yggdrasil.portal':'Yggdrasil Portal','block.yggdrasil.portal_inactive':'Ancient Nordic Portal','block.yggdrasil.rune_wood':'Glowing Runewood'})
# Realm travel must use one-to-one coordinates and permit sleeping in the shelter.
base={'ultrawarm':False,'natural':False,'coordinate_scale':1.0,'has_skylight':True,'has_ceiling':False,'ambient_light':0.12,'fixed_time':18000,'piglin_safe':False,'bed_works':True,'respawn_anchor_works':False,'has_raids':False,'logical_height':384,'min_y':-64,'height':384,'infiniburn':'#minecraft:infiniburn_overworld','effects':'minecraft:overworld','monster_spawn_block_light_limit':0,'monster_spawn_light_level':0}
write(D+'dimension_type/yggdrasil.json',base)
for name in ['solfara','hrimdalr','draugavik','skaldheim']:
 t=base.copy();t.pop('fixed_time');t['natural']=True
 if name=='draugavik':t['fixed_time']=18000
 write(D+f'dimension_type/{name}.json',t)
write(D+'dimension/yggdrasil.json',{'type':'yggdrasil:yggdrasil','generator':{'type':'minecraft:flat','settings':{'biome':'yggdrasil:astral','features':False,'lakes':False,'layers':[],'structure_overrides':[]}}})
biomes={'astral':(0.5,0,0x04071c,0x101437,0x112847,0x17394f),'solfara':(0.8,0.4,0xe8a856,0xd5a068,0x5d837c,0xb96526),'hrimdalr':(-0.8,0.4,0xb0d9f0,0xc3e3ef,0x486d97,0x7fbbc8),'draugavik':(0.1,0.9,0x062a34,0x0a3b42,0x071820,0x28524b),'skaldheim':(0.4,0.5,0x728b9e,0x839ba4,0x3e7180,0x6e866c)}
for name,(temp,down,sky,fog,water,grass) in biomes.items():
 features=[[] for _ in range(11)]
 if name=='solfara':features[9]=['minecraft:trees_cherry','minecraft:flower_cherry'];features[10]=['minecraft:patch_grass_plain']
 if name=='hrimdalr':features[2]=['minecraft:ice_spike'];features[10]=['minecraft:freeze_top_layer']
 if name=='skaldheim':features[9]=['minecraft:trees_old_growth_spruce_taiga'];features[10]=['minecraft:patch_grass_taiga']
 spawns={} if name=='astral' else {'creature':[{'type':'minecraft:wolf' if name in ['hrimdalr','skaldheim'] else 'minecraft:sheep','weight':8,'minCount':2,'maxCount':4}]}
 if name=='draugavik':spawns={'water_creature':[{'type':'minecraft:squid','weight':10,'minCount':1,'maxCount':3}]}
 write(D+f'worldgen/biome/{name}.json',{'has_precipitation':name in ['hrimdalr','skaldheim'],'temperature':temp,'downfall':down,'effects':{'sky_color':sky,'fog_color':fog,'water_color':water,'water_fog_color':water,'grass_color':grass,'foliage_color':grass},'spawners':spawns,'spawn_costs':{},'carvers':[] if name=='astral' else ['minecraft:cave','minecraft:canyon'],'features':features})
for name,setting in [('solfara','overworld'),('hrimdalr','overworld'),('draugavik','large_biomes'),('skaldheim','amplified')]:
 write(D+f'dimension/{name}.json',{'type':f'yggdrasil:{name}','generator':{'type':'minecraft:noise','settings':f'minecraft:{setting}','biome_source':{'type':'minecraft:fixed','biome':f'yggdrasil:{name}'}}})
