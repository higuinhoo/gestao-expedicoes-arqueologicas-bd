#!/usr/bin/env python3
"""MER editável nativo do diagrams.net e prévia PDF com o mesmo posicionamento.
As posições são intencionais; o roteamento ortogonal evita o interior das entidades.
Uso: python scripts/gerar_drawio.py
"""
from pathlib import Path
import sys,math,heapq,html,xml.etree.ElementTree as ET
from collections import defaultdict
from reportlab.pdfgen import canvas
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
sys.path.insert(0,str(Path(__file__).resolve().parent))
from gerar_modelos import SCHEMA,REL,TITLE
ROOT=Path(__file__).resolve().parents[1]
FONT=Path('/usr/share/fonts/truetype/dejavu')
for alias,file in [('DV','DejaVuSans.ttf'),('DVB','DejaVuSans-Bold.ttf')]:
 pdfmetrics.registerFont(TTFont(alias,str(FONT/file)))
PALETTE={'pessoas':('#fff8e8','#a26b16'),'campo':('#eef5ff','#265f9c'),'acervo':('#edf8f5','#217061'),'laboratorio':('#f2effc','#6b50a0'),'custodia':('#fff2ec','#a85432')}
GROUP={n:('pessoas' if n in ['instituicao','pesquisador','arqueologo','conservador','analista_laboratorial','participacao_campanha'] else 'campo' if n in ['sitio_arqueologico','campanha','unidade_escavacao'] else 'acervo' if n in ['artefato','fragmento'] else 'custodia' if n in ['custodia_artefato','local_armazenamento'] else 'laboratorio') for n in SCHEMA}
TITLES=dict(TITLE,pesquisador='PESQUISADOR',fragmento='FRAGMENTO «fraca»')
RELS=[('conservador' if a=='pesquisador' and b=='intervencao_conservacao' else a,b,'realiza [RN20]' if b=='intervencao_conservacao' and a=='pesquisador' else label,ca,cb) for a,b,label,ca,cb in REL]
# Folhas com espaço entre as entidades e leitura do fluxo da esquerda para a direita.
PAGES=[
 ('MER completo',3020,2440,{
 'instituicao':(100,220),'pesquisador':(900,220),'arqueologo':(1480,220),'conservador':(1980,220),'analista_laboratorial':(2480,220),
 'sitio_arqueologico':(100,820),'campanha':(660,820),'participacao_campanha':(1220,820),'local_armazenamento':(2480,820),
 'unidade_escavacao':(660,1380),'artefato':(1220,1380),'analise_laboratorial':(1840,1380),'custodia_artefato':(2480,1380),
 'fragmento':(660,1980),'artefato_analise':(1840,1980),'intervencao_conservacao':(1220,1980)}),
 ('01 · Equipes e especialização',1660,1580,{
 'instituicao':(100,240),'pesquisador':(660,240),'arqueologo':(100,740),'conservador':(660,740),'analista_laboratorial':(1220,740),
 'participacao_campanha':(660,1200),'campanha':(1220,1200)}),
 ('02 · Campo e acervo',1660,1380,{
 'sitio_arqueologico':(100,260),'campanha':(660,260),'unidade_escavacao':(1220,260),'artefato':(660,880),'fragmento':(1220,880)}),
 ('03 · Análises e conservação',1660,1760,{
 'instituicao':(100,240),'analise_laboratorial':(1220,240),'artefato':(660,840),'artefato_analise':(1220,840),
 'conservador':(100,1380),'intervencao_conservacao':(660,1380)}),
 ('04 · Cadeia de custódia',1660,1740,{
 'instituicao':(100,240),'local_armazenamento':(1220,240),'custodia_artefato':(660,840),'artefato':(100,1380),'pesquisador':(1220,1380)})]

def fields(name):
 return [c['name'] for c in SCHEMA[name]['columns'] if not c['derived'] and (c['name'] not in SCHEMA[name]['fks'] or c['name'] in SCHEMA[name]['pk'])]

def height(name):return 48+len(fields(name))*24+(38 if name=='pesquisador' else 14)

def inside(p,r):return r[0]+.01<p[0]<r[2]-.01 and r[1]+.01<p[1]<r[3]-.01

def obstructed(a,b,obstacles):
 if abs(a[0]-b[0])<.01:
  x=a[0];y0,y1=sorted((a[1],b[1]))
  return any(r[0]+.01<x<r[2]-.01 and max(y0,r[1])<min(y1,r[3])-.01 for r in obstacles)
 y=a[1];x0,x1=sorted((a[0],b[0]))
 return any(r[1]+.01<y<r[3]-.01 and max(x0,r[0])<min(x1,r[2])-.01 for r in obstacles)

