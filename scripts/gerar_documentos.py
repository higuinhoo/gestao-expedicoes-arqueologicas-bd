#!/usr/bin/env python3
"""Gera relatório e dicionário em PDF a partir de Markdown e diagramas vetoriais.
Requer Python 3, reportlab, pypdf, pandoc e as fontes DejaVu do sistema.
Uso: python scripts/gerar_modelos.py && python scripts/gerar_documentos.py
"""
from pathlib import Path
import io,json,html,re,subprocess
from reportlab.pdfgen import canvas
from reportlab.platypus import SimpleDocTemplate,Paragraph,Spacer,LongTable,TableStyle,Preformatted,Image
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from pypdf import PdfReader,PdfWriter
ROOT=Path(__file__).resolve().parents[1]
FONT=Path('/usr/share/fonts/truetype/dejavu')
for name,file in [('DV','DejaVuSans.ttf'),('DV-Bold','DejaVuSans-Bold.ttf'),('DV-Oblique','DejaVuSans-Oblique.ttf'),('DVMono','DejaVuSansMono.ttf')]:
 pdfmetrics.registerFont(TTFont(name,str(FONT/file)))
pdfmetrics.registerFontFamily('DV',normal='DV',bold='DV-Bold',italic='DV-Oblique',boldItalic='DV-Bold')
WIDTH=A4[0]-88
STYLES={
 'body':ParagraphStyle('body',fontName='DV',fontSize=10.2,leading=15,spaceAfter=9,splitLongWords=True),
 'title':ParagraphStyle('title',fontName='DV-Bold',fontSize=22,leading=29,spaceAfter=22,textColor=colors.HexColor('#19334f'),keepWithNext=True),
 'h2':ParagraphStyle('h2',fontName='DV-Bold',fontSize=15,leading=21,spaceBefore=18,spaceAfter=11,textColor=colors.HexColor('#19334f'),keepWithNext=True),
 'h3':ParagraphStyle('h3',fontName='DV-Bold',fontSize=12,leading=17,spaceBefore=13,spaceAfter=8,textColor=colors.HexColor('#19334f'),keepWithNext=True),
 'cell':ParagraphStyle('cell',fontName='DV',fontSize=9.2,leading=12.8,splitLongWords=True),
 'th':ParagraphStyle('th',fontName='DV-Bold',fontSize=9.2,leading=12.8,textColor=colors.white,splitLongWords=True),
 'bullet':ParagraphStyle('bullet',fontName='DV',fontSize=10.2,leading=15,leftIndent=13,firstLineIndent=-9,spaceAfter=7,splitLongWords=True),
 'code':ParagraphStyle('code',fontName='DVMono',fontSize=8,leading=11,spaceAfter=10),
}
def inline(items):
 out=''
 for item in items:
  t=item['t'];c=item.get('c')
  if t=='Str':out+=html.escape(c)
  elif t in ('Space','SoftBreak'):out+=' '
  elif t=='LineBreak':out+='<br/>'
  elif t=='Code':out+='<font color="#25466c">'+html.escape(c[1])+'</font>'
  elif t in ('Strong','Emph','Strikeout'):
   tag='b' if t=='Strong' else 'i';out+='<'+tag+'>'+inline(c)+'</'+tag+'>'
  elif t=='Link':out+='<link href="'+html.escape(c[2][0],quote=True)+'" color="#25466c">'+inline(c[1])+'</link>'
  elif t=='Quoted':out+='“'+inline(c[1])+'”'
  elif t=='Math':out+=html.escape(c[1])
  elif t=='Span':out+=inline(c[1])
 return out

def block_inline(blocks):
 return '<br/>'.join(inline(b['c']) for b in blocks if b['t'] in ('Para','Plain'))

def table_flow(c):
 headers=[r[1] for r in c[3][1]]
 bodies=[]
 for body in c[4]:bodies.extend(r[1] for r in body[2]+body[3])
 raw=headers+bodies
 data=[[Paragraph(block_inline(cell[4]),STYLES['th' if i<len(headers) else 'cell']) for cell in row] for i,row in enumerate(raw)]
 n=len(data[0]);first=[block_inline(cell[4]) for cell in raw[0]]
 if first[0]=='Atributo':widths=[118,90,62,48,WIDTH-318]
 elif first[0]=='Entidade A':widths=[110,112,49,WIDTH-320,49]
 elif first[0]=='Critério':widths=[54,106,WIDTH-274,114]
 elif n==3:widths=[112,172,WIDTH-284]
 else:widths=[WIDTH/n]*n
 table=LongTable(data,colWidths=widths,repeatRows=len(headers),hAlign='LEFT',splitInRow=1)
 table.setStyle(TableStyle([('BACKGROUND',(0,0),(-1,len(headers)-1),colors.HexColor('#19334f')),('VALIGN',(0,0),(-1,-1),'TOP'),('LEFTPADDING',(0,0),(-1,-1),6),('RIGHTPADDING',(0,0),(-1,-1),6),('TOPPADDING',(0,0),(-1,-1),6),('BOTTOMPADDING',(0,0),(-1,-1),6),('LINEBELOW',(0,len(headers)-1),(-1,-1),0.3,colors.HexColor('#cbd5e1')),('ROWBACKGROUNDS',(0,len(headers)),(-1,-1),[colors.HexColor('#f0f4f8'),colors.white])]))
 return [table,Spacer(1,10)]

