SELECT 'CREATE DATABASE learning_platform'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'learning_platform')\gexec

SELECT 'CREATE DATABASE keycloak'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'keycloak')\gexec
