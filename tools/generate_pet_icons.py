#!/usr/bin/env python3
"""Gera um ícone de app para cada estado do pet.

O Android só troca o ícone entre variações declaradas no manifesto, então cada combinação de
pet, fase e humor vira um arquivo em res/mipmap-anydpi-v26 e um activity-alias em
app/src/main/pet-icons/AndroidManifest.xml. A troca em si acontece em notifications/PetIcon.kt.

A geometria repete a de ui/components/Mascot.kt (grade de 100 x 100): ao mudar o desenho lá,
atualize aqui e rode de novo:

    python tools/generate_pet_icons.py [pasta-da-previa]

Com o argumento, também escreve uma página HTML com todos os ícones, para conferir de uma vez.
"""
import math
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res"
OVERLAY = ROOT / "app/src/main/pet-icons/AndroidManifest.xml"

INK = "#FF1B123F"
WHITE = "#FFFFFFFF"
DEFAULT_ALIAS = "Faisca5Happy"
DROP_BLUE = "#FF7CC8FF"
MOODS = ["HAPPY", "EXCITED", "WORRIED", "SAD", "SLEEPY"]


def argb(color, alpha=1.0):
    color = color.lstrip("#")
    if len(color) == 8:
        return "#" + color
    return "#%02X%s" % (round(alpha * 255), color)


class Fill:
    """Degradê radial com as três paradas usadas no corpo dos personagens."""

    def __init__(self, light, mid, edge, cx=50, cy=58, r=56):
        self.stops = ((0, light), (0.55, mid), (1, edge))
        self.cx, self.cy, self.r = cx, cy, r


class P:
    def __init__(self, d, fill=None, stroke=None, width=0):
        self.d, self.fill, self.stroke, self.width = d, fill, stroke, width


class Clip:
    def __init__(self, d, children):
        self.d, self.children = d, children


def oval(cx, cy, rx, ry=None):
    ry = ry if ry is not None else rx
    return f"M{cx - rx:g},{cy:g} a{rx:g},{ry:g} 0 1,0 {2 * rx:g},0 a{rx:g},{ry:g} 0 1,0 {-2 * rx:g},0 Z"


def curve(x1, y1, cx, cy, x2, y2, width, color=INK):
    return P(f"M{x1:g},{y1:g} Q{cx:g},{cy:g} {x2:g},{y2:g}", stroke=color, width=width)


def drop(x, y):
    """Lágrima ou gota de suor, com a ponta para cima."""
    return P(f"M{x:g},{y:g} Q{x - 3.4:g},{y + 6:g} {x:g},{y + 7:g} Q{x + 3.4:g},{y + 6:g} {x:g},{y:g} Z", fill=DROP_BLUE)


def star(cx, cy, radius, color, alpha=1.0):
    w = radius * 0.25
    pts = [(0, -radius), (w, -w), (radius, 0), (w, w), (0, radius), (-w, w), (-radius, 0), (-w, -w)]
    return P("M" + " L".join(f"{cx + x:g},{cy + y:g}" for x, y in pts) + " Z", fill=argb(color, alpha))


class Face:
    def __init__(self, left, right, width, height, iris, look_left, look_right, lid, lid_color, iris_color,
                 mouth, mouth_width, rest):
        self.left, self.right = left, right
        self.width, self.height = width, height
        self.iris, self.look_left, self.look_right = iris, look_left, look_right
        self.lid, self.lid_color, self.iris_color = lid, lid_color, iris_color
        self.mouth, self.mouth_width = mouth, mouth_width
        self.rest = rest


def eye(center, f, iris_r, look, lid, droop=0):
    cx, cy = center
    rx, ry = f.width, f.height
    shape = oval(cx, cy, rx, ry)
    ix, iy = cx + look, cy + 0.6
    inner = [
        P(oval(ix, iy, iris_r), fill=argb(f.iris_color)),
        P(oval(ix, iy, iris_r * 0.55), fill=INK),
        P(oval(ix + iris_r * 0.38, cy - iris_r * 0.32, iris_r * 0.32), fill=WHITE),
        P(oval(ix - iris_r * 0.4, cy + iris_r * 0.42, iris_r * 0.15), fill=argb("FFFFFF", 0.85)),
    ]
    if lid > 0:
        top = cy - ry - 1
        cut = cy - ry + 2 * ry * lid
        left, right = cut + droop, cut - droop
        inner.append(P(f"M{cx - rx - 1:g},{left:g} Q{cx:g},{cut - 1.8:g} {cx + rx + 1:g},{right:g} "
                       f"L{cx + rx + 1:g},{top:g} L{cx - rx - 1:g},{top:g} Z", fill=argb(f.lid_color)))
    return [P(shape, fill=WHITE), Clip(shape, inner)]


