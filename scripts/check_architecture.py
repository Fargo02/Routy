"""Small dependency checks supplement Kotlin compilation and Android lint."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
violations = []
for path in (root / "shared/src/commonMain/kotlin").rglob("*.kt"):
    text = path.read_text()
    imports = [line for line in text.splitlines() if line.startswith("import ")]
    if "domain" in path.parts:
        for line in imports:
            if any(token in line for token in ("io.ktor", "android.", "androidx.compose", "platform.", ".data.", "kotlinx.serialization", "org.maplibre")):
                violations.append(f"{path.relative_to(root)}: domain dependency: {line}")
    if "presentation" in path.parts:
        for line in imports:
            if any(token in line for token in (".data.", "io.ktor")):
                violations.append(f"{path.relative_to(root)}: presentation accesses data: {line}")
    if "GlobalScope" in text:
        violations.append(f"{path.relative_to(root)}: GlobalScope")
    if "fun getPointsBetweenStations(" in text:
        violations.append(f"{path.relative_to(root)}: undocumented endpoint")
if violations:
    raise SystemExit("\n".join(violations))
print("Architecture dependency checks passed")
