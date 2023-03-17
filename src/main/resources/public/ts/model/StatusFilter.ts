import {IFilter} from "./Filter";
import {ORDER_STATUS_ENUM} from "../enum/order-status-enum";
import {idiom as lang} from 'entcore';

export class StatusFilter implements IFilter{
    private _orderStatusEnum: ORDER_STATUS_ENUM;

    constructor(orderStatusEnum: ORDER_STATUS_ENUM) {
        this._orderStatusEnum = orderStatusEnum;
    }

    get orderStatusEnum(): ORDER_STATUS_ENUM {
        return this._orderStatusEnum;
    }

    set orderStatusEnum(value: ORDER_STATUS_ENUM) {
        this._orderStatusEnum = value;
    }

    getValue(): string {
        return this._orderStatusEnum;
    }

    toString() {
        if (this._orderStatusEnum === ORDER_STATUS_ENUM.IN_PROGRESS) {
            return lang.translate("NEW");
        } else {
            return lang.translate(this._orderStatusEnum)
        }
    }
}