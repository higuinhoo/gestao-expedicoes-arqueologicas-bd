# Ambiente de apresentação

Este modo usa contas públicas de demonstração: `admin/admin`, `catalogador/catalogador` e `consulta/consulta`. A senha do usuário é igual ao login, conforme a convenção do grupo. As senhas do aplicativo continuam armazenadas com PBKDF2 e sal.

O Compose publica o banco somente em `127.0.0.1:33319` e usa volume persistente. É um ambiente de dados fictícios para testes/apresentação. Para outro uso, configure contas próprias e a conexão externa.

`07_usuarios.sql` cria/atualiza somente as três contas de demonstração e seus hashes. Ele não recria o esquema nem apaga os dados do domínio. Os scripts destrutivos `sql/01_ddl.sql` e de carga rodam automaticamente apenas quando o volume do Compose está vazio.

No primeiro uso, `iniciar.bat` ou `sh iniciar.sh` requer internet para obter MySQL e, se necessário, Java 17. Depois, o runtime e a imagem ficam em cache na máquina. Transferir somente o JAR para uma máquina sem Docker/banco/Java não substitui essa preparação.

Porta alternativa: defina `ARQ_DB_PORT` antes de iniciar. Use sempre a mesma porta no Compose e na configuração gerada. A senha de manutenção padrão do ambiente demo é `arqueologia-root-demo`; ela não é usada pela interface. `ARQ_MYSQL_ROOT_PASSWORD` permite escolher outra na primeira criação do volume; alterar a variável depois não altera uma senha já persistida.

Parar o banco: `parar.bat` ou `sh parar.sh`. Os registros permanecem no volume. Os inicializadores não removem volumes nem executam reset automático.
