#!/usr/bin/env python3
"""Regera fontes Mermaid e DOT e PDFs vetoriais a partir do DDL autoral.
Requer Python 3, Graphviz e pypdf. Não cria nem altera tabelas no banco.
"""
from pathlib import Path
import re,html,subprocess
from pypdf import PdfReader,PdfWriter,Transformation
ROOT=Path(__file__).resolve().parents[1]
DDL=(ROOT/'sql/01_ddl.sql').read_text()
SCHEMA={}
for m in re.finditer(r'CREATE TABLE (\w+) \((.*?)\n\) ENGINE',DDL,re.S):
 name,body=m.groups();cols=[]
 for line in body.splitlines():
  c=re.match(r'    (\w+) ((?:INT|SMALLINT|DATE|TEXT|BOOLEAN)\b|(?:VARCHAR|CHAR|DECIMAL)\([^)]*\))(.*)',line)
  if c:cols.append({'name':c[1],'type':c[2],'required':'NOT NULL' in c[3],'derived':'GENERATED' in c[3]})
 pk=re.search(r'CONSTRAINT pk_\w+ PRIMARY KEY \(([^)]+)\)',body)[1].split(', ')
 fks=re.findall(r'CONSTRAINT fk_\w+ FOREIGN KEY \(([^)]+)\)',body)
 SCHEMA[name]={'columns':cols,'pk':pk,'fks':fks}
# Cardinalidades na ponta: instâncias dessa ponta para uma da ponta oposta.
REL=[
 ('instituicao','pesquisador','vincula','1,1','0,N'),
 ('pesquisador','arqueologo','ISA','1,1','0,1'),
 ('pesquisador','conservador','ISA','1,1','0,1'),
 ('pesquisador','analista_laboratorial','ISA','1,1','0,1'),
 ('sitio_arqueologico','campanha','recebe','1,1','0,N'),
 ('pesquisador','campanha','coordena [RN08]','1,1','0,N'),
 ('campanha','participacao_campanha','possui','1,1','0,N'),
 ('pesquisador','participacao_campanha','integra','1,1','0,N'),
 ('campanha','unidade_escavacao','divide-se','1,1','0,N'),
 ('unidade_escavacao','artefato','revela','1,1','0,N'),
 ('artefato','artefato','referência','0,1','0,N'),
 ('artefato','fragmento','compõe / identificador','1,1','0,N'),
 ('instituicao','analise_laboratorial','executa [RN17]','1,1','0,N'),
 ('artefato','artefato_analise','participa','1,1','0,N'),
 ('analise_laboratorial','artefato_analise','examina','1,1','0,N'),
 ('artefato','intervencao_conservacao','recebe','1,1','0,N'),
 ('pesquisador','intervencao_conservacao','responsável conservador [RN20]','1,1','0,N'),
 ('instituicao','local_armazenamento','possui','1,1','0,N'),
 ('artefato','custodia_artefato','histórico datado','1,1','0,N'),
 ('instituicao','custodia_artefato','assume','1,1','0,N'),
 ('local_armazenamento','custodia_artefato','abriga [RN25]','0,1','0,N'),
 ('pesquisador','custodia_artefato','responde','1,1','0,N'),
]
TITLE={'instituicao':'INSTITUIÇÃO','pesquisador':'PESQUISADOR · T,X','arqueologo':'ARQUEÓLOGO','conservador':'CONSERVADOR','analista_laboratorial':'ANALISTA LABORATORIAL','sitio_arqueologico':'SÍTIO ARQUEOLÓGICO','campanha':'CAMPANHA','participacao_campanha':'PARTICIPAÇÃO EM CAMPANHA','unidade_escavacao':'UNIDADE DE ESCAVAÇÃO','artefato':'ARTEFATO','fragmento':'FRAGMENTO · FRACA','analise_laboratorial':'ANÁLISE LABORATORIAL','artefato_analise':'ARTEFATO / ANÁLISE','intervencao_conservacao':'INTERVENÇÃO DE CONSERVAÇÃO','local_armazenamento':'LOCAL DE ARMAZENAMENTO','custodia_artefato':'CUSTÓDIA DO ARTEFATO'}

