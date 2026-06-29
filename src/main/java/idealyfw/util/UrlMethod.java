package idealyfw.util;

import java.util.Objects;

public class UrlMethod {

    private String urlString;
    private String method;

    public UrlMethod(String urlString, String method) {
        this.urlString = urlString;
        this.method = method.toUpperCase();
    }

    public String getUrlString() {
        return urlString;
    }

    public String getMethod() {
        return method;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;

        if (!(obj instanceof UrlMethod))
            return false;

        UrlMethod other = (UrlMethod) obj;

        return Objects.equals(urlString, other.urlString)
                && Objects.equals(method, other.method);
    }

    @Override
    public int hashCode() {
        return Objects.hash(urlString, method);
    }
}