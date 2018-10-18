package com.zboo.tool;

import javax.management.ObjectName;

public class HtmlJmxObjectReporter extends JmxObjectReporter{
    @Override
    public void makeReport(String outputFile) {

    }

    @Override
    public String makeReportObjectName(ObjectName name) {
        return null;
    }
}
