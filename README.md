# Health Check Plugin for Neo4j

## Neo4j version 5.x

The target database name needs to be specified as a path parameter e.g. */labs/health/neo4j*

Increased the major-minor version numbering to reflect the Neo4j version and follow the APOC scheme. 

## Query result

The health check has 4 different results depending from the server's state:

* UP
    * The server is up and running. Also the server has nodes.  
    * `{"status":"UP","description":"Neo4j health check was successful."}`
* OUT_OF_SERVICE
    * The server is up and running but it has 0 nodes.
    * `{"status":"OUT_OF_SERVICE","description":"Neo4j has no available nodes!"}`
* DOWN
    * The server is not available.
    * `{"status":"DOWN","description":"Neo4j health check result was invalid!"}`
* OUT_OF_SERVICE
    * An error occurred during the query state.
    * `{"status":"OUT_OF_SERVICE","description":"Neo4j health check failed!","error":"java.lang.NullPointerException: detail"}`

## Installation

### Neo4j (3.0+)

`NEO4J_HOME/conf/neo4j.conf`

```
dbms.security.auth_enabled=false
dbms.unmanaged_extension_classes=com.brinkus.labs.neo4j.health.unmanaged=/labs
```

### Neo4j (<3.0)

`NEO4J_HOME/conf/neo4j-server.properties`

```
dbms.security.auth_enabled=false
org.neo4j.server.thirdparty_jaxrs_classes=com.brinkus.labs.neo4j.health.unmanaged=/labs
```
