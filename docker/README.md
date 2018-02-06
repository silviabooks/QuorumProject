## Docker Compose
Permette di avviare contemporaneamente più container, tra cui le repliche di MariaDB (solo usa per adesso) e RabbitMQ.
### Avviare docker-compose:
- Installare [docker](https://docs.docker.com/install/linux/docker-ce/ubuntu/) e [docker-compose](https://docs.docker.com/compose/install/).
- Posizionarsi nella cartella /docker e dare `docker-compose up` per avviare i container.
- Una volta terminati, rimuovere i container con il comando:
`docker-compose rm` oppure riavviarli con `docker-compose restart`

