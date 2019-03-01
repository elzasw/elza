Obecný popis
------------
${basePath}/${repositoryIdentifier}/external-systems-config.yaml
${basePath}/${repositoryIdentifier}/digi-requests/{requestIdentifier}.yaml
${basePath}/${repositoryIdentifier}/packages/{packageIdentifier}/package-config.yaml
${basePath}/${repositoryIdentifier}/packages/{packageIdentifier}/{daoIdentifier}/dao-config.yaml

${basePath}/${repositoryIdentifier}/packages/{packageIdentifier}/{daoIdentifier}/deleted
${basePath}/${repositoryIdentifier}/destr-requests/{requestIdentifier}/request-info.yaml
${basePath}/${repositoryIdentifier}/trans-requests/{requestIdentifier}/request-info.yaml
requestIdentifier - timestamp
deleted - destruction-requests/{requestIdentifier} || transfer-requests/{requestIdentifier}


Vzorová konfigurace serveru
---------------------------
Uveden je vždy název souboru, včetně cesty a jeho vzorový obsah.

./storage/repo/external-systems-config.yaml
digilinka-local: http://localhost:8080/services/

./storage/repo/digi-requests/1544537470675.xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<digitizationRequestInfo status="FINISHED" identifier="1963f757-ebc1-4083-bcf5-8b36c30e5cf1" systemIdentifier="digilinka-local">
    <description>koment 1</description>
    <materials>
        <did identifier="1e84d826-8afe-477b-b27c-a93904034de8"/>
        <did identifier="b4924ba2-8a54-4d0a-822f-86291a9091cf"/>
    </materials>
</digitizationRequestInfo>

./storage/repo/packages/package01/package-config.yaml
fundIdentifier: d19d16d6-db1c-4015-9fb7-45f15863d3a4
batchIdentifier: 1
batchLabel: "batchLabel1"

./storage/repo/packages/package01/daoid_v_ulozisti_1/dao-config.yaml
label: "objekt 1"
didIdentifier: "1e84d826-8afe-477b-b27c-a93904034de8"

./storage/repo/packages/package01/daoid_v_ulozisti_1/image01.jpg
./storage/repo/packages/package01/daoid_v_ulozisti_1/image02.jpg

./storage/repo/packages/package01/daoid_v_ulozisti_2/dao-config.yaml
label: "objekt 2"
didIdentifier: "b4924ba2-8a54-4d0a-822f-86291a9091cf"

./storage/repo/packages/package01/daoid_v_ulozisti_2/image03.jpg