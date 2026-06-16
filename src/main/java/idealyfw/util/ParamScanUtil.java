package idealyfw.util;

import java.io.File;
import idealyfw.annotation.Controller;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ParamScanUtil {

    public static List<Class<?>> scan(String packageName)
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
        File[] classFiles = packageDir.listFiles(
            file -> file.getName().endsWith(".class")
        );

        // ✅ garde-fou 2
        if (classFiles == null) return controllers;

        for (File file : classFiles) {
            String className = file.getName().replace(".class", "");
            String fullname  = packageName + "." + className;
            Class<?> clazz   = Class.forName(fullname);
            if (clazz.isAnnotationPresent(Controller.class)) {
                controllers.add(clazz);
                System.out.println("[Scanner] Controller trouve : " + clazz.getName());
            }
        }

        return controllers;
    }
}