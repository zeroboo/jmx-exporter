package com.zboo.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

/**
 * Hello world!
 */
@CommandLine.Command(name = "RemoteJMXExporter"
        , footer = "tanhung.vn@gmail.com"
        , description = "Connect to JMX remotely and generate report")
public class RemoteJMXExporter implements Runnable {
    private final transient Logger logger = LoggerFactory.getLogger(getClass());

    @CommandLine.Option(names = {"-h", "--host"}, paramLabel = "RemoteHost", required = true, description = "JMX remote host")
    String jmxRemoteHost;

    @CommandLine.Option(names = {"-p", "--port"}, paramLabel = "RemotePort", required = true, description = "JMX remote port")
    int jmxRemotePort;

    @CommandLine.Option(names = {"-o", "--output"}, paramLabel = "OutputFile", required = true, description = "")
    String outputFile;

    @CommandLine.Option(names = {"-f", "--format"}, paramLabel = "Format of output file", completionCandidates = FormatCandidates.class
            , required = true,  description = "Supported: ${COMPLETION-CANDIDATES}")
    String outputFormat;

    @CommandLine.Option(names = {"-d", "--desc"}, paramLabel = "Description", required = false,  description = "Extra text to put into report")
    String description = "";

    static class FormatCandidates extends ArrayList<String> {
        FormatCandidates() {
            super(Arrays.asList("text", "json", "html"));
        }
    }





    public RemoteJMXExporter() {

    }

    public static void main(String[] args) throws Exception {

        CommandLine.run(new RemoteJMXExporter(), args);
    }


    /* For simplicity, we declare "throws Exception".
           Real programs will usually want finer-grained exception handling. */
    public void ConnectJmx(String host, int port, String reportType, String outputFile, String description) throws Exception {
        // Create an RMI connector client and
        // connect it to the RMI connector server
        logger.info("ConnectJmx: {}:{}", host, port);
        logger.info("Create an RMI connector client and " +
                "connect it to the RMI connector server");
        JMXServiceURL url = new JMXServiceURL(String.format("service:jmx:rmi:///jndi/rmi://%s:%d/jmxrmi", host, port));
        JMXConnector jmxc = JMXConnectorFactory.connect(url, null);

        // Get an MBeanServerConnection
        //
        logger.info("Get an MBeanServerConnection");
        MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();

        // Get domains from MBeanServer
        //
        logger.info("Domains:");
        String domains[] = mbsc.getDomains();
        Arrays.sort(domains);
        for (String domain : domains) {
            logger.info("\tDomain:" + domain);
        }

        // Get MBeanServer's default domain
        //
        logger.info("MBeanServer default domain = " + mbsc.getDefaultDomain());

        // Get MBean count
        //
        logger.info("MBean count = " + mbsc.getMBeanCount());

        // Query MBean names
        //
        logger.info("Query MBeanServer MBeans:");
        Set<ObjectName> names =
                new TreeSet<ObjectName>(mbsc.queryNames(null, null));
        TextReporter reporter = new TextReporter(mbsc);
        reporter.setReportTitle(String.format("Report remote JMX on %s:%d", host, port));
        if(description!=null)
        {
            reporter.setDescription(description);
        }

        for (ObjectName name : names) {
            logger.info("\tObjectName = " + name);
        }
        reporter.reportConnection(outputFile);

        jmxc.close();
        logger.info("\nBye! Bye!");
    }


    @Override
    public void run() {
        try {
            this.ConnectJmx(jmxRemoteHost, jmxRemotePort, outputFormat, outputFile, description);
        } catch (Exception e) {
            logger.error("Exception", e);
        }
    }
}