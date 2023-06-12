import {_, idiom as lang, model, moment, toasts} from 'entcore';
import {Mix, Selection} from 'entcore-toolkit';
import {
    Basket,
    Baskets,
    Campaign,
    Equipment,
    Equipments,
    IFilter,
    Offer,
    Offers,
    Order,
    OrderRegion, OrderSearchAmountFilter,
    Structure,
    Utils
} from './index';
import http, {AxiosPromise, AxiosResponse} from 'axios';
import {IUserModel, UserModel} from "./UserModel";
import {ValidatorOrderWaitingFilter} from "./ValidatorOrderWaitingFilter";
import {OrderUniversal} from "./OrderUniversal";
import {OrderSearchAmount} from "./OrderSearchAmount";
import {ExportOrderFilter} from "./ExportOrderFilter";
import {TypeCatalogEnum} from "../enum/type-catalog-enum";
import {ORDER_STATUS_ENUM} from "../enum/order-status-enum";

declare let window: any;

export class OrderClient implements Order {

    amount: number;
    campaign: Campaign;
    comment: string;
    creation_date: Date;
    equipment: Equipment;
    equipment_key: number;
    id?: number;
    id_structure: string;
    inheritedClass: Order | OrderClient | OrderRegion;
    order_parent?: any;
    price: number;
    selected: boolean;
    structure: Structure;
    typeOrder: string;
    total?: number;
    action?: string;
    cause_status?: string;
    description: string;
    files;
    id_campaign: number;
    id_order: number;
    id_project: number;
    id_basket: number;
    name: string;
    name_structure: string;
    priceTotalTTC: number;
    summary: string;
    image: string;
    status: string;
    user_name: string;
    user_id: string;
    grade: string;
    reassort: boolean;
    offers: Offers;
    type: string;
    displayStatus: boolean;
    projectTitle: string;
    valid: boolean

    constructor() {
        this.typeOrder = "client";
        this.valid = true;
    }

    async updateAmount(amount: number): Promise<void> {
        try {
            await http.put(`/crre/order/${this.id}/amount`, {amount: amount});
            let equipment: Equipment = new Equipment();
            await equipment.sync(this.equipment_key);
            if (equipment.typeCatalogue == TypeCatalogEnum.NUMERIC) {
                this.offers = Utils.computeOffer(this, equipment);
            }
        } catch (e) {
            toasts.warning('crre.order.update.error');
        }
    }

    async updateReassort(): Promise<void> {
        try {
            await http.put(`/crre/order/${this.id}/reassort`, {reassort: this.reassort});
        } catch (e) {
            toasts.warning('crre.order.getMine.err');
        }
    }
}

export class OrdersClient extends Selection<OrderClient> {

    dateGeneration?: Date;
    filters: Array<string>;

    constructor() {
        super([]);
        this.dateGeneration = new Date();
        this.filters = [];
    }

    async searchOrder(idStructure: string, filter: ValidatorOrderWaitingFilter, replace: boolean, page?: number) {
        let queryParam = "";
        if (filter.queryName != null && filter.queryName.trim() !== "") {
            queryParam = `&q=${filter.queryName}`
        }

        let pageParam = "";
        if (page != null) {
            pageParam = `&page=${page}`
        }

        let dateParam = "";
        if (filter.startDate && filter.endDate) {
            const {startDate, endDate} = Utils.formatDate(filter.startDate, filter.endDate);
            dateParam = `&startDate=${startDate}&endDate=${endDate}`
        }

        let params: string = '';
        filter.filterChoiceCorrelation.forEach((value: string, key: string) => {
            filter[key].forEach((el: IFilter) => params += "&" + value + "=" + el.getValue())
        });

        let response: AxiosResponse;
        if (queryParam != "" || params != "") {
            response = await http.get(`/crre/orders/search_filter?idStructure=${idStructure}${dateParam}${pageParam}${queryParam}${params}`);
        } else {
            response = await http.get(`/crre/orders?idStructure=${idStructure}${dateParam}${pageParam}&status=WAITING`);
        }

        let newOrderClient: Array<OrderClient> = Mix.castArrayAs(OrderClient, response.data);
        await this.reformatOrders(newOrderClient, replace);
        return newOrderClient.length > 0;
    }

