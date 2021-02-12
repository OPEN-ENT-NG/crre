import {
    Campaign,
    Equipment,
    OrderClient,
    OrderOptionClient,
    OrderRegion,
    Structure,
    Structures,
    Utils
} from './index';
import {Mix, Selectable} from "entcore-toolkit";
import {_} from "entcore";

export interface OrderImp extends Selectable{
    amount: number;
    campaign: Campaign;
    comment: string;
    creation_date: Date;
    equipment_key:number;
    id_structure: string;
    price: number;
    price_proposal: number;
    price_single_ttc: number;
    rank: number;
    rankOrder: Number;
    selected:boolean;
    structure: Structure;
    tax_amount: number;
}

export class Order implements OrderImp{
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
    basket_name?:string;
    user_name?:string;

    constructor(order: Order, structures:Structures){
        this.amount  = order.amount? parseInt(order.amount.toString()) : null;
        this.campaign = order.campaign? Mix.castAs(Campaign, JSON.parse(order.campaign.toString())) : null;
        this.comment = order.comment;
        this.equipment_key = order.equipment_key;
        this.inheritedClass = order;
        this.price = order.price? parseFloat(order.price.toString()) : null;
        this.price_proposal = order.price_proposal? parseFloat(order.price_proposal.toString()) : null;
        this.price_single_ttc  = order.price_single_ttc? parseFloat(order.price_single_ttc.toString()) : null;
        this.rank  = order.rank? parseInt(order.rank.toString()) + 1: null;
        this.structure = order.id_structure? OrderUtils.initStructure( order.id_structure, structures) : new Structure();
        this.tax_amount  = order.tax_amount? parseFloat(order.tax_amount.toString()) : null;
        this.typeOrder = order.typeOrder;
        if(order.id)this.id = order.id;
        if(order.id_operation)this.id_operation = order.id_operation;
        if(order.order_parent){
            this.order_parent = order.order_parent;
        }
        if(order.options){
            this.options = order.options.toString() !== '[null]' && order.options !== null ?
                Mix.castArrayAs(OrderOptionClient, JSON.parse(order.options.toString()))  :
                [];
        }
    }
}

export class OrderUtils {
    static initStructure(idStructure:string, structures:Structures):Structure{
        const structure = _.findWhere(structures, { id : idStructure});
        return  structure ? structure : new Structure() ;
    }

    static initNameStructure (idStructure: string, structures: Structures):string {
        let structure = _.findWhere(structures, { id : idStructure});
        return  structure ? structure.uai + '-' + structure.name : '' ;
    }

    static calculatePriceTTC( roundNumber?: number, order?:Order|OrderClient|OrderRegion):number|any {
        let price = parseFloat(Utils.calculatePriceTTC(order.equipment,roundNumber).toString());
        return (!isNaN(price)) ? (roundNumber ? price.toFixed(roundNumber) : price ) : price ;
    }
}