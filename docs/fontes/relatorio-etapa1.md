# Sistema de Gestão de Expedições Arqueológicas

## Integrantes

- Arthur Tavares Silva
- Higor Samuel Ferreira Silva
- Igor Vendramini Perini
- João Gabriel da Cruz Viana

## Escopo

Banco de dados para rastrear expedições arqueológicas, equipes, unidades de escavação, artefatos, análises, conservação e cadeia temporal de custódia. O sistema não substitui licenças governamentais, não armazena imagens binárias nesta etapa e utiliza somente dados sintéticos.

## Regras de negócio

- **RN01** Cada instituição deve possuir nome único e ser classificada como universidade, museu, laboratório ou órgão público. Atendimento: uq_instituicao_nome e ck_instituicao_tipo.
- **RN02** Cada pesquisador deve estar vinculado a uma instituição e possuir e-mail único. Atendimento: fk_pesquisador_instituicao e uq_pesquisador_email.
- **RN03** Todo pesquisador deve pertencer a exatamente uma especialização: arqueólogo, conservador ou analista laboratorial. Atendimento: tipo_especializacao e tabelas de subtipo; consistência verificada na consulta 15.
- **RN04** Somente pesquisadores ativos podem participar de novas campanhas. Atendimento: validação operacional; consulta 15 verifica inconsistências.
- **RN05** Cada sítio arqueológico deve possuir nome, município, UF e coordenadas geográficas válidas. Atendimento: NOT NULL e checks de latitude e longitude.
- **RN06** O código de cada campanha deve ser único em todo o banco. Atendimento: uq_campanha_codigo.
- **RN07** Cada campanha ocorre em um único sítio, enquanto um sítio pode receber várias campanhas. Atendimento: fk_campanha_sitio.
- **RN08** O coordenador de uma campanha deve ser um pesquisador da especialização arqueólogo. Atendimento: fk_campanha_coordenador e verificação pela consulta 15.
- **RN09** A data final de uma campanha não pode ser anterior à data inicial. Atendimento: ck_campanha_periodo.
- **RN10** Um pesquisador pode participar de várias campanhas e cada campanha pode possuir vários pesquisadores, registrando função, período e carga horária. Atendimento: participacao_campanha com PK composta e atributos próprios.
- **RN11** A data de saída de uma participação não pode preceder a data de entrada. Atendimento: ck_participacao_periodo.
- **RN12** Cada unidade de escavação pertence a uma campanha e seu código é único dentro dessa campanha. Atendimento: fk_unidade_campanha e uq_unidade_codigo.
- **RN13** Cada artefato deve possuir código de catálogo único e estar associado à unidade em que foi encontrado. Atendimento: uq_artefato_catalogo e fk_artefato_unidade.
- **RN14** A profundidade de descoberta e as dimensões físicas não podem assumir valores negativos. Atendimento: checks de artefato e fragmento.
- **RN15** Um artefato pode referenciar outro artefato como conjunto principal, mas não pode referenciar a si próprio. Atendimento: fk_artefato_referencia e triggers de validação.
- **RN16** Um fragmento só existe como parte de um artefato e é identificado pelo número sequencial dentro dele. Atendimento: fragmento com PK composta e FK com ON DELETE CASCADE.
- **RN17** Cada análise laboratorial deve ser realizada por uma instituição do tipo laboratório. Atendimento: fk_analise_laboratorio e verificação pela consulta 15.
- **RN18** Um artefato pode participar de várias análises e uma análise pode abranger vários artefatos, com amostra e resultado registrados na associação. Atendimento: artefato_analise com PK composta e atributos próprios.
- **RN19** A massa consumida por uma análise não pode ser negativa. Atendimento: ck_artefato_analise_massa.
- **RN20** Cada intervenção de conservação deve indicar artefato, conservador, período e estado de execução. Atendimento: FKs, NOT NULL e checks de intervenção.
- **RN21** A data final de uma intervenção não pode ser anterior à data inicial. Atendimento: ck_intervencao_periodo.
- **RN22** Cada local de armazenamento possui código único dentro de sua instituição. Atendimento: uq_local_codigo.
- **RN23** Todo período de custódia deve indicar artefato, instituição responsável, data inicial e documento de movimentação. Atendimento: NOT NULL e FKs em custodia_artefato.
- **RN24** Um artefato pode possuir vários registros históricos de custódia, mas apenas um registro vigente sem data final. Atendimento: uq_custodia_aberta sobre coluna gerada.
- **RN25** Quando informado, o local de armazenamento deve pertencer à mesma instituição responsável pela custódia. Atendimento: verificação pela consulta 15 e pela aplicação futura.

