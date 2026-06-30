package idealyfw.exception;

import idealyfw.util.UrlMethod;

public class ExceptionDoublageURL extends RuntimeException {
    public ExceptionDoublageURL(UrlMethod key){
        super("meme url , avec meme methode :" + key.getUrlString() + "," + key.getMethod() ) ;
    }
}
