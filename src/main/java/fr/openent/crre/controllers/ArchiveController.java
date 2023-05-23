package fr.openent.crre.controllers;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.helpers.IModelHelper;
import fr.openent.crre.logging.Actions;
import fr.openent.crre.logging.Contexts;
import fr.openent.crre.logging.Logging;
import fr.openent.crre.model.BasketOrderItem;
import fr.openent.crre.model.FilterItemModel;
import fr.openent.crre.model.item.Item;
import fr.openent.crre.service.ServiceFactory;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.AdminFilter;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ArchiveController extends ControllerHelper {
    private static final Logger log = LoggerFactory.getLogger(ArchiveController.class);
    private final ServiceFactory serviceFactory;

    public ArchiveController(ServiceFactory serviceFactory) {
        this.serviceFactory = serviceFactory;
    }

    @Get("/archive/bookseller/:bookseller/start")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdminFilter.class)
    public void startArchive(final HttpServerRequest request) {
        String bookseller = request.params().get(Field.BOOKSELLER);

        if (bookseller == null) {
            Renders.renderError(request);
            return;
        }

        UserUtils.getUserInfos(eb, request, userInfos -> {
            Future<List<Item>> equimpmentItemFuture =  this.serviceFactory.getEquipmentService().searchFilter(new FilterItemModel().setBooksellers(Arrays.asList(bookseller)), Arrays.asList(Field._ID));

            equimpmentItemFuture.compose(itemList -> this.serviceFactory.getBasketOrderItemService().listBasketOrderItem(null, null, null, itemList.stream().map(Item::getEan).collect(Collectors.toList())))
                    .compose(basketOrderItems -> this.serviceFactory.getBasketOrderItemService().delete(basketOrderItems.stream()
                            .map(BasketOrderItem::getId)
                            .collect(Collectors.toList())))
                    .onSuccess(deletedBasketItem -> {
                        Renders.ok(request);
                        Logging.insert(userInfos, Contexts.BASKET_ITEM.toString(), Actions.DELETE.toString(), bookseller, IModelHelper.toJsonArray(deletedBasketItem));
                    })
                    .onFailure(error -> {
                        log.error(String.format("[CRRE@%s::startArchive] Fail to archive %s:%s",
                                this.getClass().getSimpleName(), error.getClass().getSimpleName(), error.getMessage()));
                        Renders.renderError(request);
                    });
        });
    }
}