## Modelo conceitual

Consulte `mer-conceitual.mmd` e `../mer-conceitual.pdf`. Todas as associações exibem cardinalidade mínima e máxima. A especialização de pesquisador é total e exclusiva.

## Dicionário de dados integral

### instituicao

| Atributo | Domínio | Obrigatório | Chave | Descrição |
|---|---|---|---|---|
| id_instituicao | INT | Sim | PK | Identificador interno |
| nome | VARCHAR(120) | Sim | UQ | Nome oficial ou fictício |
| sigla | VARCHAR(20) | Não |  | Sigla institucional |
| tipo | VARCHAR(20) | Sim | CK | UNIVERSIDADE, MUSEU, LABORATORIO ou ORGAO_PUBLICO |
| pais | VARCHAR(60) | Sim |  | País da instituição |
| uf | CHAR(2) | Não |  | Unidade federativa |
| cidade | VARCHAR(80) | Sim |  | Cidade |

### pesquisador

| Atributo | Domínio | Obrigatório | Chave | Descrição |
|---|---|---|---|---|
| id_pesquisador | INT | Sim | PK | Identificador interno |
| id_instituicao | INT | Sim | FK | Instituição de vínculo |
| nome | VARCHAR(120) | Sim |  | Nome do pesquisador |
| email | VARCHAR(160) | Sim | UQ | E-mail institucional |
| registro_profissional | VARCHAR(40) | Não | UQ | Registro profissional quando aplicável |
| tipo_especializacao | VARCHAR(24) | Sim | CK | Discriminador da especialização |
| situacao | VARCHAR(10) | Sim | CK | ATIVO ou INATIVO |

### arqueologo

| Atributo | Domínio | Obrigatório | Chave | Descrição |
|---|---|---|---|---|
| id_pesquisador | INT | Sim | PK/FK | Identifica o pesquisador especializado |
| area_pesquisa | VARCHAR(100) | Sim |  | Área de pesquisa arqueológica |
| numero_autorizacao | VARCHAR(40) | Sim | UQ | Autorização profissional |

### conservador

| Atributo | Domínio | Obrigatório | Chave | Descrição |
|---|---|---|---|---|
| id_pesquisador | INT | Sim | PK/FK | Identifica o pesquisador especializado |
| especialidade_material | VARCHAR(80) | Sim |  | Material de especialização |
| numero_registro | VARCHAR(40) | Sim | UQ | Registro profissional |

### analista_laboratorial

| Atributo | Domínio | Obrigatório | Chave | Descrição |
|---|---|---|---|---|
| id_pesquisador | INT | Sim | PK/FK | Identifica o pesquisador especializado |
| especialidade_analise | VARCHAR(100) | Sim |  | Especialidade técnica |
| nivel_formacao | VARCHAR(30) | Sim |  | Titulação |

### sitio_arqueologico

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

### campanha

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

### participacao_campanha

| Atributo | Domínio | Obrigatório | Chave | Descrição |
|---|---|---|---|---|
| id_campanha | INT | Sim | PK/FK | Campanha |
| id_pesquisador | INT | Sim | PK/FK | Pesquisador |
| funcao | VARCHAR(60) | Sim |  | Função na campanha |
| data_entrada | DATE | Sim |  | Início da participação |
| data_saida | DATE | Não | CK | Fim da participação |
| horas_previstas | SMALLINT | Sim | CK | Carga horária positiva |

### unidade_escavacao

