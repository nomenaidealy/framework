package idealyfw.servlet;

import java.io.IOException;
import java.io.PrintWriter;
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

public class FrontControllerServlet extends HttpServlet {

    private List<Class<?>> controllers = new ArrayList<>();
    private ParamScanUtil scanner;

    @Override
    public void init() throws ServletException {

        String packageName =
                getServletConfig()
                .getInitParameter("controllerPackage");

        try {

            scanner = new ParamScanUtil();

            controllers = scanner.scan(packageName);

            System.out.println(
                    "[FrontController] "
                    + controllers.size()
                    + " controller(s) trouvé(s)"
            );

            for (Class<?> c : controllers) {
                System.out.println(" → " + c.getName());
            }

        } catch (Exception e) {
            throw new ServletException(e);
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

            
            Mapping mapping = scanner.verifyUrl(url, scanner);

            out.println("<h3 style='color:green;'>Mapping trouvé</h3>");
            out.println("<h3>TOUS LES MAPPINGS</h3>");

            for (Map.Entry<String, Mapping> entry :
                    scanner.registry.getMappings().entrySet()) {

                String u = entry.getKey();
                Mapping m = entry.getValue();

                out.println("<p>"
                        + u + " -> "
                        + m.getControllerClass().getSimpleName()
                        + "."
                        + m.getMethod().getName()
                        + "</p>");
            }
        

        } catch (ExceptionUrl e) {

            out.println("<h3 style='color:red;'>404 - URL introuvable</h3>");
            out.println("<p>" + e.getMessage() + "</p>");
        }

        out.println("</body>");
        out.println("</html>");
    }
}