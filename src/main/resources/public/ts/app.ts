import {Behaviours, model, ng, routes} from 'entcore';
import * as controllers from './controllers';
import * as directives from './directives';
import * as filters from './filters';
import * as services from './services';

for (let controller in controllers) {
    ng.controllers.push(controllers[controller]);
}

for (let directive in directives) {
    ng.directives.push(directives[directive]);
}

for (let filter in filters) {
    ng.filters.push(filters[filter]);
}

for (let service in services) {
    ng.services.push(services[service]);
}

routes.define(($routeProvider) => {
    $routeProvider
        .when('/', {
            action: 'main'
        });
    if (model.me.hasWorkflow(Behaviours.applicationsBehaviours.crre.rights.workflow.administrator)) {
        $routeProvider
            .when('/equipments/catalog', {
                action: 'showAdminCatalog'
            })
            .when('/campaigns/create', {
                action: 'createOrUpdateCampaigns'
            })
            .when('/campaigns/update', {
                action: 'createOrUpdateCampaigns'
            })
            .when('/logs', {
                action: 'viewLogs'
            })
            .when('/statistics', {
                action: 'viewStats'
            })
            .when('/structureGroups/create', {
                action: 'createStructureGroup'
            })
            .when('/structureGroups', {
                action: 'manageStructureGroups'
            })
            .when('/purses', {
                action: 'managePurse'
            })
            .when('/equipments/catalog/equipment/:idEquipment/:idCampaign', {
                action: 'adminEquipmentDetail'
            })
            .when('/order/historicAdmin', {
                action: 'orderHistoricAdmin'
            })
            .when('/order/waitingAdmin', {
                action: 'orderWaitingAdmin'
            })
            .when('/campaigns', {
                action: 'manageCampaigns'
            });
    } else {
        $routeProvider
            .when('/equipments/catalog/:idCampaign', {
                action: 'showCatalog'
            })
            .when('/equipments/catalog/equipment/:idEquipment/:idCampaign', {
                action: 'equipmentDetail'
            });
    }
    if (model.me.hasWorkflow(Behaviours.applicationsBehaviours.crre.rights.workflow.validator) &&
    !model.me.hasWorkflow(Behaviours.applicationsBehaviours.crre.rights.workflow.administrator)) {
        $routeProvider
            .when('/order/waiting/:idStructure?', {
                action: 'orderWaiting'
            })
            .when('/order/historic/:idStructure?', {
                action: 'orderHistoric'
            });

    }
    if (model.me.hasWorkflow(Behaviours.applicationsBehaviours.crre.rights.workflow.prescriptor) &&
    !model.me.hasWorkflow(Behaviours.applicationsBehaviours.crre.rights.workflow.administrator)) {
        $routeProvider
            .when('/campaign/:idCampaign/order/:idStructure?', {
                action: 'campaignOrder'
            })
            .when('/campaign/:idCampaign/basket', {
                action: 'campaignBasket'
            });
    }
    $routeProvider.otherwise({
        redirectTo: '/'
    });
});