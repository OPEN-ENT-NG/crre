package fr.openent.lystore.export.instructions.RME;

public enum TabName {
    LYCEE("Investissement-LYCEES"),
    CMR("Investissement-CMR"),
    CMD("Investissement-CMD"),
    FONCTIONNEMENT("Fonctionnement"),
    EPLE("Récap. EPLE"),
    IMPUTATION_BUDG("Récap. Imputation budgétaire");

    private final String tabName;

    TabName(String tabName) {
        this.tabName = tabName;
    }

    @Override
    public String toString() {
        return tabName;
    }
}
