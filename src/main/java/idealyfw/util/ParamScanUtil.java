package idealyfw.util;

import java.io.File;
import java.lang.reflect.Method;

import idealyfw.annotation.Controller;
import idealyfw.annotation.UrlMapping;
import idealyfw.exception.ExceptionUrl;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class ParamScanUtil {

    public final CustomUrlRegistry registry = new CustomUrlRegistry(); 
    
    public  List<Class<?>> scan(String packageName)
            throws URISyntaxException, ClassNotFoundException {

        List<Class<?>> controllers = new ArrayList<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        
        String path = packageName.replace('.', '/');
        URL resource = classLoader.getResource(path);

        if (resource == null) {
            System.out.println("[Scanner] Package introuvable : " + packageName);
            return controllers;
        }

        File packageDir = new File(resource.toURI());

        File[] directoryFiles = packageDir.listFiles(
            file-> file.isDirectory()
        );
        for(File file : directoryFiles) {
            String sous_package = packageName + "." + file.getName() ;
           List<Class<?>> listeController =  scan(sous_package) ;
            controllers.addAll(listeController) ;
        }

        File[] classFiles = packageDir.listFiles(
            file -> file.getName().endsWith(".class")
        );

        
        if (classFiles == null) return controllers;

        for (File file : classFiles) {
            String className = file.getName().replace(".class", "");
            String fullname  = packageName + "." + className;
            Class<?> clazz   = Class.forName(fullname);
            if (clazz.isAnnotationPresent(Controller.class)) {
                controllers.add(clazz);
                enregisterUrlMapping(clazz) ;
                System.out.println("[Scanner] Controller trouve : " + clazz.getName());
            }
        }

        return controllers;
    }

    
    public void enregisterUrlMapping(Class<?> clazzControllers) {

    Method[] methods = clazzControllers.getDeclaredMethods();

    try {
        // Une seule instance créée pour CETTE classe de contrôleur
        Object instanceUnique = clazzControllers.getDeclaredConstructor().newInstance();

        for (Method method : methods) {
            if (method.isAnnotationPresent(UrlMapping.class)) {
                UrlMapping urlMapping = method.getAnnotation(UrlMapping.class);

                Mapping mapping = new Mapping(clazzControllers, method, instanceUnique);
                registry.register(urlMapping.url(), urlMapping.method(), mapping);
            }
        }
        } catch (Exception e) {
            throw new RuntimeException("Impossible d'instancier " + clazzControllers.getName(), e);
        }
    }

   public Mapping verifyUrl(String url, String method, ParamScanUtil scanner) {

    Mapping mapping = scanner.registry.getMapping(url, method);

    if (mapping == null) {
        throw new ExceptionUrl(url + " [" + method + "]");
    }

    return mapping;
}

}