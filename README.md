## DOCUMENTATION:
Can be found in "WA-G16.ticketing.documentation":
   
    API_documentation.json
    Data Layer Design.mdj
## RUN THE SERVER
Start a valid Postgress container with: 
- url = jdbc:postgresql://localhost:5432/postgres
- username = postgres
- password = postgres
- port = 5432

Server (named:ticketing) run on port: 8081, commands to run:
- enter in the folder: .../WA-G16/ticketing
- build:
 
      on linux: ./gradlew
            [on windows: gradlew.bat]
            docker run -d --name ticketing-spring-api -p 8081:8081 ticketing-spring-api
      run:
          docket start ticketing-spring-api

## RUN THE TEST
Enter in the folder: .../WA-G16/ticketing
-execute: 
        
    linux: ./gradlew test --tests "it.polito.wa2.ticketing.TicketingApplicationTests"
    windows: gradlew.bat test --tests "it.polito.wa2.ticketing.TicketingApplicationTests"
    
## OTHER RESOURCES
Can be found "WA-G16.ticketing.src.main.resources":
        
- customers.csv
- employees.csv
- products.csv

### Keycloak users:


| username  | password |
|-----------|----------|
| manager   | password |
| expert1   | password |
| expert2   | password |
| customer1 | password |
| customer2 | password |