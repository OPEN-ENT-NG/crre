package fr.openent.lystore.security;

public enum WorkflowActions {
    ACCESS_RIGHT ("lystore.access"),
    ADMINISTRATOR_RIGHT ("lystore.administrator"),
    MANAGER_RIGHT ("lystore.manager");

    private final String actionName;

    WorkflowActions(String actionName) {
        this.actionName = actionName;
    }

    @Override
    public String toString () {
        return this.actionName;
    }
}
