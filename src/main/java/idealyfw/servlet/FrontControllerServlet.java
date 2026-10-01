package idealyfw.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

import idealyfw.annotation.ApiRest;
import idealyfw.exception.ExceptionUrl;
import idealyfw.util.JsonUtil;
import idealyfw.util.Mapping;
import idealyfw.util.ModelAndView;
import idealyfw.util.UrlMethod;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Front Controller : point d'entrée unique de toutes les requêtes HTTP.
 *
 * Rôle :
 *  1. Retrouver, à partir de l'URL + verbe HTTP, quelle méthode de quel
 *     contrôleur doit être exécutée (grâce à la Map "globalMappings"
 *     construite au démarrage par ParamScanUtil / RequestContextListener).
 *  2. Invoquer cette méthode par réflexion.
 *  3. Traiter la valeur de retour selon deux modes :
 *       - mode "vue"  (par défaut) : ModelAndView -> forward vers une JSP
 *       - mode "API"  (@ApiRest)   : conversion en JSON -> écrit dans la réponse
 */
public class FrontControllerServlet extends HttpServlet {

    // Table de routage globale : (URL, verbe HTTP) -> (classe + méthode à invoquer)
    Map<UrlMethod, Mapping> mappings;

    // Contexte Spring : permet de récupérer les contrôleurs en tant que beans
    // (donc avec injection de dépendances possible dedans)
    ApplicationContext springContext;

    // Chemins ajoutés autour du nom de vue renvoyé par un ModelAndView
    // ex : prefix = "/WEB-INF/views/", suffix = ".jsp"
    String prefix;
    String suffix;

    // Cache des instances de contrôleurs créées par LE FRAMEWORK (pas Spring).
    // Un seul contrôleur = une seule instance réutilisée pour toutes les requêtes
    // (comportement singleton, comme le fait Spring par défaut).
    private final Map<Class<?>, Object> controllerInstances = new ConcurrentHashMap<>();

    @Override
    public void init() throws ServletException {

        // Récupère ce que RequestContextListener a préparé au démarrage du serveur
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

    /**
     * Fournit une instance du contrôleur, gérée par LE FRAMEWORK (pas Spring).
     *
     * - Instanciation : via réflexion, constructeur par défaut (pas besoin
     *   de @Component/@ComponentScan sur les contrôleurs).
     * - Dépendances (@Autowired) : Spring les injecte quand même dans les
     *   champs de l'instance, via autowireBean(), sans que le contrôleur
     *   soit lui-même déclaré comme bean. Ça permet d'utiliser Spring
     *   uniquement pour les services/repositories, comme demandé.
     * - Mise en cache : une seule instance par classe de contrôleur,
     *   réutilisée pour toutes les requêtes (comme le ferait Spring par défaut).
     */
    private Object getControllerInstance(Class<?> controllerClass)
            throws ReflectiveOperationException {

        Object instance = controllerInstances.get(controllerClass);

        if (instance == null) {
            instance = controllerClass.getDeclaredConstructor().newInstance();

            AutowireCapableBeanFactory factory = springContext.getAutowireCapableBeanFactory();
            factory.autowireBean(instance); // remplit les champs @Autowired s'il y en a

            controllerInstances.put(controllerClass, instance);
        }

        return instance;
    }

    protected void processRequest(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Content-type par défaut : sera écrasé en "application/json" si la
        // méthode ciblée est annotée @ApiRest (voir plus bas).
        resp.setContentType("text/html;charset=UTF-8");

        String uri        = req.getRequestURI();
        String context     = req.getContextPath();
        String url         = uri.substring(context.length());
        String methodHttp  = req.getMethod();

        System.out.println("[FrontController] " + methodHttp + " " + url);

        try {
            // ---- 1. Retrouver la route ----
            UrlMethod key   = new UrlMethod(url, methodHttp);
            Mapping mapping = mappings.get(key);

            if (mapping == null) {
                throw new ExceptionUrl(url + " [" + methodHttp + "]");
            }

            // ---- 2. Récupérer le contrôleur (instancié par LE FRAMEWORK, pas Spring) ----
            // Spring n'a plus besoin de connaître la classe du contrôleur.
            // On l'instancie nous-mêmes, puis Spring injecte juste les champs
            // @Autowired (services, repositories...) s'il y en a.
            Object controllerInstance = getControllerInstance(mapping.getControllerClass());

            Method method     = mapping.getMethod();
            Class<?>[] params = method.getParameterTypes();
            Object result;

            // Cas particulier : la méthode demande le contexte Spring en paramètre
            if (params.length == 1 && params[0] == ApplicationContext.class) {
                result = method.invoke(controllerInstance, springContext);
            } else {
                result = method.invoke(controllerInstance);
            }

            // ---- 3. Mode API REST ----
            // Si la méthode porte @ApiRest, on répond en JSON et on s'arrête là :
            // pas de forward vers une vue JSP dans ce mode.
            if (method.isAnnotationPresent(ApiRest.class)) {

                resp.setContentType("application/json;charset=UTF-8");
                PrintWriter out = resp.getWriter();

                if (result instanceof String) {
                    // Le développeur a déjà construit le JSON lui-même (String).
                    // Rien à convertir : on l'écrit tel quel.
                    String jsonBrut = (String) result;
                    out.print(jsonBrut);
                } else {
                    // Le développeur renvoie un objet (POJO, List, Map...).
                    // Le framework le sérialise en JSON via Jackson.
                    out.print(JsonUtil.objectToJson(result));
                }

                out.flush();
                return; // fin du traitement pour cette requête
            }

            // ---- 4. Mode "vue" classique (comportement par défaut) ----
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