# Sistema de Gestão de Expedições Arqueológicas

Laboratório de Banco de Dados — Projeto Final 2026/2 — Etapa 1 revisada em 18/10/2026.

## Integrantes

- Arthur Tavares Silva
- Higor Samuel Ferreira Silva
- Igor Vendramini Perini
- João Gabriel da Cruz Viana

## A1 — Escopo e regras de negócio

O banco rastreia instituições, pesquisadores, sítios, campanhas, unidades de escavação, artefatos, fragmentos, análises laboratoriais, conservação e cadeia temporal de custódia. O objetivo é acompanhar a descoberta, o tratamento e a guarda de um vestígio. Todos os dados de carga são fictícios. MySQL 8 foi escolhido pela equipe entre os SGBDs permitidos no enunciado.

Licenças governamentais e imagens binárias ficam fora do escopo. Interface, autenticação e experimentos operacionais serão tratados posteriormente na Etapa 2. Campanhas podem existir antes da abertura de unidades; artefatos podem existir sem fragmentos e ainda sem a primeira custódia. Custódias usam períodos [início,fim), permitindo transferência na data de encerramento da custódia anterior.

### A1.1 — Regras de negócio

- **RN01** Cada instituição deve possuir nome único e ser classificada como universidade, museu, laboratório ou órgão público. Atendimento: uq_instituicao_nome e ck_instituicao_tipo.
- **RN02** Cada pesquisador deve estar vinculado a uma instituição e possuir e-mail único. Atendimento: fk_pesquisador_instituicao e uq_pesquisador_email.
- **RN03** Todo pesquisador deve pertencer a exatamente uma especialização: arqueólogo, conservador ou analista laboratorial. Atendimento: tipo_especializacao e tabelas de subtipo; consistência verificada na consulta 15.
- **RN04** Somente pesquisadores ativos podem participar de novas campanhas. Atendimento atual: consulta 15. O bloqueio da entrada de inativos em novas campanhas fica para a aplicação da Etapa 2; a regra não equivale a proibir a inativação posterior de um participante histórico.
- **RN05** Cada sítio arqueológico deve possuir nome, município, UF e coordenadas geográficas válidas. Atendimento: NOT NULL e checks de latitude e longitude.
- **RN06** O código de cada campanha deve ser único em todo o banco. Atendimento: uq_campanha_codigo.
- **RN07** Cada campanha ocorre em um único sítio, enquanto um sítio pode receber várias campanhas. Atendimento: fk_campanha_sitio.
- **RN08** O coordenador de uma campanha deve ser um pesquisador da especialização arqueólogo. Atendimento: fk_campanha_coordenador; triggers de INSERT/UPDATE de campanha e proteção de alteração do pesquisador; consulta 15 como verificação complementar.
- **RN09** A data final de uma campanha não pode ser anterior à data inicial. Atendimento: ck_campanha_periodo.
- **RN10** Um pesquisador pode participar de várias campanhas e cada campanha pode possuir vários pesquisadores, registrando função, período e carga horária. Atendimento: participacao_campanha com PK composta e atributos próprios.
- **RN11** A data de saída de uma participação não pode preceder a data de entrada. Atendimento: ck_participacao_periodo.
- **RN12** Cada unidade de escavação pertence a uma campanha e seu código é único dentro dessa campanha. Atendimento: fk_unidade_campanha e uq_unidade_codigo.
- **RN13** Cada artefato deve possuir código de catálogo único e estar associado à unidade em que foi encontrado. Atendimento: uq_artefato_catalogo e fk_artefato_unidade.
- **RN14** A profundidade de descoberta e as dimensões físicas não podem assumir valores negativos. Atendimento: checks de artefato e fragmento.
- **RN15** Um artefato pode referenciar outro artefato como conjunto principal, mas não pode referenciar a si próprio. Atendimento: fk_artefato_referencia e triggers de validação.
- **RN16** Um fragmento só existe como parte de um artefato e é identificado pelo número sequencial dentro dele. Atendimento: fragmento com PK composta e FK com ON DELETE CASCADE.
- **RN17** Cada análise laboratorial deve ser realizada por uma instituição do tipo laboratório. Atendimento: fk_analise_laboratorio; triggers de INSERT/UPDATE de análise e proteção de alteração da instituição; consulta 15 como verificação complementar.
- **RN18** Um artefato pode participar de várias análises e uma análise pode abranger vários artefatos, com amostra e resultado registrados na associação. Atendimento: artefato_analise com PK composta e atributos próprios.
- **RN19** A massa consumida por uma análise não pode ser negativa. Atendimento: ck_artefato_analise_massa.
- **RN20** Cada intervenção de conservação deve indicar artefato, conservador, período e estado de execução. Atendimento: FKs, NOT NULL e checks de intervenção.
- **RN21** A data final de uma intervenção não pode ser anterior à data inicial. Atendimento: ck_intervencao_periodo.
- **RN22** Cada local de armazenamento possui código único dentro de sua instituição. Atendimento: uq_local_codigo.
- **RN23** Todo período de custódia deve indicar artefato, instituição responsável, data inicial e documento de movimentação. Atendimento: NOT NULL e FKs em custodia_artefato.
- **RN24** Um artefato pode possuir vários registros históricos de custódia, mas apenas um registro vigente sem data final. Atendimento: uq_custodia_aberta sobre coluna gerada.
- **RN25** Quando informado, o local de armazenamento deve pertencer à mesma instituição responsável pela custódia. Atendimento: triggers de INSERT/UPDATE de custódia e proteção de alteração da instituição do local; consulta 15 como verificação complementar.


