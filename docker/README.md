## Docker Compose
Permette di avviare contemporaneamente più container, tra cui le repliche di MariaDB (solo usa per adesso) e RabbitMQ.
### Avviare docker-compose:
- Installare [docker](https://docs.docker.com/install/linux/docker-ce/ubuntu/) e [docker-compose](https://docs.docker.com/compose/install/).
- Posizionarsi nella cartella /docker e dare `docker-compose up` per avviare i container.
------
Al primo avvio dei container, è necessario creare la tabella LOG. Per fare ciò, è necessario entrare nel container di mariadb con il comando `docker exec -it <ID_CONTAINER> bash` e creare la tabella utilizzando i seguenti comandi:
```
# mysql -u root -proot
> use ELENCO;
> create TABLE LOG(timestamp TIMESTAMP, idMacchina VARCHAR(255), message VARCHAR(255));
```


