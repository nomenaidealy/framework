package idealyfw.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileUpload {
    private final String nomOriginal ; 
    private final String type ; 
    private final byte[] contenu ; 


    public FileUpload(String nomOriginal, String type, byte[] contenu) {
        this.nomOriginal = nomOriginal;
        this.type = type;
        this.contenu = contenu;
    }

    public String getNomOriginal() {
        return nomOriginal;
    }

    public String getType() {
        return type;
    }

    public byte[] getContenu() {
        return contenu;
    }

    public  long getTaille(){
        return  contenu.length ;
    }
    public  boolean isEmpty(){
        return contenu.length == 0 ;
    }

    public Path saveTo(Path dossier) throws IOException {
    Files.createDirectories(dossier);
    Path cible = dossier.resolve(nomOriginal).normalize();
    if (!cible.startsWith(dossier.normalize())) {
        throw new IOException("Nom de fichier invalide");
    }
    return Files.write(cible, contenu);
}
}
