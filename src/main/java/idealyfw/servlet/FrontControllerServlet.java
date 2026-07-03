package idealyfw.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import idealyfw.exception.ExceptionUrl;
import idealyfw.util.Mapping;
import idealyfw.util.ParamScanUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
 import idealyfw.util.UrlMethod;
public class FrontControllerServlet extends HttpServlet {

   

    Map<UrlMethod, Mapping> mappings;
    ParamScanUtil scanner = new ParamScanUtil();

    @Override
   
    public void init() throws ServletException {
        mappings = (Map<UrlMethod, Mapping>) getServletContext().getAttribute("globalMappings");
        
        if (mappings == null) {
            throw new ServletException("[FrontController] Erreur fatale : 'globalMappings' est introuvable. Le Listener a-t-il bien démarré ?");
        }
    }


    @Override
    protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException, IOException {

        processRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws ServletException, IOException {

        processRequest(req, resp);
    }

    protected void processRequest(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {

        resp.setContentType("text/html;charset=UTF-8");

        PrintWriter out = resp.getWriter();

        String uri = req.getRequestURI();
        String context = req.getContextPath();
        String url = uri.substring(context.length());
        String methodHttp = req.getMethod();

        out.println("<html>");
        out.println("<body>");

        out.println("<h2>FrontController actif</h2>");

        out.println("<p><b>URI :</b> " + uri + "</p>");
        out.println("<p><b>URL :</b> " + url + "</p>");
        out.println("<p><b>HTTP Method :</b> " + methodHttp + "</p>");

        try {
            Mapping mapping = scanner.verifyUrl(url, methodHttp, scanner);

            Object controllerInstance = mapping.getControllerInstance();
            Method method = mapping.getMethod();
            
            Object result = scanner.registry.executeMethod(controllerInstance, method);

            out.println("<p>Méthode exécutée : " + method.getName() + "</p>");
            out.println("<p>Resultat exécutée : " + result + "</p>");

            out.println("<h3 style='color:green;'>Mapping trouvé</h3>");
            

                out.println("<p>"
                        + url + " -> "
                        + mapping.getControllerClass().getSimpleName()
                        + "."
                        + mapping.getMethod().getName()
                        + "</p>");

        } catch (ExceptionUrl e) {
             
                        
            out.println("<p style='color:red;'>" + e.getMessage() + "</p>");
            out.println("<h3 style='color:green;'>Mapping trouvé</h3>");
            out.println("<h3>TOUS LES MAPPINGS</h3>");

            for (Map.Entry<UrlMethod, Mapping> entry : mappings.entrySet()) {

                UrlMethod key = entry.getKey();
                Mapping m = entry.getValue();

                out.println("<p>"
                        + key.getMethod()
                        + " "
                        + key.getUrlString()
                        + " -> "
                        + m.getControllerClass().getSimpleName()
                        + "."
                        + m.getMethod().getName()
                        + "</p>");
            }
                    
        }catch (IllegalAccessException e) {
            out.println("<p style='color:red;'>Accès interdit à la méthode : " 
                        + e.getMessage() + "</p>");

        } catch (InvocationTargetException e) {
            
            Throwable cause = e.getCause();
            out.println("<p style='color:red;'>Erreur dans le contrôleur : " 
                        + cause.getMessage() + "</p>");
        } catch (ReflectiveOperationException e) {
            
            e.printStackTrace();
        }  

        

        out.println("</body>");
        out.println("</html>");
    }
}