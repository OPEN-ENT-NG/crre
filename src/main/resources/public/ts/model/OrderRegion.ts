import http from "axios";
import {_, moment, toasts} from "entcore";
import {Campaign, Equipment, IProjectPayload, Order, OrderClient, Projects, Structure} from "./index";
import {Mix, Selection} from "entcore-toolkit";
import {ProjectFilter} from "./ProjectFilter";
import {ORDER_STATUS_ENUM} from "../enum/order-status-enum";

export class OrderRegion implements Order  {
    amount: number;
    campaign: Campaign;
    comment: string;
    creation_date: Date;
    equipment: Equipment;
    equipment_key:number;
    id?: number;
    id_structure: string;
    inheritedClass:Order|OrderClient|OrderRegion;
    order_parent?:any;
    price: number;
    selected:boolean;
    structure: Structure;
    typeOrder:string;
    description:string;
    id_campaign:number;
    id_orderClient: number;
    id_project:number;
    name:string;
    name_structure: string;
    order_client: OrderClient;
    summary:string;
    image:string;
    status:string;
    title_id ?: number;
    user_name:string;
    user_id:string;
    reassort: boolean;
    priceTotalTTC ?: number;
    use_credit: string;
    displayStatus: boolean;
    cause_status: string;
    campaign_name: string;
    id_order_client_equipment: number;

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
            name_structure: this.name_structure,
            id_campaign: this.id_campaign,
            id_structure: this.id_structure,
            id_project: this.id_project,
            equipment_key: this.equipment_key,
            comment: (this.comment) ? this.comment : "",
            user_name: this.user_name,
            user_id: this.user_id,
            reassort: this.reassort,
            priceTotalTTC: this.priceTotalTTC,
            use_credit: this.use_credit
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
        this.campaign = order.campaign;
        this.name_structure = order.name_structure;
        this.id_campaign = order.id_campaign;
        this.id_structure = order.id_structure;
        this.id_project = order.id_project;
        this.comment = order.comment;
        this.price = order.price;
        this.structure = order.structure;
        this.equipment_key = order.equipment_key;
        this.user_name = order.user_name;
        this.user_id = order.user_id;
        this.reassort = order.reassort;
        this.use_credit = order.campaign.use_credit;
    }
}

export class OrdersRegion extends Selection<OrderRegion> {
    constructor() {
        super([]);
    }

    async create(comment: string):Promise<any> {
        let orders = [];
        let singleOrders = [];
        for(let i = 0; i < Math.min(10000, this.all.length); i++){
            let order = this.all[i];
            if(singleOrders.indexOf(order.id_orderClient) == -1) {
                orders.push(order.toJson());
                singleOrders.push(order.id_orderClient)
            }
        }
        try {
            let data = await http.post(`/crre/region/orders`, {orders: orders, comment: comment});
            if(this.all.length > 10000 && data.status === 201){
                const idProject = data.data.idProject;
                let e = 1
                while ( e * 10000 < this.all.length) {
                    orders = [];
                    for(let i = e * 10000; i < Math.min((e + 1) * 10000, this.all.length); i++){
                        let order = this.all[i];
                        if(singleOrders.indexOf(order.id_orderClient) == -1) {
                            orders.push(order.toJson());
                            singleOrders.push(order.id_orderClient)
                        }
                    }
                    data = await http.post(`/crre/region/orders/${idProject}`, {orders: orders});
                    e++;
                }
                return data;
            } else {
                return data;
            }
        } catch (e) {
            toasts.warning('crre.order.create.err');
            throw e;
        }
    }

    toJson (status: string, justification:string):any {
        const ids = _.pluck(this.all, 'id');
        let orders = [];
        this.forEach(order => {
            orders.push(Mix.castAs(OrderRegion, order).toJson());
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

    async generateLibraryOrder():Promise<any> {
        try {
            const filteredOrder : OrderRegion[] = this.all
                .filter((order:OrderRegion) => order.status != ORDER_STATUS_ENUM.SENT);
            let params_id_equipment = new Set();
            let params_id_structure = new Set();
            let params_id_order = filteredOrder.map((order:OrderRegion) => order.id);
            filteredOrder.forEach((order:OrderRegion) => params_id_equipment.add(order.equipment_key));
            filteredOrder.forEach((order:OrderRegion) => params_id_structure.add(order.id_structure));

            let data = {
                idsStructures: params_id_structure,
                idsEquipments: params_id_equipment,
                idsOrders: params_id_order,
            };
            return await  http.post(`/crre/region/orders/library`, data);
        } catch (e) {
            toasts.warning('crre.order.update.error');
            throw e;
        }
    }

    async getOrdersFromProjects(projects: Projects, filters: ProjectFilter): Promise<any> {

        return http.post(`/crre/ordersRegion/orders`, this.toJSONOrders(filters, projects));
    }

    toJSONOrders(filter: ProjectFilter, projects: Projects): IProjectPayload {

        let payload: IProjectPayload = {
            startDate: moment(filter.startDate).format('YYYY-MM-DD').toString(),
            endDate: moment(filter.endDate).format('YYYY-MM-DD').toString(),
            idsProject: projects.all.map(project => project.id)
        };

        if (!!filter.queryName) payload.searchingText = filter.queryName;
        if (filter.statusFilterList.length > 0) payload.status = filter.statusFilterList.map(status => ORDER_STATUS_ENUM[status.orderStatusEnum]);
        if (filter.campaignList.length > 0) payload.idsCampaign = filter.campaignList.map(campaign => campaign.id);
        if (filter.distributorList.length > 0) payload.distributors = filter.distributorList.map(distributor => distributor.name);
        if (filter.editorList.length > 0) payload.editors = filter.editorList.map(editor => editor.name);
        if (filter.catalogList.length > 0) payload.catalogs = filter.catalogList.map(catalog => catalog.name);

        return payload;
    }
}