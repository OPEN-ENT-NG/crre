export interface IOrderSearchAmount {
    nbItem: number;
    nbLicence: number;
    nbConsumableLicence: number;
    priceCredit: number;
    priceConsumableCredit: number;
}

export class OrderSearchAmount implements IOrderSearchAmount{

    private _nbItem: number;
    private _nbLicence: number;
    private _nbConsumableLicence: number;
    private _priceCredit: number;
    private _priceConsumableCredit: number;

    constructor(data: IOrderSearchAmount) {
        this._nbItem = data.nbItem || 0;
        this._nbLicence = data.nbLicence || 0;
        this._nbConsumableLicence = data.nbConsumableLicence || 0;
        this._priceCredit = data.priceCredit || 0;
        this._priceConsumableCredit = data.priceConsumableCredit || 0;
    }

    get nbConsumableLicence(): number {
        return this._nbConsumableLicence;
    }

    set nbConsumableLicence(value: number) {
        this._nbConsumableLicence = value;
    }

    get nbItem(): number {
        return this._nbItem;
    }

    set nbItem(value: number) {
        this._nbItem = value;
    }

    get nbLicence(): number {
        return this._nbLicence;
    }

    set nbLicence(value: number) {
        this._nbLicence = value;
    }

    get priceConsumableCredit(): number {
        return this._priceConsumableCredit;
    }

    set priceConsumableCredit(value: number) {
        this._priceConsumableCredit = value;
    }

    get priceCredit(): number {
        return this._priceCredit;
    }

    set priceCredit(value: number) {
        this._priceCredit = value;
    }
}