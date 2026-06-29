package idealyfw.util;

import java.lang.reflect.Method;

public class Mapping {

    private Class<?> controllerClass;
    private Method method;
    private Object controllerInstance; // ← nouveau

    public Mapping(Class<?> controllerClass, Method method, Object controllerInstance) {
        this.controllerClass = controllerClass;
        this.method = method;
        this.controllerInstance = controllerInstance;
    }

    public Class<?> getControllerClass() {
        return controllerClass;
    }

    public Method getMethod() {
        return method;
    }

    public Object getControllerInstance() {
        return controllerInstance;
    }
}