## A2 — Modelo Entidade-Relacionamento conceitual

O MER utiliza exclusivamente a notação de Peter Chen: entidades em retângulos, relacionamentos em losangos e atributos em elipses. O PDF começa pela visão completa em uma página própria e continua com quatro folhas de detalhe, que exibem os atributos e seus identificadores. As folhas são vetoriais e podem ser ampliadas sem perda de nitidez.

A especialização do pesquisador é total e exclusiva: a linha dupla até o círculo d indica totalidade, e d indica subtipos disjuntos. Não existe uma entidade artificial de especialização. Os atributos identificadores dos subtipos são herdados do pesquisador. FRAGMENTO é entidade fraca, com retângulo duplo e relacionamento identificador duplo; numero_fragmento é o discriminador, cuja identificação depende do artefato.

Os relacionamentos N:N Participa (pesquisador–campanha) e É analisado em (artefato–análise) possuem atributos próprios, exibidos nas folhas de detalhe. As respectivas tabelas associativas pertencem ao modelo lógico A4. CUSTODIA_ARTEFATO é entidade de histórico datado. A coluna física gerada de custódia aberta não é um atributo conceitual independente.

[INSERIR_MER]

### A2.1 — Cardinalidades para conferência

Na notação de Chen, (mín,máx) informa quantas ocorrências do relacionamento podem envolver uma instância da entidade daquele lado. Exemplo: uma instituição vincula zero ou muitos pesquisadores; cada pesquisador tem exatamente uma instituição. A leitura difere da posição dos símbolos de pé de galinha usados no modelo lógico.

| Entidade A | Relacionamento | Participação A | Entidade B | Participação B |
|---|---|---|---|---|
| instituicao | Vincula | (0,N) | pesquisador | (1,1) |
| pesquisador | Participa | (0,N) | campanha | (0,N) |
| sitio_arqueologico | Recebe | (0,N) | campanha | (1,1) |
| arqueologo | Coordena | (0,N) | campanha | (1,1) |
| campanha | Possui | (0,N) | unidade_escavacao | (1,1) |
| unidade_escavacao | Revela | (0,N) | artefato | (1,1) |
| artefato | Referencia | (0,1) | artefato | (0,N) |
| artefato | Compoe | (0,N) | fragmento | (1,1) |
| instituicao | Executa | (0,N) | analise_laboratorial | (1,1) |
| artefato | É analisado em | (0,N) | analise_laboratorial | (0,N) |
| conservador | Realiza | (0,N) | intervencao_conservacao | (1,1) |
| artefato | Recebe | (0,N) | intervencao_conservacao | (1,1) |
| instituicao | Possui | (0,N) | local_armazenamento | (1,1) |
| artefato | Tem histórico | (0,N) | custodia_artefato | (1,1) |
| instituicao | Assume | (0,N) | custodia_artefato | (1,1) |
| local_armazenamento | Abriga | (0,N) | custodia_artefato | (0,1) |
| pesquisador | Responsabiliza-se | (0,N) | custodia_artefato | (1,1) |

Na autorrelação Referencia, um artefato no papel de parte aponta para no máximo um conjunto; no papel de conjunto pode reunir várias partes. A autorreferência direta ao mesmo identificador é proibida pela RN15.

As cardinalidades das associações não substituem as regras semânticas: o coordenador é arqueólogo, a instituição que executa análises é laboratório, a intervenção tem conservador e o local pertence à instituição da custódia. A generalização é representada por totalidade/exclusividade, e não por uma associação binária adicional.

## A3 — Dicionário de dados conceitual e correspondência física

