package idealyfw.util;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public class MySession {

    private Map<String, Object> data;
    private boolean invalide = false;

    public MySession() {
        this.data = new ConcurrentHashMap<>();
    }

    public MySession(HttpServletRequest req) {
        this();
        HttpSession http = req.getSession();
        for (String name : Collections.list(http.getAttributeNames())) {
            data.put(name, http.getAttribute(name));
        }
    }

    // --- méthodes pour le développeur ---
    public void set(String cle, Object valeur) {
        if (valeur == null) data.remove(cle);   // ConcurrentHashMap refuse null
        else data.put(cle, valeur);
    }
    public Object get(String cle)   { return cle == null ? null : data.get(cle); }
    public boolean has(String cle)  { return cle != null && data.containsKey(cle); }
    public void remove(String cle)  { if (cle != null) data.remove(cle); }

    public void invalidate() {
        data.clear();
        invalide = true;
    }

    // --- pour le framework ---
    public Map<String, Object> getData() { return data; }

    public void syncHttpSession(HttpServletRequest req) {
        HttpSession http = req.getSession(false);
        if (http == null) return;

        if (invalide) {
            http.invalidate();
            return;
        }

        for (Map.Entry<String, Object> e : data.entrySet()) {
            http.setAttribute(e.getKey(), e.getValue());
        }
        for (String name : Collections.list(http.getAttributeNames())) {
            if (!data.containsKey(name)) http.removeAttribute(name);
        }
    }
}