def generate(kind):
 logical=kind=='modelo-logico'
 intro='Modelo lógico relacional' if logical else 'MER conceitual completo'
 dot=['digraph G {','graph [rankdir=TB, nodesep=0.65, ranksep=0.8, splines=polyline, bgcolor="white", pad=0.4, outputorder=edgesfirst];','node [shape=plain, fontname="DejaVu Sans"];','edge [fontname="DejaVu Sans", fontsize=13, labelfontsize=15, labeldistance=2.1, labelangle=30, color="#64748b", fontcolor="#172f4d", penwidth=1.4, arrowsize=0.9];',f'label="{intro} | Gestão de Expedições Arqueológicas\\nEngenharia da Informação (pé de galinha) · cardinalidades (mín,máx) nos dois extremos\\nCada ponta indica quantas instâncias dessa entidade se relacionam com uma da ponta oposta.\\nT,X: todo pesquisador pertence a exatamente um dos três subtipos. ISA ligado diretamente ao supertipo.\\n(0,N) permite cadastro anterior aos filhos. FRAGMENTO é identificado por artefato + número.\\nRN08/RN17/RN25: regras reforçadas por triggers. RN03 e RN20: verificação semântica na consulta 15.";', 'labelloc="t"; fontsize=18; fontname="DejaVu Sans";']
 mmd=['%% Fonte auxiliar; PDF canônico gerado a partir do DOT por scripts/gerar_modelos.py.','%% T,X no supertipo: especialização TOTAL e EXCLUSIVA. ISA não é uma entidade.','%% Campos entre aspas no MER marcam identificadores/derivação; tipos são domínios conceituais.','erDiagram']
 for name,table in SCHEMA.items():
  fill='#fff3d4' if name in ['arqueologo','conservador','analista_laboratorial'] else '#eaf0f7'
  rows=[f'<TR><TD BGCOLOR="#19334f"><FONT COLOR="white" POINT-SIZE="16"><B>{html.escape(TITLE[name])}</B></FONT></TD></TR>']
  if name=='pesquisador':rows.append('<TR><TD ALIGN="LEFT" BGCOLOR="#fff3d4">Especialização TOTAL e EXCLUSIVA (T,X)</TD></TR>')
  mmd.append('    '+name.upper()+' {')
  for c in table['columns']:
   n=c['name'];pk=n in table['pk'];fk=n in table['fks']
   if not logical and fk and not pk:continue
   if not logical and c['derived']:continue
   label=html.escape(n)
   if pk:label='<U>'+label+'</U>'
   annotation=(' [PK'+('/FK' if fk else '')+']') if pk else (' [FK]' if fk else '')
   if logical:label+=' : '+html.escape(c['type'])+annotation+(' [gerado]' if c['derived'] else '')
   elif c['derived']:label+=' [derivado]'
   rows.append(f'<TR><TD PORT="{n}" ALIGN="LEFT" BGCOLOR="{fill}"><FONT POINT-SIZE="13">{label}</FONT></TD></TR>')
   typ=c['type'].replace(',','_').replace('(','_').replace(')','') if logical else ('number' if c['type'].startswith(('INT','SMALLINT','DECIMAL')) else 'date' if c['type']=='DATE' else 'boolean' if c['type']=='BOOLEAN' else 'string')
   mmd.append('        '+typ+' '+n+(' PK,FK' if pk and fk else ' PK' if pk else ' FK' if logical and fk else ''))
  mmd.append('    }')
  dot.append(name+' [label=<<TABLE BORDER="1" CELLBORDER="0" CELLSPACING="0" CELLPADDING="5" COLOR="#19334f">'+''.join(rows)+'</TABLE>>];')
 # Mesmo ranking dos subtipos: especialização integrada por ligações ISA ao pesquisador.
 dot.extend(['{rank=same; instituicao; sitio_arqueologico;}','{rank=same; arqueologo; conservador; analista_laboratorial;}','{rank=same; participacao_campanha; unidade_escavacao;}','{rank=same; fragmento; artefato_analise; intervencao_conservacao; custodia_artefato;}'])
 arrows={'1,1':'teetee','0,N':'crowodot','0,1':'teeodot'}
 mmleft={'1,1':'||','0,1':'|o','0,N':'}o'};mmright={'1,1':'||','0,1':'o|','0,N':'o{'}
 used={}
 for a,b,label,ca,cb in REL:
  if not logical and b=='intervencao_conservacao' and a=='pesquisador':a='conservador'
  ports=[]
  for node,side in [(a,'e'),(b,'w')]:
   fields=[c['name'] for c in SCHEMA[node]['columns'] if logical or not c['derived'] and (c['name'] not in SCHEMA[node]['fks'] or c['name'] in SCHEMA[node]['pk'])]
   k=used.get(node,0);used[node]=k+1
   ports.append(fields[min(k,len(fields)-1)]+':'+side)
  extra=', style=dashed, color="#b7791f"' if label=='ISA' else ''
  if a==b:extra+=', constraint=false'
  dot.append(f'{a} -> {b} [dir=both, arrowtail={arrows[ca]}, arrowhead={arrows[cb]}, taillabel="({ca})", headlabel="({cb})", label="{label}", tailport="{ports[0]}", headport="{ports[1]}"{extra}];')
  mmd.append(f'    {a.upper()} {mmleft[ca]}--{mmright[cb]} {b.upper()} : "{label}"')
 dot.append('}')
 source=ROOT/'docs/fontes'/f'{kind}.dot';source.write_text('\n'.join(dot)+'\n')
 (ROOT/'docs/fontes'/f'{kind}.mmd').write_text('\n'.join(mmd)+'\n')
 temp=ROOT/'docs'/f'.{kind}-raw.pdf'
 subprocess.run(['dot','-Tpdf',str(source),'-o',str(temp)],check=True)
 page=PdfReader(temp).pages[0];w,h=map(float,(page.mediabox.width,page.mediabox.height))
 target=(1683.78,2383.94) # A1 vertical, em pontos; uma página vetorial completa.
 scale=min((target[0]-60)/w,(target[1]-60)/h)
 writer=PdfWriter();out=writer.add_blank_page(*target)
 out.merge_transformed_page(page,Transformation().scale(scale).translate((target[0]-w*scale)/2,(target[1]-h*scale)/2))
 writer.write(ROOT/'docs'/f'{kind}.pdf');temp.unlink()
 print(kind,'fonte',round(w),round(h),'escala',round(scale,3),'atributos em',round(13*scale,1),'pt na folha A1')

if __name__=='__main__':
 generate('mer-conceitual');generate('modelo-logico')
