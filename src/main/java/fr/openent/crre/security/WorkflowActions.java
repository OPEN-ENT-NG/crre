package fr.openent.crre.security;

import fr.openent.crre.Crre;

public enum WorkflowActions {
    ACCESS_RIGHT (Crre.ACCESS_RIGHT),
    ADMINISTRATOR_RIGHT (Crre.ADMINISTRATOR_RIGHT),
    VALIDATOR_RIGHT (Crre.VALIDATOR_RIGHT),
    PRESCRIPTOR_RIGHT (Crre.PRESCRIPTOR_RIGHT),
    REASSORT_RIGHT (Crre.REASSORT_RIGHT),
    UPDATE_STUDENT_RIGHT(Crre.UPDATE_STUDENT_RIGHT);

    private final String actionName;

    WorkflowActions(String actionName) {
        this.actionName = actionName;
    }

    @Override
    public String toString () {
        return this.actionName;
    }
}
