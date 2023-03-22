package fr.openent.crre.model;

import fr.openent.crre.core.constants.Field;
import io.vertx.core.json.JsonObject;

public class OrderRegionComplex implements IModel<OrderRegionComplex>, Cloneable {
    private OrderRegionEquipmentModel orderRegion;
    private Campaign campaign;
    private ProjectModel project;
    private OrderClientEquipmentModel orderClient;
    private BasketOrder basketOrder;
    private StudentsTableModel students;

    public OrderRegionComplex() {
    }

    public OrderRegionComplex(JsonObject jsonObject) {
        this.orderRegion = new OrderRegionEquipmentModel(jsonObject.getJsonObject(Field.ORDER_REGION));
        this.campaign = new Campaign(jsonObject.getJsonObject(Field.CAMPAIGN));
        this.project = new ProjectModel(jsonObject.getJsonObject(Field.PROJECT));
        this.orderClient = new OrderClientEquipmentModel(jsonObject.getJsonObject(Field.ORDER_CLIENT));
        this.basketOrder = new BasketOrder(jsonObject.getJsonObject(Field.BASKET_ORDER));
        this.students = new StudentsTableModel(jsonObject.getJsonObject(Field.STUDENTS));
    }

    @Override
    public JsonObject toJson() {
        return null;
    }

    public OrderRegionEquipmentModel getOrderRegion() {
        return orderRegion;
    }

    public OrderRegionComplex setOrderRegion(OrderRegionEquipmentModel orderRegion) {
        this.orderRegion = orderRegion;
        return this;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public OrderRegionComplex setCampaign(Campaign campaign) {
        this.campaign = campaign;
        return this;
    }

    public ProjectModel getProject() {
        return project;
    }

    public OrderRegionComplex setProject(ProjectModel project) {
        this.project = project;
        return this;
    }

    public BasketOrder getBasketOrder() {
        return basketOrder;
    }

    public OrderRegionComplex setBasketOrder(BasketOrder basketOrder) {
        this.basketOrder = basketOrder;
        return this;
    }

    public StudentsTableModel getStudents() {
        return students;
    }

    public OrderRegionComplex setStudents(StudentsTableModel students) {
        this.students = students;
        return this;
    }

    public OrderClientEquipmentModel getOrderClient() {
        return orderClient;
    }

    public OrderRegionComplex setOrderClient(OrderClientEquipmentModel orderClient) {
        this.orderClient = orderClient;
        return this;
    }

    @Override
    public OrderRegionComplex clone() {
        OrderRegionComplex clone = new OrderRegionComplex();
        if (this.orderRegion != null) clone.setOrderRegion(this.orderRegion.clone());
        if (this.campaign != null) clone.setCampaign(this.campaign.clone());
        if (this.basketOrder != null) clone.setBasketOrder(this.basketOrder.clone());
        if (this.project != null) clone.setProject(this.project.clone());
        if (this.students != null) clone.setStudents(this.students.clone());
        if (this.orderClient != null) clone.setOrderClient(this.orderClient.clone());
        return clone;
    }
}
