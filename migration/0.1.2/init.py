#!/usr/bin/env python
# -*- coding: utf-8 -*-

import argparse
import uuid
from datetime import datetime
from neo4jrestclient.client import GraphDatabase

application_role = 'lystore.access'

parser = argparse.ArgumentParser()
parser.add_argument('-a', '--neo_address', help='Neo4j host', required=True)
parser.add_argument('-p', '--neo_port', help='Neo4j port', required=True)
args = parser.parse_args()

gdb = GraphDatabase(str("http://" + args.neo_address + ":" + args.neo_port + "/db/data/"))


# Generate a UUID to store it as resource identifier
def generate_uuid():
    return str(uuid.uuid4())


# Create applicative role and return its Id
def create_applicative_role():
    id = generate_uuid()
    query = """MATCH (n:Role) WHERE n.name = {roleName} WITH count(*) AS exists WHERE exists=0 
            CREATE (m:Role {id: {id}, name: {roleName}}) WITH m MATCH (n:Action) 
            WHERE n.name IN ["fr.openent.lystore.controllers.LystoreController|view"] 
            CREATE UNIQUE m-[:AUTHORIZE]->n RETURN DISTINCT m.id as id"""
    params = {
        'roleName': 'Lystore - Personnel établissement',
        'id': id
    }
    gdb.query(q=query, params=params)
    return id


# Returns structures list
def get_structure_list():
    query = "MATCH (s:Structure) return s"
    return gdb.query(q=query)


# Create a manual group in the structure provided by the structure_id
# The manual group will be used to store administrative personnel and apply the applicative role
def create_manual_group(structure_id):
    query = """MERGE (t:Group:ManualGroup:Visible { id : {id}}) 
            SET t.groupDisplayName = 'Lystore', t.displayNameSearchField = 'lystore', t.name = 'Lystore' 
            RETURN t.id as id"""
    group_id = gdb.query(q=query, params={'id': generate_uuid()}).get_response()['data'][0][0]

    query = """MATCH (s:Structure {id : {structure_id}}), (g:Group {id : {group_id}}) 
            CREATE UNIQUE s<-[:DEPENDS]-g"""
    gdb.query(q=query, params={'structure_id': structure_id, 'group_id': group_id}).get_response()
    return group_id


# Returns all user that contains administrative function
def get_administrative_personnel(structure_id):
    query = """MATCH (s:Structure {id:{id}})<-[ADMINISTRATIVE_ATTACHMENT]-(u:User) 
    WHERE ANY(function IN u.functions WHERE function =~ '.*\\\$DIRECTION\\\$.*') return u
    UNION ALL
    MATCH (s:Structure {id:{id}})<-[ADMINISTRATIVE_ATTACHMENT]-(u:User) 
    WHERE ANY(function IN u.functions WHERE function =~ '.*\\\$PERSONNELS ADMINISTRATIFS\\\$.*\\\$(GESTION COMPTABLE|GESTION MATERIELLE)') return u"""
    return gdb.query(q=query, params={'id': structure_id}).get_response()


# Add user to group
def add_user_to_group(user, group_id):
    query = """MATCH (g:Group:ManualGroup:Visible {id:{id}}), (u:User {id:{user_id}}) CREATE u-[:IN]->g"""
    gdb.query(q=query, params={'id': group_id, 'user_id': user})
    return


# Link group to applicative role
def link_group_to_role(group_id, role_id):
    query = """MATCH (n:Role), (m:Group) WHERE m.id = {groupId} AND n.id = {roleId} CREATE UNIQUE m-[:AUTHORIZED]->n"""
    return gdb.query(q=query, params={'groupId': group_id, 'roleId': role_id})


print('Starting initialization : ' + str(datetime.now()))
role_id = create_applicative_role()
structures = get_structure_list()
for structure in structures:
    structure_id = structure[0]['data']['id']
    print(str(datetime.now()) + ' : ' + structure[0]['data']['name'])
    group_id = create_manual_group(structure_id)
    personnel = get_administrative_personnel(structure_id)
    users = []
    for user in personnel['data']:
        add_user_to_group(user[0]['data']['id'], group_id)
    link_group_to_role(group_id, role_id)
print('Ending initialization : ' + str(datetime.now()))
