import {ORDER_STATUS_ENUM} from "../enum/order-status-enum";

export interface IOrderSearchAmountFilter {
    idsUser: Array<string>,
    idsCampaign: Array<number>,
    status: Array<ORDER_STATUS_ENUM>,
    searchingText: string,
    startDate: string,
    endDate: string,
}

export class OrderSearchAmountFilter implements IOrderSearchAmountFilter {
    private _idsCampaign: Array<number>;
    private _idsUser: Array<string>;
    private _searchingText: string;
    private _startDate: string;
    private _endDate: string;


    private _status: Array<ORDER_STATUS_ENUM>;

    get idsCampaign(): Array<number> {
        return this._idsCampaign;
    }

    set idsCampaign(value: Array<number>) {
        this._idsCampaign = value;
    }

    get idsUser(): Array<string> {
        return this._idsUser;
    }

    set idsUser(value: Array<string>) {
        this._idsUser = value;
    }

    get status(): Array<ORDER_STATUS_ENUM> {
        return this._status;
    }

    set status(value: Array<ORDER_STATUS_ENUM>) {
        this._status = value;
    }

    get searchingText(): string {
        return this._searchingText;
    }

    set searchingText(value: string) {
        this._searchingText = value;
    }

    get startDate(): string {
        return this._startDate;
    }

    set startDate(value: string) {
        this._startDate = value;
    }

    get endDate(): string {
        return this._endDate;
    }

    set endDate(value: string) {
        this._endDate = value;
    }

    toJson(): IOrderSearchAmountFilter {
        return {
            idsCampaign: this._idsCampaign,
            idsUser: this._idsUser,
            status: this._status,
            searchingText: this._searchingText,
            endDate: this._endDate,
            startDate: this._startDate
        }
    }
}