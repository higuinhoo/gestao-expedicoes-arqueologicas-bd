#!/usr/bin/env python3
"""Regressão da devolutiva. Usa APENAS um container MySQL descartável.
Uso: python tests/validar_etapa1.py NOME_DO_CONTAINER
O esquema arqueologia_ucb desse container será apagado/recriado.
"""
from pathlib import Path
import subprocess,sys
ROOT=Path(__file__).resolve().parents[1]
CONTAINER=sys.argv[1] if len(sys.argv)>1 else 'arqueologia-feedback-mysql'
def mysql(sql,ok=True):
 p=subprocess.run(['docker','exec','-i',CONTAINER,'mysql','-uroot','--batch','--raw','--default-character-set=utf8mb4'],input=sql,text=True,capture_output=True)
 if ok and p.returncode:raise AssertionError(p.stderr)
 return p

def rejects(label,sql,rn):
 p=mysql('USE arqueologia_ucb; START TRANSACTION;\n'+sql+';\nROLLBACK;',ok=False)
 assert p.returncode and rn in p.stderr,(label,p.returncode,p.stderr)
 print('PASS',label,'—',rn)

print('MySQL:',mysql('SELECT VERSION();').stdout.strip().splitlines()[-1])
for cycle in (1,2):
 if cycle==1:mysql('DROP DATABASE IF EXISTS arqueologia_ucb;')
 for name in ['01_ddl.sql','02_carga.sql','03_consultas.sql']:
  mysql((ROOT/'sql'/name).read_text())
 print('PASS reconstrução',cycle,'— DDL, carga e 15 consultas sem erro')
counts={'instituicao':40,'pesquisador':40,'arqueologo':14,'conservador':13,'analista_laboratorial':13,'sitio_arqueologico':40,'campanha':40,'participacao_campanha':120,'unidade_escavacao':80,'artefato':100,'fragmento':150,'analise_laboratorial':45,'artefato_analise':100,'intervencao_conservacao':60,'local_armazenamento':40,'custodia_artefato':119}
for table,n in counts.items():
 actual=int(mysql(f'USE arqueologia_ucb; SELECT COUNT(*) FROM {table};').stdout.splitlines()[-1]);assert actual==n,(table,actual,n)