def small_smile(m, w):
    """Boca da comemoração: pequena e fechada embaixo, para não parecer um grito."""
    x, y = m
    width = min(w * 0.5, 12)
    depth = width * 0.46
    return [P(f"M{x - width / 2:g},{y:g} Q{x:g},{y + 1.2:g} {x + width / 2:g},{y:g} "
              f"Q{x + width * 0.34:g},{y + depth:g} {x:g},{y + depth:g} "
              f"Q{x - width * 0.34:g},{y + depth:g} {x - width / 2:g},{y:g} Z", fill=INK)]


def expression(f, mood):
    out = []
    if mood == "SLEEPY":
        for c in (f.left, f.right):
            w = f.width * 0.72
            out.append(curve(c[0] - w, c[1], c[0], c[1] + 5, c[0] + w, c[1], 2.6))
    else:
        lid = 0 if mood in ("EXCITED", "WORRIED") else max(f.lid, 0.42) if mood == "SAD" else f.lid
        iris = f.iris * (0.8 if mood == "WORRIED" else 1.08 if mood == "EXCITED" else 1)
        droop = 2.8 if mood == "SAD" else 0
        out += eye(f.left, f, iris, f.look_left, lid, droop)
        out += eye(f.right, f, iris, f.look_right, lid, -droop)
    x, y = f.mouth
    w = f.mouth_width
    if mood == "HAPPY":
        out += f.rest
    elif mood == "EXCITED":
        out += small_smile(f.mouth, w)
    elif mood == "WORRIED":
        out.append(P(f"M{x - w * 0.34:g},{y + 2:g} Q{x - w * 0.17:g},{y - 2:g} {x:g},{y + 2:g} "
                     f"Q{x + w * 0.17:g},{y + 6:g} {x + w * 0.34:g},{y + 2:g}", stroke=INK, width=2.6))
        out.append(drop(f.right[0] + f.width + 1.5, f.right[1] - f.height - 2))
    elif mood == "SAD":
        out.append(curve(x - w * 0.3, y + 5, x, y - 2, x + w * 0.3, y + 5, 2.8))
        out.append(drop(f.left[0] - 2, f.left[1] + f.height + 0.5))
    else:
        out.append(P(oval(x, y + 2, 2.2), fill=INK))
    return out


LOOKS = [
    ("#C9CBE0", "#8C8FB5", "#5F6290", "#5F6290", "#3B3D63"),
    ("#FFF2A0", "#FFC928", "#F79A0B", "#E88A08", "#7A3E12"),
    ("#FFC27A", "#FF6F2C", "#E0340F", "#D93A0C", "#6E2A10"),
    ("#EDB2FF", "#B63BE6", "#7A1DC4", "#8E25D0", "#4E1A86"),
    ("#9CD0FF", "#2F7DFF", "#1546C9", "#1A4FD4", "#123A9E"),
    ("#C9BEFF", "#6F5BFF", "#2C46DC", "#3A3BD6", "#2A2190"),
]

DROP = ("M50,90 C30,90 18,76 18,60 C18,46 28,40 33,29 C36,34 39,36 42,35 C43,27 49,20 57,12 "
        "C58,23 67,30 73,39 C79,45 82,52 82,60 C82,76 70,90 50,90 Z")
HOOK = ("M50,91 C28,91 15,76 16,58 C17,44 28,37 34,24 C37,30 41,34 46,33 C48,23 58,14 74,8 "
        "C69,18 68,26 72,33 C75,30 78,26 80,20 C81,33 85,44 84,58 C84,77 72,91 50,91 Z")
