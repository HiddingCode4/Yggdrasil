"""Use the actual 26.3 registry schema, including clocks and environment attributes."""
from pathlib import Path
import zipfile,json,copy,sys
R=Path(__file__).resolve().parents[1]/'src/main/resources'
z=zipfile.ZipFile(sys.argv[1]);D=R/'data/yggdrasil'
def get(p):return json.loads(z.read('data/minecraft/'+p+'.json'))
def put(p,o):
 p=D/(p+'.json');p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(o,ensure_ascii=False,indent=2)+'\n')
base=get('dimension_type/overworld')
for name in ['yggdrasil','solfara','hrimdalr','draugavik','skaldheim']:
 t=copy.deepcopy(base);t['ambient_light']=.14;t['has_ender_dragon_fight']=False
 attrs=t['attributes'];attrs['minecraft:gameplay/nether_portal_spawns_piglin']=False
 attrs['minecraft:gameplay/bed_rule']={'can_set_spawn':'always','can_sleep':'when_dark','error_message':{'translate':'block.minecraft.bed.no_sleep'}}
 if name in ['yggdrasil','draugavik']:
  t['timelines']=[];t['has_fixed_time']=True
  attrs.update({'minecraft:visual/sky_color':'#04071c' if name=='yggdrasil' else '#062a34','minecraft:visual/fog_color':'#0a1428' if name=='yggdrasil' else '#0a3b42','minecraft:visual/star_brightness':1.0,'minecraft:visual/sun_angle':180.0,'minecraft:visual/moon_angle':0.0,'minecraft:visual/sky_light_factor':.2,'minecraft:visual/cloud_color':'#00000000'})
 if name=='solfara':t['default_clock']='yggdrasil:solfara';t['timelines']=['yggdrasil:solfara_day','yggdrasil:solfara_moon'];attrs['minecraft:visual/ambient_light_color']='#24170a'
 put('dimension_type/'+name,t)
put('world_clock/solfara',{})
for name in ['day','moon']:
 t=get('timeline/'+name);t['clock']='yggdrasil:solfara';t['period_ticks']*=2
 for track in t['tracks'].values():
  for frame in track.get('keyframes',[]):frame['ticks']*=2
 for k,v in t.get('time_markers',{}).items():
  if isinstance(v,dict):v['ticks']*=2
  else:t['time_markers'][k]=v*2
 put('timeline/solfara_'+name,t)
for name,template,sky,fog,grass in [('astral','the_void','#04071c','#0a1428','#17394f'),('solfara','cherry_grove','#e8a856','#d5a068','#b96526'),('hrimdalr','ice_spikes','#b0d9f0','#c3e3ef','#7fbbc8'),('draugavik','deep_cold_ocean','#062a34','#0a3b42','#28524b'),('skaldheim','old_growth_spruce_taiga','#728b9e','#839ba4','#6e866c')]:
 b=get('worldgen/biome/'+template);attrs=b.setdefault('attributes',{});attrs.update({'minecraft:visual/sky_color':sky,'minecraft:visual/fog_color':fog});b['effects']['grass_color']=grass;b['effects']['foliage_color']=grass
 if name=='astral':attrs.pop('minecraft:gameplay/natural_mob_spawns',None)
 if name=='draugavik':b['effects']['water_color']='#071820';attrs['minecraft:visual/water_fog_color']='#062a34'
 put('worldgen/biome/'+name,b)
# Low continental terrain produces a sea-dominated realm with islands.
n=get('worldgen/noise_settings/large_biomes');n['noise_router']['final_density']={'type':'minecraft:add','left':-.025,'right':n['noise_router']['final_density']};put('worldgen/noise_settings/draugavik',n)
p=D/'dimension/draugavik.json';v=json.loads(p.read_text());v['generator']['settings']='yggdrasil:draugavik';p.write_text(json.dumps(v,indent=2))
