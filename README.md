# JMX Exporter  

A simple JAVA client for extracting JMX data from a JMX-enabled java process.

# Features

  - Connect and collect data to JMX enabled java process
  - Export to various formats: text, JSON, HTML

---

# Build

On project folder run
```maven
mvn clean package
```
Go to folder target and execute the jar for Usage
```
java -jar jmx-reporter-1.0-full.jar
```

---

# Usage

java -jar jmx-reporter-1.0-full.jar [-ssl] [-d=Description] -f=Format of output file
                   -h=RemoteHost -o=OutputFile -p=RemotePort [-ps=Password]
                   [-u=JMX url path] [-us=Username] [Object names...]
`

- [Object names...]      A list of names of JMX MBeans to be collected, giving no name means
                               collect all. 
   - Eg:
      - Collect only one object: `"java.lang:type=Memory"`
      - Collect 3 objects: `"java.lang:type=Memory" "java.lang:type=OperatingSystem" "java.lang:type=Threading"`
	  - Collect all available objects: ``
	  
- -d, --desc=Description     Extra text to put into report
- -f, --format=Format of output file. 
Supported: 
  - text
  - json
  - html
- -h, --jmx-host=RemoteHost  JMX remote host
- -o, --output=OutputFile
- -p, --jmx-port=RemotePort  JMX remote port
- -ps, --pass=Password   Password
- -ssl, --useSSL         Use SSL
- -u, --jmx-url-path=JMX url path. Path to JMX Bean server, default is "jmxrmi"
- -us, --user=Username   Username

