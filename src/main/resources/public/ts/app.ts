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
    if (model.me.hasWorkflow(Behaviours.applicationsBehaviours.lystore.rights.workflow.administrator)) {
        $routeProvider.when('/campaigns/create', {
            action: 'createCampaigns'
        })
            .when('/campaigns/update', {
                action: 'updateCampaigns'
            })
            .when('/equipments/create', {
                action: 'createEquipment'
            })
            .when('/logs', {
                action: 'viewLogs'
            })
            .when('/structureGroups/create', {
                    action: 'createStructureGroup'
                }
            )
        ;
    }
    if (model.me.hasWorkflow(Behaviours.applicationsBehaviours.lystore.rights.workflow.manager)) {
        $routeProvider.when('/campaigns', {
            action: 'manageCampaigns'
        })
            .when('/agents', {
                action: 'manageAgents'
            })
            .when('/suppliers', {
                action: 'manageSuppliers'
            })
            .when('/contracts', {
                action: 'manageContracts'
            })
            .when('/tags', {
                action: 'manageEquipmentTags'
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
            .when('/campaigns/:idCampaign/titles', {
                action: 'manageTitles'
            })
            .when('/order/update/:idOrder', {
                action: 'updateOrder'
            })
            .when('/order/waiting', {
                action: 'orderWaiting'
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
            .when('/order/region/create', {
                action: 'createRegionOrder'
            })
            .when('/operation',{
                action:'operation'
            })
            .when('/instruction', {
                action: 'instruction'
            })
            .when('/operation/order/:idOperation', {
                action: 'operationOrders'
            })
            .when('/exports', {
                action: 'exportList'
            });
    } else {
        $routeProvider
            .when('/campaign/:idCampaign/catalog', {
                action: 'campaignCatalog'
            })
            .when('/campaign/:idCampaign/catalog/equipment/:idEquipment', {
                action: 'equipmentDetail'
            })
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