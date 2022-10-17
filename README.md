# À propos de l'application CRRE
* Licence : [AGPL v3](http://www.gnu.org/licenses/agpl.txt)
* Développeur : CGI
* Financeur : Région Ile de France
* Description : Le Catalogue de Ressources Régionales Educatives est un service de gestion des achats des ressources numériques

# Présentation du module

L'application **CRRE**, mise à disposition des collèges et lycées d'île-de-France, permet de gérer les différentes campagnes d'équipements en ressources numériques issus de marchés publics en s'appuyant sur un catalogue. Elle propose deux types de campagnes :

 - Les campagnes basées sur un panier/commande et une enveloppe budgétaire/nombre de licences annuelle allouée aux établissements
 - Les campagnes basées sur un panier/demande organisé sous forme d'une liste de voeux avec instruction de la demande

## Fonctionnalités

4 profils utilisateurs sont disponibles :
 - Le profil Enseignant/Personnel lambda qui peut consulter les ressources numériques proposées dans le catalogue
 - Le profil Prescripteur qui peut choisir une campagne, se constituer un panier et le soumettre à validation. Il peut également suivre l'état de ses demandes
 - Le profil Valideur qui voit la liste des demandes à valider et qui peut suivre l'ensemble des demandes de l'établissement
 - Le profil Administrateur qui voit la liste des demandes à traiter, génère les bons de commande, clôture les demandes. Il gère également les campagnes, les regroupements d'établissements et éventuellement le catalogue
 - Le profil Libraire qui gère le catalogue et l'état de chacun des articles.

## Configuration

<pre>
{
   "name":"fr.openent~crre~1.1.8",
   "config":{
      "main":"fr.openent.crre.Crre",
      "port":8150,
      "app-name":"CRRE",
      "app-address":"/crre",
      "app-icon":"${host}/crre/public/img/logo.png",
      "app-type":"END_USER",
      "host":"${host}",
      "ssl":$ssl,
      "auto-redeploy":false,
      "userbook-host":"${host}",
      "integration-mode":"HTTP",
      "app-registry.port":8012,
      "mode":"${mode}",
      "entcore.port":8009,
      "sql":true,
      "db-schema":"crre",
      "mail":{
         "address":"${address}"
      },
      "elasticsearch":true,
      "elasticsearchConfig":{
         "server-uri":"${elasticServerURI}",
         "index":"${elasticCRREIndexName}"
      },
      "timeSecondSynchCron":"${timeSecondSynchCron}",
      "timeSecondStatCron":"${timeSecondStatCron}",
      "timeSecondStatutCron":"${timeSecondStatutCron}"
   }
}
</pre>
Dans votre springboard, vous devez inclure des variables d'environnement :

| **conf.properties variable** | **usage**                                                                                                                                                              | **Exemple**                     |
|------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------|
| "${address}"                 | bookseller's email address                                                                                                                                             | xxx@yyy.com                     |
| "${elasticServerURI}"        | elastic server URI                                                                                                                                                     | http://XXX.XXX.XXX.XXX:XXXXX    |
| "${elasticCRREIndexName}"    | elastic CRRE Index Name                                                                                                                                                | /articlenumerique,articlepapier |
| "${timeSecondSynchCron}"     | Cron for the recovery of the statistics of all the estabs (number of digital item orders, paper, credits spent ...) and injection into the Mongo database for storage. | */30 * * * * ? *                |
| "${timeSecondStatCron}"      | Cron to update the status of orders in BDD compared to the information transmitted by LDE.                                                                             | */30 * * * * ? *                |
| "${timeSecondStatutCron}"    | Cron to recover the number of students in each sector (second to final year, cap, bma ....) of each establishment and update the numbers of each establishment in BDD. | */30 * * * * ? *                |
