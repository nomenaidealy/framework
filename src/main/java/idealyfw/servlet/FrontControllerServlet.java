package idealyfw.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.module.ModuleDescriptor.Modifier;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Parameter;
import java.io.File;
import jakarta.servlet.http.Part;
import idealyfw.util.FileUpload;
import java.nio.file.Paths;

import idealyfw.util.MySession;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

import idealyfw.annotation.ApiRest;
import idealyfw.annotation.Auth;
import idealyfw.exception.ExceptionUrl;
import idealyfw.util.JsonUtil;
import idealyfw.util.Mapping;
import idealyfw.util.ModelAndView;
import idealyfw.util.UrlMethod;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.annotation.MultipartConfig;
import idealyfw.util.MySession;

@MultipartConfig(
    fileSizeThreshold = 1024 * 1024,        // au-delà de 1 Mo : fichier temporaire sur disque
    maxFileSize       = 5 * 1024 * 1024,    // 5 Mo max par fichier
    maxRequestSize    = 10 * 1024 * 1024    // 10 Mo max pour toute la requête
)

public class FrontControllerServlet extends HttpServlet {

    Map<UrlMethod, Mapping> mappings;

    ApplicationContext springContext;

    String prefix;
    String suffix;

    String userKey;
    String roleKey;

    private  Map<Class<?>, Object> controllerInstances ;

    @Override
    public void init() throws ServletException {

        mappings = (Map<UrlMethod, Mapping>) getServletContext()
                .getAttribute("globalMappings");

        
        controllerInstances = (Map<Class<?>, Object>) getServletContext().getAttribute("ctrlInstances");
        

        springContext = (ApplicationContext) getServletContext()
                .getAttribute("springContext");

        if (mappings == null) {
            throw new ServletException(
                    "[FrontController] 'globalMappings' introuvable. " +
                            "Le Listener a-t-il bien démarré ?");
        }

        if (springContext == null) {
            throw new ServletException(
                    "[FrontController] 'springContext' introuvable. " +
                            "Vérifiez springConfigClass dans web.xml.");
        }

        prefix = getServletContext().getInitParameter("prefix");
        suffix = getServletContext().getInitParameter("suffix");

        userKey = getServletContext().getInitParameter("sessionUserKey");
        roleKey = getServletContext().getInitParameter("sessionRoleKey");


        if (prefix == null)
            prefix = "";
        if (suffix == null)
            suffix = "";

        if (userKey == null || userKey.isBlank()) userKey = "user";  
        if (roleKey == null || roleKey.isBlank()) roleKey = "role";

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

    private Object getControllerInstance(Class<?> controllerClass)
            throws ReflectiveOperationException {

        Object instance = controllerInstances.get(controllerClass);

        // if (instance == null) {
        //     instance = controllerClass.getDeclaredConstructor().newInstance();

        //     AutowireCapableBeanFactory factory = springContext.getAutowireCapableBeanFactory();
        //     factory.autowireBean(instance);

        //     controllerInstances.put(controllerClass, instance);
        // }

        return instance;
    }

    private Object convert(String valeurBrute, Class<?> type) {

        if (valeurBrute == null || valeurBrute.isEmpty()) {
            if (type == int.class)
                return 0;
            if (type == long.class)
                return 0L;
            if (type == double.class)
                return 0.0;
            if (type == float.class)
                return 0f;
            if (type == boolean.class)
                return false;
            return null;
        }

        if (type == String.class) {
            return valeurBrute;
        }
        if (type == int.class || type == Integer.class) {
            return Integer.parseInt(valeurBrute);
        }
        if (type == long.class || type == Long.class) {
            return Long.parseLong(valeurBrute);
        }
        if (type == double.class || type == Double.class) {
            return Double.parseDouble(valeurBrute);
        }
        if (type == float.class || type == Float.class) {
            return Float.parseFloat(valeurBrute);
        }
        if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(valeurBrute);
        }

        return valeurBrute;
    }

    private boolean isSimpleType(Class<?> type) {
        return type.isPrimitive()
                || type == String.class
                || Number.class.isAssignableFrom(type)
                || type == Boolean.class
                || type == Character.class;
    }

    private boolean hasParamWithPrefix(String prefixe, HttpServletRequest req) {
        for (String nom : req.getParameterMap().keySet()) {
            if (nom.startsWith(prefixe))
                return true;
        }
        return false;
    }

    private Object bind(Class<?> type, String prefixe, HttpServletRequest req) throws ReflectiveOperationException , IOException, ServletException {

       
        Object objet = type.getDeclaredConstructor().newInstance();

      
        for (Field champ : type.getDeclaredFields()) {

            String nomParam = prefixe + champ.getName();     // "e.photo"
            Class<?> typeChamp = champ.getType();
            Object valeur;

            if (typeChamp == FileUpload.class) {          
                valeur = readFile(req, nomParam);
                if (valeur == null) continue;

            } else if (typeChamp == byte[].class) {       
                FileUpload f = readFile(req, nomParam);
                if (f == null) continue;
                valeur = f.getContenu();

            } else if (isSimpleType(typeChamp)) {
                String brut = req.getParameter(nomParam);
                if (brut == null) continue;
                valeur = convert(brut, typeChamp);

            } else {
                if (!hasParamWithPrefix(nomParam + ".", req)) continue;
                valeur = bind(typeChamp, nomParam + ".", req);
            }

            champ.setAccessible(true);
            champ.set(objet, valeur);
        }

        return objet;
    }

