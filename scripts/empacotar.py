#!/usr/bin/env python3
"""Empacota somente arquivos versionaveis, sem credenciais ou caches locais."""
from pathlib import Path
import hashlib,subprocess,zipfile
ROOT=Path(__file__).resolve().parents[1]
def digest(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def package():
    jar=ROOT/'dist/arqueologia-2.0.0.jar'
    if not jar.is_file():raise FileNotFoundError('Compile e copie o JAR para dist/ antes de empacotar.')
    paths=subprocess.check_output(['git','ls-files','--cached','--others','--exclude-standard','-z'],cwd=ROOT).decode().split('\0')
    target=ROOT/'dist/arqueologia-etapa2.zip'
    skip={'dist/arqueologia-etapa2.zip','dist/SHA256SUMS'}
    entries=sorted({p for p in paths if p and p not in skip and (ROOT/p).is_file()})
    forbidden=('local/','.git/','backups/','app/target/')
    if any(p.startswith(forbidden) or p=='app/config.properties' or p.endswith('.env') for p in entries):
        raise ValueError('Arquivo privado detectado; revise o versionamento antes de empacotar.')
    prefix='gestao-expedicoes-arqueologicas-bd/'
    jar_line=digest(jar)+'  arqueologia-2.0.0.jar\n'
    with zipfile.ZipFile(target,'w',zipfile.ZIP_DEFLATED) as archive:
        for relative in entries:
            content=(ROOT/relative).read_bytes()
            suffix=Path(relative).suffix
            if suffix in {'.sh','.sql','.yaml'}:content=content.replace(b'\r\n',b'\n')
            elif suffix in {'.bat','.ps1'}:content=content.replace(b'\r\n',b'\n').replace(b'\n',b'\r\n')
            archive.writestr(prefix+relative,content)
        archive.writestr(prefix+'dist/SHA256SUMS',jar_line)
    (ROOT/'dist/SHA256SUMS').write_text(jar_line+digest(target)+'  arqueologia-etapa2.zip\n',encoding='ascii')
    with zipfile.ZipFile(target) as archive:
        if archive.testzip() is not None:raise RuntimeError('Falha na verificacao do ZIP.')
    print(f'Pacote pronto: {target.name}; {len(entries)+1} arquivos; {target.stat().st_size} bytes.')
if __name__=='__main__':package()