O dicionário integral está nas próximas páginas, sem depender da abertura de um arquivo separado. As descrições e domínios explicitam o significado dos atributos; as referências RN por entidade permitem rastrear as regras. Os tipos são a correspondência física dos domínios. A coluna gerada é assinalada como detalhe físico.

### instituicao

Regras associadas: RN01.

| Atributo | Domínio | Obrig. | Chave | Descrição |
|---|---|---|---|---|
| id_instituicao | INT | Sim | PK | Identificador interno |
| nome | VARCHAR(120) | Sim | UQ | Nome oficial ou fictício |
| sigla | VARCHAR(20) | Não |  | Sigla institucional |
| tipo | VARCHAR(20) | Sim | CK | UNIVERSIDADE, MUSEU, LABORATORIO ou ORGAO_PUBLICO |
| pais | VARCHAR(60) | Sim |  | País da instituição |
| uf | CHAR(2) | Não |  | Unidade federativa |
| cidade | VARCHAR(80) | Sim |  | Cidade |

### pesquisador

Regras associadas: RN02–RN04.

| Atributo | Domínio | Obrig. | Chave | Descrição |
|---|---|---|---|---|
| id_pesquisador | INT | Sim | PK | Identificador interno |
| id_instituicao | INT | Sim | FK | Instituição de vínculo |
| nome | VARCHAR(120) | Sim |  | Nome do pesquisador |
| email | VARCHAR(160) | Sim | UQ | E-mail institucional |
| registro_profissional | VARCHAR(40) | Não | UQ | Registro profissional quando aplicável |
| tipo_especializacao | VARCHAR(24) | Sim | CK | Discriminador da especialização |
| situacao | VARCHAR(10) | Sim | CK | ATIVO ou INATIVO |

### arqueologo

Regras associadas: RN03/RN08.

| Atributo | Domínio | Obrig. | Chave | Descrição |
|---|---|---|---|---|
| id_pesquisador | INT | Sim | PK/FK | Identifica o pesquisador especializado |
| area_pesquisa | VARCHAR(100) | Sim |  | Área de pesquisa arqueológica |
| numero_autorizacao | VARCHAR(40) | Sim | UQ | Autorização profissional |

### conservador

Regras associadas: RN03/RN20.

| Atributo | Domínio | Obrig. | Chave | Descrição |
|---|---|---|---|---|
| id_pesquisador | INT | Sim | PK/FK | Identifica o pesquisador especializado |
| especialidade_material | VARCHAR(80) | Sim |  | Material de especialização |
| numero_registro | VARCHAR(40) | Sim | UQ | Registro profissional |

### analista_laboratorial

Regras associadas: RN03.

| Atributo | Domínio | Obrig. | Chave | Descrição |
|---|---|---|---|---|
| id_pesquisador | INT | Sim | PK/FK | Identifica o pesquisador especializado |
| especialidade_analise | VARCHAR(100) | Sim |  | Especialidade técnica |
| nivel_formacao | VARCHAR(30) | Sim |  | Titulação |

### sitio_arqueologico

Regras associadas: RN05.

| Atributo | Domínio | Obrig. | Chave | Descrição |
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

Regras associadas: RN06–RN09.

| Atributo | Domínio | Obrig. | Chave | Descrição |
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

Regras associadas: RN10–RN11.

| Atributo | Domínio | Obrig. | Chave | Descrição |
|---|---|---|---|---|
| id_campanha | INT | Sim | PK/FK | Campanha |
| id_pesquisador | INT | Sim | PK/FK | Pesquisador |
| funcao | VARCHAR(60) | Sim |  | Função na campanha |
| data_entrada | DATE | Sim |  | Início da participação |
| data_saida | DATE | Não | CK | Fim da participação |
| horas_previstas | SMALLINT | Sim | CK | Carga horária positiva |

### unidade_escavacao

Regras associadas: RN12.

| Atributo | Domínio | Obrig. | Chave | Descrição |
|---|---|---|---|---|
| id_unidade | INT | Sim | PK | Identificador interno |
| id_campanha | INT | Sim | FK | Campanha responsável |
| codigo | VARCHAR(20) | Sim | UQ composto | Código dentro da campanha |
| setor | VARCHAR(40) | Sim |  | Setor espacial |
| nivel_estratigrafico | VARCHAR(40) | Não |  | Nível estratigráfico |
| data_abertura | DATE | Sim |  | Data de abertura |
| data_encerramento | DATE | Não | CK | Data de encerramento |

### artefato

Regras associadas: RN13–RN15.

| Atributo | Domínio | Obrig. | Chave | Descrição |
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

