package br.edu.ucb.arqueologia;
import br.edu.ucb.arqueologia.service.Passwords;import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;
class PasswordsTest {
    @Test void saltAndMalformedHashes(){
        char[] password="uma frase de teste segura".toCharArray();String one=Passwords.hash(password),two=Passwords.hash(password);
        assertNotEquals(one,two);assertFalse(one.contains(new String(password)));assertTrue(Passwords.verify(password,one));assertFalse(Passwords.verify("errada".toCharArray(),one));
        assertFalse(Passwords.verify(password,"texto_claro"));assertFalse(Passwords.verify(password,"pbkdf2_sha256$1$AA==$AA=="));
    }
}
