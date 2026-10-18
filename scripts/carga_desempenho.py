#!/usr/bin/env python3
"""Carga sintética adicional, apenas em base de ensaio descartável. IDs reservados >=10000.
Uso: python scripts/carga_desempenho.py --container NOME --confirmar-base-teste --quantidade 20000
"""
from pathlib import Path
import argparse,datetime,json,sys
from preparar_etapa2 import mysql,quote

def load(container,n):
    if not 1000<=n<=100000:raise ValueError('Quantidade entre 1000 e 100000.')
    count=mysql(container,'USE arqueologia_ucb; SELECT COUNT(*) FROM artefato WHERE id_artefato>=10000;').strip().splitlines()[-1]
    if int(count):raise ValueError('Faixa de IDs já ocupada. Reconstrua a base de ensaio antes de repetir a carga.')
    def insert(table,columns,rows):
        prefix='INSERT INTO '+table+' ('+columns+') VALUES '
        for k in range(0,len(rows),1000):mysql(container,'USE arqueologia_ucb; '+prefix+','.join(rows[k:k+1000])+';')
    def values(items):return '('+','.join('NULL' if v is None else quote(v) if isinstance(v,str) else str(v) for v in items)+')'
    artifacts=[];analyses=[];links=[];custodies=[]
    materials=['Cerâmica','Metal','Vidro','Pedra','Osso','Madeira'];states=['INTEGRO','ESTAVEL','FRAGIL','CRITICO'];statuses=['CONCLUIDA','EM_EXECUCAO','SOLICITADA','CANCELADA']
    for i in range(n):
        ident=10000+i;discovery=datetime.date(2025,1,1)+datetime.timedelta(days=i%365)
        artifacts.append(values([ident,i%80+1,'BENCH-ART-'+str(i),'Amostra sintética '+str(i),'Material de ensaio de desempenho, sem dados pessoais.',materials[i%6],discovery.isoformat(),(i%500)/10,states[i%4],'ARMAZENADO']))
        start=datetime.date(2026,1,1)+datetime.timedelta(days=i%365);status=statuses[i%4];end=(start+datetime.timedelta(days=12)).isoformat() if status=='CONCLUIDA' else None
        analyses.append(values([ident,3+4*(i%10),'Arqueometria','Método sintético de ensaio',start.isoformat(),end,status,round(50+(i%1000)*1.31,2)]))
        for j in range(2):links.append(values([10000+(i+j)%n,ident,'BENCH-AMO-'+str(i)+'-'+str(j),round(.05+(i%20)/100,3),'Resultado sintético' if status=='CONCLUIDA' else None]))
        for event in range(5):
            inst=1+(i+event)%40;begin=discovery+datetime.timedelta(days=1+event*30);finish=(begin+datetime.timedelta(days=30)).isoformat() if event<4 else None
            custodies.append(values([ident,inst,inst,1,begin.isoformat(),finish,'BENCH-MOV-'+str(i)+'-'+str(event),'Guarda sintética para ensaio de consulta']))
    insert('artefato','id_artefato,id_unidade,codigo_catalogo,nome,descricao,material,data_descoberta,profundidade_cm,estado_conservacao,situacao',artifacts)
    insert('analise_laboratorial','id_analise,id_laboratorio,tipo_analise,metodo,data_inicio,data_fim,situacao,custo',analyses)
    insert('artefato_analise','id_artefato,id_analise,codigo_amostra,massa_consumida_g,resultado_resumido',links)
    insert('custodia_artefato','id_artefato,id_instituicao,id_local,id_responsavel,data_inicio,data_fim,documento_movimentacao,motivo',custodies)
    print(json.dumps({'artefatos_adicionais':n,'analises_adicionais':n,'associacoes_adicionais':n*2,'custodias_adicionais':n*5},ensure_ascii=False))
    print(mysql(container,"SELECT 'artefato' AS tabela,COUNT(*) AS registros FROM arqueologia_ucb.artefato UNION ALL SELECT 'analise_laboratorial',COUNT(*) FROM arqueologia_ucb.analise_laboratorial UNION ALL SELECT 'artefato_analise',COUNT(*) FROM arqueologia_ucb.artefato_analise UNION ALL SELECT 'custodia_artefato',COUNT(*) FROM arqueologia_ucb.custodia_artefato;"))

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--container',required=True);p.add_argument('--quantidade',type=int,default=20000);p.add_argument('--confirmar-base-teste',action='store_true');a=p.parse_args()
    if not a.confirmar_base_teste:p.error('Confirme explicitamente --confirmar-base-teste.')
    load(a.container,a.quantidade)
