package idealyfw.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import idealyfw.util.ParamScanUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class FrontControllerServlet extends HttpServlet {
     private List<Class<?>> controllers = new ArrayList<>();
    @Override
    public void init() throws ServletException {
        String packageName = getServletConfig()
            .getInitParameter("controllerPackage");
        try {
            controllers = ParamScanUtil.scan(packageName);  
            System.out.println("[FrontController] "
                + controllers.size() + " controller(s) trouvés :");
            for (Class<?> c : controllers) {
                System.out.println("  → " + c.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        PrintWriter out = resp.getWriter();

        String uri    = req.getRequestURI();   
        String method = req.getMethod();       
       

        out.println("<html><body>");
        out.println("<h2>FrontController actif</h2>");
        out.println("<p>URI : " + uri + "</p>");
        out.println("<p>Méthode : " + method + "</p>");

         out.println("<h3>Controllers trouves :</h3>");
        out.println("<ul>");
        for (Class<?> c : controllers) {
            out.println("<li>" + c.getName() + "</li>");
        }
        out.println("</ul>");

        out.println("</body></html>");

        
        System.out.println("[FrontController] " + method + " " + uri);
    }
}
