#!/bin/bash
set -e

SERVER_IP="39.97.158.22"
SERVER_PORT="22"
SERVER_USER="root"
SERVER_PASS="wangmeng0902++"
DB_URL="jdbc:mysql://rm-uf6abp6renk8g3l2wio.mysql.rds.aliyuncs.com:3306/huiqitong_erp?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
DB_USER="root"
DB_PASS="f5mF2hKiOkbxKqs5"

echo "Building backend..."
mvn clean package -DskipTests -f backend/pom.xml

echo "Building frontend..."
cd frontend && npm install && npm run build && cd ..

echo "Deploying to server..."
sshpass -p "${SERVER_PASS}" ssh -o StrictHostKeyChecking=no -p ${SERVER_PORT} ${SERVER_USER}@${SERVER_IP} "mkdir -p /opt/wmdb/backend /opt/wmdb/frontend"

echo "Copying backend jar..."
sshpass -p "${SERVER_PASS}" scp -o StrictHostKeyChecking=no -P ${SERVER_PORT} backend/target/wmdb-backend-1.0.0-SNAPSHOT.jar ${SERVER_USER}@${SERVER_IP}:/opt/wmdb/backend/

echo "Copying frontend dist..."
tar -czf frontend-dist.tar.gz -C frontend/dist .
sshpass -p "${SERVER_PASS}" scp -o StrictHostKeyChecking=no -P ${SERVER_PORT} frontend-dist.tar.gz ${SERVER_USER}@${SERVER_IP}:/opt/wmdb/frontend/

echo "Starting application on server..."
sshpass -p "${SERVER_PASS}" ssh -o StrictHostKeyChecking=no -p ${SERVER_PORT} ${SERVER_USER}@${SERVER_IP} << 'EOF'
  pkill -f wmdb-backend-1.0.0-SNAPSHOT.jar || true

  cd /opt/wmdb/frontend
  tar -xzf frontend-dist.tar.gz

  cd /opt/wmdb/backend
  java -Dspring.datasource.dynamic.datasource.master.url='jdbc:mysql://rm-uf6abp6renk8g3l2wio.mysql.rds.aliyuncs.com:3306/huiqitong_erp?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true' \
       -Dspring.datasource.dynamic.datasource.master.username='root' \
       -Dspring.datasource.dynamic.datasource.master.password='f5mF2hKiOkbxKqs5' \
       -jar wmdb-backend-1.0.0-SNAPSHOT.jar > backend.log 2>&1 &
EOF

echo "Deployment complete."
