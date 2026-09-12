"""Genera el PDF de la Obligatoria 1 a partir del Markdown versionado.

Uso:
    python docs/entrega-o1/generar_pdf.py
"""

from __future__ import annotations

import html
import re
from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfbase import pdfmetrics
from reportlab.platypus import (
    BaseDocTemplate,
    Frame,
    HRFlowable,
    ListFlowable,
    ListItem,
    PageBreak,
    PageTemplate,
    Paragraph,
    Spacer,
)


ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "docs" / "entrega-o1" / "informe-obligatoria-1.md"
OUTPUT = ROOT / "output" / "pdf" / "informe-obligatoria-1.pdf"

NAVY = colors.HexColor("#12233F")
BLUE = colors.HexColor("#315EFB")
CYAN = colors.HexColor("#00A7A7")
INK = colors.HexColor("#20283A")
MUTED = colors.HexColor("#65718A")
PALE_BLUE = colors.HexColor("#EEF3FF")
PALE_CYAN = colors.HexColor("#EAF9F7")
LINE = colors.HexColor("#D9E0EE")


def register_fonts() -> tuple[str, str, str]:
    """Usa DejaVu cuando esta disponible y Helvetica como alternativa."""

    candidates = [
        Path("C:/Windows/Fonts/DejaVuSans.ttf"),
        Path("C:/Windows/Fonts/arial.ttf"),
    ]
    bold_candidates = [
        Path("C:/Windows/Fonts/DejaVuSans-Bold.ttf"),
        Path("C:/Windows/Fonts/arialbd.ttf"),
    ]
    mono_candidates = [
        Path("C:/Windows/Fonts/DejaVuSansMono.ttf"),
        Path("C:/Windows/Fonts/consola.ttf"),
    ]

    regular = next((path for path in candidates if path.exists()), None)
    bold = next((path for path in bold_candidates if path.exists()), None)
    mono = next((path for path in mono_candidates if path.exists()), None)

    if regular and bold:
        pdfmetrics.registerFont(TTFont("PasslySans", str(regular)))
        pdfmetrics.registerFont(TTFont("PasslySans-Bold", str(bold)))
        pdfmetrics.registerFontFamily(
            "PasslySans",
            normal="PasslySans",
            bold="PasslySans-Bold",
            italic="PasslySans",
            boldItalic="PasslySans-Bold",
        )
        body_font = "PasslySans"
        bold_font = "PasslySans-Bold"
    else:
        body_font = "Helvetica"
        bold_font = "Helvetica-Bold"

    if mono:
        pdfmetrics.registerFont(TTFont("PasslyMono", str(mono)))
        mono_font = "PasslyMono"
    else:
        mono_font = "Courier"

    return body_font, bold_font, mono_font


BODY_FONT, BOLD_FONT, MONO_FONT = register_fonts()


