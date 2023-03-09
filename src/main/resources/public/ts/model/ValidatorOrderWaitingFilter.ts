import {moment} from "entcore";
import {UserModel} from "./UserModel";
import {Campaign} from "./Campaign";

export class ValidatorOrderWaitingFilter {
    startDate;
    endDate;
    queryName: string;
    users: Array<UserModel>;
    typeCampaign: Array<Campaign>;
    filterChoiceCorrelation: Array<string>;


    constructor(filterChoiceCorrelation: Array<string>) {
        this.startDate = moment().add(-6, 'months')._d;
        this.endDate = moment()._d;
        this.queryName = "";
        this.users = [];
        this.typeCampaign = [];
        this.filterChoiceCorrelation = filterChoiceCorrelation;
    }
}