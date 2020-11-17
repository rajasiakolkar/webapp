## Technology Stack

1. Framework : Spring Boot
2. Database : PostgreSQL
3. Version Control : Git

## Build Instructions

Pre-requisites: Postman, IDE
1. First, clone the repository:git@github.com:akolkarrajasi/webapp.git with SSH Key in your local machine
2. Open the directory webapp
3. Download the required maven dependencies by going to File > Maven > Re-import dependencies
4. Run the WebappApplication from your IDE running the  webapp/cloudwebapp/src/main/java/com/neu/cloudwebapp/CloudebappApplication.java file

## Deploy Instructions

1. Register User: 
	curl -X POST \
	  http://localhost:8080/user/register \
	  -H 'Accept: /' \
	  -H 'Cache-Control: no-cache' \
	  -H 'Connection: keep-alive' \
	  -H 'Content-Type: application/json' \
	  -H 'Host: localhost:8080' \
	  -H 'User-Agent: PostmanRuntime/7.13.0' \
	  -H 'accept-encoding: gzip, deflate' \
	  -H 'cache-control: no-cache' \
	  -H 'content-length: 67' \
	  -d '{
		"first_name": "abc",
	    "last_name": "pqr",
	    "emai_address": "abc@aa.com",
	    "password" : "Awwweqwt6#"
	}
    

2. Get a User details:
	curl -X GET \
	  http://localhost:8080/user/self \
	  -H 'Accept: /' \
	  -H 'Authorization: Basic cHBAcHAuY29tOk9uZXBsdXM2IQ==' \
	  -H 'Cache-Control: no-cache' \
	  -H 'Connection: keep-alive' \
	  -H 'Content-Type: application/json' \
	  -H 'Host: localhost:8080' \
	  -H 'User-Agent: PostmanRuntime/7.13.0' \
	  -H 'accept-encoding: gzip, deflate' \
	  -H 'cache-control: no-cache'  

    
 3. Update a User:
	curl -X PUT \
	  http://localhost:8080/user/self \
	  -H 'Authorization: Basic cHBAcHAuY29tOk9uZXBsdXM2IQ==' \
	  -H 'Content-Type: application/json' \
	  -H 'cache-control: no-cache' \
	  -d '{
	    "first_name": "abc",
	    "last_name": "pqr",
	    "password" : "Awwweqwt6#" }
	}'

## Running Tests
Frameworks used for Testing: Mockito, JUnit

To Run the test cases on the WebappApplication: 
1. Open webapp aplication in your IDE 
2. Right click on Webapp project and select 'Run All Tests'