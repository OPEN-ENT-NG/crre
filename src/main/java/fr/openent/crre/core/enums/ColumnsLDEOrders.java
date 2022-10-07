package fr.openent.crre.core.enums;

public enum ColumnsLDEOrders {
    TITRE(0),
    EDITEUR(1),
    DISTRIBUTEUR(2),
    TYPE(3),
    EAN(4),
    QTE(5),
    PRIX_HT(6),
    TVA5_5(7),
    TVA20(8),
    PRIX_TTC(9),
    REMISE(10),
    UAI(11),
    NOM_ETS(12),
    ADRESSE(13),
    REASSORT(14),
    TYPE_LDC(15),
    UUID_LIGNE_LDE(16),
    DATE_ACHAT(17),
    DATE_BL(18),
    REF_ACHAT(19),
    ID_BL(20),
    ETAT(21),
    ID_CGI(22);



    private final int column;

    ColumnsLDEOrders(int column) {
        this.column = column;
    }

    public int column() {
        return this.column;
    }
}