Regras associadas: RN14/RN16.

| Atributo | Domínio | Obrig. | Chave | Descrição |
|---|---|---|---|---|
| id_artefato | INT | Sim | PK/FK | Artefato proprietário |
| numero_fragmento | SMALLINT | Sim | PK | Número dentro do artefato |
| peso_g | DECIMAL(9,3) | Sim | CK | Peso não negativo |
| comprimento_mm | DECIMAL(8,2) | Não | CK | Comprimento não negativo |
| descricao | VARCHAR(240) | Não |  | Características do fragmento |
| localizado | BOOLEAN | Sim |  | Indica presença física |

### analise_laboratorial

Regras associadas: RN17.

| Atributo | Domínio | Obrig. | Chave | Descrição |
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

Regras associadas: RN18–RN19.

| Atributo | Domínio | Obrig. | Chave | Descrição |
|---|---|---|---|---|
| id_artefato | INT | Sim | PK/FK | Artefato analisado |
| id_analise | INT | Sim | PK/FK | Análise |
| codigo_amostra | VARCHAR(32) | Sim | UQ | Código da amostra |
| massa_consumida_g | DECIMAL(8,3) | Sim | CK | Massa consumida |
| resultado_resumido | VARCHAR(300) | Não |  | Resultado sintético |
| conclusao | TEXT | Não |  | Conclusão técnica |

### intervencao_conservacao

Regras associadas: RN20–RN21.

| Atributo | Domínio | Obrig. | Chave | Descrição |
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

Regras associadas: RN22.

| Atributo | Domínio | Obrig. | Chave | Descrição |
|---|---|---|---|---|
| id_local | INT | Sim | PK | Identificador interno |
| id_instituicao | INT | Sim | FK | Instituição proprietária |
| codigo | VARCHAR(24) | Sim | UQ composto | Código interno |
| tipo | VARCHAR(30) | Sim |  | Reserva técnica, laboratório ou exposição |
| temperatura_c | DECIMAL(5,2) | Não |  | Temperatura de referência |
| umidade_percentual | DECIMAL(5,2) | Não | CK | Umidade entre 0 e 100 |
| capacidade | INT | Sim | CK | Capacidade positiva |

### custodia_artefato

O campo gerado abaixo pertence ao projeto físico; não é um atributo independente do modelo conceitual.

Regras associadas: RN23–RN25.

| Atributo | Domínio | Obrig. | Chave | Descrição |
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
## A4 — Modelo lógico relacional e decisões de mapeamento

O diagrama relacional integra este relatório com uma visão geral, uma folha de esquema completo e quatro folhas de detalhe. PKs são sublinhadas, FKs são identificadas, e os relacionamentos N:N aparecem mapeados nas tabelas associativas. A identificação de FRAGMENTO é composta e dependente. Todas as FKs possuem ações ON DELETE e ON UPDATE explícitas no DDL.

[INSERIR_LOGICO]

### Decisões de mapeamento por relação

| Relação | Chave e estratégia | Justificativa |
|---|---|---|
| instituicao | id_instituicao substituta; nome é candidata | O nome pode sofrer ajuste institucional; a chave inteira mantém relacionamentos estáveis e UNIQUE preserva a identificação de negócio. |
| pesquisador | id_pesquisador substituta; email é candidata | Email e registro podem mudar ou ser ausentes; o identificador interno evita propagação de alterações. |
| arqueologo | PK herdada id_pesquisador | Tabela de subtipo evita nulos no supertipo e representa a especialização total e exclusiva; tipo_especializacao e consulta 15 verificam a consistência. O esquema atual não bloqueia toda violação de totalidade/exclusividade, que exige escrita coordenada da aplicação futura. |
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


## A5 — Dependências funcionais e normalização

### Critérios utilizados

Uma dependência funcional X -> Y significa que duas tuplas com os mesmos valores de X têm os mesmos valores de Y. A notação {a, b} indica um determinante composto. As dependências abaixo explicitam os atributos; dependências triviais e as obtidas por transitividade estão omitidas.

Uma relação está em 3FN quando, para toda dependência não trivial X -> A, X é superchave ou A é atributo primo (pertence a alguma chave candidata). BCNF exige que X seja superchave em toda dependência não trivial: não há a exceção do atributo primo. Portanto BCNF implica 3FN, mas o inverso não é garantido.

1FN: os atributos armazenam valores atômicos e não há grupos repetidos. 2FN: nas relações com chave composta, os atributos não primos dependem da chave inteira. As relações com chave simples já não possuem dependência parcial dessa chave, mas as demais chaves candidatas também precisam ser consideradas. 3FN/BCNF: examinam-se todos os determinantes semânticos conhecidos, e não apenas a chave primária escolhida.

