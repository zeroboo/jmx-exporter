package com.zboo.tool;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.rmi.RemoteException;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class JsonJmxObjectReporter extends JmxObjectReporter {
    private final transient Logger logger = LoggerFactory.getLogger(getClass());
    Gson gsonPretty;
    Gson gsonNormal;
    public JsonJmxObjectReporter() {
        super();
        init();
    }

    public JsonJmxObjectReporter(MBeanServerConnection connection) {
        super(connection);
        init();

    }

    private void init()
    {
        gsonNormal = new GsonBuilder().serializeNulls().disableHtmlEscaping().create();
        gsonPretty = new GsonBuilder().serializeNulls().disableHtmlEscaping().setPrettyPrinting().create();

    }


    @Override
    public void makeReport(String outputFile)
    {
        OutputStream os = null;
        Path path = Paths.get(outputFile);
        logger.info("START");
        logger.info("Included object names: {}", !this.includeObjectNames.isEmpty()?this.includeObjectNames.toString():"ALL");

        try {

            os = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            JsonWriter writer = new JsonWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.setHtmlSafe(false);
            writer.setSerializeNulls(true);
            writer.setIndent(" ");///Also turn pretty on/of
            writer.beginObject();
            writer.name("title");
            writer.value(reportTitle);
            writer.name("reportDescription").value(reportDescription); // "name" : "mkyong"

            Set<ObjectName> names = new TreeSet<ObjectName>(connection.queryNames(null, null));
            Object nameList = names.stream().map(x -> x.getCanonicalName()).collect(Collectors.toList());
            writer.name("objectCount").value(names.size());
            writer.name("objectNames");
            gsonNormal.toJson(nameList, nameList.getClass(), writer);

            writer.name("objects");
            writer.beginObject();

            for (ObjectName name : names) {
                if(isObjectIncluded(name.getCanonicalName())) {
                    if (name != null) {
                        logger.info("Reading {}", name.getCanonicalName());
                        writer.name(name.getCanonicalName());
                        writer.beginObject();

                        writer.name("name").value(name.getCanonicalName());
                        writer.name("domain").value(name.getDomain());
                        writer.name("keys").value(name.getKeyPropertyListString());

                        try {
                            MBeanInfo info = connection.getMBeanInfo(name);
                            if (info != null) {
                                ///writer.setIndent("");
                                writer.name("beanInfo");
                                gsonNormal.toJson(info, info.getClass(), writer);
                                writer.name("attributes");
                                writer.beginObject();
                                for (MBeanAttributeInfo x : info.getAttributes()) {
                                    writer.name(x.getName());
                                    writer.beginObject();
                                    if (x != null) {
                                        writer.name("type").value(x.getType());
                                        writer.name("name").value(x.getName());
                                        writer.name("description").value(x.getDescription());

                                        logger.debug("\tGetting attribute {}", x.getName());
                                        try {
                                            Object data = connection.getAttribute(name, x.getName());
                                            if (data != null) {
                                                logger.debug("\tGetting attribute {}", x.getName());
                                                ///writer.setIndent("");
                                                writer.name("value");
                                                gsonNormal.toJson(data, data.getClass(), writer);
                                            } else {
                                                logger.warn("Exception when get attribute {} of {}", x.getName(), name.getCanonicalName());
                                                writer.name("attributeData").value("null");
                                            }
                                        } catch (RemoteException | JMException | RuntimeMBeanException e) {
                                            logger.warn("Exception when get attribute {} of {}: {}, {}", x.getName(), name.getCanonicalName(), e.getClass().getName(), e.getMessage());
                                        }
                                    } else {
                                        logger.warn("Meet null info in {} ", name.getCanonicalName());
                                    }
                                    writer.endObject();
                                    writer.setIndent(" ");
                                }
                                writer.endObject();
                                writer.setIndent(" ");
                            } else {
                                logger.error("\tNull bean info in {}", name.getCanonicalName());
                                writer.name("info").value("");
                            }

                        } catch (InstanceNotFoundException e) {
                            e.printStackTrace();
                        } catch (IntrospectionException e) {
                            e.printStackTrace();
                        } catch (ReflectionException e) {
                            e.printStackTrace();
                        }

                        writer.endObject();
                    }
                } else {
                    logger.info("Object not included: {}", name.getCanonicalName());
                }
            }
            writer.endObject();

            writer.endObject();


            writer.flush();
            writer.close();
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


    @Override
    public String makeReportObjectName(ObjectName name) {
        return gsonPretty.toJson(name);
    }
}
