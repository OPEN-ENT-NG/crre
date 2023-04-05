export interface IOrderSearchAmount {
    licence: number;
    consumable_licence: number;
    credit: number;
    consumable_credit: number;
    total: number;
    total_filtered: number;
    total_filtered_consumable: number;
}

export class OrderSearchAmount implements IOrderSearchAmount{
    private _licence: number;
    private _consumable_licence: number;
    private _credit: number;
    private _consumable_credit: number;
    private _total: number;
    private _total_filtered: number;
    private _total_filtered_consumable: number;


    constructor(data: IOrderSearchAmount) {
        this._licence = data.licence;
        this._consumable_licence = data.consumable_licence;
        this._credit = data.credit;
        this._consumable_credit = data.consumable_credit;
        this._total = data.total;
        this._total_filtered = data.total_filtered;
        this._total_filtered_consumable = data.total_filtered_consumable;
    }

    get licence(): number {
        return this._licence;
    }

    set licence(value: number) {
        this._licence = value;
    }

    get consumable_licence(): number {
        return this._consumable_licence;
    }

    set consumable_licence(value: number) {
        this._consumable_licence = value;
    }

    get credit(): number {
        return this._credit;
    }

    set credit(value: number) {
        this._credit = value;
    }

    get consumable_credit(): number {
        return this._consumable_credit;
    }

    set consumable_credit(value: number) {
        this._consumable_credit = value;
    }

    get total(): number {
        return this._total;
    }

    set total(value: number) {
        this._total = value;
    }

    get total_filtered(): number {
        return this._total_filtered;
    }

    set total_filtered(value: number) {
        this._total_filtered = value;
    }

    get total_filtered_consumable(): number {
        return this._total_filtered_consumable;
    }

    set total_filtered_consumable(value: number) {
        this._total_filtered_consumable = value;
    }
}