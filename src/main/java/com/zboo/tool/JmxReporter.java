package com.zboo.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;
import javax.naming.Context;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import java.util.*;

/**
 * Hello world!
 */
@CommandLine.Command(name = "JmxExporter"
        , footer = "tanhung.vn@gmail.com"
        , description = "Connect to JMX remotely and generate report")
public class JmxReporter implements Runnable {
    private final transient Logger logger = LoggerFactory.getLogger(getClass());

    static final String EMPTY_STRING = "";
    static final String OUTPUT_FORMAT_JSON = "json";
    static final String OUTPUT_FORMAT_TEXT = "text";
    static final String OUTPUT_FORMAT_HTML= "html";
    static final String DEFAULT_JMX_PATH = "jmxrmi";
    @CommandLine.Option(names = {"-h", "--jmx-host"}, paramLabel = "RemoteHost", required = true, description = "JMX remote host")
    String jmxRemoteHost;

    @CommandLine.Option(names = {"-p", "--jmx-port"}, paramLabel = "RemotePort", required = true, description = "JMX remote port")
    int jmxRemotePort = 0;

    @CommandLine.Option(names = {"-u", "--jmx-url-path"}, paramLabel = "JMX url path", required = false, description = "Path to JMX Bean server, default is " + DEFAULT_JMX_PATH)
    String jmxUrlPath = DEFAULT_JMX_PATH;


    @CommandLine.Option(names = {"-o", "--output"}, paramLabel = "OutputFile", required = true, description = "")
    String outputFile;

    @CommandLine.Option(names = {"-f", "--format"}, paramLabel = "Format of output file"
            , required = true,  description = "Supported: ${COMPLETION-CANDIDATES}")
    OutputFormat outputFormat;

    @CommandLine.Option(names = {"-d", "--desc"}, paramLabel = "Description", required = false,  description = "Extra text to put into report")
    String description = "";

    @CommandLine.Parameters(paramLabel = "Object names", arity = "0..*", description = "Name of JMX MBean to be collected, giving no name means collect all." +
            " Eg: \"java.lang:type=Memory\"")
    String[] objectNames;

    @CommandLine.Option(names = {"-ssl", "--useSSL"}, paramLabel = "SSL", required = false, description = "Use SSL")
    boolean useSSL;

    @CommandLine.Option(names = {"-us", "--user"}, paramLabel = "Username", required = false, description = "Username")
    String user = EMPTY_STRING;

    @CommandLine.Option(names = {"-ps", "--pass"}, paramLabel = "Password", required = false, description = "Password")
    String pass = EMPTY_STRING;

    @CommandLine.Option(names = {"-a", "--action"}, paramLabel = "Action to perform", completionCandidates = Actions.class
            , required = true,  description = "Supported: ${COMPLETION-CANDIDATES}")
    Action action = Action.Report;

    enum OutputFormat
    {
        Json, html, text;
    }
    enum Action
    {
        Report, ReportSystem, ReportThread
    }
    static class Actions extends ArrayList<String> {
        Actions() {
            super(Arrays.asList("report", "get_memory_info", "get_thread_info", "get_memory_info"));
        }
    }

    public JmxReporter() {

    }

    public static void main(String[] args) throws Exception {
        JmxReporter reporter = new JmxReporter();
        CommandLine cmd = new CommandLine(reporter);
        cmd.setCaseInsensitiveEnumValuesAllowed(true);
        try {
            cmd.parse(args);
            if (cmd.isUsageHelpRequested()) {
                cmd.usage(System.out);
            } else if (cmd.isVersionHelpRequested()) {
                cmd.printVersionHelp(System.out);
            }
            reporter.run();
        } catch (CommandLine.ParameterException ex) {
            System.err.println(ex.getMessage());
            if (!CommandLine.UnmatchedArgumentException.printSuggestions(ex, System.err)) {
                ex.getCommandLine().usage(System.err);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    @Override
    public void run() {
        try {

            // Create an RMI connector client and
            // connect it to the RMI connector server
            logger.info("Create an RMI connector client and connect it to the RMI connector server");
            String connectionString = String.format("service:jmx:rmi:///jndi/rmi://%s:%d/%s", this.jmxRemoteHost, this.jmxRemotePort, this.jmxUrlPath);

            Map<String, Object> env = new HashMap<>();
            if(!user.isEmpty() || !pass.isEmpty()) {
                env.put("jmx.remote.credentials", new String[]{user, pass});
                env.put(Context.SECURITY_PRINCIPAL, user);
                env.put(Context.SECURITY_CREDENTIALS, pass);
            }
            if (useSSL) {
                SslRMIClientSocketFactory csf = new SslRMIClientSocketFactory();
                env.put("com.sun.jndi.rmi.factory.socket", csf);
                env.put(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE, csf);
            }
            logger.info("Connection url: {}", connectionString);
            logger.info("Connection environment: {}", env.toString());

            JMXServiceURL url = new JMXServiceURL(connectionString);
            JMXConnector jmxc = JMXConnectorFactory.connect(url, env);


            logger.info("Get a MBeanServerConnection");
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


            for (ObjectName name : names) {
                logger.info("\tObjectName = " + name);
            }

            logger.info("Making report: format {}, ouput file {}", outputFormat, outputFile);

            JmxObjectReporter reporter = new JmxObjectReporter();
            IReportWriter writer = null;
            if(action == Action.ReportSystem)
            {
                reporter.addIncludeObjects(new String[]{"java.lang:type=Memory"
                        , "java.lang:type=OperatingSystem"
                        , "java.lang:type=Runtime"
                        , "java.lang:type=Threading"
                });
            }
            if(outputFormat == OutputFormat.Json)
            {
                writer = new ReportWriterJson();
            }
            else if(outputFormat==OutputFormat.html)
            {
                writer = new ReportWriterHtml();
            }
            else
            {
                writer = new ReportWriterText();
            }
            reporter.setConnection(mbsc);
            reporter.setReportTitle(String.format("Report remote JMX on %s:%d", this.jmxRemoteHost, jmxRemotePort));
            if(description!=null)
            {
                reporter.setReportDescription(description);
            }
            logger.info("Report class: {}", reporter.getClass().getCanonicalName());
            logger.info("Writer class: {}", writer.getClass().getCanonicalName());
            reporter.addIncludeObjects(objectNames);
            reporter.collectData();
            writer.renderToFile(outputFile, reporter);

            jmxc.close();

            logger.info("\nBye! Bye!");
        } catch (Exception e) {
            logger.error("Exception", e);
        }
    }
}