TALL = ("M50,93 C38,93 29,86 26,74 C23,63 17,53 19,42 C16,39 13,35 12,28 C19,32 24,33 28,31 "
        "C27,22 30,13 38,7 C38,15 42,20 48,20 C50,13 55,6 64,1 C62,11 65,17 71,20 C74,16 78,12 "
        "84,10 C82,18 82,26 85,31 C87,29 89,26 90,22 C91,32 87,42 81,50 C81,60 77,66 74,74 C71,86 62,93 50,93 Z")
SHINE = argb("FFFFFF", 0.55)


def rest_for(stage):
    if stage == 0:
        return [curve(44, 74, 50, 77, 56, 74, 2.4)]
    if stage == 1:
        return [curve(44, 75, 50, 80, 57, 73, 2.4)]
    if stage == 2:
        return [P("M42,73 Q50,75 58,72 Q56,82 49,81 Q44,80 42,73 Z", fill=INK)]
    if stage == 3:
        return [curve(41, 74, 49, 71, 60, 70, 2.8)]
    if stage == 4:
        return [P("M34,62 Q50,68 68,60 Q65,74 48,73 Q37,72 34,62 Z", fill=INK),
                P("M36.5,63.2 Q50,68.5 65.5,61.6 Q64,65.6 61,66.4 Q50,70 39.5,66.8 Z", fill=WHITE)]
    return [P("M35,63 Q50,68 71,56 Q68,74 50,74 Q39,73 35,63 Z", fill=INK),
            P("M37.5,64.2 Q50,68.5 68.5,58.4 Q67,63 63.5,65.4 Q50,71 41,67.5 Z", fill=WHITE),
            star(63, 62.5, 3.4, "#7FB4FF")]


def face_for(stage):
    light, mid, edge, limb, iris = LOOKS[stage]
    specs = {
        0: ((39, 59), (61, 59), 8.4, 8.6, 5.6, 0.8, -0.8, 0.34, (50, 75), 14),
        1: ((39, 58), (61, 58), 10, 11.5, 7.6, 0.8, -0.8, 0, (50, 76), 16),
        2: ((37, 56), (63, 56), 10.5, 12, 7.8, 1.2, -1.2, 0, (50, 74), 20),
        3: ((36, 55), (64, 55), 11, 10, 6.2, 2.2, 0.4, 0.42, (50, 74), 20),
        4: ((37, 46), (63, 46), 10.5, 9.5, 6.2, 1.2, -0.2, 0.36, (50, 64), 34),
        5: ((37, 45), (63, 45), 10.5, 10, 6.6, 2, 1, 0.22, (52, 63), 36),
    }[stage]
    left, right, w, h, ir, ll, lr, lid, mouth, mw = specs
    return Face(left, right, w, h, ir, ll, lr, lid, mid, iris, mouth, mw, rest_for(stage))


def limbs(color, mood):
    c = argb(color)
    out = []
    for x, side in ((40, -1), (60, 1)):
        out.append(P(f"M{x},86 C{x},92 {x + side},95 {x + side},97", stroke=c, width=3))
        out.append(P(f"M{x - 3 * side},98 Q{x + side},94 {x + 6 * side},98 Z", fill=c, stroke=c, width=1.5))
    excited = mood == "EXCITED"
    for mirrored in (False, True):
        def mx(v):
            return 100 - v if mirrored else v
        arm = f"M{mx(20)},62 C{mx(13)},56 {mx(9)},48 {mx(11)},40" if excited else f"M{mx(20)},62 C{mx(13)},66 {mx(11)},74 {mx(13)},80"
        out.append(P(arm, stroke=c, width=3))
        out.append(P(oval(mx(11), 39, 2.6) if excited else oval(mx(13), 81, 2.6), fill=c))
    return out


def faisca(stage, mood):
    light, mid, edge, limb, iris = LOOKS[stage]
    fill = Fill(light, mid, edge)
    body, shine = (DROP, [curve(54, 20, 49, 26, 46, 34, 2.2, SHINE)]) if stage < 2 else \
        (HOOK, [curve(68, 14, 58, 20, 52, 31, 2.2, SHINE)]) if stage < 4 else \
        (TALL, [curve(36, 14, 33, 22, 34, 29, 2.2, SHINE), curve(62, 8, 59, 14, 60, 20, 2.2, SHINE),
                curve(16, 31, 19, 36, 23, 38, 2, argb("FFFFFF", 0.45))])
    return limbs(limb, mood) + [P(body, fill=fill, stroke=fill, width=3)] + shine + expression(face_for(stage), mood)


