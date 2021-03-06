# Enactment Engine (EE)

## File structure

- **[externalJars](externalJars)** contain pre-build jars used within the EE. Keep them upToDate if they work properly.
- **[src/main](src/main)** 
    - **[/java](src/main/java)** contains the source code of the EE.
    - **[/resources](src/main/resources)** contains example yaml files to test the execution.
- You will need to create a **credentials.properties** file in the root directory of the project, which contains the login credentials for the FaaS providers. This file will be ignored from git (**[.gitignore](.gitignore)**).

    The file should look as follows:
    ````
    aws_access_key=<your_aws_access_key>
    aws_secret_key=<your_aws_secret_key>
    ibm_api_key=<your_ibm_api_key>
    ````
    
---------------
    
## Deploy

### Local
Simply run the [main method in Local.java](src/main/java/at/enactmentengine/serverless/main/Local.java) and pass the workflow yaml file as parameter.

### Service
Run the [main method in Service.java](src/main/java/at/enactmentengine/serverless/main/Service.java). The Service will wait on port 9000 for a `.yaml` file and return the result of the execution in json format.

### Docker
````
gradle updateDocker         // to start the service in a docker container with a gradle task.
````

Alternatively, follow [/docker/README.md](docker/README.md) to run the container without gradle.
<!---
NOT SUPPORTED RIGHT NOW:

### AWS
1. Create an AWS Lambda function representing the EE (Upload the .jar)
   ````
   Runtime: Java8
   Handler: at.enactmentengine.serverless.main.LambdaHandler::handleRequest
   ````
2. Invoke the function with a specific input file:
    ````
    {
      "filename": "<your_workflow.yaml>",
      "language": "yaml"
    }
    ````
    or
    ````
    {
      "workflow": "<your_workflow_json_string>",
      "language": "json"
    }
    ````
    Use tools like https://www.json2yaml.com/ to convert from yaml to json and https://www.freeformatter.com/json-escape.html to escape characters.
    
### IBM
1. Create an IBM action representing the EE
    ````
    wsk action create EnactmentEngine <exported_ee.jar> --main at.enactmentengine.serverless.main.OpenWhiskHandler#main
    ````
2. Invoke the action with a specific input file:
    Input:
    ````
    wsk action invoke --result EnactmentEngine --param filename <your_workflow.yaml>
    ````
    or
    ````
    {
      "filename": "<your_workflow.yaml>",
      "language": "yaml"
    }
    ````
    or
    ````
    {
      "workflow": "<your_workflow_json_string>",
      "language": "json"
    }
    ````
    Use tools like https://www.json2yaml.com/ to convert from yaml to json and https://www.freeformatter.com/json-escape.html to escape characters.
--->

---------------

## Fault Tolerance

The enactment engine will automatically pass functions with fault tolerance and constraint settings to the fault tolerance module for execution.

### Features

This section will show the features of the Fault Tolerance engine with some very simple examples. All examples are based on the following simple workflow (only the constraints field of a function needs to be changed within the CFCL file):
````yaml
name: "exampleWorkflow"
workflowBody:
- function:
    name: "hello"
    type: "helloType"
    properties:
    - name: "resource"
      value: "python:https://eu-gb.functions.cloud.ibm.com/<link.to.function>/hello.json"
    constraints:
    - name: "<FT-Name>"
      value: "<FT-Value>"
    dataOuts:
    - name: "message"
      type: "string"
dataOuts:
- name: "OutVal"
  type: "string"
  source: "hello/message"
````

- **FT-Retries** and **FT-AltStrat-requiredAvailability**

First we specify the number of times a function should be retried (`FT-Retries`) before the alternative strategy is executed. Additionally we can specify the required availability (`FT-AltStrat-requiredAvailability`) as a double value. `FT-Retries` is mandatory, while `FT-AltStrat-requiredAvailability` is optional when using the fault tolerant execution. 

````yaml
constraints:
- name: "FT-Retries"
  value: "2"
- name: "FT-AltStrat-requiredAvailability"
  value: "0.9"
````

The `FT-AltStrat-requiredAvailability` requires some additional steps to find the alternative plan which is understandable by the enactment engine. For simplicity you can use the `addAlternativePlansToYAML()` method from [AlternativePlanScheduler](src/main/java/at/enactmentengine/serverless/scheduler/AlternativePlanScheduler.java) to find alternative plan(s). Please note that this might be changed in future and this method will be ported to another module. 

In order for the [AlternativePlanScheduler](src/main/java/at/enactmentengine/serverless/scheduler/AlternativePlanScheduler.java) to work properly the following steps are required:

1. A database with function invocations is required to retrieve old executions. Example: [FTDatabase.db](Database/FTDatabase.db). The database should contain the function which will be executed in the **Function** table.
2. (To generate random invocations to fill the database [DataBaseFiller](src/main/java/at/enactmentengine/serverless/main/DataBaseFiller.java) can be used) 
3. Now the `addAlternativePlansToYAML("path/to/file.yaml", "path/to/newly/created/optimizedFile.yaml")` can be used to generate the alternative strategy at runtime.
4. The optimized file can now be executed by the enactment engine.

Possible output from `addAlternativePlansToYAML()`:
````yaml
constraints:
    - name: "FT-AltPlan-0"
      value: "0.9879;https://jp-tok.functions.appdomain.cloud/api/v1/web/<link.to.function>/hello.json;https://eu-gb.functions.cloud.ibm.com/api/v1/web/<link.to.function>/hello.json;"
    - name: "FT-AltPlan-1"
      value: "0.9808;https://eu-de.functions.appdomain.cloud/api/v1/web/<link.to.function>/hello.json;https://us-south.functions.appdomain.cloud/api/v1/web/<link.to.function>/hello.json;"
````

The following example would invoke each function within an alternative plan `2` times:
````yaml
constraints:
    - name: "FT-Retries"
      value: "2"
    - name: "FT-AltPlan-0"
      value: "0.9879;https://jp-tok.functions.appdomain.cloud/api/v1/web/<link.to.function>/hello.json;https://eu-gb.functions.cloud.ibm.com/api/v1/web/<link.to.function>/hello.json;"
    - name: "FT-AltPlan-1"
      value: "0.9808;https://eu-de.functions.appdomain.cloud/api/v1/web/<link.to.function>/hello.json;https://us-south.functions.appdomain.cloud/api/v1/web/<link.to.function>/hello.json;"
````

- **C-latestStartingTime**

````yaml
constraints:
- name: "C-latestStartingTime"
  value: "2011-10-02 18:48:05.123"
````

will throw an `at.uibk.dps.exception.LatestStartingTimeException` if the start time of the function is before the specified time. 

- **C-latestFinishingTime**

````yaml
constraints:
- name: "C-latestFinishingTime"
  value: "2011-10-02 18:48:05.123"
````

will throw an `at.uibk.dps.exception.LatestFinishingTimeException` if the function did not finish before the specified time.

- **C-maxRunningTime**

````yaml
constraints:
- name: "C-maxRunningTime"
  value: "1240"
````

will stop waiting for a response of the cloud function after `1240` milliseconds. The enactment-engine will throw an `at.uibk.dps.exception.MaxRunningTimeException` exception if the specified runtime is exceeded.



  