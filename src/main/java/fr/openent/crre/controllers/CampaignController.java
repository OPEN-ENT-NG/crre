package fr.openent.crre.controllers;

import fr.openent.crre.Crre;
import fr.openent.crre.logging.Actions;
import fr.openent.crre.logging.Contexts;
import fr.openent.crre.logging.Logging;
import fr.openent.crre.security.*;
import fr.openent.crre.service.CampaignService;
import fr.openent.crre.service.impl.DefaultCampaignService;
import fr.openent.crre.utils.SqlQueryUtils;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserUtils;

import java.util.List;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;

public class CampaignController extends ControllerHelper {

    private final CampaignService campaignService;

    public CampaignController () {
        super();
        this.campaignService = new DefaultCampaignService(Crre.crreSchema, "campaign");
    }

    @Get("/campaigns")
    @ApiDoc("List all campaigns")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessRight.class)
    @Override
    public void list(final HttpServerRequest  request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if(! (WorkflowActionUtils.hasRight(user, WorkflowActions.ADMINISTRATOR_RIGHT.toString()))){
                String idStructure = request.params().contains("idStructure")
                        ? request.params().get("idStructure")
                        : null;
                campaignService.listCampaigns(idStructure, arrayResponseHandler(request), user);
            }else{
                campaignService.listCampaigns(arrayResponseHandler(request));
            }

        });
    }

    @Get("/campaigns/:id")
    @ApiDoc("Get campaign in database")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void campaign(HttpServerRequest request) {
        try {
            Integer id = Integer.parseInt(request.params().get("id"));
            campaignService.getCampaign(id, defaultResponseHandler(request));
        } catch (ClassCastException e) {
            log.error(" An error occurred when casting campaign id", e);
        }
    }

    @Post("/campaign")
    @ApiDoc("Create a campaign")
    @SecuredAction(value =  "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    @Override
    public void create(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "campaign",
                campaign -> campaignService.create(campaign,
                        Logging.defaultResponseHandler(eb, request, Contexts.CAMPAIGN.toString(), Actions.CREATE.toString(),
                                null, campaign)));
    }

    @Put("/campaign/accessibility/:id")
    @ApiDoc("Update an accessibility campaign")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void updateAccessibility(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "campaign", campaign -> {
            try {
                Integer id = Integer.parseInt(request.params().get("id"));
                campaignService.updateAccessibility(id, campaign, Logging.defaultResponseHandler(eb,
                        request,
                        Contexts.CAMPAIGN.toString(),
                        Actions.UPDATE.toString(),
                        request.params().get("id"),
                        campaign));
            } catch (ClassCastException e) {
                log.error(" An error occurred when casting campaign id", e);
            }
        });
    }

    @Put("/campaign/:id")
    @ApiDoc("Update a campaign")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    @Override
    public void update(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "campaign", campaign -> {
            try {
                Integer id = Integer.parseInt(request.params().get("id"));
                campaignService.update(id, campaign, Logging.defaultResponseHandler(eb,
                        request,
                        Contexts.CAMPAIGN.toString(),
                        Actions.UPDATE.toString(),
                        request.params().get("id"),
                        campaign));
            } catch (ClassCastException e) {
                log.error(" An error occurred when casting campaign id", e);
            }
        });
    }

    @Delete("/campaign")
    @ApiDoc("Delete a campaign")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    @Override
    public void delete(HttpServerRequest request) {
        try{
            List<String> params = request.params().getAll("id");
            if (!params.isEmpty()) {
                List<Integer> ids = SqlQueryUtils.getIntegerIds(params);
                campaignService.delete(ids, Logging.defaultResponsesHandler(eb,
                        request,
                        Contexts.CAMPAIGN.toString(),
                        Actions.DELETE.toString(),
                        params,
                        null));
            } else {
                badRequest(request);
            }
        } catch (ClassCastException e) {
            log.error(" An error occurred when casting campaign(s) id(s)", e);
            badRequest(request);
        }
    }
}
