import {Eventer, Mix, Selectable, Selection} from "entcore-toolkit";
import {Grade, Grades, Title, Titles} from './index';
import http from "axios";
import {_, notify} from "entcore";

export class Project implements Selectable {
    selected: boolean;
    id: number;
    description?: string;
    title: Title;
    grade: Grade;
    building?: string;
    preference?: number;
    stair?: number;
    room?: string;
    site?: string;
    titles: Titles;
    grades: Grades;
    eventer: Eventer;
    id_grade: number;


    constructor() {
        if (this.title) {
            this.title = Mix.castAs(Title, JSON.parse(this.title.toString()));
        }
        if (this.grade) {
            this.grade = Mix.castAs(Grade, JSON.parse(this.grade.toString()));
        }
        this.grades = new Grades();
        this.titles = new Titles();
        this.eventer = new Eventer();
    }

    async init(idCampaign: number, idStructure: string) {
        this.eventer.trigger('init:start');
        await this.titles.sync(idCampaign, idStructure);
        await this.grades.sync();
        if (!this.title) {
            this.title = this.titles.all[0];
        }
        if (!this.grade) {
            this.grade = _.findWhere(this.grades.all, {id: this.id_grade});
        }
        this.eventer.trigger('init:end');
    }

    toJson() {
        let data =
            {
            description: this.description,
            id_title: this.title.id,
            id_grade: (this.grade ? this.grade.id : undefined),
            building: this.building,
            site: this.site,
            stair: this.stair,
            room: this.room
        };

        return this.parseToJsonOptionnal(data);
    }

    parseToJsonOptionnal({description, id_title, id_grade, building, site, stair, room}) {
        return {
            ...(description && {description}),
            id_title: id_title,
            id_grade: id_grade,
            ...(building && {building: building}),
            ...(site && {site: site}),
            ...((stair || stair == 0) && {stair: stair}),
            ...(room && {room: room}),

        }
    }

    async create(id_campaign: number, id_structure:string ) {
        try {
            let id_project = await  http.post(`/lystore/project/${id_campaign}/${id_structure}`, this.toJson());
            this.id = (id_project.data["id"]);
            this.eventer.trigger('create:end');
            return id_project;

        } catch (e) {
            notify.error('lystore.project.create.err');
        }
    }

    async delete(id_campaign, id_structure) {
        try {
            return await http.delete(`/lystore/project/${this.id}/${id_campaign}/${id_structure}`);
        } catch (e) {
            notify.error('lystore.project.delete.err')
        }
    }

    async update(idCampaign) {
        try {
            return await  http.put(`/lystore/project/${this.id}/campaign/${idCampaign}`, this.toJson());
        } catch (e) {
            notify.error('lystore.project.update.err');
        }
    }
}


export class Projects extends Selection<Project> {
    constructor() {
        super([]);
    }

    async sync(): Promise<void> {
        {
            let projects = await http.get(`/lystore/projects`);
            this.all = Mix.castArrayAs(Project, projects.data);

        }
    }
}

