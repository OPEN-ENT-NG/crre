import http from "axios";
import {_, moment, toasts} from "entcore";
import {
    Campaign,
    Order,
    Structure,
    Structures, TechnicalSpec,
    OrderClient,
    Equipment
} from "./index";
import {Mix, Selection} from "entcore-toolkit";


export class OrderRegion implements Order  {
    amount: number;
    campaign: Campaign;
    comment: string;
    creation_date: Date;
    equipment: Equipment;
    equipment_key:number;
    id?: number;
    id_operation:Number;
    id_structure: string;
    inheritedClass:Order|OrderClient|OrderRegion;
    options;
    order_parent?:any;
    price: number;
    price_proposal: number;
    price_single_ttc: number;
    rank: number;
    rankOrder: Number;
    selected:boolean;
    structure: Structure;
    tax_amount: number;
    typeOrder:string;
    contract_name?: string;
    description:string;
    files: string;
    id_campaign:number;
    id_contract:number;
    id_orderClient: number;
    id_project:number;
    id_supplier: string;
    name:string;
    name_structure: string;
    number_validation:string;
    label_program:string;
    order_client: OrderClient;
    order_number?: string;
    preference: number;
    structure_groups: any;
    summary:string;
    image:string;
    status:string;
    technical_spec:TechnicalSpec;
    title_id ?: number;
    user_name:string;
    user_id:string;
    reassort: boolean;
    priceTotalTTC ?: number;

    constructor() {
        this.typeOrder = "region";
    }

    toJson():any {
        return {
            amount: this.amount,
            price: this.price,
            ...(this.id_orderClient && {id_order_client_equipment: this.id_orderClient}),
            creation_date: moment().format('YYYY-MM-DD'),
            status: this.status,
            ...(this.title_id && {title_id: this.title_id}),
            files: this.files,
            name_structure: this.name_structure,
            id_campaign: this.id_campaign,
            id_structure: this.id_structure,
            id_project: this.id_project,
            equipment_key: this.equipment_key,
            comment: (this.comment) ? this.comment : "",
            user_name: this.user_name,
            user_id: this.user_id,
            reassort: this.reassort,
            priceTotalTTC: this.priceTotalTTC
        }
    }

    createFromOrderClient(order: OrderClient):void {
        this.order_client = order;
        this.id_orderClient = order.id;
        this.amount = order.amount;
        this.name = order.name;
        this.summary = order.summary;
        this.description = order.description;
        this.image = order.image;
        this.creation_date = order.creation_date;
        this.status = order.status;
        this.number_validation = order.number_validation;
        this.technical_spec = order.technical_spec;
        this.campaign = order.campaign;
        this.structure_groups = order.structure_groups;
        this.contract_name = order.contract_name;
        this.files = order.files;
        this.name_structure = order.name_structure;
        this.id_contract = order.id_contract;
        this.id_campaign = order.id_campaign;
        this.id_structure = order.id_structure;
        this.id_project = order.id_project;
        this.comment = order.comment;
        this.price = order.price;
        this.rank = order.rank;
        this.structure = order.structure;
        this.id_operation = order.id_operation;
        this.equipment_key = order.equipment_key;
        this.user_name = order.user_name;
        this.user_id = order.user_id;
        this.reassort = order.reassort;
    }

    initDataFromEquipment():void {
        if (this.equipment) {
            this.summary = this.equipment.name;
            this.image = this.equipment.urlcouverture;

        }
    }

    async delete(id:number):Promise<any>{
        try{
            return await http.delete(`/crre/region/${id}/order`);
        } catch (e) {
            toasts.warning('crre.admin.order.update.err');
            throw e;
        }
    }

    async getOneOrderRegion(id:number, structures:Structures):Promise<Order>{
        try{
            const {data} =  await http.get(`/crre/orderRegion/${id}/order`);
            return new Order(Object.assign(data, {typeOrder:"region"}), structures);
        } catch (e) {
            toasts.warning('crre.admin.order.update.err');
            throw e;
        }
    }
}

export class OrdersRegion extends Selection<OrderRegion> {
    constructor() {
        super([]);
    }

    async sync () {
        try {
            let { data } = await http.get(`/crre/orderRegion/orders`);
            this.all = Mix.castArrayAs(OrderRegion, data);
        } catch (e) {
            toasts.warning('crre.basket.sync.err');
        }
    }

    async create():Promise<any> {
        let orders = [];
        this.all.map(order => {
            orders.push(order.toJson());
        });
        try {
            return await http.post(`/crre/region/orders/`, {orders: orders});
        } catch (e) {
            toasts.warning('crre.order.create.err');
            throw e;
        }
    }

    toJson (status: string, justification:string):any {
        const ids = _.pluck(this.all, 'id');
        let orders = [];
        this.all.map(order => {
            orders.push(order.toJson());
        });
        return {
            ids,
            orders: orders,
            status : status,
            justification : justification
        };
    }

    async updateStatus(status: string, justification?:string):Promise<any> {
        try {
            if(!justification)
                justification="";
            return await  http.put(`/crre/region/orders/${status.toLowerCase()}`, this.toJson(status,justification));
        } catch (e) {
            toasts.warning('crre.order.update.error');
            throw e;
        }
    }
}