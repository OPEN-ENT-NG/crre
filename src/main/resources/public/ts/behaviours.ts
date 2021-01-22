import { Behaviours } from 'entcore';

Behaviours.register('crre', {
    rights: {
        workflow: {
            access: 'fr.openent.crre.controllers.CrreController|view',
            administrator: 'fr.openent.crre.controllers.AgentController|createAgent',
            manager: 'fr.openent.crre.controllers.AgentController|getAgents',
            prescriptor: 'fr.openent.crre.controllers.AgentController|deleteAgent',
            validator: 'fr.openent.crre.controllers.AgentController|updateAgent',
            reassort: 'fr.openent.crre.controllers.BasketController|updateReassort'
        },
        resource: {}
    }
});
