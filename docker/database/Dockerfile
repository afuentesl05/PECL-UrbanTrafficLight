FROM mariadb:11

# Script SQL que se ejecuta SOLO la primera vez que se crea el contenedor
COPY init.sql /docker-entrypoint-initdb.d/init.sql
