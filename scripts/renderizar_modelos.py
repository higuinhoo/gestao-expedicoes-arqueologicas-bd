#!/usr/bin/env python3
"""Renderiza modelos com PlantUML 1.2025.2 + Graphviz; exporta SVG, PDF e PNG.
Uso: PLANTUML_JAR=/caminho/plantuml.jar python scripts/gerar_modelos.py
"""
from pathlib import Path
import os,re,subprocess,io,math,xml.etree.ElementTree as ET
import cairosvg,fitz
from pypdf import PdfReader,PdfWriter,Transformation
from reportlab.pdfgen import canvas
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from gerar_modelos import ROOT,SCHEMA,TITLE,REL
OUT=ROOT/'docs/diagramas';SRC=ROOT/'docs/fontes/plantuml'
NAMES={n:TITLE[n].replace(' · T,X','').replace(' · FRACA','') for n in SCHEMA}
# Cardinalidades de Chen: participação de uma instância da entidade naquele relacionamento.
# Totalidade/exclusividade é representada nativamente por =>= d.
CR=[
 ('instituicao','pesquisador','Vincula','0,N','1,1','vincula',[]),
 ('pesquisador','campanha','Participa','0,N','0,N','participa',['funcao','data_entrada','data_saida','horas_previstas']),
 ('sitio_arqueologico','campanha','Recebe','0,N','1,1','recebe',[]),
 ('arqueologo','campanha','Coordena','0,N','1,1','coordena',[]),
 ('campanha','unidade_escavacao','Possui','0,N','1,1','unidades',[]),
 ('unidade_escavacao','artefato','Revela','0,N','1,1','revela',[]),
 ('artefato','artefato','Referencia','0,1','0,N','referencia',[]),
 ('artefato','fragmento','Compoe','0,N','1,1','compoe',[]),
 ('instituicao','analise_laboratorial','Executa','0,N','1,1','executa',[]),
 ('artefato','analise_laboratorial','É analisado em','0,N','0,N','examina',['codigo_amostra','massa_consumida_g','resultado_resumido','conclusao']),
 ('conservador','intervencao_conservacao','Realiza','0,N','1,1','realiza',[]),
 ('artefato','intervencao_conservacao','Recebe','0,N','1,1','intervencoes',[]),
 ('instituicao','local_armazenamento','Possui','0,N','1,1','locais',[]),
 ('artefato','custodia_artefato','Tem histórico','0,N','1,1','historico',[]),
 ('instituicao','custodia_artefato','Assume','0,N','1,1','assume',[]),
 ('local_armazenamento','custodia_artefato','Abriga','0,N','0,1','abriga',[]),
 ('pesquisador','custodia_artefato','Responsabiliza-se','0,N','1,1','responsavel',[]),
]
CONCEPT=[n for n in SCHEMA if n not in ('participacao_campanha','artefato_analise')]
GROUPS=[
 ('01-equipes','Equipes e especialização',['instituicao','pesquisador','arqueologo','conservador','analista_laboratorial','campanha','participacao_campanha']),
 ('02-campo','Campo e descoberta',['sitio_arqueologico','campanha','unidade_escavacao','artefato','fragmento']),
 ('03-analises','Análises e conservação',['instituicao','artefato','analise_laboratorial','artefato_analise','conservador','intervencao_conservacao']),
 ('04-custodia','Histórico de custódia',['instituicao','pesquisador','local_armazenamento','artefato','custodia_artefato'])]
SKIN='''skinparam backgroundColor #FFFFFF
skinparam shadowing false
skinparam defaultFontName DejaVu Sans
skinparam defaultFontSize 18
skinparam nodesep 65
skinparam ranksep 85
skinparam ArrowColor #3A4653
skinparam ArrowThickness 1.3
skinparam EntityBackgroundColor #FFFFFF
skinparam EntityBorderColor #223447
skinparam RelationshipBackgroundColor #F1F4F7
skinparam RelationshipBorderColor #223447
skinparam AttributeBackgroundColor #FFFFFF
skinparam AttributeBorderColor #718096
'''

