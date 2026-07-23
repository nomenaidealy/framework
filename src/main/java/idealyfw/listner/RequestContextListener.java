package idealyfw.listner;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import idealyfw.util.Mapping;
import idealyfw.util.ParamScanUtil;
import idealyfw.util.UrlMethod;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebListener;
import java.util.Map;

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
            scanner.scan(packageName);
            Map<UrlMethod, Mapping> mappings = scanner.registry.getMappings();

            sce.getServletContext().setAttribute("globalMappings", mappings);

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