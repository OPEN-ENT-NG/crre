import { _, notify } from 'entcore';
import http from 'axios';
import { Mix, Selectable, Selection } from 'entcore-toolkit';

export class Agent implements Selectable {
    id: string;
    email: string;
    name: string;
    phone: string;
    department: string;

    selected: boolean;

    constructor (name?: string, email?: string, phone?: string, department?: string) {
        if (name) this.name = name;
        if (email) this.email = email;
        if (phone) this.phone = phone;
        if (department) this.department = department;

        this.selected = false;
    }

    toJson () {
        return {
            email: this.email,
            name: this.name,
            phone: this.phone,
            department: this.department
        };
    }

    async save (): Promise<void> {
        if (this.id) await this.update();
        else await this.create();
    }

    async create (): Promise<void> {
        try {
            let agent = await http.post(`/crre/agent`, this.toJson());
            this.id = agent.data.id;
        } catch (e) {
            notify.error('crre.agent.create.err');
        }

    }

    async update (): Promise<void> {
        try {
            let agent = await http.put(`/crre/agent/${this.id}`, this.toJson());
            let { name, phone, email, department } = agent.data;
            this.name = name;
            this.phone = phone;
            this.email = email;
            this.department = department;
        } catch (e) {
            notify.error('crre.agent.update.err');
        }
    }

    async delete (): Promise<void> {
        try {
            await http.delete(`/crre/agent?id=${this.id}`);
        } catch (e) {
            notify.error('crre.agent.delete.err');
        }
    }

}

export class Agents extends Selection<Agent> {

    constructor () {
        super([]);
    }

    async sync (): Promise<void> {
        let agents = await http.get(`/crre/agents`);
        this.all = Mix.castArrayAs(Agent, agents.data);
    }

    async delete (agents: Agent[]): Promise<void> {
        try {
            let filter = '';
            agents.map((agent) => filter += `id=${agent.id}&`);
            filter = filter.slice(0, -1);
            await http.delete(`/crre/agent?${filter}`);
        } catch (e) {
            notify.error('crre.agent.delete.err');
        }
    }
}