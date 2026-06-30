package idealyfw.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Mapping {

    private Class<?> controllerClass;
    private Method method;
  

    public Mapping(Class<?> controllerClass, Method method) {
        this.controllerClass = controllerClass;
        this.method = method;
       
    }

    public Class<?> getControllerClass() {
        return controllerClass;
    }

    public Method getMethod() {
        return method;
    }

    public Object getControllerInstance() throws ReflectiveOperationException {
        return controllerClass.getDeclaredConstructor().newInstance();
    }
}