package idealyfw.exception;

public class ExceptionUrl extends RuntimeException{
    
    public ExceptionUrl(String url) {
        super("404 - URL introuvable : " + url);
    }
}