def build_styles():
    base = getSampleStyleSheet()
    return {
        "cover_title": ParagraphStyle(
            "CoverTitle",
            parent=base["Title"],
            fontName=BOLD_FONT,
            fontSize=34,
            leading=39,
            textColor=colors.white,
            alignment=TA_LEFT,
            spaceAfter=8,
        ),
        "cover_subtitle": ParagraphStyle(
            "CoverSubtitle",
            parent=base["Heading2"],
            fontName=BODY_FONT,
            fontSize=18,
            leading=23,
            textColor=colors.white,
            alignment=TA_LEFT,
            spaceAfter=25,
        ),
        "h1": ParagraphStyle(
            "H1",
            parent=base["Heading1"],
            fontName=BOLD_FONT,
            fontSize=19,
            leading=23,
            textColor=NAVY,
            spaceBefore=0,
            spaceAfter=10,
            keepWithNext=True,
        ),
        "h2": ParagraphStyle(
            "H2",
            parent=base["Heading2"],
            fontName=BOLD_FONT,
            fontSize=12.5,
            leading=15.5,
            textColor=BLUE,
            spaceBefore=9,
            spaceAfter=4,
            keepWithNext=True,
        ),
        "h3": ParagraphStyle(
            "H3",
            parent=base["Heading3"],
            fontName=BOLD_FONT,
            fontSize=10.2,
            leading=13,
            textColor=CYAN,
            spaceBefore=7,
            spaceAfter=3,
            keepWithNext=True,
        ),
        "body": ParagraphStyle(
            "Body",
            parent=base["BodyText"],
            fontName=BODY_FONT,
            fontSize=9.15,
            leading=12.2,
            textColor=INK,
            alignment=TA_LEFT,
            spaceAfter=5.5,
        ),
        "meta": ParagraphStyle(
            "Meta",
            parent=base["BodyText"],
            fontName=BODY_FONT,
            fontSize=10.2,
            leading=15,
            textColor=NAVY,
            spaceAfter=3,
        ),
        "cover_body": ParagraphStyle(
            "CoverBody",
            parent=base["BodyText"],
            fontName=BODY_FONT,
            fontSize=9.4,
            leading=13,
            textColor=colors.HexColor("#E8EEFA"),
            alignment=TA_LEFT,
            spaceAfter=6,
        ),
        "bullet": ParagraphStyle(
            "Bullet",
            parent=base["BodyText"],
            fontName=BODY_FONT,
            fontSize=8.95,
            leading=11.7,
            textColor=INK,
            leftIndent=2,
            spaceAfter=1,
        ),
        "callout": ParagraphStyle(
            "Callout",
            parent=base["BodyText"],
            fontName=BODY_FONT,
            fontSize=9.2,
            leading=12.5,
            textColor=NAVY,
            backColor=PALE_CYAN,
            borderColor=CYAN,
            borderWidth=0.8,
            borderPadding=8,
            leftIndent=3,
            rightIndent=3,
            spaceBefore=5,
            spaceAfter=8,
        ),
    }


STYLES = build_styles()


def inline_markup(text: str) -> str:
    escaped = html.escape(text, quote=False)
    escaped = re.sub(r"\*\*(.+?)\*\*", r"<b>\1</b>", escaped)
    escaped = re.sub(
        r"`([^`]+)`",
        rf'<font name="{MONO_FONT}" color="#263B67">\1</font>',
        escaped,
    )
    return escaped


def paragraph(text: str, style: str = "body") -> Paragraph:
    return Paragraph(inline_markup(text.strip()), STYLES[style])


def parse_markdown(markdown: str):
    story = []
    lines = markdown.splitlines()
    index = 0
    first_h1 = True
    first_h2 = True
    on_cover = True

    while index < len(lines):
        raw = lines[index].rstrip()
        stripped = raw.strip()

        if not stripped:
            index += 1
            continue

        if stripped == "<!-- PAGEBREAK -->":
            story.append(PageBreak())
            on_cover = False
            index += 1
            continue

        heading = re.match(r"^(#{1,3})\s+(.+)$", stripped)
        if heading:
            level = len(heading.group(1))
            title = heading.group(2)
            if level == 1 and first_h1:
                story.append(Spacer(1, 42 * mm))
                story.append(paragraph(title, "cover_title"))
                first_h1 = False
            elif level == 2 and first_h2:
                story.append(paragraph(title, "cover_subtitle"))
                story.append(Spacer(1, 28 * mm))
                first_h2 = False
            else:
                story.append(paragraph(title, f"h{level}"))
                if level == 1:
                    story.append(HRFlowable(width="100%", thickness=1.2, color=CYAN, spaceAfter=7))
            index += 1
            continue

        if stripped.startswith("> "):
            story.append(paragraph(stripped[2:], "callout"))
            index += 1
            continue

        if re.match(r"^-\s+", stripped):
            items = []
            while index < len(lines) and re.match(r"^-\s+", lines[index].strip()):
                value = re.sub(r"^-\s+", "", lines[index].strip())
                items.append(ListItem(paragraph(value, "bullet"), leftIndent=9))
                index += 1
            story.append(
                ListFlowable(
                    items,
                    bulletType="bullet",
                    bulletFontName=BODY_FONT,
                    bulletFontSize=6,
                    bulletColor=BLUE,
                    leftIndent=14,
                    bulletOffsetY=1,
                    spaceAfter=5,
                )
            )
            continue

        if re.match(r"^\d+\.\s+", stripped):
            items = []
            while index < len(lines) and re.match(r"^\d+\.\s+", lines[index].strip()):
                value = re.sub(r"^\d+\.\s+", "", lines[index].strip())
                items.append(ListItem(paragraph(value, "bullet"), leftIndent=12))
                index += 1
            story.append(
                ListFlowable(
                    items,
                    bulletType="1",
                    start="1",
                    bulletFontName=BOLD_FONT,
                    bulletFontSize=8,
                    bulletColor=BLUE,
                    leftIndent=18,
                    spaceAfter=5,
                )
            )
            continue

        body_lines = [stripped]
        index += 1
        while index < len(lines):
            candidate = lines[index].strip()
            if not candidate:
                break
            if (
                candidate == "<!-- PAGEBREAK -->"
                or candidate.startswith("#")
                or candidate.startswith("> ")
                or re.match(r"^-\s+", candidate)
                or re.match(r"^\d+\.\s+", candidate)
            ):
                break
            body_lines.append(candidate)
            index += 1

        text = " ".join(body_lines)
        style = "cover_body" if on_cover else "body"
        story.append(paragraph(text, style))

    return story


