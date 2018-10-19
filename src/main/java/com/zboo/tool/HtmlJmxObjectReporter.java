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
import java.util.Set;
import java.util.TreeSet;

public class HtmlJmxObjectReporter extends JmxObjectReporter {
    private final transient Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void makeReport(String outputFile) {
        OutputStream os = null;
        Path path = Paths.get(outputFile);
        logger.info("START");
        logger.info("Included object names: {}", !this.includeObjectNames.isEmpty() ? this.includeObjectNames.toString() : "ALL");
        try {

            os = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            PrintStream ps = new PrintStream(os, true, "UTF-8");
            ps.println("<!DOCTYPE html>");
            ps.println("<html>");
            ps.println("<head>");
            ///ps.println("<link rel=\"stylesheet\" id=\"smartmag-core-css\" href=\"http://scrumbucket.org/wp-content/themes/smart-mag/style.css?ver=2.4.1\" type=\"text/css\" media=\"all\">");
            ps.println("</head>");
            ps.println("<body>");

            Set<ObjectName> names = new TreeSet<ObjectName>(connection.queryNames(null, null));

            ps.print("<ol>");
            for (ObjectName obj : names) {
                if (isObjectIncluded(obj.getCanonicalName())) {
                    logger.info("Printing object: {}", obj.getCanonicalName());
                    ps.print("<li>");
                    ps.println(String.format("<h3>%s</h3>", obj.getCanonicalName()));

                    ps.println(makeReportObjectName(obj));
                    ps.println("</li>");
                } else {
                    logger.info("Object not included: {}", obj.getCanonicalName());
                }
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

    @Override
    public String makeReportObjectName(ObjectName obj) {
        StringBuilder sb = new StringBuilder();
        sb.append("<ul>");
        sb.append(String.format("<li>Properties: %s</li>", obj.getCanonicalKeyPropertyListString()));
        sb.append(String.format("<li>Domain: %s</li>", obj.getDomain()));
        if (obj != null) {
            logger.info("Reading {}", obj.getCanonicalName());
            try {
                MBeanInfo info = connection.getMBeanInfo(obj);
                if (info != null) {
                    sb.append("<li>");
                    sb.append("Bean info");
                    sb.append("<ul>");
                    sb.append(String.format("<li>Classname: %s</li>", info.getClassName()));
                    sb.append(String.format("<li>Description: %s</li>", info.getDescription()));
                    sb.append("<li>");
                    sb.append("Attributes");
                    sb.append("<table>");
                    sb.append("<thead><tr><th>Name</th><th>Type</th><th>Value</th></tr></thead>");
                    for (MBeanAttributeInfo attributeInfo : info.getAttributes()) {
                        if (attributeInfo != null) {
                            logger.debug("\tGetting attribute {}", attributeInfo.getName());
                            try {
                                Object data = connection.getAttribute(obj, attributeInfo.getName());

                                if (data instanceof ObjectName) {

                                    logger.debug("Catch an ObjectName {} inside ObjectName {} ", ((ObjectName) data).getCanonicalName(), obj.getCanonicalName());
                                } else {
                                    sb.append(makeHtmlRow(obj, attributeInfo, data));
                                }
                            } catch (IOException | JMException | RuntimeMBeanException e) {
                                logger.warn("Exception when get attribute {} of {}: {}, {}", attributeInfo.getName(), obj.getCanonicalName(), e.getClass().getName(), e.getMessage());
                            }
                        } else {
                            logger.warn("Meet null info in {} ", obj.getCanonicalName());
                        }

                    }
                    sb.append("</table>");
                    sb.append("</li>");
                    sb.append("</ul>");
                    sb.append("</li>");
                } else {
                    logger.error("\tNull bean info in {}", obj.getCanonicalName());
                }

            } catch (Exception e) {
                logger.warn("Load object exception: {}, {}, {}", obj.getCanonicalName(), e.getClass(), e.getMessage());
            }
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
