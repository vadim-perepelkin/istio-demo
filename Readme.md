# Демо по Service mesh на примере Istio
## Настройка окружения
### Необходимые компоненты:
- [Minikube 1.37.0](https://github.com/kubernetes/minikube/releases/v1.37.0)
- [Istio 1.28.3](https://github.com/istio/istio/releases/tag/1.28.3)

При установке других версий имеет смысл проверить совместимость версий: https://istio.io/latest/docs/releases/supported-releases/

### Установка и запуск Minikube с Istio:
1. Устанавливаем Minikube
2. Запускаем Minikube (если используются другие версии Minikube и Istio, то версию k8s нужно будет изменить): `minikube start --cpus=4 --memory=8g --kubernetes-version=v1.34.0`
3. Устанавливаем Istio: `istioctl install --set profile=demo -y`
4. Из папки с Istio устанавливаем компоненты:\
   `kubectl apply \` \
   `-f samples/addons/kiali.yaml \` \
   `-f samples/addons/prometheus.yaml \` \
   `-f samples/addons/grafana.yaml \` \
   `-f samples/addons/jaeger.yaml`
5. Включаем трейсинг всех сообщений: `kubectl apply -f Tracing/telemetry.yaml`

### Подготовка к запуску примеров:
1. Создаем сетевой туннель с хостовой машины до Istio Ingress, для этого в отдельной сессии: `sudo minikube tunnel`
2. Настраиваем проброс портов для Jaeger, для этого в отдельной сессии: `minikube service tracing -n istio-system`, открываем в браузере полученный url
3. Создаем namespace, в котором будем разворачивать объекты: `kubectl create namespace hello`
4. Разрешаем использование Istio в namespace: `kubectl label namespace hello istio-injection=enabled`
5. Собираем HelloService: `minikube image build -t hello-service:1.0 .`
6. `minikube image ls` проверяем, что внутри Minikube появился образ `docker.io/library/hello-service:1.0`


## Публикация hello-service через Gateway
1. Удаляем все объекты из namespace hello: \
   `kubectl delete deployment --all -n hello && \` \
   `kubectl delete service --all -n hello && \` \
   `kubectl delete virtualservice --all -n hello && \` \
   `kubectl delete destinationrule --all -n hello && \` \
   `kubectl delete gateway --all -n hello`
2. Разворачиваем компоненты:\
   `kubectl apply \` \
   `-f Basic/deployment.yaml \` \
   `-f Basic/service.yaml \` \
   `-f Basic/gateway.yaml \` \
   `-f Basic/virtual-service.yaml \` \
   `-n hello`
3. Выполняем `curl http://localhost:80/api/hello`, запрос попадет на Istio Ingress и маршрутизируется в сервис hello-service, сервис должен вернуть Hello world
4. Смотрим в Jaeger трейсы вызова hello service
5. Запускаем еще один контейнер `kubectl run -it --rm -n hello --image=alpine/curl client -- sh`, выполняем `curl http://hello-srv:8080/api/hello`, смотрим трейсы в Jaeger
6. Смотрим Grafana и Kiali (нужно получить url через команды `minikube service ...` аналогично Jaeger)


## Ретраи
1. Удаляем все объекты из namespace hello: \
   `kubectl delete deployment --all -n hello && \` \
   `kubectl delete service --all -n hello && \` \
   `kubectl delete virtualservice --all -n hello && \` \
   `kubectl delete destinationrule --all -n hello && \` \
   `kubectl delete gateway --all -n hello`
2. Разворачиваем компоненты:\
   `kubectl apply \` \
   `-f Basic/deployment.yaml \` \
   `-f Basic/service.yaml \` \
   `-f Basic/gateway.yaml \` \
   `-f Basic/virtual-service.yaml \` \
   `-n hello`
3. Несколько раз выполняем `curl -I http://localhost:80/api/hello-with-errors`, сервис отвечает с кодом 200 только в 20% случаев, с вероятностью 80% вернется код 500
4. Удаляем virtual service: `kubectl delete virtualservice hello-vs -n hello`
5. Разворачиваем virtual service с настроенными ретраями: `kubectl apply -f Basic/virtual-service-with-retry.yaml -n hello`
6. Несколько раз выполняем `curl -I http://localhost:80/api/hello-with-errors`, убеждаемся, что ошибка 500 стала возникать намного реже
7. Смотрим в Jaeger трейсы, убеждаемся, что метод /api/hello-with-errors все также часто отвечает 500-м кодом, но отрабатывает логика повторных запросов


## Канареичные релизы
1. Удаляем все объекты из namespace hello: \
   `kubectl delete deployment --all -n hello && \` \
   `kubectl delete service --all -n hello && \` \
   `kubectl delete virtualservice --all -n hello && \` \
   `kubectl delete destinationrule --all -n hello && \` \
   `kubectl delete gateway --all -n hello`
2. Разворачиваем компоненты:\
   `kubectl apply \` \
   `-f Canary/deployment-v1.yaml \` \
   `-f Canary/deployment-v2.yaml \` \
   `-f Canary/service.yaml \` \
   `-f Canary/destination-rule.yaml \` \
   `-f Canary/gateway.yaml \` \
   `-f Canary/virtual-service.yaml \` \
   `-n hello`
3. Несколько раз выполняем `curl http://localhost:80/api/hello-with-version`, Istio 80% траффика отправляет на hello-service версии v1.0, 20% на hello-service версии v2.0, убеждаемся в этом по ответу сервиса
4. Удаляем virtual service: `kubectl delete virtualservice hello-vs -n hello`
5. Разворачиваем virtual service с роутингом по хедеру: `kubectl apply -f Canary/virtual-service-route-by-header.yaml -n hello`
6. Выполняем `curl -H "x-version: v1" http://localhost:80/api/hello-with-version` с разными значениями хедера x-version, убеждаемся, что запрос попадает на нужную версию сервиса


## Istio под капотом
1. Удаляем все объекты из namespace hello: \
   `kubectl delete deployment --all -n hello && \` \
   `kubectl delete service --all -n hello && \` \
   `kubectl delete virtualservice --all -n hello && \` \
   `kubectl delete destinationrule --all -n hello && \` \
   `kubectl delete gateway --all -n hello`
2. Разворачиваем компоненты:\
   `kubectl apply \` \
   `-f Basic/deployment.yaml \` \
   `-f Basic/service.yaml \` \
   `-f Basic/gateway.yaml \` \
   `-f Basic/virtual-service.yaml \` \
   `-n hello`
3. Смотрим конфигурацию пода: `kubectl describe pod -l app=hello-service -n hello`
4. Смотрим логи прокси: `kubectl logs -l app=hello-service -c istio-proxy -n hello`
5. Смотрим, как настроен iptables в поде для перехвата траффика:
   1. Заходим внутрь Minikube `minikube ssh`, получаем PID процесса, с которым запущен контейнер с hello-service `ps -ef | grep HelloService.jar`
   2. Смотрим настройки iptables `nsenter -t <PID> -n iptables -t nat -L -n -v` (отдельный network namespace пода), см. редирект траффика на порты 15001 и 15006