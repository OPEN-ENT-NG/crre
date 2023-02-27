package fr.openent.crre.service.impl;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.core.constants.NotifyField;
import fr.openent.crre.core.enums.OrderClientEquipmentType;
import fr.openent.crre.model.*;
import fr.openent.crre.model.neo4j.Neo4jUserModel;
import fr.openent.crre.service.NotificationService;
import fr.openent.crre.service.ServiceFactory;
import fr.wseduc.webutils.I18n;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import java.util.*;
import java.util.stream.Collectors;

public class DefaultNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(DefaultNotificationService.class);

    private final TimelineHelper timelineHelper;
    private final ServiceFactory serviceFactory;
    private final EventBus eventBus;

    public DefaultNotificationService(Vertx vertx, ServiceFactory serviceFactory) {
        this.timelineHelper = new TimelineHelper(vertx, vertx.eventBus(), vertx.getOrCreateContext().config());
        this.serviceFactory = serviceFactory;
        this.eventBus = vertx.eventBus();
    }

    private boolean isBasketOrderIsComplete(Map.Entry<Integer, List<OrderClientEquipmentModel>> basketIdOrderClientEquipmentEntry) {
        return basketIdOrderClientEquipmentEntry.getValue().stream().noneMatch(orderClientEquipmentModel ->
                OrderClientEquipmentType.WAITING.equals(orderClientEquipmentModel.getStatus()) || OrderClientEquipmentType.RESUBMIT.equals(orderClientEquipmentModel.getStatus()));
    }

    private boolean isProjectIsComplete(Map.Entry<ProjectModel, List<OrderRegionEquipmentModel>> projectModelListEntry) {
        return projectModelListEntry.getValue().stream().noneMatch(orderRegionEquipmentModel ->
                OrderClientEquipmentType.WAITING.toString().equals(orderRegionEquipmentModel.getStatus()) ||
                        OrderClientEquipmentType.IN_PROGRESS.toString().equals(orderRegionEquipmentModel.getStatus()) ||
                        OrderClientEquipmentType.RESUBMIT.toString().equals(orderRegionEquipmentModel.getStatus()) ||
                        OrderClientEquipmentType.WAITING_FOR_ACCEPTANCE.toString().equals(orderRegionEquipmentModel.getStatus()));
    }

    @Override
    public void sendNotificationValidator(List<Integer> orderRegionEquipmentList) {
        List<Integer> projectIdList = new ArrayList<>();
        Map<ProjectModel, List<OrderRegionEquipmentModel>> projectModelListMapAll = new HashMap<>();
        Map<ProjectModel, List<OrderRegionEquipmentModel>> projectModelListMap = new HashMap<>();
        this.serviceFactory.getProjectService().getProjectList(orderRegionEquipmentList)
                .compose(projectModels -> {
                    projectIdList.addAll(projectModels.stream().map(ProjectModel::getId).collect(Collectors.toList()));
                    return this.serviceFactory.getOrderRegionService()
                            .getOrderRegionEquipmentInSameProject(projectIdList, false);
                })
                .compose(projectModelListMapResult -> {
                    projectModelListMap.putAll(projectModelListMapResult);
                    projectModelListMap.forEach((projectModel, orderRegionEquipmentModels) -> projectModel.setStructureId(orderRegionEquipmentModels.get(0).getIdStructure()));
                    return this.serviceFactory.getOrderRegionService()
                            .getOrderRegionEquipmentInSameProject(projectIdList, true);
                })
                .compose(projectModelListMapOld -> {
                    projectModelListMapAll.putAll(projectModelListMapOld);
                    projectModelListMapAll.forEach((projectModel, orderRegionEquipmentModels) -> projectModel.setStructureId(orderRegionEquipmentModels.get(0).getIdStructure()));
                    //merging of the 2 maps
                    projectModelListMap.forEach((projectModel, orderRegionEquipmentModels) -> {
                        ProjectModel existingProjectModel = projectModelListMapAll.keySet().stream()
                                .filter(projectModel1 -> projectModel1.getId().equals(projectModel.getId()))
                                .findFirst()
                                .orElse(null);
                        if (existingProjectModel != null) {
                            projectModelListMapAll.get(existingProjectModel).addAll(orderRegionEquipmentModels);
                        } else {
                            projectModelListMapAll.put(projectModel, orderRegionEquipmentModels);
                        }
                    });
                    List<String> structureIdList = projectModelListMapAll.entrySet().stream()
                            .filter(this::isProjectIsComplete)
                            .map(projectModelListEntry -> projectModelListEntry.getKey().getStructureId())
                            .distinct()
                            .collect(Collectors.toList());

                    return this.serviceFactory.getUserService().getValidatorUser(structureIdList);
                })
                .onSuccess(userStructureMap -> {
                    projectModelListMapAll.forEach((projectModel, orderRegionEquipmentModels) -> {
                        userStructureMap.stream()
                                .filter(neo4jUserModel -> neo4jUserModel.getStructureId().equals(projectModel.getStructureId()))
                                .forEach(neo4jUserModel -> this.prepareMessageToValidator(neo4jUserModel.getUserId(), projectModel, orderRegionEquipmentModels));
                    });
                })
                .onFailure(error -> log.error(String.format("[CRRE@%s::sendNotificationToValidator] Fail to send notification to validator %s",
                        this.getClass().getSimpleName(), error.getMessage())));
    }

    @Override
    public void sendNotificationValidatorBasket(Integer basketId) {
        List<BasketOrder> basketMap = new ArrayList<>();
        this.serviceFactory.getBasketOrderService()
                .getBasketOrderList(Collections.singletonList(basketId))
                .compose(basketResult -> {
                    basketMap.addAll(basketResult);
                    return CompositeFuture.all(
                            this.serviceFactory.getUserService().getValidatorUser(Collections.singletonList(basketResult.get(0).getIdStructure())),
                            this.serviceFactory.getCampaignService().getCampaign(basketResult.get(0).getIdCampaign()));
                })
                .onSuccess(compositeResult -> {
                    final Map<String, BasketOrder> userIdBasketMap = ((Map<Neo4jUserModel, String>) compositeResult.resultAt(0)).entrySet().stream()
                            .collect(Collectors.toMap(
                                    neo4jUserModelStringEntry -> neo4jUserModelStringEntry.getKey().getUserId(),
                                    neo4jUserModelStringEntry -> basketMap.stream()
                                            .filter(orderClientListEntry -> neo4jUserModelStringEntry.getValue().equals(orderClientListEntry.getIdStructure()))
                                            .map(basket -> basket.setNameCampaign(((JsonObject) compositeResult.resultAt(1)).getString(Field.NAME)))
                                            .findFirst()
                                            .orElse(new BasketOrder())
                            ));
                    userIdBasketMap.forEach(this::prepareMessageToValidatorBasket);
                })
                .onFailure(error -> log.error(String.format("[CRRE@%s::sendNotificationToValidator] Fail to send notification to validator %s",
                        this.getClass().getSimpleName(), error.getMessage())));
    }

    private void prepareMessageToValidator(String userId, ProjectModel projectModel, List<OrderRegionEquipmentModel> orderRegionEquipmentModels) {
        if (orderRegionEquipmentModels.isEmpty()) {
            return;
        }

        String i18nStatus = this.getI18nStatusToValidator(orderRegionEquipmentModels);
        UserUtils.getUserInfos(this.eventBus, userId, userInfos -> {
            String local = null;
            try {
                local = new JsonObject((String) ((LinkedHashMap<?, ?>) userInfos.getAttribute(Field.PREFERENCES)).get(Field.LANGUAGE)).getString(Field.DEFAULT_DASH_DOMAIN);
            } catch (Exception ignored) {
            }
            String messageStatus = I18n.getInstance().translate(i18nStatus, Field.DEFAULT_DASH_DOMAIN, local);
            this.sendNotification(null, new Notify()
                            .setMessage(messageStatus)
                            .setStructureId(projectModel.getStructureId())
                            .setProjectTitle(projectModel.getTitle()),
                    NotifyField.ORDER_VALIDATOR,
                    Collections.singletonList(userId));
        });
    }

    private void prepareMessageToValidatorBasket(String userId, BasketOrder basketOrder) {
        if (basketOrder == null) {
            return;
        }

        this.sendNotification(null, new Notify()
                        .setUserName(basketOrder.getNameUser())
                        .setCampaignName(basketOrder.getNameCampaign())
                        .setStructureId(basketOrder.getIdStructure())
                        .setBasketName(basketOrder.getName()),
                NotifyField.NEW_BASKET,
                Collections.singletonList(userId));
    }


    private void prepareMessageToPrescriber(BasketOrder basketOrder, List<OrderClientEquipmentModel> orderClientEquipmentModels) {
        if (orderClientEquipmentModels.isEmpty()) {
            return;
        }

        String i18nStatus = this.getI18nStatusToPrescriber(orderClientEquipmentModels);
        UserUtils.getUserInfos(this.eventBus, basketOrder.getIdUser(), userInfos -> {
            String local = null;
            try {
                local = new JsonObject((String) ((LinkedHashMap<?, ?>) userInfos.getAttribute(Field.PREFERENCES)).get(Field.LANGUAGE)).getString(Field.DEFAULT_DASH_DOMAIN);
            } catch (Exception ignored) {
            }
            String messageStatus = I18n.getInstance().translate(i18nStatus, Field.DEFAULT_DASH_DOMAIN, local);
            this.sendNotification(null, new Notify()
                            .setMessage(messageStatus)
                            .setCampaignId(basketOrder.getIdCampaign())
                            .setBasketName(basketOrder.getName()),
                    NotifyField.ORDER_PRESCRIPTOR,
                    Collections.singletonList(basketOrder.getIdUser()));
        });
    }

    @Override
    public void sendNotificationPrescriber(List<Integer> orderClientEquipmentList) {
        this.serviceFactory.getOrderService()
                .getOrderClientEquipmentList(orderClientEquipmentList)
                .onSuccess(this::sendNotificationToPrescriber)
                .onFailure(error -> log.error(String.format("[CRRE@%s::sendNotificationPrescriber] Fail to send notification to validator %s",
                        this.getClass().getSimpleName(), error.getMessage())));
    }

    private void sendNotificationToPrescriber(List<OrderClientEquipmentModel> orderClientEquipmentList) {
        List<Integer> basketIdList = orderClientEquipmentList.stream()
                .map(OrderClientEquipmentModel::getIdBasket)
                .collect(Collectors.toList());

        this.serviceFactory.getOrderService()
                .getOrderClientEquipmentListFromBasketId(basketIdList)
                .onSuccess(orderClientEquipmentModels -> {

                    final Map<Integer, List<OrderClientEquipmentModel>> basketIdCompletedMap = orderClientEquipmentModels.stream()
                            .collect(Collectors.groupingBy(OrderClientEquipmentModel::getIdBasket))
                            .entrySet().stream()
                            .filter(this::isBasketOrderIsComplete)
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                    if (basketIdCompletedMap.isEmpty()) {
                        return;
                    }
                    this.serviceFactory.getBasketOrderService()
                            .getBasketOrderList(new ArrayList<>(basketIdCompletedMap.keySet()))
                            .onSuccess(basketOrderList ->
                                    basketIdCompletedMap.forEach((key, value) ->
                                            basketOrderList.stream()
                                                    .filter(basketOrder1 -> basketOrder1.getId().equals(key))
                                                    .findFirst()
                                                    .ifPresent(basketOrder -> this.prepareMessageToPrescriber(basketOrder, value))))
                            .onFailure(error -> log.error(String.format("[CRRE@%s::sendNotificationToPrescriber] Fail to basket list %s",
                                    this.getClass().getSimpleName(), error.getMessage())));
                })
                .onFailure(error -> log.error(String.format("[CRRE@%s::sendNotificationToPrescriber] Fail to basket list %s",
                        this.getClass().getSimpleName(), error.getMessage())));
    }

    private String getI18nStatusToValidator(List<OrderRegionEquipmentModel> orderRegionEquipmentModels) {
        boolean containsSend = orderRegionEquipmentModels.stream().anyMatch(orderRegionEquipmentModel -> OrderClientEquipmentType.SENT.toString().equals(orderRegionEquipmentModel.getStatus()));
        boolean containsValid = orderRegionEquipmentModels.stream().anyMatch(orderRegionEquipmentModel -> OrderClientEquipmentType.VALID.toString().equals(orderRegionEquipmentModel.getStatus()));
        boolean containsRejected = orderRegionEquipmentModels.stream().anyMatch(orderRegionEquipmentModel -> OrderClientEquipmentType.REJECTED.toString().equals(orderRegionEquipmentModel.getStatus()));

        if (containsSend && !containsRejected && !containsValid) {
            return "crre.timeline.validator.send";
        } else if (containsSend) {
            return "crre.timeline.validator.partially.send";
        } else if (containsValid && !containsRejected) {
            return "crre.timeline.validator.valid";
        } else if (containsValid) {
            return "crre.timeline.validator.partially.valid";
        } else {
            return "crre.timeline.validator.refused";
        }
    }

    private String getI18nStatusToPrescriber(List<OrderClientEquipmentModel> orderClientEquipmentModels) {
        boolean containsInProgress = orderClientEquipmentModels.stream().anyMatch(orderClientEquipmentModel -> OrderClientEquipmentType.IN_PROGRESS.equals(orderClientEquipmentModel.getStatus()));
        boolean containsRejected = orderClientEquipmentModels.stream().anyMatch(orderClientEquipmentModel -> OrderClientEquipmentType.REJECTED.equals(orderClientEquipmentModel.getStatus()));

        if (containsInProgress && !containsRejected) {
            return "crre.timeline.prescriptor.in.progress";
        } else if (containsInProgress) {
            return "crre.timeline.prescriptor.partially.in.progress";
        } else {
            return "crre.timeline.prescriptor.refused";
        }
    }

    private void sendNotification(UserInfos userInfos, Notify notifyData, String notification, List<String> recipientList) {
        JsonObject params = notifyData.toJson();
        if (userInfos != null) {
            params.put(Field.USERID, "/userbook/annuaire#" + userInfos.getUserId())
                    .put(Field.USERNAME, userInfos.getUsername());
        }

        params.put(Field.PUSHNOTIF, new JsonObject().put(Field.TITLE, "push.notif.crre.new.notification").put(Field.BODY, ""))
                .put(Field.DISABLEANTIFLOOD, true);
        this.timelineHelper.notifyTimeline(null, notification, userInfos, recipientList, params);

    }
}
