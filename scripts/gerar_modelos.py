#!/usr/bin/env python3
"""Metadados do DDL e entrada para a renderização dos modelos com PlantUML.
Requer Java, PlantUML 1.2025.2, Graphviz e dependências Python. Não altera o banco.
"""
from pathlib import Path
import re,html,subprocess
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


if __name__=='__main__':
 from renderizar_modelos import main
 main()