UNIQUE anulável não é automaticamente uma chave candidata: registro_profissional e id_artefato_em_aberto aceitam NULL no MySQL. As análises abaixo consideram as dependências do domínio declarado; não inferem dependências de coincidências da carga sintética.

### instituicao

Chaves candidatas: {id_instituicao}; {nome}.

- {id_instituicao} -> {nome, sigla, tipo, pais, uf, cidade}.
- {nome} -> {id_instituicao, sigla, tipo, pais, uf, cidade}.

Todos os determinantes não triviais conhecidos são chaves candidatas; não há dependência parcial ou transitiva de atributo não primo por determinante que não seja superchave. Assim, a relação atende à BCNF e, consequentemente, à 3FN.

### pesquisador

Chaves candidatas: {id_pesquisador}; {email}.

- {id_pesquisador} -> {id_instituicao, nome, email, registro_profissional, tipo_especializacao, situacao}.
- {email} -> {id_pesquisador, id_instituicao, nome, registro_profissional, tipo_especializacao, situacao}.

Todos os determinantes não triviais conhecidos são chaves candidatas; não há dependência parcial ou transitiva de atributo não primo por determinante que não seja superchave. Assim, a relação atende à BCNF e, consequentemente, à 3FN.

### arqueologo

Chaves candidatas: {id_pesquisador}; {numero_autorizacao}.

- {id_pesquisador} -> {area_pesquisa, numero_autorizacao}.
- {numero_autorizacao} -> {id_pesquisador, area_pesquisa}.

Todos os determinantes não triviais conhecidos são chaves candidatas; não há dependência parcial ou transitiva de atributo não primo por determinante que não seja superchave. Assim, a relação atende à BCNF e, consequentemente, à 3FN.

### conservador

Chaves candidatas: {id_pesquisador}; {numero_registro}.

- {id_pesquisador} -> {especialidade_material, numero_registro}.
- {numero_registro} -> {id_pesquisador, especialidade_material}.

Todos os determinantes não triviais conhecidos são chaves candidatas; não há dependência parcial ou transitiva de atributo não primo por determinante que não seja superchave. Assim, a relação atende à BCNF e, consequentemente, à 3FN.

### analista_laboratorial

Chaves candidatas: {id_pesquisador}.

- {id_pesquisador} -> {especialidade_analise, nivel_formacao}.

Todos os determinantes não triviais conhecidos são chaves candidatas; não há dependência parcial ou transitiva de atributo não primo por determinante que não seja superchave. Assim, a relação atende à BCNF e, consequentemente, à 3FN.

### sitio_arqueologico

Chaves candidatas: {id_sitio}; {nome}.

- {id_sitio} -> {nome, municipio, uf, latitude, longitude, periodo_historico, nivel_protecao, data_cadastro}.
- {nome} -> {id_sitio, municipio, uf, latitude, longitude, periodo_historico, nivel_protecao, data_cadastro}.

Todos os determinantes não triviais conhecidos são chaves candidatas; não há dependência parcial ou transitiva de atributo não primo por determinante que não seja superchave. Assim, a relação atende à BCNF e, consequentemente, à 3FN.

### campanha

Chaves candidatas: {id_campanha}; {codigo}.

- {id_campanha} -> {id_sitio, id_coordenador, codigo, nome, objetivo, data_inicio, data_fim, situacao, orcamento}.
- {codigo} -> {id_campanha, id_sitio, id_coordenador, nome, objetivo, data_inicio, data_fim, situacao, orcamento}.

Todos os determinantes não triviais conhecidos são chaves candidatas; não há dependência parcial ou transitiva de atributo não primo por determinante que não seja superchave. Assim, a relação atende à BCNF e, consequentemente, à 3FN.

### participacao_campanha

Chaves candidatas: {id_campanha, id_pesquisador}.

- {id_campanha, id_pesquisador} -> {funcao, data_entrada, data_saida, horas_previstas}.

Todos os determinantes não triviais conhecidos são chaves candidatas; não há dependência parcial ou transitiva de atributo não primo por determinante que não seja superchave. Assim, a relação atende à BCNF e, consequentemente, à 3FN.

### unidade_escavacao

Chaves candidatas: {id_unidade}; {id_campanha, codigo}.

- {id_unidade} -> {id_campanha, codigo, setor, nivel_estratigrafico, data_abertura, data_encerramento}.
- {id_campanha, codigo} -> {id_unidade, setor, nivel_estratigrafico, data_abertura, data_encerramento}.

