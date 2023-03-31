package fr.openent.crre.controllers;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.helpers.IModelHelper;
import fr.openent.crre.logging.Actions;
import fr.openent.crre.logging.Contexts;
import fr.openent.crre.logging.Logging;
import fr.openent.crre.model.Campaign;
import fr.openent.crre.model.StructureGroupModel;
import fr.openent.crre.security.AdministratorRight;
import fr.openent.crre.security.PrescriptorAndStructureRight;
import fr.openent.crre.security.WorkflowActionUtils;
import fr.openent.crre.security.WorkflowActions;
import fr.openent.crre.service.CampaignService;
import fr.openent.crre.service.ServiceFactory;
import fr.openent.crre.utils.SqlQueryUtils;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserUtils;

import java.util.List;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;

public class CampaignController extends ControllerHelper {

    private final CampaignService campaignService;

    public CampaignController (ServiceFactory serviceFactory) {
        super();
        this.campaignService = serviceFactory.getCampaignService();
    }

    @Get("/campaigns")
    @ApiDoc("List all campaigns")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(PrescriptorAndStructureRight.class)
    @Override
    public void list(final HttpServerRequest  request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if(!(WorkflowActionUtils.hasRight(user, WorkflowActions.ADMINISTRATOR_RIGHT.toString()))){
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
            Integer id = Integer.parseInt(request.params().get(Field.ID));
            campaignService.getCampaign(id)
                    .onSuccess(success -> renderJson(request, success))
                    .onFailure(fail -> renderError(request));
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
        RequestUtils.bodyToJson(request, pathPrefix + Field.CAMPAIGN, campaign ->
                UserUtils.getUserInfos(eb, request, userInfos -> {
                    try {
                        campaignService.create(new Campaign(campaign), IModelHelper.toList(campaign.getJsonArray(Field.GROUPS), StructureGroupModel.class))
                                .onSuccess(campaignResult -> {
                                    Renders.renderJson(request, campaignResult.toJson());
                                    Logging.insert(userInfos, Contexts.CAMPAIGN.toString(), Actions.CREATE.toString(), (String) null, campaign);
                                })
                                .onFailure(error -> Renders.renderError(request));
                    } catch (ClassCastException e) {
                        log.error(String.format("[CRRE@%s::create] Fail to create campaign %s", this.getClass().getSimpleName(), e.getMessage()),
                                this.getClass().getSimpleName(), e.toString());
                        Renders.renderError(request);
                    }
                }));
    }

    @Put("/campaign/accessibility/:id")
    @ApiDoc("Update an accessibility campaign")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void updateAccessibility(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "campaign", campaign -> {
            try {
                Integer id = Integer.parseInt(request.params().get(Field.ID));
                campaignService.updateAccessibility(id, campaign, Logging.defaultResponseHandler(eb,
                        request,
                        Contexts.CAMPAIGN.toString(),
                        Actions.UPDATE.toString(),
                        request.params().get(Field.ID),
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
        RequestUtils.bodyToJson(request, pathPrefix + Field.CAMPAIGN, campaign ->
                UserUtils.getUserInfos(eb, request, userInfos -> {
                    try {
                        Integer id = Integer.parseInt(request.params().get(Field.ID));
                        campaignService.update(new Campaign(campaign).setId(id), IModelHelper.toList(campaign.getJsonArray(Field.GROUPS), StructureGroupModel.class))
                                .onSuccess(campaignResult -> {
                                    Renders.renderJson(request, campaignResult.toJson());
                                    Logging.insert(userInfos, Contexts.CAMPAIGN.toString(), Actions.UPDATE.toString(), id.toString(), campaign);
                                })
                                .onFailure(error -> Renders.renderError(request));
                    } catch (ClassCastException e) {
                        log.error(String.format("[CRRE@%s::update] Fail to update campaign %s", this.getClass().getSimpleName(), e.getMessage()),
                                this.getClass().getSimpleName(), e.toString());
                        Renders.renderError(request);
                    }
                }));
    }

    @Delete("/campaign")
    @ApiDoc("Delete a campaign")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    @Override
    public void delete(HttpServerRequest request) {
        try{
            List<String> params = request.params().getAll(Field.ID);
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

    @Get("/campaigns/types")
    @ApiDoc("Get all types of campaign in database")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void campaignTypes(HttpServerRequest request) {
        try {
            campaignService.getCampaignTypes(arrayResponseHandler(request));
        } catch (ClassCastException e) {
            log.error(" An error occurred when getting campaign types", e);
        }
    }
}
