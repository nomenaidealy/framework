package idealyfw.util;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Petit utilitaire responsable de la conversion des objets Java
 * renvoyés par les méthodes @ApiRest en texte JSON.
 *
 * Un seul ObjectMapper est réutilisé (thread-safe en lecture/écriture
 * une fois configuré, donc pas besoin d'en recréer un par requête).
 */
public class JsonUtil {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Sérialise n'importe quel objet (POJO, List, Map, etc.) en JSON.
     * Utilisé quand la méthode du contrôleur renvoie autre chose qu'une String :
     * c'est le framework qui fait le travail de conversion.
     */
    public static String objectToJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la conversion en JSON", e);
        }
    }
}
