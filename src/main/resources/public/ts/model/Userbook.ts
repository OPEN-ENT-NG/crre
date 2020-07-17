import http from 'axios';

export class Userbook {
    id: number;
    name: string;


    async  putPreferences(name,preferences){
        let data = await http.get('/userbook/preference/lystore');
        let jsonValue ={};
        if(data.data && data.data.preference)
            jsonValue = JSON.parse(data.data.preference);
        jsonValue[name] = preferences;
        http.put('/userbook/preference/lystore',jsonValue);
    }

    async getPreferences(){
         let data = await http.get('/userbook/preference/lystore');
         return data.data;
    }
}