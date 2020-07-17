import {_, idiom as lang, model, moment, notify} from 'entcore';
import {Mix, Selectable, Selection} from 'entcore-toolkit';
import {
    Campaign,
    Contract,
    ContractType, Equipment,
    Order,
    OrderRegion,
    OrderUtils,
    Program,
    Structure,
    Structures,
    Supplier,
    TechnicalSpec,
    Utils,
    Grade,
    Title,
    Project
} from './index';
import http from 'axios';

export class OrderClient implements Order  {

    amount: number;
    campaign: Campaign;
    comment: string;
    contract: Contract;
    contract_type: ContractType;
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
    program: Program;
    project: Project;
    rank: number;
    rankOrder: Number;
    selected:boolean;
    structure: Structure;
    tax_amount: number;
    title:Title;
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
    id_supplier: string;
    grade?: Grade;
    name:string;
    name_structure: string;
    number_validation:string;
    label_program:string;
    order_number?: string;
    preference: number;
    priceTotalTTC: number;
    priceUnitedTTC: number;
    structure_groups: any;
    supplier: Supplier;
    supplier_name?: string;
    summary:string;
    image:string;
    status:string;
    technical_spec:TechnicalSpec;

    constructor() {
        this.typeOrder= "client";
    }

    async updateComment():Promise<void>{
        try{
            http.put(`/lystore/order/${this.id}/comment`, { comment: this.comment });
        }catch (e){
            notify.error('lystore.basket.update.err');
            throw e;
        }
    }


    async delete ():Promise<any> {
        try {
            return await http.delete(`/lystore/order/${this.id}/${this.id_structure}/${this.id_campaign}`);
        } catch (e) {
            notify.error('lystore.order.delete.err');
        }
    }

    downloadFile(file):void {
        window.open(`/lystore/order/${this.id}/file/${file.id}`);
    }

    async updateStatusOrder(status: String, id:number = this.id):Promise<void>{
        try {
            await http.put(`/lystore/order/${id}`, {status: status});
        } catch (e) {
            notify.error('lystore.order.update.err');
        }
    }

    static formatSqlDataToModel(data: any):any {
        return {
            action: data.action,
            amount: data.amount,
            cause_status: data.cause_status,
            comment: data.comment,
            creation_date: data.creation_date,
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
            let {data} = await http.get(`/lystore/order/${this.id}`);
            Mix.extend(this, OrderClient.formatSqlDataToModel(data));

        } catch (e) {
            notify.error('lystore.order.get.err');
        }
    }

    async getOneOrderClient(id:number, structures:Structures, status:string):Promise<Order>{
        try{
            const {data} = await http.get(`/lystore/orderClient/${id}/order/${status}`);
            return new Order(Object.assign(data, {typeOrder:"client"}), structures);
        } catch (e) {
            notify.error('lystore.admin.order.get.err');
            throw e;
        }
    }

    async exportListLycee(params: string) {
        try {
           await http.get( `/lystore/orders/valid/export/structure_list?${params}`);
        } catch (e) {
            notify.error("lystore.order.get.err")
        }
    }
}
export class OrdersClient extends Selection<OrderClient> {

    supplier: Supplier;
    bc_number?: string;
    id_program?: number;
    engagement_number?: string;
    projects: Selection<Project>;
    dateGeneration?: Date;
    id_project_use?: number;
    filters: Array<string>;
    ordersOfOperation: Array<OrderClient>;

    constructor(supplier?: Supplier) {
        super([]);
        this.supplier = supplier ? supplier : new Supplier();
        this.dateGeneration = new Date();
        this.projects = new Selection<Project>([]);
        this.id_project_use = -1;
        this.filters = [];
    }

    async updateReference(tabIdsProjects: Array<object>, id_campaign:number, id_project:number, id_structure:string):Promise<void> {
        try {
            await  http.put(`/lystore/campaign/${id_campaign}/projects/${id_project}/preferences?structureId=${id_structure}`,
                { preferences: tabIdsProjects });
        }catch (e) {
            notify.error('lystore.project.update.err');
        }
    }


    async sync (status: string, structures: Structures = new Structures(), idCampaign?: number, idStructure?: string):Promise<void> {
        try {
            this.projects = new Selection<Project>([]);
            this.id_project_use = -1;
            if (idCampaign && idStructure) {
                const { data } = await http.get(  `/lystore/orders/${idCampaign}/${idStructure}` );
                this.all = Mix.castArrayAs(OrderClient, data);
                this.syncWithIdsCampaignAndStructure(idCampaign, idStructure);
            } else {
                const { data } = await http.get(  `/lystore/orders?status=${status}`);
                this.all = Mix.castArrayAs(OrderClient, data);
                this.all.map((order: OrderClient) => {
                    order.name_structure =  structures.length > 0 ? OrderUtils.initNameStructure(order.id_structure, structures) : '';
                    order.structure = structures.length > 0 ? OrderUtils.initStructure(order.id_structure, structures) : new Structure();
                    order.price = parseFloat(status === 'VALID' ? order.price.toString().replace(',', '.') : order.price.toString());
                    order.structure_groups = Utils.parsePostgreSQLJson(order.structure_groups);
                    order.files = order.files !== '[null]' ? Utils.parsePostgreSQLJson(order.files) : [];
                    if(order.files.length > 1 )
                        order.files.sort(function (a, b) {
                            return  a.filename.localeCompare(b.filename);
                        });
                    if (status !== 'VALID') {
                        this.makeOrderNotValid(order);
                    }
                });
            }
        } catch (e) {
            notify.error('lystore.order.sync.err');
        }
    }

