package idealyfw.listner;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import idealyfw.util.Mapping;
import idealyfw.util.ParamScanUtil;
import idealyfw.util.UrlMethod;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebListener;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@WebListener
public class RequestContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        String packageName = sce.getServletContext()
            .getInitParameter("controllerPackage");
        String springConfig = sce.getServletContext()
            .getInitParameter("springConfigClass");

        try {


       
     
            AnnotationConfigApplicationContext springContext =
                new AnnotationConfigApplicationContext(
                    Class.forName(springConfig) 
                );

            
            sce.getServletContext().setAttribute("springContext", springContext);

          
            ParamScanUtil scanner = new ParamScanUtil();
            List<Class<?>> classes = scanner.scan(packageName);
            Map<UrlMethod, Mapping> mappings = scanner.registry.getMappings();
            Map<Class<?>, Object> controllerInstances = new ConcurrentHashMap<>();
            for (Class<?> c : classes) {
                controllerInstances.put(c, c.getDeclaredConstructor().newInstance());
            }

            sce.getServletContext().setAttribute("globalMappings", mappings);

            sce.getServletContext().setAttribute("ctrlInstances", controllerInstances);


            System.out.println("[Listener] Spring container démarré ");
            System.out.println("[Listener] " + mappings.size() + " mapping(s) enregistrés ");

        } catch (Exception e) {
            throw new RuntimeException("Échec démarrage", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Fermer le container Spring proprement
        AnnotationConfigApplicationContext ctx =
            (AnnotationConfigApplicationContext) sce.getServletContext()
                .getAttribute("springContext");
        if (ctx != null) ctx.close();
    }
}