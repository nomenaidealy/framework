package idealyfw.util ;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import idealyfw.annotation.UrlMapping;

public class CustomUrlRegistry {

     private final Map<UrlMethod, Mapping> mappings = new HashMap<>();


     public void register(String url,String methodHttp , Mapping mapping){
        String cleanUrl = cleanPath(url);
        UrlMethod key = new UrlMethod(cleanUrl, methodHttp) ;
        mappings.put(key,  mapping);
     }

     public String cleanPath(String path){
        if(path ==  null) return "/" ;
        if(path.endsWith("/") && path.length()> 1){
            return path.substring(0,path.length()-1);
        }
        return path ;
     }

     
     public Mapping getMapping(String url, String method) {

      UrlMethod key = new UrlMethod(cleanPath(url), method);

      return mappings.get(key);
   }

   public Map<UrlMethod, Mapping> getMappings() {
    return mappings;
}

      
}
