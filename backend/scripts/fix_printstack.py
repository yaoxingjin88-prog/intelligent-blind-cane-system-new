from pathlib import Path
import re

root = Path(r"c:\Users\Lenovo\Desktop\个人项目\mangzhang\intelligent-blind-cane-system\backend\src\main\java\com\ruoyi\controller\mini")
for path in root.glob("*.java"):
    text = path.read_text(encoding="utf-8")
    if "printStackTrace()" not in text:
        continue
    if "LoggerFactory" not in text:
        text = text.replace(
            "import org.springframework.web.bind.annotation.*;",
            "import org.slf4j.Logger;\nimport org.slf4j.LoggerFactory;\nimport org.springframework.web.bind.annotation.*;",
        )
        m = re.search(r"public class (\w+) \{", text)
        if m:
            cls = m.group(1)
            text = text.replace(
                f"public class {cls} {{",
                f"public class {cls} {{\n\n    private static final Logger log = LoggerFactory.getLogger({cls}.class);",
                1,
            )
    text = text.replace("e.printStackTrace();", 'log.warn("请求处理失败", e);')
    path.write_text(text, encoding="utf-8")
    print("updated", path.name)
