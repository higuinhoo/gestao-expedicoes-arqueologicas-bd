#!/usr/bin/env python3
"""Ensaio real em containers descartáveis. O destino deve ser NOVO; não remove containers existentes.
Os backups contêm dados/hash de autenticação e ficam em local/backups, fora do Git.
"""
from pathlib import Path
import argparse,gzip,hashlib,json,shutil,subprocess,time
from preparar_etapa2 import mysql,quote
ROOT=Path(__file__).resolve().parents[1]

def counts(container):
    names=mysql(container,"SELECT table_name FROM information_schema.tables WHERE table_schema='arqueologia_ucb' AND table_type='BASE TABLE' ORDER BY table_name;").splitlines()[1:]
    result={}
    for name in names:
        if not name.replace('_','').isalnum():raise ValueError('Nome inesperado no esquema.')
        result[name]=int(mysql(container,'SELECT COUNT(*) FROM arqueologia_ucb.`'+name+'`;').strip().splitlines()[-1])
    result['VIEWS']=int(mysql(container,"SELECT COUNT(*) FROM information_schema.views WHERE table_schema='arqueologia_ucb';").strip().splitlines()[-1])
    result['TRIGGERS']=int(mysql(container,"SELECT COUNT(*) FROM information_schema.triggers WHERE trigger_schema='arqueologia_ucb';").strip().splitlines()[-1])
    return result

def run(source,destination,archive,port=None,passwords=None):
    existing=subprocess.run(['docker','container','inspect',destination],capture_output=True)
    if existing.returncode==0:raise ValueError('Destino já existe; escolha outro nome. Nenhum container foi removido.')
    archive=Path(archive);archive.parent.mkdir(parents=True,exist_ok=True)
    start=time.monotonic()
    process=subprocess.Popen(['docker','exec',source,'mysqldump','-uroot','--single-transaction','--routines','--triggers','--events','--no-tablespaces','--set-gtid-purged=OFF','--column-statistics=0','--databases','arqueologia_ucb'],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
    with gzip.open(archive,'wb') as output:shutil.copyfileobj(process.stdout,output)
    error=process.stderr.read().decode();process.wait()
    if process.returncode:raise RuntimeError(error)
    print('Backup lógico completo em gzip; tamanho:',archive.stat().st_size,'bytes; duração:',round(time.monotonic()-start,3),'s')
    command=['docker','run','--name',destination,'-e','MYSQL_ALLOW_EMPTY_PASSWORD=yes','-e','MYSQL_ROOT_HOST=%']
    if port is not None:command.extend(['-p',f'127.0.0.1:{port}:3306'])
    command.extend(['-d','mysql:8.4']);subprocess.run(command,capture_output=True,check=True)
    end=time.monotonic()+50
    while time.monotonic()<end:
        p=subprocess.run(['docker','exec',destination,'mysql','--protocol=TCP','-h127.0.0.1','-uroot','--batch','-e','SELECT 1'],capture_output=True)
        if p.returncode==0:break
        time.sleep(.5)
    else:raise TimeoutError('MySQL de restauração não ficou pronto em 50s.')
    start=time.monotonic();restore=subprocess.Popen(['docker','exec','-i',destination,'mysql','-uroot','--default-character-set=utf8mb4'],stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
    try:
        with gzip.open(archive,'rb') as source_file:shutil.copyfileobj(source_file,restore.stdin)
    except BrokenPipeError:
        pass  # A mensagem do cliente abaixo é mais útil que BrokenPipeError.
    finally:
        restore.stdin.close()
    error=restore.stderr.read().decode();restore.wait()
    if restore.returncode:raise RuntimeError(error)
    print('Restauração em outro servidor concluída em',round(time.monotonic()-start,3),'s')
    one=counts(source);two=counts(destination)
    if one!=two:raise AssertionError({'origem':one,'restauracao':two})
    print('Contagens comparadas:',json.dumps(one,ensure_ascii=False,sort_keys=True))
    # Valida execução de views e código de auditoria restaurado, não apenas a existência.
    for view in ['vw_acervo_consulta','vw_analises_consulta','vw_relatorio_custodia']:
        a=mysql(source,'SELECT COUNT(*) FROM arqueologia_ucb.'+view+';').strip().splitlines()[-1]
        b=mysql(destination,'SELECT COUNT(*) FROM arqueologia_ucb.'+view+';').strip().splitlines()[-1]
        assert a==b,(view,a,b)
        print('VIEW',view,'OK, linhas:',b)
    verification=mysql(destination,"USE arqueologia_ucb; SET @app_actor='ensaio-restauracao'; START TRANSACTION; SELECT COUNT(*) AS antes FROM auditoria; UPDATE artefato SET situacao='EXPOSTO' WHERE id_artefato=1; SELECT COUNT(*) AS durante FROM auditoria; ROLLBACK; SELECT COUNT(*) AS depois FROM auditoria;")
    nums=[int(line) for line in verification.splitlines() if line.isdigit()];assert nums[1]==nums[0]+1 and nums[2]==nums[0],nums
    print('Trigger restaurado executado: auditoria antes/durante/depois do rollback:',nums)
    mysql(destination,(ROOT/'sql/05_perfis.sql').read_text())
    if passwords:
        statements=["SET SESSION sql_mode='STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION,NO_BACKSLASH_ESCAPES';"]
        for name in ['admin','catalogador','consulta']:
            account=quote(name)+"@'%'";role=quote('arqueologia_'+name)
            statements.extend([f'CREATE USER {account} IDENTIFIED BY {quote(passwords[name])};',f'GRANT {role} TO {account};',f'SET DEFAULT ROLE {role} TO {account};'])
        mysql(destination,'\n'.join(statements));print('Três papéis e usuários reprovisionados; hashes da aplicação vieram do backup.')
    print('Ensaio aprovado. Arquivo de backup e servidor restaurado permanecem locais, fora do versionamento.')

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--origem',required=True);p.add_argument('--destino',required=True);p.add_argument('--arquivo',default=str(ROOT/'local/backups/arqueologia.sql.gz'));p.add_argument('--confirmar-base-teste',action='store_true');a=p.parse_args()
    if not a.confirmar_base_teste:p.error('Confirme --confirmar-base-teste.')
    run(a.origem,a.destino,a.arquivo)
