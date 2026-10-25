package br.edu.ucb.arqueologia.service;
import java.security.*;import java.util.*;import javax.crypto.SecretKeyFactory;import javax.crypto.spec.PBEKeySpec;
/** Mesmo formato usado no provisionamento Python: algoritmo$iterações$sal$hash. */
public final class Passwords {
    public static final int ITERATIONS=600000;
    private Passwords(){}
    public static String hash(char[] password){
        byte[] salt=new byte[16];new SecureRandom().nextBytes(salt);
        return "pbkdf2_sha256$"+ITERATIONS+"$"+Base64.getEncoder().encodeToString(salt)+"$"+Base64.getEncoder().encodeToString(derive(password,salt,ITERATIONS));
    }
    public static boolean verify(char[] password,String encoded){
        try{
            String[] parts=encoded.split("\\$");if(parts.length!=4 || !parts[0].equals("pbkdf2_sha256"))return false;
            int iterations=Integer.parseInt(parts[1]);if(iterations<100000 || iterations>2000000)return false;
            byte[] expected=Base64.getDecoder().decode(parts[3]);
            return MessageDigest.isEqual(expected,derive(password,Base64.getDecoder().decode(parts[2]),iterations));
        }catch(IllegalArgumentException ex){return false;}
    }
    private static byte[] derive(char[] password,byte[] salt,int iterations){
        PBEKeySpec spec=new PBEKeySpec(password,salt,iterations,256);
        try{return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();}
        catch(GeneralSecurityException ex){throw new IllegalStateException("PBKDF2 indisponível no Java.",ex);}
        finally{spec.clearPassword();}
    }
}
