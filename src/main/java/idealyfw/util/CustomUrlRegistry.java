package idealyfw.util ;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class CustomUrlRegistry {

     private final Map<String,Mapping> mappings = new HashMap<>() ;


     public void register(String url, Mapping mapping){
        String cleanUrl = cleanPath(url);
        mappings.put(cleanUrl,  mapping);
     }

     public String cleanPath(String path){
        if(path ==  null) return "/" ;
        if(path.endsWith("/") && path.length()> 1){
            return path.substring(0,path.length()-1);
        }
        return path ;
     }

     

     public Mapping getMapping(String url){
      return mappings.get(cleanPath(url)) ;
     }

     public Map<String, Mapping> getMappings() {
         return mappings;
      }

      
}
