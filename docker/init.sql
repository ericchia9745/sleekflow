CREATE DATABASE IF NOT EXISTS sleekflow_todo_test
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
GRANT ALL PRIVILEGES ON sleekflow_todo_test.* TO 'todo'@'%';
FLUSH PRIVILEGES;
