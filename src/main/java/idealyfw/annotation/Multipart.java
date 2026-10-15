package idealyfw.annotation ;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target ({ElementType.TYPE, ElementType.METHOD})
@Retention (RetentionPolicy.RUNTIME)
public @interface  Multipart {
    long maxFileSize() default 5 * 1024 * 1024;
    long maxRequestSize() default 20 * 1024 * 1024 ;
    long fileSizeThreshold() default 1024 * 1024 ;
}