def flow(blocks):
 story=[]
 for b in blocks:
  t=b['t'];c=b.get('c')
  if t=='Header':story.append(Paragraph(inline(c[2]),STYLES['title' if c[0]==1 else 'h2' if c[0]==2 else 'h3']))
  elif t in ('Para','Plain'):
   pictures=[item for item in c if item['t']=='Image']
   if pictures:
    for item in pictures:
     path=(ROOT/item['c'][2][0]).resolve();path.relative_to(ROOT)
     picture=Image(str(path));factor=min(WIDTH/picture.imageWidth,330/picture.imageHeight)
     picture.drawWidth=picture.imageWidth*factor;picture.drawHeight=picture.imageHeight*factor
     story.extend([picture,Spacer(1,7),Paragraph(inline(item['c'][1]),STYLES['body'])])
   else:story.append(Paragraph(inline(c),STYLES['body']))
  elif t=='BulletList':
   for item in c:story.append(Paragraph('• '+block_inline(item),STYLES['bullet']))
  elif t=='OrderedList':
   for i,item in enumerate(c[1],c[0][0]):story.append(Paragraph(str(i)+'. '+block_inline(item),STYLES['bullet']))
  elif t=='Table':story.extend(table_flow(c))
  elif t=='CodeBlock':
   for line in c[1].splitlines():story.append(Paragraph(html.escape(line).replace(' ','&nbsp;'),STYLES['code']))
  elif t=='HorizontalRule':story.append(Spacer(1,15))
 return story

def ast(markdown):
 p=subprocess.run(['pandoc','--from=gfm','--to=json'],input=markdown,text=True,capture_output=True,check=True)
 return json.loads(p.stdout)['blocks']

def render_piece(markdown):
 buf=io.BytesIO();doc=SimpleDocTemplate(buf,pagesize=A4,rightMargin=44,leftMargin=44,topMargin=44,bottomMargin=53,title='Gestão de Expedições Arqueológicas')
 doc.build(flow(ast(markdown)));buf.seek(0);return PdfReader(buf)

def generate(source,target,title):
 text=source.read_text();writer=PdfWriter()
 inserts={'[INSERIR_MER]':'mer-conceitual.pdf','[INSERIR_LOGICO]':'modelo-logico.pdf','[INSERIR_ETAPA1]':'relatorio-etapa1.pdf','[INSERIR_TECNICO]':'adendo-tecnico-etapa2.pdf'}
 for chunk in re.split(r'(\[INSERIR_(?:MER|LOGICO|ETAPA1|TECNICO)\])',text):
  if not chunk.strip():continue
  if chunk.strip() in inserts:
   filename=inserts[chunk.strip()]
   reader=PdfReader(ROOT/'docs'/filename)
  else:reader=render_piece(chunk)
  for p in reader.pages:writer.add_page(p)
 total=len(writer.pages)
 for i,page in enumerate(writer.pages,1):
  w,h=float(page.mediabox.width),float(page.mediabox.height)
  overlay=io.BytesIO();c=canvas.Canvas(overlay,pagesize=(w,h));c.setFillColor(colors.white);c.rect(0,0,w,40,fill=1,stroke=0);c.setStrokeColor(colors.HexColor('#cbd5e1'));c.line(44,36,w-44,36)
  c.setFont('DV',9 if w<1000 else 12);c.setFillColor(colors.HexColor('#475569'))
  c.drawString(44,22,'Laboratório de Banco de Dados | '+('Relatório final' if 'final' in source.name else 'Etapa 1 revisada')+' | 18/10/2026')
  c.drawRightString(w-44,22,f'Página {i} de {total}');c.save();overlay.seek(0)
  page.merge_page(PdfReader(overlay).pages[0])
 writer.add_metadata({'/Title':title,'/Author':'Arthur Tavares Silva; Higor Samuel Ferreira Silva; Igor Vendramini Perini; João Gabriel da Cruz Viana','/Subject':('Etapa 2 B1–B8 e revisão C1–C5 da Etapa 1' if 'final' in source.name else 'Revisão C1–C5 da devolutiva da Etapa 1')})
 writer.write(target);print(target.relative_to(ROOT),total,'páginas')

if __name__=='__main__':
 import sys
 if '--final' in sys.argv:
  generate(ROOT/'docs/fontes/relatorio-final.md',ROOT/'docs/relatorio-final.pdf','Gestão de Expedições Arqueológicas — Relatório final')
  raise SystemExit(0)
 generate(ROOT/'docs/fontes/relatorio-etapa1.md',ROOT/'docs/relatorio-etapa1.pdf','Gestão de Expedições Arqueológicas — Etapa 1 revisada')
 generate(ROOT/'docs/fontes/dicionario-dados.md',ROOT/'docs/dicionario-dados.pdf','Dicionário de dados — Gestão de Expedições Arqueológicas')