| Atributo | Domínio | Obrigatório | Chave | Descrição |
|---|---|---|---|---|
| id_unidade | INT | Sim | PK | Identificador interno |
| id_campanha | INT | Sim | FK | Campanha responsável |
| codigo | VARCHAR(20) | Sim | UQ composto | Código dentro da campanha |
| setor | VARCHAR(40) | Sim |  | Setor espacial |
| nivel_estratigrafico | VARCHAR(40) | Não |  | Nível estratigráfico |
| data_abertura | DATE | Sim |  | Data de abertura |
| data_encerramento | DATE | Não | CK | Data de encerramento |

### artefato

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

### fragmento

| Atributo | Domínio | Obrigatório | Chave | Descrição |
|---|---|---|---|---|
| id_artefato | INT | Sim | PK/FK | Artefato proprietário |
| numero_fragmento | SMALLINT | Sim | PK | Número dentro do artefato |
| peso_g | DECIMAL(9,3) | Sim | CK | Peso não negativo |
| comprimento_mm | DECIMAL(8,2) | Não | CK | Comprimento não negativo |
| descricao | VARCHAR(240) | Não |  | Características do fragmento |
| localizado | BOOLEAN | Sim |  | Indica presença física |

### analise_laboratorial

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

### artefato_analise

| Atributo | Domínio | Obrigatório | Chave | Descrição |
|---|---|---|---|---|
| id_artefato | INT | Sim | PK/FK | Artefato analisado |
| id_analise | INT | Sim | PK/FK | Análise |
| codigo_amostra | VARCHAR(32) | Sim | UQ | Código da amostra |
| massa_consumida_g | DECIMAL(8,3) | Sim | CK | Massa consumida |
| resultado_resumido | VARCHAR(300) | Não |  | Resultado sintético |
| conclusao | TEXT | Não |  | Conclusão técnica |

### intervencao_conservacao

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

### local_armazenamento

| Atributo | Domínio | Obrigatório | Chave | Descrição |
|---|---|---|---|---|
| id_local | INT | Sim | PK | Identificador interno |
| id_instituicao | INT | Sim | FK | Instituição proprietária |
| codigo | VARCHAR(24) | Sim | UQ composto | Código interno |
| tipo | VARCHAR(30) | Sim |  | Reserva técnica, laboratório ou exposição |
| temperatura_c | DECIMAL(5,2) | Não |  | Temperatura de referência |
| umidade_percentual | DECIMAL(5,2) | Não | CK | Umidade entre 0 e 100 |
| capacidade | INT | Sim | CK | Capacidade positiva |

### custodia_artefato

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

## Modelo lógico

Consulte `modelo-logico.md` e `../modelo-logico.pdf`.

### Decisões de mapeamento por relação

| Relação | Chave e estratégia | Justificativa |
|---|---|---|
| instituicao | id_instituicao substituta; nome é candidata | O nome pode sofrer ajuste institucional; a chave inteira mantém relacionamentos estáveis e UNIQUE preserva a identificação de negócio. |
| pesquisador | id_pesquisador substituta; email é candidata | Email e registro podem mudar ou ser ausentes; o identificador interno evita propagação de alterações. |
| arqueologo | PK herdada id_pesquisador | Tabela de subtipo evita nulos no supertipo e implementa a especialização total e exclusiva em conjunto com tipo_especializacao e auditoria. |
| conservador | PK herdada id_pesquisador | A PK também é FK para o supertipo, garantindo que nenhum conservador exista sem pesquisador. |
| analista_laboratorial | PK herdada id_pesquisador | A estratégia supertipo mais subtipo preserva atributos específicos sem repetição dos atributos comuns. |
| sitio_arqueologico | id_sitio substituta; nome é candidata | O nome é legível, mas pode ser revisto; a chave substituta permanece estável durante a pesquisa. |
| campanha | id_campanha substituta; codigo é candidata | O código atende ao negócio e é UNIQUE, enquanto a PK inteira simplifica FKs e possíveis revisões administrativas. |
| participacao_campanha | PK composta campanha e pesquisador | A ocorrência só existe pela combinação dos participantes; função, período e horas dependem da associação completa. |
| unidade_escavacao | id_unidade substituta; campanha e codigo são candidatas | O código é único apenas dentro da campanha; a PK simples reduz o tamanho das referências de artefato. |
| artefato | id_artefato substituta; codigo_catalogo é candidata | O código de catálogo permanece UNIQUE, mas a PK interna sustenta autorreferência e integrações sem acoplar o banco ao formato do código. |
| fragmento | PK composta artefato e numero | É entidade fraca: o número identifica o fragmento somente no contexto de seu artefato proprietário. |
| analise_laboratorial | id_analise substituta | Não há código natural estável para todos os métodos; a chave sequencial identifica cada execução laboratorial. |
| artefato_analise | PK composta artefato e análise; amostra é candidata | A tabela resolve o N:N e armazena atributos que pertencem ao exame daquele artefato naquela análise. |
| intervencao_conservacao | id_intervencao substituta | Uma intervenção não possui identificador natural confiável; data, tipo e responsável podem se repetir. |
| local_armazenamento | id_local substituta; instituição e codigo são candidatas | O código do local é contextual à instituição; a PK simples facilita o histórico de custódia. |
| custodia_artefato | id_custodia substituta; documento e artefato mais início são candidatas | A chave interna identifica o evento; unicidades impedem documento repetido, início duplicado e mais de uma custódia aberta. |