    async syncMyOrder(filter: ValidatorOrderWaitingFilter, idCampaign: number,
                      idStructure: string, ordersId: Array<number>): Promise<boolean> {
        let dateParam = "";
        if (filter.startDate && filter.endDate) {
            const {startDate, endDate} = Utils.formatDate(filter.startDate, filter.endDate);
            dateParam = `startDate=${startDate}&endDate=${endDate}&`
        }
        let params: string = '?';
        ordersId.map((order) => {
            params += `basket_id=${order}&`;
        });
        filter.status.forEach((status: ORDER_STATUS_ENUM) => params += `status=${status}&`);
        let url: string = `/crre/orders/mine/${idCampaign}/${idStructure}${params}${dateParam}`;
        const {data} = await http.get(url);
        let newOrderClient: Array<OrderClient> = Mix.castArrayAs(OrderUniversal, data)
            .map((order: OrderUniversal) => {
                let orderMap = new OrderClient();
                orderMap.amount = order.amount;
                orderMap.cause_status = order.cause_status;
                orderMap.comment = order.comment;
                orderMap.creation_date = order.prescriber_validation_date;
                orderMap.equipment_key = Number.parseInt(order.equipment_key);
                orderMap.id = order.order_client_id || order.order_region_id;
                orderMap.id_basket = (!order.basket) ? null : order.basket.id;
                orderMap.id_campaign = (!order.campaign) ? null : order.campaign.id;
                orderMap.id_structure = order.id_structure;
                orderMap.image = order.equipment_image;
                orderMap.name = order.equipment_name;
                orderMap.price = order.equipment_price;
                if (order.equipment_price !== 0.0 && !!order.offers) {
                    orderMap.offers = new Offers();
                    orderMap.offers.all = Mix.castArrayAs(Offer, order.offers).map((newOffer: Offer) => {
                        let offer: Offer = new Offer();
                        offer.name = newOffer.titre;
                        offer.value = parseInt(newOffer.amount);
                        offer.id = newOffer.id;
                        return offer
                    });
                }
                orderMap.reassort = order.reassort;
                orderMap.status = order.status;
                orderMap.projectTitle = (!order.project) ? null : order.project.title;
                orderMap.valid = order.valid;
                return orderMap;
            });

        this.all = newOrderClient;
        return newOrderClient.length > 0;
    }

    private async reformatOrders(newOrderClient: Array<OrderClient>, replace: boolean): Promise<void> {
        let equipments: Equipments = new Equipments();
        await equipments.getEquipments(newOrderClient);
        for (let order of newOrderClient) {
            this.reformatOrder(equipments, order);
            order.campaign = Mix.castAs(Campaign, JSON.parse(order.campaign.toString()));
            order.creation_date = moment(order.creation_date).format('L');
        }
        if (replace) {
            this.all = newOrderClient;
        } else {
            this.all = this.all.concat(newOrderClient);
        }
    }

    private reformatOrder(equipments: Equipments, order: OrderClient) {
        let equipment: Equipment = equipments.all.find(equipment => order.equipment_key.toString() == equipment.id);
        if (equipment != undefined) {
            order.priceTotalTTC = Utils.calculatePriceTTC(equipment, 2) * order.amount;
            order.price = Utils.calculatePriceTTC(equipment, 2);
            order.name = equipment.titre;
            order.valid = equipment.valid;
            order.image = equipment.urlcouverture.replace("cns-edu.org", "www.cns-edu.com");
            if (equipment.typeCatalogue == TypeCatalogEnum.NUMERIC) {
                order.offers = Utils.computeOffer(order, equipment);
            }
        } else {
            order.priceTotalTTC = 0.0;
            order.price = 0.0;
            order.name = "Manuel introuvable dans le catalogue";
            order.image = "/crre/public/img/pages-default.png";
            order.valid = false;
        }
    }

    async getUsers(status: string, idStructure: string): Promise<Array<UserModel>> {
        const {data} = await http.get(`/crre/orders/users?status=${status}&idStructure=${idStructure}`);
        return data.map((element: IUserModel) => new UserModel(element));
    }

    getEquipments(orders): AxiosPromise {
        let params: string = '';
        orders.map((order) => {
            params += `id=${order.equipment_key}&`;
        });
        params = params.slice(0, -1);
        return http.get(`/crre/equipments?${params}`);
    }