Todos os determinantes não triviais conhecidos são chaves candidatas; não há dependência parcial ou transitiva de atributo não primo por determinante que não seja superchave. Assim, a relação atende à BCNF e, consequentemente, à 3FN.

### artefato

Chaves candidatas: {id_artefato}; {codigo_catalogo}.

- {id_artefato} -> {id_unidade, id_artefato_referencia, codigo_catalogo, nome, descricao, material, periodo_estimado, data_descoberta, profundidade_cm, estado_conservacao, situacao}.
- {codigo_catalogo} -> {id_artefato, id_unidade, id_artefato_referencia, nome, descricao, material, periodo_estimado, data_descoberta, profundidade_cm, estado_conservacao, situacao}.

Todos os determinantes não triviais conhecidos são chaves candidatas; não há dependência parcial ou transitiva de atributo não primo por determinante que não seja superchave. Assim, a relação atende à BCNF e, consequentemente, à 3FN.

### fragmento

Chaves candidatas: {id_artefato, numero_fragmento}.

- {id_artefato, numero_fragmento} -> {peso_g, comprimento_mm, descricao, localizado}.

Todos os determinantes não triviais conhecidos são chaves candidatas; não há dependência parcial ou transitiva de atributo não primo por determinante que não seja superchave. Assim, a relação atende à BCNF e, consequentemente, à 3FN.

### analise_laboratorial

Chaves candidatas: {id_analise}.

- {id_analise} -> {id_laboratorio, tipo_analise, metodo, data_inicio, data_fim, situacao, custo}.

Todos os determinantes não triviais conhecidos são chaves candidatas; não há dependência parcial ou transitiva de atributo não primo por determinante que não seja superchave. Assim, a relação atende à BCNF e, consequentemente, à 3FN.

### artefato_analise

Chaves candidatas: {id_artefato, id_analise}; {codigo_amostra}.

- {id_artefato, id_analise} -> {codigo_amostra, massa_consumida_g, resultado_resumido, conclusao}.
- {codigo_amostra} -> {id_artefato, id_analise, massa_consumida_g, resultado_resumido, conclusao}.

Todos os determinantes não triviais conhecidos são chaves candidatas; não há dependência parcial ou transitiva de atributo não primo por determinante que não seja superchave. Assim, a relação atende à BCNF e, consequentemente, à 3FN.

### intervencao_conservacao

Chaves candidatas: {id_intervencao}.

- {id_intervencao} -> {id_artefato, id_conservador, tipo_intervencao, data_inicio, data_fim, descricao, material_aplicado, situacao, custo}.

Todos os determinantes não triviais conhecidos são chaves candidatas; não há dependência parcial ou transitiva de atributo não primo por determinante que não seja superchave. Assim, a relação atende à BCNF e, consequentemente, à 3FN.

### local_armazenamento

Chaves candidatas: {id_local}; {id_instituicao, codigo}.

- {id_local} -> {id_instituicao, codigo, tipo, temperatura_c, umidade_percentual, capacidade}.
- {id_instituicao, codigo} -> {id_local, tipo, temperatura_c, umidade_percentual, capacidade}.

Todos os determinantes não triviais conhecidos são chaves candidatas; não há dependência parcial ou transitiva de atributo não primo por determinante que não seja superchave. Assim, a relação atende à BCNF e, consequentemente, à 3FN.

### custodia_artefato

Chaves candidatas: {id_custodia}; {documento_movimentacao}; {id_artefato, data_inicio}.

- {id_custodia} -> {id_artefato, id_instituicao, id_local, id_responsavel, data_inicio, data_fim, documento_movimentacao, motivo}.
- {documento_movimentacao} -> {id_custodia, id_artefato, id_instituicao, id_local, id_responsavel, data_inicio, data_fim, motivo}.
- {id_artefato, data_inicio} -> {id_custodia, id_instituicao, id_local, id_responsavel, data_fim, documento_movimentacao, motivo}.

Dependência semântica adicional da RN25, para registros cujo local está informado: {id_local} -> {id_instituicao}. O local não é superchave da custódia e id_instituicao não é atributo primo. Nesse subconjunto existe dependência transitiva a partir das chaves do evento. Por isso não se afirma que a representação física de custódia esteja integralmente em 3FN ou BCNF.

Foi mantida uma redundância controlada: a instituição responsável é guardada também quando há um local. Isso permite consultar a custódia vigente por instituição sem juntar a tabela de locais e preserva a instituição quando id_local é removido por ON DELETE SET NULL. O ganho de desempenho é uma hipótese de projeto, ainda sem medição; deve ser confrontado com planos e tempos na Etapa 2. A RN25 é protegida por triggers nas duas tabelas. Uma alternativa estritamente normalizada seria separar a custódia com local da custódia sem local e obter a instituição por junção no primeiro caso; essa alternativa aumenta a complexidade das consultas e da migração.

