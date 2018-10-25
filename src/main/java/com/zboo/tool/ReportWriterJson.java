package com.zboo.tool;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;

public class ReportWriterJson implements IReportWriter {
    private final transient Logger logger = LoggerFactory.getLogger(getClass());
    Gson gsonPretty;
    Gson gsonNormal;
    public ReportWriterJson()
    {
        gsonNormal = new GsonBuilder().serializeNulls().disableHtmlEscaping().create();
        gsonPretty = new GsonBuilder().serializeNulls().disableHtmlEscaping().setPrettyPrinting().create();
    }

    @Override
    public void renderToFile(String outputFile, JmxObjectReporter reporter) throws IOException {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("title", reporter.getReportTitle());
        report.put("description", reporter.getReportDescription());
        report.put("objectNames", reporter.getJmxObjectNameIncluded());
        report.put("objects", reporter.getJmxObjectData());
        OutputStream os = null;
        Path path = Paths.get(outputFile);
        try {

            os = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            ///JsonWriter writer = new JsonWriter(new OutputStreamWriter(os, "UTF-8"));
            ///writer.close();
            os.write(gsonPretty.toJson(report).getBytes("UTF-8"));
            os.close();


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

}
