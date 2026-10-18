#!/usr/bin/env python3
"""Provisiona apenas um MySQL de desenvolvimento em container, com autorização explícita --recriar.
Senhas entram por getpass ou variáveis ARQ_*_PASSWORD; não são impressas nem salvas.
"""
from pathlib import Path
import argparse,base64,getpass,hashlib,os,secrets,subprocess
ROOT=Path(__file__).resolve().parents[1]

def mysql(container,sql):
    p=subprocess.run(['docker','exec','-i',container,'mysql','-uroot','--default-character-set=utf8mb4','--batch'],input=sql,text=True,capture_output=True)
    if p.returncode:raise RuntimeError(p.stderr)
    return p.stdout

def quote(value):return "'"+value.replace("'","''")+"'"

def hash_password(password):
    salt=secrets.token_bytes(16);digest=hashlib.pbkdf2_hmac('sha256',password.encode(),salt,600000)
    return 'pbkdf2_sha256$600000$'+base64.b64encode(salt).decode()+'$'+base64.b64encode(digest).decode()

def prepare(container,port,passwords,config,recreate):
    if not recreate:raise ValueError('A reconstrução exige --recriar; ela remove o esquema arqueologia_ucb do container indicado.')
    for name in ['admin','catalogador','consulta']:
        if len(passwords[name])<12:raise ValueError('Use senhas com pelo menos doze caracteres.')
    mysql(container,'DROP DATABASE IF EXISTS arqueologia_ucb;')
    for file in ['01_ddl.sql','02_carga.sql','03_consultas.sql','04_operacao.sql','05_perfis.sql','06_indices_desempenho.sql']:
        mysql(container,(ROOT/'sql'/file).read_text());print('OK',file)
    statements=["SET SESSION sql_mode='STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION,NO_BACKSLASH_ESCAPES'; USE arqueologia_ucb;"]
    for login,role in [('admin','ADMIN'),('catalogador','CATALOGADOR'),('consulta','CONSULTA')]:
        account=quote(login)+"@'%'";dbrole=quote('arqueologia_'+('admin' if role=='ADMIN' else role.lower()))
        statements.extend([
            f'CREATE USER IF NOT EXISTS {account} IDENTIFIED BY {quote(passwords[login])};',
            f'ALTER USER {account} IDENTIFIED BY {quote(passwords[login])};',
            f'REVOKE ALL PRIVILEGES, GRANT OPTION FROM {account};',
            f'GRANT {dbrole} TO {account};',f'SET DEFAULT ROLE {dbrole} TO {account};',
            'INSERT INTO usuario_app(login,nome,perfil,senha_hash) VALUES ('+','.join(map(quote,[login,{'admin':'Administrador do acervo','catalogador':'Catalogador do acervo','consulta':'Consulta pública'}[login],role,hash_password(passwords[login])]))+');'
        ])
    mysql(container,'\n'.join(statements))
    config=Path(config);config.parent.mkdir(parents=True,exist_ok=True)
    config.write_text(f'db.url=jdbc:mysql://127.0.0.1:{port}/arqueologia_ucb?sslMode=PREFERRED&allowPublicKeyRetrieval=true&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true\n')
    print('Contas admin/catalogador/consulta criadas; senhas armazenadas somente como hash na aplicação.')
    print('Configuração sem senhas:',config)

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--container',required=True);p.add_argument('--port',type=int,default=33316);p.add_argument('--config',default=str(ROOT/'app/config.properties'));p.add_argument('--recriar',action='store_true');a=p.parse_args()
    passwords={name:os.environ.get('ARQ_'+name.upper()+'_PASSWORD') or getpass.getpass('Nova senha de '+name+': ') for name in ['admin','catalogador','consulta']}
    prepare(a.container,a.port,passwords,a.config,a.recriar)
