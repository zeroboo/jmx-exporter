package com.zboo.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

public abstract class JmxObjectReporter {
    private final transient Logger logger = LoggerFactory.getLogger(getClass());
    public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
    MBeanServerConnection connection;
    String reportDescription = "";
    String reportTitle = "";
    Set<String> includeObjectNames;
    public JmxObjectReporter(MBeanServerConnection connection) {
        this.connection = connection;
        includeObjectNames = new HashSet<>();
    }


    public JmxObjectReporter() {
        includeObjectNames = new HashSet<>();
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

    abstract public void makeReport(String outputFile);


    abstract public String makeReportObjectName(ObjectName name);

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
}
