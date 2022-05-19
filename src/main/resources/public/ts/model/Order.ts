import {
    Campaign,
    Equipment,
    OrderClient,
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
    selected:boolean;
    structure: Structure;
}

export class Order implements OrderImp{
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
    basket_name?:string;
    user_name?:string;

    constructor(order: Order, structures:Structures){
        this.amount  = order.amount? parseInt(order.amount.toString()) : null;
        this.campaign = order.campaign? Mix.castAs(Campaign, JSON.parse(order.campaign.toString())) : null;
        this.comment = order.comment;
        this.equipment_key = order.equipment_key;
        this.inheritedClass = order;
        this.price = order.price? parseFloat(order.price.toString()) : null;
        this.structure = order.id_structure? OrderUtils.initStructure( order.id_structure, structures) : new Structure();
        this.typeOrder = order.typeOrder;
        if(order.id)this.id = order.id;
        if(order.order_parent){
            this.order_parent = order.order_parent;
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

    static calculatePriceTTC( roundNumber?: number, equipment?:Equipment):number|any {
        let price = parseFloat(Utils.calculatePriceTTC(equipment,roundNumber).toString());
        return (!isNaN(price)) ? (roundNumber ? price.toFixed(roundNumber) : price ) : price ;
    }
}