def draw_cover_background(canvas, doc):
    width, height = A4
    page = canvas.getPageNumber()
    if page != 1:
        return

    canvas.saveState()

    canvas.setFillColor(NAVY)
    canvas.rect(0, 0, width, height, stroke=0, fill=1)
    canvas.setFillColor(BLUE)
    canvas.rect(0, height - 18 * mm, width, 18 * mm, stroke=0, fill=1)
    canvas.setFillColor(CYAN)
    canvas.rect(0, 0, 7 * mm, height, stroke=0, fill=1)
    canvas.setFillColor(colors.white)
    canvas.setFont(BODY_FONT, 8)
    canvas.drawString(22 * mm, 16 * mm, "PASSLY / DESARROLLO DE APLICACIONES II")

    canvas.restoreState()


def draw_page_chrome(canvas, doc):
    width, height = A4
    page = canvas.getPageNumber()
    if page == 1:
        return

    # Se dibuja al finalizar la pagina para que ningun flowable pueda tapar el encabezado o el pie.
    canvas.saveState()
    canvas.setFillColor(NAVY)
    canvas.rect(0, height - 12 * mm, width, 12 * mm, stroke=0, fill=1)
    canvas.setFillColor(colors.white)
    canvas.setFont(BOLD_FONT, 8)
    canvas.drawString(18 * mm, height - 7.6 * mm, "PASSLY")
    canvas.setFont(BODY_FONT, 7.5)
    canvas.drawRightString(width - 18 * mm, height - 7.6 * mm, "OBLIGATORIA 1 / 14.09.2026")
    canvas.setStrokeColor(LINE)
    canvas.line(18 * mm, 13 * mm, width - 18 * mm, 13 * mm)
    canvas.setFillColor(MUTED)
    canvas.setFont(BODY_FONT, 7.5)
    canvas.drawString(18 * mm, 8.5 * mm, "Arquitectura, componentes, patrones y verificacion")
    canvas.drawRightString(width - 18 * mm, 8.5 * mm, f"Pagina {page}")

    canvas.restoreState()


def main() -> None:
    if not SOURCE.exists():
        raise FileNotFoundError(f"No existe el Markdown fuente: {SOURCE}")

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    markdown = SOURCE.read_text(encoding="utf-8")

    document = BaseDocTemplate(
        str(OUTPUT),
        pagesize=A4,
        rightMargin=18 * mm,
        leftMargin=20 * mm,
        topMargin=19 * mm,
        bottomMargin=18 * mm,
        title="Passly - Informe de la Obligatoria 1",
        author="Equipo Passly",
        subject="Arquitectura, componentes, patrones, seguridad y uso de IA",
    )
    frame = Frame(
        document.leftMargin,
        document.bottomMargin,
        document.width,
        document.height,
        id="contenido",
        showBoundary=0,
    )
    document.addPageTemplates(PageTemplate(
        id="passly",
        frames=[frame],
        onPage=draw_cover_background,
        onPageEnd=draw_page_chrome,
    ))
    document.build(parse_markdown(markdown))
    print(OUTPUT)


if __name__ == "__main__":
    main()
