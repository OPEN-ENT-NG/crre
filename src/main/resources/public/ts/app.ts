import {Behaviours, model, ng, routes} from 'entcore';
import * as controllers from './controllers';
import * as directives from './directives';
import * as filters from './filters';

for (let controller in controllers) {
    ng.controllers.push(controllers[controller]);
}

for (let directive in directives) {
    ng.directives.push(directives[directive]);
}

for (let filter in filters) {
    ng.filters.push(filters[filter]);
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
            .when('/equipments/create', {
                action: 'createEquipment'
            })
            .when('/logs', {
                action: 'viewLogs'
            })
            .when('/structureGroups/create', {
                action: 'createStructureGroup'
            })
            .when('/equipments', {
                action: 'manageEquipments'
            })
            .when('/structureGroups', {
                action: 'manageStructureGroups'
            })
            .when('/campaigns/:idCampaign/purse', {
                action: 'managePurse'
            })
            .when('/equipments/catalog/equipment/:idEquipment', {
                action: 'adminEquipmentDetail'
            })
            .when('/order/historic', {
                action: 'orderHistoricAdmin'
            })
            .when('/order/waiting', {
                action: 'orderWaitingAdmin'
            });
    } else {
        $routeProvider
            .when('/equipments/catalog', {
                action: 'showCatalog'
            })
            .when('/equipments/catalog/equipment/:idEquipment', {
                action: 'equipmentDetail'
            });
    }
    if (model.me.hasWorkflow(Behaviours.applicationsBehaviours.crre.rights.workflow.manager)) {
        $routeProvider
            .when('/campaigns', {
                action: 'manageCampaigns'
            })
            .when('/exports', {
                action: 'exportList'
            });
    }
    if (model.me.hasWorkflow(Behaviours.applicationsBehaviours.crre.rights.workflow.validator)) {
        $routeProvider
            .when('/order/:idCampaign/waiting', {
                action: 'orderWaiting'
            })
            .when('/order/region/create', {
                action: 'createRegionOrder'
            })
            .when('/order/update/:idOrder', {
                action: 'updateOrder'
            })
            .when('/order/sent', {
                action: 'orderSent'
            })
            .when('/order/valid', {
                action: 'orderClientValided'
            })
            .when('/order/preview', {
                action: 'previewOrder'
            })
            .when('/order/operation/update/:idOrder/:typeOrder', {
                action: 'updateLinkedOrder'
            })
            .when('/order/:idCampaign/historic', {
                action: 'orderHistoric'
            });

    }
    if (model.me.hasWorkflow(Behaviours.applicationsBehaviours.crre.rights.workflow.prescriptor)) {
        $routeProvider
            .when('/campaign/:idCampaign/order', {
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