# Cada pet tem 6 aparências, uma por fase: começa apagado e termina azul e violeta, como a Faísca.
# Claro, meio e borda do corpo, braços, íris, faíscas elétricas.
RAIO_LOOKS = [
    ("#EFEBD0", "#C4BD8C", "#8C875C", "#8C875C", "#55512F", "#9FB4D6"),
    ("#FFFBD6", "#FFE45C", "#F2B40A", "#D99A08", "#7A5200", "#2F7DFF"),
    ("#FFF9C9", "#FFD21F", "#F59A00", "#E88A08", "#7A4A00", "#2F7DFF"),
    ("#FFEDB0", "#FFB01F", "#EE6E00", "#D85E04", "#6E3400", "#2F7DFF"),
    ("#E3F6FF", "#5CC8FF", "#1E7BF0", "#1A63D0", "#123A9E", "#FFD21F"),
    ("#F3EBFF", "#AE90FF", "#5B3BE8", "#4A2FD0", "#2A2190", "#7FE8FF"),
]
RAIO_BODIES = [
    "M44,10 L74,10 L63,31 L82,31 L41,86 L49,55 L26,55 Z",
    "M43,6 L75,6 L63,31 L83,31 L38,91 L48,55 L25,55 Z",
    "M42,3 L76,3 L63,31 L84,31 L36,96 L47,55 L23,55 Z",
    "M42,3 L76,3 L63,31 L84,31 L36,96 L47,55 L23,55 Z",
    "M40,1 L78,1 L64,29 L87,29 L58,61 L69,61 L33,99 L45,55 L21,55 Z",
    "M40,1 L78,1 L64,29 L87,29 L58,61 L69,61 L33,99 L45,55 L21,55 Z",
]
RAIO_EDGES = [7, 6, 4, 4, 3, 3]
RAIO_ARCS = ["M86,12 L80,18 L87,20 L82,27", "M14,30 L20,35 L13,39 L19,45", "M74,58 L80,62 L74,66 L80,71"]


def mini_bolt(cx, cy, color):
    """Raiozinho solto ao redor do corpo, na fase final."""
    pts = ((-2, -9), (5, -9), (1, -2), (6, -2), (-4, 10), (-1, 1), (-5, 1))
    return P("M" + " L".join(f"{cx + x:g},{cy + y:g}" for x, y in pts) + " Z", fill=color)


def raio(stage, mood):
    light, mid, edge, limb_color, iris, spark = RAIO_LOOKS[stage]
    limb = argb(limb_color)
    electric = argb(spark)
    fill = Fill(light, mid, edge, 52, 38, 60)
    face = Face((43, 33), (63, 31), 8.6, 8, 5.2, 1.6, 0.6, 0.24, mid, iris, (52, 46), 26, [
        P("M40,45 Q50,49 64,42 Q61,52 50,52 Q43,51 40,45 Z", fill=INK),
        P("M42.2,45.8 Q50,49.2 61.8,43.5 L60,46.6 Q50,50.4 44,48 Z", fill=WHITE),
    ])
    out = [P(d, stroke=electric, width=2.2) for d in RAIO_ARCS[:stage]]
    if stage == 5:
        out += [mini_bolt(90, 50, electric), mini_bolt(11, 60, electric)]
    out += [P("M26,46 C18,48 13,52 12,58", stroke=limb, width=3), P("M72,38 C80,36 86,31 88,26", stroke=limb, width=3),
            P(oval(12, 59, 2.6), fill=limb), P(oval(88, 25, 2.6), fill=limb)]
    out.append(P(RAIO_BODIES[stage], fill=fill, stroke=fill, width=RAIO_EDGES[stage]))
    out.append(curve(50, 14, 45, 18, 40, 24, 2.4, argb("FFFFFF", 0.6)))
    return out + expression(face, mood)