def concept(names,title,detail):
 lines=['@startchen',SKIN]
 for name in CONCEPT:
  if name not in names:continue
  weak=' <<weak>>' if name=='fragmento' else ''
  lines.append(f'entity "{NAMES[name]}" as {name}{weak} {{')
  attrs=[c for c in SCHEMA[name]['columns'] if not c['derived'] and c['name'] not in SCHEMA[name]['fks']]
  if name in ['arqueologo','conservador','analista_laboratorial']:
   attrs=[c for c in attrs if c['name']!='id_pesquisador']
  # Discriminador implementa a especialização; a generalização já expressa o conceito.
  attrs=[c for c in attrs if c['name']!='tipo_especializacao']
  if not detail:attrs=[]
  for c in attrs:
   key=' <<key>>' if c['name'] in SCHEMA[name]['pk'] else ''
   lines.append(f'  "{c["name"]}" as {c["name"]}{key}')
  lines.append('}')
 if all(n in names for n in ['pesquisador','arqueologo','conservador','analista_laboratorial']):
  lines.append('pesquisador =>= d { arqueologo, conservador, analista_laboratorial }')
 for a,b,label,ca,cb,alias,attrs in CR:
  if a not in names or b not in names:continue
  identifying=' <<identifying>>' if alias=='compoe' else ''
  lines.append(f'relationship "{label}" as r_{alias}{identifying} {{')
  if detail:
   for attr in attrs:lines.append('  '+attr)
  lines.append('}')
  # Numa autorrelação os papéis são explicitados no guia, mantendo duas participações.
  lines.append(f'{a} -({ca})- r_{alias}')
  participation='=' if alias=='compoe' else '-'
  lines.append(f'r_{alias} {participation}({cb}){participation} {b}')
 lines.append('@endchen')
 return '\n'.join(lines)+'\n'

def logical(names,title,detail):
 lines=['@startuml','top to bottom direction',SKIN,'hide circle','hide methods','skinparam linetype ortho','skinparam classAttributeIconSize 0','skinparam classBackgroundColor #FFFFFF','skinparam classBorderColor #223447','skinparam classFontColor #172B3A','skinparam classHeaderBackgroundColor #EAF0F5']
 for name,meta in SCHEMA.items():
  if name not in names:continue
  lines.append(f'entity "{name}" as {name} {{')
  columns=meta['columns'] if detail else [c for c in meta['columns'] if c['name'] in meta['pk'] or c['name'] in meta['fks']]
  previous=None
  for c in columns:
   pk=c['name'] in meta['pk'];fk=c['name'] in meta['fks'];kind='key' if pk else 'field'
   if previous=='key' and not pk:lines.append('  --')
   previous=kind
   text=c['name'];text='<u>'+text+'</u>' if pk else text
   flags=' [PK/FK]' if pk and fk else ' [PK]' if pk else ' [FK]' if fk else ' [gerado]' if c['derived'] else ''
   required='* ' if c['required'] or pk else '  '
   lines.append('  '+required+text+' : '+c['type']+flags)
  lines.append('}')
 # FKs extraídas do DDL: evita confundir a restrição semântica com a referência física.
 ddl=(ROOT/'sql/01_ddl.sql').read_text()
 for name,body in re.findall(r'CREATE TABLE (\w+) \((.*?)\n\) ENGINE',ddl,re.S):
  if name not in names:continue
  for fk,col,parent,parentcol in re.findall(r'CONSTRAINT (fk_\w+) FOREIGN KEY \(([^)]+)\)\s+REFERENCES (\w+) \(([^)]+)\)',body):
   if parent not in names:continue
   c=next(c for c in SCHEMA[name]['columns'] if c['name']==col)
   left='||' if c['required'] or col in SCHEMA[name]['pk'] else '|o'
   right='o|' if name in ['arqueologo','conservador','analista_laboratorial'] else 'o{'
   lines.append(f'{parent} {left}--{right} {name} : {col}')
 lines.append('@enduml')
 return '\n'.join(lines)+'\n'

def write_source(kind,stem,title,names,detail):
 path=SRC/kind/(stem+'.puml');path.parent.mkdir(parents=True,exist_ok=True)
 path.write_text((concept if kind=='conceitual' else logical)(names,title,detail));return path

def render(path,jar):
 env=os.environ.copy();env['PLANTUML_LIMIT_SIZE']='24000'
 subprocess.run(['java','-Djava.awt.headless=true','-jar',str(jar),'-charset','UTF-8','-tsvg',str(path)],check=True,env=env,capture_output=True,text=True)
 svg=path.with_suffix('.svg');tree=ET.parse(svg);root=tree.getroot();texts=[''.join(e.itertext()) for e in root.iter() if e.tag.endswith('text')]
 assert not any('Syntax Error' in t for t in texts),(path,texts)
 return svg

