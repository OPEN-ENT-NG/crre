import {Mix, Selectable, Selection} from 'entcore-toolkit';
import {toasts} from 'entcore';
import http from 'axios';
import {Equipment, Filter, Offers, Structure, Utils} from './index';


export class Basket implements Selectable {
    id?: number;
    amount: number;
    processing_date: string| Date;
    equipment: Equipment ;
    id_campaign: number;
    id_structure: string;
    selected: boolean;
    comment?: string;
    basket_name: string;
    reassort:boolean;
    offers: Offers;

    constructor (equipment: Equipment , id_campaign?: number, id_structure?: string ) {
        this.equipment = Mix.castAs(Equipment, equipment) ;
        this.id_campaign = id_campaign;
        this.id_structure = id_structure;
        if(equipment.type === "articlenumerique") {
            this.amount = equipment.offres[0].quantiteminimaleachat;
            this.offers = Utils.computeOffer(this, equipment);
        } else {
            this.amount = 1;
        }
    }

    toJson () {
        return {
            amount: this.amount,
            processing_date : this.processing_date,
            equipment : this.equipment.ean,
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

    async sync (idCampaign: number, idStructure: string ) {
        try {
            let { data } = await http.get(`/crre/basket/${idCampaign}/${idStructure}`);
            this.all = Mix.castArrayAs(Basket, data);
            this.all.map((basket) => {
                basket.equipment = Mix.castAs(Equipment, basket.equipment);
                if(basket.equipment.type === "articlenumerique") {
                    basket.offers = Utils.computeOffer(basket, basket.equipment);
                }
            });
        } catch (e) {
            toasts.warning('crre.basket.sync.err');
        }
    }

    async takeOrder(idCampaign: number, Structure: Structure, basket_name: string) {
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
            toasts.warning('crre.order.create.err');
        }
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
}

export class BasketsOrders extends Selection<BasketOrder> {
    constructor() {
        super([]);
    }

    async search(text: String, id_campaign: number, start: string, end: string, page?:number, old?:boolean) {
        try {
            if ((text.trim() === '' || !text)) return;
            const {startDate, endDate} = Utils.formatDate(start, end);
            const oldString = (old) ? `old/` : ``;
            const pageParams = (page) ? `&page=${page}` : ``;
            let url = `/crre/basketOrder/${oldString}search?startDate=${startDate}&endDate=${endDate}&q=${text}`;
            url += `&id=${id_campaign}${pageParams}`;
            const {data} = await http.get(url);
            this.all = Mix.castArrayAs(BasketOrder, data);
        } catch (err) {
            toasts.warning('crre.basket.sync.err');
            throw err;
        }
    }

    async filter_order(filters: Filter[], id_campaign: number, start: string, end: string, word?: string, page?:number, old?:boolean){
        try {
            let params = "";
            filters.forEach(function (f, index) {
                params += f.name + "=" + f.value;
                if(index != filters.length - 1) {
                    params += "&";
                }});
            const {startDate, endDate} = Utils.formatDate(start, end);
            if(!Utils.format.test(word)) {
                const oldString = (old) ? `old/` : ``;
                const pageParams = (page) ? `&page=${page}` : ``;
                const wordParams = (word) ? `&q=${word}` : ``;
                let url = `/crre/basketOrder/${oldString}filter?startDate=${startDate}&endDate=${endDate}${wordParams}`;
                url += `&id=${id_campaign}&${params}${pageParams}`;
                let {data} = await http.get(url);
                this.all = Mix.castArrayAs(BasketOrder, data);
            } else {
                toasts.warning('crre.equipment.special');
            }
        } catch (e) {
            toasts.warning('crre.equipment.sync.err');
            throw e;
        }
    }

    async getMyOrders (page:number, start: string, end: string, id_campaign: string, old: boolean) {
        try {
            const {startDate, endDate} = Utils.formatDate(start, end);
            const oldString = (old) ? `old/` : ``;
            const pageParams = (page) ? `&page=${page}` : ``;
            let url = `/crre/basketOrder/${oldString}allMyOrders?id=${id_campaign}`;
            url += `&startDate=${startDate}&endDate=${endDate}${pageParams}`;
            let { data } = await http.get(url);
            this.all = Mix.castArrayAs(BasketOrder, data);
        } catch (e){
            toasts.warning('crre.order.getMine.err');
            throw e;
        }
    }
}