# Centro, meio e borda do orbe, braços, íris, raios, bochecha.
LUZ_LOOKS = [
    ("#FFFFFF", "#F1EDD9", "#C9C096", "#ADA47A", "#6E6438", "#D3CBA4", "#D9C9A0"),
    ("#FFFFFF", "#FFF6C4", "#FFD65C", "#EDB93A", "#C08000", "#FFD766", "#FFC27A"),
    ("#FFFFFF", "#FFF0A8", "#FFB627", "#E9A21E", "#B56A00", "#FFC233", "#FFB25A"),
    ("#FFFFFF", "#FFE0B8", "#FF8F4D", "#E8702E", "#A8420E", "#FF9A52", "#FF8A6A"),
    ("#FFFFFF", "#CFF0FF", "#54B8FF", "#2F8CE8", "#1557B0", "#7FD0FF", "#7FB4FF"),
    ("#FFFFFF", "#E8DFFF", "#A17BFF", "#7A55EE", "#4A2FB8", "#BFA6FF", "#B79BFF"),
]
LUZ_RAYS = [4, 4, 8, 8, 12, 12]
LUZ_REACH = [(45, 45), (49, 49), (56, 41), (58, 42), (60, 44), (63, 45)]


def luz(stage, mood):
    center, mid, edge, limb_color, iris, ray_color, cheek = LUZ_LOOKS[stage]
    limb = argb(limb_color)
    gold = argb(ray_color)
    cx, cy = 50, 52
    fill = Fill(center, mid, edge, cx, cy, 34)
    face = Face((39, 49), (61, 49), 8.6, 9.6, 6.2, 1.2, -1.2, 0.2, mid, iris, (50, 65), 20,
                [curve(41, 65, 50, 73, 60, 64, 2.8)])
    count = LUZ_RAYS[stage]
    long_reach, short_reach = LUZ_REACH[stage]
    out = []
    for i in range(count):
        angle = i * 2 * math.pi / count
        reach, spread = (long_reach, 0.09) if i % 2 == 0 else (short_reach, 0.06)
        pts = [(cx + 30 * math.cos(angle - spread), cy + 30 * math.sin(angle - spread)),
               (cx + reach * math.cos(angle), cy + reach * math.sin(angle)),
               (cx + 30 * math.cos(angle + spread), cy + 30 * math.sin(angle + spread))]
        out.append(P("M" + " L".join(f"{x:.2f},{y:.2f}" for x, y in pts) + " Z", fill=gold))
    if stage >= 4:
        out.append(P(oval(cx, cy, 38), stroke=gold, width=2))
    if stage == 5:
        out.append(P(oval(cx, cy, 43), stroke=argb(ray_color, 0.55), width=1.4))
    out += [P("M18,60 C12,62 10,66 11,70", stroke=limb, width=3), P("M82,60 C88,62 90,66 89,70", stroke=limb, width=3),
            P(oval(11, 71, 2.6), fill=limb), P(oval(89, 71, 2.6), fill=limb)]
    out.append(P(oval(cx, cy, 33), fill=fill))
    out += [P(oval(29, 61, 5, 3), fill=argb(cheek, 0.55)), P(oval(71, 61, 5, 3), fill=argb(cheek, 0.55))]
    return out + expression(face, mood)


