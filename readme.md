# Spring Boot with Axon Demo Application

Demo application shown in the webcast at 
https://spring.io/blog/2016/10/25/webinar-bootiful-cqrs-with-axon-nov-16


## Run RabbitMQ

Run rabbitMQ in a docker container:

    docker run -d --hostname my-rabbit --name some-rabbit -p 8080:15672 -p 5671:5671 -p 5672:5672 rabbitmq:3-management
    
now you can access the web GUI at

* Web URL: http://<dockerhost>:8080/#/queues
* user: `guest` 
* pass: `guest`

## Configure the app to use docker rabbitMQ

Add the following to both of your `application.properties` so they can reach RabbitMQ:

    spring.rabbitmq.addresses=<dockerhost>:5672


## Testing

Run both applications (e.g. in IntelliJ)

Use `httpie` client (`brew install httpie`):

### Post some complaints

    http http://localhost:8080/complaints company=microsoft description=testing
    http http://localhost:8080/complaints company=apple description=escape
    
### now read the statistics
    
    http http://localhost:8081