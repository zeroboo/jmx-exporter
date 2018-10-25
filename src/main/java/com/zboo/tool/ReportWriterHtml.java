package com.zboo.tool;

import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
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
import java.util.Arrays;
import java.util.Map;

public class ReportWriterHtml implements IReportWriter{
    private final transient Logger logger = LoggerFactory.getLogger(getClass());




    public String makeReportJmxObject(Object obj) {
        StringBuilder sb = new StringBuilder();
        sb.append("<ul>");
        if (obj != null) {

        }
        sb.append("</ul>");
        sb.append("</li>");

        return sb.toString();
    }

    public String makeHtmlRow(ObjectName bean, MBeanAttributeInfo beanAttributeInfo, Object beanAtributeData) {
        StringBuilder sb = new StringBuilder();
        ///sb.append("<h4>").append(beanAttributeInfo.getName()).append("</h4>");
        sb.append("<tr>");
        if (beanAtributeData != null) {


            sb.append("<td>");
            sb.append(beanAttributeInfo.getName());
            sb.append("</td>");

            if (beanAtributeData instanceof String[]) {
                sb.append("<td>");
                sb.append("String[]");
                sb.append("</td>");

                sb.append("<td>");
                sb.append("<ul>");
                for (String value : (String[]) beanAtributeData) {
                    sb.append("<li>").append(value).append("</li>");
                }
                sb.append("</ul>");
                sb.append("</td>");

            } else if (beanAtributeData instanceof int[]) {
                sb.append("<td>");
                sb.append("int[]");
                sb.append("</td>");

                sb.append("<td>");
                sb.append(Arrays.toString((int[]) beanAtributeData));
                sb.append("</td>");
            } else if (beanAtributeData instanceof long[]) {
                sb.append("<td>");
                sb.append("int[]");
                sb.append("</td>");

                sb.append("<td>");
                sb.append("<td>");
                sb.append(Arrays.toString((long[]) beanAtributeData));
                sb.append("</td>");
                sb.append("</td>");
            } else if (beanAtributeData instanceof CompositeData) {
                sb.append("<td>");
                sb.append("CompositeData");
                sb.append("</td>");
                CompositeData dataSupport = (CompositeData) beanAtributeData;

                for (String key : dataSupport.getCompositeType().keySet()) {
                    sb.append("<li>")
                            .append(key)
                            .append(":")
                            .append(dataSupport.get(key));

                    sb.append("</li>");
                }
                sb.append("</ul>");
            } else if (beanAtributeData instanceof CompositeData[]) {
                sb.append("<td>");
                sb.append("CompositeData[]");
                sb.append("</td>");
                CompositeData[] dataList = (CompositeData[]) beanAtributeData;

                sb.append("<td>");
                for (CompositeData subData : dataList) {
                    sb.append("<ul>");
                    for (String key : subData.getCompositeType().keySet())
                        sb.append("<li>")
                                .append(key).append("(").append(subData.getCompositeType().getClassName()).append(")")
                                .append(": ")
                                .append(subData.get(key).toString());
                    sb.append("</li>");
                    sb.append("</ul>");
                }
                sb.append("</ul>");
                sb.append("</td>");
            } else {
                sb.append("<td>");
                sb.append(beanAtributeData.getClass().getCanonicalName());
                sb.append("</td>");

                sb.append("<td>");
                sb.append(beanAtributeData.toString());
                sb.append("</td>");

            }


        } else {

            logger.warn("Null data {} of {}", bean.getCanonicalName(), beanAttributeInfo.getName());
        }
        sb.append("</tr>");
        return sb.toString();
    }

    @Override
    public void renderToFile(String outputFile, JmxObjectReporter reporter) throws IOException {
        OutputStream os = null;
        Path path = Paths.get(outputFile);
        logger.info("START");
        try {

            os = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            PrintStream ps = new PrintStream(os, true, "UTF-8");
            ps.println("<!DOCTYPE html>");
            ps.println("<html>");
            ps.println("<head>");
            ///ps.println("<link rel=\"stylesheet\" id=\"smartmag-core-css\" href=\"http://scrumbucket.org/wp-content/themes/smart-mag/style.css?ver=2.4.1\" type=\"text/css\" media=\"all\">");
            ps.println("</head>");
            ps.println("<body>");
            Map<String, Object> allData = reporter.getJmxObjectData();
            ps.print("<ol>");
            for (Object obj : reporter.getJmxObjectData().values()) {
                ps.println(makeReportJmxObject(obj));

            }
            ps.print("</ol>");
            ps.println("</body>");
            ps.println("</html>");
            ps.close();
            logger.info("DONE");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    logger.error("Exception when close file", e);
                }
            }
        }
        logger.info("Report to file: {}", path.toAbsolutePath().toString());

    }


    private static final class HTMLStyle extends ToStringStyle {

        public HTMLStyle() {
            setFieldSeparator("</td></tr>" + SystemUtils.LINE_SEPARATOR + "<tr><td>");

            setContentStart("<table>" + SystemUtils.LINE_SEPARATOR +
                    "<thead><tr><th>Field</th><th>Data</th></tr></thead>" +
                    "<tbody><tr><td>");

            setFieldNameValueSeparator("</td><td>");

            setContentEnd("</td></tr>" + SystemUtils.LINE_SEPARATOR + "</tbody></table>");

            setArrayContentDetail(true);
            setUseShortClassName(true);
            setUseClassName(false);
            setUseIdentityHashCode(false);
        }

        @Override
        public void appendDetail(StringBuffer buffer, String fieldName, Object value) {
            if (value.getClass().getName().startsWith("java.lang")) {
                super.appendDetail(buffer, fieldName, value);
            } else {
                buffer.append(ReflectionToStringBuilder.toString(value, this));
            }
        }
    }

    static public String makeHTML(Object object) {
        return ReflectionToStringBuilder.toString(object, new HTMLStyle());
    }
}
