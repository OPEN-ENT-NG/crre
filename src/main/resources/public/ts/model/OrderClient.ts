import {_, model, moment, toasts} from 'entcore';
import {Mix, Selectable, Selection} from 'entcore-toolkit';
import {
    Campaign,
    Equipment, Filter,
    Order,
    OrderRegion,
    OrderUtils,
    Structure,
    Structures,
    TechnicalSpec,
    Utils
} from './index';
import http from 'axios';

export class OrderClient implements Order  {

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
    total?:number;
    action?:string;
    cause_status?:string;
    contract_name?: string;
    description:string;
    files;
    id_campaign:number;
    id_contract:number;
    id_order:number;
    id_project:number;
    id_basket:number;
    id_supplier: string;
    name:string;
    name_structure: string;
    number_validation:string;
    label_program:string;
    order_number?: string;
    preference: number;
    priceTotalTTC: number;
    priceUnitedTTC: number;
    structure_groups: any;
    summary:string;
    image:string;
    status:string;
    technical_spec:TechnicalSpec;
    user_name:string;
    user_id:string;
    grade: string;
    reassort: boolean;

    constructor() {
        this.typeOrder= "client";
    }

    async updateComment():Promise<void>{
        try{
            http.put(`/crre/order/${this.id}/comment`, { comment: this.comment });
        }catch (e){
            toasts.warning('crre.basket.update.err');
            throw e;
        }
    }




    async delete ():Promise<any> {
        try {
            return await http.delete(`/crre/order/${this.id}/${this.id_structure}/${this.id_campaign}`);
        } catch (e) {
            toasts.warning('crre.order.delete.err');
        }
    }

    downloadFile(file):void {
        window.open(`/crre/order/${this.id}/file/${file.id}`);
    }

    async updateAmount(amount: number):Promise<void>{
        try {
            await http.put(`/crre/order/${this.id}/amount`,{ amount: amount });
        }
        catch {
            toasts.warning('crre.order.getMine.err');
        }
    }

    async updateReassort():Promise<void>{
        try {
            await http.put(`/crre/order/${this.id}/reassort`,{ reassort: this.reassort });
        }
        catch {
            toasts.warning('crre.order.getMine.err');
        }
    }

    async updateStatusOrder(status: String, id:number = this.id):Promise<void>{
        try {
            await http.put(`/crre/order/${id}`, {status: status});
        } catch (e) {
            toasts.warning('crre.order.update.err');
        }
    }

    static formatSqlDataToModel(data: any):any {
        return {
            action: data.action,
            amount: data.amount,
            cause_status: data.cause_status,
            comment: data.comment,
            creation_date: moment(data.creation_date).format('DD-MM-YYYY').toString(),
            description: data.description,
            equipment_key: data.equipment_key,
            id: data.id,
            id_campaign: data.id_campaign,
            id_contract: data.id_contract,
            id_operation: data.id_operation,
            id_order: data.id_order,
            id_project: data.id_project,
            id_structure: data.id_structure,
            image: data.image,
            name: data.name,
            number_validation: data.number_validation,
            price: data.price,
            price_proposal: data.price_proposal,
            program: data.program,
            rank: data.rank,
            status: data.status,
            summary: data.summary,
            tax_amount: data.tax_amount
        }
            ;
    }

    async get():Promise<void> {
        try {
            let {data} = await http.get(`/crre/order/${this.id}`);
            Mix.extend(this, OrderClient.formatSqlDataToModel(data));

        } catch (e) {
            toasts.warning('crre.order.get.err');
        }
    }
}
export class OrdersClient extends Selection<OrderClient> {

    dateGeneration?: Date;
    id_project_use?: number;
    filters: Array<string>;

    constructor() {
        super([]);
        this.dateGeneration = new Date();
        this.id_project_use = -1;
        this.filters = [];
    }

    async search(text: String, id_campaign: number) {
        try {
            if ((text.trim() === '' || !text)) return;
            const {data} = await http.get(`/crre/orders/search?q=${text}&id=${id_campaign}`);
            this.all = Mix.castArrayAs(OrderClient, data);
            if(this.all.length>0) {
                await this.getEquipments(this.all).then(equipments => {
                    for (let order of this.all) {
                        let equipment = equipments.data.find(equipment => order.equipment_key == equipment.id);
                        order.priceTotalTTC = (equipment.price * (1 + equipment.tax_amount / 100)) * order.amount;
                        order.name = equipment.name;
                        order.image = equipment.urlcouverture;
                        order.creation_date = moment(order.creation_date).format('L');
                    }
                });
            }
        } catch (err) {
            toasts.warning('crre.basket.sync.err');
            throw err;
        }
    }

