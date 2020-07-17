package fr.openent.lystore.controllers;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.security.ManagerRight;
import fr.openent.lystore.service.ContractTypeService;
import fr.openent.lystore.service.impl.DefaultContractTypeService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import org.entcore.common.http.filter.ResourceFilter;
import io.vertx.core.http.HttpServerRequest;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.*;

public class ContractTypeController extends ContractController {

    private ContractTypeService contractTypeService;

    public ContractTypeController() {
        super();
        this.contractTypeService = new DefaultContractTypeService(Lystore.lystoreSchema, "contract_type");
    }

    @Get("/contract/types")
    @ApiDoc("List all market types in database")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void listMarketTypes (HttpServerRequest request) {
        contractTypeService.listContractTypes(arrayResponseHandler(request));
    }
}
