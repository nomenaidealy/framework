package idealyfw.util;

import java.util.HashMap;
import java.util.Map;

public class ModelAndView {
    private String view;
    private Map<String, Object> model;

   
    public ModelAndView(String view) {
        this.view = view;
        this.model = new HashMap<>(); 
    }

  
    public ModelAndView(String view, Map<String, Object> model) {
        this.view = view;
        this.model = model;
    }

  
    public void addData(String key, Object value) {
        this.model.put(key, value);
    }

    public String getView() { return view; }
    public Map<String, Object> getModel() { return model; }
}

