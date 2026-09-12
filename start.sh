#!/bin/bash
cd "$(dirname "$0")"
export SERVER_FORWARD_HEADERS_STRATEGY=framework
RESOLVED_EUREKA_URL="http://localhost:8761/eureka"  # SHARED and already running — do not start it here.

# ---------- user-service ----------
USER_SERVICE_PORT="${USER_SERVICE_PORT:-24800}"
EXISTING_PID=$(pgrep -f -- "--server.port=$USER_SERVICE_PORT" || true)
if [ -n "$EXISTING_PID" ]; then
  echo "user-service: killing existing PID $EXISTING_PID on port $USER_SERVICE_PORT" >> user-service.log
  kill $EXISTING_PID 2>/dev/null || true
  for i in 1 2 3 4 5 6 7 8 9 10; do
    bash -c "exec 3<>/dev/tcp/127.0.0.1/$USER_SERVICE_PORT" 2>/dev/null || break
    sleep 1
  done
  if bash -c "exec 3<>/dev/tcp/127.0.0.1/$USER_SERVICE_PORT" 2>/dev/null; then
    echo "user-service: still bound after 10s, force killing" >> user-service.log
    kill -9 $EXISTING_PID 2>/dev/null || true
    sleep 1
  fi
fi
nohup java -jar user-service/target/*.jar --server.port=$USER_SERVICE_PORT --eureka.client.service-url.defaultZone=$RESOLVED_EUREKA_URL >> user-service.log 2>&1 &
sleep 5

# ---------- hall-service ----------
HALL_SERVICE_PORT="${HALL_SERVICE_PORT:-26444}"
EXISTING_PID=$(pgrep -f -- "--server.port=$HALL_SERVICE_PORT" || true)
if [ -n "$EXISTING_PID" ]; then
  echo "hall-service: killing existing PID $EXISTING_PID on port $HALL_SERVICE_PORT" >> hall-service.log
  kill $EXISTING_PID 2>/dev/null || true
  for i in 1 2 3 4 5 6 7 8 9 10; do
    bash -c "exec 3<>/dev/tcp/127.0.0.1/$HALL_SERVICE_PORT" 2>/dev/null || break
    sleep 1
  done
  if bash -c "exec 3<>/dev/tcp/127.0.0.1/$HALL_SERVICE_PORT" 2>/dev/null; then
    echo "hall-service: still bound after 10s, force killing" >> hall-service.log
    kill -9 $EXISTING_PID 2>/dev/null || true
    sleep 1
  fi
fi
nohup java -jar hall-service/target/*.jar --server.port=$HALL_SERVICE_PORT --eureka.client.service-url.defaultZone=$RESOLVED_EUREKA_URL >> hall-service.log 2>&1 &
sleep 5

# ---------- vendor-service ----------
VENDOR_SERVICE_PORT="${VENDOR_SERVICE_PORT:-25485}"
EXISTING_PID=$(pgrep -f -- "--server.port=$VENDOR_SERVICE_PORT" || true)
if [ -n "$EXISTING_PID" ]; then
  echo "vendor-service: killing existing PID $EXISTING_PID on port $VENDOR_SERVICE_PORT" >> vendor-service.log
  kill $EXISTING_PID 2>/dev/null || true
  for i in 1 2 3 4 5 6 7 8 9 10; do
    bash -c "exec 3<>/dev/tcp/127.0.0.1/$VENDOR_SERVICE_PORT" 2>/dev/null || break
    sleep 1
  done
  if bash -c "exec 3<>/dev/tcp/127.0.0.1/$VENDOR_SERVICE_PORT" 2>/dev/null; then
    echo "vendor-service: still bound after 10s, force killing" >> vendor-service.log
    kill -9 $EXISTING_PID 2>/dev/null || true
    sleep 1
  fi
fi
nohup java -jar vendor-service/target/*.jar --server.port=$VENDOR_SERVICE_PORT --eureka.client.service-url.defaultZone=$RESOLVED_EUREKA_URL >> vendor-service.log 2>&1 &
sleep 5

# ---------- proposal-service ----------
PROPOSAL_SERVICE_PORT="${PROPOSAL_SERVICE_PORT:-27035}"
EXISTING_PID=$(pgrep -f -- "--server.port=$PROPOSAL_SERVICE_PORT" || true)
if [ -n "$EXISTING_PID" ]; then
  echo "proposal-service: killing existing PID $EXISTING_PID on port $PROPOSAL_SERVICE_PORT" >> proposal-service.log
  kill $EXISTING_PID 2>/dev/null || true
  for i in 1 2 3 4 5 6 7 8 9 10; do
    bash -c "exec 3<>/dev/tcp/127.0.0.1/$PROPOSAL_SERVICE_PORT" 2>/dev/null || break
    sleep 1
  done
  if bash -c "exec 3<>/dev/tcp/127.0.0.1/$PROPOSAL_SERVICE_PORT" 2>/dev/null; then
    echo "proposal-service: still bound after 10s, force killing" >> proposal-service.log
    kill -9 $EXISTING_PID 2>/dev/null || true
    sleep 1
  fi
fi
nohup java -jar proposal-service/target/*.jar --server.port=$PROPOSAL_SERVICE_PORT --eureka.client.service-url.defaultZone=$RESOLVED_EUREKA_URL >> proposal-service.log 2>&1 &
sleep 5

# ---------- event-service ----------
EVENT_SERVICE_PORT="${EVENT_SERVICE_PORT:-23021}"
EXISTING_PID=$(pgrep -f -- "--server.port=$EVENT_SERVICE_PORT" || true)
if [ -n "$EXISTING_PID" ]; then
  echo "event-service: killing existing PID $EXISTING_PID on port $EVENT_SERVICE_PORT" >> event-service.log
  kill $EXISTING_PID 2>/dev/null || true
  for i in 1 2 3 4 5 6 7 8 9 10; do
    bash -c "exec 3<>/dev/tcp/127.0.0.1/$EVENT_SERVICE_PORT" 2>/dev/null || break
    sleep 1
  done
  if bash -c "exec 3<>/dev/tcp/127.0.0.1/$EVENT_SERVICE_PORT" 2>/dev/null; then
    echo "event-service: still bound after 10s, force killing" >> event-service.log
    kill -9 $EXISTING_PID 2>/dev/null || true
    sleep 1
  fi
fi
nohup java -jar event-service/target/*.jar --server.port=$EVENT_SERVICE_PORT --eureka.client.service-url.defaultZone=$RESOLVED_EUREKA_URL >> event-service.log 2>&1 &
sleep 5

# ---------- gateway-service (last, foreground) ----------
GATEWAY_PORT="${SERVER_PORT:-27193}"
EXISTING_PID=$(pgrep -f -- "--server.port=$GATEWAY_PORT" || true)
if [ -n "$EXISTING_PID" ]; then
  echo "gateway-service: killing existing PID $EXISTING_PID on port ${GATEWAY_PORT}" >> gateway-service.log
  kill $EXISTING_PID 2>/dev/null || true
  for i in 1 2 3 4 5 6 7 8 9 10; do
    bash -c "exec 3<>/dev/tcp/127.0.0.1/$GATEWAY_PORT" 2>/dev/null || break
    sleep 1
  done
  if bash -c "exec 3<>/dev/tcp/127.0.0.1/$GATEWAY_PORT" 2>/dev/null; then
    echo "gateway-service: still bound after 10s, force killing" >> gateway-service.log
    kill -9 $EXISTING_PID 2>/dev/null || true
    sleep 1
  fi
fi
exec java -jar gateway-service/target/*.jar --server.port=$GATEWAY_PORT --eureka.client.service-url.defaultZone=$RESOLVED_EUREKA_URL
