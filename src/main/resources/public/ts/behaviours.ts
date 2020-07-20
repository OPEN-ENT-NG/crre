import { Behaviours } from 'entcore';

Behaviours.register('crre', {
    rights: {
        workflow: {
            access: 'fr.openent.crre.controllers.CrreController|view',
            administrator: 'fr.openent.crre.controllers.AgentController|createAgent',
            manager: 'fr.openent.crre.controllers.AgentController|getAgents'
        },
        resource: {}
    }
});
