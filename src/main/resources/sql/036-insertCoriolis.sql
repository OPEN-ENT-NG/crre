
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0930133P' ,'0a40b372-5fa9-4200-8d56-d875be25f266','R15799','LYC')
ON CONFLICT (id) DO
 UPDATE SET uai='0930133P' ,code_coriolis = 'R15799'
 WHERE specific_structures.id = EXCLUDED.id    ;

INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0783549J' ,'267437cf-954b-470a-9529-6ed638ca0dc6','R3231','CMD')
 ON CONFLICT (id) DO
  UPDATE SET uai='0783549J',type='CMD',code_coriolis = 'R3231'
 WHERE specific_structures.id = EXCLUDED.id    ;

INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0783548H' ,'c6f3da5a-4c99-48a6-add9-559fde1a0130','R3233','CMD')
 ON CONFLICT (id) DO
  UPDATE SET uai='0783548H',type='CMD',code_coriolis = 'R3233'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0940743X' ,'bc3f03fc-8f86-402f-b7c3-79784569c514','R3502','CMR')
 ON CONFLICT (id) DO
  UPDATE SET uai='0940743X',type='CMR',code_coriolis = 'R3502'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0940124Z' ,'3a3c85b8-9f62-430b-80e8-90c0b44d92eb','R3504','CMR')
 ON CONFLICT (id) DO
  UPDATE SET uai='0940124Z',type='CMR',code_coriolis = 'R3504'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0940117S' ,'006cdc61-98f9-4369-a3ad-649a31634a98','R3493','CMR')
 ON CONFLICT (id) DO
  UPDATE SET uai='0940117S',type='CMR',code_coriolis = 'R3493'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0932122B' ,'c9eeec0b-3846-4fc7-9620-ec34a62f049d','R3768','CMR')
 ON CONFLICT (id) DO
  UPDATE SET uai='0932122B',type='CMR',code_coriolis = 'R3768'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0930123D' ,'8a7fcc7e-1a21-47c6-9f6b-93fc560a7203','R15801','CMR')
 ON CONFLICT (id) DO
  UPDATE SET uai='0930123D',type='CMR',code_coriolis = 'R15801'
 WHERE specific_structures.id = EXCLUDED.id ;

INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0930121B' ,'3ca9db74-2935-4181-bec4-5d3f76d468a2','R3648','CMR')
 ON CONFLICT (id) DO
  UPDATE SET uai='0930121B',type='CMR',code_coriolis = 'R3648'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0930118Y' ,'9c788eef-87a9-46f7-a8b0-a1247469bc16','R3627','CMR')
 ON CONFLICT (id) DO
  UPDATE SET uai='0930118Y',type='CMR',code_coriolis = 'R3627'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0930116W' ,'8ccd1d64-7aac-4176-be7e-440b44c40761','R3619','CMR')
 ON CONFLICT (id) DO
  UPDATE SET uai='0930116W',type='CMR',code_coriolis = 'R3619'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0920798T' ,'489190b8-7bc1-4730-bcec-fc980e1ed2b1','R3587','CMR')
 ON CONFLICT (id) DO
  UPDATE SET uai='0920798T',type='CMR',code_coriolis = 'R3587'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0920149M' ,'4902ffeb-7d8c-4938-b1dc-a99177117547','R3609','CMR')
 ON CONFLICT (id) DO
  UPDATE SET uai='0920149M',type='CMR',code_coriolis = 'R3609'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0920146J' ,'e1ea2b15-a051-41e0-a032-3ce1220ee244','R3605','CMR')
 ON CONFLICT (id) DO
  UPDATE SET uai='0920146J',type='CMR',code_coriolis = 'R3605'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0920145H' ,'b33d80c9-09a7-4cc9-9cb5-820dce859d7f','R3604','CMR')
 ON CONFLICT (id) DO
  UPDATE SET uai='0920145H',type='CMR',code_coriolis = 'R3604'
 WHERE specific_structures.id = EXCLUDED.id ;

INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0920142E' ,'ccd0e452-217d-4d8c-a2bf-248e160c6b4e','R3595','CMR')
 ON CONFLICT (id) DO
  UPDATE SET uai='0920142E',type='CMR',code_coriolis = 'R3595'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0782562L' ,'320834e4-3973-4e73-a3ce-06e4b8cca1f4','R3692','CMR')
 ON CONFLICT (id) DO
  UPDATE SET uai='0782562L',type='CMR',code_coriolis = 'R3692'
 WHERE specific_structures.id = EXCLUDED.id ;

INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0782546U' ,'58cd419a-bd39-4995-bb1d-03e3e97e2b0f','R3676','CMR')
 ON CONFLICT (id) DO
  UPDATE SET uai='0782546U',type='CMR',code_coriolis = 'R3676'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750715V' ,'c34ec4bf-53f9-483f-a384-dd43fe7c0474','R3358','CMR')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750715V',type='CMR',code_coriolis = 'R3358'
 WHERE specific_structures.id = EXCLUDED.id ;

INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750714U' ,'febe9291-71da-4d79-8d38-dd4e68c942e0','R3363','CMR')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750714U',type='CMR',code_coriolis = 'R3363'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750711R' ,'015e0643-afce-4927-93dc-1130ce9b15bf','R3351','CMR')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750711R',type='CMR',code_coriolis = 'R3351'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750705J' ,'0f1780e0-b539-4073-8ddd-4e4e5822c222','R3372','CMR')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750705J',type='CMR',code_coriolis = 'R3372'
 WHERE specific_structures.id = EXCLUDED.id ;

INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750704H' ,'34d7b711-894f-416f-8180-38bb65989f70','R3371','CMR')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750704H',type='CMR',code_coriolis = 'R3371'
 WHERE specific_structures.id = EXCLUDED.id ;


INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750703G' ,'6d3c254c-4609-4ab2-976c-095476533a24','R3390','CMR')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750703G',type='CMR',code_coriolis = 'R3390'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750702F' ,'14ae8de6-be8f-4630-95f6-2da38eff7162','R3389','CMR')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750702F',type='CMR',code_coriolis = 'R3389'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750700D' ,'3fb00c41-cdc1-4a97-957d-58a5185f6454','R3388','CMR')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750700D',type='CMR',code_coriolis = 'R3388'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750699C' ,'b7358e02-1047-4e10-b910-58c6523456f2','R3387','CMR')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750699C',type='CMR',code_coriolis = 'R3387'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750698B' ,'fad2c06a-a306-4c4e-a024-ad0097899b6c','R3386','CMR')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750698B',type='CMR',code_coriolis = 'R3386'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750694X' ,'dd9ba1e0-a277-4791-921b-764e9de8ef8c','R3384','CMR')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750694X',type='CMR',code_coriolis = 'R3384'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750693W' ,'50591e16-db7a-48f8-86bd-11abef710e05','R3462','CMR')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750693W',type='CMR',code_coriolis = 'R3462'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750690T' ,'bc05bd08-e2e2-44e6-9335-26014ac6c6e6','R3456','CMR')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750690T',type='CMR',code_coriolis = 'R3456'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750689S' ,'b9db832f-ba23-4d98-8fb5-c83271582589','R3455','CMR')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750689S',type='CMR',code_coriolis = 'R3455'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750684L' ,'4001a6d7-2283-4437-9aa5-735b5e311b0b','R3452','CMR')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750684L',type='CMR',code_coriolis = 'R3452'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750683K' ,'5c4e2673-655f-4456-af50-ab0383680054','R3446','CMR')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750683K',type='CMR',code_coriolis = 'R3446'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750682J' ,'e50e20b9-c246-4b87-944c-3a5bd2adf256','R3445','CMR')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750682J',type='CMR',code_coriolis = 'R3445'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750679F' ,'f83c66d4-6916-4491-8ac0-47ba8af38422','R3439','CMR')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750679F',type='CMR',code_coriolis = 'R3439'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750675B' ,'7b23c92f-032b-4601-92c6-9618bee023a6','R3435','CMR')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750675B',type='CMR',code_coriolis = 'R3435'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750670W' ,'b89b32f3-e9aa-4c2b-a617-77218c8d91e3','R3426','CMR')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750670W',type='CMR',code_coriolis = 'R3426'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750669V' ,'480bb965-9666-4655-9bc1-407a71eef9d9','R3425','CMR')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750669V',type='CMR',code_coriolis = 'R3425'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750668U' ,'b4a4ef3b-1563-4383-bfc1-610660746560','R3429','CMR')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750668U',type='CMR',code_coriolis = 'R3429'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750663N' ,'9b504636-beec-49d6-997e-a2736005be6e','R3422','CMR')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750663N',type='CMR',code_coriolis = 'R3422'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750662M' ,'2811fe10-e0b6-4a95-8d3a-04d0b98f3a7f','R3419','CMR')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750662M',type='CMR',code_coriolis = 'R3419'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750657G' ,'e57722e2-fef0-4a28-942d-6f9256a6b21f','R3418','CMR')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750657G',type='CMR',code_coriolis = 'R3418'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750656F' ,'fdad3421-5512-4158-8ca8-02dd69cb975f','R3414','CMR')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750656F' ,code_coriolis = 'R3414'
 WHERE specific_structures.id = EXCLUDED.id ;

INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750654D' ,'f54bd00d-8988-4a00-a612-8a744f08eb99','R3415','CMR')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750654D',type='CMR',code_coriolis = 'R3415'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750652B' ,'54a0fba4-3e9c-42ad-a59d-ccab07d8eb27','R3412','CMR')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750652B',type='CMR',code_coriolis = 'R3412'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750648X' ,'75bfcaf4-0f2f-4cfd-a380-19d9824edd62','R3407','CMR')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750648X',type='CMR',code_coriolis = 'R3407'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0952196W' ,'5515e797-0b5f-44c9-b18c-10aa5bf61a48','P0034141','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0952196W' ,code_coriolis = 'P0034141'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0952173W' ,'a1da6f72-bfd8-442e-9e76-a7ccd8b4ae7f','P0030507','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0952173W' ,code_coriolis = 'P0030507'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0951974E' ,'0e3cd613-b6cb-4b35-9b41-524e821efbf1','R3744','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0951974E' ,code_coriolis = 'R3744'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0951937P' ,'66afbedf-9054-4ebd-9c1f-b2e1e29be872','R3215','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0951937P' ,code_coriolis = 'R3215'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0951922Y' ,'6d16641b-171a-4e99-bfd3-c63886e2504c','R3214','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0951922Y' ,code_coriolis = 'R3214'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0951824S' ,'bb5e05f8-7ff3-41dc-998b-767a53f3aa5c','R3209','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0951824S' ,code_coriolis = 'R3209'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0951811C' ,'59102eb3-6445-4433-bf51-979bf166539a','R3466','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0951811C' ,code_coriolis = 'R3466'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0951788C' ,'63f31cbb-f1c3-4d0e-9f51-6d8e5536ec06','R19249','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0951788C' ,code_coriolis = 'R19249'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0951787B' ,'9262211f-359c-420c-a0ef-77fa037a755a','R3475','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0951787B' ,code_coriolis = 'R3475'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0951766D' ,'10a0ab1a-2a87-4a89-9358-adca3548f3cf','R18171','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0951766D' ,code_coriolis = 'R18171'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0951763A' ,'99d6c3f1-76e0-4d54-9589-a2eebf3ed73a','R3404','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0951763A' ,code_coriolis = 'R3404'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0951756T' ,'b2143892-a45f-4095-95e1-707b6d9c8c95','R18856','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0951756T' ,code_coriolis = 'R18856'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0951753P' ,'4c3c6203-75ad-4a4b-b187-3ff888fdc413','R16193','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0951753P' ,code_coriolis = 'R16193'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0951748J' ,'2e24eb9f-2f49-46a7-aba7-f785138d94a2','R3469','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0951748J' ,code_coriolis = 'R3469'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0951728M' ,'584858ae-b4e3-4050-bbd1-254f617422f8','R8263','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0951728M' ,code_coriolis = 'R8263'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0951727L' ,'9db734d3-c7b9-42cc-9712-a2c43cf9f673','R3728','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0951727L' ,code_coriolis = 'R3728'
 WHERE specific_structures.id = EXCLUDED.id ;


INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0951723G' ,'47862f25-c161-4104-a4ae-7e219dfc6360','R3729','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0951723G' ,code_coriolis = 'R3729'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0951722F' ,'fd457c5b-2c09-44b3-8a8e-b1023639cb11','R3726','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0951722F' ,code_coriolis = 'R3726'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0951710T' ,'8acf6e56-ad30-40d2-a5bb-11bba2967158','R3678','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0951710T' ,code_coriolis = 'R3678'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0951673C' ,'71a0478f-adda-4ae4-b5fb-40b609bcf9bb','R3686','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0951673C' ,code_coriolis = 'R3686'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0951637N' ,'9ed5e5b4-366d-4c14-b1af-4f2d63c2f9fe','R3474','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0951637N' ,code_coriolis = 'R3474'
 WHERE specific_structures.id = EXCLUDED.id ;



INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0951618T' ,'62fd28cb-9640-4d5e-a778-78fe6f73fc57','R3683','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0951618T' ,code_coriolis = 'R3683'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0951399E' ,'61fa6c59-c66f-47b3-a0ed-e57df88bd631','R3473','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0951399E' ,code_coriolis = 'R3473'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0951282C' ,'035edc92-c615-4bdc-a04c-42b56dc480c2','R3679','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0951282C' ,code_coriolis = 'R3679'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0951281B' ,'6ca84be9-f457-4a78-9b4e-90c8aa34e1fd','R3395','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0951281B' ,code_coriolis = 'R3395'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0951147F' ,'a52a64a4-a327-485d-bedb-b805f51bdd87','R3392','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0951147F' ,code_coriolis = 'R3392'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0951104J' ,'e94ab145-b09e-473d-b4fc-a78c04a7861d','R3399','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0951104J' ,code_coriolis = 'R3399'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0951090U' ,'757b81bb-1549-46ef-803f-d7e49fefbd70','R3405','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0951090U' ,code_coriolis = 'R3405'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0950983C' ,'8c17a84f-cc4d-4388-bf31-b6d3184c53b3','R3400','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0950983C' ,code_coriolis = 'R3400'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0950949R' ,'4e38c317-23c6-43d6-bc20-c2c5690944ed','R3396','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0950949R' ,code_coriolis = 'R3396'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0950947N' ,'e5f996c5-89a3-4ced-82fd-22c165aa985c','R3402','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0950947N' ,code_coriolis = 'R3402'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0950709E' ,'74af1914-6bba-4c1f-a3a2-f311e0dcdf80','R3468','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0950709E' ,code_coriolis = 'R3468'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0950667J' ,'06721ec8-3ff1-4d2b-83fc-0f4b2a1afe75','R18617','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0950667J' ,code_coriolis = 'R18617'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0950666H' ,'cbb81ddb-74ab-4d5b-87f3-fa9f50d27285','R3486','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0950666H' ,code_coriolis = 'R3486'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0950658Z' ,'ea2cd2ce-3811-4fcd-bce3-b3431c5a405d','R3365','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0950658Z' ,code_coriolis = 'R3365'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0950657Y' ,'897a7450-9b82-45d8-8320-970a5f5abbfb','R3685','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0950657Y' ,code_coriolis = 'R3685'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0950656X' ,'b939c8d6-24a1-4ea6-bb03-9a79f9355e1d','R3680','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0950656X' ,code_coriolis = 'R3680'
 WHERE specific_structures.id = EXCLUDED.id ;


INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0950651S' ,'913ab627-a607-42dc-8e07-8504b403924d','R3403','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0950651S' ,code_coriolis = 'R3403'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0950650R' ,'cffb454a-d62b-4509-bdac-f6144af79738','R18545','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0950650R' ,code_coriolis = 'R18545'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0950649P' ,'869b7b98-fa27-428f-a140-712467267b8a','R3397','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0950649P' ,code_coriolis = 'R3397'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0950648N' ,'eee46862-cef4-4f80-8b43-f098a4c6eea0','R3394','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0950648N' ,code_coriolis = 'R3394'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0950647M' ,'7dcdfcd2-ea76-4d41-a761-008d0a4f8da9','R3393','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0950647M' ,code_coriolis = 'R3393'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0950646L' ,'31ae1c5a-1b6d-4b31-91d1-1754f1f67893','R3687','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0950646L' ,code_coriolis = 'R3687'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0950645K' ,'8127151a-3906-4f58-b20a-10e255870384','R3684','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0950645K' ,code_coriolis = 'R3684'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0950641F' ,'94eb928b-e722-47da-9ffe-b5015e7d7ba2','R18539','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0950641F' ,code_coriolis = 'R18539'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0950640E' ,'4cbe5cdc-5df6-4720-af41-132146a88324','R3484','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0950640E' ,code_coriolis = 'R3484'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0950164M' ,'f606d9f0-58f0-44fe-a55b-89dd2adacfe9','R3470','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0950164M' ,code_coriolis = 'R3470'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0942269F' ,'00413db9-141a-488b-8ae8-29b3802ad18f','P0015628','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0942269F' ,code_coriolis = 'P0015628'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0942130E' ,'f5c48bee-8c4a-442a-8a3f-9c4cf98fa9fd','R3499','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0942130E' ,code_coriolis = 'R3499'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0941975L' ,'422713e1-e04d-4f28-a819-19d1bda82543','R3234','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0941975L' ,code_coriolis = 'R3234'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0941974K' ,'60c5a17e-bfaa-49b3-b8ae-e3195dd965bf','R3527','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0941974K' ,code_coriolis = 'R3527'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0941972H' ,'d6132286-391f-4fd1-8979-8afe9c897693','R3508','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0941972H' ,code_coriolis = 'R3508'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0941952L' ,'1b452581-35c0-40ca-bfe0-e7d6c131cd23','R3501','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0941952L' ,code_coriolis = 'R3501'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0941951K' ,'6f626083-a620-4900-b915-773b75ce11d6','R3528','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0941951K' ,code_coriolis = 'R3528'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0941930M' ,'16c825ff-13b8-4736-b57b-87729e0a0d5b','R19129','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0941930M' ,code_coriolis = 'R19129'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0941918Z' ,'f7bfd31d-843c-4e87-921a-42f6963f4c65','R16037','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0941918Z' ,code_coriolis = 'R16037'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0941604H' ,'638d0574-5a7e-48bd-ad80-24d4401355a3','R18525','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0941604H' ,code_coriolis = 'R18525'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0941474S' ,'5deb1d8b-83d5-458c-a556-4f4340ea2e4d','R3515','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0941474S' ,code_coriolis = 'R3515'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0941470M' ,'cc2093e1-656a-44dd-b538-0b628e96fc86','R3526','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0941470M' ,code_coriolis = 'R3526'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0941413A' ,'58a35e75-267d-49a0-bc3b-6a636308f1ab','R3522','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0941413A' ,code_coriolis = 'R3522'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0941355M' ,'0fd956f4-536c-4d0a-b5d9-598ee4f1f6ce','R3487','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0941355M' ,code_coriolis = 'R3487'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0941347D' ,'bd889668-ac06-4dbe-bbfb-dc94568ac99c','R3523','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0941347D' ,code_coriolis = 'R3523'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0941301D' ,'b407d168-3987-4a1e-b17b-30bc9b1b4647','R3525','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0941301D' ,code_coriolis = 'R3525'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0941298A' ,'9eaa44ba-638b-4852-832b-2699133d889e','R3524','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0941298A' ,code_coriolis = 'R3524'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0941294W' ,'4e4e391f-9f96-471d-81e9-b5cf6401aaf3','R3476','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0941294W' ,code_coriolis = 'R3476'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0941232D' ,'0f978fa9-5090-4120-a01d-9af00cbdc80d','R3518','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0941232D' ,code_coriolis = 'R3518'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0941018W' ,'0b48d99e-901a-4434-9506-24652f17c5bf','R18527','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0941018W' ,code_coriolis = 'R18527'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0940742W' ,'0f193f74-628e-4daa-87d8-c91b9a3f5769','R3464','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0940742W' ,code_coriolis = 'R3464'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0940585A' ,'be293db3-a0e3-48ba-b4bf-76afe10be9d7','R3496','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0940585A' ,code_coriolis = 'R3496'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0940580V' ,'9b967d3e-9290-413f-8180-f58fc1e80e85','R3533','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0940580V' ,code_coriolis = 'R3533'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0940319L' ,'3d0505b1-fdda-423c-aa5a-ff7888ac885a','R3494','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0940319L' ,code_coriolis = 'R3494'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0940171A' ,'fea41017-d86d-4acf-9896-30e042b989af','R3534','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0940171A' ,code_coriolis = 'R3534'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0940145X' ,'b0262750-a42f-45a2-b73e-80b68d861c81','R3479','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0940145X' ,code_coriolis = 'R3479'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0940143V' ,'30571e19-71b9-48c2-9030-e53450303850','R3505','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0940143V' ,code_coriolis = 'R3505'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0940141T' ,'8a003f22-2570-4b0c-aba6-84df5927f918','R3517','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0940141T' ,code_coriolis = 'R3517'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0940140S' ,'046cf34b-7acb-486f-8b1d-0c5cad1dbd8d','R3511','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0940140S' ,code_coriolis = 'R3511'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0940138P' ,'fa52b9c9-cdb2-4477-ade6-233d14143fcd','R3495','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0940138P' ,code_coriolis = 'R3495'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0940137N' ,'2bd30b2a-312c-4c45-baf6-81f2e9255d90','R3491','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0940137N' ,code_coriolis = 'R3491'
 WHERE specific_structures.id = EXCLUDED.id ;

INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0940134K' ,'e88cc640-60d1-4c92-ac12-ddc341e96870','R3516','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0940134K' ,code_coriolis = 'R3516'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0940132H' ,'9e97bc70-ca3a-417b-ab93-f96aaf89c2c8','R3529','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0940132H' ,code_coriolis = 'R3529'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0940129E' ,'087893ae-3fae-424a-96a9-a75cf78f2b42','R3478','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0940129E' ,code_coriolis = 'R3478'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0940126B' ,'14323428-6f3b-4895-aa40-78537b191641','R3503','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0940126B' ,code_coriolis = 'R3503'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0940123Y' ,'d28bd169-02db-4837-b049-125ab5203997','R3500','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0940123Y' ,code_coriolis = 'R3500'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0940122X' ,'3243107a-61bb-47b4-bbb4-667007b5f770','R3510','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0940122X' ,code_coriolis = 'R3510'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0940121W' ,'0c76001d-90e7-4608-98f4-7e764f3016ac','R3498','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0940121W' ,code_coriolis = 'R3498'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0940120V' ,'3259fe4b-584a-4274-a867-2052cff168e9','R3497','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0940120V' ,code_coriolis = 'R3497'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0940119U' ,'e288bb89-c9d0-4f23-8280-6af1c7fae795','R18533','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0940119U' ,code_coriolis = 'R18533'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0940118T' ,'25bc76f2-3d0d-4161-8e80-f1bdfb031940','R3490','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0940118T' ,code_coriolis = 'R3490'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0940116R' ,'7544011a-bbd7-4174-9ce6-ea4ef020f7bd','R3488','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0940116R' ,code_coriolis = 'R3488'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0940115P' ,'16eb188b-c4cb-4287-bac8-5d4324936085','R3507','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0940115P' ,code_coriolis = 'R3507'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0940114N' ,'eadad1ab-8fce-4336-b245-2b24bed9d552','R3521','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0940114N' ,code_coriolis = 'R3521'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0940113M' ,'65589f98-181d-4f9b-b692-dd37306687e3','R3530','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0940113M' ,code_coriolis = 'R3530'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0940112L' ,'1fd971fe-1ceb-4b72-8b4d-dbbf6a93e7a0','R3531','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0940112L' ,code_coriolis = 'R3531'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0932667U' ,'05bff1e1-3e0d-4fc7-847e-6dc88654cd72','P0036077','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0932667U' ,code_coriolis = 'P0036077'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0932638M' ,'1d6d569c-cb6b-4df7-b19e-af8d0cf69005','P0033460','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0932638M',code_coriolis = 'P0033460'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0932577W' ,'a0b7617b-a0e9-44e6-a30d-c9a1f829668b','P0028709','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0932577W' ,code_coriolis = 'P0028709'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0932291K' ,'7d683f39-1560-4d8b-aabb-01f3dd9d9abd','R3812','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0932291K' ,code_coriolis = 'R3812'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0932282A' ,'05e3bd89-1712-480a-9671-2cfceff470e8','R3781','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0932282A' ,code_coriolis = 'R3781'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0932267J' ,'568fe90c-0b41-4c63-a72a-ca68c8941b1a','R3756','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0932267J' ,code_coriolis = 'R3756'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0932260B' ,'f7f30b18-32cc-4f77-953e-8045509c7516','R3730','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0932260B' ,code_coriolis = 'R3730'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0932229T' ,'84121086-4ffa-490a-a1f3-7b6c05910c8c','R3631','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0932229T' ,code_coriolis = 'R3631'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0932222K' ,'3a80dcab-edd4-49d6-90f7-6fcc8e64743b','R3642','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0932222K' ,code_coriolis = 'R3642'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0932221J' ,'14a57e3a-b59d-4e6c-aeea-17acf45e2c78','R3757','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0932221J' ,code_coriolis = 'R3757'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0932129J' ,'d5434434-2466-462a-919a-01250a44f3aa','R3663','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0932129J' ,code_coriolis = 'R3663'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0932126F' ,'6ac28d17-67f4-4d43-8cc7-de0182af3be4','R3632','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0932126F' ,code_coriolis = 'R3632'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0932123C' ,'6bfdb882-8f7e-4fce-b296-be786fc12143','R3731','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0932123C' ,code_coriolis = 'R3731'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0932121A' ,'2cc12d6a-aa70-4a47-a1a2-67f774003a45','R3661','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0932121A' ,code_coriolis = 'R3661'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0932120Z' ,'898cab2b-45af-4b70-8c99-2d0efcc80a33','R3647','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0932120Z' ,code_coriolis = 'R3647'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0932119Y' ,'5a653a3e-f126-47f4-93b5-f2d2a32b3585','R3623','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0932119Y' ,code_coriolis = 'R3623'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0932118X' ,'3255d4e7-b490-4467-944d-20ddac2bd6a3','R3640','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0932118X',code_coriolis = 'R3640'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0932117W' ,'735514aa-e412-4fda-8c7e-47a583706112','R3654','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0932117W' ,code_coriolis = 'R3654'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0932116V' ,'23f0f686-bde8-487d-8741-91a86a358591','R3650','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0932116V' ,code_coriolis = 'R3650'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0932074Z' ,'b31cbaea-3083-4cb3-a21d-ba65caf44b16','R3542','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0932074Z' ,code_coriolis = 'R3542'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0932073Y' ,'bffa0cad-0a4c-4ac2-b639-ba871f4ab590','R3644','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0932073Y' ,code_coriolis = 'R3644'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0932048W' ,'7d1c585b-05cd-44a3-a911-381305f5185f','R19063','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0932048W' ,code_coriolis = 'R19063'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0932047V' ,'d3b5b1fb-99c4-447a-b934-3002653401d6','R18858','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0932047V' ,code_coriolis = 'R18858'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0932046U' ,'38b29754-7acd-46c8-a973-21d9413677cc','R17099','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0932046U' ,code_coriolis = 'R17099'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0932034F' ,'3ecdb1f1-5b5e-4c98-bbc2-5d48926acf4a','R15427','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0932034F' ,code_coriolis = 'R15427'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0932031C' ,'2cff7f98-0148-4946-b129-7141c0cd7108','R15357','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0932031C' ,code_coriolis = 'R15357'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0932030B' ,'481c2570-19ed-4c05-914c-281ee012242a','R15269','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0932030B' ,code_coriolis = 'R15269'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0932026X' ,'3f994252-ad80-4ed7-9431-6c21bc5d413c','R15267','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0932026X' ,code_coriolis = 'R15267'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0931779D' ,'e0989cec-d7d8-430e-aef4-4fd28556b2b9','R1331','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0931779D' ,code_coriolis = 'R1331'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0931739K' ,'aba146cf-4e35-40db-9863-7179cb3bfaa1','R3658','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0931739K' ,code_coriolis = 'R3658'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0931738J' ,'1c422031-f632-4f9e-86e4-26a12eb25102','R3638','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0931738J' ,code_coriolis = 'R3638'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0931735F' ,'3a759842-a362-437e-a87f-e5e00510e125','R3634','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0931735F' ,code_coriolis = 'R3634'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0931613Y' ,'5c99ad3e-b838-4294-ba4d-09528057f781','R3624','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0931613Y' ,code_coriolis = 'R3624'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0931585T' ,'d0454654-6854-4724-82dd-f119da11f392','R3646','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0931585T' ,code_coriolis = 'R3646'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0931584S' ,'0b667df4-5265-4675-88d6-454082adab9e','R3537','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0931584S' ,code_coriolis = 'R3537'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0931565W' ,'1f9bf8ad-fd25-4fc6-ab45-541942dcfedb','R3652','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0931565W' ,code_coriolis = 'R3652'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0931430Z' ,'ceb2ffa2-ff94-44d8-bc45-60c3f6b54d5a','R3639','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0931430Z' ,code_coriolis = 'R3639'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0931272C' ,'b7e8af25-201b-4014-ab63-3fdfa4e7b658','R3635','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0931272C' ,code_coriolis = 'R3635'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0931233K' ,'36ce5655-3a00-4fd0-8362-0fee68b31bed','R3636','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0931233K' ,code_coriolis = 'R3636'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0931198X' ,'f60e90eb-74e8-46e7-99aa-1be07c7e086a','R3625','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0931198X' ,code_coriolis = 'R3625'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0931193S' ,'fad60539-fc55-458c-8273-768d89b17665','R3540','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0931193S' ,code_coriolis = 'R3540'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0931024H' ,'8c438eec-637e-4547-801a-8ade1c8b7fd8','R3618','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0931024H' ,code_coriolis = 'R3618'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0930846P' ,'66628e08-2d0b-4027-9e3a-0b7830517e84','R18517','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0930846P' ,code_coriolis = 'R18517'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0930834B' ,'05e5912a-1dc0-4cde-803a-b3392b134369','R3620','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0930834B' ,code_coriolis = 'R3620'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0930833A' ,'3b4d05ab-22ec-43fb-9b79-b883f0424bfc','R3622','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0930833A' ,code_coriolis = 'R3622'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0930831Y' ,'6b84cef5-e2b3-4f40-aa39-011191f672d3','R3641','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0930831Y' ,code_coriolis = 'R3641'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0930830X' ,'2feb427b-29b0-481b-879b-f1c4a3b39e1e','R3643','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0930830X' ,code_coriolis = 'R3643'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0930138V' ,'c78083e5-4d95-4654-9d06-5897f9f72088','R3662','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0930138V' ,code_coriolis = 'R3662'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0930136T' ,'c7cf7c60-a3a0-41b2-adca-5117c06ccecc','R3645','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0930136T' ,code_coriolis = 'R3645'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0930135S' ,'bef09d04-c28d-4046-99c6-4f87483cda07','R3656','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0930135S' ,code_coriolis = 'R3656'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0930130L' ,'af390237-598b-494d-8d7b-cc471b17c048','R3949','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0930130L' ,code_coriolis = 'R3949'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0930129K' ,'634f61c5-6704-4927-b217-8874dc85b4aa','R3628','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0930129K' ,code_coriolis = 'R3628'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0930128J' ,'86a273d7-84f0-4f74-be86-284a84a06a89','R3637','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0930128J' ,code_coriolis = 'R3637'
 WHERE specific_structures.id = EXCLUDED.id ;


INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0930127H' ,'9554f43a-236c-4452-85e6-9b34a44ae3d1','R3539','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0930127H' ,code_coriolis = 'R3539'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0930126G' ,'6eb2210d-7edd-429e-85ab-9f6b03dc99d3','R3543','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0930126G' ,code_coriolis = 'R3543'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0930125F' ,'6c4f4351-2cde-41a1-a513-3c5cca74dfdc','R3660','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0930125F' ,code_coriolis = 'R3660'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0930124E' ,'752d8cc2-e6c9-408d-a88f-9eedd8dcb144','R3655','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0930124E' ,code_coriolis = 'R3655'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0930122C' ,'58aef46d-c05c-4802-996c-79d6386615c2','R18521','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0930122C' ,code_coriolis = 'R18521'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0930120A' ,'2ecae6cd-a06d-4718-9173-be2209f6296d','R3633','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0930120A' ,code_coriolis = 'R3633'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0930119Z' ,'0adb103c-e0c7-41a3-b8bc-893ab3899dab','R3630','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0930119Z' ,code_coriolis = 'R3630'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0930117X' ,'7f020c2b-7eb4-4c25-971b-e73a658bc835','R3616','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0930117X' ,code_coriolis = 'R3616'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0922801V' ,'55dba5f9-3b6e-4728-a433-60e23d862280','P0038493','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0922801V' ,code_coriolis = 'P0038493'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0922615T' ,'03bc9e79-4848-4152-a064-e7cdf9791489','R3981','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0922615T' ,code_coriolis = 'R3981'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0922464D' ,'9a3d16bf-8a60-44fe-aae1-83cf211a0f73','R3779','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0922464D' ,code_coriolis = 'R3779'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0922443F' ,'6b179125-222b-412b-9f93-ce758f5655dd','R3762','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0922443F' ,code_coriolis = 'R3762'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0922427N' ,'297f23ea-651b-4448-883f-926d1555315a','R3753','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0922427N' ,code_coriolis = 'R3753'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0922398G' ,'89184d1e-1f92-4afe-9481-24520db14238','R3745','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0922398G' ,code_coriolis = 'R3745'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0922397F' ,'3a999aa0-7599-4fdf-8fa8-b9db367d3905','R3743','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0922397F' ,code_coriolis = 'R3743'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0922287L' ,'00e58ae7-87dc-4727-8ff5-beee31d7bc6c','R20735','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0922287L' ,code_coriolis = 'R20735'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0922277A' ,'b55c4604-1bd8-404e-8c24-9c2fb517a4c8','R3612','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0922277A' ,code_coriolis = 'R3612'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0922276Z' ,'f0f8ccfa-7608-4208-a66d-fb9f42b69dae','R3603','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0922276Z' ,code_coriolis = 'R3603'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0922249V' ,'89aecbf3-5ecd-4c4f-8a0b-d176d7a0d217','R3584','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0922249V' ,code_coriolis = 'R3584'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0922149L' ,'895121eb-9673-46fe-abe5-68bf04be0222','R3572','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0922149L' ,code_coriolis = 'R3572'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0921935D' ,'e5dc96fd-c23e-4f37-8420-eda8495f30de','R3611','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0921935D' ,code_coriolis = 'R3611'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0921676X' ,'3b4c8fd2-0ee7-43f3-a7ec-d3a4be2ab2cd','R3614','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0921676X' ,code_coriolis = 'R3614'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0921626T' ,'a2e00432-b499-4687-992e-057f31b8b989','R3593','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0921626T' ,code_coriolis = 'R3593'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0921625S' ,'1772cec4-6eca-4092-975f-8c3e942e2b17','R3578','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0921625S' ,code_coriolis = 'R3578'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0921595J' ,'f9bfad99-c101-42b5-b9e9-6082ea3447c2','R3562','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0921595J' ,code_coriolis = 'R3562'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0921594H' ,'76dc532c-337b-43fb-9b2a-75ab906bacf8','R15803','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0921594H' ,code_coriolis = 'R15803'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0921592F' ,'d18c7415-8029-40d8-affd-13127731e564','R3588','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0921592F' ,code_coriolis = 'R3588'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0921555R' ,'312e30f7-cd96-4ab5-8621-a4831bfd3070','R3567','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0921555R' ,code_coriolis = 'R3567'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0921505L' ,'cffd8951-4a66-41e4-995a-b14398820565','R3610','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0921505L' ,code_coriolis = 'R3610'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0921500F' ,'a11bec31-d25f-4b55-a116-e46718a1905e','R3599','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0921500F' ,code_coriolis = 'R3599'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0921399W' ,'8f09dd4e-e6d1-4065-8481-7c00a07f3bd2','R3589','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0921399W' ,code_coriolis = 'R3589'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0921230M' ,'82b03b3f-7a36-40ea-a69b-07110dbd3b57','R3585','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0921230M' ,code_coriolis = 'R3585'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0921229L' ,'71749f08-5c1c-4b2a-9557-a31a453610b0','R3575','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0921229L' ,code_coriolis = 'R3575'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0921166T' ,'b670a3ff-16ef-47c3-8d7b-adb97ede92ef','R3566','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0921166T' ,code_coriolis = 'R3566'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0921156G' ,'67335752-410a-4a7e-a1ed-a827187ff6f7','R3580','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0921156G' ,code_coriolis = 'R3580'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0920810F' ,'a2ba96b4-6f3b-4272-ba77-bbef2dea7c26','R3579','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0920810F' ,code_coriolis = 'R3579'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0920802X' ,'ee0cb3eb-0fc1-4713-b73e-4eba85f1d6ee','R3844','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0920802X' ,code_coriolis = 'R3844'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0920801W' ,'6cd20b67-c961-407f-be0f-5ce48c9e0a71','R3602','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0920801W' ,code_coriolis = 'R3602'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0920799U' ,'e84b22ef-4501-4537-ac1b-2db86144cec4','R3601','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0920799U' ,code_coriolis = 'R3601'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0920680P' ,'23eaeea7-e4a9-42c1-ba7f-6182d9b4356d','R3561','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0920680P' ,code_coriolis = 'R3561'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0920429S' ,'80421ed0-4765-40a0-9073-84b8906e34d0','R3559','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0920429S' ,code_coriolis = 'R3559'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0920171L' ,'57fc5bc1-c155-4265-826e-aa20efd5638e','R3608','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0920171L' ,code_coriolis = 'R3608'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0920170K' ,'d83605e3-354c-4a0a-948f-a888b2044db7','R3606','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0920170K' ,code_coriolis = 'R3606'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0920166F' ,'9643f27d-45cd-45d0-97a1-57db833a9cbc','R3596','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0920166F' ,code_coriolis = 'R3596'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0920164D' ,'72fced08-4750-4ad3-a4f8-0e0c0d12f6b5','R3590','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0920164D' ,code_coriolis = 'R3590'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0920163C' ,'b780177d-5609-4e8e-a524-0164f3c252f1','R3586','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0920163C' ,code_coriolis = 'R3586'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0920158X' ,'4461340d-87f7-47e0-ae4d-7ef98b52bccf','R3583','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0920158X' ,code_coriolis = 'R3583'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0920150N' ,'3a017213-afaf-4a92-af74-d2f71e056f88','R3558','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0920150N' ,code_coriolis = 'R3558'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0920147K' ,'4a0fc53c-ef82-4e27-9cd3-e09b07d289af','R3607','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0920147K' ,code_coriolis = 'R3607'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0920144G' ,'3bef5959-5261-4cb2-9c99-2e396d2bb923','R3598','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0920144G' ,code_coriolis = 'R3598'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0920143F' ,'b504a376-2e50-459d-acc2-ef79460eb0c6','R3597','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0920143F' ,code_coriolis = 'R3597'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0920141D' ,'0cade417-ddaa-4b65-9672-5a88d7ea819b','R3592','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0920141D' ,code_coriolis = 'R3592'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0920138A' ,'8e9a0412-4646-4998-b496-939a848169e4','R3577','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0920138A' ,code_coriolis = 'R3577'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0920137Z' ,'3aaf4c9d-901a-42dd-a7ef-f23a1c3d131b','R3573','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0920137Z' ,code_coriolis = 'R3573'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0920136Y' ,'5fb24e70-b685-4ebb-846a-c220d3e5a525','R3570','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0920136Y',code_coriolis = 'R3570'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0920135X' ,'2d47d7de-9215-4227-88ae-207fa462573a','R3565','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0920135X' ,code_coriolis = 'R3565'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0920134W' ,'a7d2a3f3-d5ea-4dfc-bfff-5915a2a4f4c1','R3563','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0920134W' ,code_coriolis = 'R3563'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0920132U' ,'eb8a252d-26fc-476c-b0b0-86d187f2d5e4','R3560','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0920132U' ,code_coriolis = 'R3560'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0920131T' ,'9333f3b6-764c-4974-a15c-a2b317299818','R3556','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0920131T' ,code_coriolis = 'R3556'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0920130S' ,'eac3ba66-a187-4d6b-8601-c82c6c51da18','R3615','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0920130S' ,code_coriolis = 'R3615'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0912364A' ,'9de4d700-ddb4-41b6-a58f-549bbeac905a','P00037479','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0912364A' ,code_coriolis = 'P00037479'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0912251C' ,'d479ed56-4b10-4c26-9659-fac7ccd89c8a','R3547','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0912251C' ,code_coriolis = 'R3547'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0912163G' ,'2229fdbc-78b2-4caa-be89-989d2c390577','R3754','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0912163G' ,code_coriolis = 'R3754'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0912142J' ,'19fff87c-fbfd-49d7-9826-a5984fcd783d','R3742','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0912142J' ,code_coriolis = 'R3742'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0911983L' ,'ee5c2cac-7350-4abe-b18b-56d7351057e5','R3014','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0911983L' ,code_coriolis = 'R3014'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0911962N' ,'57bbdf6c-328b-488d-b958-2c612f74a2e4','R16853','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0911962N' ,code_coriolis = 'R16853'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0911961M' ,'f643946c-2392-4c04-8c82-0ed2f4925c1e','R18145','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0911961M' ,code_coriolis = 'R18145'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0911946W' ,'6e19154b-5be9-4ada-92fe-99f97b22a64e','R15431','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0911946W' ,code_coriolis = 'R15431'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0911945V' ,'fd4f4e98-3b08-4dd8-9ac4-3a923b68e585','R15365','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0911945V' ,code_coriolis = 'R15365'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0911938M' ,'aec94da7-8d9d-4643-8292-2bc933ed84dc','R3725','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0911938M' ,code_coriolis = 'R3725'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0911937L' ,'131aa20a-75c0-4719-89e6-2fbc91e83564','R3724','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0911937L' ,code_coriolis = 'R3724'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0911927A' ,'d1284baf-4037-4117-9020-b7d41145acb9','R3200','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0911927A' ,code_coriolis = 'R3200'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0911913K' ,'94c18073-a0fb-4a44-835f-0088b0f33b41','R3321','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0911913K' ,code_coriolis = 'R3321'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0911828T' ,'c79dfc32-82d8-477a-9b9f-d8d95c5e16f1','R3329','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0911828T' ,code_coriolis = 'R3329'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0911632E' ,'236a5c76-2b6b-4373-b1be-b0c51f2500bc','R3323','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0911632E' ,code_coriolis = 'R3323'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0911578W' ,'f2ccd05a-d3a8-4142-b604-114ebfe40066','R3549','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0911578W' ,code_coriolis = 'R3549'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0911577V' ,'926678fe-b3c7-48de-80b6-323121774bff','R3342','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0911577V' ,code_coriolis = 'R3342'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0911493D' ,'67aaeaa7-6c1e-46ff-999f-45c81fb663ac','R3548','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0911493D' ,code_coriolis = 'R3548'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0911492C' ,'b1ed114e-6c79-4526-8466-3a5376461bb0','R18509','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0911492C' ,code_coriolis = 'R18509'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0911401D' ,'5f8950d8-32b2-4279-b77b-3aa067dc7aae','R3333','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0911401D' ,code_coriolis = 'R3333'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0911353B' ,'4a11ac7d-f26e-409c-aac8-7d4271346d5f','R3347','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0911353B' ,code_coriolis = 'R3347'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0911346U' ,'27801107-2bbe-4fe2-b41b-5124ba0d2b80','R3550','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0911346U' ,code_coriolis = 'R3550'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0911343R' ,'12e7a27f-e6a3-431f-8a4e-93814e804f5c','R15795','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0911343R' ,code_coriolis = 'R15795'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0911254U' ,'afcfc766-5c7c-47ad-b01f-8a7eabc80eca','R3337','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0911254U' ,code_coriolis = 'R3337'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0911251R' ,'21e624e8-507e-4622-919e-1222972cf074','R3336','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0911251R' ,code_coriolis = 'R3336'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0911037H' ,'842209a4-0cfd-420a-9c7d-2711dc565105','R3545','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0911037H' ,code_coriolis = 'R3545'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0911021R' ,'bcbee570-dbc5-4fb1-b841-30de8c7d74c2','R3327','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0911021R' ,code_coriolis = 'R3327'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0910975R' ,'895fb9b0-594b-4164-b93b-38b6ae1376bd','R3714','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0910975R' ,code_coriolis = 'R3714'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0910756C' ,'5172b478-fd60-42ed-9a54-be0f9a1e3a4f','R3554','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0910756C' ,code_coriolis = 'R3554'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0910755B' ,'5af0e3be-8f3d-46b9-98ba-1e6a4002d334','R3332','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0910755B' ,code_coriolis = 'R3332'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0910727W' ,'b113009d-d242-467f-b32a-228039ea8d53','R3346','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0910727W' ,code_coriolis = 'R3346'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0910715H' ,'25fd8d0a-d66e-4b2d-b44c-0641ad225e62','R3343','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0910715H' ,code_coriolis = 'R3343'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0910687C' ,'4cf43171-368c-48d0-92ac-1e8964bfc71b','R3344','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0910687C' ,code_coriolis = 'R3344'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0910676R' ,'b78f59e9-61c7-4e35-9162-6f4e603c2c28','R3325','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0910676R' ,code_coriolis = 'R3325'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0910632T' ,'1069e0e8-d60d-47d0-873e-4b11fe6423ca','R3345','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0910632T' ,code_coriolis = 'R3345'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0910631S' ,'1ce9fda4-9915-40c6-bd87-535bec9e6dcc','R3338','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0910631S' ,code_coriolis = 'R3338'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0910630R' ,'31f1b0dd-4cb6-4ff5-a846-500c4bcd9ddd','R3339','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0910630R' ,code_coriolis = 'R3339'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0910629P' ,'3613fd8d-bea4-499b-8443-afdfd83508c1','R3335','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0910629P' ,code_coriolis = 'R3335'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0910628N' ,'aa46cfb6-e428-492e-9501-e86371722e2f','R3322','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0910628N',code_coriolis = 'R3322'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0910627M' ,'1c904453-00a7-4ad9-a9fa-5fcc3c01c434','R3552','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0910627M' ,code_coriolis = 'R3552'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0910626L' ,'02e23cc9-7184-4e6f-a648-287cd7e054dc','R3546','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0910626L' ,code_coriolis = 'R3546'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0910625K' ,'c6bd75c4-7552-42a5-8dda-f435b97f34fb','R3544','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0910625K' ,code_coriolis = 'R3544'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0910623H' ,'6784dc2f-e847-4127-85cf-31b7f161f34a','R3324','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0910623H' ,code_coriolis = 'R3324'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0910622G' ,'78d3eb49-36cf-4e72-84c5-053fecf65c1d','R3705','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0910622G' ,code_coriolis = 'R3705'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0910620E' ,'fe4d3718-e0df-43fb-82f1-2bea85c6fdd6','R3703','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0910620E' ,code_coriolis = 'R3703'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0910429X' ,'c2ee978d-480d-408e-8745-ad0d92b43719','R3555','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0910429X' ,code_coriolis = 'R3555'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0783533S' ,'b8dbe734-8a44-4d9e-b62a-08c86ecf6352','R3671','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0783533S' ,code_coriolis = 'R3671'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0783447Y' ,'06078f3d-84b2-4657-bf45-d66ce3eed98b','R3761','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0783447Y' ,code_coriolis = 'R3761'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0783431F' ,'4d6d5710-36e5-4a0f-91c8-97d79d8a3a05','R3755','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0783431F' ,code_coriolis = 'R3755'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0783259U' ,'229720ea-7425-487d-bac6-79dadea91cc2','R3307','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0783259U' ,code_coriolis = 'R3307'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0783214V' ,'c63e6cf7-091a-4a5c-9cf4-609174d8f8c7','R3303','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0783214V' ,code_coriolis = 'R3303'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0783213U' ,'8f61eb40-24cc-411b-968b-28e032ce4b5a','R3677','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0783213U' ,code_coriolis = 'R3677'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0783140P' ,'77f9bca7-01b1-4f72-ad75-df5b1dc985c5','R3312','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0783140P' ,code_coriolis = 'R3312'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0782924E' ,'eb99f538-f062-4019-a0a9-bc4deb3529cf','R3319','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0782924E' ,code_coriolis = 'R3319'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0782822U' ,'820800df-ef44-45c7-bb9e-ac9c34714c00','R3304','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0782822U' ,code_coriolis = 'R3304'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0782603F' ,'7e7a2b59-fa63-4058-ab67-28bfedb1b0ee','R3695','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0782603F' ,code_coriolis = 'R3695'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0782602E' ,'e21b5e96-21d5-4802-b53d-bc27a307fbad','R3306','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0782602E' ,code_coriolis = 'R3306'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0782593V' ,'0cf45340-bce6-4d53-b8b8-2c74af4dc532','R3314','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0782593V' ,code_coriolis = 'R3314'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0782587N' ,'34d871c6-61ad-4546-918a-05e36050885d','R3673','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0782587N' ,code_coriolis = 'R3673'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0782568T' ,'b5593552-f459-4281-9932-876dfe8fea5a','R3308','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0782568T' ,code_coriolis = 'R3308'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0782567S' ,'cda968fd-8703-40ad-9695-cb3870582068','R3694','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0782567S' ,code_coriolis = 'R3694'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0782565P' ,'06848880-f446-4674-957f-36dc33c0ec58','R3712','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0782565P' ,code_coriolis = 'R3712'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0782563M' ,'95f2997e-293e-4381-b06a-7722cfd56b38','R3693','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0782563M' ,code_coriolis = 'R3693'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0782557F' ,'f8a11d81-0981-408e-8c24-60f3fdb4c610','R3710','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0782557F' ,code_coriolis = 'R3710'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0782556E' ,'25b4925f-4632-4ac4-9641-00b38280be9f','R3317','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0782556E' ,code_coriolis = 'R3317'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0782549X' ,'68173ba6-ed79-4b35-b8b1-fcd571698f6b','R3313','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0782549X' ,code_coriolis = 'R3313'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0782540M' ,'44520bf0-3051-48fe-a41b-efc9eb0d7e0a','R3708','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0782540M' ,code_coriolis = 'R3708'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0782539L' ,'5a555de0-b1ff-48bf-8218-e21bc52437c5','R3669','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0782539L' ,code_coriolis = 'R3669'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0782132U' ,'7d3ad7a6-c065-4caa-bf2d-b2a15c4831e7','R3229','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0782132U' ,code_coriolis = 'R3229'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0781984H' ,'88a56897-09ed-4f51-a200-aed25f99e567','R3309','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0781984H' ,code_coriolis = 'R3309'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0781983G' ,'26264114-6d91-4310-8361-e35fc1ca66ac','R3675','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0781983G' ,code_coriolis = 'R3675'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0781952Y' ,'88a5a067-4914-4a24-95c0-98129527ba3d','R19646','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0781952Y' ,code_coriolis = 'R19646'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0781951X' ,'bbce869a-d4d0-45ee-b26d-2ec7afc39036','R16239','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0781951X' ,code_coriolis = 'R16239'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0781950W' ,'e7deb9e9-839e-46b5-bb64-5af658ad859a','R3718','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0781950W' ,code_coriolis = 'R3718'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0781949V' ,'cedfd730-7e27-475f-9478-ce0eb90fc12c','R19046','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0781949V' ,code_coriolis = 'R19046'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0781948U' ,'f5919cf5-7eac-438f-902b-7c705557e484','R3664','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0781948U' ,code_coriolis = 'R3664'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0781898P' ,'5079bc5d-f2fb-44e5-84c5-3c37db26cc3d','R18229','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0781898P' ,code_coriolis = 'R18229'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0781884Z' ,'0b2d7921-d369-4139-af5f-b87e66942822','R15875','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0781884Z' ,code_coriolis = 'R15875'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0781883Y' ,'ea975f9f-b345-417b-a05f-c15c2da92856','R15879','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0781883Y' ,code_coriolis = 'R15879'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0781861Z' ,'0fd4c17f-9fb6-4357-a443-6c4ca3c8a583','R8267','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0781861Z' ,code_coriolis = 'R8267'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0781860Y' ,'f34ecb81-20b5-42ac-b496-ec2772a31658','R3723','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0781860Y' ,code_coriolis = 'R3723'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0781859X' ,'69fc9523-4e9c-4014-9bd9-9933e20f2c31','R3722','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0781859X' ,code_coriolis = 'R3722'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0781845G' ,'9f9194c7-5956-4db9-86d1-283d224889a9','R3300','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0781845G' ,code_coriolis = 'R3300'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0781839A' ,'bce387c7-a7e1-406c-ada1-2dd50a52a9e5','R3305','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0781839A' ,code_coriolis = 'R3305'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0781819D' ,'af8e2777-ad16-4dcc-b72e-4b08845844d0','R18986','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0781819D' ,code_coriolis = 'R18986'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0781578S' ,'aa24196a-b316-4912-84da-2934a8d93bca','R3301','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0781578S' ,code_coriolis = 'R3301'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0781512V' ,'abbce198-93b7-461e-8f4a-28052f31399c','R3672','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0781512V' ,code_coriolis = 'R3672'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0781297L' ,'c70a2061-8020-4189-8f56-653e27393490','R3691','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0781297L' ,code_coriolis = 'R3691'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0780584L' ,'87868fb5-132d-453d-b282-ffb23de35431','R3690','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0780584L' ,code_coriolis = 'R3690'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0780582J' ,'3b9a3875-9c50-45af-81ca-885343729107','R3674','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0780582J' ,code_coriolis = 'R3674'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0780515L' ,'b2ddd672-7b5f-4e73-8333-6c026fbbe769','R3311','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0780515L' ,code_coriolis = 'R3311'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0780486E' ,'8a0eccd8-102d-4337-899d-b6378ac3f457','R3298','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0780486E' ,code_coriolis = 'R3298'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0780422K' ,'50f02fda-4847-49ed-a465-b4e2c58c76fe','R3310','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0780422K' ,code_coriolis = 'R3310'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0780273Y' ,'54c4196c-1f67-4eb0-9fc7-06c71e859907','R3689','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0780273Y' ,code_coriolis = 'R3689'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0780004F' ,'c4c8458c-f439-4815-8afa-29235666e073','R3665','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0780004F' ,code_coriolis = 'R3665'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0772751X' ,'e74b2374-f6f5-4f69-a1ef-d560bfeb93c9','P0023982','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0772751X' ,code_coriolis = 'P0023982'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0772688D' ,'437511c4-250f-4fe4-b562-301f84607d08','R3919','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0772688D' ,code_coriolis = 'R3919'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0772685A' ,'4981c3ed-2271-4809-a69f-9d64e6eb9213','R3983','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0772685A' ,code_coriolis = 'R3983'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0772342C' ,'0b49a6ea-5f1e-4452-ace2-5ff8ff18f02f','R3289','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0772342C' ,code_coriolis = 'R3289'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0772332S' ,'1abbbeb8-0ad7-4488-8078-659016996a65','R3205','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0772332S' ,code_coriolis = 'R3205'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0772312V' ,'9ee4b5d1-416a-4766-b27c-986d89e3e439','R3279','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0772312V' ,code_coriolis = 'R3279'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0772310T' ,'95e00f07-6430-4bce-bb3e-445934de2400','R3236','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0772310T' ,code_coriolis = 'R3236'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0772296C' ,'5e58ac91-34c6-4345-a408-7b02afbb3851','R19203','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0772296C' ,code_coriolis = 'R19203'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0772295B' ,'1168b583-ff88-4d02-b9a2-00b09769839e','R20737','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0772295B' ,code_coriolis = 'R20737'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0772294A' ,'6951dbed-9168-4d16-8b25-aee94392b434','R3235','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0772294A' ,code_coriolis = 'R3235'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0772292Y' ,'4d19d524-0dcb-4e71-9d5c-7568542bdf5f','R19061','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0772292Y' ,code_coriolis = 'R19061'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0772277G' ,'f846c4eb-00aa-4240-961f-17d3ad5403a5','R19205','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0772277G' ,code_coriolis = 'R19205'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0772276F' ,'eb246854-b07d-484e-8625-46567457c11e','R18914','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0772276F' ,code_coriolis = 'R18914'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0772244W' ,'6a84ceb3-dfbd-41dd-8a40-d97396cfbc07','R3286','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0772244W' ,code_coriolis = 'R3286'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0772243V' ,'163334fe-0da8-4eea-a615-14b1ae3f9b3e','R15273','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0772243V' ,code_coriolis = 'R15273'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0772230F' ,'44a949e5-5444-4b94-bbd7-47687b92330d','R15059','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0772230F' ,code_coriolis = 'R15059'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0772229E' ,'6ca7f89e-030e-4521-ae1b-790ba0e8ad4a','R15275','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0772229E' ,code_coriolis = 'R15275'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0772228D' ,'8b90ea25-14de-44c2-ab49-cce45b31157d','R15277','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0772228D' ,code_coriolis = 'R15277'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0772225A' ,'e41a8c41-c262-4f97-8382-2168bda08c65','R3203','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0772225A' ,code_coriolis = 'R3203'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0772223Y' ,'cac5d61c-81ea-4449-84a9-6fdf0b942754','R15359','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0772223Y' ,code_coriolis = 'R15359'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0772188K' ,'59de2cdc-eb83-4195-9ca9-3d7cbb7720d2','R3108','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0772188K' ,code_coriolis = 'R3108'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0772127U' ,'5bfe59d5-b582-4edd-b2b0-0c1700fcf811','R3257','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0772127U' ,code_coriolis = 'R3257'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0772120L' ,'68b81cd2-235c-436c-a457-b9036633d818','R3287','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0772120L' ,code_coriolis = 'R3287'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0771997C' ,'27a76005-50d6-4fc5-ab28-476ab75e3253','R3256','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0771997C' ,code_coriolis = 'R3256'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0771996B' ,'fef539a6-88b4-4003-acfd-27a3c032a4fd','R3276','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0771996B' ,code_coriolis = 'R3276'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0771995A' ,'87f40e36-70a6-46e0-accb-07e53038f5a3','R3255','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0771995A' ,code_coriolis = 'R3255'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0771941S' ,'1ae7160e-5f8f-4c0a-afde-1ea33a6d3bf3','R3267','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0771941S' ,code_coriolis = 'R3267'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0771940R' ,'648fbac5-b007-4504-806f-9a0b880eda21','R3268','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0771940R' ,code_coriolis = 'R3268'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0771880A' ,'ab5fb6f1-84cb-43d3-be88-b25d7603e1b9','R3271','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0771880A' ,code_coriolis = 'R3271'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0771763Y' ,'ebf75677-df43-4c88-a835-4fbf3282d76e','R3283','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0771763Y' ,code_coriolis = 'R3283'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0771663P' ,'bcf2682e-724b-4ab6-96d4-50237500c5a1','R3266','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0771663P' ,code_coriolis = 'R3266'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0771658J' ,'e0b1a8e9-3b33-4156-8cc4-e9f18dfe9872','R3258','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0771658J' ,code_coriolis = 'R3258'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0771512A' ,'74c4f358-178a-4df2-9f3a-9b5e3bb798de','R3264','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0771512A' ,code_coriolis = 'R3264'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0771436T' ,'1ebd15cf-dd2c-4f7a-a167-ebece5b699ba','R3668','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0771436T' ,code_coriolis = 'R3668'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0771357G' ,'08584019-8611-43fc-a627-1720b109f227','R3667','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0771357G' ,code_coriolis = 'R3667'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0771336J' ,'6c2f279c-4645-4e99-9454-134e9563304c','R3282','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0771336J' ,code_coriolis = 'R3282'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0771171E' ,'ef5b2be0-67b8-431e-9903-a14aadd252a2','R3699','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0771171E' ,code_coriolis = 'R3699'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0771027Y' ,'5ee4afb8-24c3-4b65-881b-90ebdf67113e','R3261','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0771027Y' ,code_coriolis = 'R3261'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0770945J' ,'2b694d36-e6c0-461b-9a2c-4a63ca97b4e1','R3290','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0770945J' ,code_coriolis = 'R3290'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0770944H' ,'caed1bd1-eb0e-429c-b374-79b9c6d0ea96','R3285','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0770944H' ,code_coriolis = 'R3285'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0770943G' ,'28f25e7d-7742-4560-ad6c-331b378f1269','R3265','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0770943G' ,code_coriolis = 'R3265'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0770942F' ,'ec8a4d89-cbd0-4c96-b1d1-dc53c874d977','R3715','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0770942F' ,code_coriolis = 'R3715'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0770940D' ,'9f8f2fd0-a69e-4780-b27a-9fef4bd6a165','R3704','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0770940D' ,code_coriolis = 'R3704'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0770938B' ,'6fd945de-e432-4ce4-bcc5-85cec62816b0','R18497','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0770938B' ,code_coriolis = 'R18497'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0770934X' ,'fd330b1c-b5c3-4b69-929a-f1ccefbc31fb','R3273','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0770934X' ,code_coriolis = 'R3273'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0770933W' ,'c8c15488-e507-4568-b97d-43dbe4a2b7c7','R3272','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0770933W' ,code_coriolis = 'R3272'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0770932V' ,'25548b0c-12a2-4af1-872c-80f975fb5efc','R3269','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0770932V' ,code_coriolis = 'R3269'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0770931U' ,'b2174059-fcbd-4be8-9b7b-2db93ed3c4c4','R18495','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0770931U' ,code_coriolis = 'R18495'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0770930T' ,'ed4f079c-6ab4-410f-b5e5-baa5501cdc4f','R3270','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0770930T' ,code_coriolis = 'R1579R32709'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0770927P' ,'87f2dd01-0f62-4ebf-bf15-50e5a9fb6471','R3263','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0770927P' ,code_coriolis = 'R3263'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0770926N' ,'af84d1ac-53af-48dc-a0d5-fa3759c62361','R3262','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0770926N' ,code_coriolis = 'R3262'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0770924L' ,'2a378bcc-610f-41ec-ba12-f79b305d6938','R3259','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0770924L' ,code_coriolis = 'R3259'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0770922J' ,'303962e5-5850-43f9-a00c-6e46c49e6be3','R3254','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0770922J' ,code_coriolis = 'R3254'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0770920G' ,'a9a5d178-febf-4bca-8c3a-3b09d829e5ed','R3252','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0770920G' ,code_coriolis = 'R3252'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0770918E' ,'459d8832-7802-4507-9eaf-1dfa68da0150','R3250','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0770918E' ,code_coriolis = 'R3250'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0770687D' ,'770824fa-67cf-48df-afa9-618f08088ce3','R3284','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0770687D' ,code_coriolis = 'R3284'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0770342D' ,'90dee713-6804-4647-93a3-76cb460cc34e','R3251','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0770342D' ,code_coriolis = 'R3251'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0754684J' ,'0bc39376-74a7-4af0-9cee-ab94a63049d6','R3210','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0754684J' ,code_coriolis = 'R3210'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0754530S' ,'cc7dac72-d27b-404a-bb73-893c6921b0ac','R3362','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0754530S' ,code_coriolis = 'R3362'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0754476H' ,'bd07eca9-29c8-4dd1-8c96-2c1e026dc5a6','R3357','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0754476H' ,code_coriolis = 'R3357'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0754475G' ,'b9795ece-473f-4bc0-a20c-74b0358fb48f','R3460','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0754475G' ,code_coriolis = 'R3460'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0753268V' ,'c5900f34-6c20-4506-bdd0-7f999ff15b92','R3444','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0753268V' ,code_coriolis = 'R3444'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0753256G' ,'b39524ad-c072-41e8-8980-2e530063f025','R3368','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0753256G' ,code_coriolis = 'R3368'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0752961L' ,'b209c740-98c3-47d6-99f5-6b3f3bd3c84f','R3420','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0752961L' ,code_coriolis = 'R3420'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0752846L' ,'324009bf-e411-4c42-a500-60d71dc354b9','R3459','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0752846L' ,code_coriolis = 'R3459'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0752799K' ,'c35c8808-6a71-429b-95bb-fc8c694a6021','R3366','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0752799K' ,code_coriolis = 'R3366'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0752700C' ,'ed853e2f-f272-4699-9944-9c9f99441d41','R3353','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0752700C' ,code_coriolis = 'R3353'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0752608C' ,'7a3555d0-c40a-472a-95fa-d0635e5bb403','R3349','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0752608C' ,code_coriolis = 'R3349'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0751710B' ,'b59c8a06-7350-4068-8001-f1a878585f57','R3451','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0751710B' ,code_coriolis = 'R3451'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0751708Z' ,'f44f99d1-62ec-4d92-b164-30c796b5d1a2','R3383','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0751708Z' ,code_coriolis = 'R3383'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750828T' ,'1322fe76-9782-4f80-b590-97a17a1cf24a','R3698','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750828T' ,code_coriolis = 'R3698'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750808W' ,'614e8849-c1d0-463e-88a4-416eef70691d','R3360','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750808W' ,code_coriolis = 'R3360'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750802P' ,'24ec1014-692e-44de-8cec-982b777331d8','R3355','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750802P' ,code_coriolis = 'R3355'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750800M' ,'81cf45de-923c-44f2-9613-148fa639457b','R3378','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750800M' ,code_coriolis = 'R3378'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750796H' ,'f6897021-350e-4715-b556-df6f4a640f74','R3369','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750796H' ,code_coriolis = 'R3369'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750794F' ,'895b7719-22dd-4e7f-911c-3161efda32d3','R3461','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750794F' ,code_coriolis = 'R3461'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750793E' ,'cce72076-9371-4262-9508-1f69417f5601','R3380','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750793E' ,code_coriolis = 'R3380'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750788Z' ,'ed377f3d-523d-492e-ac0e-9c1ff048e41f','R3437','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750788Z' ,code_coriolis = 'R3437'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750787Y' ,'d225c0c2-a70e-44c9-8e74-a7577db7166c','R3449','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750787Y' ,code_coriolis = 'R3449'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750785W' ,'b0f97762-0c9c-4cc2-bc2a-a6333ffccf2c','R3447','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750785W' ,code_coriolis = 'R3447'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750784V' ,'7c9fbfd8-b17e-4d0a-b3a3-2c84887305af','R3442','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750784V' ,code_coriolis = 'R3442'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750783U' ,'dd8b4c91-3c68-4a26-96ac-d1698291857f','R3441','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750783U' ,code_coriolis = 'R3441'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750778N' ,'306ce64c-5652-41cf-bc7c-2b8111be0b7d','R3436','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750778N' ,code_coriolis = 'R3436'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750776L' ,'7f4322ae-47b1-4b3e-905f-2750f9769220','R3431','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750776L' ,code_coriolis = 'R3431'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750775K' ,'e006cb12-5001-4a2c-b596-ffdfc8faad65','R3432','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750775K' ,code_coriolis = 'R3432'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750770E' ,'8199c2a4-d972-4c06-af0b-12929c07bc4a','R3409','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750770E' ,code_coriolis = 'R3409'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750712S' ,'0f851f90-9dff-47d0-979e-0feec63db6b6','R3350','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750712S' ,code_coriolis = 'R3350'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750710P' ,'8cbb5600-5e5a-425c-9936-46b7b86f1e09','R3379','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750710P' ,code_coriolis = 'R3379'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750708M' ,'4c000d3b-5654-49b1-8546-64d2dab65391','R3800','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750708M' ,code_coriolis = 'R3800'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750707L' ,'08cc7a51-3296-4924-b163-55a2d96749f9','R3370','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750707L' ,code_coriolis = 'R3370'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750697A' ,'b3b6dc6a-026d-4c81-b675-4effcb872a5a','R3759','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750697A' ,code_coriolis = 'R3759'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750696Z' ,'4697e5d3-fd9b-403a-9264-7456a9f8b245','R3381','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750696Z' ,code_coriolis = 'R3381'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750695Y' ,'7e85b604-0d5b-4d58-ac11-c53e696101b9','R3385','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750695Y' ,code_coriolis = 'R3385'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750692V' ,'f8a8c49e-8412-45f8-953d-9f1355fb678e','R3454','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750692V' ,code_coriolis = 'R3454'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750691U' ,'1ee0140a-b977-45b4-8a89-e9faab74728b','R3453','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750691U' ,code_coriolis = 'R3453'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750688R' ,'644b6e78-3901-4de6-92c5-9c8f6db97b75','R3376','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750688R' ,code_coriolis = 'R3376'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750685M' ,'9d593ecf-1cb3-42af-abba-d4c9b2341f00','R3364','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750685M' ,code_coriolis = 'R3364'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750680G' ,'03b4f702-c410-41cd-82bf-cf03d0b62188','R3438','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750680G' ,code_coriolis = 'R3438'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750677D' ,'7c9ed049-1120-46c1-bbba-6e61ebf30de5','R3440','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750677D' ,code_coriolis = 'R3440'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750676C' ,'be222c2f-153a-43a7-b62d-84182d83e3d0','R3433','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750676C' ,code_coriolis = 'R3433'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750674A' ,'dfb94489-b350-4724-8c39-7e5d73f17de0','R3430','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750674A' ,code_coriolis = 'R3430'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750673Z' ,'1586174c-5fb1-4136-9ad3-552bf40f4d9c','R3428','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750673Z' ,code_coriolis = 'R3428'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750671X' ,'3431bbee-00a6-4f54-944b-4277b597bbed','R3424','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750671X' ,code_coriolis = 'R3424'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750667T' ,'c8809126-e96f-4693-9b4c-f080ea1513ba','R3423','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750667T' ,code_coriolis = 'R3423'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750664P' ,'31ab6052-6931-430e-9f11-c93db8ea406d','R3421','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750664P' ,code_coriolis = 'R3421'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750660K' ,'e7304353-626d-4fcd-b68a-c9304aeb7b6f','R3417','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750660K' ,code_coriolis = 'R3417'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750658H' ,'5d2d8bfb-09be-49dd-af9e-7baf6343ad6e','R3416','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750658H' ,code_coriolis = 'R3416'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750655E' ,'1a2707e9-f5cc-4eab-babf-03d82c956323','R3413','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750655E' ,code_coriolis = 'R3413'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750653C' ,'6d513dab-ee0b-4656-89da-05db416d96ff','R3411','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750653C' ,code_coriolis = 'R3411'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750651A' ,'fe5468fb-e260-4160-9401-e5bf17d76b67','R3408','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750651A' ,code_coriolis = 'R3408'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750650Z' ,'4abd82fe-9da1-4964-adcf-2a74345ee6d5','R3354','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750650Z' ,code_coriolis = 'R3354'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750647W' ,'b4cdc9d4-03a0-4ff3-b57a-9ce31821e9e1','R3406','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750647W' ,code_coriolis = 'R3406'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750612H' ,'9771c193-2c95-4cbe-94c0-61d925b9190a','R3367','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750612H' ,code_coriolis = 'R3367'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0750558Z' ,'003c3133-9470-409d-8564-a177d492209d','R3230','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0750558Z' ,code_coriolis = 'R3230'
 WHERE specific_structures.id = EXCLUDED.id ;
INSERT INTO lystore.specific_structures(
	uai, id,   code_coriolis, type)
	VALUES (  '0780000C' ,'c3f3fbd7-26b5-443d-b8a3-07df3460716b','R2896','LYC')
 ON CONFLICT (id) DO
  UPDATE SET uai='0780000C',type='LYC',code_coriolis = 'R2896'
 WHERE specific_structures.id = EXCLUDED.id ;