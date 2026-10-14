from pathlib import Path
import re

root = Path(r"c:\Users\Lenovo\Desktop\个人项目\mangzhang\intelligent-blind-cane-system\backend\src\main\java\com\ruoyi\controller\mini")

# catch 后 return Result.success(...) 改为 Result.error，并放行 UnauthorizedException
pattern = re.compile(
    r"\} catch \(Exception e\) \{\s*"
    r"(?:log\.warn\(\"请求处理失败\", e\);\s*)?"
    r"return Result\.success\([^;]+\);\s*"
    r"\}",
    re.MULTILINE,
)

replacement = """} catch (com.ruoyi.common.UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            log.warn("请求处理失败", e);
            return Result.error("请求处理失败");
        }"""

# for files without log, still ok if log exists; MiniAlarm may not have logger
for path in root.glob("Mini*Controller.java"):
    text = path.read_text(encoding="utf-8")
    if "LoggerFactory" not in text and "catch (Exception" in text:
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
    new_text, n = pattern.subn(replacement, text)
    # also fix return Result.error("...") already in catch that doesn't rethrow Unauthorized
    new_text2 = re.sub(
        r"\} catch \(Exception e\) \{\s*"
        r"(?:log\.warn\(\"请求处理失败\", e\);\s*)?"
        r"return Result\.error\(\"([^\"]+)\"\);\s*"
        r"\}",
        r"""} catch (com.ruoyi.common.UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            log.warn("请求处理失败", e);
            return Result.error("\1");
        }""",
        new_text,
        flags=re.MULTILINE,
    )
    if new_text2 != text:
        path.write_text(new_text2, encoding="utf-8")
        print("updated", path.name)
    else:
        print("no change", path.name)
