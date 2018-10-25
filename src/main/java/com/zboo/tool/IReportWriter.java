package com.zboo.tool;

import java.io.IOException;
import java.util.Map;

public interface IReportWriter {

    public abstract void renderToFile(String outputFile, JmxObjectReporter reporter) throws IOException;
}
