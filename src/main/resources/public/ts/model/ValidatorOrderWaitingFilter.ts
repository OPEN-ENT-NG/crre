import {moment} from "entcore";
import {UserModel} from "./UserModel";
import {Campaign} from "./Campaign";

export class ValidatorOrderWaitingFilter {
    startDate;
    endDate;
    queryName: string;
    userList: Array<UserModel>;
    typeCampaignList: Array<Campaign>;
    filterChoiceCorrelation: Map<string, string>;


    constructor() {
        this.startDate = moment().add(-6, 'months')._d;
        this.endDate = moment()._d;
        this.queryName = "";
        this.userList = [];
        this.typeCampaignList = [];
        this.filterChoiceCorrelation = new Map<string, string>();
    }

    filterChoiceCorrelationKey(): Array<string> {
        return Array.from(this.filterChoiceCorrelation.keys());
    }
}