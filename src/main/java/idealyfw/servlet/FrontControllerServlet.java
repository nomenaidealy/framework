package idealyfw.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class FrontControllerServlet extends HttpServlet {
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
        out.println("</body></html>");

        
        System.out.println("[FrontController] " + method + " " + uri);
    }
}
