# Dicionário de dados

## instituicao

| Atributo | Domínio | Obrigatório | Chave | Descrição |
|---|---|---|---|---|
| id_instituicao | INT | Sim | PK | Identificador interno |
| nome | VARCHAR(120) | Sim | UQ | Nome oficial ou fictício |
| sigla | VARCHAR(20) | Não |  | Sigla institucional |
| tipo | VARCHAR(20) | Sim | CK | UNIVERSIDADE, MUSEU, LABORATORIO ou ORGAO_PUBLICO |
| pais | VARCHAR(60) | Sim |  | País da instituição |
| uf | CHAR(2) | Não |  | Unidade federativa |
| cidade | VARCHAR(80) | Sim |  | Cidade |

## pesquisador

| Atributo | Domínio | Obrigatório | Chave | Descrição |
|---|---|---|---|---|
| id_pesquisador | INT | Sim | PK | Identificador interno |
| id_instituicao | INT | Sim | FK | Instituição de vínculo |
| nome | VARCHAR(120) | Sim |  | Nome do pesquisador |
| email | VARCHAR(160) | Sim | UQ | E-mail institucional |
| registro_profissional | VARCHAR(40) | Não | UQ | Registro profissional quando aplicável |
| tipo_especializacao | VARCHAR(24) | Sim | CK | Discriminador da especialização |
| situacao | VARCHAR(10) | Sim | CK | ATIVO ou INATIVO |

## arqueologo

| Atributo | Domínio | Obrigatório | Chave | Descrição |
|---|---|---|---|---|
| id_pesquisador | INT | Sim | PK/FK | Identifica o pesquisador especializado |
| area_pesquisa | VARCHAR(100) | Sim |  | Área de pesquisa arqueológica |
| numero_autorizacao | VARCHAR(40) | Sim | UQ | Autorização profissional |

## conservador

| Atributo | Domínio | Obrigatório | Chave | Descrição |
|---|---|---|---|---|
| id_pesquisador | INT | Sim | PK/FK | Identifica o pesquisador especializado |
| especialidade_material | VARCHAR(80) | Sim |  | Material de especialização |
| numero_registro | VARCHAR(40) | Sim | UQ | Registro profissional |

## analista_laboratorial

| Atributo | Domínio | Obrigatório | Chave | Descrição |
|---|---|---|---|---|
| id_pesquisador | INT | Sim | PK/FK | Identifica o pesquisador especializado |
| especialidade_analise | VARCHAR(100) | Sim |  | Especialidade técnica |
| nivel_formacao | VARCHAR(30) | Sim |  | Titulação |

## sitio_arqueologico

| Atributo | Domínio | Obrigatório | Chave | Descrição |
|---|---|---|---|---|
| id_sitio | INT | Sim | PK | Identificador interno |
| nome | VARCHAR(120) | Sim | UQ | Nome do sítio |
| municipio | VARCHAR(80) | Sim |  | Município |
| uf | CHAR(2) | Sim |  | Unidade federativa |
| latitude | DECIMAL(9,6) | Sim | CK | Latitude entre -90 e 90 |
| longitude | DECIMAL(9,6) | Sim | CK | Longitude entre -180 e 180 |
| periodo_historico | VARCHAR(100) | Não |  | Período histórico estimado |
| nivel_protecao | VARCHAR(20) | Sim | CK | Proteção legal |
| data_cadastro | DATE | Sim |  | Data de cadastro |

## campanha

| Atributo | Domínio | Obrigatório | Chave | Descrição |
|---|---|---|---|---|
| id_campanha | INT | Sim | PK | Identificador interno |
| id_sitio | INT | Sim | FK | Sítio da campanha |
| id_coordenador | INT | Sim | FK | Pesquisador coordenador |
| codigo | VARCHAR(24) | Sim | UQ | Código público da campanha |
| nome | VARCHAR(140) | Sim |  | Nome da campanha |
| objetivo | TEXT | Sim |  | Objetivo científico |
| data_inicio | DATE | Sim |  | Início |
| data_fim | DATE | Não | CK | Encerramento |
| situacao | VARCHAR(16) | Sim | CK | PLANEJADA, EM_ANDAMENTO, CONCLUIDA ou SUSPENSA |
| orcamento | DECIMAL(12,2) | Sim | CK | Orçamento não negativo |

## participacao_campanha

| Atributo | Domínio | Obrigatório | Chave | Descrição |
|---|---|---|---|---|
| id_campanha | INT | Sim | PK/FK | Campanha |
| id_pesquisador | INT | Sim | PK/FK | Pesquisador |
| funcao | VARCHAR(60) | Sim |  | Função na campanha |
| data_entrada | DATE | Sim |  | Início da participação |
| data_saida | DATE | Não | CK | Fim da participação |
| horas_previstas | SMALLINT | Sim | CK | Carga horária positiva |

## unidade_escavacao

| Atributo | Domínio | Obrigatório | Chave | Descrição |
|---|---|---|---|---|
| id_unidade | INT | Sim | PK | Identificador interno |
| id_campanha | INT | Sim | FK | Campanha responsável |
| codigo | VARCHAR(20) | Sim | UQ composto | Código dentro da campanha |
| setor | VARCHAR(40) | Sim |  | Setor espacial |
| nivel_estratigrafico | VARCHAR(40) | Não |  | Nível estratigráfico |
| data_abertura | DATE | Sim |  | Data de abertura |
| data_encerramento | DATE | Não | CK | Data de encerramento |

