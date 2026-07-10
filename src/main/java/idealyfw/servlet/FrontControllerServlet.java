package idealyfw.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

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
    String prefix;
    String suffix;

    @Override
    public void init() throws ServletException {

        // récupère les mappings du Listener
        mappings = (Map<UrlMethod, Mapping>) getServletContext()
                        .getAttribute("globalMappings");

        if (mappings == null) {
            throw new ServletException(
                "[FrontController] Erreur : 'globalMappings' introuvable. Le Listener a-t-il bien démarré ?"
            );
        }

        // lecture prefix et suffix depuis web.xml
        prefix = getServletContext().getInitParameter("prefix");
        suffix = getServletContext().getInitParameter("suffix");

        // valeurs par défaut si non définis
        if (prefix == null) prefix = "";
        if (suffix == null) suffix = "";
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

        

        String uri        = req.getRequestURI();
        String context    = req.getContextPath();
        String url        = uri.substring(context.length());
        String methodHttp = req.getMethod();

        try {
            // cherche le mapping directement dans la map du Listener
            UrlMethod key = new UrlMethod(url, methodHttp);
            Mapping mapping = mappings.get(key);

            if (mapping == null) {
                throw new ExceptionUrl(url + " [" + methodHttp + "]");
            }

            Object controllerInstance = mapping.getControllerInstance();
            Method method             = mapping.getMethod();

            // execute la méthode UNE SEULE fois
            Object result = method.invoke(controllerInstance);
           
            // cas 1 : retourne un ModelAndView → forward vers JSP
            if (result instanceof ModelAndView) {

                ModelAndView mv = (ModelAndView) result;

                // mettre les données dans la requête
                for (Map.Entry<String, Object> entry : mv.getModel().entrySet()) {
                    req.setAttribute(entry.getKey(), entry.getValue());
                }

                // construire le chemin complet
                // ex: "/WEB-INF/view/" + "accueil" + ".jsp"
                String viewPath = prefix + mv.getView() + suffix;
              
                System.out.println(viewPath);
                // forward vers la JSP
                getServletContext().getRequestDispatcher(viewPath).forward(req, resp);
            //    return;

            // cas 2 : retourne un String → affiche directement
            } else {

            }
            

        } catch (ExceptionUrl e) {
          
            PrintWriter out = resp.getWriter();
            out.println("<html><body>");
            out.println("<p style='color:red;'>404 - URL introuvable : "
                        + e.getMessage() + "</p>");
            out.println("<h3>TOUS LES MAPPINGS</h3>");

            for (Map.Entry<UrlMethod, Mapping> entry : mappings.entrySet()) {
                UrlMethod k = entry.getKey();
                Mapping m   = entry.getValue();
                out.println("<p>"
                        + k.getMethod() + " "
                        + k.getUrlString() + " -> "
                        + m.getControllerClass().getSimpleName() + "."
                        + m.getMethod().getName()
                        + "</p>");
            }
            out.println("</body></html>");
            out.close();

        } catch (IllegalAccessException e) {

          
            PrintWriter out = resp.getWriter();
            out.println("<html><body>");
            out.println("<p style='color:red;'>Accès interdit à la méthode : "
                        + e.getMessage() + "</p>");
            out.println("</body></html>");
            out.close();

        } catch (InvocationTargetException e) {

            Throwable cause = e.getCause();
          
            PrintWriter out = resp.getWriter();
            out.println("<html><body>");
            out.println("<p style='color:red;'>Erreur dans le contrôleur : "
                        + cause.getMessage() + "</p>");
            out.println("</body></html>");
            out.close();

        } catch (ReflectiveOperationException e) {
          
            PrintWriter out = resp.getWriter();
            out.println("<html><body>");
            out.println("<p style='color:red;'>Erreur réflexion : "
                        + e.getMessage() + "</p>");
            out.println("</body></html>");
            e.printStackTrace();
            out.close();
        }
    }
}