    async filter_order(filters: Filter[], id_campaign: number, word?: string){
        try {
            let format = /^[`@#$%^&*()_+\-=\[\]{};:"\\|,.<>\/?~]/;
            let params = "";
            filters.forEach(function (f, index) {
                params += f.name + "=" + f.value;
                if(index != filters.length - 1) {
                    params += "&";
                }});
            let url;
            if(!format.test(word)) {
                if(word) {
                    url = `/crre/orders/filter?q=${word}&id=${id_campaign}&${params}`;
                } else {
                    url = `/crre/orders/filter?id=${id_campaign}&${params}`;
                }
                let {data} = await http.get(url);
                this.all = Mix.castArrayAs(OrderClient, data);
                if(this.all.length>0) {
                    await this.getEquipments(this.all).then(equipments => {
                        for (let order of this.all) {
                            let equipment = equipments.data.find(equipment => order.equipment_key == equipment.id);
                            order.priceTotalTTC = (equipment.price * (1 + equipment.tax_amount / 100)) * order.amount;
                            order.name = equipment.name;
                            order.image = equipment.urlcouverture;
                            order.creation_date = moment(order.creation_date).format('L');
                        }
                    });
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

    async sync (status: string, structures: Structures = new Structures(), idCampaign?: number, idStructure?: string):Promise<void> {
        try {
            this.id_project_use = -1;
            if (idCampaign && idStructure) {
                const { data } = await http.get(  `/crre/orders/mine/${idCampaign}/${idStructure}` );
                this.all = Mix.castArrayAs(OrderClient, data);
                if(this.all.length>0) {
                    await this.getEquipments(this.all).then(equipments => {
                        for (let order of this.all) {
                            let equipment = equipments.data.find(equipment => order.equipment_key == equipment.id);
                            //order.price = equipment.price;
                            order.name = equipment.ark;
                            order.image = equipment.urlcouverture;
                            //order.grade = equipment.grade_name;
                        }
                        //this.syncWithIdsCampaignAndStructure();
                    });
                }
            } else {
                const { data } = await http.get(  `/crre/orders?status=${status}`);
                this.all = Mix.castArrayAs(OrderClient, data);
                if(this.all.length>0) {
                    await this.getEquipments(this.all).then(equipments => {
                        for (let order of this.all) {
                            let equipment = equipments.data.find(equipment => order.equipment_key == equipment.id);
                            //order.priceTotalTTC = (equipment.price * (1 + equipment.tax_amount / 100)) * order.amount;
                            //order.price = equipment.price;
                            order.name = equipment.ark;
                            order.image = equipment.urlcouverture;
                            //order.grade = equipment.grade_name;
                            order.name_structure = structures.length > 0 ? OrderUtils.initNameStructure(order.id_structure, structures) : '';
                            order.structure = structures.length > 0 ? OrderUtils.initStructure(order.id_structure, structures) : new Structure();
                            order.structure_groups = Utils.parsePostgreSQLJson(order.structure_groups);
                            order.files = order.files !== '[null]' ? Utils.parsePostgreSQLJson(order.files) : [];
                            if (order.files.length > 1)
                                order.files.sort(function (a, b) {
                                    return a.filename.localeCompare(b.filename);
                                });
                            if (status !== 'VALID') {
                                this.makeOrderNotValid(order);
                            }
                            order.creation_date = moment(order.creation_date, 'DD/MM/YYYY').format('DD-MM-YYYY');
                        }
                    });
                }
            }
        } catch (e) {
            toasts.warning('crre.order.sync.err');
        }
    }

    getEquipments (orders):Promise<any>{
        let params = '';
        orders.map((order) => {
            params += `order_id=${order.equipment_key}&`;
        });
        params = params.slice(0, -1);
        return http.get(`/crre/equipments?${params}`);
    }

    syncWithIdsCampaignAndStructure():void{
        this.all.map((order) => {
            order.price = parseFloat(order.price.toString());
            order.files = order.files !== '[null]' ? Utils.parsePostgreSQLJson(order.files) : [];
        });
    }

    makeOrderNotValid(order:OrderClient):void{
        order.campaign = Mix.castAs(Campaign,  JSON.parse(order.campaign.toString()));
        order.creation_date = moment(order.creation_date).format('L');
        order.priceUnitedTTC = parseFloat((OrderUtils.calculatePriceTTC(2, order) as number).toString());
        order.priceTotalTTC = this.choosePriceTotal(order);
    }

    choosePriceTotal(order:OrderClient):number{
        return parseFloat((OrderUtils.calculatePriceTTC(2, order) as number).toString()) * order.amount;
    }

    toJson (status: string):any {
        const ids = _.pluck(this.all, 'id');
        return {
            ids,
            status : status,
            dateGeneration: moment(this.dateGeneration).format('DD/MM/YYYY') || null,
            userId : model.me.userId
        };
    }

    async getPreviewData (): Promise<any> {
        try {
            const params = Utils.formatGetParameters(this.toJson('SENT'));
            const { data } = await http.get(`crre/orders/preview?${params}`);
            return data;
        } catch (e) {
            throw e;
        }
    }

    async updateStatus(status: string):Promise<any> {
        try {
            let statusURL = status;
            if (status === "IN PROGRESS") {
                statusURL = "inprogress";
            }
            let config = status === 'SENT' ? {responseType: 'arraybuffer'} : {};
            return await  http.put(`/crre/orders/${statusURL.toLowerCase()}`, this.toJson(status), config);
        } catch (e) {
            toasts.warning('crre.order.update.err');
            throw e;
        }
    }

    calculTotalAmount ():number {
        let total = 0;
        this.all.map((order) => {
            total += order.amount;
        });
        return total;
    }
    calculTotalPriceTTC ():number {
        let total = 0;
        for (let i = 0; i < this.all.length; i++) {
            let order = this.all[i];
            total += this.choosePriceTotal(order);
        }
        return total;
    }
}

export class OrderOptionClient implements Selectable {
    id?: number;
    tax_amount: number;
    price: number;
    name: string;
    amount: number;
    required: boolean;
    id_order_client_equipment: number;
    selected: boolean;
}