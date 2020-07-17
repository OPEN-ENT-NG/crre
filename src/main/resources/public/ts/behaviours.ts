import { Behaviours } from 'entcore';

Behaviours.register('lystore', {
    rights: {
        workflow: {
            access: 'fr.openent.lystore.controllers.LystoreController|view',
            administrator: 'fr.openent.lystore.controllers.AgentController|createAgent',
            manager: 'fr.openent.lystore.controllers.AgentController|getAgents'
        },
        resource: {}
    },
    loadResources: async function (): Promise<void> {}
});
