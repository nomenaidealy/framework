package idealyfw.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import org.springframework.context.ApplicationContext;

import idealyfw.exception.ExceptionUrl;
import idealyfw.util.Mapping;
import idealyfw.util.ModelAndView;
import idealyfw.util.UrlMethod;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class FrontControllerServlet extends HttpServlet {

    Map<UrlMethod, Mapping> mappings;
    ApplicationContext springContext;
    String prefix;
    String suffix;

    @Override
    public void init() throws ServletException {

      
        mappings = (Map<UrlMethod, Mapping>) getServletContext()
                        .getAttribute("globalMappings");

     
        springContext = (ApplicationContext) getServletContext()
                        .getAttribute("springContext");

    
        if (mappings == null) {
            throw new ServletException(
                "[FrontController] 'globalMappings' introuvable. " +
                "Le Listener a-t-il bien démarré ?"
            );
        }

        if (springContext == null) {
            throw new ServletException(
                "[FrontController] 'springContext' introuvable. " +
                "Vérifiez springConfigClass dans web.xml."
            );
        }

  
        prefix = getServletContext().getInitParameter("prefix");
        suffix = getServletContext().getInitParameter("suffix");

        if (prefix == null) prefix = "";
        if (suffix == null) suffix = "";

        System.out.println("[FrontController] initialisé ");
        System.out.println("[FrontController] prefix = " + prefix);
        System.out.println("[FrontController] suffix = " + suffix);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        processRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        processRequest(req, resp);
    }

    protected void processRequest(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("text/html;charset=UTF-8");

        String uri        = req.getRequestURI();
        String context    = req.getContextPath();
        String url        = uri.substring(context.length());
        String methodHttp = req.getMethod();

        System.out.println("[FrontController] " + methodHttp + " " + url);

        try {
       
            UrlMethod key   = new UrlMethod(url, methodHttp);
            Mapping mapping = mappings.get(key);

            if (mapping == null) {
                throw new ExceptionUrl(url + " [" + methodHttp + "]");
            }

          
            Object controllerInstance = springContext
                    .getBean(mapping.getControllerClass());

           
            Method method       = mapping.getMethod();
            Class<?>[] params   = method.getParameterTypes();
            Object result;

            if (params.length == 1 &&
                params[0] == ApplicationContext.class) {
              
                result = method.invoke(controllerInstance, springContext);

            } else {
              
                result = method.invoke(controllerInstance);
            }

         

          
            if (result instanceof ModelAndView mv) {

                for (Map.Entry<String, Object> entry :
                        mv.getModel().entrySet()) {
                    req.setAttribute(entry.getKey(), entry.getValue());
                }

                String viewPath = prefix + mv.getView() + suffix;
                System.out.println("[FrontController] forward → " + viewPath);
                getServletContext()
                    .getRequestDispatcher(viewPath)
                    .forward(req, resp);

          
            } else if (result instanceof String texte) {
                resp.getWriter().println(texte);

            } else {
                System.out.println("[FrontController] résultat ignoré : " + result);
            }

        } catch (ExceptionUrl e) {

            PrintWriter out = resp.getWriter();
            out.println("<html><body>");
            out.println("<h2 style='color:red;'>404 — URL introuvable</h2>");
            out.println("<p><b>URL demandée :</b> "
                        + methodHttp + " " + url + "</p>");
            out.println("<h3>Mappings disponibles :</h3>");
            out.println("<ul>");
            for (Map.Entry<UrlMethod, Mapping> entry : mappings.entrySet()) {
                UrlMethod k = entry.getKey();
                Mapping   m = entry.getValue();
                out.println("<li>"
                        + k.getMethod() + " " + k.getUrlString()
                        + " → "
                        + m.getControllerClass().getSimpleName()
                        + "." + m.getMethod().getName()
                        + "</li>");
            }
            out.println("</ul>");
            out.println("</body></html>");

        } catch (IllegalAccessException e) {

            PrintWriter out = resp.getWriter();
            out.println("<html><body>");
            out.println("<h2 style='color:red;'>Accès interdit</h2>");
            out.println("<p>" + e.getMessage() + "</p>");
            out.println("</body></html>");

        } catch (InvocationTargetException e) {

            Throwable cause = e.getCause();
            cause.printStackTrace();
            PrintWriter out = resp.getWriter();
            out.println("<html><body>");
            out.println("<h2 style='color:red;'>Erreur dans le contrôleur</h2>");
            out.println("<p>" + cause.getMessage() + "</p>");
            out.println("</body></html>");

        } catch (ReflectiveOperationException e) {

            e.printStackTrace();
            PrintWriter out = resp.getWriter();
            out.println("<html><body>");
            out.println("<h2 style='color:red;'>Erreur réflexion</h2>");
            out.println("<p>" + e.getMessage() + "</p>");
            out.println("</body></html>");
        }
    }
}