def route(start,end,rects,width,height,old):
 obstacles=[(x-24,y-24,x+w+24,y+h+24) for x,y,w,h in rects.values()]
 xs=sorted(set([45,width-45,start[0],end[0]]+[v for r in obstacles for v in (r[0],r[2])]))
 ys=sorted(set([165,height-55,start[1],end[1]]+[v for r in obstacles for v in (r[1],r[3])]))
 si=(xs.index(start[0]),ys.index(start[1]));ei=(xs.index(end[0]),ys.index(end[1]))
 heap=[(0,0,si[0],si[1],-1)];best={(si[0],si[1],-1):0};prev={};finish=None
 while heap:
  _,cost,i,j,d=heapq.heappop(heap);state=(i,j,d)
  if cost!=best.get(state):continue
  if (i,j)==ei:finish=state;break
  for di,dj,nd in [(1,0,0),(-1,0,0),(0,1,1),(0,-1,1)]:
   ni,nj=i+di,j+dj
   if not(0<=ni<len(xs) and 0<=nj<len(ys)):continue
   a=(xs[i],ys[j]);b=(xs[ni],ys[nj])
   if any(inside(b,r) for r in obstacles) or obstructed(a,b,obstacles):continue
   dist=abs(a[0]-b[0])+abs(a[1]-b[1]);extra=40 if d>=0 and nd!=d else 0
   # Prefere vias separadas, sem impedir cruzamentos inevitáveis.
   for u,v in old:
    if nd==0 and abs(u[0]-v[0])<.1 and min(a[0],b[0])<u[0]<max(a[0],b[0]) and min(u[1],v[1])<a[1]<max(u[1],v[1]):extra+=55
    if nd==1 and abs(u[1]-v[1])<.1 and min(a[1],b[1])<u[1]<max(a[1],b[1]) and min(u[0],v[0])<a[0]<max(u[0],v[0]):extra+=55
   nc=cost+dist+extra;ns=(ni,nj,nd)
   if nc<best.get(ns,float('inf')):
    best[ns]=nc;prev[ns]=state;heur=abs(b[0]-end[0])+abs(b[1]-end[1]);heapq.heappush(heap,(nc+heur,nc,ni,nj,nd))
 assert finish is not None,('não foi possível rotear',start,end)
 points=[];p=finish
 while True:
  points.append((xs[p[0]],ys[p[1]]))
  if p not in prev:break
  p=prev[p]
 points.reverse();return points

def simplify(points):
 out=[]
 for p in points:
  if out and p==out[-1]:continue
  while len(out)>=2 and ((out[-2][0]==out[-1][0]==p[0]) or (out[-2][1]==out[-1][1]==p[1])):out.pop()
  out.append(p)
 return out

def anchor(rect,side,fraction):
 x,y,w,h=rect
 if side=='e':return (x+w,y+h*fraction),(x+w+55,y+h*fraction),(1,fraction)
 if side=='w':return (x,y+h*fraction),(x-55,y+h*fraction),(0,fraction)
 if side=='s':return (x+w*fraction,y+h),(x+w*fraction,y+h+55),(fraction,1)
 return (x+w*fraction,y),(x+w*fraction,y-55),(fraction,0)

def at_length(points,distance):
 for a,b in zip(points,points[1:]):
  size=math.dist(a,b)
  if distance<=size:
   t=distance/size if size else 0
   return (a[0]+(b[0]-a[0])*t,a[1]+(b[1]-a[1])*t),(b[0]-a[0],b[1]-a[1])
  distance-=size
 return points[-1],(1,0)

def cell(root,id,value,style,vertex=False,edge=False,parent='1',source=None,target=None,geo=None):
 attrs={'id':id,'value':value,'style':style,'parent':parent}
 if vertex:attrs['vertex']='1'
 if edge:attrs['edge']='1'
 if source:attrs['source']=source
 if target:attrs['target']=target
 c=ET.SubElement(root,'mxCell',attrs)
 if geo is not None:ET.SubElement(c,'mxGeometry',{**{k:str(v) for k,v in geo.items()},'as':'geometry'})
 return c