    private  FileUpload readFile(HttpServletRequest req , String nomChamp)throws IOException, ServletException {
        String ct = req.getContentType();
        if (ct == null || !ct.toLowerCase().startsWith("multipart/")) {
            return null;
        }
        Part part = req.getPart(nomChamp) ;
        if(part == null ) return  null ;
        String nomFichier = part.getSubmittedFileName() ;
        if(nomFichier == null || nomFichier .isBlank()){
            return  null ;
        }
        nomFichier = Paths.get(nomFichier).getFileName().toString() ;
        byte[] octets ;
        try( var in = part.getInputStream()) {
            octets = in.readAllBytes();
        }
        return  new FileUpload(nomFichier , part.getContentType(), octets) ;
    }


    private Object[] resolveArguments(Method method, HttpServletRequest req , MySession mySession)
            throws ReflectiveOperationException , IOException, ServletException {

        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            String nom = parameters[i].getName();
            System.out.println("param = " + nom + " | Content-Type = " + req.getContentType());
            Class<?> type = parameters[i].getType();

            if (type == HttpServletRequest.class) {
                args[i] = req;
            } 
             else if (type == MySession.class) {       
                args[i] = mySession; }
            else if (type == FileUpload.class) {            
                args[i] = readFile(req, nom);

            } else if (type == byte[].class) {                 
                FileUpload f = readFile(req, nom);
                args[i] = (f == null) ? null : f.getContenu();

            }
            else if (isSimpleType(type)) {
                args[i] = convert(req.getParameter(nom), type);
            } else {
                args[i] = bind(type, nom + ".", req); 
            }
        }
        return args;
    }

    private String lireString(HttpSession http, String cle) {
        if (http == null) return null;
        Object v = http.getAttribute(cle);
        return (v == null) ? null : v.toString();
    }

    protected void processRequest(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {

    resp.setContentType("text/html;charset=UTF-8");

    String uri = req.getRequestURI();
    String context = req.getContextPath();
    String url = uri.substring(context.length());
    String methodHttp = req.getMethod();

    System.out.println("[FrontController] " + methodHttp + " " + url);

    // 1. fichiers statiques (css, images, js...)
    String realPath = getServletContext().getRealPath(url);
    if (realPath != null) {
        File fichier = new File(realPath);
        if (fichier.isFile()) {
            getServletContext().getNamedDispatcher("default").forward(req, resp);
            return;
        }
    }

    try {

        // 2. chercher le mapping
        UrlMethod key = new UrlMethod(url, methodHttp);
        Mapping mapping = mappings.get(key);
      
        if (mapping == null) {
            throw new ExceptionUrl(url + " [" + methodHttp + "]");
        }

        // 3. instance du contrôleur
        Object controllerInstance = getControllerInstance(mapping.getControllerClass());

        Method method = mapping.getMethod();


        Auth auth = method.getAnnotation(Auth.class);
        if (auth == null) auth = mapping.getControllerClass().getAnnotation(Auth.class);

        if (auth != null) {
            HttpSession http = req.getSession(false);
            String user = lireString(http, userKey);

            if (user == null) {
                resp.sendRedirect(req.getContextPath() + "/login/form");
                return;
            }

            String[] roles = auth.roles();
            if (roles.length > 0) {
                String roleUser = lireString(http, roleKey);
                boolean ok = false;
                for (String r : roles) {
                    if (r.equals(roleUser)) ok = true;
                }
                if (!ok) {
                    resp.sendError(403, "Accès refusé : rôle " + roleUser);
                    return;
                }
            }
        }
        Class<?>[] params = method.getParameterTypes();
        Object result;

        // 4. préparer les arguments et invoquer
        if (params.length == 1 && params[0] == ApplicationContext.class) {
            result = method.invoke(controllerInstance, springContext);

        } else if (params.length == 0) {
            result = method.invoke(controllerInstance);

        } else {
            // créer la session seulement si la méthode en a besoin
            boolean veutSession = false;
            for (Class<?> t : params) {
                if (t == MySession.class) veutSession = true;
            }
            MySession mySession = veutSession ? new MySession(req) : null;

            Object[] args = resolveArguments(method, req, mySession);
            try {
                result = method.invoke(controllerInstance, args);
            } finally {
                // sauvegarde Map -> HttpSession, avant le forward vers la JSP
                if (mySession != null) mySession.syncHttpSession(req);
            }
        }

        // 5. réponse JSON
        if (method.isAnnotationPresent(ApiRest.class)) {

            resp.setContentType("application/json;charset=UTF-8");
            PrintWriter out = resp.getWriter();

            if (result instanceof String) {
                out.print((String) result);
            } else {
                out.print(JsonUtil.objectToJson(result));
            }

            out.flush();
            return;
        }

        // 6. vue ou texte
        if (result instanceof ModelAndView mv) {

            for (Map.Entry<String, Object> entry : mv.getModel().entrySet()) {
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
        out.println("<p><b>URL demandée :</b> " + methodHttp + " " + url + "</p>");
        out.println("<h3>Mappings disponibles :</h3>");
        out.println("<ul>");
        for (Map.Entry<UrlMethod, Mapping> entry : mappings.entrySet()) {
            UrlMethod k = entry.getKey();
            Mapping m = entry.getValue();
            out.println("<li>" + k.getMethod() + " " + k.getUrlString()
                    + " → " + m.getControllerClass().getSimpleName()
                    + "." + m.getMethod().getName() + "</li>");
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