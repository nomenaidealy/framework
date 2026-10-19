package idealyfw.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class ConfigUtil {

    private static final Properties props = new Properties();

    static {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try (InputStream in = cl.getResourceAsStream("application.properties")) {
            if (in != null) {
                props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            } else {
                System.out.println("[Config] application.properties introuvable, valeurs par défaut utilisées");
            }
        } catch (IOException e) {
            throw new RuntimeException("Lecture de application.properties impossible", e);
        }
    }

    public static String get(String cle, String defaut) {
        return props.getProperty(cle, defaut);
    }
}