    toJson(status: string): {} {
        const ids: Array<number> = _.pluck(this.all, 'id');
        return {
            ids,
            status: status,
            dateGeneration: moment(this.dateGeneration).format('DD/MM/YYYY') || null,
            userId: model.me.userId
        };
    }

    async updateStatus(status: string): Promise<AxiosResponse> {
        try {
            let config: {} = status === 'SENT' ? {responseType: 'arraybuffer'} : {};
            return await http.put(`/crre/orders/${status.toLowerCase()}`, this.toJson(status), config);
        } catch (e) {
            toasts.warning('crre.order.update.err');
            throw e;
        }
    }

    async calculateTotal(structureId: string, filter: OrderSearchAmountFilter): Promise<OrderSearchAmount> {
        return http.post(`/crre/orders/amount/structure/${structureId}`, filter.toJson())
            .then((res: AxiosResponse) => new OrderSearchAmount(res.data));
    }

    calculTotalAmount(): number {
        let total: number = 0;
        this.all.map((order: OrderClient) => {
            if (order.campaign.use_credit == "licences" || order.campaign.use_credit == "consumable_licences") {
                total += order.amount;
            }
        });
        return total;
    }

    calculTotalPriceTTC(consumable: boolean): number {
        let total: number = 0;
        this.all.forEach((order: OrderClient) => {
            if (order.selected) {
                if (!consumable && order.campaign.use_credit == "credits") {
                    total += order.price * order.amount;
                } else if (consumable && order.campaign.use_credit == "consumable_credits") {
                    total += order.price * order.amount;
                }
            }
        });
        return total;
    }

    async exportCSV(campaigns: Campaign[], users: UserModel[], searchingText: string, idStructure: string, start: string, end: string, all: boolean, status: string[]): Promise<void> {
        const {startDate, endDate} = Utils.formatDate(start, end);
        let params: ExportOrderFilter = new ExportOrderFilter();
        params.startDate = startDate;
        params.endDate = endDate;
        if (campaigns && campaigns.length > 0) {
            params.idsCampaign = campaigns.map(campaign => campaign.id);
        }
        if (users && users.length > 0) {
            params.idsUser = users.map(user => user.id_user);
        }
        if (idStructure) {
            params.idsStructure.push(idStructure);
        }
        if (status) {
            params.status = status;
        }

        if(searchingText) {
            params.searchingText = searchingText;
        }
        if (!all) {
            params.idsOrder = this.all.map(order => order.id);
        }

        const response = await http.post(`/crre/orders/exports`, params);
        if (response.status == 200) {
            Utils.generateCSV("order.csv", response.data)
        }
    };


    async resubmitOrderClient(baskets: Baskets, totalAmount: number, structure: Structure): Promise<void> {
        const response: AxiosResponse = await baskets.create();

        let basketsPerCampaign: {} = {};

        if (response.status === 200) {
            response.data.forEach(basketReturn => {
                let basket: Basket = new Basket();
                basket.id = basketReturn.id;
                basket.selected = true;
                const idCampaign: number = basketReturn.idCampaign;
                basketsPerCampaign[idCampaign] == undefined ? basketsPerCampaign[idCampaign] = new Baskets() : null;
                basketsPerCampaign[idCampaign].push(basket);
            })

            const current_date: string = Utils.getCurrentDate();

            let promesses: Array<Promise<AxiosResponse>> = [];
            for (const idCampaign of Object.keys(basketsPerCampaign)) {
                const baskets: Baskets = basketsPerCampaign[idCampaign];
                const panier: string = lang.translate('crre.basket.resubmit') + current_date;
                promesses.push(baskets.takeOrder(idCampaign.toString(), structure, panier));
            }

            const responses: AxiosResponse[] = await Promise.all(promesses);

            if (responses.filter((r: AxiosResponse) => r.status === 200).length === promesses.length) {
                let messageForMany: string = totalAmount + ' ' + lang.translate('articles') +
                    lang.translate('crre.confirm.orders.message');
                toasts.confirm(messageForMany);
            } else {
                toasts.warning('crre.basket.added.articles.error');
            }
        } else {
            toasts.warning('crre.basket.added.articles.error');
        }
    };

}
