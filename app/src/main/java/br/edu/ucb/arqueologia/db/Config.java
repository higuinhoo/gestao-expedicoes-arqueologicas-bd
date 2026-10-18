package br.edu.ucb.arqueologia.db;
import java.io.*;import java.nio.charset.StandardCharsets;import java.nio.file.*;import java.util.Properties;
public record Config(String url) {
    public static Config load() throws IOException {
        String path=System.getProperty("arq.config",System.getenv().getOrDefault("ARQ_CONFIG","config.properties"));
        Properties p=new Properties();
        try(Reader r=Files.newBufferedReader(Path.of(path),StandardCharsets.UTF_8)){p.load(r);}
        String url=p.getProperty("db.url");
        if(url==null || !url.startsWith("jdbc:mysql:"))throw new IOException("Informe db.url MySQL no arquivo de configuração.");
        return new Config(url);
    }
}
