package fr.openent.lystore.controllers;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.EquipmentTypeService;
import fr.openent.lystore.service.impl.DefaultEquipmentType;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;

public class EquipmentTypeController extends ControllerHelper {
    private final EquipmentTypeService equipmentTypeService;

    public EquipmentTypeController () {
        super();
        this.equipmentTypeService = new DefaultEquipmentType(Lystore.lystoreSchema, "equipment_type");
    }

    @Get("/equipmentType")
    @ApiDoc("List all equipment type in database")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void listEquipmentType (HttpServerRequest request) {
        equipmentTypeService.list(arrayResponseHandler(request));
    }


}
