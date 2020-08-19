package fr.openent.crre.security;

public enum WorkflowActions {
    ACCESS_RIGHT ("crre.access"),
    ADMINISTRATOR_RIGHT ("crre.administrator"),
    VALIDATOR_RIGHT ("crre.validator"),
    MANAGER_RIGHT ("crre.manager");

    private final String actionName;

    WorkflowActions(String actionName) {
        this.actionName = actionName;
    }

    @Override
    public String toString () {
        return this.actionName;
    }
}