print('PASS volumes:',', '.join(f'{t}={n}' for t,n in counts.items()))
sql=(ROOT/'sql/03_consultas.sql').read_text();audit=sql[sql.index("SELECT 'COORDENADOR_NAO_ARQUEOLOGO'"):]
assert len(mysql('USE arqueologia_ucb;'+audit).stdout.strip())==0
print('PASS consulta 15 — zero violações semânticas')
rejects('RN08 INSERT',"INSERT INTO campanha (id_sitio,id_coordenador,codigo,nome,objetivo,data_inicio,situacao) VALUES (1,2,'TESTE-RN08','Teste','Teste','2026-10-01','PLANEJADA')",'RN08')
rejects('RN08 UPDATE','UPDATE campanha SET id_coordenador=2 WHERE id_campanha=1','RN08')
rejects('RN08 alteração de pesquisador',"UPDATE pesquisador SET tipo_especializacao='CONSERVADOR' WHERE id_pesquisador=1",'RN08')
rejects('RN17 INSERT',"INSERT INTO analise_laboratorial (id_laboratorio,tipo_analise,metodo,data_inicio,situacao) VALUES (1,'Teste','Teste','2026-10-01','SOLICITADA')",'RN17')
rejects('RN17 UPDATE','UPDATE analise_laboratorial SET id_laboratorio=1 WHERE id_analise=1','RN17')
rejects('RN17 alteração de instituição',"UPDATE instituicao SET tipo='MUSEU' WHERE id_instituicao=3",'RN17')
rejects('RN25 INSERT',"INSERT INTO custodia_artefato (id_artefato,id_instituicao,id_local,id_responsavel,data_inicio,documento_movimentacao,motivo) VALUES (100,1,2,1,'2026-10-01','TESTE-RN25','Teste')",'RN25')
rejects('RN25 UPDATE','UPDATE custodia_artefato SET id_local=1 WHERE id_custodia=21','RN25')
rejects('RN25 alteração de local','UPDATE local_armazenamento SET id_instituicao=6 WHERE id_local=5','RN25')
# Casos válidos e local opcional; nada deste bloco fica na carga.
mysql("""USE arqueologia_ucb; START TRANSACTION;
UPDATE campanha SET id_coordenador=4 WHERE id_campanha=1;
UPDATE analise_laboratorial SET id_laboratorio=7 WHERE id_analise=1;
UPDATE custodia_artefato SET id_local=NULL WHERE id_custodia=21;
INSERT INTO campanha (id_sitio,id_coordenador,codigo,nome,objetivo,data_inicio,situacao) VALUES (1,1,'TESTE-OK','Teste','Teste','2026-10-01','PLANEJADA');
INSERT INTO analise_laboratorial (id_laboratorio,tipo_analise,metodo,data_inicio,situacao) VALUES (3,'Teste','Teste','2026-10-01','SOLICITADA');
INSERT INTO custodia_artefato (id_artefato,id_instituicao,id_local,id_responsavel,data_inicio,documento_movimentacao,motivo) VALUES (100,1,1,1,'2026-10-01','TESTE-OK','Teste');
ROLLBACK;""")
print('PASS INSERT/UPDATE válidos e local opcional; ROLLBACK dos dados de teste')
# Regressão de snapshot antigo: a proteção do pai deve ver filhos recém-confirmados.
# Trata-se de teste de integridade dos triggers, não do experimento B3 da Etapa 2.
mysql("USE arqueologia_ucb; INSERT INTO instituicao (id_instituicao,nome,tipo,cidade) VALUES (901,'Instituicao temporaria de teste','LABORATORIO','Teste');")
session=None
try:
 session=subprocess.Popen(['docker','exec','-i',CONTAINER,'mysql','-uroot','--batch','--raw','--unbuffered'],stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True)
 session.stdin.write("USE arqueologia_ucb; START TRANSACTION WITH CONSISTENT SNAPSHOT; SELECT COUNT(*) FROM analise_laboratorial WHERE id_laboratorio=901; SELECT 'SNAPSHOT_PRONTO';\n");session.stdin.flush()
 # A leitura da marca é imediata após o SELECT; communicate abaixo tem timeout.
 while True:
  line=session.stdout.readline()
  assert line,'sessão terminou antes de estabelecer snapshot'
  if line.strip()=='SNAPSHOT_PRONTO':break
 mysql("USE arqueologia_ucb; INSERT INTO analise_laboratorial (id_analise,id_laboratorio,tipo_analise,metodo,data_inicio,situacao) VALUES (901,901,'Teste','Teste','2026-10-01','SOLICITADA');")
 out,err=session.communicate("UPDATE instituicao SET tipo='MUSEU' WHERE id_instituicao=901; ROLLBACK;\n",timeout=20)
 assert session.returncode and 'RN17' in err,(session.returncode,err)
 print('PASS proteção de pai com snapshot antigo — filho recém-confirmado visível ao trigger')
finally:
 if session is not None and session.poll() is None:session.kill();session.communicate()
 mysql('USE arqueologia_ucb; DELETE FROM analise_laboratorial WHERE id_analise=901; DELETE FROM instituicao WHERE id_instituicao=901;')

# Contornos documentados.
checks={
 'custódia com local pendente':'SELECT COUNT(*) FROM custodia_artefato WHERE id_local IS NULL',
 'artefato sem custódia':'SELECT COUNT(*) FROM artefato a WHERE NOT EXISTS (SELECT 1 FROM custodia_artefato c WHERE c.id_artefato=a.id_artefato)',
 'datação pendente':'SELECT COUNT(*) FROM artefato WHERE periodo_estimado IS NULL',
 'dimensão desconhecida':'SELECT COUNT(*) FROM fragmento WHERE comprimento_mm IS NULL',
 'sítio sem artefatos':'SELECT COUNT(*) FROM sitio_arqueologico s WHERE NOT EXISTS (SELECT 1 FROM campanha c JOIN unidade_escavacao u ON u.id_campanha=c.id_campanha JOIN artefato a ON a.id_unidade=u.id_unidade WHERE c.id_sitio=s.id_sitio)',
}
for label,q in checks.items():
 n=int(mysql('USE arqueologia_ucb;'+q+';').stdout.splitlines()[-1]);assert n>0,label
 print('PASS contorno:',label,'=',n)
print(mysql("SELECT 'tabelas',COUNT(*) FROM information_schema.tables WHERE table_schema='arqueologia_ucb' UNION ALL SELECT 'FKs',COUNT(*) FROM information_schema.referential_constraints WHERE constraint_schema='arqueologia_ucb' UNION ALL SELECT 'CHECKs',COUNT(*) FROM information_schema.table_constraints WHERE constraint_schema='arqueologia_ucb' AND constraint_type='CHECK' UNION ALL SELECT 'triggers',COUNT(*) FROM information_schema.triggers WHERE trigger_schema='arqueologia_ucb';").stdout.strip())
print('Validação concluída. Nenhum dado dos testes negativos foi persistido.')
