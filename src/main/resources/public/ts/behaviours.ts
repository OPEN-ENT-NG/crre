import { Behaviours } from 'entcore';
import {SnipletScrollService} from "./services/sniplet-scroll-service";
import {InfiniteScrollService} from "./services/infinite-scroll.service";

Behaviours.register('crre', {
    rights: {
        workflow: {
            access: 'fr.openent.crre.controllers.CrreController|view',
            administrator: 'fr.openent.crre.controllers.PurseController|purse',
            prescriptor: 'fr.openent.crre.controllers.BasketController|takeOrder',
            validator: 'fr.openent.crre.controllers.OrderRegionController|createAdminOrder',
            reassort: 'fr.openent.crre.controllers.BasketController|updateReassort',
            updateStudent: 'fr.openent.crre.controllers.StructureController|updateAmount'
        },
        resource: {}
    },
    SnipletScrollService: new SnipletScrollService,
    InfiniteScrollService: new InfiniteScrollService
});
