@echo off
cd /d "%~dp0"
set SERVER_FORWARD_HEADERS_STRATEGY=framework
set RESOLVED_EUREKA_URL=http://localhost:8761/eureka

echo Starting user-service on 24800...
start "user-service" /B java -jar user-service\target\user-service.jar --server.port=24800 --eureka.client.service-url.defaultZone=%RESOLVED_EUREKA_URL% >> user-service.log 2>&1
timeout /t 5 /nobreak > NUL

echo Starting hall-service on 26444...
start "hall-service" /B java -jar hall-service\target\hall-service.jar --server.port=26444 --eureka.client.service-url.defaultZone=%RESOLVED_EUREKA_URL% >> hall-service.log 2>&1
timeout /t 5 /nobreak > NUL

echo Starting vendor-service on 25485...
start "vendor-service" /B java -jar vendor-service\target\vendor-service.jar --server.port=25485 --eureka.client.service-url.defaultZone=%RESOLVED_EUREKA_URL% >> vendor-service.log 2>&1
timeout /t 5 /nobreak > NUL

echo Starting proposal-service on 27035...
start "proposal-service" /B java -jar proposal-service\target\proposal-service.jar --server.port=27035 --eureka.client.service-url.defaultZone=%RESOLVED_EUREKA_URL% >> proposal-service.log 2>&1
timeout /t 5 /nobreak > NUL

echo Starting event-service on 23021...
start "event-service" /B java -jar event-service\target\event-service.jar --server.port=23021 --eureka.client.service-url.defaultZone=%RESOLVED_EUREKA_URL% >> event-service.log 2>&1
timeout /t 5 /nobreak > NUL

echo Starting gateway-service on 27193 (foreground)...
java -jar gateway-service\target\gateway-service.jar --server.port=27193 --eureka.client.service-url.defaultZone=%RESOLVED_EUREKA_URL%
