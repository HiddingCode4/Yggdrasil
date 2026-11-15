"""Convert embedded GLB meshes to NeoForge OBJ assets. Keep original inputs intact."""
from pathlib import Path
import json, struct, io, shutil
import numpy as np
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'src/main/resources/assets/yggdrasil'

def convert(path,name,height):
 b=path.read_bytes(); size,typ=struct.unpack_from('<II',b,12); j=json.loads(b[20:20+size]); off=20+size; bs,bt=struct.unpack_from('<II',b,off); binary=b[off+8:off+8+bs]
 def accessor(i):
  a=j['accessors'][i];v=j['bufferViews'][a['bufferView']]; count={'SCALAR':1,'VEC2':2,'VEC3':3,'VEC4':4}[a['type']];dtype={5126:'<f4',5125:'<u4',5123:'<u2',5121:'u1'}[a['componentType']];dt=np.dtype(dtype)
  return np.ndarray((a['count'],count),dtype=dt,buffer=binary,offset=v.get('byteOffset',0)+a.get('byteOffset',0),strides=(v.get('byteStride',count*dt.itemsize),dt.itemsize)).copy()
 # These supplied exports use a single mesh; apply the scene node transform explicitly.
 node=next(n for n in j['nodes'] if 'mesh' in n); mesh=j['meshes'][node['mesh']]; matrix=np.array(node.get('matrix',np.eye(4).T.flatten()),float).reshape((4,4)).T
 if 'matrix' not in node:
  matrix[:3,3]=node.get('translation',[0,0,0]);matrix[:3,:3]=np.diag(node.get('scale',[1,1,1]));q=node.get('rotation',[0,0,0,1]);x,y,z,w=q;rot=np.array([[1-2*y*y-2*z*z,2*x*y-2*z*w,2*x*z+2*y*w],[2*x*y+2*z*w,1-2*x*x-2*z*z,2*y*z-2*x*w],[2*x*z-2*y*w,2*y*z+2*x*w,1-2*x*x-2*y*y]]);matrix[:3,:3]=rot@matrix[:3,:3]
 prim=mesh['primitives'][0]; pos=accessor(prim['attributes']['POSITION']);pos=(matrix@np.c_[pos,np.ones(len(pos))].T).T[:,:3];lo=pos.min(0);hi=pos.max(0);scale=height/(hi[1]-lo[1]);pos=(pos-np.array([(lo[0]+hi[0])/2,lo[1],(lo[2]+hi[2])/2]))*scale+np.array([.5,0,.5]);uv=accessor(prim['attributes']['TEXCOORD_0']);inds=accessor(prim['indices']).reshape((-1,3));normal=accessor(prim['attributes']['NORMAL'])@np.linalg.inv(matrix[:3,:3]); length=np.linalg.norm(normal,axis=1); normal[length==0]=[0,1,0]; length[length==0]=1; normal/=length[:,None]
 model=OUT/'models/block';model.mkdir(parents=True,exist_ok=True);tex=OUT/'textures/block';tex.mkdir(parents=True,exist_ok=True)
 mat=j['materials'][prim.get('material',0)];ti=mat['pbrMetallicRoughness']['baseColorTexture']['index'];im=j['images'][j['textures'][ti]['source']];bv=j['bufferViews'][im['bufferView']];img=Image.open(io.BytesIO(binary[bv.get('byteOffset',0):bv.get('byteOffset',0)+bv['byteLength']]));img=img.resize((2048,2048),Image.Resampling.LANCZOS);img.save(tex/f'{name}.png')
 lines=[f'mtllib {name}.mtl','o '+name];lines += ['v '+' '.join(f'{v:.6f}' for v in p) for p in pos];lines += [f'vt {u:.6f} {v:.6f}' for u,v in uv];lines += ['vn '+' '.join(f'{v:.6f}' for v in n) for n in normal];lines+=['usemtl surface'];lines += ['f '+' '.join(f'{i+1}/{i+1}/{i+1}' for i in face) for face in inds];(model/f'{name}.obj').write_text('\n'.join(lines));(model/f'{name}.mtl').write_text(f'newmtl surface\nKd 1 1 1\nmap_Kd #surface\n')
 (model/f'{name}.json').write_text(json.dumps({'loader':'neoforge:obj','model':f'yggdrasil:models/block/{name}.obj','flip_v':False,'automatic_culling':False,'shade_quads':True,'textures':{'surface':f'yggdrasil:block/{name}','particle':'#surface'}}))
 print(name,len(inds),'triangles; dimensions',((hi-lo)*scale).round(2))
if __name__=='__main__':
 source=ROOT.parent/'upload'
 for file,name,h in [('Waystone iggdrasil.glb','waystone',5),('Portal 1.glb','portal_inactive',5),('portal 2.glb','portal_active',5)]:convert(source/file,name,h)
 target=OUT/'textures/gui';target.mkdir(parents=True,exist_ok=True);shutil.copyfile(source/'Map(1).png',target/'realms.png')
