package fr.openent.crre.model;

import fr.openent.crre.core.enums.ColumnsLDEOrders;

import java.util.regex.Pattern;

public class OrderLDEModel {
    private String etat;
    private String CGIId;

    public OrderLDEModel(String line) {
        String[] values = line.split(Pattern.quote("|"));
        this.etat = values[ColumnsLDEOrders.ETAT.column()];
        this.CGIId = values[ColumnsLDEOrders.ID_CGI.column()];
    }

    public String getEtat() {
        return etat;
    }

    public OrderLDEModel setEtat(String etat) {
        this.etat = etat;
        return this;
    }

    public String getCGIId() {
        return CGIId;
    }

    public OrderLDEModel setCGIId(String CGIId) {
        this.CGIId = CGIId;
        return this;
    }
}