A coluna física id_artefato_em_aberto satisfaz {id_artefato, data_fim} -> {id_artefato_em_aberto}. Trata-se de materialização determinística para permitir a unicidade de custódia vigente no MySQL. Ela é excluída da relação lógica de eventos usada na análise das chaves, mas a exceção física é documentada; não se usa sua existência para alegar BCNF.

Conclusão: o núcleo de eventos, sem a dependência de local e sem a coluna gerada, tem determinantes candidatos; a representação física atual contém as exceções explícitas acima. Não se repete a afirmação anterior de que todo o esquema físico estaria em 3FN sem ressalvas.


## Implementação física, carga e consultas

O DDL cria arqueologia_ucb com InnoDB e utf8mb4. Há 16 tabelas, 22 chaves estrangeiras, 28 CHECKs e 11 triggers. Os nove triggers acrescentados reforçam RN08, RN17 e RN25 tanto no INSERT/UPDATE dos registros dependentes quanto nas alterações dos registros referenciados. Leituras com FOR SHARE preservam o pai validado até o fim da transação. Os dois triggers originais impedem autorreferência direta de artefato. Esses mecanismos são de integridade; ainda não implementam os gatilhos de auditoria B6.

Foram acrescentados idx_campanha_aberta_inicio, idx_analise_aberta_inicio e idx_custodia_vigencia_instituicao. idx_artefato_material foi substituído por idx_artefato_material_estado, aproveitando o prefixo material. Os índices de campanha/análise priorizam data_fim IS NULL antes do intervalo/ordenação por data_inicio (consultas 1 e 4). O índice de custódia atende à vigência por instituição (consulta 7) e o de artefato ao material/estado (consulta 2). Não se alega ganho medido: a análise de três consultas com EXPLAIN antes/depois e tempos permanece para B4.

A carga contém 40 instituições, 40 pesquisadores (14 arqueólogos, 13 conservadores e 13 analistas), 40 sítios, 40 campanhas, 120 participações, 80 unidades, 100 artefatos, 150 fragmentos, 45 análises, 100 associações de análise, 60 intervenções, 40 locais e 119 custódias. Os materiais foram alinhados aos objetos; há cem descrições distintas de artefatos, objetivos de campanha variados e cronologia de descoberta, análises, intervenções e custódias coerente.

### Casos de contorno verificáveis

- Artefato 100: catalogado em estado crítico, descoberto em superfície, ainda sem custódia.
- Artefato 99: instituição de custódia conhecida, mas local ainda não informado.
- Sítio 40: possui campanha/unidades, mas ainda nenhum artefato encontrado.
- Oito artefatos com datação pendente e onze fragmentos sem dimensão conhecida.
- Fragmento 2 do artefato 1: resíduo abaixo do limite de quantificação da balança.
- Históricos com mais de uma custódia e transferência na mesma data do encerramento anterior.
- Campanhas/análises/intervenções abertas, análises sem resultado e fragmentos não localizados.

A consulta 5 agora faz triagem de artefatos frágeis/críticos aguardando conservação, priorizando risco e antiguidade. A consulta 11 usa subconsulta escalar correlacionada explícita para comparar o custo de uma análise com a média do seu próprio laboratório; a condição al2.id_laboratorio = al.id_laboratorio liga os escopos interno e externo. A consulta 12 mantém EXISTS correlacionado. As quinze consultas preservam a distribuição exigida de cinco básicas, cinco de junção/agregação e cinco avançadas.

## Validação reproduzível

Executamos os três scripts no MySQL 8.4.11, em base limpa e em nova reconstrução. As quinze consultas executaram sem erro e a consulta 15 não retornou violações semânticas. Foram exercitados nove cenários negativos das três regras reforçadas e operações válidas com ROLLBACK. O teste e sua saída encontram-se no repositório. A ausência de violações na carga não significa que todas as regras futuras da aplicação já estejam implementadas.

## Estrutura do repositório

