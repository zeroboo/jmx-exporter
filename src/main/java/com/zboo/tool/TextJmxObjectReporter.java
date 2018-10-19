package com.zboo.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import javax.management.openmbean.CompositeData;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.TreeSet;

public class TextJmxObjectReporter extends JmxObjectReporter {
    private final transient Logger logger = LoggerFactory.getLogger(getClass());

    public void makeReport(String outputFile)
    {
        OutputStream os = null;
        Path path = Paths.get(outputFile);
        logger.info("START");
        logger.info("Included object names: {}", !this.includeObjectNames.isEmpty()?this.includeObjectNames.toString():"ALL");
        try {
            os = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            PrintStream ps = new PrintStream(os, true, DEFAULT_CHARSET.name());
            ps.println(reportTitle);
            ps.println(reportDescription);

            Set<ObjectName> names = new TreeSet<ObjectName>(connection.queryNames(null, null));

            for (ObjectName name : names) {
                if(isObjectIncluded(name.getCanonicalName())) {
                    logger.info("Reporting object: {}", name.toString());
                    String objectData = this.makeReportObjectName(name);
                    ps.println(objectData);
                }
                else {
                    logger.info("Object not included: {}", name.getCanonicalName());
                }
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
    public String makeReportObjectName(ObjectName name)
    {

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
                            logger.warn("Null attribute: {} of {}", name.toString(), name.getCanonicalName());
                        }
                    }
                    catch(JMRuntimeException |IOException dataException)
                    {
                        logger.warn("Get bean attribute exception: {}, Exception {}", name.toString(), dataException.getClass().toString(), dataException.getMessage());
                        logger.debug("Get bean attribute exception: ", dataException);
                    }
                } else {
                    logger.warn("Null BeanInfo: {}", name.toString());
                }
            }


        } catch (Exception exceptionGetBeanInfo) {
            logger.error("Get bean info exception: ", exceptionGetBeanInfo);
        }


        return sb.toString();
    }
}
