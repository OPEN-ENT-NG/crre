import {_, model, moment, toasts} from 'entcore';
import {Mix, Selection} from 'entcore-toolkit';
import {
    Campaign,
    Equipment, Equipments,
    Filter, Offer,
    Offers,
    Order,
    OrderRegion,
    OrderUtils,
    Structure,
    Structures,
    Utils
} from './index';
import http from 'axios';

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
    structure_groups: any;
    summary: string;
    image: string;
    status: string;
    user_name: string;
    user_id: string;
    grade: string;
    reassort: boolean;
    offers: Offers;

    constructor() {
        this.typeOrder = "client";
    }

    async updateAmount(amount: number): Promise<void> {
        try {
            await http.put(`/crre/order/${this.id}/amount`, {amount: amount});
            let equipment = new Equipment()
            await equipment.sync(this.equipment_key);
            if (equipment.type === "articlenumerique") {
                this.offers = Utils.computeOffer(this, equipment);
            }
        } catch (e) {
            toasts.warning('crre.order.getMine.err');
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

    async search(text: String, id_campaign: number, start: string, end: string, page?: number) {
        try {
            if ((text.trim() === '' || !text)) return;
            let params = (id_campaign) ? `&id=${id_campaign}` : ``;
            const {startDate, endDate} = Utils.formatDate(start, end);
            const {data} = await http.get(`/crre/orders/search_filter?startDate=${startDate}&endDate=${endDate}&page=${page}&q=${text}${params}`);
            let newOrderClient = Mix.castArrayAs(OrderClient, data);
            if (newOrderClient.length > 0) {
                await this.reformatOrders(newOrderClient);
                return true;
            }
        } catch (err) {
            toasts.warning('crre.basket.sync.err');
            throw err;
        }
    }

    async filter_order(filters: Filter[], id_campaign: number, start: string, end: string, word?: string, page?: number) {
        try {
            let params = (id_campaign) ? `&id=${id_campaign}` : ``;
            filters.forEach(function (f) {
                params += "&" + f.name + "=" + f.value;
            });
            const {startDate, endDate} = Utils.formatDate(start, end);
            if (!Utils.format.test(word)) {
                let url = `/crre/orders/search_filter?startDate=${startDate}&endDate=${endDate}&page=${page}${params}`;
                url += (!!word) ? `&q=${word}` : ``;
                let {data} = await http.get(url);
                let newOrderClient = Mix.castArrayAs(OrderClient, data);
                if (newOrderClient.length > 0) {
                    await this.reformatOrders(newOrderClient);
                    return true;
                }
            } else {
                toasts.warning('crre.equipment.special');
            }
        } catch (e) {
            toasts.warning('crre.equipment.sync.err');
            throw e;
        } finally {
        }
    }

    private async reformatOrders(newOrderClient: any[]) {
        let equipments = new Equipments();
        await equipments.getEquipments(newOrderClient);
        for (let order of newOrderClient) {
            this.reformatOrder(equipments, order);
            order.creation_date = moment(order.creation_date).format('L');
        }
        this.all = this.all.concat(newOrderClient);
    }

    private reformatOrder(equipments, order) {
        let equipment = equipments.all.find(equipment => order.equipment_key == equipment.id);
        if (equipment != undefined) {
            order.priceTotalTTC = Utils.calculatePriceTTC(equipment, 2) * order.amount;
            order.price = Utils.calculatePriceTTC(equipment, 2);
            order.name = equipment.titre;
            order.image = equipment.urlcouverture.replace("cns-edu.org", "www.cns-edu.com");
            if (equipment.type === "articlenumerique") {
                order.offers = Utils.computeOffer(order, equipment);
            }
        } else {
            order.priceTotalTTC = 0.0;
            order.price = 0.0;
            order.name = "Manuel introuvable dans le catalogue";
            order.image = "/crre/public/img/pages-default.png";
        }
    }

    async sync(status: string, start: string, end: string, structures: Structures = new Structures(), idCampaign?: number,
               idStructure?: string, ordersId?, page?: number, old = false): Promise<boolean> {
        try {
            const {startDate, endDate} = Utils.formatDate(start, end);
            if (idCampaign && idStructure && ordersId) {
                return await this.getSpecificOrders(ordersId, old, idCampaign, idStructure, startDate, endDate);
            } else {
                const {data} = await http.get(`/crre/orders?startDate=${startDate}&endDate=${endDate}&page=${page}&status=${status}`);
                let newOrderClient = Mix.castArrayAs(OrderClient, data);
                if (newOrderClient.length > 0) {
                    if (!old) {
                        let equipments = new Equipments();
                        await equipments.getEquipments(newOrderClient);
                        for (let order of newOrderClient) {
                            this.reformatOrder(equipments, order);
                            order.name_structure = (structures && structures.length > 0) ?
                                OrderUtils.initNameStructure(order.id_structure, structures) : '';
                            order.structure = (structures && structures.length > 0) ?
                                OrderUtils.initStructure(order.id_structure, structures) : new Structure();
                            order.structure_groups = Utils.parsePostgreSQLJson(order.structure_groups);
                            order.campaign = Mix.castAs(Campaign, JSON.parse(order.campaign.toString()));
                            order.priceTotalTTC = parseFloat((OrderUtils.calculatePriceTTC(2, order.equipment) as number).toString()) * order.amount;
                            order.creation_date = moment(order.creation_date).format('DD-MM-YYYY');
                        }
                    }
                    this.all = this.all.concat(newOrderClient);
                    return true;
                }
            }
        } catch (e) {
            toasts.warning('crre.order.sync.err');
        }
    }

    private async getSpecificOrders(ordersId, old: boolean, idCampaign: number, idStructure: string, startDate, endDate) {
        let params = '?';
        ordersId.map((order) => {
            params += `order_id=${order}&`;
        });
        let url = `/crre/orders/mine/${idCampaign}/${idStructure}${params}startDate=${startDate}&endDate=${endDate}&old=${old}`;
        const {data} = await http.get(url);
        let newOrderClient = Mix.castArrayAs(OrderClient, data);
        for (let order of newOrderClient) {
            if (order.price !== 0.0) {
                order.price = parseFloat(order.price.toString());
                if (!old) {
                    let equipments = new Equipments();
                    await equipments.getEquipments(newOrderClient);
                    let equipment = equipments.all.find(equipment => order.equipment_key == equipment.id);
                    if (equipment.type === "articlenumerique") {
                        order.offers = Utils.computeOffer(order, equipment);
                    }
                } else {
                    if (order.type === "articlenumerique") {
                        let newOffers = JSON.parse(order.offers);
                        let offers = new Offers();
                        for (let newOffer of newOffers) {
                            let offer = new Offer();
                            offer.name = newOffer.titre;
                            offer.value = newOffer.amount;
                            offer.id = newOffer.id;
                            offers.all.push(offer);
                        }
                        order.offers = offers;
                    }
                }
            }
        }
        this.all = newOrderClient;
        return true;
    }

    async getUsers(status: string): Promise<boolean> {
        const {data} = await http.get(`/crre/orders/users?status=${status}`);
        return data;
    }

    getEquipments(orders): Promise<any> {
        let params = '';
        orders.map((order) => {
            params += `id=${order.equipment_key}&`;
        });
        params = params.slice(0, -1);
        return http.get(`/crre/equipments?${params}`);
    }

    toJson(status: string): any {
        const ids = _.pluck(this.all, 'id');
        return {
            ids,
            status: status,
            dateGeneration: moment(this.dateGeneration).format('DD/MM/YYYY') || null,
            userId: model.me.userId
        };
    }

    async updateStatus(status: string): Promise<any> {
        try {
            let statusURL = status;
            if (status === "IN PROGRESS") {
                statusURL = "inprogress";
            }
            let config = status === 'SENT' ? {responseType: 'arraybuffer'} : {};
            return await http.put(`/crre/orders/${statusURL.toLowerCase()}`, this.toJson(status), config);
        } catch (e) {
            toasts.warning('crre.order.update.err');
            throw e;
        }
    }

    async calculTotal(status: string, start: string, end: string): Promise<any> {
        const {startDate, endDate} = Utils.formatDate(start, end);
        const {data} = await http.get(`/crre/orders/amount?startDate=${startDate}&endDate=${endDate}&status=${status}`);
        return data;
    }

    calculTotalAmount(): number {
        let total = 0;
        this.all.map((order) => {
            if (order.campaign.use_credit == "licences" || order.campaign.use_credit == "consumable_licences") {
                total += order.amount;
            }
        });
        return total;
    }

    calculTotalPriceTTC(): number {
        let total = 0;
        this.all.map((order) => {
            if (order.campaign.use_credit == "credits") {
                total += order.price * order.amount;
            }
        });
        return total;
    }

    exportCSV(old: boolean, idCampaign: string, start: string, end: string, all: boolean, statut?: string) {
        const {startDate, endDate} = Utils.formatDate(start, end);
        let params = ``;
        params += `startDate=${startDate}&endDate=${endDate}&`;
        params += !!idCampaign ? `idCampaign=${idCampaign}&` : ``;
        params += !!statut ? `statut=${statut}&` : ``;
        params += !all ? Utils.formatKeyToParameter(this.all, 'id') : ``;
        const oldString = (old) ? `old/` : ``;
        window.location = `/crre/orders/${oldString}exports?${params}`;
    };
    
}
