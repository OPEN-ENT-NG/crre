package fr.openent.crre.controllers;

import fr.openent.crre.Crre;
import fr.openent.crre.security.ManagerRight;
import fr.openent.crre.service.ContractTypeService;
import fr.openent.crre.service.impl.DefaultContractTypeService;
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
        this.contractTypeService = new DefaultContractTypeService(Crre.crreSchema, "contract_type");
    }

    @Get("/contract/types")
    @ApiDoc("List all market types in database")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void listMarketTypes (HttpServerRequest request) {
        contractTypeService.listContractTypes(arrayResponseHandler(request));
    }
}