file=ET.Element('mxfile',{'host':'app.diagrams.net','agent':'MER editável gerado a partir do esquema autoral','version':'24.7.17','type':'device'})
preview=canvas.Canvas(str(ROOT/'docs/mer-drawio-previa.pdf'),pagesize=(2383.94,1683.78),pageCompression=1)
all_routes=[]
for page_index,(name,pw,ph,positions) in enumerate(PAGES):
 rects={n:(x,y,360,height(n)) for n,(x,y) in positions.items()}
 # Folhas de detalhe podem ganhar altura para manter as margens.
 ph=max(ph,max(y+h for x,y,w,h in rects.values())+100)
 diagram=ET.SubElement(file,'diagram',{'id':f'mer-{page_index}','name':name})
 model=ET.SubElement(diagram,'mxGraphModel',{'dx':'1600','dy':'1000','grid':'1','gridSize':'10','guides':'1','tooltips':'1','connect':'1','arrows':'1','fold':'1','page':'1','pageScale':'1','pageWidth':str(pw),'pageHeight':str(ph),'math':'0','shadow':'0'})
 root=ET.SubElement(model,'root');ET.SubElement(root,'mxCell',{'id':'0'});ET.SubElement(root,'mxCell',{'id':'1','parent':'0'})
 title='GESTÃO DE EXPEDIÇÕES ARQUEOLÓGICAS'
 subtitle='MER completo · visão de conjunto' if page_index==0 else name
 for k,text,y,size in [('title',title,32,29),('subtitle',subtitle,78,21),('legend','Identificadores sublinhados  •  (mín,máx) em cada ponta  •  T,X = especialização total e exclusiva',118,15)]:
  cell(root,k,text,f'text;html=1;align=left;verticalAlign=middle;whiteSpace=wrap;fontFamily=Helvetica;fontSize={size};fontColor=#21354A;fontStyle={1 if k=="title" else 0};',vertex=True,geo={'x':80,'y':y,'width':pw-160,'height':36})
 # Entidades: cabeçalho e cada atributo são objetos nativos editáveis.
 for n,(x,y,w,h) in rects.items():
  fill,stroke=PALETTE[GROUP[n]]
  cell(root,n,TITLES[n],f'swimlane;html=1;startSize=44;horizontal=1;rounded=0;collapsible=0;fillColor={fill};swimlaneFillColor=#FFFFFF;strokeColor={stroke};strokeWidth=1.5;fontFamily=Helvetica;fontSize=17;fontStyle=1;fontColor=#21354A;align=left;spacingLeft=14;',vertex=True,geo={'x':x,'y':y,'width':w,'height':h})
  for i,attribute in enumerate(fields(n)):
   label='<u>'+attribute+'</u>' if attribute in SCHEMA[n]['pk'] else attribute
   cell(root,f'{n}-{attribute}',label,'text;html=1;align=left;verticalAlign=middle;whiteSpace=wrap;fontFamily=Helvetica;fontSize=15;fontColor=#334155;spacingLeft=14;connectable=0;',vertex=True,parent=n,geo={'x':0,'y':46+24*i,'width':w,'height':24})
  if n=='pesquisador':
   cell(root,n+'-tx','Especialização total e exclusiva (T,X)','text;html=1;align=left;fontFamily=Helvetica;fontSize=13;fontStyle=1;fontColor=#925F12;spacingLeft=14;connectable=0;',vertex=True,parent=n,geo={'x':0,'y':h-35,'width':w,'height':26})
  if n=='fragmento':
   cell(root,n+'-weak','',f'rounded=0;fillColor=none;strokeColor={stroke};pointerEvents=0;connectable=0;',vertex=True,parent=n,geo={'x':4,'y':4,'width':w-8,'height':h-8})
 routes=[];used=defaultdict(int);oldsegments=[]
 for index,(a,b,label,ca,cb) in enumerate(RELS):
  if a not in rects or b not in rects:continue
  ra,rb=rects[a],rects[b];ax,ay,aw,ah=ra;bx,by,bw,bh=rb
  if a==b:
   p1,o1,sport=anchor(ra,'w',.35);p2,o2,tport=anchor(rb,'w',.7)
   points=[p1,o1,(ax-85,o1[1]),(ax-85,o2[1]),o2,p2]
  else:
   dx=(bx+bw/2)-(ax+aw/2);dy=(by+bh/2)-(ay+ah/2)
   if label=='ISA' and page_index>0:sa,sb='s','n'
   elif abs(dx)>abs(dy)*.8:sa,sb=('e','w') if dx>0 else ('w','e')
   else:sa,sb=('s','n') if dy>0 else ('n','s')
   fractions=[.5,.25,.75,.12,.88]
   fa=fractions[min(used[(a,sa)],4)];used[(a,sa)]+=1
   fb=fractions[min(used[(b,sb)],4)];used[(b,sb)]+=1
   p1,o1,sport=anchor(ra,sa,fa);p2,o2,tport=anchor(rb,sb,fb)
   points=[p1]+route(o1,o2,rects,pw,ph,oldsegments)+[p2]
  points=simplify(points);oldsegments.extend(zip(points,points[1:]));length=sum(math.dist(u,v) for u,v in zip(points,points[1:]));eid=f'rel-{index}'
  markers={'1,1':'ERmandOne','0,1':'ERzeroToOne','0,N':'ERzeroToMany'}
  color='#A26B16' if label=='ISA' else '#64748B'
  style=f'edgeStyle=segmentEdgeStyle;html=1;rounded=0;orthogonalLoop=1;jettySize=auto;strokeColor={color};strokeWidth=1.5;fontFamily=Helvetica;fontSize=14;fontColor=#334155;labelBackgroundColor=#FFFFFF;jumpStyle=arc;jumpSize=9;startArrow={markers[ca]};endArrow={markers[cb]};startFill=0;endFill=0;exitX={sport[0]};exitY={sport[1]};exitDx=0;exitDy=0;entryX={tport[0]};entryY={tport[1]};entryDx=0;entryDy=0;'+('dashed=1;' if label=='ISA' else '')
  edge=cell(root,eid,'',style,edge=True,source=a,target=b)
  geo=ET.SubElement(edge,'mxGeometry',{'relative':'1','as':'geometry'})
  array=ET.SubElement(geo,'Array',{'as':'points'})
  for px,py in points[1:-1]:ET.SubElement(array,'mxPoint',{'x':str(px),'y':str(py)})
  for suffix,value,fraction,offset in [('from','('+ca+')',min(65/length,.25),-18),('to','('+cb+')',1-min(65/length,.25),-18),('name',label,.5,18)]:
   labelcell=cell(root,eid+'-'+suffix,value,'edgeLabel;html=1;align=center;verticalAlign=middle;resizable=1;points=[];fontFamily=Helvetica;fontSize=15;fontColor=#334155;labelBackgroundColor=#FFFFFF;connectable=0;',vertex=True,parent=eid,geo={'x':2*fraction-1,'y':offset,'width':max(68,len(value)*8+12),'height':26,'relative':1})
   ET.SubElement(labelcell.find('mxGeometry'),'mxPoint',{'x':str(-max(68,len(value)*8+12)/2),'y':'-13','as':'offset'})
  routes.append((a,b,label,ca,cb,points))
 cell(root,'footer','As abas de detalhe repetem entidades de referência. Nenhum atributo físico gerado é tratado como fato conceitual independente.','text;html=1;align=left;fontFamily=Helvetica;fontSize=14;fontColor=#64748B;',vertex=True,geo={'x':80,'y':ph-55,'width':pw-160,'height':25})
 # Prévia em PDF, desenhada da mesma geometria e sem rasterizar as entidades.
 paper=(2383.94,1683.78) if page_index==0 else (1190.55,1683.78)
 preview.setPageSize(paper);scale=min((paper[0]-56)/pw,(paper[1]-56)/ph);ox=(paper[0]-pw*scale)/2;oy=(paper[1]-ph*scale)/2
 def point(p):return ox+p[0]*scale,oy+(ph-p[1])*scale
 def text_at(text,x,y,size=15,font='DV',color='#334155',align='left'):
  preview.setFillColor(color);preview.setFont(font,size*scale)
  px,py=point((x,y));(preview.drawCentredString if align=='center' else preview.drawString)(px,py,text)
 def box_text(text,p,size=15,offset=0):
  x,y=p;w=pdfmetrics.stringWidth(text,'DV',size)+14
  px,py=point((x-w/2,y-12+offset));preview.setFillColor('#FFFFFF');preview.rect(px,py-20*scale,w*scale,23*scale,fill=1,stroke=0)
  text_at(text,x,y+5+offset,size,align='center')
 text_at(title,80,61,29,'DVB','#21354A');text_at(subtitle,80,105,21,'DV','#21354A');text_at('Identificadores sublinhados · (mín,máx) nas duas pontas · T,X = total e exclusiva',80,145,15)
 for a,b,label,ca,cb,points in routes:
  preview.setStrokeColor('#A26B16' if label=='ISA' else '#8290A2');preview.setLineWidth(1.4*scale);preview.setDash([6*scale,4*scale] if label=='ISA' else [])
  path=preview.beginPath();path.moveTo(*point(points[0]))
  for p in points[1:]:path.lineTo(*point(p))
  preview.drawPath(path);preview.setDash([])
  # Marcadores de pé de galinha equivalentes aos do arquivo nativo.
  for card,origin,neighbor in [(ca,points[0],points[1]),(cb,points[-1],points[-2])]:
   distance=math.dist(origin,neighbor);ux=(neighbor[0]-origin[0])/distance;uy=(neighbor[1]-origin[1])/distance
   def marker(d,side=0):return point((origin[0]+ux*d-uy*side,origin[1]+uy*d+ux*side))
   preview.setStrokeColor('#A26B16' if label=='ISA' else '#64748B');preview.setLineWidth(1.3*scale)
   if card=='0,N':
    for side in [-8,0,8]:preview.line(*marker(22),*marker(4,side))
   else:
    preview.line(*marker(10,-8),*marker(10,8))
    if card=='1,1':preview.line(*marker(18,-8),*marker(18,8))
   if card.startswith('0'):
    center=marker(33 if card=='0,N' else 24);preview.setFillColor('#FFFFFF');preview.circle(*center,5*scale,stroke=1,fill=1)
  length=sum(math.dist(u,v) for u,v in zip(points,points[1:]))
  for card,d in [(ca,min(65,length*.25)),(cb,length-min(65,length*.25))]:
   p,direction=at_length(points,d);offset=(0,-22) if abs(direction[0])>abs(direction[1]) else (40,0)
   box_text('('+card+')',(p[0]+offset[0],p[1]+offset[1]),15)
  p,direction=at_length(points,length*.5);offset=(0,22) if abs(direction[0])>abs(direction[1]) else (-80,0)
  box_text(label,(p[0]+offset[0],p[1]+offset[1]),14)
 for n,(x,y,w,h) in rects.items():
  fill,stroke=PALETTE[GROUP[n]];px,py=point((x,y+h));preview.setFillColor('#FFFFFF');preview.setStrokeColor(stroke);preview.setLineWidth(1.3*scale);preview.rect(px,py,w*scale,h*scale,fill=1,stroke=1)
  hx,hy=point((x,y+44));preview.setFillColor(fill);preview.rect(hx,hy,w*scale,44*scale,fill=1,stroke=0)
  text_at(TITLES[n],x+14,y+29,17,'DVB','#21354A')
  for i,attribute in enumerate(fields(n)):
   yy=y+64+24*i;text_at(attribute,x+14,yy,15)
   if attribute in SCHEMA[n]['pk']:
    u,v=point((x+14,yy+2));preview.setStrokeColor('#334155');preview.setLineWidth(.7*scale);preview.line(u,v,u+pdfmetrics.stringWidth(attribute,'DV',15)*scale,v)
  if n=='pesquisador':text_at('Especialização total e exclusiva (T,X)',x+14,y+h-17,13,'DVB','#925F12')
  if n=='fragmento':
   ix,iy=point((x+4,y+h-4));preview.setStrokeColor(stroke);preview.rect(ix,iy,(w-8)*scale,(h-8)*scale,fill=0,stroke=1)
 text_at('Prévia do arquivo editável do diagrams.net · '+str(page_index+1)+' / '+str(len(PAGES)),80,ph-35,14)
 preview.showPage();all_routes.append((name,rects,routes))
preview.save()
ET.indent(file,space='  ')
path=ROOT/'docs/fontes/mer-conceitual.drawio';ET.ElementTree(file).write(path,encoding='utf-8',xml_declaration=True)
# Validação estrutural e geométrica do artefato editável.
read=ET.parse(path);assert len(read.findall('diagram'))==5
for name,rects,routes in all_routes:
 assert len(routes)==sum(a in rects and b in rects for a,b,*_ in RELS)
 for a,b,_,_,_,points in routes:
  for p,q in zip(points,points[1:]):
   assert p[0]==q[0] or p[1]==q[1],(name,'segmento não ortogonal')
   obstacles=[(x,y,x+w,y+h) for n,(x,y,w,h) in rects.items() if n not in (a,b)]
   assert not obstructed(p,q,obstacles),(name,a,b,'linha atravessa entidade')
 print(name,':',len(rects),'entidades;',len(routes),'relacionamentos; roteamento OK')
print(path)
