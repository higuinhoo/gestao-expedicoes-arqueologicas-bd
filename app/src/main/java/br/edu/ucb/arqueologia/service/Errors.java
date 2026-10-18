package br.edu.ucb.arqueologia.service;
import java.sql.SQLException;
public final class Errors {
    private Errors(){}
    public static String friendly(Throwable error){
        Throwable e=error;while(e.getCause()!=null && !(e instanceof SQLException) && !(e instanceof BusinessException))e=e.getCause();
        if(e instanceof BusinessException || e instanceof IllegalArgumentException)return e.getMessage();
        if(e instanceof SQLException sql){
            return switch(sql.getErrorCode()){
                case 1045->"Usuário ou senha inválidos, ou acesso ao banco não autorizado.";
                case 1044,1142,1143,1227,1370->"Seu perfil não tem permissão para esta operação.";
                case 1062->"Já existe um registro com esse código, e-mail ou identificador único.";
                case 1451->"Este registro está sendo usado por outros dados e não pode ser removido.";
                case 1452->"A referência informada não existe. Confira os registros relacionados.";
                case 3819,1048,1406,1264,1292,1366->"Valor inválido: confira os campos obrigatórios, limites, datas e números.";
                case 1205,1213->"Outro usuário está alterando estes dados. A operação foi desfeita; atualize e tente novamente.";
                case 1644->semantic(sql.getMessage());
                default->"Não foi possível concluir a operação no banco. Verifique a conexão e os dados informados.";
            };
        }
        if(e instanceof java.io.IOException)return "Não foi possível abrir a configuração. Defina ARQ_CONFIG ou use config.properties.";
        return "Não foi possível concluir a operação. Confira os dados e tente novamente.";
    }
    private static String semantic(String message){
        if(message.contains("RN03"))return "A especialização deve corresponder a exatamente um subtipo do pesquisador.";
        if(message.contains("RN04"))return "Somente pesquisadores ativos podem entrar em uma campanha.";
        if(message.contains("RN08"))return "O coordenador da campanha deve ser arqueólogo.";
        if(message.contains("RN17"))return "A instituição responsável pela análise deve ser um laboratório.";
        if(message.contains("RN20"))return "A intervenção deve ter um conservador como responsável.";
        if(message.contains("RN25"))return "O local de armazenamento deve pertencer à instituição da custódia.";
        if(message.contains("referenciar a si"))return "Um artefato não pode ser referência de si próprio.";
        return "A alteração viola uma regra de integridade do projeto.";
    }
}