# Claro, meio e borda do tronco, músculo, braço, punho, bíceps, brilho, pé, íris.
MAROMBA_LOOKS = [
    ("#DAD7DF", "#9A95A8", "#66617A", "#55506A", "#8A85A0", "#7A7590", "#A8A3B8", "#E6E3EC", "#45405A", "#3B3D63"),
    ("#FFF0A0", "#FFA32B", "#D9640A", "#B4530A", "#F08A1A", "#E0780F", "#FFB84D", "#FFE9A8", "#8A4204", "#7A3E12"),
    ("#FFE08A", "#FF5A1F", "#B9130A", "#A81208", "#F0401A", "#E5340F", "#FF7A3D", "#FFD08A", "#7E0E06", "#6E1A08"),
    ("#FFC8B0", "#E8264A", "#8E0E3A", "#7A0A32", "#D81E48", "#C0183E", "#F0506E", "#FFC8CE", "#5E0826", "#5E0A2C"),
    ("#C8EAFF", "#2F8CFF", "#1546C9", "#123A9E", "#2F7DFF", "#1F63E0", "#5CA8FF", "#C8EAFF", "#0E2E80", "#123A9E"),
    ("#E8DFFF", "#7A5BFF", "#2C2EC9", "#2A2190", "#6F5BFF", "#5B45E8", "#9C86FF", "#E8DFFF", "#211A78", "#2A2190"),
]
# Por fase: largura do tronco, braço, antebraço, punho, bíceps e quanto a coroa de fogo abaixa.
MAROMBA_BUILDS = [
    (0.86, 8, 6, 4.6, (5.2, 4), 10),
    (0.92, 10, 7, 5.6, (6.4, 5), 6),
    (1, 13, 9, 7, (8, 6.6), 0),
    (1, 14, 9.5, 7.2, (8.6, 7.2), 0),
    (1.06, 15, 10.5, 7.8, (9.6, 8), 0),
    (1.1, 16.5, 11.5, 8.4, (10.8, 9), 0),
]
MAROMBA_TORSO = ((37, 88), (35, 78), (30, 68), (24, 60), (19, 53), (19, 45), (27, 41), (30, 39), (33, 39), (35, 37),
                 (30, 29), (31, 18), (38, 10), (38, 17), (41, 20), (46, 20), (48, 13), (52, 6), (60, 0), (59, 9),
                 (62, 15), (67, 18), (69, 15), (72, 12), (76, 10), (75, 20), (73, 30), (65, 37), (67, 39), (70, 39),
                 (73, 41), (81, 45), (81, 53), (76, 60), (70, 68), (65, 78), (63, 88))


def maromba_flame(x, y, side, color):
    """Labareda no ombro, nas fases mais fortes."""
    return P(f"M{x:g},{y:g} C{x + 5 * side:g},{y - 3:g} {x + 8 * side:g},{y - 8:g} {x + 8 * side:g},{y - 15:g} "
             f"C{x + 3 * side:g},{y - 12:g} {x:g},{y - 9:g} {x - 3 * side:g},{y - 4:g} Z", fill=color)


def maromba(stage, mood):
    light, mid, edge, muscle_color, arm_color, fist, bulge, highlight, foot, iris = MAROMBA_LOOKS[stage]
    width, upper, fore, fist_r, (bulge_x, bulge_y), crown = MAROMBA_BUILDS[stage]
    muscle = argb(muscle_color)
    fill = Fill(light, mid, edge, 50, 54, 60)
    arm = argb(arm_color)
    face = Face((42, 26), (58, 26), 6.6, 6, 3.9, 1.2, 0.2, 0.34, mid, iris, (50, 34), 18, [
        P("M43,33.4 Q50,36.4 58,32.6 Q56.4,38.6 50,38.6 Q44.6,38.6 43,33.4 Z", fill=INK),
        P("M44.6,34.4 Q50,36.6 56.4,33.8 L55.2,35.8 Q50,37.6 45.8,36 Z", fill=WHITE),
    ])

    def tx(v):
        return 50 + (v - 50) * width

    def ty(v):
        return v + crown * max(0, 37 - v) / 37

    out = [P("M42,86 L41,95", stroke=muscle, width=8), P("M58,86 L59,95", stroke=muscle, width=8)]
    for a, b in ((34, 46), (54, 66)):
        out.append(P(f"M{a},99 Q{(a + b) / 2:g},92 {b},99 Z", fill=argb(foot), stroke=argb(foot), width=2))
    for mirrored in (False, True):
        def x(v):
            return 100 - v if mirrored else v

        def inner(v):
            return 100 - tx(v) if mirrored else tx(v)
        if stage >= 4:
            side = 1 if mirrored else -1
            out.append(maromba_flame(inner(27) + 3 * side, 42, side, argb(bulge)))
        out.append(P(f"M{inner(25):g},47 C{x(16)},48 {x(8)},52 {x(5)},46", stroke=arm, width=upper))
        out.append(P(f"M{x(4)},46 C{x(2)},40 {x(4)},34 {x(8)},29", stroke=arm, width=fore))
        out.append(P(oval(x(9), 25, fist_r), fill=argb(fist)))
        out.append(P(oval(x(14), 43, bulge_x, bulge_y), fill=argb(bulge)))
        out.append(curve(x(10), 42, x(14), 38, x(19), 41, 1.8, argb(highlight, 0.8)))
    spaced = [(tx(px), ty(py)) for px, py in MAROMBA_TORSO]
    torso = f"M{spaced[0][0]:g},{spaced[0][1]:g} "
    for i in range(1, len(spaced), 3):
        (ax, ay), (bx, by), (cx, cy) = spaced[i:i + 3]
        torso += f"C{ax:g},{ay:g} {bx:g},{by:g} {cx:g},{cy:g} "
    out.append(P(torso + "Z", fill=fill, stroke=fill, width=3))
    out += [curve(tx(29), 50, 40, 60, 49, 51, 2.2, muscle), curve(tx(71), 50, 60, 60, 51, 51, 2.2, muscle),
            P("M50,52 L50,84", stroke=muscle, width=1.8)]
    for y in (64, 72):
        out += [curve(39, y, 43.5, y + 2, 48, y, 1.8, muscle), curve(52, y, 56.5, y + 2, 61, y, 1.8, muscle)]
    if stage >= 4:
        out += [curve(41, 80, 44, 82, 48, 80, 1.8, muscle), curve(52, 80, 56, 82, 59, 80, 1.8, muscle)]
    return out + expression(face, mood)


