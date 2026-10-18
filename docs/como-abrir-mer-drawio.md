# MER editável no diagrams.net

Arquivo: [fontes/mer-conceitual.drawio](fontes/mer-conceitual.drawio).
Prévia: [mer-drawio-previa.pdf](mer-drawio-previa.pdf).

1. Acesse https://app.diagrams.net/ e escolha **Dispositivo** como local de armazenamento.
2. Use **Arquivo → Abrir de → Dispositivo** e selecione `mer-conceitual.drawio`.
3. Navegue pelas cinco abas na parte inferior: visão completa, equipes e especialização, campo e acervo, análises e conservação, cadeia de custódia.
4. Para editar, mova as entidades, ajuste os pontos das conexões ou dê dois cliques nos textos. Cada atributo e cada cardinalidade é um objeto editável.
5. Salve pelo menu **Arquivo → Salvar como**. Para PDF, use **Arquivo → Exportar como → PDF**, escolhendo as páginas desejadas.

A primeira aba reúne as 16 entidades e os 22 relacionamentos. As demais apresentam recortes ampliados do mesmo modelo, repetindo entidades como referência. Identificadores estão sublinhados; FRAGMENTO é identificado por artefato e número. A especialização total/exclusiva (T,X) está indicada dentro de PESQUISADOR e conectada diretamente aos três subtipos.

Os números nas pontas seguem o pé de galinha: indicam quantas instâncias da entidade naquela ponta podem se relacionar com uma instância da entidade oposta. Por exemplo, em INSTITUIÇÃO–PESQUISADOR, há (1,1) junto de INSTITUIÇÃO e (0,N) junto de PESQUISADOR.

O arquivo XML foi validado e a prévia vetorial foi gerada com as mesmas posições e rotas. A abertura no editor web não foi testada nesta sessão, pois o navegador conectado não estava disponível.

O gerador `python scripts/gerar_drawio.py` recria essa versão a partir do DDL e do posicionamento definido no código. Se editar o arquivo no site, guarde uma cópia antes de regenerá-lo, para preservar seus ajustes manuais.