## Normalização

O esquema foi analisado em 1FN, 2FN e 3FN; não houve desnormalização deliberada.

| Relação | Determinantes candidatos | Dependência e conclusão | Forma |
|---|---|---|---|
| instituicao | id_instituicao; nome | Cada determinante é chave candidata; demais atributos dependem integralmente dela. | BCNF |
| pesquisador | id_pesquisador; email | Instituição, nome, registro, especialização e situação dependem da chave escolhida. | 3FN/BCNF |
| arqueologo | id_pesquisador; numero_autorizacao | Área de pesquisa e autorização descrevem um único arqueólogo. | BCNF |
| conservador | id_pesquisador; numero_registro | Especialidade e registro dependem do identificador do subtipo. | BCNF |
| analista_laboratorial | id_pesquisador | Especialidade e formação dependem diretamente da PK herdada. | BCNF |
| sitio_arqueologico | id_sitio; nome | Localização, coordenadas, período, proteção e cadastro dependem da chave. | BCNF |
| campanha | id_campanha; codigo | Sítio, coordenador, objetivo, período, situação e orçamento dependem da campanha. | BCNF |
| participacao_campanha | id_campanha + id_pesquisador | Função, datas e horas dependem da chave composta inteira; não há dependência parcial. | BCNF |
| unidade_escavacao | id_unidade; id_campanha + codigo | Setor, nível e período dependem de cada chave candidata completa. | BCNF |
| artefato | id_artefato; codigo_catalogo | Contexto, descrição, material, período, descoberta, estado e situação dependem da chave. | BCNF |
| fragmento | id_artefato + numero_fragmento | Peso, dimensão, descrição e localização dependem da chave composta inteira. | BCNF |
| analise_laboratorial | id_analise | Laboratório, método, período, situação e custo dependem da execução identificada. | BCNF |
| artefato_analise | id_artefato + id_analise; codigo_amostra | Massa, resultado e conclusão dependem do relacionamento completo ou da amostra única. | BCNF |
| intervencao_conservacao | id_intervencao | Artefato, conservador, tratamento, período, materiais, situação e custo dependem da PK. | BCNF |
| local_armazenamento | id_local; id_instituicao + codigo | Tipo, condições ambientais e capacidade dependem de cada chave candidata completa. | BCNF |
| custodia_artefato | id_custodia; documento; id_artefato + data_inicio | Instituição, local, responsável, fim e motivo dependem do evento; a coluna gerada apenas materializa a regra de vigência. | 3FN |

## Validação

Os scripts foram executados em sequência no MySQL Community Server 8.4.11. Foram confirmadas 16 tabelas, 22 chaves estrangeiras, 28 CHECKs, 2 triggers, os volumes de carga e as quinze consultas. A auditoria semântica retornou zero violações.

## Uso de inteligência artificial

A equipe utilizou IA como apoio à estruturação, geração de dados sintéticos, documentação e revisão de SQL. O resultado foi verificado por execução no MySQL, testes de restrições e revisão visual dos PDFs.