# ---------- saída ----------


def gradient_xml(fill, indent):
    pad = " " * indent
    items = "".join(f'{pad}        <item android:offset="{o}" android:color="{argb(c)}" />\n' for o, c in fill.stops)
    return (f'{pad}<gradient android:type="radial" android:centerX="{fill.cx}" android:centerY="{fill.cy}" '
            f'android:gradientRadius="{fill.r}">\n{items}{pad}</gradient>\n')


def path_xml(p, indent):
    pad = " " * indent
    attrs = [f'android:pathData="{p.d}"']
    inner = ""
    if isinstance(p.fill, Fill) or isinstance(p.stroke, Fill):
        if isinstance(p.fill, Fill):
            inner += f'{pad}    <aapt:attr name="android:fillColor">\n{gradient_xml(p.fill, indent + 8)}{pad}    </aapt:attr>\n'
        if isinstance(p.stroke, Fill):
            inner += f'{pad}    <aapt:attr name="android:strokeColor">\n{gradient_xml(p.stroke, indent + 8)}{pad}    </aapt:attr>\n'
    if isinstance(p.fill, str):
        attrs.append(f'android:fillColor="{p.fill}"')
    if isinstance(p.stroke, str):
        attrs.append(f'android:strokeColor="{p.stroke}"')
    if p.stroke is not None:
        attrs += [f'android:strokeWidth="{p.width}"', 'android:strokeLineCap="round"', 'android:strokeLineJoin="round"']
    body = " ".join(attrs)
    if inner:
        return f"{pad}<path {body}>\n{inner}{pad}</path>\n"
    return f"{pad}<path {body} />\n"


def nodes_xml(nodes, indent):
    out = ""
    for n in nodes:
        if isinstance(n, Clip):
            pad = " " * indent
            out += f'{pad}<group>\n{pad}    <clip-path android:pathData="{n.d}" />\n'
            out += nodes_xml(n.children, indent + 4)
            out += f"{pad}</group>\n"
        else:
            out += path_xml(n, indent)
    return out


def adaptive_icon(nodes):
    return ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<!-- Gerado por tools/generate_pet_icons.py. Não edite à mão. -->\n'
            '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    xmlns:aapt="http://schemas.android.com/aapt">\n'
            '    <background android:drawable="@color/ic_launcher_background" />\n'
            '    <foreground>\n'
            '        <aapt:attr name="android:drawable">\n'
            '            <vector android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">\n'
            '                <group android:scaleX="0.6" android:scaleY="0.6" android:translateX="24" android:translateY="23">\n'
            + nodes_xml(nodes, 20) +
            '                </group>\n'
            '            </vector>\n'
            '        </aapt:attr>\n'
            '    </foreground>\n'
            '</adaptive-icon>\n')


CLIP_ID = [0]


def svg_color(color):
    return "#" + color[3:], int(color[1:3], 16) / 255


