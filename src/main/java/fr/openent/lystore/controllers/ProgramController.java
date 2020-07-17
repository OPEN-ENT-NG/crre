package fr.openent.lystore.controllers;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.security.ManagerRight;
import fr.openent.lystore.service.ProgramService;
import fr.openent.lystore.service.impl.DefaultProgramService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import org.entcore.common.http.filter.ResourceFilter;
import io.vertx.core.http.HttpServerRequest;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.*;

public class ProgramController extends ContractController {

    private ProgramService programService;

    public ProgramController () {
        super();
        this.programService = new DefaultProgramService(Lystore.lystoreSchema, "program");
    }

    @Get("/programs")
    @ApiDoc("List all programs in database")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void listPrograms (HttpServerRequest request) {
        programService.listPrograms(arrayResponseHandler(request));
    }
}
