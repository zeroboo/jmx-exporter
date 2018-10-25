package com.zboo.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import java.io.IOException;
import java.nio.charset.Charset;
import java.rmi.RemoteException;
import java.util.*;
import java.util.stream.Collectors;

public class JmxObjectReporter {
    private final transient Logger logger = LoggerFactory.getLogger(getClass());
    public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
    MBeanServerConnection connection;
    String reportDescription = "";
    String reportTitle = "";
    Set<String> includeObjectNames;
    String reportMode;

    Map<String, Object> jmxObjectData;
    List<Map<String,String>> jmxObjectNameIncluded;
    List<ObjectName> jmxObjectNameAll;
    public JmxObjectReporter(MBeanServerConnection connection) {
        this.connection = connection;
        init();
    }
    private void init()
    {
        includeObjectNames = new HashSet<>();
        jmxObjectData = new LinkedHashMap<>();
        jmxObjectNameIncluded = new ArrayList<>();
        jmxObjectNameAll = new ArrayList<>();
    }
    public JmxObjectReporter() {
        init();
    }

    public void addIncludeObjects(String[] objectNames)
    {
        if(objectNames != null)
        {
            for(String name: objectNames)
            {
                includeObjectNames.add(name);
            }
        }
    }
    public void setReportTitle(String reportTitle) {
        this.reportTitle = reportTitle;
    }

    public void setReportDescription(String reportDescription) {
        this.reportDescription = reportDescription;
    }

    public void setConnection(MBeanServerConnection connection) {
        this.connection = connection;
    }

    static final String ENDLINE = "\n";

    public void collectData() {
        logger.info("Collecting data START:");
        logger.info("Included object names: {}", !this.includeObjectNames.isEmpty()?this.includeObjectNames.toString():"ALL");
        Set<ObjectName> names = null;

        try {
            this.jmxObjectData.clear();
            this.jmxObjectNameIncluded.clear();
            this.jmxObjectNameAll.clear();
            names = new TreeSet<ObjectName>(connection.queryNames(null, null));

            for (ObjectName name : names) {
                this.jmxObjectNameAll.add(name);

                if(isObjectIncluded(name.getCanonicalName())) {
                    if (name != null) {
                        Map<String, String> nameDetail = new LinkedHashMap<>();
                        nameDetail.put("name", name.getCanonicalName());
                        nameDetail.put("domain", name.getDomain());
                        nameDetail.put("properties", name.getKeyPropertyListString().toString());
                        jmxObjectNameIncluded.add(nameDetail);


                        logger.info("Reading {}", name.getCanonicalName());
                        Map<String, Object> object = new LinkedHashMap<>();
                        try {
                            MBeanInfo info = connection.getMBeanInfo(name);

                            if (info != null) {
                                object.put("beanInfo", info);
                                List<Object> attributes = new ArrayList<>();
                                for (MBeanAttributeInfo x : info.getAttributes()) {
                                    if (x != null) {
                                        try {
                                            Object loadedAttribute = connection.getAttribute(name, x.getName());
                                            attributes.add(loadedAttribute);
                                            logger.debug("\tLoaded attribute {}, ", x.getName(), loadedAttribute!=null);
                                        } catch (RemoteException | JMException | RuntimeMBeanException e) {
                                            logger.warn("Exception when get attribute {} of {}: {}, {}", x.getName(), name.getCanonicalName(), e.getClass().getName(), e.getMessage());
                                            attributes.add(null);
                                        }
                                    } else {
                                        logger.warn("Meet null info in {} ", name.getCanonicalName());
                                        attributes.add(null);
                                    }
                                }
                                object.put("attributes", attributes);

                            } else {
                                logger.error("\tNull bean info in {}", name.getCanonicalName());
                            }
                            jmxObjectData.put(name.getCanonicalName(), object);
                        } catch (InstanceNotFoundException e) {
                            e.printStackTrace();
                        } catch (IntrospectionException e) {
                            e.printStackTrace();
                        } catch (ReflectionException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    logger.info("Object not included: {}", name.getCanonicalName());
                }
            }

            logger.info("DONE");
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.info("Collecting data DONE: total {} name, loaded {} objects"
                , names!=null?names.size():0
                , jmxObjectData!=null?jmxObjectData.size():0
        );
    }

    public boolean isObjectIncluded(String objectName)
    {
        if(this.includeObjectNames.isEmpty())
        {
            return true;
        }
        else
        {
            if(this.includeObjectNames.contains(objectName))
            {
                return true;
            }
        }
        return false;
    }

    public String getReportDescription() {
        return reportDescription;
    }

    public String getReportTitle() {
        return reportTitle;
    }

    public Map<String, Object> getJmxObjectData() {
        return jmxObjectData;
    }

    public List<Map<String,String>> getJmxObjectNameIncluded() {
        return jmxObjectNameIncluded;
    }

    public List<ObjectName> getJmxObjectNameAll() {
        return jmxObjectNameAll;
    }
}