def nodes_svg(nodes, defs):
    out = ""
    for n in nodes:
        if isinstance(n, Clip):
            CLIP_ID[0] += 1
            defs.append(f'<clipPath id="c{CLIP_ID[0]}"><path d="{n.d}"/></clipPath>')
            out += f'<g clip-path="url(#c{CLIP_ID[0]})">{nodes_svg(n.children, defs)}</g>'
            continue
        attrs = ""
        for name, value in (("fill", n.fill), ("stroke", n.stroke)):
            if isinstance(value, Fill):
                CLIP_ID[0] += 1
                stops = "".join(f'<stop offset="{o}" stop-color="{c}"/>' for o, c in value.stops)
                defs.append(f'<radialGradient id="g{CLIP_ID[0]}" gradientUnits="userSpaceOnUse" cx="{value.cx}" cy="{value.cy}" r="{value.r}">{stops}</radialGradient>')
                attrs += f' {name}="url(#g{CLIP_ID[0]})"'
            elif value is not None:
                c, a = svg_color(value)
                attrs += f' {name}="{c}" {name}-opacity="{a:.2f}"'
            else:
                attrs += f' {name}="none"'
        if n.stroke is not None:
            attrs += f' stroke-width="{n.width}" stroke-linecap="round" stroke-linejoin="round"'
        out += f'<path d="{n.d}"{attrs}/>'
    return out


def combos():
    for name, draw in (("Faisca", faisca), ("Raio", raio), ("Luz", luz), ("Maromba", maromba)):
        for stage in range(6):
            for mood in MOODS:
                yield f"{name}{stage}", mood, lambda d=draw, st=stage, m=mood: d(st, m)


def alias_name(pet, mood):
    return pet + mood.capitalize()


def main():
    out = RES / "mipmap-anydpi-v26"
    aliases = []
    preview = ['<!doctype html><meta charset="utf-8"><body style="margin:0;background:#EEF;font:12px sans-serif">']
    current = None
    for pet, mood, draw in combos():
        nodes = draw()
        key = alias_name(pet, mood)
        file = "ic_pet_" + key.lower()
        (out / f"{file}.xml").write_text(adaptive_icon(nodes), encoding="utf-8", newline="\n")
        enabled = "true" if key == DEFAULT_ALIAS else "false"
        aliases.append(f'''        <activity-alias
            android:name="com.ritmo.app.icon.{key}"
            android:enabled="{enabled}"
            android:exported="true"
            android:icon="@mipmap/{file}"
            android:label="Ritmo"
            android:targetActivity="com.ritmo.app.LauncherActivity">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity-alias>
''')
        if pet != current:
            preview.append(f'<div style="clear:both;padding:6px 8px 0">{pet}</div>')
            current = pet
        defs = []
        body = nodes_svg(nodes, defs)
        size = 112
        preview.append(f'<div style="float:left;margin:4px;text-align:center"><svg width="{size}" height="{size}" viewBox="18 18 72 72">'
                       f'<defs>{"".join(defs)}</defs><circle cx="54" cy="54" r="36" fill="#211951"/>'
                       f'<g transform="translate(24 23) scale(.6)">{body}</g></svg><br>{mood}</div>')

    OVERLAY.parent.mkdir(exist_ok=True)
    OVERLAY.write_text('<?xml version="1.0" encoding="utf-8"?>\n'
                       '<!-- Gerado por tools/generate_pet_icons.py. Não edite à mão. -->\n'
                       '<manifest xmlns:android="http://schemas.android.com/apk/res/android">\n'
                       '    <application>\n'
                       '        <activity\n'
                       '            android:name="com.ritmo.app.LauncherActivity"\n'
                       '            android:exported="false"\n'
                       '            android:relinquishTaskIdentity="true" />\n'
                       + "".join(aliases) + '    </application>\n</manifest>\n',
                       encoding="utf-8", newline="\n")

    # O ícone usado pelo sistema fora do launcher (ajustes do app) é a fase final, feliz.
    (out / "ic_launcher.xml").write_text(adaptive_icon(faisca(5, "HAPPY")), encoding="utf-8", newline="\n")
    print(len(aliases), "ícones gerados")

    if len(sys.argv) > 1:
        pathlib.Path(sys.argv[1], "icons.html").write_text("".join(preview), encoding="utf-8")


if __name__ == "__main__":
    main()