    syncWithIdsCampaignAndStructure(idCampaign:number, idStructure:string):void{
        this.all.map((order) => {
            order.price = parseFloat(order.price.toString());
            order.price_proposal = order.price_proposal? parseFloat( order.price_proposal.toString()) : null;
            order.tax_amount = parseFloat(order.tax_amount.toString());
            order.project = Mix.castAs(Project, JSON.parse(order.project.toString()));
            order.project.init(idCampaign, idStructure);
            order.project.title = Mix.castAs(Title, JSON.parse(order.title.toString()));
            if(this.id_project_use != order.project.id)this.makeProjects(order);
            order.rank = order.rank ? parseInt(order.rank.toString()) : null ;
            order.options = order.options.toString() !== '[null]' && order.options !== null ?
                Mix.castArrayAs(OrderOptionClient, JSON.parse(order.options.toString()))
                : order.options = [];
            order.options.map((order) => order.selected = true);
            order.files = order.files !== '[null]' ? Utils.parsePostgreSQLJson(order.files) : [];
        });
        this.all = _.sortBy(this.all, (order)=> order.rank != null ? order.rank : this.all.length );
        this.projects.all = _.sortBy(this.projects.all, (project)=> project.preference != null
            ? project.preference
            : this.projects.all.length );
    }

    makeProjects(order:OrderClient, ordersClients:OrdersClient = this):void{
        ordersClients.id_project_use = order.project.id;
        ordersClients.projects.push(order.project);
    }

    makeOrderNotValid(order:OrderClient):void{
        order.tax_amount = parseFloat(order.tax_amount.toString());
        order.contract = Mix.castAs(Contract,  JSON.parse(order.contract.toString()));
        order.contract_type = Mix.castAs(ContractType,  JSON.parse(order.contract_type.toString()));
        order.supplier = Mix.castAs(Supplier,  JSON.parse(order.supplier.toString()));
        order.id_supplier = order.supplier.id;
        order.campaign = Mix.castAs(Campaign,  JSON.parse(order.campaign.toString()));
        order.project = Mix.castAs(Project, JSON.parse(order.project.toString()));
        order.project.title = Mix.castAs(Title, JSON.parse(order.title.toString()));
        order.rank = order.rank ? parseInt(order.rank.toString()) : null ;
        if (this.id_project_use != order.project.id)this.makeProjects(order);
        order.creation_date = moment(order.creation_date).format('L');
        order.options.toString() !== '[null]' && order.options !== null ?
            order.options = Mix.castArrayAs( OrderOptionClient, JSON.parse(order.options.toString()))
            : order.options = [];
        order.priceUnitedTTC = order.price_proposal ?
            parseFloat(( order.price_proposal).toString()):
            parseFloat((OrderUtils.calculatePriceTTC(2, order) as number).toString());
        order.priceTotalTTC = this.choosePriceTotal(order);
        if( order.campaign.orderPriorityEnable()){
            order.rankOrder = order.rank + 1;
        } else if (order.campaign.projectPriorityEnable()){
            order.rankOrder = order.project.preference + 1;
        }else{
            order.rankOrder = lang.translate("lystore.order.not.prioritized");
        }
    }

    choosePriceTotal(order:OrderClient):number{
        return order.price_proposal !== null?
            parseFloat(( order.price_proposal).toString()) * order.amount :
            parseFloat((OrderUtils.calculatePriceTTC(2, order) as number).toString()) * order.amount;
    }

    toJson (status: string):any {
        const ids = status === 'SENT'
            ? _.pluck(this.all, 'number_validation')
            : _.pluck(this.all, 'id');

        const supplierId = status === 'SENT'
            ? _.pluck(this.all, 'supplierid')[0]
            : this.supplier.id;
        return {
            ids,
            status : status,
            bc_number: this.bc_number || null,
            engagement_number: this.engagement_number || null,
            dateGeneration: moment(this.dateGeneration).format('DD/MM/YYYY') || null,
            supplierId,
            userId : model.me.userId,
            id_program: this.id_program || null
        };
    }

    async getPreviewData (): Promise<any> {
        try {
            const params = Utils.formatGetParameters(this.toJson('SENT'));
            const { data } = await http.get(`lystore/orders/preview?${params}`);
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
            return await  http.put(`/lystore/orders/${statusURL.toLowerCase()}`, this.toJson(status), config);
        } catch (e) {
            notify.error('lystore.order.update.err');
            throw e;
        }
    }

    async updateOrderRanks(tabIdsProjects: Array<object>, structureId:string, campaignId:number):Promise<void>{
        try {
            await  http.put(`/lystore/order/rank/move?idStructure=${structureId}&idCampaign=${campaignId}`,{ orders: tabIdsProjects });
        }catch (e) {
            notify.error('lystore.project.update.err');
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

    async cancel (orders: OrderClient[]):Promise<void> {
        try {
            let params = '';
            orders.map((order) => {
                params += `number_validation=${order.number_validation}&`;
            });
            params = params.slice(0, -1);
            await http.delete(`/lystore/orders/valid?${params}`);
        } catch (e) {
            throw e;
        }
    }
    async addOperation (idOperation:number, idsOrder: Array<number>):Promise<void> {
        try{
            await http.put(`/lystore/orders/operation/${idOperation}`, idsOrder);
        }catch (e){
            notify.error('lystore.basket.update.err');
            throw e;
        }
    }
    async addOperationInProgress (idOperation:number, idsOrder: Array<number>):Promise<void> {
        try{
            await http.put(`/lystore/orders/operation/in-progress/${idOperation}`, idsOrder);
        }catch (e){
            notify.error('lystore.basket.update.err');
            throw e;
        }
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