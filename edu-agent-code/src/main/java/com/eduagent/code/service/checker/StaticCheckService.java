package com.eduagent.code.service.checker;

import com.eduagent.code.service.compiler.SourceFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;
import com.puppycrawl.tools.checkstyle.XMLLogger;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.reporting.Report;
import net.sourceforge.pmd.reporting.RuleViolation;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 静态检查：Checkstyle（教学级 checkstyle.xml）+ PMD（pmd/ruleset.xml），产出违规列表与 JSON 文本。
 * 严格防御：任一检查器升级不兼容 / 运行异常都降级为空结果并记日志，绝不因静态检查失败阻断判分主流程。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StaticCheckService {

    private static final String CHECKSTYLE_CFG = "classpath:checkstyle/checkstyle.xml";
    private static final String PMD_RULESET = "classpath:pmd/ruleset.xml";

    private final ObjectMapper objectMapper;

    public StaticCheckResult check(List<SourceFile> sources) {
        List<CheckstyleViolation> cs = runCheckstyleQuietly(sources);
        List<PmdViolation> pmd = runPmdQuietly(sources);
        return new StaticCheckResult(
                cs, pmd,
                toJson(cs), toJson(pmd),
                cs.stream().filter(v -> "error".equals(v.severity())).mapToInt(v -> 1).sum(),
                cs.stream().filter(v -> "warning".equals(v.severity())).mapToInt(v -> 1).sum());
    }

    // ---------- Checkstyle ----------

    private List<CheckstyleViolation> runCheckstyleQuietly(List<SourceFile> sources) {
        Path dir = null;
        try {
            dir = Files.createTempDirectory("edujava-cs-");
            List<Path> files = writeTemp(dir, sources);
            return runCheckstyle(files);
        } catch (Exception e) {
            log.warn("Checkstyle 执行失败，降级为空结果", e);
            return List.of();
        } finally {
            deleteQuietly(dir);
        }
    }

    private List<CheckstyleViolation> runCheckstyle(List<Path> files) throws Exception {
        String cfgPath = new ClassPathResource("checkstyle/checkstyle.xml").getFile().getAbsolutePath();
        Configuration config = ConfigurationLoader.loadConfiguration(
                cfgPath, new PropertiesExpander(new Properties()));
        Checker checker = new Checker();
        checker.setModuleClassLoader(Checker.class.getClassLoader());
        checker.configure(config);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        checker.addListener(new XMLLogger(baos,
                com.puppycrawl.tools.checkstyle.api.AutomaticBean.OutputStreamOptions.NONE));
        checker.process(files.stream().map(Path::toFile).toList());
        checker.destroy();
        return parseCheckstyleXml(baos.toString(StandardCharsets.UTF_8));
    }

    private List<CheckstyleViolation> parseCheckstyleXml(String xml) {
        List<CheckstyleViolation> out = new ArrayList<>();
        if (xml == null || xml.isBlank()) {
            return out;
        }
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new org.xml.sax.InputSource(new java.io.StringReader(xml)));
            NodeList files = doc.getElementsByTagName("file");
            for (int i = 0; i < files.getLength(); i++) {
                Element f = (Element) files.item(i);
                String fileName = f.getAttribute("name");
                NodeList errors = f.getElementsByTagName("error");
                for (int j = 0; j < errors.getLength(); j++) {
                    Element e = (Element) errors.item(j);
                    out.add(new CheckstyleViolation(
                            fileName,
                            intAttr(e, "line"),
                            intAttr(e, "column"),
                            e.getAttribute("severity"),
                            e.getAttribute("message"),
                            e.getAttribute("source"),
                            ruleFromSource(e.getAttribute("source"))));
                }
            }
        } catch (Exception e) {
            log.warn("解析 Checkstyle XML 失败：{}", xml, e);
        }
        return out;
    }

    private String ruleFromSource(String source) {
        return source == null ? "" : source.substring(source.lastIndexOf('.') + 1);
    }

    // ---------- PMD ----------

    private List<PmdViolation> runPmdQuietly(List<SourceFile> sources) {
        Path dir = null;
        try {
            dir = Files.createTempDirectory("edujava-pmd-");
            List<Path> files = writeTemp(dir, sources);
            return runPmd(files);
        } catch (Exception e) {
            log.warn("PMD 执行失败，降级为空结果", e);
            return List.of();
        } finally {
            deleteQuietly(dir);
        }
    }

    private List<PmdViolation> runPmd(List<Path> files) throws Exception {
        String ruleset = new ClassPathResource("pmd/ruleset.xml").getFile().getAbsolutePath();
        PMDConfiguration config = new PMDConfiguration();
        config.setIgnoreIncrementalAnalysis(true);
        config.addRuleSet(ruleset);

        List<PmdViolation> out = new ArrayList<>();
        try (PmdAnalysis analysis = PmdAnalysis.create(config)) {
            files.forEach(f -> analysis.files().addFile(f));
            Report report = analysis.performAnalysisAndCollectReport();
            for (RuleViolation rv : report.getViolations()) {
                out.add(new PmdViolation(
                        rv.getFileId() == null ? "?" : rv.getFileId().getFileName(),
                        rv.getBeginLine(),
                        rv.getRule().getRuleSetName(),
                        rv.getRule().getName(),
                        rv.getRule().getPriority().getPriority(),
                        rv.getDescription(),
                        null));
            }
        }
        return out;
    }

    // ---------- 通用 ----------

    private List<Path> writeTemp(Path dir, List<SourceFile> sources) throws Exception {
        List<Path> files = new ArrayList<>();
        for (SourceFile sf : sources) {
            Path p = dir.resolve(sf.name().replace('\\', '/'));
            if (p.getParent() != null) {
                Files.createDirectories(p.getParent());
            }
            Files.writeString(p, sf.source(), StandardCharsets.UTF_8);
            files.add(p);
        }
        return files;
    }

    private String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return "[]";
        }
    }

    private int intAttr(Element e, String attr) {
        try {
            return (int) Double.parseDouble(e.getAttribute(attr));
        } catch (Exception ex) {
            return 0;
        }
    }

    private void deleteQuietly(Path p) {
        if (p != null) {
            try {
                FileSystemUtils.deleteRecursively(p);
            } catch (Exception e) {
                log.warn("清理临时目录失败 {}", p, e);
            }
        }
    }
}
