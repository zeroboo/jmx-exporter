package com.zboo.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.xml.soap.Text;
import java.beans.BeanInfo;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

public class TextReporter {
    private final transient Logger logger = LoggerFactory.getLogger(getClass());
    public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
    MBeanServerConnection connection;
    String description = "";
    String reportTitle = "";
    public TextReporter(MBeanServerConnection connection) {
        this.connection = connection;
    }

    public void setReportTitle(String reportTitle) {
        this.reportTitle = reportTitle;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    static final String ENDLINE = "\n";
    public void reportConnection(String outputFile)
    {
        OutputStream os = null;
        Path path = Paths.get(outputFile);
        logger.info("START");
        try {
            os = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            PrintStream ps = new PrintStream(os, true, DEFAULT_CHARSET.name());
            ps.println(reportTitle);
            ps.println(description);

            Set<ObjectName> names = new TreeSet<ObjectName>(connection.queryNames(null, null));

            for (ObjectName name : names) {
                logger.info("Reporting object: {}", name.toString());
                String objectData = reportObjectName(name);
                ps.println(objectData.getBytes());
            }

            logger.info("DONE");
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if(os!=null)
            {
                try {
                    os.close();
                } catch (IOException e) {
                    logger.error("Exception when close file", e);
                }
            }
        }
        logger.info("Report to file: {}", path.toAbsolutePath().toString());

    }

    public String reportObjectName(ObjectName name) {

        StringBuilder sb = new StringBuilder();
        sb.append("ObjectName: ").append(name.toString()).append(ENDLINE);

        try {
            MBeanInfo info = connection.getMBeanInfo(name);
            for (MBeanAttributeInfo x : info.getAttributes()) {
                if (x != null) {
                    sb.append("\tAttribute: ").append(x.getName()).append(ENDLINE);

                    ///Class attributeType = x.getClass();
                    try {
                        Object data = connection.getAttribute(name, x.getName());
                        if (data != null) {
                            if (data instanceof String[]) {
                                sb.append("\t\tType: ").append("String[]").append(ENDLINE);
                                sb.append("\t\tValue: ").append(ENDLINE);
                                for(String value: (String[])data)
                                {
                                    sb.append("\t\t\t").append(value).append(ENDLINE);
                                }

                            } else if (data instanceof CompositeData) {
                                CompositeData dataSupport = (CompositeData) data;

                                sb.append("\t\tType: ").append(data.getClass().getCanonicalName()).append(ENDLINE);
                                sb.append("\t\tValue:").append(ENDLINE);
                                for (String key : dataSupport.getCompositeType().keySet()) {
                                    sb.append("\t\t\t").append(key).append(": ").append(dataSupport.get(key).toString()).append(ENDLINE);
                                }
                            } else {
                                sb.append("\t\tType: ").append(data.getClass().getCanonicalName()).append(ENDLINE);
                                sb.append("\t\tValue: ").append(data.toString()).append(ENDLINE);
                            }

                        } else {
                            sb.append("\t\tValue: null").append(ENDLINE);
                            logger.error("Null data: {}", name.toString());
                        }
                    }
                    catch(JMRuntimeException|IOException dataException)
                    {
                        logger.error("Get bean data exception: {}, {}", name.toString(), dataException.getMessage());
                        logger.debug("Get bean data exception: ", dataException);
                    }
                } else {
                    logger.error("Null BeanInfo: {}", name.toString());
                }
            }


        } catch (Exception exceptionGetBeanInfo) {
            logger.error("Get bean info exception: ", exceptionGetBeanInfo);
        }


        return sb.toString();
    }
}