## artefato

| Atributo | Domínio | Obrigatório | Chave | Descrição |
|---|---|---|---|---|
| id_artefato | INT | Sim | PK | Identificador interno |
| id_unidade | INT | Sim | FK | Unidade de descoberta |
| id_artefato_referencia | INT | Não | FK autorreferente | Conjunto ou artefato principal |
| codigo_catalogo | VARCHAR(32) | Sim | UQ | Código de catálogo |
| nome | VARCHAR(120) | Sim |  | Denominação |
| descricao | TEXT | Sim |  | Descrição técnica |
| material | VARCHAR(60) | Sim |  | Material predominante |
| periodo_estimado | VARCHAR(100) | Não |  | Datação estimada |
| data_descoberta | DATE | Sim |  | Data de descoberta |
| profundidade_cm | DECIMAL(7,2) | Sim | CK | Profundidade não negativa |
| estado_conservacao | VARCHAR(16) | Sim | CK | Estado físico |
| situacao | VARCHAR(18) | Sim | CK | Situação operacional |

## fragmento

| Atributo | Domínio | Obrigatório | Chave | Descrição |
|---|---|---|---|---|
| id_artefato | INT | Sim | PK/FK | Artefato proprietário |
| numero_fragmento | SMALLINT | Sim | PK | Número dentro do artefato |
| peso_g | DECIMAL(9,3) | Sim | CK | Peso não negativo |
| comprimento_mm | DECIMAL(8,2) | Não | CK | Comprimento não negativo |
| descricao | VARCHAR(240) | Não |  | Características do fragmento |
| localizado | BOOLEAN | Sim |  | Indica presença física |

## analise_laboratorial

| Atributo | Domínio | Obrigatório | Chave | Descrição |
|---|---|---|---|---|
| id_analise | INT | Sim | PK | Identificador interno |
| id_laboratorio | INT | Sim | FK | Instituição executora |
| tipo_analise | VARCHAR(60) | Sim |  | Tipo do exame |
| metodo | VARCHAR(120) | Sim |  | Método aplicado |
| data_inicio | DATE | Sim |  | Início |
| data_fim | DATE | Não | CK | Conclusão |
| situacao | VARCHAR(16) | Sim | CK | SOLICITADA, EM_EXECUCAO, CONCLUIDA ou CANCELADA |
| custo | DECIMAL(10,2) | Sim | CK | Custo não negativo |

## artefato_analise

| Atributo | Domínio | Obrigatório | Chave | Descrição |
|---|---|---|---|---|
| id_artefato | INT | Sim | PK/FK | Artefato analisado |
| id_analise | INT | Sim | PK/FK | Análise |
| codigo_amostra | VARCHAR(32) | Sim | UQ | Código da amostra |
| massa_consumida_g | DECIMAL(8,3) | Sim | CK | Massa consumida |
| resultado_resumido | VARCHAR(300) | Não |  | Resultado sintético |
| conclusao | TEXT | Não |  | Conclusão técnica |

## intervencao_conservacao

| Atributo | Domínio | Obrigatório | Chave | Descrição |
|---|---|---|---|---|
| id_intervencao | INT | Sim | PK | Identificador interno |
| id_artefato | INT | Sim | FK | Artefato conservado |
| id_conservador | INT | Sim | FK | Pesquisador conservador |
| tipo_intervencao | VARCHAR(80) | Sim |  | Procedimento |
| data_inicio | DATE | Sim |  | Início |
| data_fim | DATE | Não | CK | Conclusão |
| descricao | TEXT | Sim |  | Descrição do tratamento |
| material_aplicado | VARCHAR(160) | Não |  | Materiais empregados |
| situacao | VARCHAR(16) | Sim | CK | PLANEJADA, EM_EXECUCAO, CONCLUIDA ou CANCELADA |
| custo | DECIMAL(10,2) | Sim | CK | Custo não negativo |

## local_armazenamento

| Atributo | Domínio | Obrigatório | Chave | Descrição |
|---|---|---|---|---|
| id_local | INT | Sim | PK | Identificador interno |
| id_instituicao | INT | Sim | FK | Instituição proprietária |
| codigo | VARCHAR(24) | Sim | UQ composto | Código interno |
| tipo | VARCHAR(30) | Sim |  | Reserva técnica, laboratório ou exposição |
| temperatura_c | DECIMAL(5,2) | Não |  | Temperatura de referência |
| umidade_percentual | DECIMAL(5,2) | Não | CK | Umidade entre 0 e 100 |
| capacidade | INT | Sim | CK | Capacidade positiva |

## custodia_artefato

| Atributo | Domínio | Obrigatório | Chave | Descrição |
|---|---|---|---|---|
| id_custodia | INT | Sim | PK | Identificador interno |
| id_artefato | INT | Sim | FK | Artefato custodiado |
| id_instituicao | INT | Sim | FK | Instituição responsável |
| id_local | INT | Não | FK | Local de armazenamento |
| id_responsavel | INT | Sim | FK | Pesquisador responsável |
| data_inicio | DATE | Sim |  | Início da custódia |
| data_fim | DATE | Não | CK | Fim da custódia |
| documento_movimentacao | VARCHAR(40) | Sim | UQ | Documento de transferência |
| motivo | VARCHAR(200) | Sim |  | Motivo da movimentação |
| id_artefato_em_aberto | INT gerado | Não | UQ | Garante uma custódia vigente por artefato |
