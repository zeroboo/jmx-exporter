package com.zboo.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.TabularData;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ReportWriterText implements IReportWriter {
    static final String INDENT_THIRD = "\t\t\t";
    static final String INDENT_ATTRIBUTE_VALUE = INDENT_THIRD+ "\t";
    static final String INDENT_ATTRIBUTE_VALUE_COLLECTION = INDENT_THIRD+ "\t\t";
    static final String INDENT_SECOND = "\t\t";
    static final String NULL_TEXT = "NULL";
    static final String ENDLINE = "\n";

    public static Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
    @Override
    public void renderToFile(String outputFile, JmxObjectReporter reporter) throws IOException {
        OutputStream os = null;
        Path path = Paths.get(outputFile);
        logger.info("START");
        try {
            os = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            PrintStream ps = new PrintStream(os, true, DEFAULT_CHARSET.name());
            ps.println(reporter.getReportTitle());
            ps.println(reporter.getReportDescription());

            ps.println("Object names: ");
            for(Map<String, String> name: reporter.getJmxObjectNameIncluded())
            {
                for(Map.Entry<String, String> ent: name.entrySet()) {
                    ps.print("\t");
                    ps.print(ent.getKey());
                    ps.print(":");
                    ps.print(ent.getValue());
                    ps.println();
                }
            }

            for (String name: reporter.getJmxObjectData().keySet()) {
                logger.info("Reporting object: {}, {}", name, name!=null?name.getClass().getCanonicalName():"null");

                String objectData = this.makeReportObjectNameText(name, (Map<String, Object>) reporter.getJmxObjectData().get(name));
                ps.println(objectData);
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
    public String makeReportObjectNameText(String objectName, Map<String, Object> objectData)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Object: ").append(objectName).append(ENDLINE);
        try {
            MBeanInfo info = (MBeanInfo) objectData.get("beanInfo");

            List<Object> attributeData = (List<Object>) objectData.get("attributes");
            logger.info("Writing{}, total attributes in info {}, total attributes {}", info.getClassName(), info.getAttributes().length, attributeData.size());
            if (info.getAttributes().length != attributeData.size()) {
                logger.info("", info.getClassName(), info.getAttributes().length, attributeData.size());
                for (MBeanAttributeInfo infoName : info.getAttributes()) {
                    logger.info("\tInfo name: {}", infoName.getName());
                }
                for (Object data : attributeData) {
                    logger.info("\tData name: {}", data.getClass().getCanonicalName());
                }
            }
            sb.append(INDENT_SECOND).append("BeanInfo: " + info.getClassName()).append(ENDLINE);
            sb.append(INDENT_SECOND).append("ClassName: " + info.getClassName()).append(ENDLINE);
            sb.append(INDENT_SECOND).append("Description: " + info.getDescription()).append(ENDLINE);
            sb.append(INDENT_SECOND).append("AttributesCount: " + info.getAttributes().length).append(ENDLINE);
            sb.append(INDENT_SECOND).append("Attributes: " + info.getAttributes().length).append(ENDLINE);
            for (int i = 0; i < info.getAttributes().length; i++) {

                Object data = attributeData.get(i);
                MBeanAttributeInfo attInfo = info.getAttributes()[i];
                logger.debug("\tAttribute {}/{}: {}, type {}", i, info.getAttributes().length, attInfo.getName(), attInfo.getType());

                sb.append(INDENT_THIRD).append(attInfo.getName()).append(" (").append(attInfo.getType()).append("):").append(ENDLINE);

                if (data != null) {
                    if (data.getClass().isArray()) {
                        sb.append(INDENT_ATTRIBUTE_VALUE).append("Type: ").append(data.getClass().getCanonicalName()).append(ENDLINE);
                        sb.append(INDENT_ATTRIBUTE_VALUE).append("Values: ");

                        if(data instanceof int[])
                        {
                            int[] dataArray = (int[]) data;
                            for (int dataEntry : dataArray) {
                                sb.append(INDENT_ATTRIBUTE_VALUE_COLLECTION).append(dataEntry).append(ENDLINE);
                            }
                        }
                        else if(data instanceof float[])
                        {
                            float[] dataArray = (float[]) data;
                            for (float dataEntry : dataArray) {
                                sb.append(INDENT_ATTRIBUTE_VALUE_COLLECTION).append(dataEntry).append(ENDLINE);
                            }
                        }
                        else if(data instanceof long[])
                        {
                            long[] dataArray = (long[]) data;
                            for (long dataEntry : dataArray) {
                                sb.append(INDENT_ATTRIBUTE_VALUE_COLLECTION).append(dataEntry).append(ENDLINE);
                            }
                        }
                        else if(data instanceof double[])
                        {
                            float[] dataArray = (float[]) data;
                            for (float dataEntry : dataArray) {
                                sb.append(INDENT_ATTRIBUTE_VALUE_COLLECTION).append(dataEntry).append(ENDLINE);
                            }
                        }
                        else {
                            Object[] dataArray = (Object[]) data;
                            for (Object dataEntry : dataArray) {
                                sb.append(INDENT_ATTRIBUTE_VALUE_COLLECTION).append(dataEntry != null ? dataEntry.toString() : NULL_TEXT).append(ENDLINE);
                            }
                        }
                    } else if (data instanceof CompositeData) {
                        CompositeData dataSupport = (CompositeData) data;
                        for (String key : dataSupport.getCompositeType().keySet()) {
                            sb.append(INDENT_ATTRIBUTE_VALUE_COLLECTION).append(key).append(": ").append(dataSupport.get(key).toString()).append(ENDLINE);
                        }
                    } else if (data instanceof CompositeDataSupport) {
                        CompositeDataSupport dataSupport = (CompositeDataSupport) data;
                        sb.append(INDENT_ATTRIBUTE_VALUE).append("Type: ").append(data.getClass().getCanonicalName()).append(ENDLINE);
                        sb.append(INDENT_ATTRIBUTE_VALUE).append("Values:").append(ENDLINE);
                        for (String key : dataSupport.getCompositeType().keySet()) {
                            sb.append(INDENT_ATTRIBUTE_VALUE_COLLECTION).append(key).append(": ").append(dataSupport.get(key).toString()).append(ENDLINE);
                        }
                    } else if (data instanceof TabularData) {
                        TabularData tabularData = (TabularData) data;

                        sb.append(INDENT_ATTRIBUTE_VALUE).append("Type: ").append(data.getClass().getCanonicalName()).append(ENDLINE);
                        sb.append(INDENT_ATTRIBUTE_VALUE).append("Values: ").append(ENDLINE);
                        sb.append(INDENT_ATTRIBUTE_VALUE).append("TabularType: ").append(tabularData.getTabularType().toString()).append(ENDLINE);
                        for (Object row : tabularData.values()) {
                            CompositeData compositeData = (CompositeData) row;
                            sb.append(INDENT_ATTRIBUTE_VALUE_COLLECTION).append(row.toString()).append(ENDLINE);
                        }
                    } else if (data instanceof Collection<?>) {
                        Collection<?> dataCollection = (Collection<?>) data;
                        sb.append(INDENT_ATTRIBUTE_VALUE).append("Type: ").append(data.getClass().getCanonicalName()).append(ENDLINE);
                        sb.append(INDENT_ATTRIBUTE_VALUE).append("Values:").append(ENDLINE);
                        for (Object dataEntry : dataCollection) {
                            sb.append(INDENT_ATTRIBUTE_VALUE_COLLECTION).append(dataEntry != null ? dataEntry.toString() : NULL_TEXT).append(ENDLINE);
                        }
                    } else {
                        sb.append(INDENT_ATTRIBUTE_VALUE).append("Type: ").append(data.getClass().getCanonicalName()).append(ENDLINE);
                        sb.append(INDENT_ATTRIBUTE_VALUE).append("Value: ").append(data.toString()).append(ENDLINE);
                    }

                } else {

                    sb.append(INDENT_ATTRIBUTE_VALUE).append("Type: ").append(data.getClass().getCanonicalName()).append(ENDLINE);
                    sb.append(INDENT_ATTRIBUTE_VALUE).append("Value: ").append(NULL_TEXT).append(ENDLINE);
                    logger.debug("\tAttribute {}/{}: {}, type {} => {}", i, info.getAttributes().length, attInfo.getName(), attInfo.getType(), NULL_TEXT);

                }
            }
        }
        catch (Exception exceptionGetBeanInfo) {
            logger.error("Get bean info exception: ", exceptionGetBeanInfo);
        }


        return sb.toString();
    }
    private final transient Logger logger = LoggerFactory.getLogger(getClass());


}