- README.md: visão geral, integrantes, requisitos e instruções de execução.
- sql/01_ddl.sql: esquema físico autoral, restrições, índices e triggers de integridade.
- sql/02_carga.sql: carga sintética, casos de contorno e contagens.
- sql/03_consultas.sql: quinze perguntas de negócio, organizadas em cinco básicas, cinco de junção/agregação e cinco avançadas.
- docs/relatorio-etapa1.pdf: relatório consolidado revisado com A1–A5 e o registro da devolutiva.
- docs/mer-conceitual.pdf: MER Chen com visão completa e folhas de detalhe; permite ampliação sem perda de nitidez.
- docs/modelo-logico.pdf: esquema relacional completo e folhas de detalhe.
- docs/dicionario-dados.pdf: dicionário integral, também incorporado ao relatório.
- docs/fontes/: fontes Markdown e PlantUML utilizadas para regenerar os PDFs.
- docs/revisao-devolutiva-etapa1.md: rastreabilidade C1–C5 para incorporar ao relatório final.
- docs/evidencias/validacao-etapa1.txt: saída da validação executada no MySQL.
- scripts/: geração reproduzível dos diagramas e documentos.
- tests/validar_etapa1.py: reconstrução e testes positivos e negativos em container descartável.

## Registro de revisão da devolutiva da Etapa 1

Data da revisão: 18/10/2026. Escopo: correção de C1 a C5; a aplicação e os demais artefatos operacionais da Etapa 2 ainda não foram implementados.

| Critério | Devolutiva | Correção realizada | Evidência |
|---|---|---|---|
| C1 | Diagramas pequenos; cardinalidades ilegíveis/ausentes; especialização isolada | MER Chen vetorial, visão completa e detalhes com atributos; cardinalidades verificáveis; especialização total e disjunta por símbolo nativo; fontes PlantUML sincronizadas | docs/mer-conceitual.pdf; fontes PlantUML; seção A2 |
| C2 | Formalizar dependências; discutir 3FN versus BCNF | Dependências X -> Y por relação e por chave candidata; definições formais; análise explícita da redundância de custódia e da coluna gerada | seção A5; docs/fontes/normalizacao.md |
| C3 | Acrescentar idx_ e triggers para coordenador, laboratório e local | Quatro índices compostos (um substitui índice simples); nove triggers adicionais, incluindo proteção de alterações no lado referenciado; comentários RN corrigidos | sql/01_ddl.sql; testes de INSERT/UPDATE e alterações de pais |
| C4 | Variar descrições e contornos; melhorar consulta 5; correlação explícita | Cem descrições distintas de artefatos, materiais coerentes e datas encadeadas; contornos verificáveis; consulta 5 de triagem por risco; consulta 11 escalar correlacionada por laboratório | sql/02_carga.sql; sql/03_consultas.sql; evidências de validação |
| C5 | Rotular A1–A5, dicionário legível e estrutura do repositório | Relatório com seções A1–A5, dicionário integral com cabeçalhos repetidos e fonte legível, estrutura comentada e modelos em páginas vetoriais próprias | docs/relatorio-etapa1.pdf; README.md |

### Evidência da revisão

Os três scripts foram executados em sequência no MySQL 8.4.11, em base limpa e novamente para verificar reconstrução. As quinze consultas executaram sem erro; a consulta 15 retornou zero violações. Nove testes negativos cobrem INSERT/UPDATE dos filhos e alteração dos pais nas RN08/RN17/RN25; também foram exercitados casos válidos com ROLLBACK. Há 16 tabelas, 22 FKs, 28 CHECKs e 11 triggers. A carga mantém os mínimos exigidos e possui 119 registros de custódia: o artefato 100 ainda aguarda a primeira custódia.

O teste reproduzível está em tests/validar_etapa1.py e a saída em docs/evidencias/validacao-etapa1.txt. A criação dos PDFs está em scripts/gerar_documentos.py e scripts/gerar_modelos.py.

### Incorporação futura ao relatório final

Este registro e as seções revisadas A1–A5 devem compor o relatório final da Etapa 2. A inclusão desse registro documenta a correção da devolutiva; não afirma que B1–B8 estejam concluídos. Permanecem para a aplicação futura as validações completas de especialização e participação de pesquisadores ativos. A consulta 15 continua sendo uma verificação complementar, não um substituto dessas validações.

## Declaração de uso de inteligência artificial

A equipe utilizou IA como apoio à estruturação inicial, geração de dados sintéticos, documentação e revisão do SQL. Na revisão da devolutiva, a ferramenta auxiliou na redação das dependências funcionais, nos triggers, na variação da carga, na descrição dos modelos em PlantUML e na geração dos PDFs/PNGs com PlantUML, Graphviz e CairoSVG. A verificação técnica foi feita por execução em MySQL 8.4.11, testes positivos e negativos de restrições e inspeção dos PDFs. Os integrantes devem revisar o material, conferir a interpretação das regras e conseguir explicar as decisões na apresentação.
