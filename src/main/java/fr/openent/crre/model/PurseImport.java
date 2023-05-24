package fr.openent.crre.model;

import java.util.List;

public class PurseImport {
    private List<String> uaiError;
    private String base64;

    public PurseImport(List<String> uaiErrorList, String base64) {
        this.uaiError = uaiErrorList;
        this.base64 = base64;
    }

    public List<String> getUaiError() {
        return uaiError;
    }

    public PurseImport setUaiError(List<String> uaiError) {
        this.uaiError = uaiError;
        return this;
    }

    public String getBase64() {
        return base64;
    }

    public PurseImport setBase64(String base64) {
        this.base64 = base64;
        return this;
    }

}
