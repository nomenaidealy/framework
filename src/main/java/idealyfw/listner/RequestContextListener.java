package idealyfw.listner;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import idealyfw.util.Mapping;
import idealyfw.util.ParamScanUtil;
import idealyfw.util.UrlMethod;
import jakarta.servlet.* ;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class RequestContextListener implements ServletContextListener {
    Map<UrlMethod, Mapping> mappings;

  
    @Override
    public void contextInitialized(ServletContextEvent sce) {
         String packageName = sce.getServletContext().getInitParameter("controllerPackage") ;
        try {

            ParamScanUtil scanner = new ParamScanUtil();

            List<Class<?>> controllers = scanner.scan(packageName);
            Map<UrlMethod,Mapping> mappings =  scanner.registry.getMappings();
            sce.getServletContext().setAttribute("globalMappings", mappings);

        } catch (Exception e) {
            throw new RuntimeException("Échec du scan au démarrage", e);
        }
    }
}
