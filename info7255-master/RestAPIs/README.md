## Running the prototype

### Pre-requisites
1. Java
2. Maven
3. OAuth 2.0 client (Refer Google APIs for more details)

### Build & Test
```
mvn clean install
```

### Run as Spring Boot application
- Method 1 (**Recommended**)  
```
mvn spring-boot:run -Dsecurity.oauth2.client.clientId=<CLIENT_ID> -Dsecurity.oauth2.client.clientSecret=<CLIENT_SECRET>
```
- Method 2  
```
Set the security credentials in `application.properties` file in the project resources
```