package fr.openent.crre.model;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.core.enums.OrderClientEquipmentType;
import fr.openent.crre.core.enums.OrderByProjectFieldEnum;
import fr.openent.crre.helpers.JsonHelper;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FilterModel implements IModel<FilterModel> {
    private String startDate;
    private String endDate;
    private UserInfos user;
    private List<Integer> idsCampaign;
    private List<String> idsStructure;
    private Integer page;
    private List<OrderClientEquipmentType> status;
    private String searchingText;
    private List<String> distributors;
    private List<String> editors;
    private List<String> catalogs;
    private List<String> structureTypes;
    private Boolean renew;

    private OrderByProjectFieldEnum orderBy;
    private Boolean orderDesc;

    public FilterModel() {
        this.startDate = null;
        this.endDate = null;
        this.user = null;
        this.idsCampaign = new ArrayList<>();
        this.idsStructure = new ArrayList<>();
        this.page = null;
        this.status = new ArrayList<>();
        this.searchingText = null;
        this.distributors = new ArrayList<>();
        this.editors = new ArrayList<>();
        this.catalogs = new ArrayList<>();
        this.structureTypes = new ArrayList<>();
        this.renew = null;
    }

    public FilterModel(JsonObject jsonObject) {

        this.idsCampaign =  JsonHelper.jsonArrayToList(jsonObject.getJsonArray(Field.IDS_CAMPAIGN, new JsonArray()), Integer.class);
        this.idsStructure = JsonHelper.jsonArrayToList(jsonObject.getJsonArray(Field.IDS_STRUCTURE, new JsonArray()), String.class);
        this.startDate = jsonObject.getString(Field.STARTDATE, null);
        this.endDate = jsonObject.getString(Field.ENDDATE, null);
        this.page = jsonObject.getInteger(Field.PAGE, null);
        this.searchingText = jsonObject.getString(Field.SEARCHING_TEXT, null);
        this.distributors = JsonHelper.jsonArrayToList(jsonObject.getJsonArray(Field.DISTRIBUTORS, new JsonArray()), String.class);
        this.editors = JsonHelper.jsonArrayToList(jsonObject.getJsonArray(Field.EDITORS, new JsonArray()), String.class);
        this.catalogs = JsonHelper.jsonArrayToList(jsonObject.getJsonArray(Field.CATALOGS, new JsonArray()), String.class);
        this.structureTypes = JsonHelper.jsonArrayToList(jsonObject.getJsonArray(Field.STRUCTURE_TYPES, new JsonArray()), String.class);
        this.user = new UserInfos();
        List<String> renewList = JsonHelper.jsonArrayToList(jsonObject.getJsonArray(Field.RENEW, new JsonArray()), String.class);
        this.renew = renewList.contains(Field.TRUE) && !renewList.contains(Field.FALSE) ? Boolean.TRUE :
                renewList.contains(Field.FALSE) && !renewList.contains(Field.TRUE) ? Boolean.FALSE : null;;
        this.status = jsonObject.getJsonArray(Field.STATUS, new JsonArray()).stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(OrderClientEquipmentType::getValue)
                .collect(Collectors.toList());

        this.orderBy = OrderByProjectFieldEnum.getValue(jsonObject.getString(Field.ORDERBY, ""), OrderByProjectFieldEnum.ID);
        this.orderDesc = jsonObject.getBoolean(Field.ORDERDESC, true);
    }

    @Override
    public JsonObject toJson() {
        JsonObject filterModelJson = new JsonObject();

        if (this.idsCampaign != null && !this.idsCampaign.isEmpty()) {
            filterModelJson.put(Field.IDS_CAMPAIGN, new JsonArray(this.idsCampaign));
        }
        if (this.idsStructure != null && !this.idsStructure.isEmpty()) {
            filterModelJson.put(Field.IDS_STRUCTURE, new JsonArray(this.idsStructure));
        }
        if (this.startDate != null) {
            filterModelJson.put(Field.STARTDATE, this.startDate);
        }
        if (this.endDate != null) {
            filterModelJson.put(Field.ENDDATE, this.endDate);
        }
        if (this.page != null) {
            filterModelJson.put(Field.PAGE, this.page);
        }
        if (this.searchingText != null) {
            filterModelJson.put(Field.SEARCHING_TEXT, this.searchingText);
        }
        if (this.distributors != null && !this.distributors.isEmpty()) {
            filterModelJson.put(Field.DISTRIBUTORS, new JsonArray(this.distributors));
        }
        if (this.editors != null && !this.editors.isEmpty()) {
            filterModelJson.put(Field.EDITORS, new JsonArray(this.editors));
        }
        if (this.catalogs != null && !this.catalogs.isEmpty()) {
            filterModelJson.put(Field.CATALOGS, new JsonArray(this.catalogs));
        }
        if (this.structureTypes != null && !this.structureTypes.isEmpty()) {
            filterModelJson.put(Field.STRUCTURE_TYPES, new JsonArray(this.structureTypes));
        }
        if (this.status != null && !this.status.isEmpty()) {
            filterModelJson.put(Field.STATUS, new JsonArray(this.status.stream()
                    .map(OrderClientEquipmentType::toString)
                    .collect(Collectors.toList())));
        }

        return filterModelJson;
    }

    public boolean isOld() {
        return this.status.contains(OrderClientEquipmentType.SENT);
    }


    public List<Integer> getIdsCampaign() {
        return idsCampaign;
    }

    public FilterModel setIdsCampaign(List<Integer> idsCampaign) {
        this.idsCampaign = idsCampaign;
        return this;
    }

    public List<String> getIdsStructure() {
        return idsStructure;
    }

    public FilterModel setIdsStructure(List<String> idsStructure) {
        this.idsStructure = idsStructure;
        return this;
    }

    public List<OrderClientEquipmentType> getStatus() {
        return status;
    }

    public FilterModel setStatus(List<OrderClientEquipmentType> status) {
        this.status = status;
        return this;
    }

    public String getStartDate() {
        return startDate;
    }

    public FilterModel setStartDate(String startDate) {
        this.startDate = startDate;
        return this;
    }

    public String getEndDate() {
        return endDate;
    }

    public FilterModel setEndDate(String endDate) {
        this.endDate = endDate;
        return this;
    }

    public UserInfos getUser() {
        return user;
    }

    public FilterModel setUser(UserInfos user) {
        this.user = user;
        return this;
    }

    public Integer getPage() {
        return page;
    }

    public FilterModel setPage(Integer page) {
        this.page = page;
        return this;
    }

    public String getSearchingText() {
        return searchingText;
    }

    public FilterModel setSearchingText(String searchingText) {
        this.searchingText = searchingText;
        return this;
    }

    public List<String> getDistributors() {
        return distributors;
    }

    public FilterModel setDistributors(List<String> distributors) {
        this.distributors = distributors;
        return this;
    }

    public List<String> getEditors() {
        return editors;
    }

    public FilterModel setEditors(List<String> editors) {
        this.editors = editors;
        return this;
    }

    public List<String> getCatalogs() {
        return catalogs;
    }

    public FilterModel setCatalogs(List<String> catalogs) {
        this.catalogs = catalogs;
        return this;
    }

    public List<String> getStructureTypes() {
        return structureTypes;
    }

    public FilterModel setStructureTypes(List<String> structureTypes) {
        this.structureTypes = structureTypes;
        return this;
    }

    public Boolean getRenew() {
        return renew;
    }

    public FilterModel setRenew(Boolean renew) {
        this.renew = renew;
        return this;
    }

    public OrderByProjectFieldEnum getOrderBy() {
        return orderBy;
    }

    public FilterModel setOrderBy(OrderByProjectFieldEnum orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    public Boolean getOrderDesc() {
        return orderDesc;
    }

    public FilterModel setOrderDesc(Boolean orderDesc) {
        this.orderDesc = orderDesc;
        return this;
    }
}
