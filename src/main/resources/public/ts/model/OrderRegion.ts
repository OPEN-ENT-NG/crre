import http from "axios";
import {_, moment, notify} from "entcore";
import {
    Campaign,
    Contract,
    ContractType, Grade,
    Order,
    Program,
    Structure,
    Structures, Supplier, TechnicalSpec, Title,
    Utils,
    OrderClient,
    Equipment,
    Project,
} from "./index";
import {Selection} from "entcore-toolkit";


export class OrderRegion implements Order  {
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

    contract_name?: string;
    description:string;
    files: string;
    id_campaign:number;
    id_contract:number;
    id_orderClient: number;
    id_project:number;
    id_supplier: string;
    grade?: Grade;
    name:string;
    name_structure: string;
    number_validation:string;
    label_program:string;
    order_client: OrderClient;
    order_number?: string;
    preference: number;
    priceUnitedTTC: number;
    structure_groups: any;
    supplier: Supplier;
    supplier_name?: string;
    summary:string;
    image:string;
    status:string;
    technical_spec:TechnicalSpec;
    title_id ?: number;

    constructor() {
        this.typeOrder = "region";
    }

    toJson():any {
        return {
            amount: this.amount,
            name: this.equipment.name,
            price: this.price,
            summary: this.summary,
            description: (this.description) ? this.description : "",
            ...(this.id_orderClient && {id_order_client_equipment: this.id_orderClient}),
            image: this.image,
            creation_date: moment().format('YYYY-MM-DD'),
            status: this.status,
            ...(this.number_validation && {number_validation: this.number_validation}),
            ...(this.title_id && {title_id: this.title_id}),

            id_contract: this.id_contract,
            files: this.files,
            name_structure: this.name_structure,
            id_campaign: this.id_campaign,
            id_structure: this.id_structure,
            id_project: this.id_project,
            equipment_key: this.equipment.id? this.equipment.id : this.equipment_key,
            comment: (this.comment) ? this.comment : "",
            ...(this.rank && {rank: this.rank}),
            technical_specs: (Utils.parsePostgreSQLJson(this.technical_spec) === null || Utils.parsePostgreSQLJson(this.technical_spec).length === 0) ?
                []:
                Utils.parsePostgreSQLJson(this.technical_spec).map(spec => {
                    return {
                        name: spec.name,
                        value: spec.value
                    }
                }),
            id_operation: this.id_operation,
            rank: this.rank -1,
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
        this.contract = order.contract;
        this.campaign = order.campaign;
        this.structure_groups = order.structure_groups;
        this.contract_name = order.contract_name;
        this.project = order.project;
        this.files = order.files;
        this.contract_type = order.contract_type;
        this.name_structure = order.name_structure;
        this.id_contract = order.id_contract;
        this.id_campaign = order.id_campaign;
        this.id_structure = order.id_structure;
        this.id_project = order.id_project;
        this.comment = order.comment;
        this.price = order.price_single_ttc;
        this.rank = order.rank;
        this.structure = order.structure;
        this.id_operation = order.id_operation;
        this.equipment = order.equipment;
    }


    async create():Promise<any> {
        try {
            return await http.post(`/lystore/region/order`, this.toJson());
        } catch (e) {
            notify.error('lystore.admin.order.update.err');
            throw e;
        }
    }

    async update(id:number):Promise<any>{
        try {
            return await http.put(`/lystore/region/order/${id}`, this.toJson());
        } catch (e) {
            notify.error('lystore.admin.order.update.err');
            throw e;
        }
    }

    initDataFromEquipment():void {
        if (this.equipment) {
            this.summary = this.equipment.name;
            this.image = this.equipment.image;

        }
    }

    async delete(id:number):Promise<any>{
        try{
            return await http.delete(`/lystore/region/${id}/order`);
        } catch (e) {
            notify.error('lystore.admin.order.update.err');
            throw e;
        }
    }

    async getOneOrderRegion(id:number, structures:Structures):Promise<Order>{
        try{
            const {data} =  await http.get(`/lystore/orderRegion/${id}/order`);
            return new Order(Object.assign(data, {typeOrder:"region"}), structures);
        } catch (e) {
            notify.error('lystore.admin.order.update.err');
            throw e;
        }
    }
}

export class OrdersRegion extends Selection<OrderRegion> {
    constructor() {
        super([]);
    }

    async create():Promise<any> {
        let orders = [];
        this.all.map(order => {
            order.initDataFromEquipment();
            orders.push(order.toJson());
        });
        try {
            return await http.post(`/lystore/region/orders/`, {orders: orders});
        } catch (e) {
            notify.error('lystore.order.create.err');
            throw e;
        }
    }
    async updateOperation(idOperation:number, idsRegions: Array<number>):Promise<any>{
        try {
            await http.put(`/lystore/order/region/${idOperation}/operation`, idsRegions);
        } catch (e) {
            notify.error('lystore.admin.order.update.err');
            throw e;
        }
    }
}