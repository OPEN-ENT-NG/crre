import {moment} from "entcore";
import {UserModel} from "./UserModel";
import {Campaign} from "./Campaign";
import {ORDER_STATUS_ENUM} from "../enum/order-status-enum";

export class ValidatorOrderWaitingFilter {
    startDate;
    endDate;
    queryName: string;
    userList: Array<UserModel>;
    typeCampaignList: Array<Campaign>;
    filterChoiceCorrelation: Map<string, string>;
    private _status: Array<ORDER_STATUS_ENUM>;


    constructor() {
        this.startDate = moment().add(-6, 'months')._d;
        this.endDate = moment()._d;
        this.queryName = "";
        this.userList = [];
        this.typeCampaignList = [];
        this.filterChoiceCorrelation = new Map<string, string>();
        this._status = [];
    }

    filterChoiceCorrelationKey(): Array<string> {
        return Array.from(this.filterChoiceCorrelation.keys());
    }


    get status(): Array<ORDER_STATUS_ENUM> {
        return this._status;
    }

    set status(value: Array<ORDER_STATUS_ENUM>) {
        this._status = value;
    }
}