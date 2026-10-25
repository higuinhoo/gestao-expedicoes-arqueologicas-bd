#!/usr/bin/env python3
"""Reconstrói e testa a Etapa 2 em um container criado explicitamente para testes.
Use --recriar, um JDK 17+ completo e Maven. DISPLAY habilita o teste real de Swing.
As senhas aleatórias permanecem somente na memória deste processo e dos filhos.
"""
from pathlib import Path
import argparse,json,os,secrets,subprocess,time,xml.etree.ElementTree as ET
from preparar_etapa2 import prepare
ROOT=Path(__file__).resolve().parents[1]

def run(container,port,mvn,settings,display_required):
    if not container.startswith('arqueologia-teste-'):raise ValueError('O nome de teste deve começar com arqueologia-teste-.')
    found=subprocess.run(['docker','inspect',container,'--format','{{index .Config.Labels "arqueologia.teste"}}'],capture_output=True,text=True)
    if found.returncode==0:
        if found.stdout.strip()!='true':raise ValueError('Container existente não foi criado por este teste; nada será removido.')
        subprocess.run(['docker','rm','-f',container],capture_output=True,check=True)
    subprocess.run(['docker','run','--name',container,'--label','arqueologia.teste=true','-p',f'127.0.0.1:{port}:3306','-e','MYSQL_ALLOW_EMPTY_PASSWORD=yes','-e','MYSQL_ROOT_HOST=%','-d','mysql:8.4'],capture_output=True,check=True)
    end=time.monotonic()+50
    while time.monotonic()<end:
        p=subprocess.run(['docker','exec',container,'mysql','--protocol=TCP','-h127.0.0.1','-uroot','-e','SELECT 1'],capture_output=True)
        if p.returncode==0:break
        time.sleep(.5)
    else:raise TimeoutError('MySQL não iniciou em 50 segundos.')
    credentials={name:secrets.token_urlsafe(20) for name in ['admin','catalogador','consulta']}
    config=ROOT/'local/test-config.properties';prepare(container,port,credentials,config,True)
    env=os.environ.copy();env['ARQ_CONFIG']=str(config)
    for k,v in credentials.items():env['ARQ_TEST_'+k.upper()+'_PASSWORD']=v
    if display_required and not env.get('DISPLAY'):raise ValueError('Defina DISPLAY para exigir o teste Swing.')
    command=[mvn]
    if settings:command+=['-s',settings]
    command+=['-f',str(ROOT/'app/pom.xml'),'clean','verify']
    output=ROOT/'docs/evidencias/etapa2/maven-validacao.txt'
    with output.open('w') as log:
        result=subprocess.run(command,env=env,cwd=ROOT/'app',text=True,stdout=log,stderr=subprocess.STDOUT)
    if result.returncode:raise RuntimeError('Falha no Maven. Consulte '+str(output))
    records=[]
    for path in sorted((ROOT/'app/target/surefire-reports').glob('TEST-*.xml')):
        root=ET.parse(path).getroot();records.append({k:root.get(k) for k in ['name','tests','errors','failures','skipped','time']})
    if not records or any(int(r['errors'])+int(r['failures']) for r in records):raise AssertionError(records)
    if display_required and any(int(r['skipped']) for r in records):raise AssertionError('Testes ignorados: '+str(records))
    summary={'classes':records,'total':sum(int(r['tests']) for r in records),'falhas':0,'erros':0,'ignorados':sum(int(r['skipped']) for r in records),'java':subprocess.run(['java','-version'],capture_output=True,text=True).stderr.splitlines()[0],'base':'MySQL 8.4.11 em container descartável','interface_real':display_required}
    (ROOT/'docs/evidencias/etapa2/testes.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2)+'\n');print(json.dumps(summary,ensure_ascii=False,indent=2))
    print('JAR construído e verificações concluídas. Credenciais temporárias não foram gravadas.')

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--container',default='arqueologia-teste-etapa2');p.add_argument('--port',type=int,default=33318);p.add_argument('--mvn',default='mvn');p.add_argument('--settings');p.add_argument('--recriar',action='store_true');p.add_argument('--exigir-interface',action='store_true');a=p.parse_args()
    if not a.recriar:p.error('Confirme --recriar para reconstruir exclusivamente a base de teste.')
    run(a.container,a.port,a.mvn,a.settings,a.exigir_interface)
