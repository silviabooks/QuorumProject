Refecence: https://hub.docker.com/r/trion/ng-cli/


* Scaricare la docker image: `docker pull trion/ng-cli`
* Buildare l'app in ./my-app: `docker run -u $(id -u) --rm -v "$PWD":/app trion/ng-cli ng build`
* Runnare l'analyzer con: `docker run -u $(id -u) --rm -p 4200:4200 -v "$PWD":/app trion/ng-cli ng serve -host 0.0.0.0`

