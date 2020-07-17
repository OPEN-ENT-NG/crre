import {Mix, Selectable, Selection} from 'entcore-toolkit';
import http from 'axios';
import {moment, notify} from "entcore";
import {Utils} from "./Utils";
import {Instruction} from "./instruction";
import {OrderClient} from "./OrderClient";
import {Structure} from "./Structure";
import {Contract} from "./Contract";
import {Notification} from "./Notification";


export class Operation implements Selectable {
    id?:number;
    id_label:number;
    label : label;
    status:boolean = false;
    Operations : any;
    bc_numbers:Array<any> ;
    programs : Array<any> ;
    contracts: Array<any> ;
    nb_orders: number;
    amount: number;
    selected:boolean;
    id_instruction: number;
    instruction: Instruction;
    date_cp: Date;
    date_operation: Date;
    nbOrberSub: Number;
    number_sub : Number;
    constructor(){

    }
    async save () {
        if (this.id) {
            await this.update();
        } else {
            await this.create();
        }
    }

    async create () {
        try {
            await http.post(`/lystore/operation`, this.toJson());
        } catch (e) {
            notify.error('lystore.operation.create.err');
            throw e;
        }
    }

    async deleteOrders(orders){
        let ordersClientId= [];
        let ordersRegionId= [];

        orders.forEach(order => {
            if(order.typeOrder === "region"){
                ordersRegionId.push(order.id);
                if(order.id_order_client_equipment){
                    ordersClientId.push(order.id_order_client_equipment);
                }
            } else {
                ordersClientId.push(order.id);
            }
        });
        try{
            return await http.put(`lystore/operation/delete/orders`, [{"ordersClientId":ordersClientId,"ordersRegionId":ordersRegionId}])
        }catch(e){
            notify.error('lystore.operation.order.delete.err');
            throw e;
        }
    }
    async update () {
        try {
            await http.put(`/lystore/operation/${this.id}`, this.toJson());
        } catch (e) {
            notify.error('lystore.operation.update.err');
            throw e;
        }
    }

    async getOrders( structures: Structure[] = []) {
        try {
            const {data} = await http.get(`/lystore/operations/${this.id}/orders`);
            let resultData = data;
            if(structures.length > 0){
                resultData = resultData.map(order => {
                    return {
                        ...order,
                        structure : structures.find(structureFilter => structureFilter.id === order.id_structure),
                    }
                })
            }
            return Mix.castArrayAs(OrderClient, resultData);
        } catch (e) {
            notify.error("lystore.operation.orders.sync.err");
            throw e;
        }
    }

    toJson(){
        return {
            id_label : this.id_label,
            status : this.status,
            id_instruction: this.id_instruction,
            date_operation: this.date_operation? Utils.formatDatePost(this.date_operation) : null,
        };
    }

    displayOperation = () => {
        return this.label.title + " - " + (this.date_cp && this.date_cp === null) ? "Pas de date de CP" : this.date_cp.toDateString();
    }

}

export class Operations extends Selection<Operation>{

    filters: Array<string>;

    constructor() {
        super([]);
        this.filters = [];
    }

    async sync() {
        try{
            const queriesFilter = Utils.formatGetParameters({q: this.filters});
            let { data } = await http.get(`/lystore/operations/?${queriesFilter}`);
            this.all = Mix.castArrayAs(Operation, data);
            this.all.map( operation => {
                operation.date_cp = operation.date_cp !== null  && operation.instruction ? moment(operation.instruction.date_cp) : null;
                operation.date_operation = operation.date_operation !== null ? moment(operation.date_operation) : null;
                operation.label.toString() !== 'null' && operation.label !== null ?
                    operation.label = Mix.castAs(label, JSON.parse(operation.label.toString()))
                    : operation.label = new label();
                operation.instruction ?
                    operation.instruction =  JSON.parse(operation.instruction.toString())
                    : operation.instruction = null;
                operation.nb_orders = operation.nb_orders || 0;
                operation.nbOrberSub = operation.number_sub || 0;
            })
        } catch(e){
            notify.error('lystore.operation.sync.err');
            throw e;
        }
    }

    async delete (){
        let operationsIds = this.selected.map(operation => operation.id);
        try{
            await http.delete('/lystore/operations', { data: operationsIds });
        } catch(err){
            notify.error('lystore.operation.delete.err');
        }
    }
    async updateOperations(id_instruction: number, operationIds: Array<number>){
        try {
            await http.put(`/lystore/operations/instructionAttribute/${id_instruction}`, operationIds);
        } catch (e) {
            notify.error('lystore.operation.update.err');
            throw e;
        }
    }
    async updateRemoveOperations(operationIds: Array<number>){
        try {
            await http.put('/lystore/operations/instructionRemove', operationIds);
        } catch (e) {
            notify.error('lystore.operation.update.err');
            throw e;
        }
    }
}

export class label implements Selectable{
    id: number;
    title: string;
    selected: boolean;
}

export class labels extends Selection<label>{

    constructor() {
        super([]);
    }

    async sync() {
        let { data } = await http.get('/lystore/labels');
        this.all = Mix.castArrayAs(label, data);
    }
}