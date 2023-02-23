package fr.openent.crre.service.impl;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.core.enums.OrderClientEquipmentType;
import fr.openent.crre.model.BasketOrder;
import fr.openent.crre.model.OrderClientEquipmentModel;
import fr.openent.crre.model.OrderRegionEquipmentModel;
import fr.openent.crre.model.ProjectModel;
import fr.openent.crre.service.NotificationService;
import fr.openent.crre.service.ServiceFactory;
import fr.wseduc.webutils.I18n;
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
                        if (projectModelListMapAll.keySet().stream().anyMatch(projectModel1 -> projectModel1.getStructureId().equals(projectModel.getStructureId()))){
                            projectModelListMapAll.get(projectModelListMapAll.keySet().stream().filter(projectModel1 -> projectModel1.getStructureId().equals(projectModel.getStructureId())).findFirst().orElse(null)).addAll(orderRegionEquipmentModels);
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
                    final Map<String, List<OrderRegionEquipmentModel>> userIdOrderRegionMap = userStructureMap.entrySet().stream()
                            .collect(Collectors.toMap(neo4jUserModelStringEntry -> neo4jUserModelStringEntry.getKey().getUserId(),
                                    neo4jUserModelStringEntry -> projectModelListMapAll.entrySet().stream()
                                            .filter(this::isProjectIsComplete)
                                            .filter(projectModelListEntry -> neo4jUserModelStringEntry.getValue().equals(projectModelListEntry.getKey().getStructureId()))
                                            .findFirst()
                                            .map(Map.Entry::getValue)
                                            .orElse(new ArrayList<>())));

                    userIdOrderRegionMap.forEach(this::prepareMessageToValidator);
                })
                .onFailure(error -> log.error(String.format("[CRRE@%s::sendNotificationToValidator] Fail to send notification to validator %s",
                        this.getClass().getSimpleName(), error.getMessage())));
    }

    private void prepareMessageToValidator(String userId, List<OrderRegionEquipmentModel> orderRegionEquipmentModels) {
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
            this.sendNotification(null, messageStatus, Collections.singletonList(userId));
        });
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
            String messageStatus = I18n.getInstance().translate(i18nStatus, Field.DEFAULT_DASH_DOMAIN, local).replace("{0}", basketOrder.getName());
            this.sendNotification(null, messageStatus, Collections.singletonList(basketOrder.getIdUser()));
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

    private void sendNotification(UserInfos userInfos, String message, List<String> recipientList) {
        JsonObject params = new JsonObject();
        if (userInfos != null) {
            params.put(Field.USERID, "/userbook/annuaire#" + userInfos.getUserId())
                    .put(Field.USERNAME, userInfos.getUsername());
        }
        params.put(Field.PUSHNOTIF, new JsonObject().put(Field.TITLE, "push.notif.crre.new.notification").put(Field.BODY, ""))
                .put(Field.MESSAGE, message)
                .put(Field.DISABLEANTIFLOOD, true);
        this.timelineHelper.notifyTimeline(null, "crre.new_notification", userInfos, recipientList, params);

    }
}
