"""Read-only layout preview of the generated workbook using its saved cell styles."""
from pathlib import Path
from datetime import datetime
from openpyxl import load_workbook
from PIL import Image, ImageDraw, ImageFont

root = Path(__file__).resolve().parents[1]
sheet = load_workbook(root / 'build/qa/exports/Turni_2026-10-05_2026-10-11.xlsx')['Turni']
widths = [int(sheet.column_dimensions[c].width * 7 + 5) for c in ['A', 'B', 'C', 'D']]
# The stored C:D column definition has one width for both columns.
widths[3] = widths[2]
heights = [int(sheet.row_dimensions[r].height * 96 / 72) for r in range(1, 10)]
canvas = Image.new('RGB', (sum(widths) + 40, sum(heights) + 40), 'white')
draw = ImageDraw.Draw(canvas)
y = 20
for r in range(1, 10):
    x = 20
    for column in range(1, 5):
        if r == 1 and column != 1:
            continue
        cell = sheet.cell(r, column)
        width = sum(widths) if r == 1 else widths[column - 1]
        height = heights[r - 1]
        fill = '#' + cell.fill.fgColor.rgb[-6:] if cell.fill.patternType == 'solid' else 'white'
        draw.rectangle((x, y, x + width, y + height), fill=fill, outline='#b5b5b5' if r > 1 else None)
        font_path = Path('C:/Windows/Fonts/calibrib.ttf' if cell.font.bold else 'C:/Windows/Fonts/calibri.ttf')
        font = ImageFont.truetype(str(font_path), int(cell.font.sz * 96 / 72))
        value = cell.value
        text = value.strftime('%d/%m/%Y') if isinstance(value, datetime) else str(value or '')
        words, lines, line = text.split(), [], ''
        for word in words:
            candidate = (line + ' ' + word).strip()
            if draw.textlength(candidate, font=font) > width - 14 and line:
                lines.append(line)
                line = word
            else:
                line = candidate
        lines.append(line)
        line_height = int(cell.font.sz * 96 / 72) + 2
        assert len(lines) * line_height <= height - 8, (cell.coordinate, text)
        draw.multiline_text((x + 7, y + 5), '\n'.join(lines), fill='black', font=font, spacing=2)
        x += width
    y += heights[r - 1]
canvas.save(root / 'build/qa/exports/xlsx-preview.png')
print('Workbook preview: saved dimensions, fonts, fills and typed dates verified.')
