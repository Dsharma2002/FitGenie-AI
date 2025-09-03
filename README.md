Start the backend microservice server

user-service uses Postgres as the database
activity-service uses MongoDB as the database
ai-service uses MongoDB as the database

# latest RabbitMQ 4.x
docker run -it --rm --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:4-management

# run keycloak 
using publicaly available docker image

activity service does the job of publishing the activity to the rabbitmq
ai service does the job of consuming the activity from the rabbitmq