def package(kind,files):
 folder=OUT/kind;folder.mkdir(parents=True,exist_ok=True);writer=PdfWriter();pages=[]
 for i,(path,title,svg) in enumerate(files):
  data=svg.read_bytes();(folder/(path.stem+'.svg')).write_bytes(data)
  # Cairo gera uma página vetorial diretamente a partir da saída do PlantUML.
  buf=io.BytesIO();cairosvg.svg2pdf(bytestring=data,write_to=buf,background_color='#FFFFFF');buf.seek(0)
  source=PdfReader(buf).pages[0];w,h=float(source.mediabox.width),float(source.mediabox.height)
  # Seleção da folha para impressão: maximiza área útil; detalhes a partir de A3.
  options=[(1190.55,841.89),(1683.78,1190.55),(2383.94,1683.78),(3370.39,2383.94)]
  if i==0:options=options[2:]
  sizes=[(a,b) for a,b in options for a,b in [(a,b),(b,a)]]
  selected=None
  for area in sorted(set(a*b for a,b in sizes)):
   candidates=[(min((a-64)/w,(b-142)/h),a,b) for a,b in sizes if abs(a*b-area)<.1]
   best=max(candidates)
   # SVG: fonte 18px -> PDF 13,5pt; busca pelo menos 10pt em impressão.
   if best[0]*13.5>=10 or area==max(a*b for a,b in sizes):selected=best;break
  scale,pw,ph=selected;page=writer.add_blank_page(pw,ph)
  page.merge_transformed_page(source,Transformation().scale(scale).translate((pw-w*scale)/2,60+(ph-142-h*scale)/2))
  overlay=io.BytesIO();cv=canvas.Canvas(overlay,pagesize=(pw,ph));cv.setFillColor('#172B3A');cv.setFont('Helvetica-Bold',20);cv.drawString(32,ph-36,'GESTÃO DE EXPEDIÇÕES ARQUEOLÓGICAS');cv.setFont('Helvetica',14);cv.drawString(32,ph-60,title)
  cv.setFillColor('#46576A');cv.setFont('Helvetica',10)
  legend='Chen: retângulo = entidade; losango = relacionamento; elipse = atributo; d + linha dupla = especialização total/exclusiva.' if kind=='conceitual' else 'PK sublinhada; FK identificada; * obrigatório. Ligações correspondem às chaves estrangeiras do DDL.'
  cv.drawString(32,ph-78,legend)
  cv.drawString(32,26,f'{i+1} / {len(files)} · '+('Na visão geral, atributos omitidos são detalhados nas folhas seguintes.' if i==0 else 'Recorte do modelo completo; entidades repetidas servem de referência.'))
  cv.save();overlay.seek(0);page.merge_page(PdfReader(overlay).pages[0]);pages.append((pw,ph,scale))
 filename='mer-conceitual' if kind=='conceitual' else 'modelo-logico'
 target=ROOT/'docs'/f'{filename}.pdf';writer.add_metadata({'/Title':filename+' — Gestão de Expedições Arqueológicas','/Creator':'PlantUML 1.2025.2 / Graphviz / CairoSVG'});writer.write(target)
 doc=fitz.open(target)
 for i,page in enumerate(doc):
  # PNG final em alta resolução, pronto para inserir em documentos ou capturar a tela.
  longest=6200 if i==0 else 4000;scale=longest/max(page.rect.width,page.rect.height)
  page.get_pixmap(matrix=fitz.Matrix(scale,scale),alpha=False).save(folder/(files[i][0].stem+'.png'))
  print(kind,files[i][0].stem,'folha',round(page.rect.width),round(page.rect.height),'escala',round(pages[i][2],2))
 return target

def main():
 jar=Path(os.environ.get('PLANTUML_JAR','/tmp/plantuml.jar'))
 if not jar.is_file():raise SystemExit('Defina PLANTUML_JAR com o caminho do plantuml-1.2025.2.jar. Consulte README.md.')
 OUT.mkdir(exist_ok=True);sources={}
 for kind in ['conceitual','logico']:
  allnames=CONCEPT if kind=='conceitual' else list(SCHEMA)
  sources[kind]=[(write_source(kind,'00-visao-geral','Visão geral',allnames,False),'MER conceitual · visão geral' if kind=='conceitual' else 'Modelo lógico · visão geral das chaves')]
  # Uma folha completa garante que todos os atributos sejam visíveis no modelo lógico.
  if kind=='logico':sources[kind].append((write_source(kind,'01-esquema-completo','Esquema completo',allnames,True),'Modelo lógico · esquema completo com todos os atributos'))
  for stem,title,names in GROUPS:
   sources[kind].append((write_source(kind,stem if kind=='conceitual' else str(int(stem[:2])+1).zfill(2)+stem[2:],title,names,True),('MER conceitual · ' if kind=='conceitual' else 'Modelo lógico · ')+title))
 for kind,items in sources.items():
  rendered=[]
  for path,title in items:rendered.append((path,title,render(path,jar)))
  package(kind,rendered)
 # Não deixa saída intermediária misturada às fontes.
 for p in SRC.rglob('*.svg'):p.unlink()

if __name__=='__main__':main()
