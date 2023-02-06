import {Mix, Selectable, Selection} from 'entcore-toolkit';
import {moment, toasts} from 'entcore';
import http from 'axios';
import {Equipment, Offers, OrdersClient, Structure, Utils} from './index';


export class Basket implements Selectable {
    id?: number;
    amount: number;
    processing_date: string| Date;
    equipment: Equipment;
    id_campaign: number;
    id_structure: string;
    selected: boolean;
    comment?: string;
    basket_name: string;
    reassort:boolean;
    offers: Offers;

    constructor (equipment?: Equipment , id_campaign?: number, id_structure?: string ) {
        if(equipment) {
            this.equipment = Mix.castAs(Equipment, equipment);
            if(equipment.type === "articlenumerique") {
                this.amount = equipment.offres.length > 0 ? equipment.offres[0].quantiteminimaleachat : 0;
                this.offers = Utils.computeOffer(this, equipment);
            } else {
                this.amount = 1;
            }
        }
        this.id_campaign = id_campaign;
        this.id_structure = id_structure;
    }

    toJson () {
        return {
            amount: this.amount,
            processing_date : this.processing_date,
            id_item : this.equipment.ean,
            id_campaign : this.id_campaign,
            id_structure : this.id_structure,
        };
    }

    async create () {
        try {
            return await  http.post(`/crre/basket/campaign/${this.id_campaign}`, this.toJson());
        } catch (e) {
            toasts.warning('crre.basket.create.err');
        }
    }

    async updateAmount () {
        try {
            if(this.amount) {
                http.put(`/crre/basket/${this.id}/amount`, this.toJson());
                if (this.equipment.type === "articlenumerique") {
                    this.offers = Utils.computeOffer(this, this.equipment);
                }
            }
        } catch (e) {
            toasts.warning('crre.basket.update.err');
            throw e;
        }
    }

    async updateComment(){
        try{
            this.comment.replace(/\n|\r|\R|;/g, ".");
            http.put(`/crre/basket/${this.id}/comment`, { comment: this.comment });
        }catch (e){
            toasts.warning('crre.basket.update.err');
            throw e;
        }
    }

    async updateReassort(){
        try{
            http.put(`/crre/basket/${this.id}/reassort`, { reassort: this.reassort });
        }catch (e){
            toasts.warning('crre.basket.update.err');
            throw e;
        }
    }

    async delete () {
        try {
            return await  http.delete(`/crre/basket/${this.id}/campaign/${this.id_campaign}`);
        } catch (e) {
            toasts.warning('crre.basket.delete.err');
        }
    }

    amountIncrease = () => {
        this.amount += 1;
        this.updateAmount ();
    };

    amountDecrease = () => {
        if(this.amount >= 1){
            this.amount -= 1;
            this.updateAmount();
        }
    };

}

export class Baskets extends Selection<Basket> {
    constructor() {
        super([]);
    }

    async sync (idCampaign: number, idStructure: string, reassort: boolean ) {
        try {
            let { data } = await http.get(`/crre/basket/${idCampaign}/${idStructure}`);
            this.all = Mix.castArrayAs(Basket, data);
            this.all.map((basket) => {
                if (reassort != undefined) {
                    basket.reassort = reassort;
                    basket.updateReassort();
                }
                basket.equipment = Mix.castAs(Equipment, basket.equipment);
                if(basket.equipment.type === "articlenumerique") {
                    basket.offers = Utils.computeOffer(basket, basket.equipment);
                }
            });
        } catch (e) {
            toasts.warning('crre.basket.sync.err');
        }
    }

    async takeOrder(idCampaign: string, Structure: Structure, basket_name: string) {
        try {
            let baskets = [];
            let newlistBaskets = new Selection<Basket>([]);
            this.all.map((basket: Basket) => {
                    if (basket.selected) {
                        baskets.push(basket.id);
                    } else {
                        newlistBaskets.push(basket);
                    }
                }
            );
            this.all = newlistBaskets.all;

            let data = {
                id_structure: Structure.id,
                structure_name: Structure.name,
                baskets: baskets,
                basket_name: basket_name
            };

            return await http.post(`/crre/baskets/to/orders/${idCampaign}`, data);
        } catch (e) {
            toasts.warning('crre.basket.create.err');
        }
    }

    async create () {
        let baskets = [];
        this.all.map((basket: Basket) => {
            let basketToCreate = {
                amount: basket.amount,
                processing_date : basket.processing_date,
                equipment : basket.equipment.ean,
                id_campaign : basket.id_campaign,
                id_structure : basket.id_structure,
            };
            baskets.push(basketToCreate);
        });
        return http.post(`/crre/baskets/campaign`, baskets);
    }
}

export class BasketOrder implements Selectable {
    id?: number;
    name: string;
    id_structure: string;
    id_campaign: number;
    name_user: string;
    id_user: string;
    total: number;
    amount: number;
    created: string| Date;
    selected: boolean;
    orders: OrdersClient;
    status: string;
}

export class BasketsOrders extends Selection<BasketOrder> {
    constructor() {
        super([]);
    }

    async search(text: String, id_campaign: number, start: string, end: string, page?:number, old?:boolean) {
        try {
            if ((text.trim() === '' || !text)) return;
            const {startDate, endDate} = Utils.formatDate(start, end);
            const pageParams = (page) ? `&page=${page}` : ``;
            let url = `/crre/basketOrder/search?startDate=${startDate}&endDate=${endDate}&old=${old}&q=${text}`;
            url += `&idCampaign=${id_campaign}${pageParams}`;
            const {data} = await http.get(url);
            return this.setBaskets(data);
        } catch (err) {
            toasts.warning('crre.basket.sync.err');
            throw err;
        }
    }

    async getMyOrders (page:number, start: string, end: string, id_campaign: string, old: boolean) {
        try {
            const {startDate, endDate} = Utils.formatDate(start, end);
            const pageParams = (page) ? `&page=${page}` : ``;
            let url = `/crre/basketOrder/allMyOrders?idCampaign=${id_campaign}&old=${old}`;
            url += `&startDate=${startDate}&endDate=${endDate}${pageParams}`;
            let { data } = await http.get(url);
            return this.setBaskets(data);
        } catch (e){
            toasts.warning('crre.order.getMine.error');
            throw e;
        }
    }

    private setBaskets(data) {
        const currencyFormatter = new Intl.NumberFormat('fr-FR', {style: 'currency', currency: 'EUR'});
        data.map(basket => {
            basket.name_user = basket.name_user.toUpperCase();
            basket.total = currencyFormatter.format(basket.total);
            basket.created = moment(basket.created).format('DD-MM-YYYY').toString();
            basket.expanded = false;
            basket.orders = new OrdersClient();
        });
        this.all = this.all.concat(Mix.castArrayAs(BasketOrder, data));
        return data;
    }
}