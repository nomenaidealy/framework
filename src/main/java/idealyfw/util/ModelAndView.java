package idealyfw.util;

import java.util.HashMap;
import java.util.Map;

public class ModelAndView {
    private String view;
    private Map<String, Object> model;

    // constructeur avec view seulement ← plus pratique
    public ModelAndView(String view) {
        this.view = view;
        this.model = new HashMap<>(); // ← initialise automatiquement
    }

    // constructeur avec view et model
    public ModelAndView(String view, Map<String, Object> model) {
        this.view = view;
        this.model = model;
    }

    // ajouter une donnée facilement ← très utile
    public void addData(String key, Object value) {
        this.model.put(key, value);
    }

    public String getView() { return view; }
    public Map<String, Object> getModel() { return model; }
}

